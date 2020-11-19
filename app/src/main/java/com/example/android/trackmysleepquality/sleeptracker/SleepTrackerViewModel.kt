/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(

        // we need Application access, to access Strings and other resources
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    private var viewModelJob = Job()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private var tonight = MutableLiveData<SleepNight?>()

    val nights = database.getAllNights()

    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    val startButtonVisible = Transformations.map(tonight) {
        null == it // if tonight is null, the start button is visible
    }

    val stopButtonVisible = Transformations.map(tonight) {
        null != it // stop button is visible, if tonight exists
    }

    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty() // clear button is visible if there are nights to clear
    }

    private var _showSnackbarEvent = MutableLiveData<Boolean>();
    val showSnackbarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>();
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    private val _navigateToSleepDataQuality = MutableLiveData<Long>();
    val navigateToSleepDataQuality: LiveData<Long>
        get() = _navigateToSleepDataQuality

    fun onSleepNightClicked(id: Long) {
        _navigateToSleepDataQuality.value = id
    }

    fun onSleepDataQualityNavigated() {
        _navigateToSleepDataQuality.value = null
    }


    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        // we use coroutines, so we do not block the UI while waiting for the result
        // launch = without blocking the UI thread
        // executes immediately
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    /**
     *  Handling the case of the stopped app or forgotten recording,
     *  the start and end times will be the same.j
     *
     *  If the start time and end time are not the same, then we do not have an unfinished
     *  recording.
     */
    private suspend fun getTonightFromDatabase(): SleepNight? {
        var night = database.getTonight()

        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    /**
     * Executes when the START button is clicked.
     */
    fun onStartTracking() {
        // we use a coroutine, because everything will be time-consuming
        // UIscope - because we need it to update the UI with the result
        viewModelScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight) {
        return withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }

    /**
     * Executes when the STOP button is clicked.
     */
    fun onStopTracking() {
        viewModelScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)
            _navigateToSleepQuality.value = oldNight
        }
    }

    private suspend fun update(night: SleepNight) {
        database.update(night)
    }

    fun onClear() {
        viewModelScope.launch {
            clear()
            tonight.value = null
        }
    }

    /**
     * Executes when the CLEAR button is clicked.
     */
    private suspend fun clear() {
        _showSnackbarEvent.value = true
            database.clear()
    }
}