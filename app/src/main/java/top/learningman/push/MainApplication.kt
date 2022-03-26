package top.learningman.push

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Process
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.xiaomi.mipush.sdk.MiPushClient
import top.learningman.push.utils.Utils

class MainApplication : Application() {
    companion object {
        lateinit var handler: Handler

        fun isHandlerInit(): Boolean {
            return this::handler.isInitialized
        }
    }

    fun setHandler(h: Handler) {
        handler = h
    }

    fun getHandler(): Handler {
        return handler
    }

    override fun onCreate() {
        super.onCreate()

        if (shouldInit()) {
            Log.d("Manufacturer", Build.MANUFACTURER)
            if (Utils.isXiaoMi()) {
                MiPushClient.registerPush(this, "2882303761520035342", "5272003587342")
            } else {
                Firebase.messaging.isAutoInitEnabled = true
                FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
            }
        }
    }

    private fun shouldInit(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processInfo = am.runningAppProcesses
        val mainProcessName = applicationInfo.processName
        val myPid = Process.myPid()
        for (info in processInfo) {
            if (info.pid == myPid && mainProcessName == info.processName)
                return true
        }
        return false
    }
}