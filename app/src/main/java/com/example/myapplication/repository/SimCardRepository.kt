package com.example.myapplication.repository

import androidx.lifecycle.LiveData
import com.example.myapplication.data.SimCard
import com.example.myapplication.data.SimCardDao

class SimCardRepository(private val simCardDao: SimCardDao) {
    val allSimCards: LiveData<List<SimCard>> = simCardDao.getAllSimCards()

    suspend fun insert(simCard: SimCard) = simCardDao.insert(simCard)
    suspend fun update(simCard: SimCard) = simCardDao.update(simCard)
    suspend fun delete(simCard: SimCard) = simCardDao.delete(simCard)
}

