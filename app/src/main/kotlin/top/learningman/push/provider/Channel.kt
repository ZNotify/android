package top.learningman.push.provider

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import top.learningman.push.data.Repo

interface Permission {
    val name: String
    val description: String

    fun check(context: Context): Boolean?
    fun grant(activity: Activity)
}

interface Channel {
    val name: String

    fun init(context: Context)
    fun should(context: Context): Boolean {
        return true
    }

    fun available(context: Context): Boolean {
        val permissions = permissions()
        return if (permissions.isEmpty()) {
            true
        } else {
            permissions.all {
                it.check(context) ?: true
            }
        }
    }

    fun permissions(): List<Permission> {
        return emptyList()
    }

    fun setUserCallback(context: Context, userID: String) {}
    fun setUserCallback(context: Context, userID: String, scope: CoroutineScope) {
        setUserCallback(context, userID)
    }
}

val channels = arrayOf(FCM, Host)

class AutoChannel private constructor(channel: Channel) : Channel by channel {
    companion object {
        private var instance: Channel? = null
        fun belong(chan: Channel): Boolean {
            if (instance == null) {
                return false
            }
            return instance!!.name == chan.name
        }

        fun getInstance(context: Context, channelText: String? = null): Channel {
            return if (instance != null && channelText == null) {
                instance as Channel
            } else {
                var impl: Channel? = null
                if (channelText == null) {
                    val channelID = Repo.getInstance(context).getChannel()
                    if (channelID != null) {
                        impl = channels.firstOrNull { it.name == channelID }
                    }
                } else {
                    impl = channels.firstOrNull { it.name == channelText }
                }

                if (impl == null) {
                    impl = when {
                        FCM.should(context) -> FCM
                        else -> Host
                    }
                }

                instance = AutoChannel(impl)
                instance as Channel
            }
        }
    }
}