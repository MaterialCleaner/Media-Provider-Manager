package me.gm.cleaner.plugin.widget

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.ImageViewState
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class StateSavedSubsamplingScaleImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SubsamplingScaleImageView(context, attrs) {
    var uri: Uri? = null

    fun setImageUri(uri: Uri) {
        setImage(ImageSource.uri(uri))
        this.uri = uri
    }

    fun setImageUri(uri: Uri, state: ImageViewState) {
        setImage(ImageSource.uri(uri), state)
        this.uri = uri
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
        setImageUri(uri, state)
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
