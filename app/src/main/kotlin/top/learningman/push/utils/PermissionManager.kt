package top.learningman.push.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import top.learningman.push.provider.AutoChannel
import top.learningman.push.provider.Permission

class PermissionManager(val activity: Activity) {
    private val _permissions = mutableListOf<Permission>()
    val permissions: List<Permission> = _permissions

    init {
        val overlayPermission = object : Permission {
            override val name: String
                get() = "悬浮窗"
            override val description: String
                get() = "悬浮窗权限用于从通知显示完整通知"

            override fun check(context: Context): Boolean {
                return Settings.canDrawOverlays(context)
            }

            override fun grant(activity: Activity) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            }
        }
        _permissions.add(overlayPermission)

        val notificationPermission = object : Permission {
            override val name: String
                get() = "通知"
            override val description: String
                get() = "通知权限用于发送通知"

            override fun check(context: Context): Boolean {
                return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                }
            }

            override fun grant(activity: Activity) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    activity.startActivity(intent)
                } else {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                }
            }
        }
        _permissions.add(notificationPermission)

        val channelPermissions = AutoChannel.getInstance(activity).permissions()
        _permissions.addAll(channelPermissions)
    }

    fun ok() = _permissions.all { it.check(activity) ?: true }

    companion object
}