package top.learningman.push

import android.app.Application
import android.content.Context
import android.util.Log
import com.microsoft.appcenter.crashes.Crashes
import dev.zxilly.lib.upgrader.Upgrader
import dev.zxilly.lib.upgrader.checker.GitHubRMCConfig
import dev.zxilly.lib.upgrader.checker.GitHubReleaseMetadataChecker
import top.learningman.push.activity.TranslucentActivity

internal fun checkerInit(app: Application) {
    runCatching {
        Upgrader.init(
            app, Upgrader.Companion.Config(
                GitHubReleaseMetadataChecker(
                    GitHubRMCConfig(
                        owner = "ZNotify",
                        repo = "android",
                        upgradeChannel = GitHubRMCConfig.UpgradeChannel.PRE_RELEASE
                    )
                ),
                listOf(TranslucentActivity::class.java)
            )
        )
    }.onFailure {
        Log.e("Upgrader", "Failed to initialize Upgrader", it)
        Crashes.trackError(it)
    }
}

internal fun checkUpgrade(context: Context) {
    Upgrader.getInstance()?.tryUpgrade()
}
