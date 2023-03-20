package com.example.bitfit

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.data.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

private const val TAG = "SleepSummaryActivity/"

class SleepSummaryActivity() : AppCompatActivity() {
    private lateinit var averageSleepTimeTextView: TextView
    private lateinit var averageFeelingRatingBar: RatingBar
    private lateinit var sleepEntriesRecyclerView: RecyclerView
    private var averageSleepTime by Delegates.notNull<Float>()
    private var averageFeeling by Delegates.notNull<Float>()
    private val sleepEntries = mutableListOf<SleepEntry>()

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep_summary)
        Log.i(TAG, "Starting sleep summary activity")

        // Get the average sleep time and average feeling rating from the intent extras
        averageSleepTime = intent.getFloatExtra("averageSleepTime", 0f)
        averageFeeling = intent.getFloatExtra("averageFeeling", 0f)

        // Get the views
        averageSleepTimeTextView = findViewById(R.id.average_sleep_timeTv)
        averageFeelingRatingBar = findViewById(R.id.average_feelingRb)
        sleepEntriesRecyclerView = findViewById(R.id.sleepEntriesSummaryRv)

        // Set the values of the views
        averageSleepTimeTextView.text = "Average sleep time: ${String.format("%.1f", averageSleepTime)}"
        averageFeelingRatingBar.rating = averageFeeling

        val sleepEntryAdapter = SleepEntryAdapter(this, sleepEntries)
        val sleepEntryDAO = (application as SleepApplication).db.sleepDao()
        val chart = findViewById<CombinedChart>(R.id.combinedChart)
        chart.description.isEnabled = false

        // Listen to any changes to items in the database
        lifecycleScope.launch(IO) {
            sleepEntryDAO.getAll().collect { dbList ->
                dbList.map { entity ->
                    SleepEntry(
                        entity.hoursSlept,
                        entity.feeling,
                        entity.notes,
                        entity.date
                    )
                }.also { mappedList ->
                    sleepEntries.clear()
                    sleepEntries.addAll(mappedList)
                    sleepEntryAdapter.notifyDataSetChanged()
                    averageSleepTime = sleepEntryDAO.getAverageSleepTime()
                    averageFeeling = sleepEntryDAO.getAverageFeeling()

                    // Set data
                    val entries = ArrayList<Entry>()
                    val values = ArrayList<BarEntry>()

                    // Populate entries and values arrays with data from sleep entries
                    val reversedSleepEntries = sleepEntries.asReversed()
                    for ((index, sleepEntry) in reversedSleepEntries.withIndex()) {
                        entries.add(Entry(index.toFloat(), sleepEntry.hoursSlept.toFloat()))
                        values.add(BarEntry(index.toFloat(), sleepEntry.feeling.toFloat()))
                    }

                    Log.i(TAG, "${entries.size}  ${values.size}")

                    // Create data sets
                    val lineDataSet = LineDataSet(entries, "Hours Slept")
                    val barDataSet = BarDataSet(values, "Feeling")

                    // Set colors for data sets
                    lineDataSet.color = Color.BLUE
                    lineDataSet.lineWidth = 3f
                    lineDataSet.valueTextSize = 12f
                    barDataSet.valueTextSize = 12f
                    barDataSet.color = Color.GREEN

                    // Create combined data set
                    val barData = BarData(barDataSet)
                    barData.barWidth = 0.5f
                    val data = CombinedData().apply {
                        setData(LineData(lineDataSet))
                        setData(barData)
                    }

                    // Set chart data and refresh
                    chart.data = data
                    chart.invalidate()
                }
            }
        }
        sleepEntriesRecyclerView.adapter = sleepEntryAdapter

        sleepEntriesRecyclerView.layoutManager = LinearLayoutManager(this).also {
            val dividerItemDecoration = DividerItemDecoration(this, it.orientation)
            sleepEntriesRecyclerView.addItemDecoration(dividerItemDecoration)
        }

        // Set the result data and finish the activity
        setResult(Activity.RESULT_OK, null)
    }
}