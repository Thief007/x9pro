package com.mediatek.settings.wfd;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.UserHandle;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.Arrays;

public class WfdSettingsExt {
    public static final ArrayList<Integer> DEVICE_RESOLUTION_LIST = new ArrayList(Arrays.asList(new Integer[]{Integer.valueOf(2), Integer.valueOf(3)}));
    private Context mContext;
    private SwitchPreference mDevicePref;
    private DisplayManager mDisplayManager;
    private WifiP2pDevice mP2pDevice;
    private final BroadcastReceiver mReceiver = new C07411();

    class C07411 extends BroadcastReceiver {
        C07411() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("@M_WfdSettingsExt", "receive action: " + action);
            if ("android.net.wifi.p2p.THIS_DEVICE_CHANGED".equals(action)) {
                WfdSettingsExt.this.mP2pDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
                WfdSettingsExt.this.updateDeviceName();
            }
        }
    }

    public WfdSettingsExt(Context context) {
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
    }

    public void onCreateOptionMenu(Menu menu, WifiDisplayStatus status) {
        boolean z = true;
        int currentResolution = Global.getInt(this.mContext.getContentResolver(), "wifi_display_max_resolution", 0);
        Log.d("@M_WfdSettingsExt", "current resolution is " + currentResolution);
        if (DEVICE_RESOLUTION_LIST.contains(Integer.valueOf(currentResolution))) {
            MenuItem add = menu.add(0, 2, 0, R.string.wfd_change_resolution_menu_title);
            if (status.getFeatureState() != 3) {
                z = false;
            } else if (status.getActiveDisplayState() == 1) {
                z = false;
            }
            add.setEnabled(z).setShowAsAction(0);
        }
    }

    public boolean onOptionMenuSelected(MenuItem item, FragmentManager fragmentManager) {
        if (item.getItemId() != 2) {
            return false;
        }
        new WfdChangeResolutionFragment().show(fragmentManager, "change resolution");
        return true;
    }

    public boolean addAdditionalPreference(PreferenceScreen preferenceScreen, boolean available) {
        if (!available || !FeatureOption.MTK_WFD_SINK_SUPPORT) {
            return false;
        }
        if (this.mDevicePref == null) {
            this.mDevicePref = new SwitchPreference(this.mContext);
            if (this.mContext.getResources().getBoolean(17956947)) {
                this.mDevicePref.setIcon(R.drawable.ic_wfd_cellphone);
            } else {
                this.mDevicePref.setIcon(R.drawable.ic_wfd_laptop);
            }
            this.mDevicePref.setPersistent(false);
            this.mDevicePref.setSummary(R.string.wfd_sink_summary);
            this.mDevicePref.setOrder(2);
            this.mDevicePref.setIntent(new Intent("mediatek.settings.WFD_SINK_SETTINGS"));
        }
        preferenceScreen.addPreference(this.mDevicePref);
        updateDeviceName();
        ProgressCategory cat = new ProgressCategory(this.mContext, null, 0);
        cat.setEmptyTextRes(R.string.wifi_display_no_devices_found);
        cat.setOrder(3);
        cat.setTitle(R.string.wfd_device_category);
        preferenceScreen.addPreference(cat);
        return true;
    }

    public void onStart() {
        Log.d("@M_WfdSettingsExt", "onStart");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
            this.mContext.registerReceiver(this.mReceiver, filter);
        }
    }

    public void onStop() {
        Log.d("@M_WfdSettingsExt", "onStop");
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    private void updateDeviceName() {
        if (this.mP2pDevice != null && this.mDevicePref != null) {
            if (TextUtils.isEmpty(this.mP2pDevice.deviceName)) {
                this.mDevicePref.setTitle(this.mP2pDevice.deviceAddress);
            } else {
                this.mDevicePref.setTitle(this.mP2pDevice.deviceName);
            }
        }
    }

    public void handleWfdStatusChanged(WifiDisplayStatus status) {
        if (FeatureOption.MTK_WFD_SINK_SUPPORT) {
            boolean bStateOn = status != null ? status.getFeatureState() == 3 : false;
            Log.d("@M_WfdSettingsExt", "handleWfdStatusChanged bStateOn: " + bStateOn);
            if (bStateOn) {
                int wfdState = status.getActiveDisplayState();
                Log.d("@M_WfdSettingsExt", "handleWfdStatusChanged wfdState: " + wfdState);
                handleWfdStateChanged(wfdState, isSinkMode());
            } else {
                handleWfdStateChanged(0, isSinkMode());
            }
        }
    }

    private void handleWfdStateChanged(int wfdState, boolean sinkMode) {
        switch (wfdState) {
            case 0:
                if (!sinkMode) {
                    if (this.mDevicePref != null) {
                        this.mDevicePref.setEnabled(true);
                        this.mDevicePref.setChecked(false);
                    }
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        Intent intent = new Intent();
                        intent.setClassName("com.mediatek.floatmenu", "com.mediatek.floatmenu.FloatMenuService");
                        this.mContext.stopServiceAsUser(intent, UserHandle.CURRENT);
                        return;
                    }
                    return;
                }
                return;
            case 1:
                if (!sinkMode && this.mDevicePref != null) {
                    this.mDevicePref.setEnabled(false);
                    return;
                }
                return;
            case 2:
                if (!sinkMode && this.mDevicePref != null) {
                    this.mDevicePref.setEnabled(false);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void prepareWfdConnect() {
        if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
            Intent intent = new Intent();
            intent.setClassName("com.mediatek.floatmenu", "com.mediatek.floatmenu.FloatMenuService");
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        }
    }

    private boolean isSinkMode() {
        return this.mDisplayManager.isSinkEnabled();
    }
}
