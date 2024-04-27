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

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.ColorInt

val Context.colorPrimary: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorPrimary)!!

val Context.colorPrimaryContainer: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorPrimaryContainer)!!

val Context.colorOnPrimaryContainer: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorOnPrimaryContainer)!!

val Context.colorAccent: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorAccent)!!

val Context.colorBackground: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorBackground)!!

val Context.colorBackgroundFloating: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorBackgroundFloating)!!

val Context.colorSurface: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorSurface)!!

val Context.colorOnSurface: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorOnSurface)!!

val Context.colorOnSurfaceVariant: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)!!

val Context.colorError: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorError)!!

val Context.colorControlNormal: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorControlNormal)!!

val Context.colorControlHighlight: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorControlHighlight)!!

val Context.textColorPrimary: ColorStateList
    get() = getColorStateListByAttr(android.R.attr.textColorPrimary)!!

val Context.textColorPrimaryInverse: ColorStateList
    get() = getColorStateListByAttr(android.R.attr.textColorPrimaryInverse)!!

val Context.navAnimTime: Long
    get() = resources.getInteger(androidx.navigation.ui.R.integer.config_navAnimTime).toLong()

val Context.shortAnimTime: Long
    get() = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

val Context.mediumAnimTime: Long
    get() = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

val Context.longAnimTime: Long
    get() = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
