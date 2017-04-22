package com.p003v.smartwake;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SmartWakeSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener, OnSwitchChangeListener {
    private PreferenceCategory mAppsPerCategory;
    private CheckBoxPreference mCheckBoxDown;
    private CheckBoxPreference mCheckBoxLeft;
    private CheckBoxPreference mCheckBoxRight;
    private CheckBoxPreference mCheckBoxU;
    private CheckBoxPreference mCheckBoxUp;
    private Context mContext;
    private SwitchBar mSwitchBar;
    private boolean mSwitchChecked;
    private CustomizedSwitchPreference mSwitchPreferenceC;
    private CustomizedSwitchPreference mSwitchPreferenceE;
    private CustomizedSwitchPreference mSwitchPreferenceM;
    private CustomizedSwitchPreference mSwitchPreferenceO;
    private CustomizedSwitchPreference mSwitchPreferenceW;
    private boolean mValidListener = false;

    protected int getMetricsCategory() {
        return 81;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SmartWakeSettings", "onCreate() :");
        this.mContext = getActivity();
        ContentResolver resolver = getActivity().getContentResolver();
        addPreferencesFromResource(R.xml.v_smartwake_settings);
        this.mAppsPerCategory = (PreferenceCategory) findPreference("smartwake_support_gestures");
        this.mCheckBoxU = (CheckBoxPreference) findPreference("smartwake_settings_u");
        this.mCheckBoxUp = (CheckBoxPreference) findPreference("smartwake_settings_up");
        this.mCheckBoxDown = (CheckBoxPreference) findPreference("smartwake_settings_down");
        this.mCheckBoxRight = (CheckBoxPreference) findPreference("smartwake_settings_right");
        this.mCheckBoxLeft = (CheckBoxPreference) findPreference("smartwake_settings_left");
        this.mSwitchPreferenceC = (CustomizedSwitchPreference) findPreference("smartwake_settings_c");
        this.mSwitchPreferenceE = (CustomizedSwitchPreference) findPreference("smartwake_settings_e");
        this.mSwitchPreferenceW = (CustomizedSwitchPreference) findPreference("smartwake_settings_w");
        this.mSwitchPreferenceM = (CustomizedSwitchPreference) findPreference("smartwake_settings_m");
        this.mSwitchPreferenceO = (CustomizedSwitchPreference) findPreference("smartwake_settings_o");
        this.mCheckBoxU.setOnPreferenceChangeListener(this);
        this.mCheckBoxUp.setOnPreferenceChangeListener(this);
        this.mCheckBoxDown.setOnPreferenceChangeListener(this);
        this.mCheckBoxRight.setOnPreferenceChangeListener(this);
        this.mCheckBoxLeft.setOnPreferenceChangeListener(this);
        this.mSwitchPreferenceC.setOnPreferenceChangeListener(this);
        this.mSwitchPreferenceC.setOnPreferenceClickListener(this);
        this.mSwitchPreferenceE.setOnPreferenceChangeListener(this);
        this.mSwitchPreferenceE.setOnPreferenceClickListener(this);
        this.mSwitchPreferenceW.setOnPreferenceChangeListener(this);
        this.mSwitchPreferenceW.setOnPreferenceClickListener(this);
        this.mSwitchPreferenceM.setOnPreferenceChangeListener(this);
        this.mSwitchPreferenceM.setOnPreferenceClickListener(this);
        this.mSwitchPreferenceO.setOnPreferenceChangeListener(this);
        this.mSwitchPreferenceO.setOnPreferenceClickListener(this);
    }

    public void onResume() {
        Intent mainIntent = new Intent("android.intent.action.MAIN", null);
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> allIntentApps = this.mContext.getPackageManager().queryIntentActivities(mainIntent, 0);
        String packageNameC = SystemProperties.get("persist.sys.smartwake_c_name", "com.mediatek.camera&com.android.camera.CameraLauncher").split("&")[0];
        String activityNameC = SystemProperties.get("persist.sys.smartwake_c_name", "com.mediatek.camera&com.android.camera.CameraLauncher").split("&")[1];
        int i = 0;
        while (i < allIntentApps.size()) {
            if (((ResolveInfo) allIntentApps.get(i)).activityInfo.packageName.equals(packageNameC) && ((ResolveInfo) allIntentApps.get(i)).activityInfo.name.equals(activityNameC)) {
                this.mSwitchPreferenceC.setTitle(this.mContext.getResources().getString(R.string.smartwake_settings_c_title) + " " + ((ResolveInfo) allIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
                break;
            }
            i++;
        }
        String packageNameE = SystemProperties.get("persist.sys.smartwake_e_name", "com.android.browser&com.android.browser.BrowserActivity").split("&")[0];
        String activityNameE = SystemProperties.get("persist.sys.smartwake_e_name", "com.android.browser&com.android.browser.BrowserActivity").split("&")[1];
        i = 0;
        while (i < allIntentApps.size()) {
            if (((ResolveInfo) allIntentApps.get(i)).activityInfo.packageName.equals(packageNameE) && ((ResolveInfo) allIntentApps.get(i)).activityInfo.name.equals(activityNameE)) {
                this.mSwitchPreferenceE.setTitle(this.mContext.getResources().getString(R.string.smartwake_settings_e_title) + " " + ((ResolveInfo) allIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
                break;
            }
            i++;
        }
        String packageNameW = SystemProperties.get("persist.sys.smartwake_w_name", "com.mediatek.filemanager&com.mediatek.filemanager.FileManagerOperationActivity").split("&")[0];
        String activityNameW = SystemProperties.get("persist.sys.smartwake_w_name", "com.mediatek.filemanager&com.mediatek.filemanager.FileManagerOperationActivity").split("&")[1];
        i = 0;
        while (i < allIntentApps.size()) {
            if (((ResolveInfo) allIntentApps.get(i)).activityInfo.packageName.equals(packageNameW) && ((ResolveInfo) allIntentApps.get(i)).activityInfo.name.equals(activityNameW)) {
                this.mSwitchPreferenceW.setTitle(this.mContext.getResources().getString(R.string.smartwake_settings_w_title) + " " + ((ResolveInfo) allIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
                break;
            }
            i++;
        }
        String packageNameM = SystemProperties.get("persist.sys.smartwake_m_name", "com.android.music&com.android.music.MusicBrowserActivity").split("&")[0];
        String activityNameM = SystemProperties.get("persist.sys.smartwake_m_name", "com.android.music&com.android.music.MusicBrowserActivity").split("&")[1];
        i = 0;
        while (i < allIntentApps.size()) {
            if (((ResolveInfo) allIntentApps.get(i)).activityInfo.packageName.equals(packageNameM) && ((ResolveInfo) allIntentApps.get(i)).activityInfo.name.equals(activityNameM)) {
                this.mSwitchPreferenceM.setTitle(this.mContext.getResources().getString(R.string.smartwake_settings_m_title) + " " + ((ResolveInfo) allIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
                break;
            }
            i++;
        }
        String packageNameO = SystemProperties.get("persist.sys.smartwake_o_name", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[0];
        String activityNameO = SystemProperties.get("persist.sys.smartwake_o_name", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[1];
        i = 0;
        while (i < allIntentApps.size()) {
            if (((ResolveInfo) allIntentApps.get(i)).activityInfo.packageName.equals(packageNameO) && ((ResolveInfo) allIntentApps.get(i)).activityInfo.name.equals(activityNameO)) {
                this.mSwitchPreferenceO.setTitle(this.mContext.getResources().getString(R.string.smartwake_settings_o_title) + " " + ((ResolveInfo) allIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
                break;
            }
            i++;
        }
        super.onResume();
        if (!this.mValidListener) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mValidListener = true;
        }
    }

    public void onPause() {
        if (this.mValidListener) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mValidListener = false;
        }
        super.onPause();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        boolean z = true;
        super.onActivityCreated(savedInstanceState);
        Log.d("SmartWakeSettings", "onActivityCreated() :");
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.show();
        if (SystemProperties.getInt("persist.sys.smartwake_switch", 0) != 1) {
            z = false;
        }
        this.mSwitchChecked = z;
        this.mSwitchBar.setChecked(this.mSwitchChecked);
        setCheckBoxEnabled(this.mSwitchBar.isChecked());
        initGestureProperties();
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    public boolean onPreferenceClick(Preference preference) {
        Log.d("SmartWakeSettings", "onPreferenceClick() :");
        Intent allAppsIntent = new Intent();
        allAppsIntent.setClassName("com.android.settings", "com.v.smartwake.AllAppsListActivity");
        if (preference == this.mSwitchPreferenceC) {
            allAppsIntent.putExtra("gesture", 0);
        } else if (preference == this.mSwitchPreferenceE) {
            allAppsIntent.putExtra("gesture", 1);
        } else if (preference == this.mSwitchPreferenceW) {
            allAppsIntent.putExtra("gesture", 2);
        } else if (preference == this.mSwitchPreferenceM) {
            allAppsIntent.putExtra("gesture", 3);
        } else if (preference == this.mSwitchPreferenceO) {
            allAppsIntent.putExtra("gesture", 4);
        }
        this.mContext.startActivity(allAppsIntent);
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("SmartWakeSettings", "onPreferenceChange() : key = " + preference.getKey() + " newValue = " + newValue);
        boolean isChecked = String.valueOf(newValue).contains("true");
        if (preference == this.mCheckBoxU) {
            SystemProperties.set("persist.sys.smartwake_u", String.valueOf(newValue).contains("true") ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_U_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_u", 0));
        } else if (preference == this.mCheckBoxUp) {
            SystemProperties.set("persist.sys.smartwake_up", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_UP_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_up", 0));
        } else if (preference == this.mCheckBoxDown) {
            SystemProperties.set("persist.sys.smartwake_down", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_DOWN_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_down", 0));
        } else if (preference == this.mCheckBoxRight) {
            SystemProperties.set("persist.sys.smartwake_right", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_RIGHT_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_right", 0));
        } else if (preference == this.mCheckBoxLeft) {
            SystemProperties.set("persist.sys.smartwake_left", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_LEFT_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_left", 0));
        } else if (preference == this.mSwitchPreferenceC) {
            SystemProperties.set("persist.sys.smartwake_c", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_C_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_c", 0));
        } else if (preference == this.mSwitchPreferenceE) {
            SystemProperties.set("persist.sys.smartwake_e", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_E_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_e", 0));
        } else if (preference == this.mSwitchPreferenceW) {
            SystemProperties.set("persist.sys.smartwake_w", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_W_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_w", 0));
        } else if (preference == this.mSwitchPreferenceM) {
            SystemProperties.set("persist.sys.smartwake_m", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_M_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_m", 0));
        } else if (preference == this.mSwitchPreferenceO) {
            SystemProperties.set("persist.sys.smartwake_o", isChecked ? "1" : "0");
            Log.d("SmartWakeSettings", "onPreferenceChange() : SMARTWAKE_O_FLAG = " + SystemProperties.getInt("persist.sys.smartwake_o", 0));
        }
        return true;
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            Log.d("SmartWakeSettings", "onSwitchChanged() isChecked");
            setCheckBoxEnabled(isChecked);
            SystemProperties.set("persist.sys.smartwake_switch", isChecked ? "1" : "0");
            writeFile(1);
            return;
        }
        Log.d("SmartWakeSettings", "onSwitchChanged() noChecked");
        setCheckBoxEnabled(isChecked);
        SystemProperties.set("persist.sys.smartwake_switch", isChecked ? "1" : "0");
        writeFile(0);
    }

    private String searchGestureFile(Context context) {
        String[] files = context.getResources().getStringArray(17236041);
        for (int i = 0; i < files.length; i++) {
            if (new File(files[i] + "gesture").exists()) {
                return files[i];
            }
        }
        return null;
    }

    private void writeFile(int value) {
        try {
            String fileString = searchGestureFile(this.mContext);
            if (fileString == null) {
                Toast.makeText(this.mContext, "GESTURE FILE NOT FOUND", 0).show();
                return;
            }
            File file = new File(fileString + "gesture");
            if (file.exists()) {
                FileWriter mWriter = new FileWriter(file);
                mWriter.write(String.valueOf(value));
                mWriter.flush();
                mWriter.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't open gesture device", e);
        }
    }

    private void setCheckBoxEnabled(boolean isEnabled) {
        this.mCheckBoxU.setEnabled(isEnabled);
        this.mCheckBoxUp.setEnabled(isEnabled);
        this.mCheckBoxDown.setEnabled(isEnabled);
        this.mCheckBoxRight.setEnabled(isEnabled);
        this.mCheckBoxLeft.setEnabled(isEnabled);
        this.mSwitchPreferenceC.setEnabled(isEnabled);
        this.mSwitchPreferenceE.setEnabled(isEnabled);
        this.mSwitchPreferenceW.setEnabled(isEnabled);
        this.mSwitchPreferenceM.setEnabled(isEnabled);
        this.mSwitchPreferenceO.setEnabled(isEnabled);
    }

    private void initGestureProperties() {
        boolean z;
        boolean z2 = true;
        CheckBoxPreference checkBoxPreference = this.mCheckBoxU;
        if (SystemProperties.getInt("persist.sys.smartwake_u", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxUp;
        if (SystemProperties.getInt("persist.sys.smartwake_up", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxDown;
        if (SystemProperties.getInt("persist.sys.smartwake_down", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxLeft;
        if (SystemProperties.getInt("persist.sys.smartwake_left", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxRight;
        if (SystemProperties.getInt("persist.sys.smartwake_right", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        CustomizedSwitchPreference customizedSwitchPreference = this.mSwitchPreferenceO;
        if (SystemProperties.getInt("persist.sys.smartwake_o", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        customizedSwitchPreference.setChecked(z);
        customizedSwitchPreference = this.mSwitchPreferenceW;
        if (SystemProperties.getInt("persist.sys.smartwake_w", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        customizedSwitchPreference.setChecked(z);
        customizedSwitchPreference = this.mSwitchPreferenceC;
        if (SystemProperties.getInt("persist.sys.smartwake_c", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        customizedSwitchPreference.setChecked(z);
        customizedSwitchPreference = this.mSwitchPreferenceE;
        if (SystemProperties.getInt("persist.sys.smartwake_e", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        customizedSwitchPreference.setChecked(z);
        CustomizedSwitchPreference customizedSwitchPreference2 = this.mSwitchPreferenceM;
        if (SystemProperties.getInt("persist.sys.smartwake_m", 0) != 1) {
            z2 = false;
        }
        customizedSwitchPreference2.setChecked(z2);
        if (SystemProperties.getInt("persist.sys.smartwake_u", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxU);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_up", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxUp);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_down", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxDown);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_right", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxRight);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_left", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxLeft);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_o", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mSwitchPreferenceO);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_e", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mSwitchPreferenceE);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_w", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mSwitchPreferenceW);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_c", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mSwitchPreferenceC);
        }
        if (SystemProperties.getInt("persist.sys.smartwake_m", 0) == -1) {
            this.mAppsPerCategory.removePreference(this.mSwitchPreferenceM);
        }
    }
}
