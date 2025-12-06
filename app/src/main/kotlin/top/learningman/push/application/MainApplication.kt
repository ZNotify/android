package top.learningman.push.application

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.learningman.push.Constant
import top.learningman.push.checkerInit
import top.learningman.push.data.Repo
import top.learningman.push.utils.Network
import kotlin.system.exitProcess

class MainApplication : Application() {
    val repo by lazy {
        Repo.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)

        checkerInit(this)

        // FIXME: support dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        CoroutineScope(Dispatchers.IO).launch {
            Network.updateClient(repo.getUser())
            Log.i("MainApplication", "client init")
        }
    }
}