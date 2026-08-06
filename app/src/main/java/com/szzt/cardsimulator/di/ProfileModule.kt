package com.szzt.cardsimulator.di

import com.szzt.cardsimulator.profile.api.ProfileRepository
import com.szzt.cardsimulator.profile.impl.RoomProfileRepository
import com.szzt.cardsimulator.profile.impl.db.ProfileDao
import com.szzt.cardsimulator.profile.impl.db.ProfileDatabase
import org.koin.dsl.module

val profileModule = module {
    single<ProfileDatabase> { ProfileDatabase.getInstance(get()) }
    single<ProfileDao> { get<ProfileDatabase>().profileDao() }
    single<ProfileRepository> { RoomProfileRepository(get()) }
}
