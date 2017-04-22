package com.mediatek.settings.hotknot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Switch;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.mediatek.hotknot.HotKnotAdapter;

public final class HotKnotEnabler implements OnSwitchChangeListener {
    private HotKnotAdapter mAdapter;
    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new C07341();
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private boolean mUpdateStatusOnly = false;
    private boolean mValidListener;

    class C07341 extends BroadcastReceiver {
        C07341() {
        }

        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("com.mediatek.hotknot.extra.ADAPTER_STATE", -1);
            Log.d("@M_HotKnotEnabler", "HotKnot state changed to" + state);
            HotKnotEnabler.this.handleStateChanged(state);
        }
    }

    public HotKnotEnabler(Context context, SwitchBar switchBar) {
        this.mContext = context;
        this.mSwitchBar = switchBar;
        this.mSwitch = switchBar.getSwitch();
        this.mValidListener = false;
        this.mAdapter = HotKnotAdapter.getDefaultAdapter(this.mContext);
        if (this.mAdapter == null) {
            this.mSwitch.setEnabled(false);
        }
        setupSwitchBar();
        this.mIntentFilter = new IntentFilter("com.mediatek.hotknot.action.ADAPTER_STATE_CHANGED");
    }

    public void setupSwitchBar() {
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    public void resume() {
        if (this.mAdapter == null) {
            this.mSwitch.setEnabled(false);
            return;
        }
        int i;
        if (this.mAdapter.isEnabled()) {
            i = 2;
        } else {
            i = 1;
        }
        handleStateChanged(i);
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        this.mValidListener = true;
    }

    public void pause() {
        if (this.mAdapter != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mValidListener = false;
        }
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.d("@M_HotKnotEnabler", "onSwitchChanged to " + isChecked + ", mUpdateStatusOnly is " + this.mUpdateStatusOnly);
        if (!(this.mAdapter == null || this.mUpdateStatusOnly)) {
            if (isChecked) {
                this.mAdapter.enable();
            } else {
                this.mAdapter.disable();
            }
        }
        switchView.setEnabled(false);
    }

    void handleStateChanged(int state) {
        switch (state) {
            case 1:
                this.mUpdateStatusOnly = true;
                Log.d("@M_HotKnotEnabler", "Begin update status: set mUpdateStatusOnly to true");
                setSwitchChecked(false);
                this.mSwitch.setEnabled(true);
                this.mUpdateStatusOnly = false;
                Log.d("@M_HotKnotEnabler", "End update status: set mUpdateStatusOnly to false");
                return;
            case 2:
                this.mUpdateStatusOnly = true;
                Log.d("@M_HotKnotEnabler", "Begin update status: set mUpdateStatusOnly to true");
                setSwitchChecked(true);
                this.mSwitch.setEnabled(true);
                this.mUpdateStatusOnly = false;
                Log.d("@M_HotKnotEnabler", "End update status: set mUpdateStatusOnly to false");
                return;
            default:
                setSwitchChecked(false);
                this.mSwitch.setEnabled(true);
                return;
        }
    }

    private void setSwitchChecked(boolean isChecked) {
        if (isChecked != this.mSwitch.isChecked()) {
            this.mSwitch.setChecked(isChecked);
        }
    }
}
