package top.learningman.push.utils

import android.content.Context
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler


object Markwon {
    fun getInstance(context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugin(ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(
                    OkHttpNetworkSchemeHandler.create()
                )
            })
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }
}