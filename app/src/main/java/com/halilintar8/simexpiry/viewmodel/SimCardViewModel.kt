package com.halilintar8.simexpiry.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.halilintar8.simexpiry.data.SimCard
import com.halilintar8.simexpiry.data.SimCardDatabase
import com.halilintar8.simexpiry.repository.SimCardRepository
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

