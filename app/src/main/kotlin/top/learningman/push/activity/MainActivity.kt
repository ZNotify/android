package top.learningman.push.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder.IconValue
import top.learningman.push.R
import top.learningman.push.application.MainApplication
import top.learningman.push.data.Repo
import top.learningman.push.databinding.ActivityMainBinding
import top.learningman.push.provider.AutoChannel
import top.learningman.push.provider.Channel
import top.learningman.push.utils.Network
import top.learningman.push.utils.PermissionManager
import com.google.android.material.R as MaterialR

class MainActivity : AppCompatActivity() {

    private val repo by lazy {
        (application as MainApplication).repo
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var channel: Channel

    private val startSetting =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("MainActivity", "Setting Activity Result ${it.resultCode}")
            if ((it.resultCode and SettingsActivity.UPDATE_CHANNEL) == SettingsActivity.UPDATE_CHANNEL) {
                Log.d("MainActivity", "Update Channel")
                channel = AutoChannel.getInstance(this)
            } else if ((it.resultCode and SettingsActivity.UPDATE_USERNAME) == SettingsActivity.UPDATE_USERNAME) {
                Log.d("MainActivity", "Update User")
            }

            if (it.resultCode != 0) {
                refreshStatus()
                lifecycleScope.launch {
                    Network.updateClient(repo.getUser())
                }
                channel.setUserCallback(this, repo.getUser(), lifecycleScope)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "onCreate")

        installSplashScreen()
        super.onCreate(savedInstanceState)
        channel = AutoChannel.getInstance(this)
        if (!PermissionManager(this).ok()) {
            startActivity(Intent(this, SetupActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
            })
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goSetting.setOnClickListener {
            startSetting.launch(Intent(this, SettingsActivity::class.java))
        }

        binding.goHistory.setOnClickListener {
            if (status == RegStatus.SUCCESS) {
                startActivity(Intent(this, MessagesActivity::class.java))
            } else {
                Log.w("MainActivity", "Not registered $status")
                Toast.makeText(this, "请输入正确用户名", Toast.LENGTH_SHORT).show()
            }
        }

        val userid = repo.getUser()

        if (userid == Repo.PREF_USER_DEFAULT) {
            setStatus(RegStatus.NOT_SET)
        } else {
            refreshStatus()
        }

        channel.init(this)
    }

    private fun refreshStatus() {
        setStatus(RegStatus.PENDING)

        lifecycleScope.launch {
            Network.check(repo.getUser())
                .onSuccess {
                    if (it) {
                        setStatus(RegStatus.SUCCESS)
                    } else {
                        setStatus(RegStatus.USERID_FAILED)
                    }
                }
                .onFailure {
                    setStatus(RegStatus.NETWORK_FAILED)
                }
        }
    }

    private var status = RegStatus.NOT_SET

    @SuppressLint("ResourceType")
    private fun setStatus(status: RegStatus) {
        this.status = status

        data class StatusData(
            val success: Boolean,
            val text: String,
            val icon: IconValue
        )

        val statusMap = mapOf(
            RegStatus.SUCCESS to StatusData(
                true,
                getString(R.string.userid_success),
                IconValue.CHECK
            ),
            RegStatus.PENDING to StatusData(
                false,
                getString(R.string.loading),
                IconValue.SYNC
            ),
            RegStatus.NETWORK_FAILED to StatusData(
                false,
                getString(R.string.connect_err),
                IconValue.SYNC_ALERT
            ),
            RegStatus.USERID_FAILED to StatusData(
                false,
                getString(R.string.userid_failed),
                IconValue.ACCOUNT_ALERT
            ),
            RegStatus.NOT_SET to StatusData(
                false,
                getString(R.string.not_set_userid_err),
                IconValue.ALERT_CIRCLE_OUTLINE
            )
        )
        runOnUiThread {
            val currentStatus = statusMap[status] ?: return@runOnUiThread
            binding.regStatusText.text = currentStatus.text
            if (currentStatus.success) {
                binding.channelStatusText.visibility = View.VISIBLE
                binding.channelStatusText.text = channel.name

                val colorSurfaceVariant = MaterialColors.getColor(
                    this,
                    MaterialR.attr.colorSurfaceVariant,
                    MaterialR.color.m3_sys_color_light_surface_variant
                )
                val colorOnSurfaceVariant = MaterialColors.getColor(
                    this,
                    MaterialR.attr.colorOnSurfaceVariant,
                    MaterialR.color.m3_sys_color_light_on_surface_variant
                )

                binding.cardView.setCardBackgroundColor(ColorStateList.valueOf(colorSurfaceVariant))
                binding.channelStatusText.setTextColor(colorOnSurfaceVariant)
                binding.regStatusText.setTextColor(colorOnSurfaceVariant)
                binding.regStatusIcon.setColor(colorOnSurfaceVariant)
            } else {
                binding.channelStatusText.visibility = View.GONE
                binding.channelStatusText.visibility = View.GONE

            }
            binding.regStatusIcon.setIcon(currentStatus.icon)
        }
    }


    companion object {

        enum class RegStatus {
            SUCCESS,
            PENDING,
            NETWORK_FAILED,
            USERID_FAILED,
            NOT_SET
        }
    }
}