package top.learningman.push

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Process
import android.widget.Toast
import com.google.android.material.color.DynamicColors
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.distribute.Distribute
import top.learningman.push.channel.FCM

class MainApplication : Application() {
    companion object {
        lateinit var handler: Handler

        fun isHandlerInit(): Boolean {
            return this::handler.isInitialized
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppCenter.start(
            this,
            Constant.APP_CENTER_SECRET,
            Analytics::class.java,
            Crashes::class.java,
            Distribute::class.java
        )

        DynamicColors.applyToActivitiesIfAvailable(this)

        if (shouldInit()) {
            when (true) {
                FCM.should(this) -> FCM.init(this)
                else -> {
                    Toast.makeText(this, "Use fallback push channel.", Toast.LENGTH_SHORT).show()
                    // FIXME: add fallback push channel
                }
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