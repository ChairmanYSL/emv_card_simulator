package com.szzt.cardsimulator.di

import com.szzt.cardsimulator.emv.api.CertificateProvider
import com.szzt.cardsimulator.emv.api.CryptoEngine
import com.szzt.cardsimulator.emv.api.EmvKernel
import com.szzt.cardsimulator.emv.impl.DefaultCertificateProvider
import com.szzt.cardsimulator.emv.impl.DefaultCryptoEngine
import com.szzt.cardsimulator.emv.impl.DefaultEmvKernel
import org.koin.dsl.module

val emvModule = module {
    single<CryptoEngine> { DefaultCryptoEngine() }
    single<CertificateProvider> { DefaultCertificateProvider() }
    single<EmvKernel> { DefaultEmvKernel(get(), get()) }
}
