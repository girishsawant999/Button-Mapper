package com.example.buttonmapper.data


import android.content.Context
import com.example.buttonmapper.KeyMapping
import com.example.buttonmapper.StorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


interface DataRepository {
  fun getKeyMappings(context: Context): Flow<List<KeyMapping>>
  fun setKeyMappings(context: Context, mappings: List<KeyMapping>)
}

class DefaultDataRepository : DataRepository {
  override fun getKeyMappings(context: Context): Flow<List<KeyMapping>> = flow {
    emit(StorageHelper.loadKeyMappings(context))
  }

  override fun setKeyMappings(context: Context, mappings: List<KeyMapping>) {
    StorageHelper.saveKeyMappings(context, mappings)
  }
}
