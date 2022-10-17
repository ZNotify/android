package top.learningman.push.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.microsoft.appcenter.crashes.Crashes
import dev.zxilly.notify.sdk.Client
import kotlinx.coroutines.runBlocking
import top.learningman.push.BuildConfig
import top.learningman.push.Constant
import top.learningman.push.R
import top.learningman.push.checkUpgrade
import top.learningman.push.databinding.ActivitySettingsBinding
import top.learningman.push.provider.AutoChannel
import top.learningman.push.provider.Channel
import top.learningman.push.provider.channels
import top.learningman.push.provider.getChannel
import java.util.*
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {
    var pendingInitChannel: Channel? = null

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

            findPreference<Preference>(getString(R.string.pref_version_key))?.apply {
                setOnPreferenceClickListener {
                    runCatching {
                        checkUpgrade(requireActivity())
                    }.onFailure {
                        Log.e("Upgrader", "Failed to check upgrade", it)
                        Crashes.trackError(it)
                    }
                    true
                }

                val summaryView: TextView? = view?.findViewById(android.R.id.summary)
                if (summaryView != null) {
                    summaryView.maxLines = 3
                    summaryView.isSingleLine = false
                }

                fun timeStampToFormattedString(timeStamp: Int): String {
                    return if (timeStamp == 0) {
                        "未知"
                    } else {
                        Log.d("Settings", "timeStamp: $timeStamp")
                        val date = Date((timeStamp.toLong() * 1000))
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                        format.format(date)
                    }
                }

                summary = "${BuildConfig.VERSION_NAME}\n"+
                        "${BuildConfig.VERSION_CODE}\n"+
                        timeStampToFormattedString(BuildConfig.VERSION_CODE)
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
                        it.available(context)
                    }.map {
                        it.name
                    }.toTypedArray()
                entryValues = entries
                if (value == null) {
                    value = AutoChannel.getInstance(context).name
                }

                setOnPreferenceChangeListener { _, newValue ->
                    val old = getChannel(value)
                    val next = getChannel(newValue as String)
                    if (old != null && next != null) {
                        old.release(context)

                        if (next.granted(context)) {
                            next.init(context)
                        } else {
                            (requireActivity() as SettingsActivity).pendingInitChannel = next
                            Toast.makeText(
                                context,
                                getString(R.string.pref_grant_permission_tip),
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(context, SetupActivity::class.java).apply {
                                action = SetupActivity.PERMISSION_GRANT_ACTION
                            })
                        }
                        AutoChannel.updateInstance(next)
                        (requireActivity() as SettingsActivity).updateResult(UPDATE_CHANNEL)
                    } else {
                        Log.e("SettingsActivity", "Failed to update channel")
                        if (old == null) {
                            Log.e("SettingsActivity", "Old channel $value is null")
                        }
                        if (next == null) {
                            Log.e("SettingsActivity", "New channel $newValue is null")
                        }
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

        if (pendingInitChannel != null) {
            pendingInitChannel?.init(this)
            pendingInitChannel = null
        }
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