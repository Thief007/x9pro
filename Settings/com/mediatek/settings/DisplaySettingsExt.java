package com.mediatek.settings;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.mediatek.hdmi.IMtkHdmiManager;
import com.mediatek.hdmi.IMtkHdmiManager.Stub;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.IStatusBarPlmnDisplayExt;

public class DisplaySettingsExt implements OnPreferenceClickListener {
    private Preference mClearMotion;
    private Context mContext;
    private ISettingsMiscExt mExt;
    private IMtkHdmiManager mHdmiManager;
    private Preference mHdmiSettings;
    private Intent mMiraIntent = new Intent("com.android.settings.MIRA_VISION");
    private Preference mMiraVision;
    private IStatusBarPlmnDisplayExt mPlmnName;
    private DropDownPreference mRotatePreference;
    private RotationPolicyListener mRotationPolicyListener = new C07211();
    private Preference mScreenTimeoutPreference;

    class C07211 extends RotationPolicyListener {
        C07211() {
        }

        public void onChange() {
            if (DisplaySettingsExt.this.mRotatePreference != null) {
                DisplaySettingsExt.this.mRotatePreference.setSelectedItem(RotationPolicy.isRotationLocked(DisplaySettingsExt.this.mContext) ? 1 : 0);
            }
        }
    }

    public DisplaySettingsExt(Context context) {
        Log.d("mediatek.DisplaySettings", "DisplaySettingsExt");
        this.mContext = context;
    }

    private Preference createPreference(int type, int titleRes, String key) {
        Preference preference = null;
        switch (type) {
            case 0:
                preference = new Preference(this.mContext);
                break;
            case 1:
                preference = new CheckBoxPreference(this.mContext);
                preference.setOnPreferenceClickListener(this);
                break;
            case 2:
                preference = new ListPreference(this.mContext);
                preference.setOnPreferenceClickListener(this);
                break;
        }
        preference.setKey(key);
        preference.setTitle(titleRes);
        return preference;
    }

    private void initPreference(PreferenceScreen screen) {
        this.mClearMotion = createPreference(0, R.string.clear_motion_title, "clearMotion");
        this.mClearMotion.setOrder(-100);
        this.mClearMotion.setSummary(R.string.clear_motion_summary);
        if (FeatureOption.MTK_CLEARMOTION_SUPPORT) {
            screen.addPreference(this.mClearMotion);
        }
        this.mMiraVision = createPreference(0, R.string.mira_vision_title, "mira_vision");
        this.mMiraVision.setSummary(R.string.mira_vision_summary);
        this.mMiraVision.setOrder(-99);
        if (FeatureOption.MTK_MIRAVISION_SETTING_SUPPORT && (UserHandle.myUserId() == 0 || !FeatureOption.MTK_PRODUCT_IS_TABLET)) {
            Log.d("mediatek.DisplaySettings", "No MiraVision support");
            screen.addPreference(this.mMiraVision);
        }
        this.mHdmiManager = Stub.asInterface(ServiceManager.getService("mtkhdmi"));
        if (this.mHdmiManager != null) {
            this.mHdmiSettings = createPreference(0, R.string.hdmi_settings, "hdmi_settings");
            this.mHdmiSettings.setSummary(R.string.hdmi_settings_summary);
            this.mHdmiSettings.setFragment("com.mediatek.hdmi.HdmiSettings");
            try {
                if (this.mHdmiManager.getDisplayType() == 2) {
                    String hdmi = this.mContext.getString(R.string.hdmi_replace_hdmi);
                    String mhl = this.mContext.getString(R.string.hdmi_replace_mhl);
                    this.mHdmiSettings.setTitle(this.mHdmiSettings.getTitle().toString().replaceAll(hdmi, mhl));
                    this.mHdmiSettings.setSummary(this.mHdmiSettings.getSummary().toString().replaceAll(hdmi, mhl));
                }
            } catch (RemoteException e) {
                Log.d("mediatek.DisplaySettings", "getDisplayType RemoteException");
            }
            this.mHdmiSettings.setOrder(-98);
            screen.addPreference(this.mHdmiSettings);
        }
        this.mScreenTimeoutPreference = screen.findPreference("screen_timeout");
        this.mExt = UtilsExt.getMiscPlugin(this.mContext);
        this.mExt.setTimeoutPrefTitle(this.mScreenTimeoutPreference);
        this.mPlmnName = UtilsExt.getStatusBarPlmnPlugin(this.mContext);
        this.mPlmnName.createCheckBox(screen, 1000);
        if (screen.findPreference("screensaver") != null && FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
            screen.removePreference(screen.findPreference("screensaver"));
        }
        UtilsExt.getDisplaySettingsPlugin(this.mContext).addPreference(this.mContext, screen);
    }

    public void onCreate(PreferenceScreen screen) {
        if (!FeatureOption.MTK_A1_FEATURE) {
            Log.d("mediatek.DisplaySettings", "onCreate");
            initPreference(screen);
        }
    }

    public void onResume() {
        if (!FeatureOption.MTK_A1_FEATURE) {
            Log.d("mediatek.DisplaySettings", "onResume of DisplaySettings");
            if (RotationPolicy.isRotationSupported(this.mContext)) {
                RotationPolicy.registerRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
            }
        }
    }

    public void onPause() {
        if (!FeatureOption.MTK_A1_FEATURE) {
            Log.d("mediatek.DisplaySettings", "onPause of DisplaySettings");
            if (RotationPolicy.isRotationSupported(this.mContext)) {
                RotationPolicy.unregisterRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
            }
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (FeatureOption.MTK_A1_FEATURE) {
            return false;
        }
        if (preference == this.mClearMotion) {
            Intent intent = new Intent();
            intent.setClass(this.mContext, ClearMotionSettings.class);
            this.mContext.startActivity(intent);
        } else if (preference == this.mMiraVision) {
            this.mContext.startActivity(this.mMiraIntent);
        }
        return true;
    }

    public void setRotatePreference(DropDownPreference preference) {
        this.mRotatePreference = preference;
    }
}
