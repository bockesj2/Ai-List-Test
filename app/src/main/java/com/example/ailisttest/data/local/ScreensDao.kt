package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreensDao {
    @Query("SELECT * FROM Screens ORDER BY Type ASC, id ASC")
    fun getAllScreens(): Flow<List<Screens>>

    @Query("SELECT * FROM Screens ORDER BY Type ASC, id ASC")
    suspend fun getAllScreensList(): List<Screens>

    @Query("SELECT * FROM Screens WHERE id = :id")
    suspend fun getScreenById(id: Long): Screens?

    @Query("SELECT * FROM Screens WHERE Type = :type ORDER BY Type ASC, id ASC")
    fun getScreensByType(type: Int): Flow<List<Screens>>

    @Transaction
    @Query("SELECT * FROM Screens ORDER BY Type ASC, id ASC")
    fun getListScreensWithItems(): Flow<List<ScreenWithListItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreen(screen: Screens): Long

    @Update
    suspend fun updateScreen(screen: Screens)

    @Delete
    suspend fun deleteScreen(screen: Screens)

    @Query("DELETE FROM Screens")
    suspend fun deleteAllScreens()
}
