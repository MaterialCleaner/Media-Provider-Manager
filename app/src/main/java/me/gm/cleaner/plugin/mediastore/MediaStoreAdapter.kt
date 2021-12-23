/*
 * Copyright 2021 Green Mushroom
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

package me.gm.cleaner.plugin.mediastore

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class MediaStoreAdapter<M : MediaStoreModel, VH : MediaStoreAdapter.ViewHolder> :
    ListAdapter<M, VH>(MediaStoreModel.createCallback<M>()) {

    lateinit var selectionTracker: SelectionTracker<Long>

    protected val selectionTrackerInitialized: Boolean
        get() = ::selectionTracker.isInitialized

    // Id is unique in media store, hence we can use it as stable id directly.
    override fun getItemId(position: Int) = getItem(position).id

    abstract class ViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var details: ItemDetails<Long>
    }
}

class DetailsLookup(private val list: RecyclerView) : ItemDetailsLookup<Long>() {

    override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
        val view = list.findChildViewUnder(e.x, e.y)
        if (view != null) {
            val viewHolder = list.getChildViewHolder(view)
            if (viewHolder is MediaStoreAdapter.ViewHolder) {
                return viewHolder.details
            }
        }
        return null
    }
}
