package top.learningman.push.channel

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.learningman.push.utils.MessageUtils

object FCM : Channel {
    override fun init(context: Context) {
        Firebase.messaging.isAutoInitEnabled = true
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
    }

    override fun should(context: Context): Boolean {
        val ga = GoogleApiAvailability.getInstance()
        return when (ga.isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> true
            else -> false
        }
    }

    override fun setCallback(context: Context, userID: String, scope: CoroutineScope) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("Firebase", "token: $token")
                scope.launch {
                    MessageUtils.reportFCMToken(userID, token)
                        .onSuccess {
                            Toast.makeText(context, "FCM 注册成功", Toast.LENGTH_LONG).show()
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