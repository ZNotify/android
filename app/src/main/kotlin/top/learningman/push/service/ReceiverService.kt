package top.learningman.push.service

import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.util.Log
import top.learningman.push.service.websocket.WebSocketSessionManager
import java.util.*


class ReceiverService : NotificationListenerService() {
    init {
        Log.d("ReceiverService", "ReceiverService init")
    }

    val id = UUID.randomUUID().toString()

    private val tag
        get() = "Recv-${id.substring(0, 8)}"

    override fun onCreate() {
        super.onCreate()
        if (manager == null) {
            manager = WebSocketSessionManager(this)
        }
        manager?.setServiceID(id)
        Log.i(tag, "ReceiverService $id create")

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand executed with startId: $startId")
        if (intent != null) {
            Log.d(tag, "using an intent with action ${intent.action}")
            when (intent.action) {
                Action.UPDATE.name -> {
                    val nextUserID =
                        intent.getStringExtra(INTENT_USERID_KEY)
                    if (!nextUserID.isNullOrEmpty()) {
                        manager?.updateUserID(nextUserID)
                    }
                }
                else -> {
                    Log.d("ReceiverService", "Unknown action ${intent.action}")
                    manager?.tryResume()
                }
            }
        } else {
            Log.d(tag, "with a null intent. It has been probably restarted by the system.")
            manager?.tryResume()
        }
        return START_STICKY
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(tag, "onListenerDisconnected in $id")
        requestRebind(
            ComponentName(
                applicationContext,
                NotificationListenerService::class.java
            )
        )
        manager?.tryResume()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(tag, "onListenerConnected")
        manager?.tryResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(tag, "ReceiverService $id destroyed")
    }


    enum class Action {
        UPDATE
    }

    companion object {
        const val INTENT_USERID_KEY = "nextUserID"
        var manager: WebSocketSessionManager? = null
    }
}
