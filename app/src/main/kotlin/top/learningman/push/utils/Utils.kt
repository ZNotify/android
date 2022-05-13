package top.learningman.push.utils

import android.content.Context
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object Utils {
    fun isXiaoMi(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    fun isGMS(context: Context): Boolean {
        val ga = GoogleApiAvailability.getInstance()
        return when (ga.isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> true
            else -> false
        }
    }
}