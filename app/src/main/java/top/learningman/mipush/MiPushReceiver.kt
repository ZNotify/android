package top.learningman.mipush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver
import kotlin.concurrent.thread

class MiPushReceiver : PushMessageReceiver() {
}