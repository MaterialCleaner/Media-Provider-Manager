package me.gm.cleaner.plugin.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.SparseArray
import android.widget.ImageView
import androidx.collection.LruCache
import kotlinx.coroutines.*
import me.gm.cleaner.plugin.R
import rikka.core.util.BuildUtils
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

object AppIconCache : CoroutineScope {

    private class AppIconLruCache constructor(maxSize: Int) :
        LruCache<Triple<String, Int, Int>, Bitmap>(maxSize) {

        override fun sizeOf(key: Triple<String, Int, Int>, bitmap: Bitmap): Int =
            bitmap.byteCount / 1024
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val lruCache: LruCache<Triple<String, Int, Int>, Bitmap>

    private val dispatcher: CoroutineDispatcher

    private var appIconLoaders = SparseArray<AppIconLoader>()

    init {
        // Initialize app icon lru cache
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()
        lruCache = AppIconLruCache(availableCacheSize)

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

    private fun get(packageName: String, userId: Int, size: Int): Bitmap? =
        lruCache[Triple(packageName, userId, size)]

    private fun put(packageName: String, userId: Int, size: Int, bitmap: Bitmap) {
        get(packageName, userId, size) ?: lruCache.put(Triple(packageName, userId, size), bitmap)
    }

    private fun remove(packageName: String, userId: Int, size: Int) {
        lruCache.remove(Triple(packageName, userId, size))
    }

    @SuppressLint("NewApi")
    fun getOrLoadBitmap(context: Context, info: ApplicationInfo, userId: Int, size: Int): Bitmap {
        get(info.packageName, userId, size)?.let {
            return it
        }
        val loader = appIconLoaders[size] ?: let {
            val shrinkNonAdaptiveIcons =
                BuildUtils.atLeast30 && context.applicationInfo.loadIcon(context.packageManager) is AdaptiveIconDrawable
            val _loader = AppIconLoader(size, shrinkNonAdaptiveIcons, context)
            appIconLoaders[size] = _loader
            _loader
        }
        val bitmap = loader.loadIcon(info, false)
        put(info.packageName, userId, size, bitmap)
        return bitmap
    }

    @JvmStatic
    fun loadIconBitmapAsync(
        context: Context, info: ApplicationInfo, userId: Int, view: ImageView
    ): Job = launch {
        val size = view.measuredWidth.let {
            if (it > 0) it else context.resources.getDimensionPixelSize(R.dimen.icon_size)
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
            view.setImageDrawable(context.packageManager.defaultActivityIcon)
        }
    }
}
