package com.android.settings.applications;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Space;

public class SpacePreference extends Preference {
    private int mHeight;

    public SpacePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 16842894);
    }

    public SpacePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SpacePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mHeight = context.obtainStyledAttributes(attrs, new int[]{16842997}, defStyleAttr, defStyleRes).getDimensionPixelSize(0, 0);
    }

    protected View onCreateView(ViewGroup parent) {
        return new Space(getContext());
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        view.setLayoutParams(new LayoutParams(-1, this.mHeight));
    }
}
