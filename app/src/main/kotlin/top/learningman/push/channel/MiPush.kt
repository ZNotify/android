package top.learningman.push.channel

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.xiaomi.mipush.sdk.MiPushClient
import top.learningman.push.MainApplication
import top.learningman.push.hook.MiPushReceiver

object MiPush : Channel {
    override fun init(context: Context) {
        MiPushClient.registerPush(context, "2882303761520145940", "5542014546940")
    }

    override fun should(context: Context): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    override fun setCallback(context: Context, userID: String) {
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MiPushReceiver.Companion.ActionEnum.REG_SUCCESS.ordinal -> {
                        Toast.makeText(context, "MiPush 注册成功", Toast.LENGTH_SHORT).show()
                        Log.i("MiPush", "reg success")
                        val accounts = MiPushClient.getAllUserAccount(context)
                        if (!accounts.contains(userID)) {
                            MiPushClient.setUserAccount(context, userID, null)
                        }
                        accounts.filter { it != userID }.forEach {
                            MiPushClient.unsetUserAccount(context, it, null)
                        }
                    }
                    MiPushReceiver.Companion.ActionEnum.REG_FAILED.ordinal -> {
                        Toast.makeText(context, "MiPush 注册失败", Toast.LENGTH_LONG).show()
                        Log.i("MiPush", "reg failed")
                    }
                }
            }
        }
        if (!MainApplication.isHandlerInit()) {
            MainApplication.handler = handler // FIXME: use viewModel instead
        }
    }
}