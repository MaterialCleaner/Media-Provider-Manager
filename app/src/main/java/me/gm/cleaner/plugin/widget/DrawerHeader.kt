package me.gm.cleaner.plugin.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes

class DrawerHeader @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    override fun onApplyWindowInsets(insets: WindowInsets?): WindowInsets {
        val localInsets = Rect()
        val result = computeSystemWindowInsets(insets, localInsets)
        applyInsets(localInsets)
        return result
    }

    private fun applyInsets(insets: Rect) {
        setPaddingRelative(insets.left, insets.top, 0, 0)
    }
}
