package com.mediatek.audioprofile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.search.Indexable;

public class AudioProfileNotification extends SettingsPreferenceFragment implements Indexable {
    private Context mContext;
    private DropDownPreference mLockscreen;
    private int mLockscreenSelectedValue;
    private Preference mNotificationAccess;
    private TwoStatePreference mNotificationPulse;
    private PackageManager mPM;
    private boolean mSecure;

    class C06791 implements OnPreferenceChangeListener {
        C06791() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return System.putInt(AudioProfileNotification.this.getContentResolver(), "notification_light_pulse", ((Boolean) newValue).booleanValue() ? 1 : 0);
        }
    }

    class C06802 implements Callback {
        C06802() {
        }

        public boolean onItemSelected(int pos, Object value) {
            int i = 0;
            int val = ((Integer) value).intValue();
            if (val == AudioProfileNotification.this.mLockscreenSelectedValue) {
                return true;
            }
            int i2;
            boolean enabled = val != R.string.lock_screen_notifications_summary_disable;
            boolean show = val == R.string.lock_screen_notifications_summary_show;
            ContentResolver -wrap0 = AudioProfileNotification.this.getContentResolver();
            String str = "lock_screen_allow_private_notifications";
            if (show) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            Secure.putInt(-wrap0, str, i2);
            ContentResolver -wrap02 = AudioProfileNotification.this.getContentResolver();
            String str2 = "lock_screen_show_notifications";
            if (enabled) {
                i = 1;
            }
            Secure.putInt(-wrap02, str2, i);
            AudioProfileNotification.this.mLockscreenSelectedValue = val;
            return true;
        }
    }

    public void onCreate(Bundle icicle) {
        Log.d("@M_AudioProfileNotification", "onCreate");
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mPM = this.mContext.getPackageManager();
        this.mSecure = new LockPatternUtils(getActivity()).isSecure(UserHandle.myUserId());
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.audioprofile_notification);
        initPulse();
        initLockscreenNotifications();
        this.mNotificationAccess = findPreference("manage_notification_access");
        refreshNotificationListeners();
    }

    public void onResume() {
        Log.d("@M_AudioProfileNotification", "onResume");
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    private void initPulse() {
        this.mNotificationPulse = (TwoStatePreference) findPreference("notification_pulse");
        if (this.mNotificationPulse == null) {
            Log.i("@M_AudioProfileNotification", "Preference not found: notification_pulse");
            return;
        }
        if (getResources().getBoolean(17956924)) {
            updatePulse();
            this.mNotificationPulse.setOnPreferenceChangeListener(new C06791());
        } else {
            getPreferenceScreen().removePreference(this.mNotificationPulse);
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
                Log.e("@M_AudioProfileNotification", "notification_light_pulse not found");
            }
        }
    }

    private void initLockscreenNotifications() {
        this.mLockscreen = (DropDownPreference) findPreference("lock_screen_notifications");
        if (this.mLockscreen == null) {
            Log.i("@M_AudioProfileNotification", "Preference not found: lock_screen_notifications");
            return;
        }
        this.mLockscreen.addItem((int) R.string.lock_screen_notifications_summary_show, Integer.valueOf(R.string.lock_screen_notifications_summary_show));
        if (this.mSecure) {
            this.mLockscreen.addItem((int) R.string.lock_screen_notifications_summary_hide, Integer.valueOf(R.string.lock_screen_notifications_summary_hide));
        }
        this.mLockscreen.addItem((int) R.string.lock_screen_notifications_summary_disable, Integer.valueOf(R.string.lock_screen_notifications_summary_disable));
        updateLockscreenNotifications();
        this.mLockscreen.setCallback(new C06802());
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

    private void refreshNotificationListeners() {
        if (this.mNotificationAccess == null) {
            return;
        }
        if (NotificationAccessSettings.getListenersCount(this.mPM) == 0) {
            getPreferenceScreen().removePreference(this.mNotificationAccess);
            return;
        }
        int n = NotificationAccessSettings.getEnabledListenersCount(this.mContext);
        if (n == 0) {
            this.mNotificationAccess.setSummary(getResources().getString(R.string.manage_notification_access_summary_zero));
            return;
        }
        this.mNotificationAccess.setSummary(String.format(getResources().getQuantityString(R.plurals.manage_notification_access_summary_nonzero, n, new Object[]{Integer.valueOf(n)}), new Object[0]));
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 0) != 0;
    }

    protected int getMetricsCategory() {
        return 71;
    }
}
