package com.halilintar8.simexpiry.data

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Data Access Object (DAO) for SIM card records.
 *
 * Provides database operations such as insert, update, delete, and queries.
 * Supports both LiveData (for reactive UI) and suspend functions (for background tasks).
 */
@Dao
interface SimCardDao {

    // --- Insert & Update ---

    /**
     * Insert a new SIM card into the database.
     * If a record with the same ID exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(simCard: SimCard)

    /**
     * Update an existing SIM card entry.
     */
    @Update
    suspend fun update(simCard: SimCard)

    // --- Delete ---

    /**
     * Delete a SIM card entry.
     */
    @Delete
    suspend fun delete(simCard: SimCard)

    // --- Queries ---

    /**
     * Fetch all SIM cards ordered by ID.
     * Returns LiveData for real-time UI observation.
     */
    @Query("SELECT * FROM sim_cards ORDER BY id ASC")
    fun getAllSimCards(): LiveData<List<SimCard>>

    /**
     * Fetch all SIM cards ordered by ID as a plain List.
     * Suitable for background tasks like Workers, Alarms, or Receivers.
     */
    @Query("SELECT * FROM sim_cards ORDER BY id ASC")
    suspend fun getAllSimCardsList(): List<SimCard>

    /**
     * Fetch a SIM card by its unique ID.
     */
    @Query("SELECT * FROM sim_cards WHERE id = :id LIMIT 1")
    suspend fun getSimCardById(id: Int): SimCard?

    /**
     * Delete all SIM card records.
     */
    @Query("DELETE FROM sim_cards")
    suspend fun clearAll()
}
