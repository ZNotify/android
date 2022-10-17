package top.learningman.push.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import top.learningman.push.R
import top.learningman.push.utils.fromRFC3339Nano
import top.learningman.push.utils.toRFC3339Nano
import java.util.*

class Repo(private val sharedPref: SharedPreferences) {

    fun getUser(): String {
        return sharedPref.getString(PREF_USER_KEY, PREF_USER_DEFAULT)!!
    }

    fun getLastMessageTime(): String {
        return sharedPref.getString(PREF_LAST_MESSAGE_TIME_KEY, Date().toRFC3339Nano())!!
    }

    fun setLastMessageTime(time: String) {
        fun save(string: String) {
            sharedPref.edit().putString(PREF_LAST_MESSAGE_TIME_KEY, string).apply()
        }

        val rawCurrent = sharedPref.getString(PREF_LAST_MESSAGE_TIME_KEY, null)
        if (rawCurrent == null) {
            save(time)
        } else {
            val current = rawCurrent.fromRFC3339Nano()
            val new = time.fromRFC3339Nano()
            if (new.isAfter(current)) {
                save(time)
            }
        }


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
        const val PREF_LAST_MESSAGE_TIME_KEY = "last_message_time"
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