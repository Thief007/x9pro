package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class AppHeader {
    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon, CharSequence label, Intent settingsIntent) {
        createAppHeader(fragment, icon, label, settingsIntent, 0);
    }

    public static void createAppHeader(Activity activity, Drawable icon, CharSequence label, Intent settingsIntent, ViewGroup pinnedHeader) {
        View bar = activity.getLayoutInflater().inflate(R.layout.app_header, pinnedHeader, false);
        setupHeaderView(activity, icon, label, settingsIntent, 0, bar);
        pinnedHeader.addView(bar);
    }

    public static void createAppHeader(SettingsPreferenceFragment fragment, Drawable icon, CharSequence label, Intent settingsIntent, int tintColorRes) {
        setupHeaderView(fragment.getActivity(), icon, label, settingsIntent, tintColorRes, fragment.setPinnedHeaderView((int) R.layout.app_header));
    }

    private static View setupHeaderView(final Activity activity, Drawable icon, CharSequence label, final Intent settingsIntent, int tintColorRes, View bar) {
        ImageView appIcon = (ImageView) bar.findViewById(R.id.app_icon);
        appIcon.setImageDrawable(icon);
        if (tintColorRes != 0) {
            appIcon.setImageTintList(ColorStateList.valueOf(activity.getColor(tintColorRes)));
        }
        ((TextView) bar.findViewById(R.id.app_name)).setText(label);
        View appSettings = bar.findViewById(R.id.app_settings);
        if (settingsIntent == null) {
            appSettings.setVisibility(8);
        } else {
            appSettings.setClickable(true);
            appSettings.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    activity.startActivity(settingsIntent);
                }
            });
        }
        return bar;
    }
}
