package com.szzt.cardsimulator.di

import org.koin.core.module.Module

/**
 * Aggregate module for Koin DI.
 * Import this single module in [CardSimulatorApp.startKoin].
 */
val appModule: List<Module> = listOf(
    hceModule,
    emvModule,
    profileModule,
    keyMgmtModule,
    logModule,
    viewModelModule
)
