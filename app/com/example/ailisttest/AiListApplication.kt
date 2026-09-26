package com.example.ailisttest

import android.app.Application
import com.example.ailisttest.data.MainRepository
import com.example.ailisttest.data.local.AppDatabase

class AiListApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        MainRepository(
            database.nodeDao(),
            database.packetDao(),
            database.tagDao(),
            database.dataTypeDao()
        ) 
    }
}
