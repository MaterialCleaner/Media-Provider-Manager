package androidx.swiperefreshlayout.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.util.DisplayUtils.getColorByAttr

open class ThemedSwipeRefreshLayout(context: Context, attrs: AttributeSet?) :
    SwipeRefreshLayout(context, attrs) {
    private fun init() {
        setColorSchemeColors(context.getColorByAttr(android.R.attr.colorPrimary))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val child = childView
        if (child != null) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(
                child.measuredWidth + paddingLeft + paddingRight,
                child.measuredHeight + paddingTop + paddingBottom
            )
        }
    }

    private val childView: View?
        get() {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != mCircleView) {
                    return child
                }
            }
            return null
        }

    init {
        init()
    }
}
