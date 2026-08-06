package com.szzt.cardsimulator.di

import com.szzt.cardsimulator.keymgmt.api.KeyStore
import com.szzt.cardsimulator.keymgmt.api.KeyImporter
import com.szzt.cardsimulator.keymgmt.impl.FileBasedKeyStore
import com.szzt.cardsimulator.keymgmt.impl.JsonKeyImporter
import org.koin.dsl.module

val keyMgmtModule = module {
    single<KeyStore> { FileBasedKeyStore(get()) }
    single<KeyImporter> { JsonKeyImporter(get(), get()) }
}
