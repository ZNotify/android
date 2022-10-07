package top.learningman.push.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.zxilly.lib.upgrader.Upgrader
import dev.zxilly.notify.sdk.Client
import kotlinx.coroutines.runBlocking
import top.learningman.push.BuildConfig
import top.learningman.push.Constant
import top.learningman.push.R
import top.learningman.push.databinding.ActivitySettingsBinding
import top.learningman.push.playUpgrade
import top.learningman.push.provider.AutoChannel
import top.learningman.push.provider.channels
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("version")?.apply {
                setOnPreferenceClickListener {
                    @Suppress("KotlinConstantConditions")
                    when(BuildConfig.FLAVOR){
                        "free" -> {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/ZNotify/android/releases")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                        "play" -> {
                            playUpgrade(requireActivity())
                        }
                        else -> {
                            Upgrader.getInstance()?.tryUpgrade(false)
                                ?: let {
                                    Toast.makeText(context, "应用内更新未生效", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    true
                }

                summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            }

            findPreference<EditTextPreference>(getString(R.string.pref_user_id_key))?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    thread {
                        runBlocking {
                            val ret = Client.check(newValue as String, Constant.API_ENDPOINT)
                            if (!ret) {
                                Log.d("SettingsActivity", "User ID is invalid")
                                val activity = runCatching { requireActivity() }
                                activity.getOrNull()?.runOnUiThread {
                                    Toast.makeText(context, "不是有效的用户ID", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    (requireActivity() as SettingsActivity).updateResult(UPDATE_USERNAME)
                    true
                }
            }

            findPreference<ListPreference>(getString(R.string.pref_channel_key))?.apply {
                entries = channels
                    .filter {
                        it.should(context)
                    }.map {
                        it.name
                    }.toTypedArray()
                entryValues = entries
                if (value == null) {
                    value = AutoChannel.getInstance(context).name
                }

                setOnPreferenceChangeListener { _, newValue ->
                    val next = AutoChannel.getInstance(context, newValue as String)
                    if (!next.available(context)) {
                        Toast.makeText(
                            context,
                            getString(R.string.pref_grant_permission_tip),
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(context, SetupActivity::class.java).apply {
                            action = SetupActivity.PERMISSION_GRANT_ACTION
                        })
                    }
                    (requireActivity() as SettingsActivity).updateResult(UPDATE_CHANNEL)
                    true
                }
            } ?: run {
                Log.e("SettingsActivity", "Cannot find channel preference")
            }

        }
    }

    override fun onResume() {
        super.onResume()
    }

    private var resultCode = 0
    private fun updateResult(result: Int) {
        resultCode = resultCode or result
        setResult(resultCode)
    }

    companion object {
        const val UPDATE_USERNAME = 1 shl 0
        const val UPDATE_CHANNEL = 1 shl 1
    }
}