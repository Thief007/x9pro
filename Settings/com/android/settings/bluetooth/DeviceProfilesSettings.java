package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDevice.Callback;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;
import java.util.HashMap;

public final class DeviceProfilesSettings extends DialogFragment implements Callback, OnClickListener, View.OnClickListener {
    private AlertDialog mAlertDialog;
    private final HashMap<LocalBluetoothProfile, CheckBoxPreference> mAutoConnectPrefs = new HashMap();
    private CachedBluetoothDevice mCachedDevice;
    private AlertDialog mDisconnectDialog;
    private LocalBluetoothManager mManager;
    private ViewGroup mProfileContainer;
    private boolean mProfileGroupIsRemoved;
    private TextView mProfileLabel;
    private LocalBluetoothProfileManager mProfileManager;
    private View mRootView;

    class C03071 implements TextWatcher {
        C03071() {
        }

        public void afterTextChanged(Editable s) {
            DeviceProfilesSettings.this.mAlertDialog.getButton(-1).setEnabled(s.toString().trim().length() > 0);
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mManager = Utils.getLocalBtManager(getActivity());
        CachedBluetoothDeviceManager deviceManager = this.mManager.getCachedDeviceManager();
        BluetoothDevice remoteDevice = this.mManager.getBluetoothAdapter().getRemoteDevice(getArguments().getString("device_address"));
        this.mCachedDevice = deviceManager.findDevice(remoteDevice);
        if (this.mCachedDevice == null) {
            this.mCachedDevice = deviceManager.addDevice(this.mManager.getBluetoothAdapter(), this.mManager.getProfileManager(), remoteDevice);
        }
        this.mProfileManager = this.mManager.getProfileManager();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.mRootView = LayoutInflater.from(getContext()).inflate(R.layout.device_profiles_settings, null);
        this.mProfileContainer = (ViewGroup) this.mRootView.findViewById(R.id.profiles_section);
        this.mProfileLabel = (TextView) this.mRootView.findViewById(R.id.profiles_label);
        EditText deviceName = (EditText) this.mRootView.findViewById(R.id.name);
        deviceName.setText(this.mCachedDevice.getName(), BufferType.EDITABLE);
        this.mAlertDialog = new Builder(getContext()).setView(this.mRootView).setNegativeButton(R.string.forget, this).setPositiveButton(R.string.okay, this).setTitle(R.string.bluetooth_preference_paired_devices).create();
        deviceName.addTextChangedListener(new C03071());
        return this.mAlertDialog;
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                this.mCachedDevice.unpair();
                Utils.updateSearchIndex(getContext(), BluetoothSettings.class.getName(), this.mCachedDevice.getName(), getString(R.string.bluetooth_settings), R.drawable.ic_settings_bluetooth, false);
                return;
            case -1:
                this.mCachedDevice.setName(((EditText) this.mRootView.findViewById(R.id.name)).getText().toString());
                return;
            default:
                return;
        }
    }

    public void onDestroy() {
        Log.d("DeviceProfilesSettings", "onDestroy");
        super.onDestroy();
        if (this.mDisconnectDialog != null) {
            this.mDisconnectDialog.dismiss();
            this.mDisconnectDialog = null;
        }
        this.mAlertDialog = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void onResume() {
        super.onResume();
        Log.d("DeviceProfilesSettings", "onResume");
        this.mManager.setForegroundActivity(getActivity());
        if (this.mCachedDevice != null) {
            this.mCachedDevice.registerCallback(this);
            Log.d("DeviceProfilesSettings", "onResume, registerCallback");
            if (this.mCachedDevice.getBondState() == 10) {
                dismiss();
            } else {
                addPreferencesForProfiles();
                refresh();
            }
        }
    }

    public void onPause() {
        super.onPause();
        Log.d("DeviceProfilesSettings", "onPause");
        if (this.mCachedDevice != null) {
            this.mCachedDevice.unregisterCallback(this);
            Log.d("DeviceProfilesSettings", "onPause, unregisterCallback");
        }
        this.mManager.setForegroundActivity(null);
    }

    private void addPreferencesForProfiles() {
        this.mProfileContainer.removeAllViews();
        for (LocalBluetoothProfile profile : this.mCachedDevice.getConnectableProfiles()) {
            this.mProfileContainer.addView(createProfilePreference(profile));
        }
        if (this.mCachedDevice.getPhonebookPermissionChoice() != 0) {
            this.mProfileContainer.addView(createProfilePreference(this.mManager.getProfileManager().getPbapProfile()));
        }
        MapProfile mapProfile = this.mManager.getProfileManager().getMapProfile();
        if (this.mCachedDevice.getMessagePermissionChoice() != 0) {
            this.mProfileContainer.addView(createProfilePreference(mapProfile));
        }
        showOrHideProfileGroup();
    }

    private void showOrHideProfileGroup() {
        int numProfiles = this.mProfileContainer.getChildCount();
        if (!this.mProfileGroupIsRemoved && numProfiles == 0) {
            this.mProfileContainer.setVisibility(8);
            this.mProfileLabel.setVisibility(8);
            this.mProfileGroupIsRemoved = true;
        } else if (this.mProfileGroupIsRemoved && numProfiles != 0) {
            this.mProfileContainer.setVisibility(0);
            this.mProfileLabel.setVisibility(0);
            this.mProfileGroupIsRemoved = false;
        }
    }

    private CheckBox createProfilePreference(LocalBluetoothProfile profile) {
        CheckBox pref = new CheckBox(getActivity());
        pref.setTag(profile.toString());
        pref.setText(profile.getNameResource(this.mCachedDevice.getDevice()));
        pref.setOnClickListener(this);
        refreshProfilePreference(pref, profile);
        return pref;
    }

    public void onClick(View v) {
        if (v instanceof CheckBox) {
            onProfileClicked(getProfileOf(v), (CheckBox) v);
        }
    }

    private void onProfileClicked(LocalBluetoothProfile profile, CheckBox profilePref) {
        boolean z = true;
        BluetoothDevice device = this.mCachedDevice.getDevice();
        if ("PBAP Server".equals(profilePref.getTag())) {
            int newPermission;
            if (this.mCachedDevice.getPhonebookPermissionChoice() == 1) {
                newPermission = 2;
            } else {
                newPermission = 1;
            }
            this.mCachedDevice.setPhonebookPermissionChoice(newPermission);
            if (newPermission != 1) {
                z = false;
            }
            profilePref.setChecked(z);
            return;
        }
        if (profilePref.isChecked()) {
            if (profile instanceof MapProfile) {
                this.mCachedDevice.setMessagePermissionChoice(1);
            }
            Log.d("DeviceProfilesSettings", this.mCachedDevice.getName() + " " + profile.toString() + " isPreferred() : " + profile.isPreferred(device));
            if (!profile.isPreferred(device)) {
                profile.setPreferred(device, true);
                Log.d("DeviceProfilesSettings", profile.toString() + " setPreferred true and connect profile");
                this.mCachedDevice.connectProfile(profile);
            } else if (profile instanceof PanProfile) {
                this.mCachedDevice.connectProfile(profile);
            } else {
                Log.d("DeviceProfilesSettings", profile.toString() + " setPreferred false");
                profile.setPreferred(device, false);
            }
            refreshProfilePreference(profilePref, profile);
        } else {
            profilePref.setChecked(true);
            askDisconnect(this.mManager.getForegroundActivity(), profile);
        }
    }

    private void askDisconnect(Context context, final LocalBluetoothProfile profile) {
        final CachedBluetoothDevice device = this.mCachedDevice;
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String profileName = context.getString(profile.getNameResource(device.getDevice()));
        String title = context.getString(R.string.bluetooth_disable_profile_title);
        String message = context.getString(R.string.bluetooth_disable_profile_message, new Object[]{profileName, name});
        this.mDisconnectDialog = Utils.showDisconnectDialog(context, this.mDisconnectDialog, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                device.disconnect(profile);
                Log.d("DeviceProfilesSettings", "disconnect " + profile.toString() + " , setPreferred false");
                profile.setPreferred(device.getDevice(), false);
                if (profile instanceof MapProfile) {
                    device.setMessagePermissionChoice(2);
                }
                DeviceProfilesSettings.this.refreshProfilePreference(DeviceProfilesSettings.this.findProfile(profile.toString()), profile);
            }
        }, title, Html.fromHtml(message));
    }

    public void onDeviceAttributesChanged() {
        refresh();
    }

    private void refresh() {
        EditText deviceNameField = (EditText) this.mRootView.findViewById(R.id.name);
        if (deviceNameField != null) {
            deviceNameField.setText(this.mCachedDevice.getName());
        }
        refreshProfiles();
    }

    private void refreshProfiles() {
        for (LocalBluetoothProfile profile : this.mCachedDevice.getConnectableProfiles()) {
            CheckBox profilePref = findProfile(profile.toString());
            if (profilePref == null) {
                this.mProfileContainer.addView(createProfilePreference(profile));
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }
        for (LocalBluetoothProfile profile2 : this.mCachedDevice.getRemovedProfiles()) {
            profilePref = findProfile(profile2.toString());
            if (profilePref != null) {
                Log.d("DeviceProfilesSettings", "Removing " + profile2.toString() + " from profile list");
                this.mProfileContainer.removeView(profilePref);
            }
        }
        showOrHideProfileGroup();
    }

    private CheckBox findProfile(String profile) {
        return (CheckBox) this.mProfileContainer.findViewWithTag(profile);
    }

    private void refreshProfilePreference(CheckBox profilePref, LocalBluetoothProfile profile) {
        boolean z;
        boolean z2 = true;
        BluetoothDevice device = this.mCachedDevice.getDevice();
        Log.d("DeviceProfilesSettings", "isBusy : " + this.mCachedDevice.isBusy());
        if (this.mCachedDevice.isBusy()) {
            z = false;
        } else {
            z = true;
        }
        profilePref.setEnabled(z);
        if (profile instanceof MapProfile) {
            if (this.mCachedDevice.getMessagePermissionChoice() != 1) {
                z2 = false;
            }
            profilePref.setChecked(z2);
        } else if (profile instanceof PbapServerProfile) {
            if (this.mCachedDevice.getPhonebookPermissionChoice() != 1) {
                z2 = false;
            }
            profilePref.setChecked(z2);
        } else if (profile instanceof PanProfile) {
            if (profile.getConnectionStatus(device) != 2) {
                z2 = false;
            }
            profilePref.setChecked(z2);
        } else {
            Log.d("DeviceProfilesSettings", profile.toString() + " isPreferred : " + profile.isPreferred(device));
            profilePref.setChecked(profile.isPreferred(device));
        }
    }

    private LocalBluetoothProfile getProfileOf(View v) {
        if (!(v instanceof CheckBox)) {
            return null;
        }
        String key = (String) v.getTag();
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        try {
            return this.mProfileManager.getProfileByName(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
