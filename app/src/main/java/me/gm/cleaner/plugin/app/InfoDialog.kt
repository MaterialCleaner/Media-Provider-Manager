package me.gm.cleaner.plugin.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InfoDialog : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(requireArguments().getString(KEY_MESSAGE))
            .setPositiveButton(android.R.string.ok, null)
            .create().apply {
                setOnShowListener {
                    requireDialog().window?.findViewById<TextView>(android.R.id.message)
                        ?.apply {
                            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                            setTextIsSelectable(true)
                        }
                }
            }

    companion object {
        private const val KEY_MESSAGE = "me.gm.cleaner.key.message"
        fun newInstance(message: String) =
            InfoDialog().apply { arguments = bundleOf(KEY_MESSAGE to message) }
    }
}
