package top.learningman.mipush

import android.content.Context
import android.content.Intent
import android.os.Message
import com.xiaomi.mipush.sdk.*
import net.steamcrafted.materialiconlib.MaterialDrawableBuilder
import top.learningman.mipush.entity.UIMessage
import kotlin.concurrent.thread

class MiPushReceiver : PushMessageReceiver() {
    companion object {
        enum class ActionEnum {
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
                uiMsgObj = UIMessage(
                    MaterialDrawableBuilder.IconValue.CHECK,
                    R.color.reg_success,
                    context.getString(R.string.reg_success)
                )
                uiMsgWhat = ActionEnum.REG_SUCCESS
            } else {
                uiMsgObj =
                    UIMessage(
                        MaterialDrawableBuilder.IconValue.ALERT,
                        R.color.reg_failed,
                        context.getString(R.string.reg_failed)
                    )
                uiMsgWhat = ActionEnum.REG_FAILED
            }

            val uiMsg = Message.obtain()
            uiMsg.obj = uiMsgObj
            uiMsg.what = uiMsgWhat.ordinal
            thread {
                while (!MainApplication.isHandlerInit()) {
                    Thread.sleep(100) // This is a ugly trick. Finding a better solution.
                }
                MainApplication.handler.sendMessage(uiMsg)
            }
        }
    }
}