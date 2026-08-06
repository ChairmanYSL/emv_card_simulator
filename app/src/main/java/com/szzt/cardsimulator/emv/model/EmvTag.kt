package com.szzt.cardsimulator.emv.model

/**
 * Common EMV Tag constants (BER-TLV tag values).
 */
object EmvTag {
    // Payment System Environment
    const val FCI_TEMPLATE = 0x6F
    const val DF_NAME = 0x84
    const val FCI_PROPRIETARY_TEMPLATE = 0xA5
    const val APPLICATION_LABEL = 0x50
    const val APPLICATION_PRIORITY = 0x87
    const val PDOL = 0x9F38
    const val FCI_ISSUER_DISCRETIONARY = 0xBF0C

    // Application Data
    const val AIP = 0x82
    const val AFL = 0x94
    const val APPLICATION_EXPIRATION_DATE = 0x5F24
    const val PAN = 0x5A
    const val TRACK2_EQ = 0x57
    const val CARDHOLDER_NAME = 0x5F20

    // Transaction Data
    const val AMOUNT_AUTHORIZED = 0x9F02
    const val AMOUNT_OTHER = 0x9F03
    const val TERMINAL_COUNTRY_CODE = 0x9F1A
    const val TERMINAL_VERIFICATION_RESULTS = 0x95
    const val TRANSACTION_CURRENCY_CODE = 0x5F2A
    const val TRANSACTION_DATE = 0x9A
    const val TRANSACTION_TYPE = 0x9C
    const val UNPREDICTABLE_NUMBER = 0x9F37
    const val TERMINAL_TYPE = 0x9F35
    const val INTERFACE_DEVICE_SERIAL = 0x9F1E

    // Cryptogram
    const val CRYPTOGRAM = 0x9F26
    const val APPLICATION_CRYPTOGRAM = 0x9F26
    const val CRYPTOGRAM_INFORMATION_DATA = 0x9F27
    const val ATC = 0x9F36
    const val CVR = 0x9F27

    // Issuer Data
    const val ISSUER_APPLICATION_DATA = 0x9F10
    const val APPLICATION_VERSION_NUMBER = 0x9F08
    const val IAC_DEFAULT = 0x9F0D
    const val IAC_DENIAL = 0x9F0E
    const val IAC_ONLINE = 0x9F0F

    // CDOL
    const val CDOL1 = 0x8C
    const val CDOL2 = 0x8D
    const val TDOL = 0x97
    const val DDOL = 0x9F49

    // CVM
    const val CVM_LIST = 0x8E
    const val ISSUER_COUNTRY_CODE = 0x5F28

    // ODA (Offline Data Authentication)
    const val CA_PUBLIC_KEY_INDEX = 0x8F
    const val ISSUER_PUBLIC_KEY_CERTIFICATE = 0x90
    const val ICC_PUBLIC_KEY_CERTIFICATE = 0x9F46
    const val ICC_PUBLIC_KEY_EXPONENT = 0x9F47
    const val ICC_PUBLIC_KEY_REMAINDER = 0x9F48
    const val SDA_DATA = 0x93
    const val DDA_DATA = 0x9F4B

    // Additional Check Values
    const val APPLICATION_INTERCHANGE_PROFILE = 0x82
    const val ISSUER_PUBLIC_KEY_REMAINDER = 0x92
    const val SIGNED_DYNAMIC_APPLICATION_DATA = 0x9F4B
    const val ISSUER_AUTHENTICATION_DATA = 0x91
}
