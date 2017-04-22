package com.android.settings.applications;

import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings.Secure;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.voice.VoiceInputListPreference;

public class ManageAssist extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private SwitchPreference mContextPref;
    private DefaultAssistPreference mDefaultAssitPref;
    private Handler mHandler = new Handler();
    private SwitchPreference mScreenshotPref;
    private VoiceInputListPreference mVoiceInputPref;

    class C02621 implements Runnable {
        C02621() {
        }

        public void run() {
            ManageAssist.this.updateUi();
        }
    }

    public void onCreate(Bundle icicle) {
        boolean z;
        boolean z2 = true;
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.manage_assist);
        this.mDefaultAssitPref = (DefaultAssistPreference) findPreference("default_assist");
        this.mDefaultAssitPref.setOnPreferenceChangeListener(this);
        this.mContextPref = (SwitchPreference) findPreference("context");
        SwitchPreference switchPreference = this.mContextPref;
        if (Secure.getInt(getContentResolver(), "assist_structure_enabled", 1) != 0) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setChecked(z);
        this.mContextPref.setOnPreferenceChangeListener(this);
        this.mScreenshotPref = (SwitchPreference) findPreference("screenshot");
        SwitchPreference switchPreference2 = this.mScreenshotPref;
        if (Secure.getInt(getContentResolver(), "assist_screenshot_enabled", 1) == 0) {
            z2 = false;
        }
        switchPreference2.setChecked(z2);
        this.mScreenshotPref.setOnPreferenceChangeListener(this);
        this.mVoiceInputPref = (VoiceInputListPreference) findPreference("voice_input_settings");
        updateUi();
    }

    protected int getMetricsCategory() {
        return 201;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int i = 0;
        ContentResolver contentResolver;
        String str;
        if (preference == this.mContextPref) {
            contentResolver = getContentResolver();
            str = "assist_structure_enabled";
            if (((Boolean) newValue).booleanValue()) {
                i = 1;
            }
            Secure.putInt(contentResolver, str, i);
            postUpdateUi();
            return true;
        } else if (preference == this.mScreenshotPref) {
            contentResolver = getContentResolver();
            str = "assist_screenshot_enabled";
            if (((Boolean) newValue).booleanValue()) {
                i = 1;
            }
            Secure.putInt(contentResolver, str, i);
            return true;
        } else if (preference != this.mDefaultAssitPref) {
            return false;
        } else {
            String newAssitPackage = (String) newValue;
            if (newAssitPackage == null || newAssitPackage.contentEquals("")) {
                setDefaultAssist("");
                return false;
            }
            String currentPackage = this.mDefaultAssitPref.getValue();
            if (currentPackage == null || !newAssitPackage.contentEquals(currentPackage)) {
                confirmNewAssist(newAssitPackage);
            }
            return false;
        }
    }

    private void postUpdateUi() {
        this.mHandler.post(new C02621());
    }

    private void updateUi() {
        this.mDefaultAssitPref.refreshAssistApps();
        this.mVoiceInputPref.refreshVoiceInputs();
        ComponentName currentAssist = this.mDefaultAssitPref.getCurrentAssist();
        if (currentAssist != null) {
            getPreferenceScreen().addPreference(this.mContextPref);
            getPreferenceScreen().addPreference(this.mScreenshotPref);
        } else {
            getPreferenceScreen().removePreference(this.mContextPref);
            getPreferenceScreen().removePreference(this.mScreenshotPref);
        }
        if (isCurrentAssistVoiceService()) {
            getPreferenceScreen().removePreference(this.mVoiceInputPref);
        } else {
            getPreferenceScreen().addPreference(this.mVoiceInputPref);
            this.mVoiceInputPref.setAssistRestrict(currentAssist);
        }
        this.mScreenshotPref.setEnabled(this.mContextPref.isChecked());
        if (!this.mContextPref.isChecked()) {
            this.mScreenshotPref.setChecked(false);
        }
    }

    private boolean isCurrentAssistVoiceService() {
        ComponentName currentAssist = this.mDefaultAssitPref.getCurrentAssist();
        ComponentName activeService = this.mVoiceInputPref.getCurrentService();
        if (currentAssist == null && activeService == null) {
            return true;
        }
        return currentAssist != null ? currentAssist.equals(activeService) : false;
    }

    private void confirmNewAssist(final String newAssitPackage) {
        CharSequence appLabel = this.mDefaultAssitPref.getEntries()[this.mDefaultAssitPref.findIndexOfValue(newAssitPackage)];
        String title = getString(R.string.assistant_security_warning_title, new Object[]{appLabel});
        String message = getString(R.string.assistant_security_warning, new Object[]{appLabel});
        OnClickListener onAgree = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ManageAssist.this.setDefaultAssist(newAssitPackage);
            }
        };
        Builder builder = new Builder(getContext());
        builder.setTitle(title).setMessage(message).setCancelable(true).setPositiveButton(R.string.assistant_security_warning_agree, onAgree).setNegativeButton(R.string.assistant_security_warning_disagree, null);
        builder.create().show();
    }

    private void setDefaultAssist(String assistPackage) {
        this.mDefaultAssitPref.setValue(assistPackage);
        updateUi();
    }
}
