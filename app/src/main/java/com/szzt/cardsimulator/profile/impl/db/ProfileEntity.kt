package com.szzt.cardsimulator.profile.impl.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for card profiles.
 *
 * Complex nested fields (aids, cvmList, etc.) are serialized to JSON columns
 * using Room TypeConverters.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val network: String,
    val isActive: Boolean,
    val pan: String,
    val panSequenceNumber: Int,
    val applicationExpirationDate: String,
    val cardholderName: String,
    val track2EquivalentData: String,
    val applicationLabel: String,
    val applicationVersionNumber: String,

    // Complex fields stored as JSON strings
    val aidsJson: String,
    val cvmListJson: String,

    // GPO
    val aip: String,
    val afl: String,

    // Issuer
    val issuerApplicationData: String,
    val issuerCountryCode: String,

    // DOLs
    val pdol: String,
    val cdol1: String,
    val cdol2: String,
    val tdol: String,
    val ddol: String,

    // Key references
    val symmetricKeyId: String,
    val rsaKeyId: String,
    val certificateProfileId: String,

    // Metadata
    val createdAt: Long,
    val updatedAt: Long
)
