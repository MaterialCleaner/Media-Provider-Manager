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

package me.gm.cleaner.plugin.module.usagerecord

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.text.buildSpannedString
import androidx.core.text.strikeThrough
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_DELETE
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_INSERT
import me.gm.cleaner.plugin.dao.MediaProviderOperation.Companion.OP_QUERY
import me.gm.cleaner.plugin.dao.MediaProviderRecord
import me.gm.cleaner.plugin.databinding.UsagerecordItemBinding
import me.gm.cleaner.plugin.widget.makeSnackbarWithFullyDraggableContainer

class UsageRecordAdapter(private val fragment: UsageRecordFragment) :
    ListAdapter<MediaProviderRecord, UsageRecordAdapter.ViewHolder>(CALLBACK) {
    private val context = fragment.requireContext()
    private val clipboardManager by lazy { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(UsagerecordItemBinding.inflate(LayoutInflater.from(parent.context)))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val record = getItem(position)
        Glide.with(fragment)
            .load(record.packageInfo)
            .into(binding.icon)
        binding.title.text = record.label ?: record.packageName
        val operation = when (record.operation) {
            OP_QUERY -> fragment.getString(R.string.queried_at)
            OP_INSERT -> fragment.getString(R.string.inserted_at)
            OP_DELETE -> fragment.getString(R.string.deleted_at)
            else -> throw IllegalArgumentException()
        } + formatDateTime(record.timeMillis)
        binding.operation.text = if (record.intercepted.any { it })
            buildSpannedString { strikeThrough { append(operation) } } else operation
        val more = record.data.size - 1
        val hasMore = more > 0
        binding.record.setTextAndSuffix(
            record.data.first(), if (hasMore) fragment.getString(R.string.and_more, more) else null
        )
        binding.root.setOnClickListener {
            val adapter = ArrayAdapter(
                context, R.layout.usagerecord_popup_item,
                record.data.zip(record.intercepted).map { (data, intercepted) ->
                    if (intercepted) buildSpannedString { strikeThrough { append(data) } } else data
                }
            )
            val listPopupWindow = ListPopupWindow(context)
            listPopupWindow.setAdapter(adapter)
            listPopupWindow.anchorView = binding.root
            listPopupWindow.setOnItemClickListener { _, _, position, _ ->
                val data = adapter.getItem(position).toString()
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, data))
                makeSnackbarWithFullyDraggableContainer(
                    { fragment.requireActivity().findViewById(R.id.fully_draggable_container) },
                    fragment.requireView(), fragment.getString(R.string.copied, data),
                    Snackbar.LENGTH_SHORT
                ).show()
                listPopupWindow.dismiss()
            }
            listPopupWindow.show()
        }
    }

    private fun formatDateTime(timeMillis: Long): String {
        val flags = DateUtils.FORMAT_NO_NOON or DateUtils.FORMAT_NO_MIDNIGHT or
                DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME
        return DateUtils.formatDateTime(context, timeMillis, flags)
    }

    class ViewHolder(val binding: UsagerecordItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<MediaProviderRecord> =
            object : DiffUtil.ItemCallback<MediaProviderRecord>() {
                override fun areItemsTheSame(
                    oldItem: MediaProviderRecord, newItem: MediaProviderRecord
                ) = oldItem.timeMillis == newItem.timeMillis

                override fun areContentsTheSame(
                    oldItem: MediaProviderRecord, newItem: MediaProviderRecord
                ) = oldItem == newItem
            }
    }
}
