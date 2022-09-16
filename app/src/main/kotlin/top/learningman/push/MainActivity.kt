package top.learningman.push

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder
import top.learningman.push.channel.FCM
import top.learningman.push.databinding.ActivityMainBinding
import top.learningman.push.utils.MessageUtils

class MainActivity : AppCompatActivity() {
    lateinit var currentUserID: String
    var inited = false

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goSetting.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.goHistory.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

        requireOverLay()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val userid = pref.getString("user_id", "none")!!
        currentUserID = userid

        if (userid == "none") {
            setStatus(RegStatus.NOT_SET)
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
            setStatus(RegStatus.PENDING)

            when {
                FCM.should(this) -> FCM.setCallback(this, userid, lifecycleScope)
                else -> null
            }

            lifecycleScope.launch {
                MessageUtils.check(userid)
                    .onSuccess {
                        setStatus(RegStatus.SUCCESS)
                    }
                    .onFailure {
                        setStatus(RegStatus.FAILED)
                    }
            }
        } else {
            setStatus(RegStatus.NOT_SET)
        }
    }

    private fun setStatus(status: RegStatus) {
        runOnUiThread {
            when (status) {
                RegStatus.SUCCESS -> {
                    binding.regStatus.setCardBackgroundColor(this.colorList(R.color.reg_success))
                    binding.regStatusText.text = getString(R.string.userid_success)
                    binding.regStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.CHECK)
                }
                RegStatus.PENDING -> {
                    binding.regStatus.setCardBackgroundColor(this.colorList(R.color.reg_pending))
                    binding.regStatusText.text = getString(R.string.loading)
                    binding.regStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.SYNC)
                }
                RegStatus.FAILED -> {
                    binding.regStatus.setCardBackgroundColor(this.colorList(R.color.reg_failed))
                    binding.regStatusText.text = getString(R.string.connect_err)
                    binding.regStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.SYNC_ALERT)
                }
                RegStatus.NOT_SET -> {
                    binding.regStatus.setCardBackgroundColor(this.colorList(R.color.reg_failed))
                    binding.regStatusText.text = getString(R.string.not_set_userid_err)
                    binding.regStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.ALERT_CIRCLE_OUTLINE)
                }
            }
        }
    }

    private fun Context.colorList(id: Int): ColorStateList {
        return ColorStateList.valueOf(ContextCompat.getColor(this, id))
    }

    private fun requireOverLay() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this).setTitle("权限需求").setMessage("应用需要悬浮窗权限以正常工作，请在设置中开启")
                .setPositiveButton("去授权") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }.setNegativeButton("拒绝") { _, _ ->
                    finish()
                }.show().apply { setCanceledOnTouchOutside(false) }
        }
    }

    companion object {
        enum class RegStatus {
            SUCCESS,
            PENDING,
            FAILED,
            NOT_SET
        }
    }
}

