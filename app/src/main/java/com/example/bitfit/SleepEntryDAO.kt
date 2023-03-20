package com.example.bitfit

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepEntryDao {
    @Query("SELECT * FROM sleep_table ORDER BY id DESC")
    fun getAll(): Flow<List<SleepEntity>>

    @Insert
    fun insert(sleepEntity: SleepEntity)

    @Query("DELETE FROM sleep_table WHERE date=:date AND feeling=:feeling AND hours=:hours AND notes=:notes")
    fun deleteSleepEntity(date: String, feeling: Float, hours: Float, notes: String)

    @Query("DELETE FROM sleep_table")
    fun deleteAll()

    @Query("SELECT AVG(hours) FROM sleep_table")
    fun getAverageSleepTime(): Float

    @Query("SELECT AVG(feeling) FROM sleep_table")
    fun getAverageFeeling(): Float
}