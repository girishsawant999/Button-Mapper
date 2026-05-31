package com.example.buttonmapper.data


import android.content.Context
import com.example.buttonmapper.KeyMapping
import com.example.buttonmapper.ScheduledAlarm
import com.example.buttonmapper.StorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


interface DataRepository {
  fun getKeyMappings(context: Context): Flow<List<KeyMapping>>
  fun setKeyMappings(context: Context, mappings: List<KeyMapping>)
  fun getScheduledAlarms(context: Context): Flow<List<ScheduledAlarm>>
  fun setScheduledAlarms(context: Context, alarms: List<ScheduledAlarm>)
}

class DefaultDataRepository : DataRepository {
  override fun getKeyMappings(context: Context): Flow<List<KeyMapping>> = flow {
    emit(StorageHelper.loadKeyMappings(context))
  }

  override fun setKeyMappings(context: Context, mappings: List<KeyMapping>) {
    StorageHelper.saveKeyMappings(context, mappings)
  }

  override fun getScheduledAlarms(context: Context): Flow<List<ScheduledAlarm>> = flow {
    emit(StorageHelper.loadScheduledAlarms(context))
  }

  override fun setScheduledAlarms(context: Context, alarms: List<ScheduledAlarm>) {
    StorageHelper.saveScheduledAlarms(context, alarms)
  }
}
