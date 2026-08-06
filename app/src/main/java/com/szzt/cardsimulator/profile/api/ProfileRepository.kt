package com.szzt.cardsimulator.profile.api

import com.szzt.cardsimulator.profile.model.CardProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for card profile CRUD operations.
 * The active profile is used by the EMV kernel and HCE service.
 */
interface ProfileRepository {

    /** Observe all profiles (ordered by last modified). */
    fun observeAll(): Flow<List<CardProfile>>

    /** Get a single profile by ID. */
    suspend fun getById(id: String): CardProfile?

    /** Get the currently active (emulating) profile. */
    suspend fun getActive(): CardProfile?

    /** Observe the currently active profile. */
    fun observeActive(): Flow<CardProfile?>

    /** Insert or update a profile. */
    suspend fun save(profile: CardProfile)

    /** Delete a profile by ID. */
    suspend fun delete(id: String)

    /** Set a profile as the active (emulating) one. Deactivates others. */
    suspend fun setActive(id: String)

    /** Import a profile from JSON. */
    suspend fun importFromJson(json: String): CardProfile

    /** Export the active profile to JSON. */
    suspend fun exportToJson(id: String): String
}
