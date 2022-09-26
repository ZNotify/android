package top.learningman.push.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.microsoft.appcenter.distribute.Distribute
import top.learningman.push.BuildConfig
import top.learningman.push.R
import top.learningman.push.databinding.ActivitySettingsBinding
import top.learningman.push.provider.AutoChannel
import top.learningman.push.provider.Channel
import top.learningman.push.provider.channels

class SettingsActivity : AppCompatActivity() {
    private var nextChannel: Channel? = null

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
                    Log.d("SettingsActivity", "Version Clicked")
                    Distribute.checkForUpdate()
                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    true
                }
                summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            }

            findPreference<ListPreference>(getString(R.string.pref_channel_key))?.apply {
                entries = channels
                    .filter {
                        it.should(context)
                    }.map {
                        it.name
                    }.toTypedArray()
                entryValues = entries
                if (value == null){
                    value = AutoChannel.getInstance(context).name
                }

                setOnPreferenceChangeListener { _, newValue ->
                    val next = AutoChannel.getInstance(context, newValue as String)
                    if (!next.available(context)){
                        Toast.makeText(context, getString(R.string.pref_grant_permission_tip), Toast.LENGTH_LONG).show()
                        startActivity(Intent(context, SetupActivity::class.java).apply {
                            action = SetupActivity.PERMISSION_GRANT_ACTION
                        })
                        (requireActivity() as SettingsActivity).nextChannel = next
                    } else {
                        next.init(context)
                    }
                    true
                }
            } ?: run {
                Log.e("SettingsActivity", "Cannot find channel preference")
            }

        }
    }

    override fun onResume() {
        super.onResume()
        if (nextChannel != null){
            nextChannel?.init(this)
            nextChannel = null
        }
    }
}