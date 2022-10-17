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
    fun release(context: Context)

    fun available(context: Context): Boolean {
        return true
    }

    fun granted(context: Context): Boolean {
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

val channels = arrayOf(FCM, WebSocket)

fun getChannel(name: String): Channel? {
    return channels.firstOrNull { it.name == name }
}

class AutoChannel private constructor(channel: Channel) : Channel by channel {
    companion object {
        private var instance: Channel? = null
        fun by(chan: Channel): Boolean {
            if (instance == null) {
                return false
            }
            return instance!!.name == chan.name
        }

        fun updateInstance(context: Context, channelID: String) {
            val channel = getChannel(channelID)?.takeIf { it.available(context) }
            if (channel != null) {
                instance = channel
            }
        }

        fun updateInstance(chan: Channel) {
            instance = chan
        }

        fun getInstance(context: Context): Channel {
            return if (instance != null) {
                instance as Channel
            } else {
                var impl: Channel? = null
                val channelID = Repo.getInstance(context).getChannel()
                if (channelID != null) {
                    impl = getChannel(channelID).takeIf { it != null && it.available(context) }
                }

                if (impl == null) {
                    impl = when {
                        FCM.available(context) -> FCM
                        else -> WebSocket
                    }
                }

                instance = AutoChannel(impl)
                instance as Channel
            }
        }
    }
}