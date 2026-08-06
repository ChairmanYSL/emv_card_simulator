package com.szzt.cardsimulator.di

import com.szzt.cardsimulator.log.api.ApduLogger
import com.szzt.cardsimulator.log.impl.InMemoryApduLogger
import org.koin.dsl.module

val logModule = module {
    single<ApduLogger> { InMemoryApduLogger() }
}
