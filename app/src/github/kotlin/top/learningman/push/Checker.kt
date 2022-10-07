package top.learningman.push

import android.content.Context
import dev.zxilly.lib.upgrader.checker.GitHubReleaseMetadataChecker

internal val checker = GitHubReleaseMetadataChecker(
    GitHubReleaseMetadataChecker.Config(
        owner = "ZNotify",
        repo = "android",
        upgradeChannel = GitHubReleaseMetadataChecker.Config.UpgradeChannel.PRE_RELEASE
    )
)

internal fun playUpgrade(context: Context) {
}