package top.learningman.push.channel

import android.content.Context
import kotlinx.coroutines.CoroutineScope

interface Channel {
    fun init(context: Context)
    fun should(context: Context): Boolean
    fun setCallback(context: Context, userID: String) {}
    fun setCallback(context: Context, userID: String, scope: CoroutineScope) {}
}