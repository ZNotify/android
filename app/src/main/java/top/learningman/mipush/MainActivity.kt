package top.learningman.mipush

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import com.xiaomi.mipush.sdk.MiPushClient
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        go_setting.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        if (shouldInit()) {
            MiPushClient.registerPush(this, "2882303761520035342", "5272003587342")
        }
    }

    private fun shouldInit(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processInfo = am.runningAppProcesses;
        val mainProcessName = applicationInfo.processName
        val myPid = Process.myPid()
        for (info in processInfo) {
            if (info.pid == myPid && mainProcessName == info.processName)
                return true
        }
        return false
    }
}