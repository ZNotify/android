package top.learningman.push.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import top.learningman.push.R
import java.util.*

class Repo(private val sharedPref: SharedPreferences) {

    fun getUser(): String {
        return sharedPref.getString(PREF_USER_KEY, PREF_USER_DEFAULT)!!
    }

    fun getChannel(): String? {
        return sharedPref.getString(PREF_CHANNEL_KEY, null)
    }

    fun getDeviceID(): String {
        val current = sharedPref.getString(PREF_DEVICE_ID_KEY, null)
        return if (current == null) {
            val new = UUID.randomUUID().toString()
            sharedPref.edit().putString(PREF_DEVICE_ID_KEY, new).apply()
            new
        } else {
            current
        }
    }

    companion object {
        private var instance: Repo? = null

        const val PREF_USER_KEY = "user_id"
        const val PREF_USER_DEFAULT = "plNjqo1n"
        const val PREF_DEVICE_ID_KEY = "device_id"
        var PREF_CHANNEL_KEY = ""


        fun getInstance(context: Context): Repo {
            PREF_CHANNEL_KEY = context.getString(R.string.pref_channel_key)
            return synchronized(Repo::class.java) {
                instance ?: let {
                    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
                    Repo(sharedPref).also {
                        instance = it
                    }
                }
            }
        }
    }
}