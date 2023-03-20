package com.example.bitfit

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bitfit.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*

private const val TAG = "MainActivity/"

class MainActivity : AppCompatActivity() {
    private val sleepEntries = mutableListOf<SleepEntry>()
    private lateinit var  sleepEntryDAO: SleepEntryDao
    private lateinit var sleepEntriesRecyclerView: RecyclerView
    private lateinit var averageHoursTextView: TextView
    private lateinit var averageFeelingRatingBar: RatingBar
    private lateinit var binding: ActivityMainBinding
    private lateinit var summaryActivityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // set daily reminder
        setSleepReminderAlarm(this)

        // set sleep summary activity launcher
        sleepEntryDAO = (application as SleepApplication).db.sleepDao()
        summaryActivityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch(IO) {
                    val averageSleepTime = sleepEntryDAO.getAverageSleepTime()
                    val averageFeeling = sleepEntryDAO.getAverageFeeling()
                    averageHoursTextView.text = "Average sleep time: ${String.format("%.1f", averageSleepTime)}"
                    averageFeelingRatingBar.rating = averageFeeling
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sleepEntriesRecyclerView = findViewById(R.id.sleepEntriesRv)

        // get views
        val sleepSeekBar = findViewById<SeekBar>(R.id.sleepSeekBar)
        val sleepSeekBarTooltip = findViewById<View>(R.id.sleepSeekBarTooltip)
        setSeekBarTooltip(sleepSeekBar, sleepSeekBarTooltip)
        val feelingRatingBar = findViewById<RatingBar>(R.id.feelRatingBar)
        val notesEditText = findViewById<EditText>(R.id.notesEditText)
        averageHoursTextView = findViewById<TextView>(R.id.average_sleep_time)
        averageFeelingRatingBar = findViewById<RatingBar>(R.id.average_feeling_value)

        // Update the average hours text view and average feeling rating
        lifecycleScope.launch(IO) {
            averageHoursTextView.text =
                    "Average sleep time: ${String.format("%.1f", sleepEntryDAO.getAverageSleepTime())}"
            averageFeelingRatingBar.rating = sleepEntryDAO.getAverageFeeling()
        }

        val sleepEntryAdapter = SleepEntryAdapter(this, sleepEntries)

        // Listen to any changes to items in the database
        lifecycleScope.launch {
            sleepEntryDAO = (application as SleepApplication).db.sleepDao()

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
                }
            }
        }
        sleepEntriesRecyclerView.adapter = sleepEntryAdapter

        sleepEntriesRecyclerView.layoutManager = LinearLayoutManager(this).also {
            val dividerItemDecoration = DividerItemDecoration(this, it.orientation)
            sleepEntriesRecyclerView.addItemDecoration(dividerItemDecoration)
        }

        // save button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            // get values
            val hours = sleepSeekBar.progress.div(10f)
            val feeling = feelingRatingBar.rating
            val notes = notesEditText.text.toString()

            Log.i(TAG, "$hours $feeling $notes")
            lifecycleScope.launch(IO) {
                // create sleep entry
                val sleepEntry = SleepEntry(hours, feeling, notes)
                sleepEntryDAO = (application as SleepApplication).db.sleepDao()
                sleepEntryDAO.insert(SleepEntity(
                    hoursSlept = sleepEntry.hoursSlept,
                    feeling = sleepEntry.feeling,
                    notes = sleepEntry.notes,
                    date = sleepEntry.date
                ))
                val averageSleepTime = sleepEntryDAO.getAverageSleepTime()
                val averageFeeling = sleepEntryDAO.getAverageFeeling()
                averageHoursTextView.text = "Average sleep time: ${String.format("%.1f", averageSleepTime)}"
                averageFeelingRatingBar.rating = averageFeeling
                
                val intent = Intent(this@MainActivity, SleepSummaryActivity::class.java)
                intent.putExtra("averageSleepTime", averageSleepTime)
                intent.putExtra("averageFeeling", averageFeeling)
                summaryActivityLauncher.launch(intent)
            }

            // Clear views
            sleepSeekBar.progress = 0
            feelingRatingBar.rating = 0.0f
            notesEditText.text.clear()

            // Hide the keyboard
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(notesEditText.windowToken, 0)
        }
    }

    private fun setSleepReminderAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SleepDataReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 22)
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
    }

    private fun setSeekBarTooltip(seekBar: SeekBar, tooltip: View) {
        val tooltipTextView = tooltip.findViewById<TextView>(R.id.tooltipText)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Update the tooltip with the current progress
                tooltipTextView.text = (progress/10f).toString()
                val thumbX = seekBar.paddingLeft + (seekBar.width - seekBar.paddingLeft - seekBar.paddingRight) * progress / seekBar.max + tooltip.width / 2
                tooltip.x = thumbX.toFloat() - tooltip.width / 2
                tooltip.y = seekBar.y - tooltip.height
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Show the tooltip when the user starts touching the SeekBar
                tooltip.visibility = View.VISIBLE
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Hide the tooltip when the user stops touching the SeekBar
                tooltip.visibility = View.GONE
            }
        })
    }
}