/*
 * Copyright 2023 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.widget;

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ColorStateListDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.internal.ViewUtils.RelativePadding;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import me.gm.cleaner.plugin.R;

@SuppressLint("RestrictedApi")
public class BottomActionBar extends FrameLayout {
    private MenuInflater menuInflater;
    private final MenuBuilder menu;
    @NonNull
    private final BottomActionBarMenuView menuView;

    public BottomActionBar(@NonNull Context context) {
        this(context, null);
    }

    public BottomActionBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.bottomNavigationStyle);
    }

    public BottomActionBar(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, com.google.android.material.R.attr.bottomNavigationStyle);
    }

    public BottomActionBar(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // Ensure we are using the correctly themed context rather than the context that was passed in.
        context = getContext();

        /* Custom attributes */
        TintTypedArray attributes =
                ThemeEnforcement.obtainTintedStyledAttributes(
                        context, attrs, R.styleable.BottomActionBar, defStyleAttr, defStyleRes);

        this.menu = new MenuBuilder(context);
        this.menuView = createNavigationBarMenuView(context);
        this.menuView.initialize(menu);
        if (attributes.hasValue(R.styleable.BottomActionBar_menu)) {
            inflateMenu(attributes.getResourceId(R.styleable.BottomActionBar_menu, 0));
        }

        // Add a MaterialShapeDrawable as background that supports tinting in every API level.
        Drawable background = getBackground();
//        ColorStateList backgroundColorStateList = DrawableUtils.getColorStateListOrNull(background);
        // TODO: Remove when DrawableUtils.getColorStateListOrNull() available.
        ColorStateList backgroundColorStateList = getColorStateListOrNull(background);

        if (background == null || backgroundColorStateList != null) {
            ShapeAppearanceModel shapeAppearanceModel =
                    ShapeAppearanceModel.builder(context, attrs, defStyleAttr, defStyleRes).build();
            MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
            if (backgroundColorStateList != null) {
                // Setting fill color with a transparent CSL will disable the tint list.
                materialShapeDrawable.setFillColor(backgroundColorStateList);
            }
            materialShapeDrawable.initializeElevationOverlay(context);
            ViewCompat.setBackground(this, materialShapeDrawable);
        }

        if (shouldDrawCompatibilityTopDivider()) {
            addCompatibilityTopDivider(context);
        }

        attributes.recycle();

        addView(menuView);

        applyWindowInsets();
    }

    private static ColorStateList getColorStateListOrNull(@Nullable final Drawable drawable) {
        if (drawable instanceof ColorDrawable) {
            return ColorStateList.valueOf(((ColorDrawable) drawable).getColor());
        }

        if (VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (drawable instanceof ColorStateListDrawable) {
                return ((ColorStateListDrawable) drawable).getColorStateList();
            }
        }

        return null;
    }

    @NonNull
    protected BottomActionBarMenuView createNavigationBarMenuView(@NonNull Context context) {
        return new BottomActionBarMenuView(context);
    }

    /**
     * Inflate a menu resource into this navigation view.
     *
     * <p>Existing items in the menu will not be modified or removed.
     *
     * @param resId ID of a menu resource to inflate
     */
    public void inflateMenu(int resId) {
        if (menuInflater == null) {
            menuInflater = new SupportMenuInflater(getContext());
        }
        menuInflater.inflate(resId, menu);
        menuView.buildMenuView();
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    private void applyWindowInsets() {
        ViewUtils.doOnApplyWindowInsets(
                this,
                new ViewUtils.OnApplyWindowInsetsListener() {
                    @NonNull
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(
                            View view,
                            @NonNull WindowInsetsCompat insets,
                            @NonNull RelativePadding initialPadding) {
                        // Apply the bottom, start, and end padding for a BottomActionBar
                        // to dodge the system navigation bar
                        initialPadding.bottom += insets.getSystemWindowInsetBottom();

                        boolean isRtl = ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
                        int systemWindowInsetLeft = insets.getSystemWindowInsetLeft();
                        int systemWindowInsetRight = insets.getSystemWindowInsetRight();
                        initialPadding.start += isRtl ? systemWindowInsetRight : systemWindowInsetLeft;
                        initialPadding.end += isRtl ? systemWindowInsetLeft : systemWindowInsetRight;
                        initialPadding.applyToView(view);
                        return insets;
                    }
                });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minHeightSpec = makeMinHeightSpec(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, minHeightSpec);
    }

    private int makeMinHeightSpec(int measureSpec) {
        int minHeight = getSuggestedMinimumHeight();
        if (MeasureSpec.getMode(measureSpec) != MeasureSpec.EXACTLY && minHeight > 0) {
            minHeight += getPaddingTop() + getPaddingBottom();

            return MeasureSpec.makeMeasureSpec(
                    min(MeasureSpec.getSize(measureSpec), minHeight), MeasureSpec.EXACTLY);
        }

        return measureSpec;
    }

    /**
     * Returns true a divider must be added in place of shadows to maintain compatibility in pre-21
     * legacy backgrounds.
     */
    private boolean shouldDrawCompatibilityTopDivider() {
        return VERSION.SDK_INT < 21 && !(getBackground() instanceof MaterialShapeDrawable);
    }

    /**
     * Adds a divider in place of shadows to maintain compatibility in pre-21 legacy backgrounds. If a
     * pre-21 background has been updated to a MaterialShapeDrawable, MaterialShapeDrawable will draw
     * shadows instead.
     */
    private void addCompatibilityTopDivider(@NonNull Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(
                ContextCompat.getColor(
                        context,
                        com.google.android.material.R.color.design_bottom_navigation_shadow_color));
        LayoutParams dividerParams =
                new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getResources().getDimensionPixelSize(
                                com.google.android.material.
                                        R.dimen.design_bottom_navigation_shadow_height));
        divider.setLayoutParams(dividerParams);
        addView(divider);
    }

    public void setOnMenuItemClickListener(@Nullable MenuItem.OnMenuItemClickListener listener) {
        if (listener == null) {
            menu.setCallback(null);
        } else {
            menu.setCallback(new MenuBuilder.Callback() {
                @Override
                public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
                    return listener.onMenuItemClick(item);
                }

                @Override
                public void onMenuModeChange(@NonNull MenuBuilder menu) {
                }
            });
        }
    }
}
