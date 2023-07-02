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

import android.net.Uri
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.databinding.MediaStoreHeaderBinding
import java.util.Calendar

abstract class MediaStoreAdapter(private val fragment: Fragment) :
    ListAdapter<MediaStoreModel, MediaStoreAdapter.ViewHolder>(
        MediaStoreModel.createCallback<MediaStoreModel>()
    ) {
    lateinit var selectionTracker: SelectionTracker<Long>

    protected val selectionTrackerInitialized: Boolean
        get() = ::selectionTracker.isInitialized

    @CallSuper
    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is MediaStoreHeader -> R.layout.media_store_header
        else -> throw IndexOutOfBoundsException()
    }

    @CallSuper
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        when (viewType) {
            R.layout.media_store_header -> HeaderViewHolder(
                MediaStoreHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

            else -> throw IndexOutOfBoundsException()
        }

    @CallSuper
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val binding = holder.binding
                val item = getItem(position) as MediaStoreHeader
                binding.title.text = item.title
            }
        }
    }

    // Id is unique in media store, hence we can use it as stable id directly.
    override fun getItemId(position: Int): Long = getItem(position).id

    protected val then: Calendar = Calendar.getInstance()
    protected val now: Calendar = Calendar.getInstance()
    protected open fun formatDateTime(timeMillis: Long): String {
        then.timeInMillis = timeMillis
        now.timeInMillis = System.currentTimeMillis()
        val flags = DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE
        return DateUtils.formatDateTime(fragment.requireContext(), timeMillis, flags)
    }

    private val uriPositionMap: MutableList<Int> = mutableListOf()
    fun getHolderPositionForUriPosition(position: Int): Int = uriPositionMap[position]
    protected fun getUriPositionForAdapterPosition(position: Int) =
        uriPositionMap.binarySearch(position)

    protected open fun onPreSubmitList(list: List<MediaStoreModel>): List<MediaStoreModel> {
        uriPositionMap.clear()
        return when (RootPreferences.sortMediaBy) {
            RootPreferences.SORT_BY_PATH -> {
                val groupedList = mutableListOf<MediaStoreModel>()
                var lastHeader = ""
                list.forEach { model ->
                    val header = model.relativePath
                    if (lastHeader != header) {
                        lastHeader = header
                        groupedList += MediaStoreHeader(header)
                    }
                    uriPositionMap += groupedList.size
                    groupedList += model
                }
                groupedList
            }

            RootPreferences.SORT_BY_DATE_TAKEN -> {
                val groupedList = mutableListOf<MediaStoreModel>()
                var lastHeader = ""
                list.forEach { model ->
                    val header = formatDateTime(model.dateTaken)
                    if (lastHeader != header) {
                        lastHeader = header
                        groupedList += MediaStoreHeader(header)
                    }
                    uriPositionMap += groupedList.size
                    groupedList += model
                }
                groupedList
            }

            else -> {
                repeat(list.size) { position ->
                    uriPositionMap += position
                }
                list
            }
        }
    }

    override fun submitList(list: List<MediaStoreModel>?) {
        submitList(list, null)
    }

    override fun submitList(list: List<MediaStoreModel>?, commitCallback: Runnable?) {
        if (list != null) {
            super.submitList(onPreSubmitList(list), commitCallback)
        } else {
            super.submitList(list, commitCallback)
        }
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var details: ItemDetails<Long>? = null
    }

    class HeaderViewHolder(val binding: MediaStoreHeaderBinding) : ViewHolder(binding.root)
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

class MediaStoreHeader(val title: String) :
    MediaStoreModel(title.hashCode().toLong(), Uri.EMPTY, "", "", "", -1L) {
    override fun hashCode(): Int = title.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaStoreHeader

        if (title != other.title) return false

        return true
    }
}
