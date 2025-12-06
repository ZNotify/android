package top.learningman.push.provider

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.learningman.push.data.Repo
import top.learningman.push.service.ReceiverService
import top.learningman.push.utils.Network
import xyz.kumaraswamy.autostart.Autostart
import xyz.kumaraswamy.autostart.Utils
import java.util.*
import dev.zxilly.notify.sdk.entity.Channel as NotifyChannel


private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"


object WebSocket : Channel {
    override val name: String
        get() = "WebSocket"

    override fun init(context: Context) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, ReceiverService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
        Log.i("WebSocket", "WebSocket 初始化")
        CoroutineScope(Dispatchers.IO).launch {
            Network.register(
                "",
                NotifyChannel.WebSocket,
                Repo.getInstance(context).getDeviceID()
            ).onSuccess {
                Log.i("WebSocket", "WebSocket init 成功")
            }.onFailure {
                Log.e("WebSocket", "WebSocket init 失败", it)
            }
        }
    }

    override fun release(context: Context) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, ReceiverService::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
    }

    override fun setUserCallback(context: Context, userID: String, scope: CoroutineScope) {
        context.startService(Intent(context, ReceiverService::class.java).apply {
            action = ReceiverService.Action.UPDATE.name
            putExtra(ReceiverService.INTENT_USERID_KEY, userID)
        })
        scope.launch {
            val deviceID = Repo.getInstance(context).getDeviceID()
            Network.register("", NotifyChannel.WebSocket, deviceID)
                .onSuccess {
                    Toast.makeText(context, "WebSocket 注册成功", Toast.LENGTH_LONG).show()
                    Log.i("WebSocket", "WebSocket 注册成功")
                }
                .onFailure {
                    Log.e("WebSocket", "WebSocket 注册失败", it)
                }
        }
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

        if (Utils.isOnMiui()) {
            val miuiAutoStartPermission = object : Permission {
                override val name: String
                    get() = "自启动"
                override val description: String
                    get() = "Notify 需要自启动权限以保持后台运行。"

                override fun check(context: Context): Boolean? {
                    return kotlin.runCatching {
                        return Autostart.isAutoStartEnabled(context)
                    }.getOrNull()
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

            permissions.add(miuiAutoStartPermission)
        }

        return permissions
    }
}