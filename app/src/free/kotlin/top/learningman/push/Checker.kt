package top.learningman.push

import dev.zxilly.lib.upgrader.checker.Checker
import dev.zxilly.lib.upgrader.checker.Version

internal val checker = object : Checker {
    override suspend fun getLatestVersion(): Version {
        return Version(0, "", null, "", null)
    }

}