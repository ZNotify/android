package top.learningman.push.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import top.learningman.push.Constant
import top.learningman.push.entity.Message
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Network {
    private val client = OkHttpClient()
    private val apiEndpointBuilder
        get() = Constant.API_ENDPOINT.toHttpUrlOrNull()!!.newBuilder()

    suspend fun requestDelete(userID: String, msgID: String): Result<Response> {
        val url = apiEndpointBuilder
            .addPathSegment(userID)
            .addPathSegment(msgID)
            .build()
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()
        return kotlin.runCatching {
            val response = client.newCall(request).await()
            if (response.code != 200) {
                throw Exception("delete failed")
            }
            return@runCatching response
        }
    }

    suspend fun check(userID: String): Result<Response> {
        val url = apiEndpointBuilder
            .addPathSegment(userID)
            .addPathSegment("check")
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        return runCatching {
            val response = client.newCall(request).await()
            val responseText = response.body?.string() ?: throw Exception("No response")
            if (responseText != "true") {
                throw Exception("check failed")
            }
            return@runCatching response
        }
    }

    suspend fun reportFCMToken(userID: String, token: String): Result<Response> {
        val url = apiEndpointBuilder
            .addPathSegment(userID)
            .addPathSegment("fcm")
            .addPathSegment("token")
            .build()
        val request = Request.Builder()
            .url(url)
            .put(token.toRequestBody("text/plain".toMediaTypeOrNull()))
            .build()
        return runCatching {
            val response = client.newCall(request).await()
            if (response.code != 200) {
                throw Exception("report failed")
            }
            return@runCatching response
        }
    }

    suspend fun fetchMessage(userID: String): Result<List<Message>> {
        val url = apiEndpointBuilder
            .addPathSegment(userID)
            .addPathSegment("record")
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        return runCatching {
            val response = client.newCall(request).await()
            val body = response.body?.string() ?: throw Exception("no body")
            return@runCatching Gson()
                .fromJson<List<Message>?>(body, object :
                    TypeToken<List<Message>>() {}.type)
        }
    }
}

private suspend fun Call.await(recordStack: Boolean = false): Response {
    val callStack = if (recordStack) {
        IOException().apply {
            stackTrace = stackTrace.copyOfRange(1, stackTrace.size)
        }
    } else {
        null
    }
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                callStack?.initCause(e)
                continuation.resumeWithException(callStack ?: e)
            }
        })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (_: Throwable) {
            }
        }
    }
}