package com.android.settings.applications;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;
import com.android.settings.Utils;

public class LayoutPreference extends Preference {
    private View mRootView;

    public LayoutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
        int layoutResource = context.obtainStyledAttributes(attrs, R.styleable.Preference, 0, 0).getResourceId(3, 0);
        if (layoutResource == 0) {
            throw new IllegalArgumentException("LayoutPreference requires a layout to be defined");
        }
        View view = LayoutInflater.from(getContext()).inflate(layoutResource, null, false);
        ViewGroup allDetails = (ViewGroup) view.findViewById(com.android.settings.R.id.all_details);
        if (allDetails != null) {
            Utils.forceCustomPadding(allDetails, true);
        }
        this.mRootView = view;
        setShouldDisableView(false);
    }

    public View getView(View convertView, ViewGroup parent) {
        return this.mRootView;
    }

    protected void onBindView(View view) {
    }

    public View findViewById(int id) {
        return this.mRootView.findViewById(id);
    }
}
