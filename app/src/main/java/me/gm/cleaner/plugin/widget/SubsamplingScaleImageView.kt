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

package me.gm.cleaner.plugin.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.ImageViewState
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class StateSavedSubsamplingScaleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SubsamplingScaleImageView(context, attrs) {
    var uri: Uri? = null
        private set

    fun setImageUri(uri: Uri) {
        setImage(ImageSource.uri(uri))
        this.uri = uri
    }

    fun decodeImageUri(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = decodeImageUriInternal(uri).getOrElse {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                return@launch
            }
            setImage(ImageSource.bitmap(bitmap))
            this@StateSavedSubsamplingScaleImageView.uri = uri
        }
    }

    fun decodeImageUri(uri: Uri, state: ImageViewState) {
        lifecycleScope.launch {
            val bitmap = decodeImageUriInternal(uri).getOrElse {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                return@launch
            }
            setImage(ImageSource.bitmap(bitmap), state)
            this@StateSavedSubsamplingScaleImageView.uri = uri
        }
    }

    private val lifecycleScope: CoroutineScope
        get() = (context as AppCompatActivity).lifecycleScope

    private suspend fun decodeImageUriInternal(uri: Uri) = withContext(Dispatchers.IO) {
        runCatching {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
            BitmapFactory.decodeFileDescriptor(fd)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val ss = SavedState(super.onSaveInstanceState())
        ss.uri = uri
        ss.state = state
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        val uri = ss.uri ?: return
        val state = ss.state ?: return
        decodeImageUri(uri, state)
    }

    internal class SavedState : BaseSavedState {
        var uri: Uri? = null
        var state: ImageViewState? = null

        constructor(source: Parcel) : super(source) {
            uri = source.readParcelable(Uri::class.java.classLoader)
            state = source.readSerializable() as ImageViewState
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(uri, 0)
            out.writeSerializable(state)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}

class NestedScrollableSubsamplingScaleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : StateSavedSubsamplingScaleImageView(context, attrs) {
    private val vTranslate = PointF()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isImageLoaded) {
            handleInterceptTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    private fun handleInterceptTouchEvent(e: MotionEvent) {
        if (e.action == MotionEvent.ACTION_MOVE) {
            sourceToViewCoord(0F, 0F, vTranslate)
            var atXEdge = vTranslate.x >= 0F
            var atYEdge = vTranslate.y >= 0F
            sourceToViewCoord(sWidth.toFloat(), sHeight.toFloat(), vTranslate)
            atXEdge = atXEdge || vTranslate.x <= width.toFloat()
            atYEdge = atYEdge || vTranslate.y <= height.toFloat()
            if (atYEdge && !atXEdge) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        }
    }
}
