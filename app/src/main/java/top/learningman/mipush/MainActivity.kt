package top.learningman.mipush

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.xiaomi.mipush.sdk.MiPushClient
import kotlinx.android.synthetic.main.activity_main.*
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.lang.Exception
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    lateinit var currentUserID: String
    lateinit var context: Context
    var inited = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContentView(R.layout.activity_main)

        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val uiMessage = msg.obj as UIMessage
                reg_status.setCardBackgroundColor(context.getColor(uiMessage.color))
                reg_status_icon.setIcon(uiMessage.icon)
                reg_status_text.text = uiMessage.reason

                when (msg.what) {
                    MiPushReceiver.Companion.ActionEnum.REG_SUCCESS.ordinal -> {
                        val accounts = MiPushClient.getAllUserAccount(context)
                        if (!accounts.contains(currentUserID)) {
                            MiPushClient.setUserAccount(context, currentUserID, null)
                            Log.d("Accounts", accounts.toString())
                            for (account in accounts) {
                                MiPushClient.unsetUserAccount(context, account, null)
                            }
                        }
                    }
                }
            }
        }

        MainApplication.handler = handler

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
    }

    override fun onResume() {
        super.onResume()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val userid = pref.getString("user_id", "none")!!

        if (userid == currentUserID && inited || userid == "none") {
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
                        .addPathSegment(userid)
                        .addPathSegment("check")
                        .build()
                    val request = Request.Builder()
                        .url(url)
                        .build()
                    try {
                        val response = client.newCall(request).execute()
                        response.body?.string() ?: throw Exception("No response")
//                        if (responseData == "true") {
////                            runOnUiThread {
////                                reg_status.setCardBackgroundColor(this.colorList(R.color.reg_success))
////                                reg_status_text.text = getString(R.string.userid_success)
////                                reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.CHECK)
////                            }
//
//                            val accounts = MiPushClient.getAllUserAccount(this)
//                            if (!accounts.contains(userid)) {
//                                MiPushClient.setUserAccount(this, userid, null)
//                                Log.d("Accounts", accounts.toString())
//                                for (account in accounts) {
//                                    MiPushClient.unsetUserAccount(this, account, null)
//                                }
//                            }
//                        } else {
////                            runOnUiThread {
////                                reg_status.setCardBackgroundColor(this.colorList(R.color.reg_failed))
////                                reg_status_text.text = getString(R.string.userid_not_exist_err)
////                                reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.ALERT)
////                            }
//                        }

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


    fun Context.colorList(id: Int): ColorStateList {
        return ColorStateList.valueOf(ContextCompat.getColor(this, id))
    }
}

