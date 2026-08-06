package com.szzt.cardsimulator.di

import com.szzt.cardsimulator.hce.api.HceRouter
import com.szzt.cardsimulator.hce.impl.DefaultHceRouter
import org.koin.dsl.module

val hceModule = module {
    single<HceRouter> { DefaultHceRouter(get()) }
}
