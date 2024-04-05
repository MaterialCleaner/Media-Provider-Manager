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
        requesterFragment.dispatchRequestPermissions(requesterFragment.requiredPermissions, null)
    }

    fun startDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}

/**
 * 4 status:
 * granted
 * showRationale
 * neverAsked
 * permanentlyDenied
 */
abstract class RequesterFragment : BaseFragment() {
    open val requiredPermissions = emptyArray<String>()
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                // We should dispatch by ourselves rather than call dispatchRequestPermissions(),
                // or we'll stick in infinite recursion.
                val granted = result.filterValues { it }.keys
                if (granted.isNotEmpty()) {
                    onRequestPermissionsSuccess(granted, savedInstanceState)
                }
                val denied = result.keys - granted
                if (denied.isNotEmpty()) {
                    val shouldShowRationale = denied.filterTo(mutableSetOf()) {
                        ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
                    }
                    val permanentlyDenied = denied - shouldShowRationale
                    onRequestPermissionsFailure(
                        shouldShowRationale, permanentlyDenied, true, savedInstanceState
                    )
                }
            }
    }

    @CallSuper
    open fun dispatchRequestPermissions(permissions: Array<String>, savedInstanceState: Bundle?) {
        val granted = permissions.filterTo(mutableSetOf()) {
            ActivityCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (granted.isNotEmpty()) {
            onRequestPermissionsSuccess(granted, savedInstanceState)
        }
        if (permissions.size > granted.size) {
            val denied = permissions.toSet() - granted
            val shouldShowRationale = denied.filterTo(mutableSetOf()) {
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
            }
            if (shouldShowRationale.isNotEmpty()) {
                onRequestPermissionsFailure(
                    shouldShowRationale, emptySet(), false, savedInstanceState
                )
            } else {
                // Permissions that are never asked and that are permanently denied
                // can't be distinguished unless we actually request.
                onRequestPermissions(denied.toTypedArray(), savedInstanceState)
            }
        }
    }

    protected open fun onRequestPermissions(
        permissions: Array<String>, savedInstanceState: Bundle?
    ) {
        requestMultiplePermissions.launch(permissions)
    }

    protected open fun onRequestPermissionsSuccess(
        permissions: Set<String>, savedInstanceState: Bundle?
    ) {
    }

    protected open fun onRequestPermissionsFailure(
        shouldShowRationale: Set<String>, permanentlyDenied: Set<String>, haveAskedUser: Boolean,
        savedInstanceState: Bundle?
    ) {
        if (shouldShowRationale.isNotEmpty()) {
            onRequestPermissions(shouldShowRationale.toTypedArray(), savedInstanceState)
        } else if (permanentlyDenied.isNotEmpty()) {
            PermissionUtils.startDetailsSettings(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestMultiplePermissions.unregister()
    }
}
