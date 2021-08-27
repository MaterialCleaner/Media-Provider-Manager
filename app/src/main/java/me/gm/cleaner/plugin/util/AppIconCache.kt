package me.gm.cleaner.plugin.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.widget.ImageView
import kotlinx.coroutines.*
import me.gm.cleaner.plugin.R
import me.zhanghai.android.appiconloader.AppIconLoader
import rikka.core.util.BuildUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

object AppIconCache : CoroutineScope {

    override val coroutineContext: CoroutineContext get() = Dispatchers.Main

    private val dispatcher: CoroutineDispatcher

    private var appIconLoaders = ConcurrentHashMap<Int, AppIconLoader>()

    init {
        // Initialize load icon scheduler
        val availableProcessorsCount = try {
            Runtime.getRuntime().availableProcessors()
        } catch (ignored: Exception) {
            1
        }
        val threadCount = 1.coerceAtLeast(availableProcessorsCount / 2)
        val loadIconExecutor: Executor = Executors.newFixedThreadPool(threadCount)
        dispatcher = loadIconExecutor.asCoroutineDispatcher()
    }

    @SuppressLint("NewApi")
    fun getOrLoadBitmap(context: Context, info: ApplicationInfo, userId: Int, size: Int): Bitmap {
        var loader = appIconLoaders[size]
        if (loader == null) {
            val shrinkNonAdaptiveIcons =
                BuildUtils.atLeast30 && context.applicationInfo.loadIcon(context.packageManager) is AdaptiveIconDrawable
            loader = AppIconLoader(size, shrinkNonAdaptiveIcons, context)
            appIconLoaders[size] = loader
        }
        return loader.loadIcon(info, false)
    }

    @JvmStatic
    fun loadIconBitmapAsync(
        context: Context, info: ApplicationInfo, userId: Int, view: ImageView
    ): Job = launch {
        val size = view.measuredWidth.let {
            if (it > 0) it else context.resources.getDimensionPixelSize(R.dimen.large_icon_size)
        }

        val bitmap = try {
            withContext(dispatcher) {
                getOrLoadBitmap(context, info, userId, size)
            }
        } catch (e: CancellationException) {
            // do nothing if canceled
            return@launch
        } catch (e: Throwable) {
            null
        }

        if (bitmap != null) {
            view.setImageBitmap(bitmap)
        } else {
            view.setImageDrawable(null)
        }
    }
}
