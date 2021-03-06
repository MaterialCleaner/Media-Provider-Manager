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
import androidx.annotation.ColorInt

val Context.colorPrimary: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorPrimary)!!

val Context.colorAccent: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorAccent)!!

val Context.colorSurface: Int
    @ColorInt
    get() = getColorByAttr(com.google.android.material.R.attr.colorSurface)!!

val Context.colorControlHighlight: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.colorControlHighlight)!!

val Context.textColorPrimary: Int
    @ColorInt
    get() = getColorByAttr(android.R.attr.textColorPrimary)!!

val Context.shortAnimTime
    get() = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

val Context.mediumAnimTime
    get() = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

val Context.longAnimTime
    get() = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
