package me.gm.cleaner.plugin.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import me.gm.cleaner.plugin.app.BaseFragment

object PermissionUtils {
    private const val TAG = "PermissionUtils"
    fun requestPermissions(
        fragmentManager: FragmentManager, requesterFragment: RequesterFragment
    ) {
        fragmentManager.commitNow {
            val existingFragment = fragmentManager.findFragmentByTag(TAG)
            if (existingFragment != null) {
                remove(existingFragment)
            }
            add(requesterFragment, TAG)
        }
        requesterFragment.dispatchRequestPermissions(requesterFragment.requiredPermissions)
    }

    fun startDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}

abstract class RequesterFragment : BaseFragment() {
    open val requiredPermissions: Array<String> = emptyArray<String>()
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                // We should dispatch by ourselves rather than call dispatchRequestPermissions(),
                // or we'll stick in infinite recursion.
                val granted = result.filterValues { it }.keys
                if (granted.isNotEmpty()) {
                    onRequestPermissionsSuccess(granted)
                }
                val denied = result.keys - granted
                if (denied.isNotEmpty()) {
                    val shouldShowRationale = denied.filter {
                        ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
                    }.toSet()
                    onRequestPermissionsFailure(
                        shouldShowRationale, denied - shouldShowRationale
                    )
                }
            }
    }

    @CallSuper
    internal fun dispatchRequestPermissions(permissions: Array<String>) {
        val granted = permissions.filter {
            ActivityCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }.toSet()
        if (permissions.size > granted.size) {
            val denied = permissions.toSet() - granted
            val shouldShowRationale = denied.filter {
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
            }.toSet()
            if (shouldShowRationale.isNotEmpty()) {
                onRequestPermissionsFailure(shouldShowRationale, emptySet())
            } else {
                onRequestPermissions(denied.toTypedArray())
            }
        }
    }

    protected fun onRequestPermissions(permissions: Array<String>) {
        requestMultiplePermissions.launch(permissions)
    }

    protected open fun onRequestPermissionsSuccess(permissions: Set<String>) {
    }

    protected open fun onRequestPermissionsFailure(
        shouldShowRationale: Set<String>, denied: Set<String>
    ) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestMultiplePermissions.unregister()
    }
}
