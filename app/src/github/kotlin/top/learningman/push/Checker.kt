package top.learningman.push

import android.app.Application
import android.content.Context
import android.util.Log
import com.microsoft.appcenter.crashes.Crashes
import dev.zxilly.lib.upgrader.Upgrader
import dev.zxilly.lib.upgrader.checker.GitHubReleaseMetadataChecker

internal fun checkerInit(app: Application) {
    runCatching {
        Upgrader(
            GitHubReleaseMetadataChecker(
                GitHubReleaseMetadataChecker.Config(
                    owner = "ZNotify",
                    repo = "android",
                    upgradeChannel = GitHubReleaseMetadataChecker.Config.UpgradeChannel.PRE_RELEASE
                )
            ), app
        )
    }.onFailure {
        Log.e("Upgrader", "Failed to initialize Upgrader", it)
        Crashes.trackError(it)
    }
}

internal fun checkUpgrade(context: Context) {
    Upgrader.getInstance()?.tryUpgrade()
}
