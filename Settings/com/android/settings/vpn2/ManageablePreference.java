package com.android.settings.vpn2;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.settings.R;

public class ManageablePreference extends Preference {
    OnClickListener mListener;
    View mManageView;

    public ManageablePreference(Context context, AttributeSet attrs, OnClickListener onManage) {
        super(context, attrs);
        this.mListener = onManage;
        setPersistent(false);
        setOrder(0);
        setWidgetLayoutResource(R.layout.preference_vpn);
    }

    protected void onBindView(View view) {
        this.mManageView = view.findViewById(R.id.manage);
        this.mManageView.setOnClickListener(this.mListener);
        this.mManageView.setTag(this);
        super.onBindView(view);
    }
}
