package top.learningman.push.application

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.distribute.Distribute
import top.learningman.push.Constant
import top.learningman.push.data.Repo

class MainApplication : Application() {
    val repo by lazy {
        Repo.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        AppCenter.start(
            this,
            Constant.APP_CENTER_SECRET,
            Analytics::class.java,
            Crashes::class.java,
            Distribute::class.java
        )

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}