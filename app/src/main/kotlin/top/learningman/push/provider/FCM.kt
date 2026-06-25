package top.learningman.push.provider

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.Firebase
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.learningman.push.data.Repo
import top.learningman.push.utils.Network
import dev.zxilly.notify.sdk.entity.Channel as NotifyChannel

object FCM : Channel {
    override val name: String
        get() = "Firebase Cloud Messaging"

    override fun init(context: Context) {
        if (!Firebase.messaging.isAutoInitEnabled) {
            Firebase.messaging.isAutoInitEnabled = true
        }
    }

    override fun release(context: Context) {
        Firebase.messaging.isAutoInitEnabled = false
    }

    override fun available(context: Context): Boolean {
        val ga = GoogleApiAvailabilityLight.getInstance()
        return when (ga.isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> true
            else -> false
        }
    }

    override fun setUserCallback(context: Context, userID: String, scope: CoroutineScope) {
        FirebaseMessaging.getInstance().register().addOnCompleteListener { registerTask ->
            if (!registerTask.isSuccessful) {
                handleFailure(context, registerTask.exception)
                return@addOnCompleteListener
            }

            FirebaseInstallations.getInstance().id.addOnCompleteListener { idTask ->
                if (idTask.isSuccessful) {
                    registerInstallation(context, idTask.result, scope, showToast = true)
                } else {
                    handleFailure(context, idTask.exception)
                }
            }
        }
    }

    internal fun registerInstallation(
        context: Context,
        installationId: String,
        scope: CoroutineScope,
        showToast: Boolean
    ) {
        val deviceID = Repo.getInstance(context).getDeviceID()
        scope.launch {
            Network.register(installationId, NotifyChannel.FCM, deviceID)
                .onSuccess {
                    if (showToast) {
                        Toast.makeText(context, "FCM 注册成功", Toast.LENGTH_LONG).show()
                    }
                    Log.i("FCM", "FCM 注册成功")
                }
                .onFailure {
                    if (showToast) {
                        Toast.makeText(context, "FCM 注册失败", Toast.LENGTH_LONG).show()
                    }
                    Log.e("Firebase", "FCM 注册失败", it)
                }
        }
    }

    private fun handleFailure(context: Context, throwable: Throwable?) {
        Toast.makeText(context, "FCM 注册失败", Toast.LENGTH_LONG).show()
        Log.e("Firebase", "FCM registration error", throwable)
    }
}
