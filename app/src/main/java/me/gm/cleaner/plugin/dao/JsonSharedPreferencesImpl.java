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
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class JsonSharedPreferencesImpl implements SharedPreferences {
    private final Object mLock = new Object();
    private static final Object CONTENT = new Object();
    private final JSONObject mStore;
    private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners = new WeakHashMap<>();

    public JsonSharedPreferencesImpl() {
        mStore = new JSONObject();
    }

    public JsonSharedPreferencesImpl(JSONObject jsonObject) {
        mStore = jsonObject;
    }

    public JsonSharedPreferencesImpl(@Nullable String json) throws JSONException {
        if (TextUtils.isEmpty(json)) {
            mStore = new JSONObject();
        } else {
            assert json != null;
            mStore = new JSONObject(json);
        }
    }

    @Override
    public Map<String, ?> getAll() {
        var all = new HashMap<String, Object>();
        synchronized (mLock) {
            var iterator = mStore.keys();
            while (iterator.hasNext()) {
                var key = iterator.next();
                all.put(key, mStore.opt(key));
            }
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String key, @Nullable T defValue) {
        synchronized (mLock) {
            var result = mStore.opt(key);
            if (result == null) {
                return defValue;
            }
            return (T) result;
        }
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return get(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return get(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return get(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return get(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return get(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return get(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        synchronized (mLock) {
            return mStore.has(key);
        }
    }

    @Override
    public Editor edit() {
        return new JsonEditorImpl(jsonObject -> false);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (mLock) {
            mListeners.put(listener, CONTENT);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (mLock) {
            mListeners.remove(listener);
        }
    }

    @NonNull
    @Override
    public String toString() {
        synchronized (mLock) {
            return mStore.toString();
        }
    }

    public class JsonEditorImpl implements Editor {
        private final Object mEditorLock = new Object();
        private final Map<String, Object> mModified = new HashMap<>();
        private boolean mClear = false;
        private final Predicate<JSONObject> mAwaitCommit;

        public JsonEditorImpl(Predicate<JSONObject> awaitCommit) {
            mAwaitCommit = awaitCommit;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            synchronized (mEditorLock) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            synchronized (mEditorLock) {
                mModified.put(key, values);
                return this;
            }
        }

        @Override
        public Editor putInt(String key, int value) {
            synchronized (mEditorLock) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putLong(String key, long value) {
            synchronized (mEditorLock) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putFloat(String key, float value) {
            synchronized (mEditorLock) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            synchronized (mEditorLock) {
                mModified.put(key, value);
                return this;
            }
        }

        @Override
        public Editor remove(String key) {
            synchronized (mEditorLock) {
                mModified.put(key, this);
                return this;
            }
        }

        @Override
        public Editor clear() {
            synchronized (mEditorLock) {
                mClear = true;
                return this;
            }
        }

        @Override
        public boolean commit() {
            JSONObject jsonToWriteToDisk;
            Set<OnSharedPreferenceChangeListener> listeners;
            var keysModified = new ArrayList<String>();
            synchronized (mLock) {
                listeners = mListeners.keySet();
                var hasListeners = !listeners.isEmpty();
                synchronized (mEditorLock) {
                    if (mClear) {
                        mStore.keys().forEachRemaining(this::remove);
                        mModified.clear();
                        mClear = false;
                    }

                    for (Map.Entry<String, Object> e : mModified.entrySet()) {
                        String k = e.getKey();
                        Object v = e.getValue();
                        // "this" is the magic value for a removal mutation. In addition,
                        // setting a value to "null" for a given key is specified to be
                        // equivalent to calling remove on that key.
                        if (v == this || v == null) {
                            if (!contains(k)) {
                                continue;
                            }
                            mStore.remove(k);
                        } else {
                            Object existingValue = get(k, null);
                            if (existingValue != null && existingValue.equals(v)) {
                                continue;
                            }
                            try {
                                mStore.put(k, v);
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                                throw new RuntimeException(jsonException);
                            }
                        }

                        if (hasListeners) {
                            keysModified.add(k);
                        }
                    }
                    mModified.clear();
                }
                jsonToWriteToDisk = new JSONObject(getAll());
            }
            notifyListeners(listeners, keysModified);
            return commitToDisk(jsonToWriteToDisk);
        }

        public void notifyListeners(Set<OnSharedPreferenceChangeListener> listeners, List<String> keysModified) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (int i = keysModified.size() - 1; i >= 0; i--) {
                    final String key = keysModified.get(i);
                    for (OnSharedPreferenceChangeListener listener : listeners) {
                        if (listener != null) {
                            listener.onSharedPreferenceChanged(JsonSharedPreferencesImpl.this, key);
                        }
                    }
                }
            } else {
                // Run this function on the main thread.
                new Handler(Looper.getMainLooper()).post(() -> notifyListeners(listeners, keysModified));
            }
        }

        public boolean commitToDisk(JSONObject jo) {
            return mAwaitCommit.test(jo);
        }

        @Override
        public void apply() {
            new Thread(this::commit).start();
        }
    }
}
