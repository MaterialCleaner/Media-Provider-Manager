package me.gm.cleaner.plugin.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf

class InfoDialog : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        AlertDialog.Builder(requireContext(), theme)
            .setMessage(requireArguments().getString(KEY_MESSAGE))
            .setPositiveButton(android.R.string.ok, null)
            .create().apply {
                setOnShowListener {
                    requireDialog().window?.findViewById<TextView>(android.R.id.message)
                        ?.setTextIsSelectable(true)
                }
            }

    companion object {
        private const val KEY_MESSAGE = "me.gm.cleaner.key.message"
        fun newInstance(message: String) =
            InfoDialog().apply { arguments = bundleOf(KEY_MESSAGE to message) }
    }
}
