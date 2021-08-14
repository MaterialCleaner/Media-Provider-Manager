package me.gm.cleaner.plugin.settings

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.gm.cleaner.plugin.BinderReceiver
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.util.PreferencesPackageInfo
import me.gm.cleaner.plugin.util.PreferencesPackageInfo.Companion.copy
import java.text.Collator
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class AppListViewModel : ViewModel() {
    val isSearching = MutableLiveData(false)
    val queryText = MutableLiveData("")
    val showingList = MutableLiveData<MutableList<PreferencesPackageInfo>>(ArrayList())
    val searchingList = MutableLiveData<MutableList<PreferencesPackageInfo>>(ArrayList())
    val installedPackagesCache = MutableLiveData<MutableList<PreferencesPackageInfo>>(ArrayList())
    val loadingProgress = MutableLiveData(0)

    fun isSearching(): Boolean {
        val value = isSearching.value
        return value != null && value
    }

    private fun getInstalledPackagesForModulePreferences(pm: PackageManager): MutableList<PreferencesPackageInfo> {
        val installedPackages = BinderReceiver.installedPackages.toMutableList().apply {
            removeIf { !it.applicationInfo.enabled }
        }
        val size = installedPackages.size
        val count = AtomicInteger(0)
        return installedPackages.stream()
            .map {
                loadingProgress.postValue(100 * count.incrementAndGet() / size)
                PreferencesPackageInfo.newInstance(it, pm)
            }.collect(Collectors.toList()).apply { loadingProgress.postValue(-1) }
    }

    fun fetchInstalledPackages(pm: PackageManager) {
        getInstalledPackagesForModulePreferences(pm).apply {
            installedPackagesCache.postValue(this)
        }
    }

    fun refreshPreferencesCountInCache() {
        val list: MutableList<PreferencesPackageInfo> = ArrayList()
        installedPackagesCache.value?.forEach {
            list.add(it.copy())
        }
        installedPackagesCache.postValue(list)
    }

    fun refreshShowingList() {
        showingList.postValue(
            installedPackagesCache.value!!.toMutableList().apply {
                if (ModulePreferences.isHideSystemApp) {
                    removeIf {
                        it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    }
                }
                if (ModulePreferences.isHideNoStoragePermissionApp) {
                    removeIf {
                        val requestedPermissions = it.requestedPermissions
                        requestedPermissions == null || !listOf(*requestedPermissions)
                            .contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                when (ModulePreferences.sortBy) {
                    ModulePreferences.SORT_BY_NAME ->
                        sortWith { o1: PreferencesPackageInfo?, o2: PreferencesPackageInfo? ->
                            Collator.getInstance().compare(o1?.label, o2?.label)
                        }
                    ModulePreferences.SORT_BY_UPDATE_TIME ->
                        sortWith(Comparator.comparingLong {
                            -it!!.lastUpdateTime
                        })
                }
                if (ModulePreferences.ruleCount) {
//                    sortWith { o1: PreferencesPackageInfo?, o2: PreferencesPackageInfo? ->
//                        when (mTitle) {
//                            R.string.storage_redirect_title -> return@sortWith o2!!.srCount - o1!!.srCount
//                            R.string.foreground_activity_observer_title -> return@sortWith o2!!.faInfo.size - o1!!.faInfo.size
//                            else -> return@sortWith 0
//                        }
//                    }
                }
            }
        )
    }

    fun refreshSearchingList() {
        val lowerQuery = queryText.value!!.lowercase(Locale.getDefault())
        searchingList.postValue(
            showingList.value!!.toMutableList().apply {
                removeIf {
                    !it.label.lowercase(Locale.getDefault()).contains(lowerQuery)
                            && !it.applicationInfo.packageName.lowercase(Locale.getDefault())
                        .contains(lowerQuery)
                }
            }
        )
    }
}
