package top.learningman.mipush

import android.content.Context
import android.os.Message
import com.xiaomi.mipush.sdk.ErrorCode
import com.xiaomi.mipush.sdk.MiPushClient
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder

class MiPushReceiver : PushMessageReceiver() {
    companion object{
        enum class ActionEnum{
            REG_SUCCESS,
            REG_FAILED
        }
    }

    override fun onReceiveRegisterResult(context: Context, msg: MiPushCommandMessage) {

        val command = msg.command
        val uiMsgObj: UIMessage
        val uiMsgWhat: ActionEnum

        if (command == MiPushClient.COMMAND_REGISTER) {
            if (msg.resultCode == ErrorCode.SUCCESS.toLong()) {
                uiMsgObj = object : UIMessage {
                    override val reason: String
                        get() = "注册成功"
                    override val color: Int
                        get() = R.color.reg_success
                    override val icon: MaterialDrawableBuilder.IconValue
                        get() = MaterialDrawableBuilder.IconValue.CHECK
                }
                uiMsgWhat = ActionEnum.REG_SUCCESS
            } else {
                uiMsgObj = object : UIMessage {
                    override val reason: String
                        get() = "注册失败"
                    override val color: Int
                        get() = R.color.reg_failed
                    override val icon: MaterialDrawableBuilder.IconValue
                        get() = MaterialDrawableBuilder.IconValue.ALERT
                }
                uiMsgWhat = ActionEnum.REG_FAILED
            }

            val uiMsg = Message.obtain()
            uiMsg.obj = uiMsgObj
            uiMsg.what = uiMsgWhat.ordinal
            MainApplication.handler.sendMessage(uiMsg)
        }
    }
}