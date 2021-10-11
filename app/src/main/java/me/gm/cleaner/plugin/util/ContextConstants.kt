package me.gm.cleaner.plugin.util

import android.content.Context
import androidx.annotation.ColorInt

val Context.colorPrimary: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorPrimary)

val Context.colorControlHighlight: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorControlHighlight)

val Context.shortAnimTime
    get() = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

val Context.mediumAnimTime
    get() = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

val Context.longAnimTime
    get() = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
