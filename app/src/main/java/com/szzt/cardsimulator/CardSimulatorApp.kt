package com.szzt.cardsimulator

import android.app.Application
import com.szzt.cardsimulator.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class CardSimulatorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (com.szzt.cardsimulator.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Koin DI
        startKoin {
            androidContext(this@CardSimulatorApp)
            modules(appModule)
        }
    }
}
