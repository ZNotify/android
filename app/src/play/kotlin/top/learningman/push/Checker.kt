package top.learningman.push

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dev.zxilly.lib.upgrader.checker.Checker
import dev.zxilly.lib.upgrader.checker.Version

internal val checker = object : Checker {
    override suspend fun getLatestVersion(): Version {
        return Version(0, "", null, "", null)
    }
}

internal fun playUpgrade(context: Context) {
    val appUpdateManager = AppUpdateManagerFactory.create(context)

    val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        ) {
            val requestCode = 23212
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.IMMEDIATE,
                context as Activity,
                requestCode
            )
        }
    }
}