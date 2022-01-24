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

package me.gm.cleaner.plugin.ktx

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.icu.text.ListFormatter
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatDrawableManager

fun Collection<*>.listFormat() = ListFormatter.getInstance().format(this)

fun Context.buildStyledTitle(text: CharSequence, color: Int = colorAccent) =
    SpannableStringBuilder(text).apply {
        setSpan(
            TextAppearanceSpan(
                this@buildStyledTitle, androidx.appcompat.R.style.TextAppearance_AppCompat_Body2
            ), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    }

@SuppressLint("RestrictedApi")
fun Context.getBitmapFromVectorDrawable(@DrawableRes drawableId: Int): Bitmap {
    val drawable = AppCompatDrawableManager.get().getDrawable(this, drawableId)
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.setTint(colorAccent)
    drawable.draw(canvas)
    return bitmap
}

fun Context.getDimenByAttr(@AttrRes attr: Int) = obtainStyledAttributes(intArrayOf(attr)).use {
    it.getDimension(0, 0F)
}

@ColorInt
fun Context.getColorByAttr(@AttrRes attr: Int) = obtainStyledAttributes(intArrayOf(attr)).use {
    it.getColorStateList(0)?.defaultColor
}

fun Context.getDrawableByAttr(@AttrRes attr: Int) = obtainStyledAttributes(intArrayOf(attr)).use {
    it.getDrawable(0)
}

fun Context.dpToPx(dps: Int): Int {
    val density = resources.displayMetrics.density
    return (dps * density + 0.5F).toInt()
}

fun Context.pxToDp(px: Int): Int {
    val density = resources.displayMetrics.density
    return (px / density).toInt()
}
