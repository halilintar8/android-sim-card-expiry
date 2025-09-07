package com.halilintar8.simexpiry.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a SIM card record in the database.
 *
 * Each SIM card stores:
 * - name (e.g., provider or alias)
 * - simCardNumber (SIM number/identifier)
 * - expiredDate (in ISO format: YYYY-MM-DD)
 */
@Entity(
    tableName = "sim_cards",
    indices = [Index(value = ["sim_card_number"], unique = true)]
)
data class SimCard(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sim_card_number")
    val simCardNumber: String,

    @ColumnInfo(name = "expired_date")
    val expiredDate: String
)
