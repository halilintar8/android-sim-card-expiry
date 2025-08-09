package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.myapplication.data.SimCard
import com.example.myapplication.data.SimCardDatabase
import com.example.myapplication.repository.SimCardRepository
import kotlinx.coroutines.launch

class SimCardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SimCardRepository

    val allSimCards: LiveData<List<SimCard>>

    init {
        val dao = SimCardDatabase.getDatabase(application).simCardDao()
        repository = SimCardRepository(dao)
        allSimCards = repository.allSimCards
    }

    fun insert(simCard: SimCard) = viewModelScope.launch { repository.insert(simCard) }
    fun update(simCard: SimCard) = viewModelScope.launch { repository.update(simCard) }
    fun delete(simCard: SimCard) = viewModelScope.launch { repository.delete(simCard) }
}

