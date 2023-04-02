package com.example.bitfit

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SummaryFragment/"

class SummaryFragment : Fragment() {

    private lateinit var averageSleepTimeTextView: TextView
    private lateinit var averageFeelingRatingBar: RatingBar
    private lateinit var chart: CombinedChart
    private lateinit var sleepEntriesRecyclerView: RecyclerView
    private lateinit var sleepEntryAdapter: SleepEntryAdapter
    private lateinit var sleepEntryDao: SleepEntryDao
    private val sleepEntries = mutableListOf<SleepEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_summary, container, false)

        // Get the views
        averageSleepTimeTextView = view.findViewById(R.id.average_sleep_timeTv)
        averageFeelingRatingBar = view.findViewById(R.id.average_feelingRb)
        sleepEntriesRecyclerView = view.findViewById(R.id.sleepEntriesSummaryRv)

        sleepEntryDao = (requireActivity().application as SleepApplication).db.sleepDao()
        sleepEntryAdapter = SleepEntryAdapter(view.context, sleepEntries)
        sleepEntriesRecyclerView.adapter = sleepEntryAdapter
        sleepEntriesRecyclerView.layoutManager = LinearLayoutManager(view.context).also {
            val dividerItemDecoration = DividerItemDecoration(view.context, it.orientation)
            sleepEntriesRecyclerView.addItemDecoration(dividerItemDecoration)
        }
        chart = view.findViewById(R.id.combinedChart)
        chart.description.isEnabled = false

        fetchEntries()
        updateAvg()

        requireActivity().setResult(Activity.RESULT_OK, null)

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(): SummaryFragment {
            return SummaryFragment()
        }
    }

    private fun fetchEntries() {
        // Listen to any changes to items in the database
        lifecycleScope.launch(IO) {
            sleepEntryDao.getAll().collect { dbList ->
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

                    withContext(Dispatchers.Main) {
                        // Update the chart data and refresh it
                        val entries = ArrayList<Entry>()
                        val values = ArrayList<BarEntry>()

                        // Populate entries and values arrays with data from sleep entries
                        val reversedSleepEntries = sleepEntries.asReversed()
                        for ((index, sleepEntry) in reversedSleepEntries.withIndex()) {
                            entries.add(Entry(index.toFloat(), sleepEntry.hoursSlept))
                            values.add(BarEntry(index.toFloat(), sleepEntry.feeling))
                        }

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
        }
    }


    private fun updateAvg() {
        lifecycleScope.launch(Dispatchers.Main) {
            val averageSleepTimeDeferred = async(Dispatchers.IO) {
                sleepEntryDao.getAverageSleepTime()
            }
            val averageFeelingDeferred = async(Dispatchers.IO) {
                sleepEntryDao.getAverageFeeling()
            }
            val averageSleepTime = averageSleepTimeDeferred.await()
            val averageFeeling = averageFeelingDeferred.await()

            averageSleepTimeTextView.text = "Average sleep time: ${String.format("%.1f", averageSleepTime)}"
            averageFeelingRatingBar.rating = averageFeeling
            Log.i(TAG, "avg sleep: $averageSleepTime avg feeling: $averageFeeling")
        }
    }
}