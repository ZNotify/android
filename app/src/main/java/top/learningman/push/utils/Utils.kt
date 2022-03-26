package top.learningman.push.utils

import android.os.Build

object Utils {
    fun isXiaoMi(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }
}