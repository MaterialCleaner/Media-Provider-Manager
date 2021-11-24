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
    private Predicate<File> mOnMaybeFileCreatedListener;
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
            mExecutor.scheduleWithFixedDelay(() -> {
                // Less than 0 when predicate returns false.
                if (mQueueSize.decrementAndGet() <= 0 && mOnMaybeFileCreatedListener.test(mTarget) ||
                        // Don't retry after failed 3 times.
                        mQueueSize.get() <= -3) {
                    stopWatching();
                    mExecutor.shutdownNow();
                }
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    public FileCreationObserver setOnMaybeFileCreatedListener(Predicate<File> l) {
        mOnMaybeFileCreatedListener = l;
        return this;
    }
}
