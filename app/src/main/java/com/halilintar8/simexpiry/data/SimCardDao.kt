package com.halilintar8.simexpiry.data

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Data Access Object (DAO) for the SIM card database table.
 * Provides methods to insert, update, delete, and fetch SIM card records.
 */
@Dao
interface SimCardDao {

    /**
     * Insert a new SIM card or replace an existing one with the same ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(simCard: SimCard)

    /**
     * Update an existing SIM card entry.
     */
    @Update
    suspend fun update(simCard: SimCard)

    /**
     * Delete a SIM card entry.
     */
    @Delete
    suspend fun delete(simCard: SimCard)

    /**
     * Fetch all SIM cards ordered by ID for real-time UI updates.
     * Returns LiveData so UI can automatically observe changes.
     */
    @Query("SELECT * FROM sim_cards ORDER BY id ASC")
    fun getAllSimCards(): LiveData<List<SimCard>>

    /**
     * Fetch all SIM cards ordered by ID as a plain List (no LiveData).
     * Used for background tasks like Worker notifications.
     */
    @Query("SELECT * FROM sim_cards ORDER BY id ASC")
    suspend fun getAllSimCardsList(): List<SimCard>
}
