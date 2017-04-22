package com.p003v.smartwake;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class CustomizedSwitchPreference extends SwitchPreference {
    public CustomizedSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomizedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomizedSwitchPreference(Context context) {
        super(context);
    }

    protected void onClick() {
    }

    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        Switch v = (Switch) view.findViewById(16909206);
        if (v != null) {
            v.setClickable(true);
        }
        return view;
    }
}
