package top.learningman.push.utils

import android.content.Context
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import java.util.*


object Markwon {
    private val cache = Collections.synchronizedMap(WeakHashMap<Context, Markwon>())

    fun getInstance(context: Context): Markwon {
        return cache.getOrPut(context) {
            return Markwon.builder(context)
                .usePlugin(ImagesPlugin.create { plugin ->
                    plugin.addSchemeHandler(
                        OkHttpNetworkSchemeHandler.create()
                    )
                })
                .usePlugin(TablePlugin.create(context))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        super.configureConfiguration(builder)
                        builder.linkResolver { _, link ->
                            val intent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            val uri = runCatching { link.toUri() }.getOrElse {
                                Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                                return@linkResolver
                            }
                            runCatching {
                                intent.launchUrl(context, uri)
                            }.onFailure {
                                Toast.makeText(context, "无法打开链接\n$uri", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
                .build()
        }
    }
}