package com.example.bitfit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class SleepDataReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Create and send a notification to remind the user to fill in their sleep data
        val notificationManager = ContextCompat.getSystemService(
            context!!, NotificationManager::class.java) as NotificationManager
        val channelId = "sleep_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Sleep Reminder", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val actionIntent = Intent(context, MainActivity::class.java)
        val actionPendingIntent = PendingIntent.getActivity(context, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Fill in your sleep data")
            .setContentText("Don't forget to update your sleep entry for today!")
            .setSmallIcon(R.drawable.ic_sleep)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_sleep, "Open App", actionPendingIntent)
            .build()
        notificationManager.notify(0, notification)
    }

}
