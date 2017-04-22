package com.mediatek.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WpsCallback;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.provider.Settings.System;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import com.android.settings.R;
import com.android.setupwizardlib.R$styleable;

public class WifiWpsP2pEmSettings {
    private Channel mChannel;
    private Context mContext;
    private boolean mIsOnlyVisibilityWpsPin;
    private boolean mIsWifiP2pEmOpen;
    private boolean mIsWifiWpsEmOpen;
    private AccessPoint mSelectedAccessPoint;
    private WifiManager mWifiManager;
    private WifiP2pManager mWifiP2pManager;
    private AlertDialog mWifiWpsEmDialog;
    private WpsCallback mWpsListener;
    private int mWpsOrP2p = 0;

    class C07621 extends WpsCallback {
        C07621() {
        }

        public void onStarted(String pin) {
        }

        public void onSucceeded() {
        }

        public void onFailed(int reason) {
        }
    }

    class C07632 implements ActionListener {
        C07632() {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason) {
        }
    }

    class C07643 implements ActionListener {
        C07643() {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason) {
        }
    }

    class C07654 implements ActionListener {
        C07654() {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason) {
        }
    }

    class C07665 implements ActionListener {
        C07665() {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason) {
        }
    }

    class C07676 implements ActionListener {
        C07676() {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason) {
        }
    }

    class C07687 implements ActionListener {
        C07687() {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason) {
        }
    }

    class C07699 implements OnClickListener {
        C07699() {
        }

        public void onClick(DialogInterface dialog, int id) {
            WifiWpsP2pEmSettings.this.showWifiWpsEmDialog(4, WifiWpsP2pEmSettings.this.mSelectedAccessPoint, false);
        }
    }

    public WifiWpsP2pEmSettings(Context context, WifiP2pManager manager, Channel channel) {
        Log.d("@M_WifiWpsP2pEmSettings", "WifiWpsP2pEmSettings, WifiP2pManager");
        this.mContext = context;
        this.mWifiP2pManager = manager;
        this.mChannel = channel;
    }

    public WifiWpsP2pEmSettings(Context context, WifiManager manager) {
        Log.d("@M_WifiWpsP2pEmSettings", "WifiWpsP2pEmSettings, WifiManager");
        this.mContext = context;
        this.mWifiManager = manager;
    }

    public void resume() {
        boolean z = true;
        if (this.mWpsOrP2p == 0) {
            if (System.getInt(this.mContext.getContentResolver(), "wps_em_wifi_enable", 0) != 1) {
                z = false;
            }
            this.mIsWifiWpsEmOpen = z;
        } else if (this.mWpsOrP2p == 1) {
            if (System.getInt(this.mContext.getContentResolver(), "wps_em_p2p_enable", 0) != 1) {
                z = false;
            }
            this.mIsWifiP2pEmOpen = z;
        }
    }

    public void createOptionsMenu(Menu menu) {
        SubMenu nfc;
        if (this.mWpsOrP2p == 0 && this.mIsWifiWpsEmOpen) {
            nfc = menu.addSubMenu(R.string.wifi_wps_add_device);
            nfc.add(0, 13, 0, R.string.wifi_wps_em_reg_pin).setShowAsAction(1);
            nfc.add(0, 14, 0, R.string.wifi_wps_em_reg_pbc).setShowAsAction(1);
        } else if (this.mWpsOrP2p == 1 && this.mIsWifiP2pEmOpen) {
            nfc = menu.addSubMenu(R.string.wifi_p2p_nfc);
            nfc.add(0, 3, 0, R.string.wifi_write_wps_tag).setShowAsAction(1);
            nfc.add(0, 4, 0, R.string.wifi_write_p2p_tag).setShowAsAction(1);
            nfc.add(0, 5, 0, R.string.wifi_p2p_oob).setShowAsAction(1);
            nfc.add(0, 6, 0, R.string.wifi_p2p_auto_go_device).setShowAsAction(1);
            menu.add(0, 7, 0, R.string.wifi_p2p_auto_go).setCheckable(true).setShowAsAction(1);
        }
    }

    public void prepareOptionsMenu(Menu menu) {
        boolean z = true;
        if (this.mWpsOrP2p == 1 && this.mIsWifiP2pEmOpen) {
            MenuItem autoGoMenu = menu.findItem(7);
            if (System.getInt(this.mContext.getContentResolver(), "autonomous_go", 0) != 1) {
                z = false;
            }
            autoGoMenu.setChecked(z);
        }
    }

    public boolean optionsItemSelected(MenuItem item) {
        if (this.mWpsOrP2p != 0 || !this.mIsWifiWpsEmOpen) {
            if (this.mWpsOrP2p == 1 && this.mIsWifiP2pEmOpen) {
                switch (item.getItemId()) {
                    case 3:
                        Log.d("WifiWpsP2pEmSettings", "onOptionsItemSelected, MENU_ID_WPS_TAG");
                        this.mWifiP2pManager.stopPeerDiscovery(this.mChannel, null);
                        this.mWifiP2pManager.generateNfcToken(this.mChannel, 139365, new C07632());
                        return true;
                    case 4:
                        Log.d("WifiWpsP2pEmSettings", "onOptionsItemSelected, MENU_ID_P2P_TAG");
                        this.mWifiP2pManager.stopPeerDiscovery(this.mChannel, null);
                        this.mWifiP2pManager.generateNfcToken(this.mChannel, 139362, new C07643());
                        return true;
                    case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                        Log.d("WifiWpsP2pEmSettings", "onOptionsItemSelected, MENU_ID_OOB_DEVICE");
                        this.mWifiP2pManager.stopPeerDiscovery(this.mChannel, null);
                        this.mWifiP2pManager.generateNfcToken(this.mChannel, 139359, new C07654());
                        return true;
                    case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                        Log.d("WifiWpsP2pEmSettings", "onOptionsItemSelected, MENU_ID_AUTO_GO_DEVICE");
                        this.mWifiP2pManager.stopPeerDiscovery(this.mChannel, null);
                        this.mWifiP2pManager.generateNfcToken(this.mChannel, 139368, new C07665());
                        return true;
                    case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                        Log.d("WifiWpsP2pEmSettings", "onOptionsItemSelected, MENU_ID_AUTO_GO");
                        Log.d("WifiWpsP2pEmSettings", "MENU_ID_AUTO_GO isChecked: " + item.isChecked());
                        item.setChecked(!item.isChecked());
                        Log.d("WifiWpsP2pEmSettings", "MENU_ID_AUTO_GO isChecked: " + item.isChecked());
                        if (item.isChecked()) {
                            System.putInt(this.mContext.getContentResolver(), "autonomous_go", 1);
                            if (this.mWifiP2pManager != null) {
                                this.mWifiP2pManager.createGroup(this.mChannel, new C07676());
                            }
                        } else {
                            System.putInt(this.mContext.getContentResolver(), "autonomous_go", 0);
                            if (this.mWifiP2pManager != null) {
                                this.mWifiP2pManager.removeGroup(this.mChannel, new C07687());
                            }
                        }
                        return true;
                    default:
                        break;
                }
            }
        }
        switch (item.getItemId()) {
            case 13:
                showWifiWpsEmDialog(4, null, true);
                return true;
            case 14:
                Log.d("@M_WifiWpsP2pEmSettings", "click menu item: WPS Register PBC");
                this.mWpsListener = new C07621();
                WpsInfo config = new WpsInfo();
                config.setup = 0;
                this.mWifiManager.startWpsExternalRegistrar(config, this.mWpsListener);
                return true;
        }
        return false;
    }

    private void showWifiWpsEmDialog(int dialogId, AccessPoint accessPoint, boolean isOnlyVisibilityWpsPin) {
        if (this.mWifiWpsEmDialog != null) {
            ((Activity) this.mContext).removeDialog(dialogId);
            this.mWifiWpsEmDialog = null;
        }
        this.mSelectedAccessPoint = accessPoint;
        this.mIsOnlyVisibilityWpsPin = isOnlyVisibilityWpsPin;
        this.mWifiWpsEmDialog = createDialog(dialogId);
        if (this.mWifiWpsEmDialog != null) {
            this.mWifiWpsEmDialog.show();
        }
    }

    private AlertDialog createDialog(int dialogId) {
        if (this.mWpsOrP2p == 0 && this.mIsWifiWpsEmOpen) {
            switch (dialogId) {
                case 4:
                    return new WifiWpsEmDialog(this.mContext, this.mSelectedAccessPoint, this.mIsOnlyVisibilityWpsPin);
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    return new Builder(this.mContext).setMessage(R.string.wifi_confirm_config).setCancelable(false).setPositiveButton(R.string.yes, new C07699()).setNegativeButton(R.string.no, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            WifiWpsP2pEmSettings.this.showWifiWpsEmDialog(4, WifiWpsP2pEmSettings.this.mSelectedAccessPoint, true);
                        }
                    }).create();
            }
        }
        return null;
    }

    public void handleP2pStateChanged() {
        System.putInt(this.mContext.getContentResolver(), "autonomous_go", 0);
    }
}
