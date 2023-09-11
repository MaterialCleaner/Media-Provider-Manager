/*
 * Copyright 2023 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.view.ViewCompat;

import me.gm.cleaner.plugin.databinding.DesignBottomBarItemBinding;

@SuppressLint("RestrictedApi")
public class BottomActionBarMenuView extends LinearLayout implements MenuView {
    @NonNull
    private final OnClickListener onClickListener =
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    MenuItem item = (MenuItem) v.getTag();
                    menu.performItemAction(item, null, 0);
                }
            };

    private MenuBuilder menu;

    public BottomActionBarMenuView(@NonNull Context context) {
        super(context);

        setOrientation(LinearLayout.HORIZONTAL);

        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    public void initialize(MenuBuilder menu) {
        this.menu = menu;
    }

    @Override
    public int getWindowAnimations() {
        return 0;
    }

    private DesignBottomBarItemBinding getNewItem() {
        return DesignBottomBarItemBinding.inflate(LayoutInflater.from(getContext()), this, false);
    }

    public void buildMenuView() {
        removeAllViews();

        for (int i = 0; i < menu.size(); i++) {
            DesignBottomBarItemBinding itemBinding = getNewItem();
            MenuItemImpl item = (MenuItemImpl) menu.getItem(i);
            itemBinding.navigationBarItemIconView.setImageDrawable(item.getIcon());
            itemBinding.navigationBarItemSmallLabelView.setText(item.getTitle());
            View child = itemBinding.getRoot();
            child.setOnClickListener(onClickListener);
            child.setTag(item);
            TooltipCompat.setTooltipText(child, item.getTitle());
            addView(child);
        }
    }
}
