package com.p003v.otouchpad;

import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class OTouchpadSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener, OnSwitchChangeListener {
    private AnimationDrawable anim;
    private CheckBoxPreference mCheckBoxAppNameSwitch;
    private CheckBoxPreference mCheckBoxCamera;
    private CheckBoxPreference mCheckBoxMusic;
    private CheckBoxPreference mCheckBoxUnlock;
    private CheckBoxPreference mCheckBoxUpDownLeftRight;
    private Context mContext;
    private ImageView mImageView;
    private PreferenceScreen mOtouchpadActionAppPreScreen;
    private boolean mResetDefaultApp = true;
    private SwitchBar mSwitchBar;
    private boolean mSwitchChecked;
    private boolean mValidListener = false;

    class C07821 implements OnClickListener {
        C07821() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
    }

    protected int getMetricsCategory() {
        return 81;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("GestureSettings", "OTouchpad onCreate() :");
        this.mContext = getActivity();
        ContentResolver resolver = getActivity().getContentResolver();
        addPreferencesFromResource(R.xml.v_otouchpad_settings);
        this.mCheckBoxAppNameSwitch = (CheckBoxPreference) findPreference("otouchpad_settings_custom_app_switch");
        this.mOtouchpadActionAppPreScreen = (PreferenceScreen) findPreference("otouchpad_settings_custom_app_name");
        this.mOtouchpadActionAppPreScreen.setOnPreferenceClickListener(this);
        this.mCheckBoxUpDownLeftRight = (CheckBoxPreference) findPreference("otouchpad_settings_up_down_left_right");
        this.mCheckBoxCamera = (CheckBoxPreference) findPreference("otouchpad_settings_camera_mood_album");
        this.mCheckBoxMusic = (CheckBoxPreference) findPreference("otouchpad_settings_mood_music");
        this.mCheckBoxAppNameSwitch.setOnPreferenceChangeListener(this);
        this.mCheckBoxUpDownLeftRight.setOnPreferenceChangeListener(this);
        this.mCheckBoxCamera.setOnPreferenceChangeListener(this);
        this.mCheckBoxUnlock = (CheckBoxPreference) findPreference("otouchpad_settings_unlock");
        this.mCheckBoxUnlock.setOnPreferenceChangeListener(this);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        boolean z = true;
        super.onActivityCreated(savedInstanceState);
        Log.i("GestureSettings", "OTouchpad onActivityCreated() :");
        SettingsActivity activity = (SettingsActivity) getActivity();
        Intent intent = activity.getIntent();
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.show();
        if (SystemProperties.getInt("persist.sys.otouchpad_switch", 0) != 1) {
            z = false;
        }
        this.mSwitchChecked = z;
        this.mSwitchBar.setChecked(this.mSwitchChecked);
        setCheckBoxEnabled(this.mSwitchBar.isChecked());
        setCheckBoxChecked();
    }

    public void onResume() {
        Log.i("GestureSettings", "OTouchpad onResume");
        Intent mainIntent = new Intent("android.intent.action.MAIN", null);
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> allIntentApps = this.mContext.getPackageManager().queryIntentActivities(mainIntent, 0);
        String packageName = SystemProperties.get("persist.sys.otp_pkg", SystemProperties.get("persist.sys.otp_def_pkg_act", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[0]);
        String activityName = SystemProperties.get("persist.sys.otp_act", SystemProperties.get("persist.sys.otp_def_pkg_act", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[1]);
        int i = 0;
        while (i < allIntentApps.size()) {
            if (((ResolveInfo) allIntentApps.get(i)).activityInfo.packageName.equals(packageName) && ((ResolveInfo) allIntentApps.get(i)).activityInfo.name.equals(activityName)) {
                this.mOtouchpadActionAppPreScreen.setTitle(this.mContext.getResources().getString(R.string.otouchpad_settings_custom_app_action) + " " + ((ResolveInfo) allIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
                this.mResetDefaultApp = false;
                break;
            }
            i++;
        }
        if (this.mResetDefaultApp) {
            String resetPackageName = SystemProperties.get("persist.sys.otp_def_pkg_act", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[0];
            String resetActivityName = SystemProperties.get("persist.sys.otp_def_pkg_act", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[1];
            i = 0;
            while (i < allIntentApps.size()) {
                if (((ResolveInfo) allIntentApps.get(i)).activityInfo.packageName.equals(resetPackageName) && ((ResolveInfo) allIntentApps.get(i)).activityInfo.name.equals(resetActivityName)) {
                    SystemProperties.set("persist.sys.otp_pkg", resetPackageName);
                    SystemProperties.set("persist.sys.otp_act", resetActivityName);
                    this.mOtouchpadActionAppPreScreen.setTitle(this.mContext.getResources().getString(R.string.otouchpad_settings_custom_app_action) + " " + ((ResolveInfo) allIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
                    Log.i("GestureSettings", "OTouchpad settings reset def app");
                    break;
                }
                i++;
            }
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

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.hide();
    }

    private void showOTouchpadCameraHelpDlg() {
        View dialog = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.v_otouchpad_help_camera_dialog, null);
        Builder builder = new Builder(this.mContext);
        this.mImageView = (ImageView) dialog.findViewById(R.id.dialog_img);
        this.anim = (AnimationDrawable) this.mImageView.getBackground();
        this.anim.start();
        builder.setCancelable(false);
        builder.setTitle(R.string.otouchpad_settings_camera_mood_album_title);
        builder.setView(dialog);
        builder.setPositiveButton(17039370, new C07821());
        builder.show();
    }

    public boolean onPreferenceClick(Preference preference) {
        Log.i("GestureSettings", "OTouchpad onPreferenceClick() :");
        if (preference == this.mOtouchpadActionAppPreScreen) {
            Intent allAppsIntent = new Intent();
            allAppsIntent.setClassName("com.android.settings", "com.v.otouchpad.AllAppsListActivity");
            this.mContext.startActivity(allAppsIntent);
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.i("GestureSettings", "OTouchpad onPreferenceChange() : key = " + preference.getKey() + " newValue = " + newValue);
        boolean isChecked = String.valueOf(newValue).contains("true");
        if (preference == this.mCheckBoxUpDownLeftRight) {
            SystemProperties.set("persist.sys.otouchpad_u_d_l_r", isChecked ? "1" : "0");
            doCloseAction();
            Log.i("GestureSettings", "OTouchpad onPreferenceChange() : OTOUCHPAD_UP_DOWN_LEFT_RIGHT_FLAG = " + SystemProperties.getInt("persist.sys.otouchpad_u_d_l_r", 0));
        } else if (preference == this.mCheckBoxMusic) {
            SystemProperties.set("persist.sys.otouchpad_music", isChecked ? "1" : "0");
            doCloseAction();
            Log.i("GestureSettings", "OTouchpad onPreferenceChange() : OTOUCHPAD_MUSIC_FLAG = " + SystemProperties.getInt("persist.sys.otouchpad_music", 0));
        } else if (preference == this.mCheckBoxCamera) {
            SystemProperties.set("persist.sys.otouchpad_camera", isChecked ? "1" : "0");
            if (isChecked) {
                showOTouchpadCameraHelpDlg();
            }
            doCloseAction();
            Log.i("GestureSettings", "OTouchpad onPreferenceChange() : OTOUCHPAD_CAMERA_FLAG = " + SystemProperties.getInt("persist.sys.otouchpad_camera", 0));
        } else if (preference == this.mCheckBoxAppNameSwitch) {
            SystemProperties.set("persist.sys.otouchpad_ap_switch", isChecked ? "1" : "0");
            doCloseAction();
        } else if (preference == this.mCheckBoxUnlock) {
            SystemProperties.set("persist.sys.otouchpad_unlock", isChecked ? "1" : "0");
            doCloseAction();
            Log.i("GestureSettings", "OTouchpad onPreferenceChange() : OTOUCHPAD_UNLOCK_FLAG = " + SystemProperties.getInt("persist.sys.otouchpad_unlock", 0));
        }
        return true;
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.i("GestureSettings", "OTouchpad onSwitchChanged() :");
        File otouchPadFile;
        if (isChecked) {
            Log.i("GestureSettings", "OTouchpad onSwitchChanged() isChecked");
            SystemProperties.set("persist.sys.otouchpad_switch", isChecked ? "1" : "0");
            otouchPadFile = new File("/sys/devices/platform/mt-i2c.2/i2c-2/2-002c/enable_backtouch");
            if (otouchPadFile.exists()) {
                OTouchpadSettings.otouchPadWriter(otouchPadFile, "1");
            } else {
                Log.i("GestureSettings", "OTouchpad otouchPadFile is not exists!");
            }
            OTouchpadSettings.doOpenAction();
            setCheckBoxEnabled(isChecked);
            return;
        }
        Log.i("GestureSettings", "OTouchpad onSwitchChanged() noChecked");
        SystemProperties.set("persist.sys.otouchpad_switch", isChecked ? "1" : "0");
        otouchPadFile = new File("/sys/devices/platform/mt-i2c.2/i2c-2/2-002c/enable_backtouch");
        if (otouchPadFile.exists()) {
            OTouchpadSettings.otouchPadWriter(otouchPadFile, "0");
        } else {
            Log.i("GestureSettings", "OTouchpad otouchPadFile is not exists!");
        }
        setCheckBoxEnabled(isChecked);
    }

    private void setCheckBoxEnabled(boolean isEnabled) {
        this.mCheckBoxAppNameSwitch.setEnabled(isEnabled);
        this.mCheckBoxCamera.setEnabled(isEnabled);
        this.mCheckBoxUpDownLeftRight.setEnabled(isEnabled);
        this.mCheckBoxUnlock.setEnabled(isEnabled);
    }

    private void setCheckBoxChecked() {
        boolean z;
        boolean z2 = true;
        CheckBoxPreference checkBoxPreference = this.mCheckBoxAppNameSwitch;
        if (SystemProperties.getInt("persist.sys.otouchpad_ap_switch", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxCamera;
        if (SystemProperties.getInt("persist.sys.otouchpad_camera", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxUpDownLeftRight;
        if (SystemProperties.getInt("persist.sys.otouchpad_u_d_l_r", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        CheckBoxPreference checkBoxPreference2 = this.mCheckBoxUnlock;
        if (SystemProperties.getInt("persist.sys.otouchpad_unlock", 0) != 1) {
            z2 = false;
        }
        checkBoxPreference2.setChecked(z2);
    }

    private void doCloseAction() {
        File otouchPadFile;
        if (SystemProperties.getInt("persist.sys.otouchpad_switch", 0) == 1 && SystemProperties.getInt("persist.sys.otouchpad_ap_switch", 0) == 0 && SystemProperties.getInt("persist.sys.otouchpad_u_d_l_r", 0) == 0 && SystemProperties.getInt("persist.sys.otouchpad_camera", 0) == 0 && SystemProperties.getInt("persist.sys.otouchpad_unlock", 0) == 0 && SystemProperties.getInt("persist.sys.otouchpad_music", 0) == 0) {
            otouchPadFile = new File("/sys/devices/platform/mt-i2c.2/i2c-2/2-002c/enable_backtouch");
            if (otouchPadFile.exists()) {
                OTouchpadSettings.otouchPadWriter(otouchPadFile, "0");
                return;
            } else {
                Log.i("GestureSettings", "OTouchpad otouchPadFile is not exists!");
                return;
            }
        }
        otouchPadFile = new File("/sys/devices/platform/mt-i2c.2/i2c-2/2-002c/enable_backtouch");
        if (otouchPadFile.exists()) {
            OTouchpadSettings.otouchPadWriter(otouchPadFile, "1");
        } else {
            Log.i("GestureSettings", "OTouchpad otouchPadFile is not exists!");
        }
    }

    public static void doOpenAction() {
        File otouchPadFile;
        if (SystemProperties.getInt("persist.sys.otouchpad_switch", 0) == 1 && (SystemProperties.getInt("persist.sys.otouchpad_ap_switch", 0) == 1 || SystemProperties.getInt("persist.sys.otouchpad_u_d_l_r", 0) == 1 || SystemProperties.getInt("persist.sys.otouchpad_camera", 0) == 1 || SystemProperties.getInt("persist.sys.otouchpad_unlock", 0) == 1 || SystemProperties.getInt("persist.sys.otouchpad_music", 0) == 1)) {
            otouchPadFile = new File("/sys/devices/platform/mt-i2c.2/i2c-2/2-002c/enable_backtouch");
            if (otouchPadFile.exists()) {
                OTouchpadSettings.otouchPadWriter(otouchPadFile, "1");
                return;
            } else {
                Log.i("GestureSettings", "OTouchpad otouchPadFile is not exists!");
                return;
            }
        }
        otouchPadFile = new File("/sys/devices/platform/mt-i2c.2/i2c-2/2-002c/enable_backtouch");
        if (otouchPadFile.exists()) {
            OTouchpadSettings.otouchPadWriter(otouchPadFile, "0");
        } else {
            Log.i("GestureSettings", "OTouchpad otouchPadFile is not exists!");
        }
    }

    public static boolean otouchPadWriter(File file, String writeValue) {
        Exception e;
        Throwable th;
        boolean writeFlag = true;
        BufferedWriter bufferedWriter = null;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            try {
                bw.write(writeValue);
                bw.flush();
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    return writeFlag;
                }
                return writeFlag;
            } catch (Exception e3) {
                e = e3;
                bufferedWriter = bw;
                writeFlag = false;
                try {
                    e.printStackTrace();
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e22) {
                            e22.printStackTrace();
                        }
                    }
                    return writeFlag;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e222) {
                            e222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                bufferedWriter = bw;
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                throw th;
            }
        } catch (Exception e4) {
            e = e4;
            writeFlag = false;
            e.printStackTrace();
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            return writeFlag;
        }
    }
}
