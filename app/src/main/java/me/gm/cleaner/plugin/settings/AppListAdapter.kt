package me.gm.cleaner.plugin.settings

import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.databinding.ApplistItemBinding
import me.gm.cleaner.plugin.util.AppIconCache
import me.gm.cleaner.plugin.util.DisplayUtils.getColorByAttr
import me.gm.cleaner.plugin.util.PreferencesPackageInfo

class AppListAdapter(private val activity: AppListActivity) :
    ListAdapter<PreferencesPackageInfo, AppListAdapter.ViewHolder>(CALLBACK) {
    private lateinit var selectedHolder: ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ApplistItemBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val pi = getItem(position)
        holder.loadIconJob = AppIconCache.loadIconBitmapAsync(
            activity, pi.applicationInfo, pi.applicationInfo.uid / 100000, binding.icon
        )
        binding.title.text = pi.label
        binding.summary.text = getSummary(pi)
        binding.root.setOnClickListener {
            activity.startActivity(
                Intent(activity, SettingsActivity::class.java).putExtra(
                    SettingsConstants.APP_INFO, pi.applicationInfo
                )
            )
        }
        binding.root.setOnLongClickListener {
            selectedHolder = holder
            false
        }
        binding.root.setOnCreateContextMenuListener { menu: ContextMenu, _: View?, _: ContextMenuInfo? ->
            activity.menuInflater.inflate(R.menu.menu_applist_item, menu)
            menu.setHeaderTitle(pi.label)
            if (pi.srCount == 0) {
                menu.removeItem(R.id.menu_delete_all_rules)
            }
        }
    }

    private fun getSummary(pi: PreferencesPackageInfo): CharSequence {
//        if (pi.faInfo.isNotEmpty()) {
//            val enabledFunctions: MutableList<String?> = ArrayList()
//            pi.faInfo.forEach {
//                enabledFunctions.add(activity.getString(it))
//            }
//            return getSpannableString(
//                TextUtils.join(String(charArrayOf(',', ' ')), enabledFunctions)
//            )
//        }
        return pi.packageName
    }

    private fun getSpannableString(text: CharSequence): SpannableStringBuilder =
        SpannableStringBuilder(text).apply {
            setSpan(
                ForegroundColorSpan(activity.getColorByAttr(R.attr.colorPrimary)), 0, length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }

    fun onContextItemSelected(item: MenuItem): Boolean {
        if (!this::selectedHolder.isInitialized) return false
        val position = selectedHolder.bindingAdapterPosition
        val pi = getItem(position)!!
        if (item.itemId == R.id.menu_delete_all_rules) {
            ModulePreferences.removePackage(pi.packageName)
            return true
        }
        return false
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.onViewRecycled()
    }

    class ViewHolder(val binding: ApplistItemBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var loadIconJob: Job

        fun onViewRecycled() {
            if (this::loadIconJob.isInitialized && loadIconJob.isActive) {
                loadIconJob.cancel()
            }
        }
    }

    companion object {
        private val CALLBACK: DiffUtil.ItemCallback<PreferencesPackageInfo> =
            object : DiffUtil.ItemCallback<PreferencesPackageInfo>() {
                override fun areItemsTheSame(
                    oldItem: PreferencesPackageInfo, newItem: PreferencesPackageInfo
                ): Boolean =
                    oldItem.applicationInfo.packageName == newItem.applicationInfo.packageName

                override fun areContentsTheSame(
                    oldItem: PreferencesPackageInfo, newItem: PreferencesPackageInfo
                ): Boolean = oldItem.srCount == newItem.srCount
            }
    }
}
