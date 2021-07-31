package androidx.swiperefreshlayout.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.gm.cleaner.plugin.R;
import me.gm.cleaner.plugin.util.DisplayUtils;

public class ThemedSwipeRefreshLayout extends SwipeRefreshLayout {
    public ThemedSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        Context context = getContext();
        setColorSchemeColors(DisplayUtils.INSTANCE.getColorByAttr(context, R.attr.colorPrimary));
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        View child = getChildView();
        if (child != null) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(child.getMeasuredWidth() + getPaddingLeft() + getPaddingRight(),
                    child.getMeasuredHeight() + getPaddingTop() + getPaddingBottom());
        }
    }

    @Nullable
    private View getChildView() {
        for (int i = 0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            if (!child.equals(mCircleView)) {
                return child;
            }
        }
        return null;
    }
}
