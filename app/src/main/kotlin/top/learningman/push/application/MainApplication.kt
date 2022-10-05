package top.learningman.push.application

import android.app.Application
import android.util.Log
import com.google.android.material.color.DynamicColors
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import dev.zxilly.lib.upgrader.Upgrader
import dev.zxilly.lib.upgrader.checker.GitHubReleaseMetadataChecker
import top.learningman.push.Constant
import top.learningman.push.data.Repo

class MainApplication : Application() {
    val repo by lazy {
        Repo.getInstance(this)
    }

    var upgrader: Upgrader? = null

    override fun onCreate() {
        super.onCreate()
        AppCenter.start(
            this,
            Constant.APP_CENTER_SECRET,
            Analytics::class.java,
            Crashes::class.java
        )

        DynamicColors.applyToActivitiesIfAvailable(this)

        upgrader = runCatching {
            Upgrader(
                GitHubReleaseMetadataChecker(
                    GitHubReleaseMetadataChecker.Config(
                        owner = "ZNotify",
                        repo = "android",
                        upgradeChannel = GitHubReleaseMetadataChecker.Config.UpgradeChannel.PRE_RELEASE
                    )
                ),
                app = this
            )
        }.also { ret ->
            ret.onFailure {
                Log.e("Upgrader", "Failed to initialize Upgrader", it)
            }
        }.getOrNull()
    }
}