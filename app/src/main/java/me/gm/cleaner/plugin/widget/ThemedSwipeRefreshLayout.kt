package me.gm.cleaner.plugin.widget

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.ThemedSwipeRefreshLayout
import me.gm.cleaner.plugin.ktx.getDimenByAttr

class ThemedSwipeRefreshLayout(context: Context, attrs: AttributeSet?) :
    ThemedSwipeRefreshLayout(context, attrs) {

    private fun init() {
        val actionBarSizeAddTabHeight = context.getDimenByAttr(android.R.attr.actionBarSize).toInt()
        setProgressViewOffset(
            false, actionBarSizeAddTabHeight, progressViewEndOffset + actionBarSizeAddTabHeight
        )
    }

    init {
        init()
    }
}
