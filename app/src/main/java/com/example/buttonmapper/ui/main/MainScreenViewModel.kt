
package com.example.buttonmapper.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buttonmapper.KeyMapping
import com.example.buttonmapper.ScheduledTask
import com.example.buttonmapper.ScheduleScheduler
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
    val scheduledTasks: List<ScheduledTask>
  ) : MainScreenUiState
}

class MainScreenViewModel(
  private val dataRepository: DataRepository = com.example.buttonmapper.data.DefaultDataRepository()
) : ViewModel() {
  private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
  val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

  private val _installedApps = MutableStateFlow<List<Pair<String, String>>>(emptyList())
  val installedApps: StateFlow<List<Pair<String, String>>> = _installedApps.asStateFlow()

  private val _logMessages = MutableStateFlow<List<String>>(listOf("ViewModel created"))
  val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

  fun loadData(context: Context) {
    appendLog("loadData called")
    ScheduleScheduler.validateAndExecute(context)
    ScheduleScheduler.startHourlyValidation(context)

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(0).mapNotNull { pkg ->
          val appInfo = pkg.applicationInfo ?: return@mapNotNull null
          val appLabel = appInfo.loadLabel(pm).toString()
          appLabel to pkg.packageName
        }
        .filter { it.second != context.packageName }
        .distinctBy { it.second }
        .sortedBy { it.first.lowercase() }

        _installedApps.value = apps
        appendLog("Loaded ${apps.size} installed apps asynchronously on IO thread")
      } catch (e: Exception) {
        appendLog("Error loading installed apps: ${e.message}")
      }
    }

    viewModelScope.launch {
      try {
        appendLog("Getting key mappings flow...")
        val mappingsFlow = dataRepository.getKeyMappings(context)
        appendLog("Getting scheduled tasks flow...")
        val tasksFlow = dataRepository.getScheduledTasks(context)
        appendLog("Combining flows...")
        combine(mappingsFlow, tasksFlow) { mList, tList ->
          appendLog("combine emitted: $mList, $tList")
          MainScreenUiState.Success(mList, tList)
        }.collect { state ->
          appendLog("collect: $state")
          _uiState.value = state
        }
      } catch (e: Exception) {
        _uiState.value = MainScreenUiState.Error(e)
        appendLog("Error: ${e.message}")
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

  fun addScheduledTask(context: Context, task: ScheduledTask) {
    viewModelScope.launch {
      val current = dataRepository.getScheduledTasks(context).first()
      // Filter out any task with the same ID to prevent duplication
      val filtered = current.filter { it.id != task.id }
      val updated = filtered + task
      dataRepository.setScheduledTasks(context, updated)
      
      appendLog("Added scheduled task: $task")
      ScheduleScheduler.validateAndExecute(context)
      loadData(context)
    }
  }

  fun removeScheduledTask(context: Context, taskId: String) {
    viewModelScope.launch {
      val current = dataRepository.getScheduledTasks(context).first()
      val updated = current.filter { it.id != taskId }
      dataRepository.setScheduledTasks(context, updated)
      
      appendLog("Removed scheduled task: $taskId")
      ScheduleScheduler.validateAndExecute(context)
      loadData(context)
    }
  }

  fun saveKeyMappings(context: Context, mappings: List<KeyMapping>) {
    dataRepository.setKeyMappings(context, mappings)
    loadData(context)
  }

  fun saveScheduledTasks(context: Context, tasks: List<ScheduledTask>) {
    dataRepository.setScheduledTasks(context, tasks)
    loadData(context)
  }
}
