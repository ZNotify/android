package top.learningman.mipush

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder

interface UIMessage {
    val icon: MaterialDrawableBuilder.IconValue
    val reason: String
    val color: Int
}