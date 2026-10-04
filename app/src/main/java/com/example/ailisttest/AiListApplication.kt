package com.example.ailisttest

import android.app.Application
import com.example.ailisttest.data.MainRepository
import com.example.ailisttest.data.UserPreferences
import com.example.ailisttest.data.local.AppDatabase

class AiListApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferences by lazy { UserPreferences(this) }
    val repository by lazy { 
        MainRepository(
            database.nodeDao(),
            database.packetDao(),
            database.tagDao(),
            database.dataTypeDao(),
            database.bitTagDao(),
            database.screensDao(),
            database.listScreenItemsDao(),
            database.graphicsScreenItemsDao(),
            database.customGroupDao(),
            database.headerItemDao(),
            database.tagListItemDao(),
            database.internalTagDao(),
            database.internalTagGroupDao(),
            userPreferences
        ) 
    }
}
