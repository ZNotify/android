package top.learningman.push.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.learningman.push.data.Repo
import top.learningman.push.provider.FCM
import top.learningman.push.utils.Network

class FCMService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)

        val repo = Repo.getInstance(this)
        val userId = repo.getUser()
        if (userId == Repo.PREF_USER_DEFAULT) {
            Log.d("FCMService", "Skip FCM registration: user ID is not configured")
            return
        }

        scope.launch {
            runCatching {
                Network.updateClient(userId)
                FCM.registerInstallation(this@FCMService, installationId, this, showToast = false)
            }.onFailure {
                Log.e("FCMService", "Failed to register refreshed FCM installation", it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
