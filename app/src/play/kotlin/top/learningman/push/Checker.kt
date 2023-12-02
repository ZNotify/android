package top.learningman.push

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

internal fun checkerInit(app: Application) {}
internal fun checkUpgrade(context: Context) {
    val appUpdateManager = AppUpdateManagerFactory.create(context)

    val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        ) {
            appUpdateManager.startUpdateFlow(
                appUpdateInfo,
                context as Activity,
                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
            )
        }
    }
}