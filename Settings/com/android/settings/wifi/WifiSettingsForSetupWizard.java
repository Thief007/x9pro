package com.android.settings.wifi;

import android.app.Dialog;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.SetupWizardListLayout;
import com.android.setupwizardlib.view.NavigationBar;

public class WifiSettingsForSetupWizard extends WifiSettings {
    private View mAddOtherNetworkItem;
    private TextView mEmptyFooter;
    private SetupWizardListLayout mLayout;
    private boolean mListLastEmpty = false;
    private View mMacAddressFooter;

    class C06181 implements OnClickListener {
        C06181() {
        }

        public void onClick(View v) {
            if (WifiSettingsForSetupWizard.this.mWifiManager.isWifiEnabled()) {
                WifiSettingsForSetupWizard.this.onAddNetworkPressed();
            }
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mLayout = (SetupWizardListLayout) inflater.inflate(R.layout.setup_wifi_layout, container, false);
        ListView list = this.mLayout.getListView();
        this.mAddOtherNetworkItem = inflater.inflate(R.layout.setup_wifi_add_network, list, false);
        list.addFooterView(this.mAddOtherNetworkItem, null, true);
        this.mAddOtherNetworkItem.setOnClickListener(new C06181());
        this.mMacAddressFooter = inflater.inflate(R.layout.setup_wifi_mac_address, list, false);
        list.addFooterView(this.mMacAddressFooter, null, false);
        NavigationBar navigationBar = this.mLayout.getNavigationBar();
        if (navigationBar != null) {
            ((WifiSetupActivity) getActivity()).onNavigationBarCreated(navigationBar);
        }
        return this.mLayout;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (hasNextButton()) {
            getNextButton().setVisibility(8);
        }
        updateMacAddress();
    }

    public void onAccessPointsChanged() {
        boolean z = false;
        super.onAccessPointsChanged();
        if (getPreferenceScreen().getPreferenceCount() == 0) {
            z = true;
        }
        updateFooter(z);
    }

    public void onWifiStateChanged(int state) {
        super.onWifiStateChanged(state);
        updateMacAddress();
    }

    public void registerForContextMenu(View view) {
    }

    WifiEnabler createWifiEnabler() {
        return null;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    public Dialog onCreateDialog(int dialogId) {
        Dialog dialog = super.onCreateDialog(dialogId);
        SetupWizardUtils.applyImmersiveFlags(dialog);
        return dialog;
    }

    protected void connect(WifiConfiguration config) {
        ((WifiSetupActivity) getActivity()).networkSelected();
        super.connect(config);
    }

    protected TextView initEmptyView() {
        this.mEmptyFooter = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.setup_wifi_empty, getListView(), false);
        return this.mEmptyFooter;
    }

    protected void updateFooter(boolean isEmpty) {
        if (getView() == null) {
            Log.d("WifiSettingsForSetupWizard", "exceptional life cycle that may cause JE");
            return;
        }
        if (isEmpty != this.mListLastEmpty && hasListView()) {
            ListView list = getListView();
            list.removeFooterView(this.mEmptyFooter);
            list.removeFooterView(this.mAddOtherNetworkItem);
            list.removeFooterView(this.mMacAddressFooter);
            if (isEmpty) {
                list.addFooterView(this.mEmptyFooter, null, false);
            } else {
                list.addFooterView(this.mAddOtherNetworkItem, null, true);
                list.addFooterView(this.mMacAddressFooter, null, false);
            }
            this.mListLastEmpty = isEmpty;
        }
    }

    public View setPinnedHeaderView(int layoutResId) {
        return null;
    }

    public void setPinnedHeaderView(View pinnedHeader) {
    }

    protected void setProgressBarVisible(boolean visible) {
        if (this.mLayout == null) {
            return;
        }
        if (visible) {
            this.mLayout.showProgressBar();
        } else {
            this.mLayout.hideProgressBar();
        }
    }

    private void updateMacAddress() {
        if (this.mMacAddressFooter != null) {
            CharSequence macAddress = null;
            if (this.mWifiManager != null) {
                WifiInfo connectionInfo = this.mWifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    macAddress = connectionInfo.getMacAddress();
                }
            }
            TextView macAddressTextView = (TextView) this.mMacAddressFooter.findViewById(R.id.mac_address);
            if (TextUtils.isEmpty(macAddress)) {
                macAddress = getString(R.string.status_unavailable);
            }
            macAddressTextView.setText(macAddress);
        }
    }
}
