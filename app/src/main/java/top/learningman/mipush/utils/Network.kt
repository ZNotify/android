package top.learningman.mipush.utils

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import top.learningman.mipush.BuildConfig

object Network {
    fun requestDelete(userID: String, msgID: String) {
        val client = OkHttpClient()
        val url = BuildConfig.APIURL.toHttpUrlOrNull()!!.newBuilder()
            .addPathSegment(userID)
            .addPathSegment(msgID)
            .build()
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.code != 200) {
                throw Exception("delete failed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}