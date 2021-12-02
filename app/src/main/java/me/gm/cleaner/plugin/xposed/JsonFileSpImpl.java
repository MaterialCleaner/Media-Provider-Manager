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

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import de.robv.android.xposed.XposedBridge;
import me.gm.cleaner.plugin.dao.JsonSharedPreferencesImpl;
import me.gm.cleaner.plugin.dao.SharedPreferencesWrapper;

public final class JsonFileSpImpl extends SharedPreferencesWrapper {
    public final File file;

    public JsonFileSpImpl(File src) {
        file = src;

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
            XposedBridge.log(e);
            json = new JSONObject();
        }
        delegate = new JsonSharedPreferencesImpl(json);
    }

    private void ensureFile() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                XposedBridge.log(e);
                throw new RuntimeException(e);
            }
        }
    }

    public String read() {
        ensureFile();
        try (var it = new FileInputStream(file)) {
            var bb = ByteBuffer.allocate(it.available());
            it.getChannel().read(bb);
            return new String(bb.array());
        } catch (IOException e) {
            XposedBridge.log(e);
        }
        return null;
    }

    public void write(String what) {
        ensureFile();
        var bb = ByteBuffer.wrap(what.getBytes());
        try (var it = new FileOutputStream(file)) {
            it.getChannel().write(bb);
        } catch (IOException e) {
            XposedBridge.log(e);
        }

        try {
            delegate = new JsonSharedPreferencesImpl(new JSONObject(what));
        } catch (JSONException e) {
            XposedBridge.log(e);
        }
    }
}
