package top.learningman.push

import android.app.Application
import android.content.Context
import android.util.Log
import com.microsoft.appcenter.crashes.Crashes
import dev.zxilly.lib.upgrader.Upgrader
import dev.zxilly.lib.upgrader.checker.AppCenterChecker
import top.learningman.push.activity.TranslucentActivity

fun checkerInit(app: Application) {
    Upgrader.init(
        app, Upgrader.Companion.Config(
            AppCenterChecker("0c045975-212b-441d-9ee4-e6ab9c76f8a3"),
            listOf(TranslucentActivity::class.java)
        )
    )
}

@Suppress("UNUSED_PARAMETER")
fun checkUpgrade(context: Context) {
    runCatching {
        Upgrader.getInstance()?.tryUpgrade()?: Log.e("Checker", "Upgrader is null")
    }.onFailure {
        Log.e("Upgrader", "Failed to check upgrade", it)
        Crashes.trackError(it)
    }
}