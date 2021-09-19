package me.gm.cleaner.plugin.widget

import android.content.Context
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

    internal class SavedState(superState: Parcelable?) : BaseSavedState(superState) {
        var image: ImageSource? = null
        var state: ImageViewState? = null
    }
}
