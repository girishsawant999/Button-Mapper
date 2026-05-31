package com.example.buttonmapper.data


import android.content.Context
import com.example.buttonmapper.KeyMapping
import com.example.buttonmapper.ScheduledTask
import com.example.buttonmapper.StorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


interface DataRepository {
  fun getKeyMappings(context: Context): Flow<List<KeyMapping>>
  fun setKeyMappings(context: Context, mappings: List<KeyMapping>)
  fun getScheduledTasks(context: Context): Flow<List<ScheduledTask>>
  fun setScheduledTasks(context: Context, tasks: List<ScheduledTask>)
}

class DefaultDataRepository : DataRepository {
  override fun getKeyMappings(context: Context): Flow<List<KeyMapping>> = flow {
    emit(StorageHelper.loadKeyMappings(context))
  }

  override fun setKeyMappings(context: Context, mappings: List<KeyMapping>) {
    StorageHelper.saveKeyMappings(context, mappings)
  }

  override fun getScheduledTasks(context: Context): Flow<List<ScheduledTask>> = flow {
    emit(StorageHelper.loadScheduledTasks(context))
  }

  override fun setScheduledTasks(context: Context, tasks: List<ScheduledTask>) {
    StorageHelper.saveScheduledTasks(context, tasks)
  }
}
