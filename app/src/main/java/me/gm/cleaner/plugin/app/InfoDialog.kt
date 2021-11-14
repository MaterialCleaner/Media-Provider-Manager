package me.gm.cleaner.plugin.app

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf

class InfoDialog : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext(), theme)
            .setMessage(requireArguments().getString(KEY_MESSAGE))
            .setPositiveButton(android.R.string.ok, null)
            .create()

    companion object {
        private const val KEY_MESSAGE = "me.gm.cleaner.key.message"
        fun newInstance(message: String) =
            InfoDialog().apply { arguments = bundleOf(KEY_MESSAGE to message) }
    }
}
