package top.learningman.push

object Constant {
    private val ENDPOINT_LIST = arrayOf("push.learningman.top", "192.168.1.103:14444")
    private const val ENDPOINT_INDEX = 0

    private val HOST:String
    get() {
        if (BuildConfig.DEBUG){
            return ENDPOINT_LIST[ENDPOINT_INDEX]
        }
        return ENDPOINT_LIST[0]
    }

    private val USE_SECURE_PROTOCOL: Boolean
    get() {
        if (BuildConfig.DEBUG){
            return true
        }
        return true
    }

    private val HTTP_PROTOCOL:String
    get() {
        if (USE_SECURE_PROTOCOL){
            return "https://"
        }
        return "http://"
    }

    private val WEBSOCKET_PROTOCOL:String
    get() {
        if (USE_SECURE_PROTOCOL){
            return "wss://"
        }
        return "ws://"
    }

    const val APP_CENTER_SECRET = "0c045975-212b-441d-9ee4-e6ab9c76f8a3"
    val API_ENDPOINT = "${HTTP_PROTOCOL}${HOST}"
    val API_WS_ENDPOINT = "${WEBSOCKET_PROTOCOL}${HOST}"
}