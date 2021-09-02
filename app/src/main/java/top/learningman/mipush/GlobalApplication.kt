package top.learningman.mipush

import android.app.Application

open class GlobalApplication : Application() {
    protected var userid_reliable = false

    public fun getUserIDReliable(): Boolean {
        return this.userid_reliable
    }

    public fun setUserIDReliable(userid_reliable: Boolean): Unit {
        this.userid_reliable = userid_reliable
    }
}