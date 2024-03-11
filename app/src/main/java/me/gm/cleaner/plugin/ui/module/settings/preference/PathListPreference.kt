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

package me.gm.cleaner.plugin.ui.module.settings.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.icu.text.ListFormatter
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.DialogPreference
import me.gm.cleaner.plugin.R
import java.text.Collator

class PathListPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    @SuppressLint("RestrictedApi") @AttrRes defStyleAttr: Int = TypedArrayUtils.getAttr(
        context, androidx.preference.R.attr.dialogPreferenceStyle,
        android.R.attr.dialogPreferenceStyle
    ), @StyleRes defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    private val _values = mutableSetOf<String>()
    var values: Set<String>
        get() = _values
        set(value) {
            _values.clear()
            _values.addAll(value)
            persistStringSet(value)
            notifyChanged()
        }

    init {
        summaryProvider = instance
    }

    override fun getDialogLayoutResource(): Int = R.layout.path_list_dialog

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        val defaultValues = a.getTextArray(index)
        val result = mutableSetOf<String>()

        for (defaultValue in defaultValues) {
            result.add(defaultValue.toString())
        }
        return result
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        values = getPersistedStringSet(defaultValue as Set<String>?)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state
            return superState
        }
        val myState = SavedState(superState)
        myState.mValues = values
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        values = myState.mValues
    }

    private class SavedState : BaseSavedState {
        var mValues = emptySet<String>()

        constructor(source: Parcel) : super(source) {
            val size = source.readInt()
            val strings = arrayOfNulls<String>(size)
            source.readStringArray(strings)
            mValues = strings.mapNotNullTo(mutableSetOf()) { it!! }
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(mValues.size)
            dest.writeStringArray(mValues.toTypedArray())
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel) = SavedState(`in`)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    class SimpleSummaryProvider : SummaryProvider<PathListPreference> {
        override fun provideSummary(preference: PathListPreference): String =
            if (preference.values.isEmpty()) {
                preference.context.getString(androidx.preference.R.string.not_set)
            } else {
                val collator = Collator.getInstance()
                ListFormatter.getInstance().format(preference.values.sortedWith { o1, o2 ->
                    collator.compare(o1, o2)
                })
            }
    }

    companion object {
        val instance by lazy { SimpleSummaryProvider() }
    }
}
