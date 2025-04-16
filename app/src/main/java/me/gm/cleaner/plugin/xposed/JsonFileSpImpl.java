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

package me.gm.cleaner.plugin.xposed;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import de.robv.android.xposed.XposedBridge;
import me.gm.cleaner.plugin.dao.JsonSharedPreferencesImpl;
import me.gm.cleaner.plugin.dao.SharedPreferencesWrapper;
import me.gm.cleaner.plugin.provider.xSpUtils;

public class JsonFileSpImpl extends SharedPreferencesWrapper {
    protected String contentCache;
    protected String fileName;
    protected Context ctx;

    public JsonFileSpImpl(Context context, String src) {
        fileName = src;
        ctx = context;
        JSONObject json;
        try {
            var str = read();
            if (TextUtils.isEmpty(str)) {
                // don't throw an exception in this case.
                json = new JSONObject();
            } else {
                json = new JSONObject(str);
            }
        } catch (JSONException e) {
            json = new JSONObject();
        }
        delegate = new JsonSharedPreferencesImpl(json);
    }

    public String read() {
        if (contentCache == null || contentCache.isEmpty()) {
            try {
                contentCache = xSpUtils.getSharedStringPrefs(ctx, fileName);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }
        return contentCache;
    }

    public void refreshSpData() {
        contentCache = xSpUtils.getSharedStringPrefs(ctx, fileName);
    }


}
