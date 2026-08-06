package com.szzt.cardsimulator.di

import com.szzt.cardsimulator.ui.profile.ProfileViewModel
import com.szzt.cardsimulator.ui.log.LogViewModel
import com.szzt.cardsimulator.ui.settings.SettingsViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory { ProfileViewModel(get()) }
    factory { LogViewModel(get()) }
    factory { SettingsViewModel(get(), get()) }
}
