package top.learningman.push

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessaging
import com.xiaomi.mipush.sdk.MiPushClient
import kotlinx.android.synthetic.main.activity_main.*
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import top.learningman.push.hook.MiPushReceiver
import top.learningman.push.utils.Utils
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
                when (msg.what) {
                    MiPushReceiver.Companion.ActionEnum.REG_SUCCESS.ordinal -> {
                        Toast.makeText(context, "MiPush 注册成功", Toast.LENGTH_SHORT).show()
                        Log.i("MiPush", "reg success")
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

        MainApplication.handler = handler // FIXME: use viewModel instead

        go_setting.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        go_history.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
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

        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("权限需求")
                .setMessage("应用需要悬浮窗权限以正常工作，请在设置中开启")
                .setPositiveButton("去授权") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                .setNegativeButton("拒绝") { _, _ ->
                    finish()
                }
                .show()
                .apply { setCanceledOnTouchOutside(false) }
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val userid = pref.getString("user_id", "none")!!

        if (userid == currentUserID && inited) {
            return
        }

        if (!inited) {
            inited = true
        }

        if (userid != "none") {
            if (!Utils.isXiaoMi()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "FCM 注册成功", Toast.LENGTH_LONG).show()
                        val token = task.result
                        Log.d("Firebase", "token: $token")
                        thread {
                            val url = BuildConfig.APIURL.toHttpUrlOrNull()!!
                                .newBuilder()
                                .addPathSegment(userid)
                                .addPathSegment("token")
                                .build()
                            val client = OkHttpClient()
                            val request = Request.Builder()
                                .url(url)
                                .put(token.toRequestBody("text/plain".toMediaTypeOrNull()))
                                .build()
                            try {
                                val response = client.newCall(request).execute()
                                Log.d("Push", "response: ${response.body?.string()}")
                            } catch (e: Exception) {
                                Log.e("Push", "error: ${e.message}")
                            }
                        }
                    } else {
                        Log.e("Firebase", "token error: ${task.exception}")
                    }
                }
            }


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
                        val responseText = response.body?.string() ?: throw Exception("No response")
                        if (responseText == "true") {
                            runOnUiThread {
                                reg_status.setCardBackgroundColor(this.colorList(R.color.reg_success))
                                reg_status_text.text = getString(R.string.userid_success)
                                reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.CHECK)
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
        } else {
            reg_status.setCardBackgroundColor(this.colorList(R.color.reg_failed))
            reg_status_text.text = getString(R.string.not_set_userid_err)
            reg_status_icon.setIcon(MaterialDrawableBuilder.IconValue.ALERT_CIRCLE_OUTLINE)
        }
    }


    private fun Context.colorList(id: Int): ColorStateList {
        return ColorStateList.valueOf(ContextCompat.getColor(this, id))
    }
}

