package top.learningman.push.provider

import RomUtils
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.work.*
import top.learningman.push.BuildConfig
import top.learningman.push.data.Repo
import top.learningman.push.service.PollWorker
import top.learningman.push.service.ReceiverService
import java.util.concurrent.TimeUnit


private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"


object Host : Channel {
    override val name: String
        get() = "Websocket"

    override fun init(context: Context) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, ReceiverService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    override fun setUserCallback(context: Context, userID: String) {
        context.startService(Intent(context, ReceiverService::class.java).apply {
            action = ReceiverService.Action.UPDATE.name
            putExtra(ReceiverService.INTENT_USERID_KEY, userID)
        })
    }

    override fun permissions(): List<Permission> {
        val permissions = mutableListOf<Permission>()

        val notificationListenerPermission = object : Permission {
            override val name: String
                get() = "通知监听器"
            override val description: String
                get() = "Notify 需要通知监听器权限以保持后台运行，Notify 不会读取您的通知内容。"

            override fun check(context: Context): Boolean {
                val enabledNotificationListeners =
                    Settings.Secure.getString(
                        context.contentResolver,
                        ENABLED_NOTIFICATION_LISTENERS
                    )
                val componentName = ComponentName(context, ReceiverService::class.java)
                return enabledNotificationListeners != null
                        && enabledNotificationListeners.contains(componentName.flattenToString())
            }

            override fun grant(activity: Activity) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                activity.startActivity(intent)
            }
        }
        permissions.add(notificationListenerPermission)

        val batteryIgnorePermission = object : Permission {
            override val name: String
                get() = "忽略电池优化"
            override val description: String
                get() = "Notify 需要忽略电池优化权限以保持后台运行。"

            override fun check(context: Context): Boolean {
                val powerManager =
                    context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                return powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }

            @SuppressLint("BatteryLife")
            override fun grant(activity: Activity) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            }
        }
        permissions.add(batteryIgnorePermission)

        val miuiAutoStartPermission = object : Permission {
            override val name: String
                get() = "自启动"
            override val description: String
                get() = "Notify 需要自启动权限以保持后台运行。"

            override fun check(context: Context): Boolean? {
                return null
            }

            override fun grant(activity: Activity) {
                val intent = Intent()
                intent.component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                activity.startActivity(intent)
            }
        }

        if (RomUtils.isMiui()){
            permissions.add(miuiAutoStartPermission)
        }

        return permissions
    }

    private fun scheduleMessageWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val repo = Repo.getInstance(context)
        val workerVersion = repo.getMessageWorkerVersion()
        val currentVersion = BuildConfig.VERSION_CODE
        val workerPolicy = if (workerVersion == currentVersion) {
            ExistingPeriodicWorkPolicy.KEEP
        } else {
            repo.setMessageWorkerVersion(currentVersion)
            ExistingPeriodicWorkPolicy.REPLACE
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work =
            PeriodicWorkRequestBuilder<PollWorker>(POLL_WORKER_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(PollWorker.TAG)
                .addTag(PollWorker.WORK_NAME_PERIODIC_ALL)
                .build()
        Log.d(
            "MainActivity",
            "Poll worker: Scheduling period work every $POLL_WORKER_INTERVAL_MINUTES minutes"
        )
        workManager.enqueueUniquePeriodicWork(PollWorker.WORK_NAME_PERIODIC_ALL, workerPolicy, work)
    }

    private const val POLL_WORKER_INTERVAL_MINUTES = 60L
}