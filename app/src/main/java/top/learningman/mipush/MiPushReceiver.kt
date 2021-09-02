package top.learningman.mipush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver

class MiPushReceiver : PushMessageReceiver() {

    override fun onReceiveRegisterResult(context: Context, msg: MiPushCommandMessage) {
        val command = msg.command
        val args = msg.commandArguments
        Log.i("Command", command)
        Log.i("Args", args.toString())
    }
}