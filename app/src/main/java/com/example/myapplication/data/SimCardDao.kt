package com.example.myapplication.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SimCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(simCard: SimCard)

    @Update
    suspend fun update(simCard: SimCard)

    @Delete
    suspend fun delete(simCard: SimCard)

    @Query("SELECT * FROM sim_cards ORDER BY id ASC")
    fun getAllSimCards(): LiveData<List<SimCard>>
}

