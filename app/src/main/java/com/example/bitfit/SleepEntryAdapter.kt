package com.example.bitfit

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

private const val TAG = "SleepEntryAdapter/"

class SleepEntryAdapter(private val context: Context, private val sleepEntries: MutableList<SleepEntry>) :
    RecyclerView.Adapter<SleepEntryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val dateTextView = itemView.findViewById<TextView>(R.id.dateTextView)
        private val hoursTextView = itemView.findViewById<TextView>(R.id.hoursSleptTextView)
        private val notesTextView = itemView.findViewById<TextView>(R.id.notesTextView)
        private val feelingRatingBar = itemView.findViewById<RatingBar>(R.id.feelingRatingBar)

        fun bind(sleepEntry: SleepEntry){
            dateTextView.text = sleepEntry.date
            hoursTextView.text = "Hours slept: ${sleepEntry.hoursSlept.toString()}"
            notesTextView.text = sleepEntry.notes
            feelingRatingBar.rating = sleepEntry.feeling
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sleepEntry = sleepEntries[position]
        holder.bind(sleepEntry)

        // Set long click listener to delete item
        holder.itemView.setOnLongClickListener {
            (context as LifecycleOwner).lifecycleScope.launch(IO) {
                // delete from database
                val dao = (holder.itemView.context.applicationContext as SleepApplication).db.sleepDao()
                sleepEntry.notes?.let { it1 ->
                    dao.deleteSleepEntity(sleepEntry.date, sleepEntry.feeling,
                        sleepEntry.hoursSlept, it1
                    )
                }
                val averageHoursTextView = (context as Activity).findViewById<TextView>(R.id.average_sleep_time)
                val averageFeelingRatingBar = (context as Activity).findViewById<RatingBar>(R.id.average_feeling_value)
                val averageSleepTimeTextViewSummary = (context as Activity).findViewById<TextView>(R.id.average_sleep_timeTv)
                val averageFeelingRatingBarSummary = (context as Activity).findViewById<RatingBar>(R.id.average_feelingRb)
                val averageSleepTime = dao.getAverageSleepTime()
                val averageFeeling = dao.getAverageFeeling()

                (context as Activity).runOnUiThread {
                    if (averageHoursTextView != null) {
                        averageHoursTextView.text = "Average sleep time: ${String.format("%.1f", averageSleepTime)}"
                    }
                    if (averageFeelingRatingBar != null) {
                        averageFeelingRatingBar.rating = averageFeeling
                    }
                    if (averageSleepTimeTextViewSummary != null) {
                        averageSleepTimeTextViewSummary.text = "Average sleep time: ${String.format("%.1f", averageSleepTime)}"
                    }
                    if (averageFeelingRatingBarSummary != null) {
                        averageFeelingRatingBarSummary.rating = averageFeeling
                    }
                }

            }
            sleepEntries.removeAt(position)
            notifyItemRemoved(position)
            true
        }
    }

    override fun getItemCount() = sleepEntries.size
}