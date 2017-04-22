package com.android.settings.applock;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import com.android.settings.R;

public abstract class BaseActivity extends Activity {
    private boolean mAllowFullScreen = false;

    public abstract void findViews();

    public abstract void setListeners();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViews();
        try {
            init();
        } catch (Exception e) {
        }
        setListeners();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_back));
    }

    protected void onResume() {
        onResume_init();
        super.onResume();
    }

    protected void onResume_init() {
    }

    protected void init() {
    }
}
