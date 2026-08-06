package com.szzt.cardsimulator.profile.model

import kotlinx.serialization.Serializable

/**
 * Cardholder Verification Method entry.
 *
 * Each entry defines a CVM type, condition, and whether it applies
 * for the current transaction.
 */
@Serializable
data class CvmEntry(
    /** CVM code (e.g., 0x41 = Plaintext PIN, 0x42 = Enciphered PIN, 0x5E = Signature). */
    val cvmCode: Int,
    /** CVM condition code. */
    val cvmCondition: Int,
    /** Whether this CVM applies if succeeding CVM fails (1 = no, 2 = yes). */
    val applySucceedingCvm: Int = 1
)
