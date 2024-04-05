/*
 * Copyright 2023 Green Mushroom
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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

fun Context.buildSpannableString(
    text: CharSequence,
    style: Int = com.google.android.material.R.attr.textAppearanceBody2,
    color: Int? = colorPrimary
): SpannableStringBuilder = SpannableStringBuilder(text).apply {
    setSpan(
        TextAppearanceSpan(this@buildSpannableString, getResourceIdByAttr(style)), 0, length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    if (color != null) {
        setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    }
}

fun Context.startActivitySafe(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
    }
}

val Context.hasWifiTransport: Boolean
    get() {
        val connManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

inline fun <T : TypedArray, R> T.use(block: (T) -> R): R = try {
    block(this)
} finally {
    recycle()
}

fun Context.getDimenByAttr(@AttrRes attr: Int): Float =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getDimension(0, 0F)
    }

@ColorInt
fun Context.getColorByAttr(@AttrRes attr: Int): Int? =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getColorStateList(0)?.defaultColor
    }

fun Context.getColorStateListByAttr(@AttrRes attr: Int): ColorStateList? =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getColorStateList(0)
    }

fun Context.getDrawableByAttr(@AttrRes attr: Int): Drawable? =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getDrawable(0)
    }

@AnyRes
fun Context.getResourceIdByAttr(@AttrRes attr: Int, index: Int = 0): Int =
    obtainStyledAttributes(intArrayOf(attr)).use {
        it.getResourceId(index, 0)
    }

fun Context.dpToPx(dps: Int): Int {
    val density = resources.displayMetrics.density
    return (dps * density + 0.5F).toInt()
}

fun Context.pxToDp(px: Int): Int {
    val density = resources.displayMetrics.density
    return (px / density).toInt()
}
