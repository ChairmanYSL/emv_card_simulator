package com.szzt.cardsimulator.emv.model

/**
 * EMV certificate chain for offline data authentication (SDA/DDA/CDA).
 *
 * Hierarchy:
 *   CA Public Key → Issuer Public Key Certificate → ICC Public Key Certificate → ICC Private Key
 */
data class Certificates(
    /** Certificate Authority public key (RSA public key modulus + exponent, DER-encoded). */
    val caPublicKey: ByteArray,
    /** CA public key exponent (if separate from key blob). */
    val caPublicKeyExponent: ByteArray? = null,
    /** Issuer Public Key Certificate as returned via EMV Tag 0x90. */
    val issuerCertificate: ByteArray,
    /** ICC Public Key Certificate as returned via EMV Tag 0x9F46. */
    val iccCertificate: ByteArray,
    /** ICC public key exponent (Tag 0x9F47) — may be embedded in ICC cert. */
    val iccPublicKeyExponent: ByteArray? = null,
    /** ICC public key remainder (Tag 0x9F48) for large RSA keys. */
    val iccPublicKeyRemainder: ByteArray? = null,
    /** ICC private key (RSA private key, PKCS#1 DER-encoded). */
    val iccPrivateKey: ByteArray,
    /** PAN used for certificate recovery validation. */
    val pan: String? = null,
    /** PAN sequence number. */
    val panSequenceNumber: Int = 1
)
