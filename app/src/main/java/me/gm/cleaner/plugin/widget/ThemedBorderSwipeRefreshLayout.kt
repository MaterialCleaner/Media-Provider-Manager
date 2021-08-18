package me.gm.cleaner.plugin.widget

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.ThemedSwipeRefreshLayout
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.util.DisplayUtils.getDimenByAttr

class ThemedBorderSwipeRefreshLayout(context: Context, attrs: AttributeSet?) :
    ThemedSwipeRefreshLayout(context, attrs) {

    private fun init() {
        val actionBarSize = context.getDimenByAttr(android.R.attr.actionBarSize).toInt()
        setProgressViewOffset(false, actionBarSize, progressViewEndOffset + actionBarSize)
    }

    init {
        init()
    }
}
