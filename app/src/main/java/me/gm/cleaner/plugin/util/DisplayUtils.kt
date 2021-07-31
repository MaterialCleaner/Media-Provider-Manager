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

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import android.annotation.SuppressLint
import android.content.Context
import kotlin.jvm.Synchronized
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatDrawableManager
import me.gm.cleaner.plugin.R

object DisplayUtils {
    fun getDimenByAttr(context: Context, attr: Int): Float {
        val a = context.obtainStyledAttributes(intArrayOf(attr))
        val dimen = a.getDimension(0, 0f)
        a.recycle()
        return dimen
    }

    @ColorInt
    fun getColorByAttr(context: Context, attr: Int): Int {
        val a = context.obtainStyledAttributes(intArrayOf(attr))
        val color = a.getColorStateList(0)!!.defaultColor
        a.recycle()
        return color
    }

    @SuppressLint("RestrictedApi")
    @Synchronized
    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = AppCompatDrawableManager.get().getDrawable(context, drawableId)
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.setTint(getColorByAttr(context, R.attr.colorPrimary))
        drawable.draw(canvas)
        return bitmap
    }

    fun dipToPx(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    fun pxToDip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
}