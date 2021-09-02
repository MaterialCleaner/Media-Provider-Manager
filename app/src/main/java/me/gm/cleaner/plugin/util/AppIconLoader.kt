package me.gm.cleaner.plugin.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.annotation.Px
import androidx.core.os.UserHandleCompat
import me.zhanghai.android.appiconloader.iconloaderlib.BaseIconFactory
import me.zhanghai.android.appiconloader.iconloaderlib.BitmapInfo

class AppIconLoader(
    @field:Px @param:Px private val mIconSize: Int, private val mShrinkNonAdaptiveIcons: Boolean,
    private val context: Context
) {
    @JvmOverloads
    fun loadIcon(applicationInfo: ApplicationInfo, isInstantApp: Boolean = false): Bitmap {
        val unbadgedIcon = applicationInfo.loadUnbadgedIcon(context.packageManager)
        val user = UserHandleCompat.getUserHandleForUid(applicationInfo.uid)
        return IconFactory(mIconSize, context).createBadgedIconBitmap(
            unbadgedIcon, user, mShrinkNonAdaptiveIcons, isInstantApp
        ).icon
    }

    private class IconFactory(@Px iconBitmapSize: Int, context: Context) : BaseIconFactory(
        context, context.resources.configuration.densityDpi, iconBitmapSize, true
    ) {
        private val mTempScale = FloatArray(1)

        fun createBadgedIconBitmap(
            icon: Drawable, user: UserHandle, shrinkNonAdaptiveIcons: Boolean, isInstantApp: Boolean
        ): BitmapInfo {
            return super.createBadgedIconBitmap(
                icon, user, shrinkNonAdaptiveIcons, isInstantApp, mTempScale
            )
        }
    }

    init {
        // workaround MIUI
        try {
            @SuppressLint("PrivateApi")
            val clazz = Class.forName("android.graphics.drawable.AdaptiveIconDrawableInjector")
            val field = clazz.getDeclaredField("MASK_PAINT")
            field.isAccessible = true
            val maskPaint = field[null] as Paint
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST)
        } catch (e: Exception) {
        }
    }
}
