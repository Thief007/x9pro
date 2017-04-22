package com.mediatek.audioprofile;

import android.content.ContentQueryMap;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import com.mediatek.audioprofile.VolumeSeekBarPreference.Callback;
import com.mediatek.settings.FeatureOption;
import java.util.Observable;
import java.util.Observer;

public class Editprofile extends SettingsPreferenceFragment {
    private TwoStatePreference mBootAudio;
    private ContentQueryMap mContentQueryMap;
    private Context mContext;
    private int mCurOrientation;
    private TwoStatePreference mDtmfTone;
    private final C0690H mHandler = new C0690H();
    private TwoStatePreference mHapticFeedback;
    private boolean mIsMeetingMode;
    private boolean mIsSilentMode;
    private String mKey;
    private TwoStatePreference mLockSounds;
    private DefaultRingtonePreference mNotify;
    private AudioProfileManager mProfileManager;
    private final ContentObserver mRingtoneObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Editprofile.this.mHandler.sendEmptyMessage(5);
        }
    };
    private String mSIMSelectorTitle;
    private int mSelectRingtongType = -1;
    private Cursor mSettingsCursor;
    private Observer mSettingsObserver;
    private long mSimId = -1;
    private DefaultRingtonePreference mSipRingtone;
    private TwoStatePreference mSoundEffects;
    private TelephonyManager mTeleManager;
    private TwoStatePreference mVibrat;
    private DefaultRingtonePreference mVideoRingtone;
    private boolean mVoiceCapable;
    private DefaultRingtonePreference mVoiceRingtone;
    public VolumeSeekBarPreference mVolume;
    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback();

    class C06882 implements Observer {
        C06882() {
        }

        public void update(Observable o, Object arg) {
            Log.d("@M_AudioProfile/EditProfile", "update");
            if (Editprofile.this.mVibrat != null) {
                String name = AudioProfileManager.getVibrationKey(Editprofile.this.mKey);
                Log.d("@M_AudioProfile/EditProfile", "name " + name);
                String vibrateEnabled = System.getString(Editprofile.this.getContentResolver(), name);
                if (vibrateEnabled != null) {
                    Editprofile.this.mVibrat.setChecked("true".equals(vibrateEnabled));
                    Log.d("@M_AudioProfile/EditProfile", "vibrate setting is " + "true".equals(vibrateEnabled));
                }
            }
        }
    }

    private final class C0690H extends Handler {
        private C0690H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    Editprofile.this.mVolumeCallback.stopSample();
                    return;
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    Log.d("@M_AudioProfile/EditProfile", "Ringtone changed.");
                    Editprofile.this.mVolumeCallback.ringtoneChanged();
                    return;
                default:
                    return;
            }
        }
    }

    private final class VolumePreferenceCallback implements Callback {
        private SeekBarVolumizer mCurrent;

        private VolumePreferenceCallback() {
        }

        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (!(this.mCurrent == null || this.mCurrent == sbv)) {
                this.mCurrent.stopSample();
            }
            this.mCurrent = sbv;
            if (this.mCurrent != null) {
                Editprofile.this.mHandler.removeMessages(3);
                Editprofile.this.mHandler.sendEmptyMessageDelayed(3, 2000);
            }
        }

        public void stopSample() {
            if (this.mCurrent != null) {
                this.mCurrent.stopSample();
            }
        }

        public void ringtoneChanged() {
            if (this.mCurrent != null) {
                this.mCurrent.ringtoneChanged();
            } else {
                Editprofile.this.mVolume.getSeekBar().ringtoneChanged();
            }
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        addPreferencesFromResource(R.xml.edit_profile_prefs);
        this.mTeleManager = (TelephonyManager) getSystemService("phone");
        this.mVoiceCapable = Utils.isVoiceCapable(this.mContext);
        SettingsActivity parentActivity = (SettingsActivity) getActivity();
        Bundle bundle = getArguments();
        Log.d("@M_AudioProfile/EditProfile", "onCreate activity = " + parentActivity + ",bundle = " + bundle + ",this = " + this);
        this.mKey = bundle.getString("profileKey");
        this.mProfileManager = (AudioProfileManager) getSystemService("audioprofile");
        Scenario scenario = AudioProfileManager.getScenario(this.mKey);
        this.mIsSilentMode = scenario.equals(Scenario.SILENT);
        this.mIsMeetingMode = scenario.equals(Scenario.MEETING);
        this.mSIMSelectorTitle = getActivity().getString(R.string.settings_label);
        initPreference();
    }

    private boolean isSmsCapable() {
        return this.mTeleManager != null ? this.mTeleManager.isSmsCapable() : false;
    }

    public void onStart() {
        super.onStart();
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("ringtone"), false, this.mRingtoneObserver);
        this.mSettingsCursor = getContentResolver().query(System.CONTENT_URI, null, "(name=?)", new String[]{AudioProfileManager.getVibrationKey(this.mKey)}, null);
        this.mContentQueryMap = new ContentQueryMap(this.mSettingsCursor, "name", true, null);
    }

    public void onPause() {
        super.onPause();
        Log.d("@M_AudioProfile/EditProfile", "onPause");
        this.mVolumeCallback.stopSample();
        if (this.mSettingsObserver != null) {
            this.mContentQueryMap.deleteObserver(this.mSettingsObserver);
        }
        if (this.mSettingsCursor != null) {
            this.mSettingsCursor.close();
            this.mSettingsCursor = null;
        }
    }

    public void onStop() {
        super.onStop();
        this.mContext.getContentResolver().unregisterContentObserver(this.mRingtoneObserver);
    }

    private void initPreference() {
        PreferenceScreen parent = getPreferenceScreen();
        initVolume(parent);
        initRingtoneAndNotification(parent);
        initSystemAudio();
        if (this.mIsSilentMode || this.mIsMeetingMode) {
            removePrefWhenSilentOrMeeting(parent);
            return;
        }
        if (this.mVoiceCapable) {
            initVoiceCapablePref(parent);
        } else {
            initNoVoiceCapablePref(parent);
        }
        if (this.mBootAudio != null) {
            parent.removePreference(this.mBootAudio);
        }
    }

    private void initVolume(PreferenceScreen parent) {
        initVolumePreference("alarm_volume", 4);
        if (this.mVoiceCapable) {
            this.mVolume = initVolumePreference("ring_volume", 2);
            parent.removePreference(parent.findPreference("notification_volume"));
            return;
        }
        this.mVolume = initVolumePreference("notification_volume", 5);
        parent.removePreference(parent.findPreference("ring_volume"));
    }

    private void initRingtoneAndNotification(PreferenceScreen parent) {
        initNotification(parent);
        initRingtone(parent);
    }

    private void initNotification(PreferenceScreen parent) {
        this.mNotify = (DefaultRingtonePreference) parent.findPreference("notifications_ringtone");
        if (this.mNotify != null) {
            this.mNotify.setStreamType("NOTIFICATION");
            this.mNotify.setProfile(this.mKey);
            this.mNotify.setRingtoneType(2);
            this.mNotify.setNoNeedSIMSelector(true);
        }
    }

    private void initRingtone(PreferenceScreen parent) {
        this.mVoiceRingtone = (DefaultRingtonePreference) parent.findPreference("phone_ringtone");
        this.mVideoRingtone = (DefaultRingtonePreference) parent.findPreference("video_call_ringtone");
        this.mSipRingtone = (DefaultRingtonePreference) parent.findPreference("sip_call_ringtone");
    }

    private void initSystemAudio() {
        this.mVibrat = (TwoStatePreference) findPreference("phone_vibrate");
        this.mDtmfTone = (TwoStatePreference) findPreference("audible_touch_tones");
        this.mSoundEffects = (TwoStatePreference) findPreference("audible_selection");
        this.mLockSounds = (TwoStatePreference) findPreference("screen_lock_sounds");
        this.mHapticFeedback = (TwoStatePreference) findPreference("haptic_feedback");
        this.mBootAudio = (TwoStatePreference) findPreference("boot_audio");
        setPreferenceListener("boot_audio", this.mBootAudio);
        setPreferenceListener("phone_vibrate", this.mVibrat);
        setPreferenceListener("audible_touch_tones", this.mDtmfTone);
        setPreferenceListener("audible_selection", this.mSoundEffects);
        setPreferenceListener("screen_lock_sounds", this.mLockSounds);
        setPreferenceListener("haptic_feedback", this.mHapticFeedback);
    }

    private void removePrefWhenSilentOrMeeting(PreferenceScreen parent) {
        parent.removePreference(this.mDtmfTone);
        parent.removePreference(this.mSoundEffects);
        parent.removePreference(this.mLockSounds);
        parent.removePreference(this.mVoiceRingtone);
        parent.removePreference(this.mVideoRingtone);
        parent.removePreference(this.mSipRingtone);
        parent.removePreference(this.mNotify);
        this.mVibrat.setEnabled(false);
    }

    private void initVoiceCapablePref(PreferenceScreen parent) {
        parent.removePreference(this.mVideoRingtone);
        this.mVoiceRingtone.setTitle(R.string.ringtone_title);
        parent.removePreference(this.mSipRingtone);
        if (this.mVoiceRingtone != null) {
            this.mVoiceRingtone.setStreamType("RING");
            this.mVoiceRingtone.setProfile(this.mKey);
            this.mVoiceRingtone.setRingtoneType(1);
            if (!FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
                this.mVoiceRingtone.setNoNeedSIMSelector(true);
            }
        }
        if (this.mVideoRingtone != null) {
            this.mVideoRingtone.setStreamType("RING");
            this.mVideoRingtone.setProfile(this.mKey);
            this.mVideoRingtone.setRingtoneType(8);
            if (!FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
                this.mVideoRingtone.setNoNeedSIMSelector(true);
            }
        }
    }

    private void initNoVoiceCapablePref(PreferenceScreen parent) {
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            this.mVibrat.setSummary(R.string.sms_vibrate_summary);
        }
        if (!isSmsCapable()) {
            parent.removePreference(this.mVibrat);
        }
        parent.removePreference(this.mDtmfTone);
        parent.removePreference(this.mVoiceRingtone);
        parent.removePreference(this.mVideoRingtone);
        parent.removePreference(this.mSipRingtone);
    }

    private void updatePreference() {
        boolean z;
        this.mVibrat.setChecked(this.mProfileManager.isVibrationEnabled(this.mKey));
        this.mDtmfTone.setChecked(this.mProfileManager.isDtmfToneEnabled(this.mKey));
        this.mSoundEffects.setChecked(this.mProfileManager.isSoundEffectEnabled(this.mKey));
        this.mLockSounds.setChecked(this.mProfileManager.isLockScreenEnabled(this.mKey));
        this.mHapticFeedback.setChecked(this.mProfileManager.isVibrateOnTouchEnabled(this.mKey));
        TwoStatePreference twoStatePreference = this.mBootAudio;
        if (SystemProperties.get("persist.sys.boot_ringtone", "0").equals("1")) {
            z = true;
        } else {
            z = false;
        }
        twoStatePreference.setChecked(z);
    }

    public void onResume() {
        super.onResume();
        updatePreference();
        if (this.mIsSilentMode && this.mSettingsObserver == null) {
            this.mSettingsObserver = new C06882();
            this.mContentQueryMap.addObserver(this.mSettingsObserver);
        }
    }

    private VolumeSeekBarPreference initVolumePreference(String key, int stream) {
        Log.d("@M_AudioProfile/EditProfile", "Init volume preference, key = " + key + ",stream = " + stream);
        VolumeSeekBarPreference volumePref = (VolumeSeekBarPreference) findPreference(key);
        volumePref.setStream(stream);
        volumePref.setCallback(this.mVolumeCallback);
        volumePref.setProfile(this.mKey);
        return volumePref;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d("@M_AudioProfile/EditProfile", "Key :" + preference.getKey());
        if (preference.getKey().equals("phone_ringtone")) {
            setRingtongTypeAndStartSIMSelector(1);
        } else if (preference.getKey().equals("video_call_ringtone")) {
            setRingtongTypeAndStartSIMSelector(2);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void setPreferenceListener(final String preferenceType, Preference p) {
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Editprofile.this.setPreferenceChangeToDatabase(((Boolean) newValue).booleanValue(), preferenceType);
                return true;
            }
        });
    }

    private void setPreferenceChangeToDatabase(boolean isChecked, String preferenceType) {
        Log.d("@M_AudioProfile/EditProfile", "Preference type :" + preferenceType);
        if (preferenceType.equals("phone_vibrate")) {
            this.mProfileManager.setVibrationEnabled(this.mKey, isChecked);
        } else if (preferenceType.equals("audible_touch_tones")) {
            this.mProfileManager.setDtmfToneEnabled(this.mKey, isChecked);
        } else if (preferenceType.equals("audible_selection")) {
            this.mProfileManager.setSoundEffectEnabled(this.mKey, isChecked);
        } else if (preferenceType.equals("screen_lock_sounds")) {
            this.mProfileManager.setLockScreenEnabled(this.mKey, isChecked);
        } else if (preferenceType.equals("haptic_feedback")) {
            this.mProfileManager.setVibrateOnTouchEnabled(this.mKey, isChecked);
        } else if (preferenceType.equals("boot_audio")) {
            SystemProperties.set("persist.sys.boot_ringtone", isChecked ? "1" : "0");
        }
    }

    private void setRingtongTypeAndStartSIMSelector(int keyIndex) {
        Log.d("@M_AudioProfile/EditProfile", "Selected ringtone type index = " + keyIndex);
        if (FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT) {
            int simNum = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoCount();
            Log.d("@M_AudioProfile/EditProfile", "simList.size() == " + simNum);
            if (simNum > 1) {
                this.mSelectRingtongType = keyIndex;
                setRingtoneType(keyIndex);
                startSIMCardSelectorActivity();
            }
        }
    }

    private void setRingtoneType(int keyIndex) {
        switch (keyIndex) {
            case 1:
                this.mVoiceRingtone.setRingtoneType(1);
                return;
            case 2:
                this.mVideoRingtone.setRingtoneType(8);
                return;
            default:
                return;
        }
    }

    private void startSIMCardSelectorActivity() {
        Intent intent = new Intent();
        intent.setAction("com.android.settings.sim.SELECT_SUB");
        startActivityForResult(intent, 0);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("@M_AudioProfile/EditProfile", "onConfigurationChanged: newConfig = " + newConfig + ",mCurOrientation = " + this.mCurOrientation + ",this = " + this);
        super.onConfigurationChanged(newConfig);
        if (!(newConfig == null || newConfig.orientation == this.mCurOrientation)) {
            this.mCurOrientation = newConfig.orientation;
        }
        getListView().clearScrapViewsIfNeeded();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("@M_AudioProfile/EditProfile", "onActivityResult requestCode " + requestCode + " " + resultCode + "resultCode");
        if (requestCode == 0) {
            if (resultCode == -1) {
                this.mSimId = data.getLongExtra("subscription", -1);
                setRingtoneSIMId(this.mSimId);
            }
            Log.v("@M_AudioProfile/EditProfile", "Select SIM id = " + this.mSimId);
        }
    }

    private void setRingtoneSIMId(long simId) {
        switch (this.mSelectRingtongType) {
            case 1:
                this.mVoiceRingtone.setSimId(simId);
                this.mVoiceRingtone.simSelectorOnClick();
                return;
            case 2:
                this.mVideoRingtone.setSimId(simId);
                this.mVideoRingtone.simSelectorOnClick();
                return;
            default:
                return;
        }
    }

    protected int getMetricsCategory() {
        return 100004;
    }
}
