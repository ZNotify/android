package top.learningman.push.provider

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope

interface Permission{
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

class AutoChannel private constructor(context: Context) : Channel {
    private val impl: Channel

    init {
        impl = when {
            FCM.should(context) -> FCM
            else -> Host
        }
    }

    override val name: String
        get() = impl.name

    override fun init(context: Context) {
        impl.init(context)
    }

    override fun permissions(): List<Permission> {
        return impl.permissions()
    }

    override fun setUserCallback(context: Context, userID: String) {
        impl.setUserCallback(context, userID)
    }

    override fun setUserCallback(context: Context, userID: String, scope: CoroutineScope) {
        impl.setUserCallback(context, userID, scope)
    }

    companion object {
        private var impl: Channel? = null
        fun getInstance(context: Context): Channel {
            if (impl == null) {
                impl = AutoChannel(context)
            }
            return impl!!
        }

        fun belong(chan: Channel): Boolean {
            if (impl == null) {
                return false
            }
            return impl!!.name == chan.name
        }

    }
}