package com.halilintar8.simexpiry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sim_cards")
data class SimCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val simCardNumber: String,
    val expiredDate: String
)
