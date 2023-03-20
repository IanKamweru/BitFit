package com.example.bitfit

import java.text.SimpleDateFormat
import java.util.*

data class SleepEntry(
    val hoursSlept: Float,
    val feeling: Float,
    val notes: String?,
    val date: String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
)
