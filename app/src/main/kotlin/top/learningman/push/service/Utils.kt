package top.learningman.push.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import top.learningman.push.R
import top.learningman.push.activity.TranslucentActivity
import top.learningman.push.entity.Message
import top.learningman.push.entity.MessageAdapter.MessageHolder.Companion.fromRFC3339Nano
import top.learningman.push.entity.MessageAdapter.MessageHolder.Companion.toRFC3339
import kotlin.random.Random

object Utils {
    @SuppressLint("MissingPermission")
    fun notifyMessage(context: Context, message: Message) {
        val notificationManager = NotificationManagerCompat.from(context)
        val notifyChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notifyChannel)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, TranslucentActivity::class.java).also {
                it.putExtra(
                    TranslucentActivity.TIME_INTENT_KEY,
                    message.createdAt.fromRFC3339Nano().toRFC3339()
                )
                it.putExtra(TranslucentActivity.CONTENT_INTENT_KEY, message.content)
                it.putExtra(TranslucentActivity.TITLE_INTENT_KEY, message.title)
                it.putExtra(TranslucentActivity.MSGID_INTENT_KEY, message.id)
                it.putExtra(TranslucentActivity.LONG_INTENT_KEY, message.long)
                it.putExtra(TranslucentActivity.USERID_INTENT_KEY, message.userID)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(message.title)
            .setContentText(message.content)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    private const val NOTIFICATION_CHANNEL_ID = "ReceiverService"
    private const val NOTIFICATION_CHANNEL_NAME = "Normal Message"
}