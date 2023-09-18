/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.material.carousel;

import static com.google.android.material.carousel.CarouselStrategyHelper.createKeylineState;
import static com.google.android.material.carousel.CarouselStrategyHelper.getSmallSizeMax;
import static com.google.android.material.carousel.CarouselStrategyHelper.getSmallSizeMin;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.RecyclerView.LayoutParams;

/**
 * A {@link CarouselStrategy} that knows how to size and fit one large item and one small item into
 * a container to create a layout to browse one 'hero' item at a time with a preview item.
 *
 * <p>Note that this strategy resizes Carousel items to take up the full width or height of the
 * Carousel, save room for the small item.
 *
 * <p>This class will automatically be reversed by {@link CarouselLayoutManager} if being laid out
 * right-to-left and does not need to make any account for layout direction itself.
 *
 * <p>For more information, see the <a
 * href="https://github.com/material-components/material-components-android/blob/master/docs/components/Carousel.md">component
 * developer guidance</a> and <a href="https://material.io/components/carousel/overview">design
 * guidelines</a>.
 */
public class CustomHeroCarouselStrategy extends CarouselStrategy {

    @Override
    @NonNull
    KeylineState onFirstChildMeasuredWithMargins(@NonNull Carousel carousel, @NonNull View child) {
        int availableSpace = carousel.getContainerHeight();
        if (carousel.isHorizontal()) {
            availableSpace = carousel.getContainerWidth();
        }

        LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();
        float childMargins = childLayoutParams.topMargin + childLayoutParams.bottomMargin;
        float measuredChildSize = child.getMeasuredWidth();

        if (carousel.isHorizontal()) {
            childMargins = childLayoutParams.leftMargin + childLayoutParams.rightMargin;
            measuredChildSize = child.getMeasuredHeight();
        }

        float smallSizeMin = getSmallSizeMin(child.getContext()) / 2;
        float smallSizeMax = getSmallSizeMax(child.getContext());
        float smallChildSizeMin = smallSizeMin + childMargins;
        float smallChildSizeMax = smallSizeMax + childMargins;

        float targetLargeChildSize = measuredChildSize + childMargins;
        // Ideally we would like to create a balanced arrangement where a small item is 2/3 the size of
        // the large item. Clamp the small target size within our min-max range and as close to 2/3 of
        // the target large item size as possible.
        float targetSmallChildSize =
                MathUtils.clamp(
                        measuredChildSize * 2F / 3F + childMargins,
                        smallChildSizeMin,
                        smallChildSizeMax);

        Arrangement arrangement = new Arrangement(
                /* priority= */ 0,
                targetSmallChildSize,
                smallChildSizeMin,
                smallChildSizeMax,
                /* smallCount= */ 2,
                /* targetMediumSize= */ 0,
                /* mediumCount= */ 0,
                targetLargeChildSize,
                /* largeCount= */ 1,
                availableSpace);
        return createKeylineState(
                child.getContext(),
                childMargins,
                availableSpace,
                arrangement,
                carousel.getCarouselAlignment());
    }
}
