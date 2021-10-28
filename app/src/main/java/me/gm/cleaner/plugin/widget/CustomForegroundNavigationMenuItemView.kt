package me.gm.cleaner.plugin.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.appcompat.view.menu.MenuView.ItemView
import com.google.android.material.R
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import me.gm.cleaner.plugin.util.colorControlHighlight
import me.gm.cleaner.plugin.util.dipToPx

@SuppressLint("RestrictedApi", "PrivateResource")
class CustomForegroundNavigationMenuItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0
) : NavigationMenuItemView(context, attrs, defStyleAttr), ItemView {

    init {
        val materialShapeDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel
                .builder(context, R.style.ShapeAppearanceOverlay_Material3_NavigationView_Item, 0)
                .build()
        )
        val inset = context.dipToPx(12F)
        foreground = RippleDrawable(
            ColorStateList.valueOf(context.colorControlHighlight), null,
            InsetDrawable(materialShapeDrawable, inset, 0, inset, 0)
        )
    }
}
