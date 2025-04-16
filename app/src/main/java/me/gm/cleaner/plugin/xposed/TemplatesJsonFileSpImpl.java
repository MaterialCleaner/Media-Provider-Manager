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

import me.gm.cleaner.plugin.model.Templates;

public final class TemplatesJsonFileSpImpl extends JsonFileSpImpl {
    private volatile Templates templatesCache;

    public TemplatesJsonFileSpImpl(Context context, String src) {
        super(context, src);
        templatesCache = new Templates(read());
    }

    public Templates getTemplates() {
        synchronized (this) {
            if (templatesCache == null || templatesCache.isEmpty()) {
                refreshSpData();
            }
        }
        return templatesCache;
    }

    @Override
    public void refreshSpData() {
        super.refreshSpData();
        templatesCache = new Templates(read());
    }
}
