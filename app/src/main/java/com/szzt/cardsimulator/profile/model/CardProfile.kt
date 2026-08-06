package com.szzt.cardsimulator.profile.model

import com.szzt.cardsimulator.emv.model.CardNetwork
import kotlinx.serialization.Serializable

/**
 * Domain model for a single card profile.
 *
 * All EMV-relevant parameters are captured here.
 * The active profile drives the EMV kernel's behavior.
 */
@Serializable
data class CardProfile(
    val id: String,
    val name: String,
    val description: String = "",

    /** Card network this profile belongs to. */
    val network: CardNetwork = CardNetwork.VISA,

    /** Is this profile currently active for NFC emulation? */
    val isActive: Boolean = false,

    // --- Application identifiers ---
    /** PPSE directory entries (one or more AIDs this card supports). */
    val aids: List<AidConfig> = emptyList(),

    // --- Card data ---
    /** Primary Account Number. */
    val pan: String = "",
    /** PAN sequence number. */
    val panSequenceNumber: Int = 1,
    /** Application expiration date (YYMMDD). */
    val applicationExpirationDate: String = "",
    /** Cardholder name. */
    val cardholderName: String = "",
    /** Track 2 Equivalent Data (hex string). */
    val track2EquivalentData: String = "",
    val applicationLabel: String = "",
    val applicationVersionNumber: String = "0001",

    // --- GPO configuration ---
    val aip: String = "0000",
    val afl: String = "",

    // --- Issuer data ---
    val issuerApplicationData: String = "",
    val issuerCountryCode: String = "",

    // --- CVM ---
    val cvmList: List<CvmEntry> = emptyList(),

    // --- DOL configuration ---
    val pdol: String = "",
    val cdol1: String = "",
    val cdol2: String = "",
    val tdol: String = "",
    val ddol: String = "",

    // --- Key references (IDs in KeyStore) ---
    /** Symmetric key ID (MDK/UDK) for ARQC/ARPC. */
    val symmetricKeyId: String = "",
    /** RSA key pair ID for SDA/DDA/CDA. */
    val rsaKeyId: String = "",

    // --- Certificate chain reference ---
    val certificateProfileId: String = "",

    // --- Metadata ---
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * AID registration entry.
 */
@Serializable
data class AidConfig(
    /** Application Identifier (hex string, e.g. "A0000000031010" for Visa). */
    val aid: String,
    /** Application label for PPSE directory. */
    val label: String = "",
    /** Application priority indicator (0 = highest). */
    val priority: Int = 0
)
