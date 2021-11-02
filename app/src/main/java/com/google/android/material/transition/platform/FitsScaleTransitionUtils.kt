package com.google.android.material.transition.platform

import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.lang.Float.max
import java.lang.Float.min

object FitsScaleTransitionUtils {
    @JvmStatic
    fun getLocationOnScreen(view: View): RectF {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        var left = location[0].toFloat()
        var top = location[1].toFloat()
        if (view is SubsamplingScaleImageView) {
            val vTarget = PointF()
            view.sourceToViewCoord(0F, 0F, vTarget)
            // We don't need this fix when the source height is greater than the screen height.
            if (vTarget.y > 0) {
                left = max(left, left + vTarget.x)
                top = max(top, top + vTarget.y)
                val width = min(view.scale * view.sWidth, view.width.toFloat())
                val height = min(view.scale * view.sHeight, view.height.toFloat())
                val right = left + width
                val bottom = top + height
                return RectF(left, top, right, bottom)
            }
        }
        val right = left + view.width
        val bottom = top + view.height
        return RectF(left, top, right, bottom)
    }
}
