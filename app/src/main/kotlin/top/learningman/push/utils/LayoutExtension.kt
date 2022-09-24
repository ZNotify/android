package top.learningman.push.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt


fun Dp.toPX(context: Context): Int {
    val r: Resources = context.resources
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.value, r.displayMetrics
    ).roundToInt()
}