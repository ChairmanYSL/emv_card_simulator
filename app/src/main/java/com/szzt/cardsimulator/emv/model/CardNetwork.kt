package com.szzt.cardsimulator.emv.model

/**
 * Supported card payment networks.
 * Each network has slightly different EMV contactless entry-point behavior.
 */
enum class CardNetwork {
    VISA,
    MASTERCARD,
    AMEX,
    JCB,
    UNIONPAY;

    companion object {
        /**
         * Common PPSE application label mapping.
         */
        val defaultLabel: Map<CardNetwork, String> = mapOf(
            VISA to "Visa",
            MASTERCARD to "Mastercard",
            AMEX to "American Express",
            JCB to "JCB",
            UNIONPAY to "UnionPay"
        )
    }
}
