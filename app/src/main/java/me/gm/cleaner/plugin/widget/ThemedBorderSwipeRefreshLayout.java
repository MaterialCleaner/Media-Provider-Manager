package me.gm.cleaner.plugin.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.ThemedSwipeRefreshLayout;

import me.gm.cleaner.plugin.R;
import me.gm.cleaner.plugin.util.DisplayUtils;

public class ThemedBorderSwipeRefreshLayout extends ThemedSwipeRefreshLayout {
    public ThemedBorderSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        int actionBarSize = (int) DisplayUtils.INSTANCE.getDimenByAttr(getContext(), R.attr.actionBarSize);
        setProgressViewOffset(false, actionBarSize, getProgressViewEndOffset() + actionBarSize);
    }
}
