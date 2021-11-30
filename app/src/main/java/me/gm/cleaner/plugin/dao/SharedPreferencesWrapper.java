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

package me.gm.cleaner.plugin.dao;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Set;

public class SharedPreferencesWrapper implements SharedPreferences {
    protected SharedPreferences delegate;

    @Override
    public Map<String, ?> getAll() {
        return delegate.getAll();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return delegate.getString(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return delegate.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return delegate.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return delegate.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return delegate.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return delegate.getBoolean(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return delegate.contains(key);
    }

    @Override
    public Editor edit() {
        return delegate.edit();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        delegate.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        delegate.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
