
package com.example.buttonmapper.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buttonmapper.KeyMapping
import com.example.buttonmapper.ScheduledAlarm
import com.example.buttonmapper.AlarmScheduler
import com.example.buttonmapper.data.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState
  data class Error(val throwable: Throwable) : MainScreenUiState
  data class Success(
    val keyMappings: List<KeyMapping>,
    val scheduledAlarms: List<ScheduledAlarm>
  ) : MainScreenUiState
}

class MainScreenViewModel(
  private val dataRepository: DataRepository = com.example.buttonmapper.data.DefaultDataRepository()
) : ViewModel() {
  private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
  val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

  private val _logMessages = MutableStateFlow<List<String>>(listOf("ViewModel created"))
  val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

  fun loadData(context: Context) {
    appendLog("loadData called")
    viewModelScope.launch {
      try {
        appendLog("Getting key mappings flow...")
        val mappingsFlow = dataRepository.getKeyMappings(context)
        appendLog("Getting scheduled alarms flow...")
        val alarmsFlow = dataRepository.getScheduledAlarms(context)
        appendLog("Combining flows...")
        combine(mappingsFlow, alarmsFlow) { mList, aList ->
          appendLog("combine emitted: ${'$'}mList, ${'$'}aList")
          MainScreenUiState.Success(mList, aList)
        }.collect { state ->
          appendLog("collect: ${'$'}state")
          _uiState.value = state
        }
      } catch (e: Exception) {
        _uiState.value = MainScreenUiState.Error(e)
        appendLog("Error: ${'$'}{e.message}")
      }
    }
  }

  fun appendLog(message: String) {
    _logMessages.update { it + message }
  }

  fun addKeyMapping(context: Context, mapping: KeyMapping) {
    viewModelScope.launch {
      val current = dataRepository.getKeyMappings(context).first()
      // Remove any existing mapping for this keyCode
      val filtered = current.filter { it.keyCode != mapping.keyCode }
      val updated = filtered + mapping
      dataRepository.setKeyMappings(context, updated)
      appendLog("Added/overridden key mapping: $mapping")
      loadData(context)
    }
  }

  fun removeKeyMapping(context: Context, keyCode: Int) {
    viewModelScope.launch {
      val current = dataRepository.getKeyMappings(context).first()
      val updated = current.filter { it.keyCode != keyCode }
      dataRepository.setKeyMappings(context, updated)
      appendLog("Removed key mapping for KeyCode: $keyCode")
      loadData(context)
    }
  }

  fun addScheduledAlarm(context: Context, alarm: ScheduledAlarm) {
    viewModelScope.launch {
      val current = dataRepository.getScheduledAlarms(context).first()
      // Filter out any alarm with the same ID to prevent duplication
      val filtered = current.filter { it.id != alarm.id }
      // Cancel previous system alarm if it exists
      AlarmScheduler.cancelAlarm(context, alarm.id)
      
      val updated = filtered + alarm
      dataRepository.setScheduledAlarms(context, updated)
      
      // Schedule the new alarm in system
      AlarmScheduler.scheduleAlarm(context, alarm)
      
      appendLog("Added scheduled alarm: $alarm")
      loadData(context)
    }
  }

  fun removeScheduledAlarm(context: Context, alarmId: String) {
    viewModelScope.launch {
      val current = dataRepository.getScheduledAlarms(context).first()
      val updated = current.filter { it.id != alarmId }
      dataRepository.setScheduledAlarms(context, updated)
      
      // Cancel the system alarm
      AlarmScheduler.cancelAlarm(context, alarmId)
      
      appendLog("Removed scheduled alarm: $alarmId")
      loadData(context)
    }
  }

  fun saveKeyMappings(context: Context, mappings: List<KeyMapping>) {
    dataRepository.setKeyMappings(context, mappings)
    loadData(context)
  }

  fun saveScheduledAlarms(context: Context, alarms: List<ScheduledAlarm>) {
    dataRepository.setScheduledAlarms(context, alarms)
    loadData(context)
  }
}
