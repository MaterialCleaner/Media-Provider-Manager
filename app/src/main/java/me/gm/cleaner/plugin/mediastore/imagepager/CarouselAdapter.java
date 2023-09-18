/*
 * Copyright 2022 The Android Open Source Project
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

package me.gm.cleaner.plugin.mediastore.imagepager;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import me.gm.cleaner.plugin.R;
import me.gm.cleaner.plugin.mediastore.images.MediaStoreImage;

/**
 * An adapter that displays {@link MediaStoreImage}s for a Carousel.
 */
class CarouselAdapter extends ListAdapter<MediaStoreImage, CarouselItemViewHolder> {

    private static final DiffUtil.ItemCallback<MediaStoreImage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MediaStoreImage>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull MediaStoreImage oldItem, @NonNull MediaStoreImage newItem) {
                    // User properties may have changed if reloaded from the DB, but ID is fixed
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull MediaStoreImage oldItem, @NonNull MediaStoreImage newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final CarouselItemListener listener;
    @LayoutRes
    private final int itemLayoutRes;

    CarouselAdapter(CarouselItemListener listener) {
        this(listener, R.layout.cat_carousel_item);
    }

    CarouselAdapter(CarouselItemListener listener, @LayoutRes int itemLayoutRes) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.itemLayoutRes = itemLayoutRes;
    }

    @NonNull
    @Override
    public CarouselItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int pos) {
        return new CarouselItemViewHolder(
                LayoutInflater.from(viewGroup.getContext())
                        .inflate(itemLayoutRes, viewGroup, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselItemViewHolder carouselItemViewHolder, int pos) {
        carouselItemViewHolder.bind(getItem(pos));
    }

}
