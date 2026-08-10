package com.szzt.cardsimulator.profile.impl

import com.szzt.cardsimulator.profile.api.ProfileRepository
import com.szzt.cardsimulator.profile.impl.db.ProfileDao
import com.szzt.cardsimulator.profile.impl.db.ProfileEntity
import com.szzt.cardsimulator.profile.model.AidConfig
import com.szzt.cardsimulator.profile.model.CardProfile
import com.szzt.cardsimulator.profile.model.CvmEntry
import com.szzt.cardsimulator.emv.model.CardNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class RoomProfileRepository(
    private val dao: ProfileDao
) : ProfileRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override fun observeAll(): Flow<List<CardProfile>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: String): CardProfile? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getActive(): CardProfile? {
        return dao.getActive()?.toDomain()
    }

    override fun observeActive(): Flow<CardProfile?> {
        return dao.observeActive().map { it?.toDomain() }
    }

    override suspend fun save(profile: CardProfile) {
        dao.save(profile.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
        // If the deleted profile was active, no profile is active anymore —
        // this matches the invariant "at most one active profile". The UI
        // observes the change through observeActive().
    }

    override suspend fun setActive(id: String) {
        dao.setActive(id)
    }

    override suspend fun importFromJson(jsonString: String): CardProfile {
        val profile = json.decodeFromString(CardProfile.serializer(), jsonString)
        val imported = if (profile.isActive) {
            // Preserve the "only one active" invariant: activating an imported
            // profile deactivates all others in the same atomic statement.
            dao.setActive(profile.id)
            profile.copy(isActive = true, updatedAt = System.currentTimeMillis())
        } else {
            profile.copy(updatedAt = System.currentTimeMillis())
        }
        dao.save(imported.toEntity())
        return imported
    }

    override suspend fun exportToJson(id: String): String {
        val profile = getById(id) ?: throw IllegalArgumentException("Profile not found: $id")
        return json.encodeToString(profile)
    }

    // --- Mapping ---

    private fun ProfileEntity.toDomain(): CardProfile {
        return CardProfile(
            id = id,
            name = name,
            description = description,
            network = try { CardNetwork.valueOf(network) } catch (_: Exception) { CardNetwork.VISA },
            isActive = isActive,
            pan = pan,
            panSequenceNumber = panSequenceNumber,
            applicationExpirationDate = applicationExpirationDate,
            cardholderName = cardholderName,
            track2EquivalentData = track2EquivalentData,
            applicationLabel = applicationLabel,
            applicationVersionNumber = applicationVersionNumber,
            aip = aip,
            afl = afl,
            issuerApplicationData = issuerApplicationData,
            issuerCountryCode = issuerCountryCode,
            pdol = pdol,
            cdol1 = cdol1,
            cdol2 = cdol2,
            tdol = tdol,
            ddol = ddol,
            symmetricKeyId = symmetricKeyId,
            rsaKeyId = rsaKeyId,
            certificateProfileId = certificateProfileId,
            aids = decodeJsonList(aidsJson, ListSerializer(AidConfig.serializer()), "aids") ?: emptyList(),
            cvmList = decodeJsonList(cvmListJson, ListSerializer(CvmEntry.serializer()), "cvmList") ?: emptyList(),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Decode a JSON column defensively: a single corrupt row must not crash
     * the whole [observeAll] flow — it degrades to null (caller falls back
     * to an empty list) instead.
     */
    private fun <T> decodeJsonList(raw: String, serializer: kotlinx.serialization.KSerializer<T>, column: String): T? {
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            Timber.e(e, "Corrupt JSON column '$column' in profile row, degrading to empty")
            null
        }
    }

    private fun CardProfile.toEntity(): ProfileEntity {
        return ProfileEntity(
            id = id,
            name = name,
            description = description,
            network = network.name,
            isActive = isActive,
            pan = pan,
            panSequenceNumber = panSequenceNumber,
            applicationExpirationDate = applicationExpirationDate,
            cardholderName = cardholderName,
            track2EquivalentData = track2EquivalentData,
            applicationLabel = applicationLabel,
            applicationVersionNumber = applicationVersionNumber,
            aip = aip,
            afl = afl,
            issuerApplicationData = issuerApplicationData,
            issuerCountryCode = issuerCountryCode,
            pdol = pdol,
            cdol1 = cdol1,
            cdol2 = cdol2,
            tdol = tdol,
            ddol = ddol,
            symmetricKeyId = symmetricKeyId,
            rsaKeyId = rsaKeyId,
            certificateProfileId = certificateProfileId,
            aidsJson = json.encodeToString(aids),
            cvmListJson = json.encodeToString(cvmList),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
