package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

/**
 * Play-Store compliant reminder notification helper.
 * Uses standard notification channels with ZERO SCHEDULE_EXACT_ALARM permissions.
 */
object ReminderHelper {
    private const val CHANNEL_ID = "allenamente_daily_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Promemoria AllenaMente",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Promemoria gentile per il circuito cognitivo quotidiano di 3 minuti."
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showReminderConfirmation(context: Context, hour: Int, minute: Int) {
        try {
            ensureChannel(context)
            val timeStr = "%02d:%02d".format(hour, minute)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Promemoria AllenaMente attivo")
                .setContentText("Ti ricorderemo il tuo circuito quotidiano intorno alle $timeStr.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1042, notification)
        } catch (_: SecurityException) {
            // Permission not granted; ignored gracefully
        }
    }
}
