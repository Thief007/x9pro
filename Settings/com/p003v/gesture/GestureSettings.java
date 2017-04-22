package com.p003v.gesture;

import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
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

public class GestureSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener, OnSwitchChangeListener {
    private static String NEW_SENSOR_STATE_PATH = "/sys/class/input/input3/ges_power_state";
    private static String PSENSOR_STATE_PATH = "/sys/bus/platform/drivers/als_ps/ps";
    private AnimationDrawable anim;
    private PreferenceCategory mAppsPerCategory;
    private CheckBoxPreference mCheckBoxBrowser;
    private CheckBoxPreference mCheckBoxCamera;
    private CheckBoxPreference mCheckBoxClock;
    private CheckBoxPreference mCheckBoxContacts;
    private CheckBoxPreference mCheckBoxContactsPhone;
    private CheckBoxPreference mCheckBoxFmRadio;
    private CheckBoxPreference mCheckBoxGallery;
    private CheckBoxPreference mCheckBoxKeyguard;
    private CheckBoxPreference mCheckBoxLauncher;
    private CheckBoxPreference mCheckBoxMms;
    private CheckBoxPreference mCheckBoxMusic;
    private CheckBoxPreference mCheckBoxMute;
    private CheckBoxPreference mCheckBoxPhone;
    private Context mContext;
    private PreferenceCategory mHelpPerCategory;
    private ImageView mImageView;
    private PreferenceScreen mLearnActionPreScreen;
    private SwitchBar mSwitchBar;
    private boolean mSwitchChecked;
    private boolean mValidListener = false;

    class C07791 implements OnPreferenceClickListener {
        C07791() {
        }

        public boolean onPreferenceClick(Preference arg0) {
            GestureSettings.this.showDialog();
            return false;
        }
    }

    class C07802 implements OnClickListener {
        C07802() {
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
        Log.d("GestureSettings", "onCreate() :");
        this.mContext = getActivity();
        ContentResolver resolver = getActivity().getContentResolver();
        addPreferencesFromResource(R.xml.v_gesture_settings);
        initView();
        customViewAsProperty();
        this.mLearnActionPreScreen.setOnPreferenceClickListener(new C07791());
    }

    private void initView() {
        this.mHelpPerCategory = (PreferenceCategory) findPreference("gesture_settings_help");
        this.mLearnActionPreScreen = (PreferenceScreen) findPreference("gesture_settings_learn_action");
        this.mAppsPerCategory = (PreferenceCategory) findPreference("gesture_support_apps");
        this.mCheckBoxMms = (CheckBoxPreference) findPreference("gesture_settings_mms");
        this.mCheckBoxGallery = (CheckBoxPreference) findPreference("gesture_settings_gallery");
        this.mCheckBoxCamera = (CheckBoxPreference) findPreference("gesture_settings_camera");
        this.mCheckBoxMusic = (CheckBoxPreference) findPreference("gesture_settings_music");
        this.mCheckBoxLauncher = (CheckBoxPreference) findPreference("gesture_settings_launcher");
        this.mCheckBoxPhone = (CheckBoxPreference) findPreference("gesture_settings_phone");
        this.mCheckBoxMute = (CheckBoxPreference) findPreference("gesture_settings_mute");
        this.mCheckBoxKeyguard = (CheckBoxPreference) findPreference("gesture_settings_keyguard");
        this.mCheckBoxBrowser = (CheckBoxPreference) findPreference("gesture_settings_browser");
        this.mCheckBoxContacts = (CheckBoxPreference) findPreference("gesture_settings_contacts");
        this.mCheckBoxFmRadio = (CheckBoxPreference) findPreference("gesture_settings_fmradio");
        this.mCheckBoxClock = (CheckBoxPreference) findPreference("gesture_settings_clock");
        this.mCheckBoxContactsPhone = (CheckBoxPreference) findPreference("gesture_settings_contacts_phone");
        this.mCheckBoxMms.setOnPreferenceChangeListener(this);
        this.mCheckBoxGallery.setOnPreferenceChangeListener(this);
        this.mCheckBoxCamera.setOnPreferenceChangeListener(this);
        this.mCheckBoxMusic.setOnPreferenceChangeListener(this);
        this.mCheckBoxLauncher.setOnPreferenceChangeListener(this);
        this.mCheckBoxPhone.setOnPreferenceChangeListener(this);
        this.mCheckBoxMute.setOnPreferenceChangeListener(this);
        this.mCheckBoxKeyguard.setOnPreferenceChangeListener(this);
        this.mCheckBoxBrowser.setOnPreferenceChangeListener(this);
        this.mCheckBoxContacts.setOnPreferenceChangeListener(this);
        this.mCheckBoxFmRadio.setOnPreferenceChangeListener(this);
        this.mCheckBoxClock.setOnPreferenceChangeListener(this);
        this.mCheckBoxContactsPhone.setOnPreferenceChangeListener(this);
    }

    private void customViewAsProperty() {
        if (!isGlleryCameryGestureSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxGallery);
            this.mAppsPerCategory.removePreference(this.mCheckBoxCamera);
        }
        if (isMusicGestureSupport()) {
            boolean isGoogleOrigMusicExist = false;
            try {
                this.mContext.getPackageManager().getPackageInfo("com.android.music", 1);
                isGoogleOrigMusicExist = true;
            } catch (NameNotFoundException e) {
                Log.d("GestureSettings", "google original music not found");
            }
            if (!isGoogleOrigMusicExist) {
                Log.d("GestureSettings", "google original music not founded, remove mCheckBoxMusic");
                this.mAppsPerCategory.removePreference(this.mCheckBoxMusic);
            }
        } else {
            this.mAppsPerCategory.removePreference(this.mCheckBoxMusic);
        }
        if (!isLauncherGestureSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxLauncher);
        }
        if (!isMmsGestureSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxMms);
        }
        if (!isCallAnswerGestureSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxPhone);
        }
        this.mAppsPerCategory.removePreference(this.mCheckBoxMute);
        if (!isKeyguardGestureSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxKeyguard);
        }
        if (SystemProperties.getInt("ro.init.gesture_browser", 0) != 1) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxBrowser);
        }
        if (SystemProperties.getInt("ro.init.gesture_contacts", 0) != 1) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxContacts);
        }
        if (!isFmRadioGestureSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxFmRadio);
        }
        if (!isClockFlipMuteSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxClock);
        }
        if (!isContactsPhoneGestureSupport()) {
            this.mAppsPerCategory.removePreference(this.mCheckBoxContactsPhone);
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        boolean z = true;
        super.onActivityCreated(savedInstanceState);
        Log.d("GestureSettings", "onActivityCreated() :");
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.show();
        if (SystemProperties.getInt("persist.sys.gesture_switch", 0) != 1) {
            z = false;
        }
        this.mSwitchChecked = z;
        this.mSwitchBar.setChecked(this.mSwitchChecked);
        setCheckBoxEnabled(this.mSwitchBar.isChecked());
        initGestureProperties();
    }

    public void onResume() {
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

    private void showDialog() {
        View dialog = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.v_gesture_help_dialog, null);
        Builder builder = new Builder(this.mContext);
        this.mImageView = (ImageView) dialog.findViewById(R.id.dialog_img);
        this.anim = (AnimationDrawable) this.mImageView.getBackground();
        this.anim.start();
        builder.setCancelable(false);
        builder.setTitle(R.string.gesture_settings_learn_action);
        builder.setView(dialog);
        builder.setPositiveButton(17039370, new C07802());
        builder.show();
    }

    public boolean onPreferenceClick(Preference preference) {
        Log.d("GestureSettings", "onPreferenceClick() :");
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("GestureSettings", "onPreferenceChange() : key = " + preference.getKey() + " newValue = " + newValue);
        boolean isChecked = String.valueOf(newValue).contains("true");
        if (preference == this.mCheckBoxMms) {
            SystemProperties.set("persist.sys.gesture_mms", String.valueOf(newValue).contains("true") ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_MMS_FLAG = " + SystemProperties.getInt("persist.sys.gesture_mms", 0));
        } else if (preference == this.mCheckBoxGallery) {
            SystemProperties.set("persist.sys.gesture_gallery", isChecked ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_GALLERY_FLAG = " + SystemProperties.getInt("persist.sys.gesture_gallery", 0));
        } else if (preference == this.mCheckBoxCamera) {
            SystemProperties.set("persist.sys.gesture_camera", isChecked ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_CAMERA_FLAG = " + SystemProperties.getInt("persist.sys.gesture_camera", 0));
        } else if (preference == this.mCheckBoxMusic) {
            SystemProperties.set("persist.sys.gesture_music", String.valueOf(newValue).contains("true") ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_MUSIC_FLAG = " + SystemProperties.getInt("persist.sys.gesture_music", 0));
        } else if (preference == this.mCheckBoxLauncher) {
            SystemProperties.set("persist.sys.gesture_launcher", isChecked ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_LAUNCHER_FLAG = " + SystemProperties.getInt("persist.sys.gesture_launcher", 0));
        } else if (preference == this.mCheckBoxPhone) {
            SystemProperties.set("persist.sys.gesture_phone", isChecked ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_PHONE_FLAG = " + SystemProperties.getInt("persist.sys.gesture_phone", 0));
        } else if (preference == this.mCheckBoxMute) {
            SystemProperties.set("persist.sys.gesture_mute", isChecked ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_MUTE_FLAG = " + SystemProperties.getInt("persist.sys.gesture_mute", 0));
        } else if (preference == this.mCheckBoxKeyguard) {
            SystemProperties.set("persist.sys.gesture_keyguard", String.valueOf(newValue).contains("true") ? "1" : "0");
            Log.d("GestureSettings", "onPreferenceChange() : GESTURE_KEYGUARD_FLAG = " + SystemProperties.getInt("persist.sys.gesture_keyguard", 0));
        } else if (preference == this.mCheckBoxContacts) {
            SystemProperties.set("persist.sys.gesture_contacts", isChecked ? "1" : "0");
        } else if (preference == this.mCheckBoxBrowser) {
            SystemProperties.set("persist.sys.gesture_browser", isChecked ? "1" : "0");
        } else if (preference == this.mCheckBoxFmRadio) {
            SystemProperties.set("persist.sys.gesture_fmradio", isChecked ? "1" : "0");
        } else if (preference == this.mCheckBoxClock) {
            SystemProperties.set("persist.sys.gesture_clock", isChecked ? "1" : "0");
        } else if (preference == this.mCheckBoxContactsPhone) {
            SystemProperties.set("persist.sys.gesture_pcontacts", isChecked ? "1" : "0");
        }
        return true;
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            Log.d("GestureSettings", "onSwitchChanged() isChecked");
            SystemProperties.set("persist.sys.gesture_switch", isChecked ? "1" : "0");
            setCheckBoxEnabled(isChecked);
            return;
        }
        Log.d("GestureSettings", "onSwitchChanged() noChecked");
        SystemProperties.set("persist.sys.gesture_switch", isChecked ? "1" : "0");
        setCheckBoxEnabled(isChecked);
    }

    private void setCheckBoxEnabled(boolean isEnabled) {
        this.mCheckBoxMms.setEnabled(isEnabled);
        this.mCheckBoxGallery.setEnabled(isEnabled);
        this.mCheckBoxCamera.setEnabled(isEnabled);
        this.mCheckBoxMusic.setEnabled(isEnabled);
        this.mCheckBoxLauncher.setEnabled(isEnabled);
        this.mCheckBoxPhone.setEnabled(isEnabled);
        this.mCheckBoxMute.setEnabled(isEnabled);
        this.mCheckBoxKeyguard.setEnabled(isEnabled);
        this.mCheckBoxBrowser.setEnabled(isEnabled);
        this.mCheckBoxContacts.setEnabled(isEnabled);
        this.mCheckBoxFmRadio.setEnabled(isEnabled);
        this.mCheckBoxClock.setEnabled(isEnabled);
        this.mCheckBoxContactsPhone.setEnabled(isEnabled);
    }

    private void initGestureProperties() {
        boolean z;
        boolean z2 = true;
        CheckBoxPreference checkBoxPreference = this.mCheckBoxCamera;
        if (SystemProperties.getInt("persist.sys.gesture_camera", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxGallery;
        if (SystemProperties.getInt("persist.sys.gesture_gallery", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxMusic;
        if (SystemProperties.getInt("persist.sys.gesture_music", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxLauncher;
        if (SystemProperties.getInt("persist.sys.gesture_launcher", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxMms;
        if (SystemProperties.getInt("persist.sys.gesture_mms", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxPhone;
        if (SystemProperties.getInt("persist.sys.gesture_phone", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxMute;
        if (SystemProperties.getInt("persist.sys.gesture_mute", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxKeyguard;
        if (SystemProperties.getInt("persist.sys.gesture_keyguard", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxBrowser;
        if (SystemProperties.getInt("persist.sys.gesture_browser", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxContacts;
        if (SystemProperties.getInt("persist.sys.gesture_contacts", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxFmRadio;
        if (SystemProperties.getInt("persist.sys.gesture_fmradio", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        checkBoxPreference = this.mCheckBoxClock;
        if (SystemProperties.getInt("persist.sys.gesture_clock", 0) == 1) {
            z = true;
        } else {
            z = false;
        }
        checkBoxPreference.setChecked(z);
        CheckBoxPreference checkBoxPreference2 = this.mCheckBoxContactsPhone;
        if (SystemProperties.getInt("persist.sys.gesture_pcontacts", 0) != 1) {
            z2 = false;
        }
        checkBoxPreference2.setChecked(z2);
    }

    private boolean isGlleryCameryGestureSupport() {
        return true;
    }

    private boolean isLauncherGestureSupport() {
        return true;
    }

    private boolean isMusicGestureSupport() {
        return true;
    }

    private boolean isFmRadioGestureSupport() {
        return true;
    }

    private boolean isClockFlipMuteSupport() {
        return false;
    }

    private boolean isMmsGestureSupport() {
        return true;
    }

    private boolean isKeyguardGestureSupport() {
        return false;
    }

    private boolean isContactsPhoneGestureSupport() {
        return true;
    }

    private boolean isCallAnswerGestureSupport() {
        return true;
    }
}
