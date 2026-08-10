package com.szzt.cardsimulator.profile.impl.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [ProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ProfileDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ProfileDatabase? = null

        /**
         * Schema migrations, ordered by from-version.
         *
         * Version 1 is the initial schema (no migrations yet). Every future
         * schema change MUST add a [Migration] here — never re-enable
         * destructive migration, which silently wipes user data on upgrade.
         */
        private val MIGRATIONS: Array<Migration> = arrayOf()

        fun getInstance(context: Context): ProfileDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ProfileDatabase::class.java,
                    "card_simulator.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
