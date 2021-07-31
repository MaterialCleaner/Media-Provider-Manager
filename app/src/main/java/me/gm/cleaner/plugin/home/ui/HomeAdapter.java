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

package me.gm.cleaner.plugin.home.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import me.gm.cleaner.plugin.BinderReceiver;
import me.gm.cleaner.plugin.R;
import me.gm.cleaner.plugin.databinding.HomeButtonBinding;
import me.gm.cleaner.plugin.databinding.HomeCardBinding;
import me.gm.cleaner.plugin.databinding.HomeCardButtonBinding;
import me.gm.cleaner.plugin.settings.AppListActivity;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CARD = View.generateViewId();
    private static final int TYPE_CARD_BUTTON = View.generateViewId();
    private static final int TYPE_BUTTON = View.generateViewId();
    private final HomeActivity activity;

    public HomeAdapter(HomeActivity activity) {
        this.activity = activity;
        BinderReceiver.MODULE_VER.observe(activity, serverVer -> notifyItemRangeChanged(0, 2));
       // ServicePreferences.getInstance().setOnPreferenceChangeListener(() -> notifyItemChanged(1));
    }

    @Override
    public int getItemViewType(int position) {
        switch (position) {
            case 0:
                return TYPE_CARD;
            case 1:
                return TYPE_CARD_BUTTON;
            default:
                return TYPE_BUTTON;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CARD) {
            HomeCardBinding cardBinding
                    = HomeCardBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new CardHolder(cardBinding);
        } else if (viewType == TYPE_CARD_BUTTON) {
            HomeCardButtonBinding cardButtonBinding
                    = HomeCardButtonBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new CardButtonHolder(cardButtonBinding);
        } else if (viewType == TYPE_BUTTON) {
            HomeButtonBinding buttonBinding
                    = HomeButtonBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new ButtonHolder(buttonBinding);
        } else throw new IllegalArgumentException("undefined view type");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int ver = CleanerClient.getServerVersion();
        switch (position) {
            case 0:
                HomeCardBinding cardBinding = ((CardHolder) holder).binding;
                if (showIfNeed(ver != -1, cardBinding.getRoot())) {
                    cardBinding.summary.setText(String.format(activity.getString(R.string.module_version), ver));
                }
                break;
            case 1:
                HomeCardButtonBinding cardButtonBinding = ((CardButtonHolder) holder).binding;
                if (showIfNeed(ver != -1, cardButtonBinding.getRoot())) {
                 //   cardButtonBinding.summary.setText(String.format(Locale.ENGLISH,
                 //           activity.getString(R.string.enabled_app_count), count));
                    cardButtonBinding.getRoot().setOnClickListener(v -> activity.startActivity(
                            new Intent(activity, AppListActivity.class)));
                }
                break;
            case 2:

                break;
            case 3:
                HomeButtonBinding binding = ((ButtonHolder) holder).binding;
                binding.getRoot().setOnClickListener(  v -> {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(Uri.parse("https://github.com/GuhDoy/Media-Provider-Manager"));
                    activity.startActivity(intent);
                });
                break;
            case 4:
                HomeButtonBinding binding = ((ButtonHolder) holder).binding;
                binding.getRoot().setOnClickListener( v -> {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(Uri.parse("https://t.me/TabSwitch"));
                    activity.startActivity(intent);
                });
                break;
            case 5:
                HomeButtonBinding binding = ((ButtonHolder) holder).binding;
             binding.getRoot().setOnClickListener(view -> new AlertDialog.Builder(activity)
                     .setMessage("About page is under development.")
                     .setPositiveButton(android.R.string.ok, null)
                     .show());
                break;
        }
    }

    private boolean showIfNeed(boolean isShow, ViewGroup root) {
        ViewGroup.LayoutParams lp;
        if (isShow) {
            lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            lp = new ViewGroup.LayoutParams(0, 0);
        }
        root.setLayoutParams(lp);
        return isShow;
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    public static class CardHolder extends RecyclerView.ViewHolder {
        public HomeCardBinding binding;

        public CardHolder(@NonNull HomeCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class CardButtonHolder extends RecyclerView.ViewHolder {
        public HomeCardButtonBinding binding;

        public CardButtonHolder(@NonNull HomeCardButtonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class ButtonHolder extends RecyclerView.ViewHolder {
        public HomeButtonBinding binding;

        public ButtonHolder(@NonNull HomeButtonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
