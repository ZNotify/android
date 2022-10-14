package top.learningman.push.provider

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.learningman.push.data.Repo
import top.learningman.push.utils.APIUtils
import dev.zxilly.notify.sdk.entity.Channel as NotifyChannel

object FCM : Channel {
    override val name: String
        get() = "Firebase Cloud Messaging"

    override fun init(context: Context) {
        if (!Firebase.messaging.isAutoInitEnabled) {
            Firebase.messaging.isAutoInitEnabled = true
        }
    }

    override fun should(context: Context): Boolean {
        val ga = GoogleApiAvailability.getInstance()
        return when (ga.isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> true
            else -> false
        }
    }

    override fun setUserCallback(context: Context, userID: String, scope: CoroutineScope) {
        val deviceID = Repo.getInstance(context).getDeviceID()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                scope.launch {
                    APIUtils.register(userID, token, NotifyChannel.FCM, deviceID)
                        .onSuccess {
                            Toast.makeText(context, "FCM 注册成功", Toast.LENGTH_LONG).show()
                            Log.i("FCM", "FCM 注册成功")
                        }
                        .onFailure {
                            Toast.makeText(context, "FCM 注册失败", Toast.LENGTH_LONG).show()
                            Log.e("Firebase", "FCM 注册失败", it)
                            Crashes.trackError(it)
                        }
                }
            } else {
                Toast.makeText(context, "FCM 注册失败", Toast.LENGTH_LONG).show()
                Log.e("Firebase", "token error: ${task.exception}")
                task.exception?.let { Crashes.trackError(it) } ?: run {
                    Crashes.trackError(Throwable("Failed to get token from FCM"))
                }
            }
        }
    }
}