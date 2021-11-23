package me.gm.cleaner.plugin.xposed.util;

import android.os.FileObserver;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.function.Predicate;

public class FileCreationObserver extends FileObserver {
    private final File mTarget;
    private Predicate<File> mOnMaybeFileCreatedListener;

    public FileCreationObserver(File file) {
        super(file.getParentFile(), FileObserver.MODIFY | FileObserver.CREATE);
        mTarget = file;
    }

    @Override
    public void onEvent(int event, @Nullable String path) {
        if (path == null) {
            return;
        }
        if (mTarget.getName().equals(path) && mOnMaybeFileCreatedListener.test(mTarget)) {
            stopWatching();
        }
    }

    public FileCreationObserver setOnMaybeFileCreatedListener(Predicate<File> l) {
        mOnMaybeFileCreatedListener = l;
        return this;
    }
}
