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

package me.gm.cleaner.plugin.module.settings;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import me.gm.cleaner.plugin.dao.JsonSharedPreferencesImpl;
import me.gm.cleaner.plugin.module.BinderViewModel;

public final class BinderSpImpl extends JsonSharedPreferencesImpl {
    public static final int WHO = 0;
    private final BinderViewModel mBinderViewModel;

    private BinderSpImpl(JSONObject jo, BinderViewModel binderViewModel) {
        super(jo);
        mBinderViewModel = binderViewModel;
    }

    public static BinderSpImpl create(BinderViewModel binderViewModel) {
        JSONObject json;
        try {
            var str = binderViewModel.readSp(WHO);
            if (TextUtils.isEmpty(str)) {
                // don't throw an exception in this case.
                json = new JSONObject();
            } else {
                json = new JSONObject(str);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            json = new JSONObject();
        }
        return new BinderSpImpl(json, binderViewModel);
    }

    @Override
    public Editor edit() {
        return new JsonEditorImpl(jsonObject -> {
            mBinderViewModel.writeSp(WHO, jsonObject.toString());
            return true;
        });
    }
}
