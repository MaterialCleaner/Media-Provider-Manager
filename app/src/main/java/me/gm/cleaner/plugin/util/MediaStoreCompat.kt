package me.gm.cleaner.plugin.util

import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreCompat {

    // @see https://developer.android.com/training/data-storage/shared/media#remove-item
    suspend fun delete(fragment: Fragment, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            /**
             * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
             * or delete items in MediaStore directly, and explicit permission
             * must usually be obtained to do this.
             *
             * The way it works is the OS will throw a [RecoverableSecurityException],
             * which we can catch here. Inside there's an [IntentSender] which the
             * activity can use to prompt the user to grant permission to the item
             * so it can be either updated or deleted.
             */
            fragment.requireContext().contentResolver.delete(uri, null, null)
            return@withContext true
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException =
                    securityException as? RecoverableSecurityException
                        ?: throw securityException
                fragment.startIntentSenderForResult(
                    recoverableSecurityException.userAction.actionIntent.intentSender,
                    DELETE_PERMISSION_REQUEST, null, 0, 0, 0, null
                )
                return@withContext false
            } else {
                throw securityException
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun delete(fragment: Fragment, uris: Collection<Uri>) {
        if (uris.isEmpty()) {
            // This check is important because media store iterates to the first element without check.
            // Pass an empty collection to createRequest results in NoSuchElementException.
            return
        }
        return withContext(Dispatchers.IO) {
            val pendingIntent = MediaStore.createDeleteRequest(
                fragment.requireContext().contentResolver, uris
            )
            fragment.startIntentSenderForResult(
                pendingIntent.intentSender, DELETE_PERMISSION_REQUEST, null, 0, 0, 0, null
            )
        }
    }

    /**
     * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
     */
    const val DELETE_PERMISSION_REQUEST = 0x1033
}
