package com.szzt.cardsimulator.profile.model

import kotlinx.serialization.Serializable

/**
 * Key material reference used by a card profile.
 *
 * Points to keys stored in the KeyStore by ID.
 */
@Serializable
data class KeyMaterial(
    /** Identifier for the symmetric key (3DES/AES) in KeyStore. */
    val symmetricKeyId: String,
    /** Identifier for the RSA key pair in KeyStore. */
    val rsaKeyId: String,
    /** Identifier for the certificate chain. */
    val certificateProfileId: String
)
