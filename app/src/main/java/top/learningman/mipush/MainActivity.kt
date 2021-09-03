package top.learningman.mipush

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.xiaomi.mipush.sdk.MiPushClient
import kotlinx.android.synthetic.main.activity_main.*
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.lang.Exception
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    lateinit var currentUserID: String
    var inited = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        go_setting.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val userid = pref.getString("user_id", "none")!!
        currentUserID = userid

        if (userid == "none") {
            reg_status.setCardBackgroundColor(this.colorList(R.color.reg_failed))
            reg_status_text.text = getString(R.string.not_set_userid_err)
            reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.ALERT_CIRCLE_OUTLINE)
            return
        }

        if (shouldInit()) {
            MiPushClient.registerPush(this, "2882303761520035342", "5272003587342")
        }
    }

    override fun onResume() {
        super.onResume()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val userid = pref.getString("user_id", "none")!!

        if (userid == currentUserID && inited) {
            return
        }

        if (!inited) {
            inited = true
        }

        if (userid != "none") {
            reg_status.setCardBackgroundColor(this.colorList(R.color.reg_pending))
            reg_status_text.text = getString(R.string.loading)
            reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.SYNC)
            thread {
                try {
                    val client = OkHttpClient()
                    val url = BuildConfig.APIURL.toHttpUrlOrNull()!!
                        .newBuilder()
                        .addQueryParameter("user_id", userid)
                        .addPathSegment("check")
                        .build()
                    val request = Request.Builder()
                        .url(url)
                        .build()
                    try {
                        val response = client.newCall(request).execute()
                        val responseData = response.body?.string() ?: throw Exception("No response")
                        if (responseData == "true") {
                            runOnUiThread {
                                reg_status.setCardBackgroundColor(this.colorList(R.color.reg_success))
                                reg_status_text.text = getString(R.string.userid_success)
                                reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.CHECK)
                            }

                            val accounts = MiPushClient.getAllUserAccount(this)
                            if (!accounts.contains(userid)) {
                                MiPushClient.setUserAccount(this, userid, null)
                                Log.d("Accounts", accounts.toString())
                                for (account in accounts) {
                                    MiPushClient.unsetUserAccount(this, account, null)
                                }
                            }
                        } else {
                            runOnUiThread {
                                reg_status.setCardBackgroundColor(this.colorList(R.color.reg_failed))
                                reg_status_text.text = getString(R.string.userid_not_exist_err)
                                reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.ALERT)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            reg_status.setCardBackgroundColor(this.colorList(R.color.reg_failed))
                            reg_status_text.text = getString(R.string.connect_err)
                            reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.SYNC_ALERT)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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

fun Context.colorList(id: Int): ColorStateList {
    return ColorStateList.valueOf(ContextCompat.getColor(this, id))
}