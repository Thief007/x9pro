package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.notification.NotificationBackend.AppRow;
import java.util.List;

public class AppNotificationSettings extends SettingsPreferenceFragment {
    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.NOTIFICATION_PREFERENCES");
    private static final boolean DEBUG = Log.isLoggable("AppNotificationSettings", 3);
    private AppRow mAppRow;
    private final NotificationBackend mBackend = new NotificationBackend();
    private SwitchPreference mBlock;
    private Context mContext;
    private boolean mCreated;
    private boolean mIsSystemPackage;
    private SwitchPreference mPeekable;
    private SwitchPreference mPriority;
    private SwitchPreference mSensitive;
    private int mUid;

    class C04305 implements OnPreferenceClickListener {
        C04305() {
        }

        public boolean onPreferenceClick(Preference preference) {
            AppNotificationSettings.this.mContext.startActivity(AppNotificationSettings.this.mAppRow.settingsIntent);
            return true;
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) {
            Log.d("AppNotificationSettings", "onActivityCreated mCreated=" + this.mCreated);
        }
        if (this.mCreated) {
            Log.w("AppNotificationSettings", "onActivityCreated: ignoring duplicate call");
            return;
        }
        this.mCreated = true;
        if (this.mAppRow != null) {
            AppHeader.createAppHeader(this, this.mAppRow.icon, this.mAppRow.label, AppInfoWithHeader.getInfoIntent(this, this.mAppRow.pkg));
        }
    }

    protected int getMetricsCategory() {
        return 72;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        if (DEBUG) {
            Log.d("AppNotificationSettings", "onCreate getIntent()=" + intent);
        }
        if (intent == null && args == null) {
            Log.w("AppNotificationSettings", "No intent");
            toastAndFinish();
            return;
        }
        String pkg;
        int intExtra;
        if (args == null || !args.containsKey("package")) {
            pkg = intent.getStringExtra("app_package");
        } else {
            pkg = args.getString("package");
        }
        if (args == null || !args.containsKey("uid")) {
            intExtra = intent.getIntExtra("app_uid", -1);
        } else {
            intExtra = args.getInt("uid");
        }
        this.mUid = intExtra;
        if (this.mUid == -1 || TextUtils.isEmpty(pkg)) {
            Log.w("AppNotificationSettings", "Missing extras: app_package was " + pkg + ", " + "app_uid" + " was " + this.mUid);
            toastAndFinish();
            return;
        }
        if (DEBUG) {
            Log.d("AppNotificationSettings", "Load details for pkg=" + pkg + " uid=" + this.mUid);
        }
        PackageManager pm = getPackageManager();
        PackageInfo info = findPackageInfo(pm, pkg, this.mUid);
        if (info == null) {
            Log.w("AppNotificationSettings", "Failed to find package info: app_package was " + pkg + ", " + "app_uid" + " was " + this.mUid);
            toastAndFinish();
            return;
        }
        this.mIsSystemPackage = Utils.isSystemPackage(pm, info);
        addPreferencesFromResource(R.xml.app_notification_settings);
        this.mBlock = (SwitchPreference) findPreference("block");
        this.mPriority = (SwitchPreference) findPreference("priority");
        this.mPeekable = (SwitchPreference) findPreference("peekable");
        this.mSensitive = (SwitchPreference) findPreference("sensitive");
        this.mAppRow = this.mBackend.loadAppRow(pm, info.applicationInfo);
        ArrayMap<String, AppRow> rows = new ArrayMap();
        rows.put(this.mAppRow.pkg, this.mAppRow);
        collectConfigActivities(getPackageManager(), rows);
        this.mBlock.setChecked(this.mAppRow.banned);
        updateDependents(this.mAppRow.banned);
        this.mPriority.setChecked(this.mAppRow.priority);
        this.mPeekable.setChecked(this.mAppRow.peekable);
        this.mSensitive.setChecked(this.mAppRow.sensitive);
        this.mBlock.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean banned = ((Boolean) newValue).booleanValue();
                if (banned) {
                    MetricsLogger.action(AppNotificationSettings.this.getActivity(), 147, pkg);
                }
                boolean success = AppNotificationSettings.this.mBackend.setNotificationsBanned(pkg, AppNotificationSettings.this.mUid, banned);
                if (success) {
                    AppNotificationSettings.this.updateDependents(banned);
                }
                return success;
            }
        });
        this.mPriority.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return AppNotificationSettings.this.mBackend.setHighPriority(pkg, AppNotificationSettings.this.mUid, ((Boolean) newValue).booleanValue());
            }
        });
        this.mPeekable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return AppNotificationSettings.this.mBackend.setPeekable(pkg, AppNotificationSettings.this.mUid, ((Boolean) newValue).booleanValue());
            }
        });
        this.mSensitive.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return AppNotificationSettings.this.mBackend.setSensitive(pkg, AppNotificationSettings.this.mUid, ((Boolean) newValue).booleanValue());
            }
        });
        if (this.mAppRow.settingsIntent != null) {
            findPreference("app_settings").setOnPreferenceClickListener(new C04305());
        } else {
            removePreference("app_settings");
        }
    }

    public void onResume() {
        super.onResume();
        if (this.mUid != -1 && getPackageManager().getPackagesForUid(this.mUid) == null) {
            finish();
        }
    }

    private void updateDependents(boolean banned) {
        boolean z;
        boolean z2 = true;
        boolean lockscreenSecure = new LockPatternUtils(getActivity()).isSecure(UserHandle.myUserId());
        boolean lockscreenNotificationsEnabled = getLockscreenNotificationsEnabled();
        boolean allowPrivate = getLockscreenAllowPrivateNotifications();
        Preference preference = this.mBlock;
        if (this.mIsSystemPackage) {
            z = false;
        } else {
            z = true;
        }
        setVisible(preference, z);
        preference = this.mPriority;
        if (this.mIsSystemPackage || !banned) {
            z = true;
        } else {
            z = false;
        }
        setVisible(preference, z);
        preference = this.mPeekable;
        if (this.mIsSystemPackage || !banned) {
            z = true;
        } else {
            z = false;
        }
        setVisible(preference, z);
        Preference preference2 = this.mSensitive;
        if (!this.mIsSystemPackage) {
            if (!banned && lockscreenSecure && lockscreenNotificationsEnabled) {
                z2 = allowPrivate;
            } else {
                z2 = false;
            }
        }
        setVisible(preference2, z2);
    }

    private void setVisible(Preference p, boolean visible) {
        if ((getPreferenceScreen().findPreference(p.getKey()) != null) != visible) {
            if (visible) {
                getPreferenceScreen().addPreference(p);
            } else {
                getPreferenceScreen().removePreference(p);
            }
        }
    }

    private boolean getLockscreenNotificationsEnabled() {
        return Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 0) != 0;
    }

    private void toastAndFinish() {
        Toast.makeText(this.mContext, R.string.app_not_found_dlg_text, 0).show();
        getActivity().finish();
    }

    private static PackageInfo findPackageInfo(PackageManager pm, String pkg, int uid) {
        String[] packages = pm.getPackagesForUid(uid);
        if (!(packages == null || pkg == null)) {
            int N = packages.length;
            int i = 0;
            while (i < N) {
                if (pkg.equals(packages[i])) {
                    try {
                        return pm.getPackageInfo(pkg, 64);
                    } catch (NameNotFoundException e) {
                        Log.w("AppNotificationSettings", "Failed to load package " + pkg, e);
                    }
                } else {
                    i++;
                }
            }
        }
        return null;
    }

    public static List<ResolveInfo> queryNotificationConfigActivities(PackageManager pm) {
        if (DEBUG) {
            Log.d("AppNotificationSettings", "APP_NOTIFICATION_PREFS_CATEGORY_INTENT is " + APP_NOTIFICATION_PREFS_CATEGORY_INTENT);
        }
        return pm.queryIntentActivities(APP_NOTIFICATION_PREFS_CATEGORY_INTENT, 0);
    }

    public static void collectConfigActivities(PackageManager pm, ArrayMap<String, AppRow> rows) {
        applyConfigActivities(pm, rows, queryNotificationConfigActivities(pm));
    }

    public static void applyConfigActivities(PackageManager pm, ArrayMap<String, AppRow> rows, List<ResolveInfo> resolveInfos) {
        if (DEBUG) {
            Log.d("AppNotificationSettings", "Found " + resolveInfos.size() + " preference activities" + (resolveInfos.size() == 0 ? " ;_;" : ""));
        }
        for (ResolveInfo ri : resolveInfos) {
            ActivityInfo activityInfo = ri.activityInfo;
            AppRow row = (AppRow) rows.get(activityInfo.applicationInfo.packageName);
            if (row == null) {
                if (DEBUG) {
                    Log.v("AppNotificationSettings", "Ignoring notification preference activity (" + activityInfo.name + ") for unknown package " + activityInfo.packageName);
                }
            } else if (row.settingsIntent == null) {
                row.settingsIntent = new Intent(APP_NOTIFICATION_PREFS_CATEGORY_INTENT).setClassName(activityInfo.packageName, activityInfo.name);
            } else if (DEBUG) {
                Log.v("AppNotificationSettings", "Ignoring duplicate notification preference activity (" + activityInfo.name + ") for package " + activityInfo.packageName);
            }
        }
    }
}
