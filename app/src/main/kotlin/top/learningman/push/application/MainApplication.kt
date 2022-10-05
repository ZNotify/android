package top.learningman.push.application

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import dev.zxilly.lib.upgrader.Upgrader
import top.learningman.push.Constant
import top.learningman.push.data.Repo

class MainApplication : Application() {
    val repo by lazy {
        Repo.getInstance(this)
    }

    lateinit var upgrader: Upgrader

    override fun onCreate() {
        super.onCreate()
        AppCenter.start(
            this,
            Constant.APP_CENTER_SECRET,
            Analytics::class.java,
            Crashes::class.java
        )

        DynamicColors.applyToActivitiesIfAvailable(this)

//        upgrader = Upgrader(object : Checker {
//            override suspend fun getLatestVersion(): Version {
//                return Version(
//                    1994896132,
//                    "1.0.0",
//                    "测试版本",
//                    "https://github.com/ZNotify/android/releases/download/20220930T183614/app-release.apk",
//                    "app.apk"
//                )
//            }
//        }, this)
    }
}