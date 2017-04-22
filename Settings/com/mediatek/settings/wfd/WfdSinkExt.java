package com.mediatek.settings.wfd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

public class WfdSinkExt {
    private Context mContext;
    private DisplayManager mDisplayManager;
    private int mPreWfdState = -1;
    private final BroadcastReceiver mReceiver = new C07421();
    private WfdSinkSurfaceFragment mSinkFragment;
    private Toast mSinkToast;
    private boolean mUiPortrait = false;

    class C07421 extends BroadcastReceiver {
        C07421() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("@M_WfdSinkExt", "receive action: " + action);
            if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action)) {
                WfdSinkExt.this.handleWfdStatusChanged(WfdSinkExt.this.mDisplayManager.getWifiDisplayStatus());
            } else if ("com.mediatek.wfd.portrait".equals(action)) {
                WfdSinkExt.this.mUiPortrait = true;
            }
        }
    }

    public WfdSinkExt(Context context) {
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
    }

    public void onStart() {
        Log.d("@M_WfdSinkExt", "onStart");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            handleWfdStatusChanged(this.mDisplayManager.getWifiDisplayStatus());
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
            filter.addAction("com.mediatek.wfd.portrait");
            this.mContext.registerReceiver(this.mReceiver, filter);
        }
    }

    public void onStop() {
        Log.d("@M_WfdSinkExt", "onStop");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    public void setupWfdSinkConnection(Surface surface) {
        Log.d("@M_WfdSinkExt", "setupWfdSinkConnection");
        setWfdMode(true);
        waitWfdSinkConnection(surface);
    }

    public void disconnectWfdSinkConnection() {
        Log.d("@M_WfdSinkExt", "disconnectWfdSinkConnection");
        this.mDisplayManager.disconnectWifiDisplay();
        setWfdMode(false);
        Log.d("@M_WfdSinkExt", "after disconnectWfdSinkConnection");
    }

    public void registerSinkFragment(WfdSinkSurfaceFragment fragment) {
        this.mSinkFragment = fragment;
    }

    private void handleWfdStatusChanged(WifiDisplayStatus status) {
        boolean bStateOn = status != null && status.getFeatureState() == 3;
        Log.d("@M_WfdSinkExt", "handleWfdStatusChanged bStateOn: " + bStateOn);
        if (bStateOn) {
            int wfdState = status.getActiveDisplayState();
            Log.d("@M_WfdSinkExt", "handleWfdStatusChanged wfdState: " + wfdState);
            handleWfdStateChanged(wfdState, isSinkMode());
            this.mPreWfdState = wfdState;
            return;
        }
        handleWfdStateChanged(0, isSinkMode());
        this.mPreWfdState = -1;
    }

    private void handleWfdStateChanged(int wfdState, boolean sinkMode) {
        switch (wfdState) {
            case 0:
                if (sinkMode) {
                    Log.d("@M_WfdSinkExt", "dismiss fragment");
                    if (this.mSinkFragment != null) {
                        this.mSinkFragment.dismissAllowingStateLoss();
                    }
                    setWfdMode(false);
                }
                if (this.mPreWfdState == 2) {
                    showToast(false);
                }
                this.mUiPortrait = false;
                return;
            case 2:
                if (sinkMode) {
                    Log.d("@M_WfdSinkExt", "mUiPortrait: " + this.mUiPortrait);
                    this.mSinkFragment.requestOrientation(this.mUiPortrait);
                    SharedPreferences preferences = this.mContext.getSharedPreferences("wifi_display", 0);
                    if (preferences.getBoolean("wifi_display_hide_guide", true) && this.mSinkFragment != null) {
                        this.mSinkFragment.addWfdSinkGuide();
                        preferences.edit().putBoolean("wifi_display_hide_guide", false).commit();
                    }
                    if (this.mPreWfdState != 2) {
                        showToast(true);
                    }
                }
                this.mUiPortrait = false;
                return;
            default:
                return;
        }
    }

    private void showToast(boolean connected) {
        if (this.mSinkToast != null) {
            this.mSinkToast.cancel();
        }
        this.mSinkToast = Toast.makeText(this.mContext, connected ? R.string.wfd_sink_toast_enjoy : R.string.wfd_sink_toast_disconnect, connected ? 1 : 0);
        this.mSinkToast.show();
    }

    private boolean isSinkMode() {
        return this.mDisplayManager.isSinkEnabled();
    }

    private void setWfdMode(boolean sink) {
        Log.d("@M_WfdSinkExt", "setWfdMode " + sink);
        this.mDisplayManager.enableSink(sink);
    }

    private void waitWfdSinkConnection(Surface surface) {
        this.mDisplayManager.waitWifiDisplayConnection(surface);
    }

    public void sendUibcEvent(String eventDesc) {
        this.mDisplayManager.sendUibcInputEvent(eventDesc);
    }
}
