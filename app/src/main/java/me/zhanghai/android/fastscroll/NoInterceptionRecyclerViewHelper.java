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

package me.zhanghai.android.fastscroll;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class NoInterceptionRecyclerViewHelper extends RecyclerViewHelper {

    @NonNull
    private final RecyclerView mView;
    private boolean mDragging;

    public NoInterceptionRecyclerViewHelper(@NonNull RecyclerView view,
                                            @Nullable PopupTextProvider popupTextProvider) {
        super(view, popupTextProvider);
        mView = view;
    }

    @Override
    public void addOnTouchEventListener(@NonNull Predicate<MotionEvent> onTouchEvent) {
        mView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView,
                                                 @NonNull MotionEvent event) {
                var action = event.getAction();
                // Intercept up event to disable fling.
                var shouldInterceptTouchEvent = (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_CANCEL) && mDragging;
                mDragging = onTouchEvent.test(event);
                return shouldInterceptTouchEvent;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView recyclerView,
                                     @NonNull MotionEvent event) {
                onTouchEvent.test(event);
            }
        });
    }
}
