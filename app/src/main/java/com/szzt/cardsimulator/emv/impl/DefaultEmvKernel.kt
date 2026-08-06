package com.szzt.cardsimulator.emv.impl

import com.szzt.cardsimulator.emv.api.EmvKernel
import com.szzt.cardsimulator.emv.api.EmvKernelState
import com.szzt.cardsimulator.emv.api.CryptoEngine
import com.szzt.cardsimulator.emv.api.CertificateProvider
import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import com.szzt.cardsimulator.profile.model.CardProfile
import timber.log.Timber

/**
 * Default EMV kernel implementation.
 * Configuration-driven — uses the active [CardProfile] for all card data.
 *
 * State machine:
 *   IDLE → (SELECT) → SELECTED → (GPO) → GPO_DONE → (READ RECORD×N) → DATA_READ → (GENERATE AC) → AC_GENERATED
 */
class DefaultEmvKernel(
    private val cryptoEngine: CryptoEngine,
    private val certificateProvider: CertificateProvider
) : EmvKernel {

    override var state: EmvKernelState = EmvKernelState.IDLE
        private set

    override var selectedAid: ByteArray? = null
        private set

    private var activeProfile: CardProfile? = null

    // Cached data built during transaction
    private var aipBytes: ByteArray? = null
    private var aflBytes: ByteArray? = null
    private var records: MutableMap<Int, MutableMap<Int, ByteArray>> = mutableMapOf() // SFI → RecordNum → Data

    fun setProfile(profile: CardProfile) {
        this.activeProfile = profile
        reset()
    }

    override suspend fun processApdu(command: ApduCommand): ApduResponse {
        val profile = activeProfile ?: return ApduResponse.FILE_NOT_FOUND

        return when {
            // SELECT (PPSE or AID)
            command.cla == 0x00 && command.ins == 0xA4 -> handleSelect(command, profile)
            // GPO
            command.cla == 0x80 && command.ins == 0xA8 -> handleGpo(command, profile)
            // READ RECORD
            command.cla == 0x00 && command.ins == 0xB2 -> handleReadRecord(command, profile)
            // GENERATE AC
            command.cla == 0x80 && command.ins == 0xAE -> handleGenerateAc(command, profile)
            else -> {
                Timber.w("Unsupported APDU: CLA=${String.format("%02X", command.cla)} INS=${String.format("%02X", command.ins)}")
                ApduResponse.INS_NOT_SUPPORTED
            }
        }
    }

    override fun reset() {
        state = EmvKernelState.IDLE
        selectedAid = null
        aipBytes = null
        aflBytes = null
        records.clear()
    }

    // ==================== SELECT ====================

    private fun handleSelect(command: ApduCommand, profile: CardProfile): ApduResponse {
        val p1 = command.p1
        val aidData = command.data

        return when {
            // SELECT by name (AID)
            p1 == 0x04 && aidData != null -> selectByAid(aidData, profile)
            // SELECT PPSE
            p1 == 0x04 && aidData == null -> selectPpse(profile)
            else -> ApduResponse.CONDITIONS_NOT_SATISFIED
        }
    }

    private fun selectByAid(aid: ByteArray, profile: CardProfile): ApduResponse {
        val aidHex = aid.joinToString("") { "%02X".format(it) }
        val matchedConfig = profile.aids.find { it.aid.equals(aidHex, ignoreCase = true) }
            ?: return ApduResponse.FILE_NOT_FOUND

        // Build FCI template
        val fci = buildFciTemplate(profile, matchedConfig.aid, matchedConfig.label)
        selectedAid = aid
        state = EmvKernelState.SELECTED
        Timber.d("AID selected: $aidHex")
        return ApduResponse(sw1 = 0x90, sw2 = 0x00, data = fci)
    }

    private fun selectPpse(profile: CardProfile): ApduResponse {
        // Build PPSE response: FCI template with directory entries
        val ppseData = buildPpseResponse(profile)
        state = EmvKernelState.IDLE // PPSE does not change selection state
        return ApduResponse(sw1 = 0x90, sw2 = 0x00, data = ppseData)
    }

    // ==================== GPO ====================

    private fun handleGpo(command: ApduCommand, profile: CardProfile): ApduResponse {
        if (state != EmvKernelState.SELECTED && state != EmvKernelState.IDLE) {
            return ApduResponse.CONDITIONS_NOT_SATISFIED
        }

        // Build GPO response (AIP + AFL)
        val aip = hexStringToBytes(profile.aip)
        val afl = hexStringToBytes(profile.afl)
        aipBytes = aip
        aflBytes = afl

        val responseData = buildTlv(0x82, aip) + buildTlv(0x94, afl)
        state = EmvKernelState.GPO_DONE
        Timber.d("GPO processed, AFL entries: ${afl.size / 4}")
        return ApduResponse(sw1 = 0x90, sw2 = 0x00, data = responseData)
    }

    // ==================== READ RECORD ====================

    private fun handleReadRecord(command: ApduCommand, profile: CardProfile): ApduResponse {
        if (state != EmvKernelState.GPO_DONE && state != EmvKernelState.DATA_READ) {
            return ApduResponse.CONDITIONS_NOT_SATISFIED
        }

        val sfi = command.p2 shr 3
        val recordNum = command.p1

        // Build record data based on profile
        val recordData = buildRecordData(profile, sfi, recordNum)

        // Cache record
        records.getOrPut(sfi) { mutableMapOf() }[recordNum] = recordData

        // Check if all AFL records have been read
        val aflEntries = aflBytes?.let { com.szzt.cardsimulator.emv.model.AflEntry.parse(it) } ?: emptyList()
        val allRead = aflEntries.all { entry ->
            val sfiRecords = records[entry.sfi] ?: return@all false
            (entry.firstRecord..entry.lastRecord).all { it in sfiRecords }
        }

        if (allRead) {
            state = EmvKernelState.DATA_READ
            Timber.d("All records read")
        }

        return ApduResponse(sw1 = 0x90, sw2 = 0x00, data = recordData)
    }

    // ==================== GENERATE AC ====================

    private suspend fun handleGenerateAc(command: ApduCommand, profile: CardProfile): ApduResponse {
        if (state != EmvKernelState.DATA_READ) {
            return ApduResponse.CONDITIONS_NOT_SATISFIED
        }

        val cryptogramType = command.p1 // 0x80 = ARQC, 0x40 = TC, 0x00 = AAC

        // Generate cryptogram
        val cdol1Data = command.data ?: ByteArray(0)
        val cryptogramResult = cryptoEngine.generateApplicationCryptogram(
            cdol1Data = cdol1Data,
            keyId = profile.symmetricKeyId,
            cryptogramType = cryptogramType
        )

        // Build GENERATE AC response
        val responseData = buildGenerateAcResponse(cryptogramResult, profile)
        state = EmvKernelState.AC_GENERATED
        Timber.d("AC generated, type: ${String.format("%02X", cryptogramType)}")
        return ApduResponse(sw1 = 0x90, sw2 = 0x00, data = responseData)
    }

    // ==================== Data Builders (stub implementations) ====================

    private fun buildFciTemplate(profile: CardProfile, aid: String, label: String): ByteArray {
        // 6F (FCI Template) {
        //   84 (DF Name = AID)
        //   A5 (FCI Proprietary Template) {
        //     50 (Application Label)
        //     87 (Application Priority Indicator)
        //     9F38 (PDOL)
        //   }
        // }
        val aidBytes = hexStringToBytes(aid)
        val labelBytes = label.toByteArray(Charsets.US_ASCII)
        val proprietaryContent =
            buildTlv(0x50, labelBytes) +
            buildTlv(0x87, byteArrayOf(0x01)) +
            buildPdolTlv(profile)

        val proprietary = buildTlv(0xA5, proprietaryContent)
        return buildTlv(0x6F, buildTlv(0x84, aidBytes) + proprietary)
    }

    private fun buildPpseResponse(profile: CardProfile): ByteArray {
        // 6F (FCI Template) {
        //   84 (DF Name = "2PAY.SYS.DDF01")
        //   A5 (FCI Proprietary Template) {
        //     BF0C (FCI Issuer Discretionary Data) {
        //       61 (Directory Entry) × N
        //     }
        //   }
        // }
        val ppseDfName = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

        val directoryEntries = profile.aids.map { config ->
            val entryContent =
                buildTlv(0x4F, hexStringToBytes(config.aid)) +
                buildTlv(0x50, config.label.toByteArray(Charsets.US_ASCII)) +
                buildTlv(0x87, byteArrayOf(config.priority.toByte()))
            buildTlv(0x61, entryContent) // Application Template
        }

        val issuerData = buildTlv(0xBF0C, directoryEntries.fold(ByteArray(0)) { acc, b -> acc + b })
        val proprietary = buildTlv(0xA5, issuerData)
        return buildTlv(0x6F, buildTlv(0x84, ppseDfName) + proprietary)
    }

    private fun buildPdolTlv(profile: CardProfile): ByteArray {
        if (profile.pdol.isBlank()) return ByteArray(0)
        return buildTlv(0x9F38, hexStringToBytes(profile.pdol))
    }

    private fun buildRecordData(profile: CardProfile, sfi: Int, recordNum: Int): ByteArray {
        // For simplicity, return a minimal record template
        // In production this would be driven by the AFL record structure
        val recordContent = mutableListOf<ByteArray>()

        // Track 2 Equivalent Data
        if (profile.track2EquivalentData.isNotBlank()) {
            recordContent.add(buildTlv(0x57, hexStringToBytes(profile.track2EquivalentData)))
        }
        // PAN
        if (profile.pan.isNotBlank()) {
            recordContent.add(buildTlv(0x5A, panToBytes(profile.pan)))
        }
        // Cardholder name
        if (profile.cardholderName.isNotBlank()) {
            recordContent.add(buildTlv(0x5F20, profile.cardholderName.toByteArray(Charsets.US_ASCII)))
        }
        // Expiration date
        if (profile.applicationExpirationDate.isNotBlank()) {
            recordContent.add(buildTlv(0x5F24, profile.applicationExpirationDate.toByteArray(Charsets.US_ASCII)))
        }
        // CVM List
        if (profile.cvmList.isNotEmpty()) {
            recordContent.add(buildTlv(0x8E, buildCvmList(profile)))
        }
        // Issuer Country Code
        if (profile.issuerCountryCode.isNotBlank()) {
            recordContent.add(buildTlv(0x5F28, hexStringToBytes(profile.issuerCountryCode)))
        }

        return buildTlv(0x70, recordContent.fold(ByteArray(0)) { acc, b -> acc + b })
    }

    private fun buildGenerateAcResponse(result: com.szzt.cardsimulator.emv.api.CryptogramResult, profile: CardProfile): ByteArray {
        val content = mutableListOf<ByteArray>()

        // Cryptogram (Tag 0x9F26)
        content.add(buildTlv(0x9F26, result.cryptogram))
        // Cryptogram Information Data (Tag 0x9F27)
        content.add(buildTlv(0x9F27, result.cvr))
        // Application Transaction Counter (Tag 0x9F36)
        content.add(buildTlv(0x9F36, result.applicationTransactionCounter))
        // Issuer Application Data (Tag 0x9F10)
        if (profile.issuerApplicationData.isNotBlank()) {
            content.add(buildTlv(0x9F10, hexStringToBytes(profile.issuerApplicationData)))
        }
        // Issuer Authentication Data (Tag 0x91) — for ARPC placeholder
        // Application Cryptogram related data

        return buildTlv(0x77, content.fold(ByteArray(0)) { acc, b -> acc + b })
    }

    // ==================== Utilities ====================

    private fun buildTlv(tag: Int, value: ByteArray): ByteArray {
        val tagBytes = if (tag > 0xFF) {
            byteArrayOf(((tag shr 8) and 0xFF).toByte(), (tag and 0xFF).toByte())
        } else {
            byteArrayOf((tag and 0xFF).toByte())
        }
        val lenBytes = when {
            value.size < 0x80 -> byteArrayOf(value.size.toByte())
            value.size <= 0xFF -> byteArrayOf(0x81.toByte(), value.size.toByte())
            else -> byteArrayOf(0x82.toByte(), ((value.size shr 8) and 0xFF).toByte(), (value.size and 0xFF).toByte())
        }
        return tagBytes + lenBytes + value
    }

    private fun hexStringToBytes(hex: String): ByteArray {
        val cleaned = hex.replace(" ", "").replace("\n", "")
        require(cleaned.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(cleaned.length / 2) {
            cleaned.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    private fun panToBytes(pan: String): ByteArray {
        // EMV PAN encoding: BCD with padding
        val digits = pan.filter { it.isDigit() }
        val padded = if (digits.length % 2 != 0) "0$digits" else digits
        return ByteArray(padded.length / 2) {
            padded.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    private fun buildCvmList(profile: CardProfile): ByteArray {
        val cvmBytes = profile.cvmList.flatMap { entry ->
            listOf(
                entry.cvmCode.toByte(),
                entry.cvmCondition.toByte(),
                entry.applySucceedingCvm.toByte()
            )
        }
        // CVM List format: <amount X> <amount Y> <CVM entries...>
        return byteArrayOf(0x00, 0x00, 0x00, 0x00) + cvmBytes.toByteArray()
    }
}
