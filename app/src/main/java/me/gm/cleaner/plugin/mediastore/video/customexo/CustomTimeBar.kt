/*
 * Copyright 2022 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore.video.customexo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.core.math.MathUtils.clamp
import androidx.core.view.ViewCompat
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.util.Util
import com.google.android.material.R
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.internal.DescendantOffsetUtils
import com.google.android.material.internal.ViewUtils
import com.google.android.material.tooltip.TooltipDrawable
import me.gm.cleaner.plugin.ktx.isRtl
import me.gm.cleaner.plugin.ktx.shortAnimTime
import kotlin.math.max

@SuppressLint("RestrictedApi")
class CustomTimeBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    timebarAttrs: AttributeSet? = null, defStyleRes: Int = 0
) : DefaultTimeBar(context, attrs, defStyleAttr, timebarAttrs, defStyleRes),
    TimeBar.OnScrubListener {
    private val label = context.obtainStyledAttributes(
        attrs, R.styleable.Slider, defStyleAttr, R.style.Widget_Material3_Slider
    ).use {
        TooltipDrawable.createFromAttributes(
            context, null, 0, it.getResourceId(
                R.styleable.Slider_labelStyle, R.style.Widget_Material3_Tooltip
            )
        )
    }

    private var labelAreAnimatedIn = false
    private var labelInAnimator: ValueAnimator? = null
    private var labelOutAnimator: ValueAnimator? = null

    private val widgetHeight = resources.getDimensionPixelOffset(R.dimen.mtrl_slider_widget_height)
    private val thumbRadius = dpToPx(density, DEFAULT_BAR_HEIGHT_DP) / 2
    private val labelPadding = resources.getDimensionPixelSize(R.dimen.mtrl_slider_label_padding)
    private var trackWidth = 0

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        label.text = Util.getStringForTime(formatBuilder, formatter, position)
        ensureLabelAdded()
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        label.text = Util.getStringForTime(formatBuilder, formatter, position)
        ensureLabelAdded()
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        ensureLabelRemoved()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTrackWidth(w)
    }

    private fun updateTrackWidth(width: Int) {
        // Update the visible track width.
        trackWidth = max(width, 0)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachLabelToContentView(label)
    }

    private fun attachLabelToContentView(label: TooltipDrawable) {
        label.setRelativeToView(ViewUtils.getContentView(this))
    }

    override fun onDetachedFromWindow() {
        labelAreAnimatedIn = false
        detachLabelFromContentView(label)
        super.onDetachedFromWindow()
    }

    private fun detachLabelFromContentView(label: TooltipDrawable) {
        overlay.remove(label)
        label.detachView(ViewUtils.getContentView(this))
    }

    private fun getAnimatorCurrentValueOrDefault(
        animator: ValueAnimator?, defaultValue: Float
    ): Float {
        // If the in animation is interrupting the out animation, attempt to smoothly interrupt by
        // getting the current value of the out animator.
        if (animator != null && animator.isRunning) {
            val value = animator.animatedValue as Float
            animator.cancel()
            return value
        }
        return defaultValue
    }

    private fun createLabelAnimator(enter: Boolean): ValueAnimator? {
        var startFraction = if (enter) 0f else 1f
        // Update the start fraction to the current animated value of the label, if any.
        startFraction = getAnimatorCurrentValueOrDefault(
            if (enter) labelOutAnimator else labelInAnimator, startFraction
        )
        val endFraction = if (enter) 1f else 0f
        val animator = ValueAnimator.ofFloat(startFraction, endFraction)
        animator.duration = context.shortAnimTime
        animator.interpolator =
            if (enter) AnimationUtils.DECELERATE_INTERPOLATOR else AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            label.setRevealFraction(fraction)
            // Ensure the labels are redrawn even if the slider has stopped moving
            ViewCompat.postInvalidateOnAnimation(this)
        }
        return animator
    }

    private fun ensureLabelAdded() {
        // If the labels are not animating in, start an animator to show them. ensureLabelsAdded will
        // be called multiple times by BaseSlider's draw method, making this check necessary to avoid
        // creating and starting an animator for each draw call.
        if (!labelAreAnimatedIn) {
            labelAreAnimatedIn = true
            labelInAnimator = createLabelAnimator(true)
            labelOutAnimator = null
            labelInAnimator?.start()
        }

        // Now set the label for the focused thumb so it's on top.
        val value = scrubPosition.toFloat() / duration.toFloat()
        setValueForLabel(label, value)
    }

    private fun ensureLabelRemoved() {
        // If the labels are animated in or in the process of animating in, create and start a new
        // animator to animate out the labels and remove them once the animation ends.
        if (labelAreAnimatedIn) {
            labelAreAnimatedIn = false
            labelOutAnimator = createLabelAnimator(false)
            labelInAnimator = null
            labelOutAnimator?.addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        overlay.remove(label)
                    }
                })
            labelOutAnimator?.start()
        }
    }

    /**
     * Returns a number between 0 and 1 indicating where on the track this value should sit with 0
     * being on the far left, and 1 on the far right.
     */
    private fun normalizeValue(value: Float): Float {
        val normalized = (value - valueFrom) / (valueTo - valueFrom)
        return if (context.resources.configuration.isRtl) 1 - normalized else normalized
    }

    private fun setValueForLabel(label: TooltipDrawable, value: Float) {
        var left =
            (scrubberPadding + normalizeValue(value) * (trackWidth - 2 * scrubberPadding)).toInt() - label.intrinsicWidth / 2
        left = clamp(
            left,
            2 * scrubberPadding - label.intrinsicWidth / 2,
            trackWidth - 2 * scrubberPadding - label.intrinsicWidth / 2
        )
        val top = widgetHeight / 2 - (labelPadding + thumbRadius)
        label.setBounds(left, top - label.intrinsicHeight, left + label.intrinsicWidth, top)

        // Calculate the difference between the bounds of this view and the bounds of the root view to
        // correctly position this view in the overlay layer.
        val rect = Rect(label.bounds)
        DescendantOffsetUtils.offsetDescendantRect(ViewUtils.getContentView(this)!!, this, rect)
        label.bounds = rect
        ViewUtils.getContentViewOverlay(this)?.add(label)
    }

    companion object {
        const val valueFrom = 0F
        const val valueTo = 1F
    }
}
