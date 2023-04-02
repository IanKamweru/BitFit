package com.example.bitfit

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HomeFragment/"

class HomeFragment : Fragment() {

    private lateinit var sleepSeekBar: SeekBar
    private lateinit var feelingRatingBar: RatingBar
    private lateinit var notesEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var averageSleepTimeTextView: TextView
    private lateinit var averageFeelingRatingBar: RatingBar
    private lateinit var sleepEntriesRecyclerView: RecyclerView
    private lateinit var sleepEntryAdapter: SleepEntryAdapter
    private lateinit var sleepEntryDao: SleepEntryDao
    private val sleepEntries = mutableListOf<SleepEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sleepEntryDao = (requireActivity().application as SleepApplication).db.sleepDao()
        sleepEntryAdapter = SleepEntryAdapter(requireContext(), sleepEntries)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fetchEntries()
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Get the views
        sleepEntriesRecyclerView = view.findViewById(R.id.sleepEntriesRv)
        sleepSeekBar = view.findViewById(R.id.sleepSeekBar)
        val sleepSeekBarTooltip = view.findViewById<View>(R.id.sleepSeekBarTooltip)
        setSeekBarTooltip(sleepSeekBar, sleepSeekBarTooltip)
        feelingRatingBar = view.findViewById(R.id.feelRatingBar)
        notesEditText = view.findViewById(R.id.notesEditText)
        averageSleepTimeTextView = view.findViewById(R.id.average_sleep_time)
        averageFeelingRatingBar = view.findViewById(R.id.average_feeling_value)
        saveButton = view.findViewById(R.id.saveButton)

        sleepEntriesRecyclerView.adapter = sleepEntryAdapter
        sleepEntriesRecyclerView.layoutManager = LinearLayoutManager(view.context).also {
            val dividerItemDecoration = DividerItemDecoration(view.context, it.orientation)
            sleepEntriesRecyclerView.addItemDecoration(dividerItemDecoration)
        }

        // set on click listener for save button
        setSaveListener()
        updateAvg()

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private fun fetchEntries() {
        // Listen to any changes to items in the database
        lifecycleScope.launch(IO) {
            sleepEntryDao.getAll().collect { dbList ->
                val mappedList = dbList.map { entity ->
                    SleepEntry(
                        entity.hoursSlept,
                        entity.feeling,
                        entity.notes,
                        entity.date
                    )
                }
                withContext(Main) {
                    sleepEntries.clear()
                    sleepEntries.addAll(mappedList)
                    sleepEntryAdapter.notifyDataSetChanged()
                }
            }
        }
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

    private fun setSaveListener() {
        saveButton.setOnClickListener {
            // get values
            val hours = sleepSeekBar.progress.div(10f)
            val feeling = feelingRatingBar.rating
            val notes = notesEditText.text.toString()

            Log.i(TAG, "$hours $feeling $notes")
            lifecycleScope.launch(IO) {
                // create sleep entry
                val sleepEntry = SleepEntry(hours, feeling, notes)
                sleepEntryDao.insert(SleepEntity(
                    hoursSlept = sleepEntry.hoursSlept,
                    feeling = sleepEntry.feeling,
                    notes = sleepEntry.notes,
                    date = sleepEntry.date
                ))
                // update views on the main thread
                lifecycleScope.launch(Dispatchers.Main) {
                    // Clear views
                    sleepSeekBar.progress = 0
                    feelingRatingBar.rating = 0.0f
                    notesEditText.text.clear()

                    // Hide the keyboard
                    val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(notesEditText.windowToken, 0)

                    // update averages
                    updateAvg()

                    // Navigate to summary fragment
                    val fragmentTransaction = parentFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.frame_layout, SummaryFragment())
                    fragmentTransaction.commit()
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