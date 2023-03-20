package com.example.bitfit

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_table")
data class SleepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "hours") val hoursSlept: Float,
    @ColumnInfo(name = "feeling") val feeling: Float,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "date") val date: String
)