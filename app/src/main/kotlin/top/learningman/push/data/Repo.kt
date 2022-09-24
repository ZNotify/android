package top.learningman.push.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import top.learningman.push.entity.MessageAdapter.MessageHolder.Companion.toRFC3339Nano
import java.util.*

class Repo(private val sharedPref: SharedPreferences) {

    fun getUser(): String {
        return sharedPref.getString(PREF_USER_KEY, PREF_USER_DEFAULT)!!
    }

    fun getRestartWorkerVersion(): Int {
        return sharedPref.getInt(
            PREF_RESTART_WORKER_VERSION_KEY,
            PREF_RESTART_WORKER_VERSION_DEFAULT
        )
    }

    fun setRestartWorkerVersion(version: Int) {
        sharedPref.edit().putInt(PREF_RESTART_WORKER_VERSION_KEY, version).apply()
    }

    fun getMessageWorkerVersion(): Int {
        return sharedPref.getInt(
            PREF_MESSAGE_WORKER_VERSION_KEY,
            PREF_MESSAGE_WORKER_VERSION_DEFAULT
        )
    }

    fun setMessageWorkerVersion(version: Int) {
        sharedPref.edit().putInt(PREF_MESSAGE_WORKER_VERSION_KEY, version).apply()
    }

    fun getLastMessageTime(): String {
        return sharedPref.getString(PREF_LAST_MESSAGE_TIME_KEY, Date().toRFC3339Nano())!!
    }

    fun setLastMessageTime(time: String) {
        sharedPref.edit().putString(PREF_LAST_MESSAGE_TIME_KEY, time).apply()
    }

    companion object {
        private var instance: Repo? = null

        const val PREF_USER_KEY = "user_id"
        const val PREF_USER_DEFAULT = "plNjqo1n"
        const val PREF_RESTART_WORKER_VERSION_KEY = "restart_worker_version"
        const val PREF_RESTART_WORKER_VERSION_DEFAULT = 0
        const val PREF_MESSAGE_WORKER_VERSION_KEY = "message_worker_version"
        const val PREF_MESSAGE_WORKER_VERSION_DEFAULT = 0
        const val PREF_LAST_MESSAGE_TIME_KEY = "last_message_time"


        fun getInstance(context: Context): Repo {
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