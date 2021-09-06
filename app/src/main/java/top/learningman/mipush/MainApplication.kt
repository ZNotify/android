package top.learningman.mipush

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Process
import com.xiaomi.mipush.sdk.MiPushClient
import top.learningman.mipush.entity.MessageRepo
import top.learningman.mipush.instance.MessageDatabase

class MainApplication : Application() {
    companion object {
        lateinit var handler: Handler

        fun isHandlerInit():Boolean {
            return this::handler.isInitialized
        }
    }

    val database by lazy { MessageDatabase.getDatabase(this) }
    val repository by lazy { MessageRepo(database.messageDao()) }

    fun setHandler(h: Handler) {
        handler = h
    }

    fun getHandler(): Handler {
        return handler
    }

    override fun onCreate() {
        super.onCreate()

        if (shouldInit()) {
            MiPushClient.registerPush(this, "2882303761520035342", "5272003587342")
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