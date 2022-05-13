package top.learningman.push.entity

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder

data class UIMessage(
    val icon: MaterialDrawableBuilder.IconValue,
    val color: Int,
    val reason: String
)