package top.learningman.push

object Constant {
    private val HOST = if (!BuildConfig.DEBUG) "192.168.1.103:14444" else "push.learningman.top"

    const val APP_CENTER_SECRET = "0c045975-212b-441d-9ee4-e6ab9c76f8a3"
    val API_ENDPOINT = if (!BuildConfig.DEBUG) "http://$HOST" else "https://$HOST"
    val API_WS_ENDPOINT = if (!BuildConfig.DEBUG) "ws://$HOST" else "wss://$HOST"
}