package io.igrant.mobileagent.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import io.igrant.mobileagent.R
import io.igrant.mobileagent.activty.InitializeActivity
import java.util.concurrent.atomic.AtomicInteger

object NotificationUtils {

     fun showNotification(context:Context,type:String,title:String,desc:String){
//        val intent = Intent(context, InitializeActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        intent.action = System.currentTimeMillis().toString()

//        val pendingIntent: PendingIntent =
//            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(
            context,
            context.resources.getString(R.string.default_notification_channel_id)
        )
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(desc)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getChannelIdForParu(type,context),
                getChannelName(type,context),
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            builder.setChannelId(getChannelIdForParu(type,context))
        }
        mNotificationManager.notify(SystemClock.uptimeMillis().toInt(), builder.build())


    }

    fun showNotification(intent:Intent,context:Context,type:String,title:String,desc:String){

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(
            context,
            context.resources.getString(R.string.default_notification_channel_id)
        )
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(desc)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getChannelIdForParu(type,context),
                getChannelName(type,context),
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            builder.setChannelId(getChannelIdForParu(type,context))
        }
        mNotificationManager.notify(SystemClock.uptimeMillis().toInt(), builder.build())
    }
    private fun getChannelName(type: String, context: Context): String {
        when (type) {
            MessageTypes.TYPE_ISSUE_CREDENTIAL -> {
                return context.resources.getString(R.string.notification_channel_success_messages)
            }
            else -> {
                return context.resources.getString(R.string.notification_channel_success_messages)
            }
        }
    }

    private fun getChannelIdForParu(type: String,context: Context): String {
        when (type) {
            MessageTypes.TYPE_ISSUE_CREDENTIAL -> {
                return context.resources.getString(R.string.notification_channel_success)
            }
            else -> {
                return context.resources.getString(R.string.notification_channel_success)
            }
        }

    }

    private fun getNotificationID(): Int {
        val c = AtomicInteger(0)
        val iD: Int = c.incrementAndGet()
        return iD
    }
}