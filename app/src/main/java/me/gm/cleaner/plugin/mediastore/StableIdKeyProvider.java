/*
 * Copyright 2017 The Android Open Source Project
 * Copyright 2021 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

/**
 * An {@link ItemKeyProvider} that provides item keys by way of native
 * {@link RecyclerView.Adapter} stable ids.
 *
 * <p>The corresponding RecyclerView.Adapter instance must:
 * <ol>
 *     <li> Enable stable ids using {@link RecyclerView.Adapter#setHasStableIds(boolean)}
 *     <li> Override {@link RecyclerView.Adapter#getItemId(int)} with a real implementation.
 * </ol>
 *
 * <p>
 * There are trade-offs with this implementation:
 * <ul>
 *     <li>It necessarily auto-boxes {@code long} stable id values into {@code Long} values for
 *     use as selection keys.
 *     <li>It deprives Chromebook users (actually, any device with an attached pointer) of support
 *     for band-selection.
 * </ul>
 *
 * <p>See com.example.android.supportv7.widget.selection.fancy.DemoAdapter.KeyProvider in the
 * SupportV7 Demos package for an example of how to implement a better ItemKeyProvider.
 */
public final class StableIdKeyProvider extends ItemKeyProvider<Long> {

    private static final String TAG = "StableIdKeyProvider";

    private final RecyclerView mRecyclerView;
    private final RecyclerView.Adapter<?> mAdapter;

    /**
     * Creates a new key provider that uses cached {@code long} stable ids associated
     * with the RecyclerView items.
     *
     * @param recyclerView the owner RecyclerView
     */
    public StableIdKeyProvider(@NonNull RecyclerView recyclerView) {
        // Since this provide is based on stable ids based on whats laid out in the window
        // we can only satisfy "window" scope key access.
        super(SCOPE_CACHED);

        mRecyclerView = recyclerView;
        mAdapter = recyclerView.getAdapter();
    }

    @NonNull
    @Override
    public Long getKey(int position) {
        return mAdapter.getItemId(position);
    }

    @Override
    public int getPosition(@NonNull Long key) {
        var holder = mRecyclerView.findViewHolderForItemId(key);
        if (holder != null) {
            return holder.getBindingAdapterPosition();
        } else {
            for (int i = 0, itemCount = mAdapter.getItemCount(); i < itemCount; i++) {
                if (key == mAdapter.getItemId(i)) {
                    return i;
                }
            }
            return RecyclerView.NO_POSITION;
        }
    }
}
