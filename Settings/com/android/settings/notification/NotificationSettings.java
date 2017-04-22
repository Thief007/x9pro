package com.android.settings.notification;

import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.SeekBarVolumizer;
import android.preference.TwoStatePreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.setupwizardlib.R$styleable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class NotificationSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String[] RESTRICTED_KEYS = new String[]{"media_volume", "alarm_volume", "ring_volume", "notification_volume", "manage_zen_access", "zen_mode"};
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C04362();
    private AudioManager mAudioManager;
    private Context mContext;
    private final C0440H mHandler = new C0440H();
    private DropDownPreference mLockscreen;
    private int mLockscreenSelectedValue;
    private final Runnable mLookupRingtoneNames = new C04351();
    private Preference mNotificationAccess;
    private TwoStatePreference mNotificationPulse;
    private Preference mNotificationRingtonePreference;
    private PackageManager mPM;
    private Preference mPhoneRingtonePreference;
    private final Receiver mReceiver = new Receiver();
    private VolumeSeekBarPreference mRingOrNotificationPreference;
    private int mRingerMode = -1;
    private boolean mSecure;
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    private ComponentName mSuppressor;
    private UserManager mUserManager;
    private TwoStatePreference mVibrateWhenRinging;
    private Vibrator mVibrator;
    private boolean mVoiceCapable;
    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback();
    private final ArrayList<VolumeSeekBarPreference> mVolumePrefs = new ArrayList();
    private Preference mZenAccess;

    class C04351 implements Runnable {
        C04351() {
        }

        public void run() {
            CharSequence summary;
            if (NotificationSettings.this.mPhoneRingtonePreference != null) {
                summary = NotificationSettings.updateRingtoneName(NotificationSettings.this.mContext, 1);
                if (summary != null) {
                    NotificationSettings.this.mHandler.obtainMessage(1, summary).sendToTarget();
                }
            }
            if (NotificationSettings.this.mNotificationRingtonePreference != null) {
                summary = NotificationSettings.updateRingtoneName(NotificationSettings.this.mContext, 2);
                if (summary != null) {
                    NotificationSettings.this.mHandler.obtainMessage(2, summary).sendToTarget();
                }
            }
        }
    }

    static class C04362 extends BaseSearchIndexProvider {
        C04362() {
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            new SearchIndexableResource(context).xmlResId = R.xml.notification_settings;
            return Arrays.asList(new SearchIndexableResource[]{sir});
        }

        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> rt = new ArrayList();
            if (Utils.isVoiceCapable(context)) {
                rt.add("notification_volume");
            } else {
                rt.add("ring_volume");
                rt.add("ringtone");
                rt.add("wifi_display");
                rt.add("vibrate_when_ringing");
            }
            return rt;
        }
    }

    class C04373 implements OnPreferenceChangeListener {
        C04373() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return System.putInt(NotificationSettings.this.getContentResolver(), "vibrate_when_ringing", ((Boolean) newValue).booleanValue() ? 1 : 0);
        }
    }

    class C04384 implements OnPreferenceChangeListener {
        C04384() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return System.putInt(NotificationSettings.this.getContentResolver(), "notification_light_pulse", ((Boolean) newValue).booleanValue() ? 1 : 0);
        }
    }

    class C04395 implements Callback {
        C04395() {
        }

        public boolean onItemSelected(int pos, Object value) {
            int i = 0;
            int val = ((Integer) value).intValue();
            if (val == NotificationSettings.this.mLockscreenSelectedValue) {
                return true;
            }
            int i2;
            boolean enabled = val != R.string.lock_screen_notifications_summary_disable;
            boolean show = val == R.string.lock_screen_notifications_summary_show;
            ContentResolver -wrap0 = NotificationSettings.this.getContentResolver();
            String str = "lock_screen_allow_private_notifications";
            if (show) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            Secure.putInt(-wrap0, str, i2);
            ContentResolver -wrap02 = NotificationSettings.this.getContentResolver();
            String str2 = "lock_screen_show_notifications";
            if (enabled) {
                i = 1;
            }
            Secure.putInt(-wrap02, str2, i);
            NotificationSettings.this.mLockscreenSelectedValue = val;
            return true;
        }
    }

    private final class C0440H extends Handler {
        private C0440H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    NotificationSettings.this.mPhoneRingtonePreference.setSummary((CharSequence) msg.obj);
                    return;
                case 2:
                    NotificationSettings.this.mNotificationRingtonePreference.setSummary((CharSequence) msg.obj);
                    return;
                case 3:
                    NotificationSettings.this.mVolumeCallback.stopSample();
                    return;
                case 4:
                    NotificationSettings.this.updateEffectsSuppressor();
                    return;
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    NotificationSettings.this.updateRingerMode();
                    return;
                default:
                    return;
            }
        }
    }

    private class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        private Receiver() {
        }

        public void register(boolean register) {
            if (this.mRegistered != register) {
                if (register) {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                    filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
                    NotificationSettings.this.mContext.registerReceiver(this, filter);
                } else {
                    NotificationSettings.this.mContext.unregisterReceiver(this);
                }
                this.mRegistered = register;
            }
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED".equals(action)) {
                NotificationSettings.this.mHandler.sendEmptyMessage(4);
            } else if ("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION".equals(action)) {
                NotificationSettings.this.mHandler.sendEmptyMessage(5);
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri LOCK_SCREEN_PRIVATE_URI = Secure.getUriFor("lock_screen_allow_private_notifications");
        private final Uri LOCK_SCREEN_SHOW_URI = Secure.getUriFor("lock_screen_show_notifications");
        private final Uri NOTIFICATION_LIGHT_PULSE_URI = System.getUriFor("notification_light_pulse");
        private final Uri VIBRATE_WHEN_RINGING_URI = System.getUriFor("vibrate_when_ringing");

        public SettingsObserver() {
            super(NotificationSettings.this.mHandler);
        }

        public void register(boolean register) {
            ContentResolver cr = NotificationSettings.this.getContentResolver();
            if (register) {
                cr.registerContentObserver(this.VIBRATE_WHEN_RINGING_URI, false, this);
                cr.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this);
                cr.registerContentObserver(this.LOCK_SCREEN_PRIVATE_URI, false, this);
                cr.registerContentObserver(this.LOCK_SCREEN_SHOW_URI, false, this);
                return;
            }
            cr.unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (this.VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                NotificationSettings.this.updateVibrateWhenRinging();
            }
            if (this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                NotificationSettings.this.updatePulse();
            }
            if (this.LOCK_SCREEN_PRIVATE_URI.equals(uri) || this.LOCK_SCREEN_SHOW_URI.equals(uri)) {
                NotificationSettings.this.updateLockscreenNotifications();
            }
        }
    }

    private final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        private VolumePreferenceCallback() {
        }

        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (!(this.mCurrent == null || this.mCurrent == sbv)) {
                this.mCurrent.stopSample();
            }
            this.mCurrent = sbv;
            if (this.mCurrent != null) {
                NotificationSettings.this.mHandler.removeMessages(3);
                NotificationSettings.this.mHandler.sendEmptyMessageDelayed(3, 2000);
            }
        }

        public void onStreamValueChanged(int stream, int progress) {
        }

        public void stopSample() {
            if (this.mCurrent != null) {
                this.mCurrent.stopSample();
            }
        }
    }

    protected int getMetricsCategory() {
        return 71;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        this.mPM = this.mContext.getPackageManager();
        this.mUserManager = UserManager.get(getContext());
        this.mVoiceCapable = Utils.isVoiceCapable(this.mContext);
        this.mSecure = new LockPatternUtils(getActivity()).isSecure(UserHandle.myUserId());
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mVibrator = (Vibrator) getActivity().getSystemService("vibrator");
        if (!(this.mVibrator == null || this.mVibrator.hasVibrator())) {
            this.mVibrator = null;
        }
        addPreferencesFromResource(R.xml.notification_settings);
        PreferenceCategory sound = (PreferenceCategory) findPreference("sound");
        initVolumePreference("media_volume", 3, 17302264);
        initVolumePreference("alarm_volume", 4, 17302262);
        if (this.mVoiceCapable) {
            this.mRingOrNotificationPreference = initVolumePreference("ring_volume", 2, 17302270);
            sound.removePreference(sound.findPreference("notification_volume"));
        } else {
            this.mRingOrNotificationPreference = initVolumePreference("notification_volume", 5, 17302270);
            sound.removePreference(sound.findPreference("ring_volume"));
        }
        initRingtones(sound);
        initVibrateWhenRinging(sound);
        PreferenceCategory notification = (PreferenceCategory) findPreference("notification");
        initPulse(notification);
        initLockscreenNotifications(notification);
        this.mNotificationAccess = findPreference("manage_notification_access");
        refreshNotificationListeners();
        this.mZenAccess = findPreference("manage_zen_access");
        refreshZenAccess();
        updateRingerMode();
        updateEffectsSuppressor();
    }

    public void onResume() {
        super.onResume();
        refreshNotificationListeners();
        refreshZenAccess();
        lookupRingtoneNames();
        this.mSettingsObserver.register(true);
        this.mReceiver.register(true);
        updateRingOrNotificationPreference();
        updateEffectsSuppressor();
        for (VolumeSeekBarPreference volumePref : this.mVolumePrefs) {
            volumePref.onActivityResume();
        }
        boolean isRestricted = this.mUserManager.hasUserRestriction("no_adjust_volume");
        for (String key : RESTRICTED_KEYS) {
            Preference pref = findPreference(key);
            if (pref != null) {
                boolean z;
                if (isRestricted) {
                    z = false;
                } else {
                    z = true;
                }
                pref.setEnabled(z);
            }
        }
    }

    public void onPause() {
        super.onPause();
        this.mVolumeCallback.stopSample();
        this.mSettingsObserver.register(false);
        this.mReceiver.register(false);
    }

    private VolumeSeekBarPreference initVolumePreference(String key, int stream, int muteIcon) {
        VolumeSeekBarPreference volumePref = (VolumeSeekBarPreference) findPreference(key);
        volumePref.setCallback(this.mVolumeCallback);
        volumePref.setStream(stream);
        this.mVolumePrefs.add(volumePref);
        volumePref.setMuteIcon(muteIcon);
        return volumePref;
    }

    private void updateRingOrNotificationPreference() {
        int i;
        VolumeSeekBarPreference volumeSeekBarPreference = this.mRingOrNotificationPreference;
        if (this.mSuppressor != null) {
            i = 17302270;
        } else if (this.mRingerMode == 1 || wasRingerModeVibrate()) {
            i = 17302271;
        } else {
            i = 17302269;
        }
        volumeSeekBarPreference.showIcon(i);
    }

    private boolean wasRingerModeVibrate() {
        if (this.mVibrator != null && this.mRingerMode == 0 && this.mAudioManager.getLastAudibleStreamVolume(2) == 0) {
            return true;
        }
        return false;
    }

    private void updateRingerMode() {
        int ringerMode = this.mAudioManager.getRingerModeInternal();
        if (this.mRingerMode != ringerMode) {
            this.mRingerMode = ringerMode;
            updateRingOrNotificationPreference();
        }
    }

    private void updateEffectsSuppressor() {
        ComponentName suppressor = NotificationManager.from(this.mContext).getEffectsSuppressor();
        if (!Objects.equals(suppressor, this.mSuppressor)) {
            this.mSuppressor = suppressor;
            if (this.mRingOrNotificationPreference != null) {
                String string;
                if (suppressor != null) {
                    string = this.mContext.getString(17040764, new Object[]{getSuppressorCaption(suppressor)});
                } else {
                    string = null;
                }
                this.mRingOrNotificationPreference.setSuppressionText(string);
            }
            updateRingOrNotificationPreference();
        }
    }

    private String getSuppressorCaption(ComponentName suppressor) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ServiceInfo info = pm.getServiceInfo(suppressor, 0);
            if (info != null) {
                CharSequence seq = info.loadLabel(pm);
                if (seq != null) {
                    String str = seq.toString().trim();
                    if (str.length() > 0) {
                        return str;
                    }
                }
            }
        } catch (Throwable e) {
            Log.w("NotificationSettings", "Error loading suppressor caption", e);
        }
        return suppressor.getPackageName();
    }

    private void initRingtones(PreferenceCategory root) {
        this.mPhoneRingtonePreference = root.findPreference("ringtone");
        if (!(this.mPhoneRingtonePreference == null || this.mVoiceCapable)) {
            root.removePreference(this.mPhoneRingtonePreference);
            this.mPhoneRingtonePreference = null;
        }
        this.mNotificationRingtonePreference = root.findPreference("notification_ringtone");
    }

    private void lookupRingtoneNames() {
        AsyncTask.execute(this.mLookupRingtoneNames);
    }

    private static CharSequence updateRingtoneName(Context context, int type) {
        if (context == null) {
            Log.e("NotificationSettings", "Unable to update ringtone name, no context provided");
            return null;
        }
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        CharSequence summary = context.getString(17040274);
        if (ringtoneUri == null) {
            summary = context.getString(17040272);
        } else {
            Cursor cursor = null;
            try {
                if ("media".equals(ringtoneUri.getAuthority())) {
                    cursor = context.getContentResolver().query(ringtoneUri, new String[]{"title"}, null, null, null);
                } else if ("content".equals(ringtoneUri.getScheme())) {
                    cursor = context.getContentResolver().query(ringtoneUri, new String[]{"_display_name"}, null, null, null);
                }
                if (cursor != null && cursor.moveToFirst()) {
                    summary = cursor.getString(0);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (SQLiteException e) {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (IllegalArgumentException e2) {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return summary;
    }

    private void initVibrateWhenRinging(PreferenceCategory root) {
        this.mVibrateWhenRinging = (TwoStatePreference) root.findPreference("vibrate_when_ringing");
        if (this.mVibrateWhenRinging == null) {
            Log.i("NotificationSettings", "Preference not found: vibrate_when_ringing");
        } else if (this.mVoiceCapable) {
            this.mVibrateWhenRinging.setPersistent(false);
            updateVibrateWhenRinging();
            this.mVibrateWhenRinging.setOnPreferenceChangeListener(new C04373());
        } else {
            root.removePreference(this.mVibrateWhenRinging);
            this.mVibrateWhenRinging = null;
        }
    }

    private void updateVibrateWhenRinging() {
        boolean z = false;
        if (this.mVibrateWhenRinging != null) {
            TwoStatePreference twoStatePreference = this.mVibrateWhenRinging;
            if (System.getInt(getContentResolver(), "vibrate_when_ringing", 0) != 0) {
                z = true;
            }
            twoStatePreference.setChecked(z);
        }
    }

    private void initPulse(PreferenceCategory parent) {
        this.mNotificationPulse = (TwoStatePreference) parent.findPreference("notification_pulse");
        if (this.mNotificationPulse == null) {
            Log.i("NotificationSettings", "Preference not found: notification_pulse");
            return;
        }
        if (getResources().getBoolean(17956924)) {
            updatePulse();
            this.mNotificationPulse.setOnPreferenceChangeListener(new C04384());
        } else {
            parent.removePreference(this.mNotificationPulse);
        }
    }

    private void updatePulse() {
        boolean z = true;
        if (this.mNotificationPulse != null) {
            try {
                TwoStatePreference twoStatePreference = this.mNotificationPulse;
                if (System.getInt(getContentResolver(), "notification_light_pulse") != 1) {
                    z = false;
                }
                twoStatePreference.setChecked(z);
            } catch (SettingNotFoundException e) {
                Log.e("NotificationSettings", "notification_light_pulse not found");
            }
        }
    }

    private void initLockscreenNotifications(PreferenceCategory parent) {
        this.mLockscreen = (DropDownPreference) parent.findPreference("lock_screen_notifications");
        if (this.mLockscreen == null) {
            Log.i("NotificationSettings", "Preference not found: lock_screen_notifications");
            return;
        }
        boolean isSecureNotificationsDisabled = isSecureNotificationsDisabled();
        boolean isUnredactedNotificationsDisabled = isUnredactedNotificationsDisabled();
        if (!(isSecureNotificationsDisabled || isUnredactedNotificationsDisabled)) {
            this.mLockscreen.addItem((int) R.string.lock_screen_notifications_summary_show, Integer.valueOf(R.string.lock_screen_notifications_summary_show));
        }
        if (this.mSecure && !isSecureNotificationsDisabled) {
            this.mLockscreen.addItem((int) R.string.lock_screen_notifications_summary_hide, Integer.valueOf(R.string.lock_screen_notifications_summary_hide));
        }
        this.mLockscreen.addItem((int) R.string.lock_screen_notifications_summary_disable, Integer.valueOf(R.string.lock_screen_notifications_summary_disable));
        updateLockscreenNotifications();
        if (this.mLockscreen.getItemCount() > 1) {
            this.mLockscreen.setCallback(new C04395());
        } else {
            this.mLockscreen.setEnabled(false);
        }
    }

    private boolean isSecureNotificationsDisabled() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
        if (dpm == null || (dpm.getKeyguardDisabledFeatures(null) & 4) == 0) {
            return false;
        }
        return true;
    }

    private boolean isUnredactedNotificationsDisabled() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
        if (dpm == null || (dpm.getKeyguardDisabledFeatures(null) & 8) == 0) {
            return false;
        }
        return true;
    }

    private void updateLockscreenNotifications() {
        if (this.mLockscreen != null) {
            int i;
            boolean enabled = getLockscreenNotificationsEnabled();
            boolean allowPrivate = this.mSecure ? getLockscreenAllowPrivateNotifications() : true;
            if (!enabled) {
                i = R.string.lock_screen_notifications_summary_disable;
            } else if (allowPrivate) {
                i = R.string.lock_screen_notifications_summary_show;
            } else {
                i = R.string.lock_screen_notifications_summary_hide;
            }
            this.mLockscreenSelectedValue = i;
            this.mLockscreen.setSelectedValue(Integer.valueOf(this.mLockscreenSelectedValue));
        }
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 0) != 0;
    }

    private void refreshNotificationListeners() {
        if (this.mNotificationAccess != null) {
            int n = NotificationAccessSettings.getEnabledListenersCount(this.mContext);
            if (n == 0) {
                this.mNotificationAccess.setSummary(getResources().getString(R.string.manage_notification_access_summary_zero));
                return;
            }
            this.mNotificationAccess.setSummary(String.format(getResources().getQuantityString(R.plurals.manage_notification_access_summary_nonzero, n, new Object[]{Integer.valueOf(n)}), new Object[0]));
        }
    }

    private void refreshZenAccess() {
    }
}
