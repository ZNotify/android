package top.learningman.mipush

import android.content.res.ColorStateList
import androidx.annotation.ColorInt

class Utils {
    companion object {
        fun colorStateListOf(@ColorInt color: Int): ColorStateList {
            return ColorStateList.valueOf(color)
        }
    }
}