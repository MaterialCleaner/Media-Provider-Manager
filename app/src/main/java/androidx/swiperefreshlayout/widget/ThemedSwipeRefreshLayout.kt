package androidx.swiperefreshlayout.widget

import android.content.Context
import android.util.AttributeSet
import me.gm.cleaner.plugin.ktx.colorAccent

class ThemedSwipeRefreshLayout(context: Context, attrs: AttributeSet?) :
    SwipeRefreshLayout(context, attrs) {

    init {
        setColorSchemeColors(context.colorAccent)
        setProgressViewOffset(false, 0, progressViewEndOffset)
    }
}
