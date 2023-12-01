package top.learningman.push

object Constant {
    private val ENDPOINT_LIST = arrayOf("push.learningman.top", "192.168.1.111:14444")
    private const val ENDPOINT_INDEX = 1

    private val HOST: String
        get() {
            return ENDPOINT_LIST[ENDPOINT_INDEX]
        }

    private val USE_SECURE_PROTOCOL: Boolean
        get() {
            return true
        }

    private val HTTP_PROTOCOL: String
        get() {
            if (USE_SECURE_PROTOCOL) {
                return "https://"
            }
            return "http://"
        }

    private val WEBSOCKET_PROTOCOL: String
        get() {
            if (USE_SECURE_PROTOCOL) {
                return "wss://"
            }
            return "ws://"
        }

    const val APP_CENTER_SECRET = "0c045975-212b-441d-9ee4-e6ab9c76f8a3"

    val API_ENDPOINT = "${HTTP_PROTOCOL}${HOST}"
    val API_WS_ENDPOINT = "${WEBSOCKET_PROTOCOL}${HOST}"
}
