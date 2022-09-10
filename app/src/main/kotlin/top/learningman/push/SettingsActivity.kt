package top.learningman.push

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.microsoft.appcenter.distribute.Distribute
import top.learningman.push.databinding.SettingsActivityBinding

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onAttach(context: Context) {
            super.onAttach(context)
            Log.d("SettingsActivity", "onAttach")
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("version")?.apply {
                Log.d("SettingsActivity", "Version Clicked")
                setOnPreferenceClickListener {
                    Distribute.checkForUpdate()
                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    true
                }
                summary = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            }
        }
    }
}