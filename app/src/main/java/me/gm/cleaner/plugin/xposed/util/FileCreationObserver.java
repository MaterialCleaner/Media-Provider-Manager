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

package me.gm.cleaner.plugin.xposed.util;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import android.os.FileObserver;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class FileCreationObserver extends FileObserver {
    private final File mTarget;
    private Predicate<Integer> mOnMaybeFileCreatedListener;
    private final ScheduledExecutorService mExecutor = newSingleThreadScheduledExecutor();
    private final AtomicInteger mQueueSize = new AtomicInteger();

    public FileCreationObserver(File file) {
        super(file.getParentFile(), FileObserver.MODIFY | FileObserver.CREATE);
        mTarget = file;
    }

    @Override
    public void onEvent(int event, @Nullable String path) {
        if (path == null) {
            return;
        }
        if (mTarget.getName().equals(path)) {
            mQueueSize.incrementAndGet();
            mExecutor.schedule(() -> {
                var queueSize = mQueueSize.decrementAndGet();
                var testTimes = 1 - queueSize;
                // Less than 0 when predicate returns false.
                if (queueSize <= 0 && mOnMaybeFileCreatedListener.test(testTimes)) {
                    stopWatching();
                    mExecutor.shutdownNow();
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    public FileCreationObserver setOnMaybeFileCreatedListener(Predicate<Integer> l) {
        mOnMaybeFileCreatedListener = l;
        return this;
    }
}
