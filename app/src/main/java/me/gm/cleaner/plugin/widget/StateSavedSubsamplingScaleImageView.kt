package me.gm.cleaner.plugin.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.ImageViewState
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class StateSavedSubsamplingScaleImageView : SubsamplingScaleImageView {
    var imageCache: ImageSource? = null

    fun setImageSource(imageSource: ImageSource) {
        setImage(imageSource)
        imageCache = imageSource
    }

    fun setImageSource(imageSource: ImageSource, state: ImageViewState) {
        setImage(imageSource, state)
        imageCache = imageSource
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet?) : super(context, attr)

    override fun onSaveInstanceState(): Parcelable {
        val ss = SavedState(super.onSaveInstanceState())
        ss.image = imageCache
        ss.state = state
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        ss.image ?: return
        ss.state ?: return
        setImageSource(ss.image!!, ss.state!!)
    }

    internal class SavedState : BaseSavedState {
        var image: ImageSource? = null
        var state: ImageViewState? = null

        constructor(source: Parcel) : super(source) {
            image = source.readValue(ImageSource::class.java.classLoader) as ImageSource
            state = source.readSerializable() as ImageViewState
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeValue(image)
            out.writeSerializable(state)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
