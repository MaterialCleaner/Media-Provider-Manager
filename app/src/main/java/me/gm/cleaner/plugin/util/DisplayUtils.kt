/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.appcompat.widget.AppCompatDrawableManager
import me.gm.cleaner.plugin.R
import java.text.DecimalFormat
import kotlin.math.roundToInt

object DisplayUtils {
    fun getFormattedLength(b: Float): String {
        if (b == 0f) return "0 B"
        val format = DecimalFormat("0.00")
        val k = b / 1024
        if (k < 1) return format.format(b.toDouble()) + " B"
        val m = k / 1024
        if (m < 1) return format.format(k.toDouble()) + " K"
        val g = m / 1024
        if (g < 1) return format.format(m.toDouble()) + " M"
        return format.format(g.toDouble()) + " G"
    }

    fun Context.getDimenByAttr(attr: Int): Float {
        val a = obtainStyledAttributes(intArrayOf(attr))
        val dimen = a.getDimension(0, 0f)
        a.recycle()
        return dimen
    }

    @ColorInt
    fun Context.getColorByAttr(attr: Int): Int {
        val a = obtainStyledAttributes(intArrayOf(attr))
        val color = a.getColorStateList(0)!!.defaultColor
        a.recycle()
        return color
    }

    fun withModulatedAlpha(
        @ColorInt value: Int, @FloatRange(from = 0.0, to = 1.0) alphaModulation: Float
    ): Int {
        val alpha = (Color.alpha(value) * alphaModulation).roundToInt()
        return alpha shl 24 or (value and 0x00FFFFFF)
    }

    @SuppressLint("RestrictedApi")
    @Synchronized
    fun Context.getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
        val drawable = AppCompatDrawableManager.get().getDrawable(this, drawableId)
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.setTint(getColorByAttr(android.R.attr.colorPrimary))
        drawable.draw(canvas)
        return bitmap
    }

    fun dipToPx(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).roundToInt()
    }

    fun pxToDip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).roundToInt()
    }
}
