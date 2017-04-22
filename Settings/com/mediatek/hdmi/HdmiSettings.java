package com.mediatek.hdmi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioSystem;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.hdmi.IMtkHdmiManager.Stub;
import java.util.ArrayList;
import java.util.List;

public class HdmiSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private BroadcastReceiver mActionReceiver = new C07031();
    private Activity mActivity;
    private ListPreference mAudioOutputPref;
    private Context mContext;
    private AlertDialog mHDMIExcludeDialog;
    private IMtkHdmiManager mHdmiManager;
    private ContentObserver mHdmiSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Log.d("@M_HDMISettings", "mHdmiSettingsObserver onChanged: " + selfChange);
            HdmiSettings.this.updatePref();
        }
    };
    private boolean mRet;
    private SwitchPreference mToggleHdmiPref;
    private ListPreference mVideoResolutionPref;
    private ListPreference mVideoScalePref;

    class C07031 extends BroadcastReceiver {
        C07031() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("HDMISettings", "receive: " + action);
            if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(action) && Global.getInt(HdmiSettings.this.mActivity.getContentResolver(), "wifi_display_on", 0) == 0 && HdmiSettings.this.mRet) {
                Log.d("HDMISettings", "wifi display disconnected");
                try {
                    HdmiSettings.this.mHdmiManager.enableHdmi(true);
                    HdmiSettings.this.mRet = false;
                } catch (RemoteException e) {
                }
            }
        }
    }

    class C07053 implements OnClickListener {
        C07053() {
        }

        public void onClick(DialogInterface dialog, int which) {
            Log.d("HDMISettings", "HDMI on, turn off WifiDisplay");
            Global.putInt(HdmiSettings.this.mContext.getContentResolver(), "wifi_display_on", 0);
            HdmiSettings.this.mRet = true;
        }
    }

    class C07064 implements OnClickListener {
        C07064() {
        }

        public void onClick(DialogInterface dialog, int which) {
            Log.d("HDMISettings", "HDMI on, user DON'T turn off WifiDisplay -> turn off HDMI");
            HdmiSettings.this.mRet = false;
            HdmiSettings.this.updatePref();
        }
    }

    class C07075 implements OnCancelListener {
        C07075() {
        }

        public void onCancel(DialogInterface arg0) {
            Log.d("HDMISettings", "onCancel(): user DON'T turn off WifiDisplay -> turn off HDMI");
            HdmiSettings.this.mRet = false;
            HdmiSettings.this.updatePref();
        }
    }

    class C07086 implements OnDismissListener {
        C07086() {
        }

        public void onDismiss(DialogInterface arg0) {
            Log.d("HDMISettings", "onDismiss()");
        }
    }

    private void popupDialog(AlertDialog dialog) {
        dialog.getWindow().setType(2003);
        LayoutParams attributes = dialog.getWindow().getAttributes();
        attributes.privateFlags |= 16;
        dialog.show();
    }

    private void showDialog() {
        Resources mResource = Resources.getSystem();
        this.mHDMIExcludeDialog = new Builder(this.mContext).setMessage(this.mActivity.getResources().getString(R.string.hdmi_wfd_off_hdmi_on)).setPositiveButton(mResource.getString(17040344), new C07053()).setNegativeButton(mResource.getString(17040292), new C07064()).setOnCancelListener(new C07075()).setOnDismissListener(new C07086()).create();
        popupDialog(this.mHDMIExcludeDialog);
    }

    protected int getMetricsCategory() {
        return 100006;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("@M_HDMISettings", "HdmiSettings.onCreate()");
        addPreferencesFromResource(R.xml.hdmi_settings);
        this.mActivity = getActivity();
        this.mContext = getActivity();
        this.mToggleHdmiPref = (SwitchPreference) findPreference("hdmi_toggler");
        this.mToggleHdmiPref.setOnPreferenceChangeListener(this);
        this.mVideoResolutionPref = (ListPreference) findPreference("video_resolution");
        this.mVideoResolutionPref.setOnPreferenceChangeListener(this);
        this.mVideoScalePref = (ListPreference) findPreference("video_scale");
        this.mVideoScalePref.setOnPreferenceChangeListener(this);
        CharSequence[] entries = this.mVideoScalePref.getEntries();
        CharSequence[] values = this.mVideoScalePref.getEntryValues();
        List<CharSequence> scaleEntries = new ArrayList();
        for (CharSequence charSequence : values) {
            if (Integer.parseInt(charSequence.toString()) != 0) {
                scaleEntries.add(this.mActivity.getResources().getString(R.string.hdmi_scale_scale_down, new Object[]{values[i]}));
            } else {
                scaleEntries.add(this.mActivity.getResources().getString(R.string.hdmi_scale_no_scale));
            }
        }
        this.mVideoScalePref.setEntries((CharSequence[]) scaleEntries.toArray(new CharSequence[scaleEntries.size()]));
        this.mAudioOutputPref = (ListPreference) findPreference("audio_output");
        this.mAudioOutputPref.setOnPreferenceChangeListener(this);
        this.mHdmiManager = Stub.asInterface(ServiceManager.getService("mtkhdmi"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        this.mContext.registerReceiverAsUser(this.mActionReceiver, UserHandle.ALL, filter, null, null);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.mHdmiManager == null) {
            finish();
            return;
        }
        try {
            String title = getString(R.string.hdmi_settings);
            if (this.mHdmiManager.getDisplayType() == 2) {
                String hdmi = getString(R.string.hdmi_replace_hdmi);
                String mhl = getString(R.string.hdmi_replace_mhl);
                this.mActivity.setTitle(title.replaceAll(hdmi, mhl));
                this.mToggleHdmiPref.setTitle(this.mToggleHdmiPref.getTitle().toString().replaceAll(hdmi, mhl));
            } else {
                this.mActivity.setTitle(title);
            }
            if (!this.mHdmiManager.hasCapability(1)) {
                Log.d("@M_HDMISettings", "remove mVideoScalePref");
                getPreferenceScreen().removePreference(this.mVideoScalePref);
            }
            if (this.mHdmiManager.getAudioParameter(120, 3) <= 2) {
                Log.d("@M_HDMISettings", "remove mAudioOutputPref");
                getPreferenceScreen().removePreference(this.mAudioOutputPref);
            }
        } catch (RemoteException e) {
            Log.d("@M_HDMISettings", "HdmiManager RemoteException");
        }
    }

    public void onResume() {
        super.onResume();
        updatePref();
        this.mActivity.getContentResolver().registerContentObserver(System.getUriFor("hdmi_enable_status"), false, this.mHdmiSettingsObserver);
        this.mActivity.getContentResolver().registerContentObserver(System.getUriFor("hdmi_cable_plugged"), false, this.mHdmiSettingsObserver);
    }

    public void onPause() {
        this.mActivity.getContentResolver().unregisterContentObserver(this.mHdmiSettingsObserver);
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    private void updatePref() {
        Log.i("@M_HDMISettings", "updatePref");
        updatePrefStatus();
        updateSelectedResolution();
        updateSelectedScale();
        updateSelectedAudioOutput();
    }

    private void updatePrefStatus() {
        Log.i("@M_HDMISettings", "updatePrefStatus");
        Dialog dlg = this.mVideoResolutionPref.getDialog();
        if (dlg != null && dlg.isShowing()) {
            dlg.cancel();
        }
        dlg = this.mVideoScalePref.getDialog();
        if (dlg != null && dlg.isShowing()) {
            dlg.cancel();
        }
        boolean shouldEnable = false;
        try {
            shouldEnable = this.mHdmiManager.isSignalOutputting();
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        this.mVideoResolutionPref.setEnabled(shouldEnable);
        this.mVideoScalePref.setEnabled(shouldEnable);
        this.mToggleHdmiPref.setChecked(System.getInt(this.mActivity.getContentResolver(), "hdmi_enable_status", 1) == 1);
    }

    private void updateSelectedResolution() {
        int i = 0;
        Log.i("@M_HDMISettings", "updateSelectedResolution");
        Dialog dlg = this.mVideoResolutionPref.getDialog();
        if (dlg != null && dlg.isShowing()) {
            dlg.cancel();
        }
        int videoResolution = System.getInt(this.mActivity.getContentResolver(), "hdmi_video_resolution", 100);
        if (videoResolution > 100) {
            videoResolution = 100;
        }
        int[] supportedResolutions = new int[]{100};
        try {
            supportedResolutions = this.mHdmiManager.getSupportedResolutions();
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        CharSequence[] resolutionEntries = this.mActivity.getResources().getStringArray(R.array.hdmi_video_resolution_entries);
        List<CharSequence> realResolutionEntries = new ArrayList();
        List<CharSequence> realResolutionValues = new ArrayList();
        realResolutionEntries.add(this.mActivity.getResources().getString(R.string.hdmi_auto));
        realResolutionValues.add(Integer.toString(100));
        int length = supportedResolutions.length;
        while (i < length) {
            int resolution = supportedResolutions[i];
            try {
                realResolutionEntries.add(resolutionEntries[resolution]);
                realResolutionValues.add(Integer.toString(resolution));
            } catch (ArrayIndexOutOfBoundsException e2) {
                Log.d("@M_HDMISettings", e2.getMessage());
            }
            i++;
        }
        this.mVideoResolutionPref.setEntries((CharSequence[]) realResolutionEntries.toArray(new CharSequence[realResolutionEntries.size()]));
        this.mVideoResolutionPref.setEntryValues((CharSequence[]) realResolutionValues.toArray(new CharSequence[realResolutionValues.size()]));
        this.mVideoResolutionPref.setValue(Integer.toString(videoResolution));
    }

    private void updateSelectedScale() {
        Log.i("@M_HDMISettings", "updateSelectedScale");
        Dialog dlg = this.mVideoScalePref.getDialog();
        if (dlg != null && dlg.isShowing()) {
            dlg.cancel();
        }
        this.mVideoScalePref.setValue(Integer.toString(System.getInt(this.mActivity.getContentResolver(), "hdmi_video_scale", 0)));
    }

    private void updateSelectedAudioOutput() {
        Dialog dlg = this.mAudioOutputPref.getDialog();
        if (dlg != null && dlg.isShowing()) {
            dlg.cancel();
        }
        try {
            this.mAudioOutputPref.setEnabled(this.mHdmiManager.isSignalOutputting());
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        int audioOutputMode = System.getIntForUser(this.mActivity.getContentResolver(), "hdmi_audio_output_mode", 0, -2);
        this.mAudioOutputPref.setValue(Integer.toString(audioOutputMode));
        Log.i("@M_HDMISettings", "updateSelectedAudioOutput audioOutputMode: " + audioOutputMode);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        Log.d("@M_HDMISettings", key + " preference changed");
        try {
            if ("hdmi_toggler".equals(key)) {
                boolean checked = ((Boolean) newValue).booleanValue();
                Log.d("@M_HDMISettings", key + " enableHdmi start");
                if (Global.getInt(this.mActivity.getContentResolver(), "wifi_display_on", 0) != 0) {
                    showDialog();
                } else {
                    this.mHdmiManager.enableHdmi(checked);
                }
            } else if ("video_resolution".equals(key)) {
                this.mHdmiManager.setVideoResolution(Integer.parseInt((String) newValue));
            } else if ("video_scale".equals(key)) {
                int scaleValue = Integer.parseInt((String) newValue);
                if (scaleValue < 0 || scaleValue > 10) {
                    Log.d("@M_HDMISettings", "scaleValue error: " + scaleValue);
                } else {
                    this.mHdmiManager.setVideoScale(scaleValue);
                }
            } else if ("audio_output".equals(key)) {
                int which = Integer.parseInt((String) newValue);
                int maxChannel = 2;
                if (which == 1) {
                    maxChannel = this.mHdmiManager.getAudioParameter(120, 3);
                }
                AudioSystem.setParameters("HDMI_channel=" + maxChannel);
                System.putIntForUser(this.mActivity.getContentResolver(), "hdmi_audio_output_mode", which, -2);
                Log.d("@M_HDMISettings", "AudioSystem.setParameters HDMI_channel = " + maxChannel + ",which: " + which);
            }
        } catch (RemoteException e) {
            Log.w("@M_HDMISettings", "hdmi manager RemoteException: " + e.getMessage());
        }
        return true;
    }
}
