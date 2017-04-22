package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter.Filter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.bluetooth.ConfigHelper;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C03053();
    private static View mSettingsDialogView = null;
    private PreferenceGroup mAvailableDevicesCategory;
    private boolean mAvailableDevicesCategoryIsPresent;
    private BluetoothEnabler mBluetoothEnabler;
    private final OnClickListener mDeviceProfilesListener = new C03042();
    private TextView mEmptyView;
    private boolean mInitialScanStarted;
    private boolean mInitiateDiscoverable;
    private final IntentFilter mIntentFilter = new IntentFilter("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
    Preference mMyDevicePreference;
    private PreferenceGroup mPairedDevicesCategory;
    private final BroadcastReceiver mReceiver = new C03031();
    private SwitchBar mSwitchBar;

    class C03031 extends BroadcastReceiver {
        C03031() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
            if (action.equals("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED")) {
                updateDeviceName(context);
            }
            if (state == 12) {
                BluetoothSettings.this.mInitiateDiscoverable = true;
            }
        }

        private void updateDeviceName(Context context) {
            if (BluetoothSettings.this.mLocalAdapter.isEnabled() && BluetoothSettings.this.mMyDevicePreference != null) {
                BluetoothSettings.this.mMyDevicePreference.setSummary(context.getResources().getString(R.string.bluetooth_is_visible_message, new Object[]{BluetoothSettings.this.mLocalAdapter.getName()}));
            }
        }
    }

    class C03042 implements OnClickListener {
        C03042() {
        }

        public void onClick(View v) {
            if (v.getTag() instanceof CachedBluetoothDevice) {
                CachedBluetoothDevice device = (CachedBluetoothDevice) v.getTag();
                Log.d("BluetoothSettings", "onClick " + device.getName());
                Bundle args = new Bundle();
                args.putString("device_address", device.getDevice().getAddress());
                DeviceProfilesSettings profileSettings = new DeviceProfilesSettings();
                profileSettings.setArguments(args);
                profileSettings.show(BluetoothSettings.this.getFragmentManager(), DeviceProfilesSettings.class.getSimpleName());
                return;
            }
            Log.w("BluetoothSettings", "onClick() called for other View: " + v);
        }
    }

    static class C03053 extends BaseSearchIndexProvider {
        C03053() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.bluetooth_settings);
            data.screenTitle = res.getString(R.string.bluetooth_settings);
            result.add(data);
            LocalBluetoothManager lbtm = Utils.getLocalBtManager(context);
            if (lbtm != null) {
                for (BluetoothDevice device : lbtm.getBluetoothAdapter().getBondedDevices()) {
                    data = new SearchIndexableRaw(context);
                    data.title = device.getName();
                    data.screenTitle = res.getString(R.string.bluetooth_settings);
                    data.enabled = enabled;
                    result.add(data);
                }
            }
            return result;
        }
    }

    class C03064 implements LinkifyUtils.OnClickListener {
        C03064() {
        }

        public void onClick() {
            ((SettingsActivity) BluetoothSettings.this.getActivity()).startPreferencePanel(ScanningSettings.class.getName(), null, R.string.location_scanning_screen_title, null, null, 0);
        }
    }

    public BluetoothSettings() {
        super("no_config_bluetooth");
    }

    protected int getMetricsCategory() {
        return 24;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mInitialScanStarted = false;
        this.mInitiateDiscoverable = true;
        this.mEmptyView = (TextView) getView().findViewById(16908292);
        getListView().setEmptyView(this.mEmptyView);
        this.mEmptyView.setGravity(8388627);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mBluetoothEnabler = new BluetoothEnabler(activity, this.mSwitchBar);
        this.mBluetoothEnabler.setupSwitchBar();
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mBluetoothEnabler.teardownSwitchBar();
    }

    void addPreferencesForActivity() {
        addPreferencesFromResource(R.xml.bluetooth_settings);
        setHasOptionsMenu(true);
    }

    public void onResume() {
        if (this.mBluetoothEnabler != null) {
            this.mBluetoothEnabler.resume(getActivity());
        }
        super.onResume();
        this.mInitiateDiscoverable = true;
        if (isUiRestricted()) {
            setDeviceListGroup(getPreferenceScreen());
            removeAllDevices();
            this.mEmptyView.setText(R.string.bluetooth_empty_list_user_restricted);
            return;
        }
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
        if (this.mLocalAdapter != null) {
            updateContent(this.mLocalAdapter.getBluetoothState());
        }
    }

    public void onPause() {
        super.onPause();
        if (this.mBluetoothEnabler != null) {
            this.mBluetoothEnabler.pause();
        }
        this.mLocalAdapter.setScanMode(21);
        if (!isUiRestricted()) {
            getActivity().unregisterReceiver(this.mReceiver);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mPairedDevicesCategory != null) {
            this.mPairedDevicesCategory.removeAll();
        }
        if (this.mAvailableDevicesCategory != null) {
            this.mAvailableDevicesCategory.removeAll();
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean z = true;
        if (this.mLocalAdapter != null && !isUiRestricted()) {
            int textId;
            boolean bluetoothIsEnabled = this.mLocalAdapter.getBluetoothState() == 12;
            boolean isDiscovering = this.mLocalAdapter.isDiscovering();
            Log.d("BluetoothSettings", "onCreateOptionsMenu, isDiscovering " + isDiscovering);
            if (isDiscovering) {
                textId = R.string.bluetooth_searching_for_devices;
            } else {
                textId = R.string.bluetooth_search_for_devices;
            }
            MenuItem add = menu.add(0, 1, 0, textId);
            if (!bluetoothIsEnabled || isDiscovering) {
                z = false;
            }
            add.setEnabled(z).setShowAsAction(0);
            menu.add(0, 2, 0, R.string.bluetooth_rename_device).setEnabled(bluetoothIsEnabled).setShowAsAction(0);
            menu.add(0, 3, 0, R.string.bluetooth_show_received_files).setShowAsAction(0);
            if (ConfigHelper.isAdvanceSettingEnabled()) {
                menu.add(0, 5, 0, R.string.bluetooth_advanced_settings).setEnabled(bluetoothIsEnabled).setShowAsAction(0);
            }
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                if (this.mLocalAdapter.getBluetoothState() == 12) {
                    MetricsLogger.action(getActivity(), 160);
                    startScanning();
                }
                return true;
            case 2:
                MetricsLogger.action(getActivity(), 161);
                new BluetoothNameDialogFragment().show(getFragmentManager(), "rename device");
                return true;
            case 3:
                MetricsLogger.action(getActivity(), 162);
                getActivity().sendBroadcast(new Intent("android.btopp.intent.action.OPEN_RECEIVED_FILES"));
                return true;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                Intent i = new Intent("android.intent.action.MAIN");
                i.setAction("com.mediatek.bluetooth.settings.action.START_BT_ADV_SETTING");
                try {
                    startActivity(i);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.e("BluetoothSettings", "Unable to start activity " + i.toString());
                    return false;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanning() {
        if (!isUiRestricted()) {
            if (!this.mAvailableDevicesCategoryIsPresent) {
                getPreferenceScreen().addPreference(this.mAvailableDevicesCategory);
                this.mAvailableDevicesCategoryIsPresent = true;
            }
            if (this.mAvailableDevicesCategory != null) {
                setDeviceListGroup(this.mAvailableDevicesCategory);
                removeAllDevices();
            }
            this.mLocalManager.getCachedDeviceManager().clearNonBondedDevices();
            this.mAvailableDevicesCategory.removeAll();
            this.mInitialScanStarted = true;
            this.mLocalAdapter.startScanning(true);
        }
    }

    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        this.mLocalAdapter.stopScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    private void addDeviceCategory(PreferenceGroup preferenceGroup, int titleId, Filter filter, boolean addCachedDevices) {
        preferenceGroup.setTitle(titleId);
        getPreferenceScreen().addPreference(preferenceGroup);
        setFilter(filter);
        setDeviceListGroup(preferenceGroup);
        if (addCachedDevices) {
            addCachedDevices();
        }
        preferenceGroup.setEnabled(true);
    }

    private void updateContent(int bluetoothState) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int messageId = 0;
        switch (bluetoothState) {
            case 10:
                setOffMessage();
                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                    break;
                }
                break;
            case 11:
                messageId = R.string.bluetooth_turning_on;
                this.mInitialScanStarted = false;
                break;
            case 12:
                preferenceScreen.removeAll();
                preferenceScreen.setOrderingAsAdded(true);
                this.mDevicePreferenceMap.clear();
                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                    break;
                }
                if (this.mPairedDevicesCategory == null) {
                    this.mPairedDevicesCategory = new PreferenceCategory(getActivity());
                } else {
                    this.mPairedDevicesCategory.removeAll();
                }
                addDeviceCategory(this.mPairedDevicesCategory, R.string.bluetooth_preference_paired_devices, BluetoothDeviceFilter.BONDED_DEVICE_FILTER, true);
                int numberOfPairedDevices = this.mPairedDevicesCategory.getPreferenceCount();
                if (isUiRestricted() || numberOfPairedDevices <= 0) {
                    preferenceScreen.removePreference(this.mPairedDevicesCategory);
                }
                if (this.mAvailableDevicesCategory == null) {
                    this.mAvailableDevicesCategory = new BluetoothProgressCategory(getActivity());
                    this.mAvailableDevicesCategory.setSelectable(false);
                } else {
                    this.mAvailableDevicesCategory.removeAll();
                }
                addDeviceCategory(this.mAvailableDevicesCategory, R.string.bluetooth_preference_found_devices, BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER, this.mInitialScanStarted);
                int numberOfAvailableDevices = this.mAvailableDevicesCategory.getPreferenceCount();
                if (!this.mInitialScanStarted) {
                    startScanning();
                }
                if (this.mMyDevicePreference == null) {
                    this.mMyDevicePreference = new Preference(getActivity());
                }
                this.mMyDevicePreference.setSummary(getResources().getString(R.string.bluetooth_is_visible_message, new Object[]{this.mLocalAdapter.getName()}));
                this.mMyDevicePreference.setSelectable(false);
                preferenceScreen.addPreference(this.mMyDevicePreference);
                getActivity().invalidateOptionsMenu();
                if (this.mInitiateDiscoverable) {
                    this.mLocalAdapter.setScanMode(23);
                    this.mInitiateDiscoverable = false;
                }
                return;
            case 13:
                messageId = R.string.bluetooth_turning_off;
                break;
        }
        setDeviceListGroup(preferenceScreen);
        removeAllDevices();
        if (messageId != 0) {
            this.mEmptyView.setText(messageId);
        }
        if (!isUiRestricted()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void setOffMessage() {
        if (this.mEmptyView != null) {
            CharSequence briefText = getText(R.string.bluetooth_empty_list_bluetooth_off);
            if (Global.getInt(getActivity().getContentResolver(), "ble_scan_always_enabled", 0) == 1) {
                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append(briefText);
                contentBuilder.append("\n\n");
                contentBuilder.append(getText(R.string.ble_scan_notify_text));
                LinkifyUtils.linkify(this.mEmptyView, contentBuilder, new C03064());
            } else {
                this.mEmptyView.setText(briefText, BufferType.SPANNABLE);
            }
            getPreferenceScreen().removeAll();
            ((Spannable) this.mEmptyView.getText()).setSpan(new TextAppearanceSpan(getActivity(), 16973892), 0, briefText.length(), 33);
        }
    }

    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        if (12 == bluetoothState) {
            this.mInitiateDiscoverable = true;
        }
        updateContent(bluetoothState);
    }

    public void onScanningStateChanged(boolean started) {
        Log.d("BluetoothSettings", "onScanningStateChanged() started : " + started);
        super.onScanningStateChanged(started);
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        setDeviceListGroup(getPreferenceScreen());
        removeAllDevices();
        updateContent(this.mLocalAdapter.getBluetoothState());
    }

    void initDevicePreference(BluetoothDevicePreference preference) {
        if (preference.getCachedDevice().getBondState() == 12) {
            preference.setOnSettingsClickListener(this.mDeviceProfilesListener);
        }
    }

    protected int getHelpResource() {
        return R.string.help_url_bluetooth;
    }
}
