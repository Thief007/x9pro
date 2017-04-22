package com.android.settings.applications;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import com.android.settings.AppListPreference;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultEmergencyPreference extends AppListPreference {
    public static final Intent QUERY_INTENT = new Intent("android.telephony.action.EMERGENCY_ASSISTANCE");
    private final ContentResolver mContentResolver;

    class C02531 extends AsyncTask<Void, Void, Set<String>> {
        C02531() {
        }

        protected Set<String> doInBackground(Void[] params) {
            return DefaultEmergencyPreference.this.resolveAssistPackageAndQueryApps();
        }

        protected void onPostExecute(Set<String> entries) {
            DefaultEmergencyPreference.this.setPackageNames((CharSequence[]) entries.toArray(new String[entries.size()]), Secure.getString(DefaultEmergencyPreference.this.mContentResolver, "emergency_assistance_application"));
        }
    }

    public DefaultEmergencyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContentResolver = context.getContentResolver();
        if (isAvailable(context)) {
            load();
        }
    }

    protected boolean persistString(String value) {
        String previousValue = Secure.getString(this.mContentResolver, "emergency_assistance_application");
        if (!(TextUtils.isEmpty(value) || Objects.equals(value, previousValue))) {
            Secure.putString(this.mContentResolver, "emergency_assistance_application", value);
        }
        setSummary(getEntry());
        return true;
    }

    private void load() {
        new C02531().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private Set<String> resolveAssistPackageAndQueryApps() {
        Set<String> packages = new ArraySet();
        PackageManager packageManager = getContext().getPackageManager();
        List<ResolveInfo> infos = packageManager.queryIntentActivities(QUERY_INTENT, 0);
        PackageInfo bestMatch = null;
        int size = infos.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo info = (ResolveInfo) infos.get(i);
            if (!(info == null || info.activityInfo == null || packages.contains(info.activityInfo.packageName))) {
                String packageName = info.activityInfo.packageName;
                packages.add(packageName);
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                    if (bestMatch == null || ((!isSystemApp(bestMatch) && isSystemApp(packageInfo)) || (isSystemApp(bestMatch) == isSystemApp(packageInfo) && bestMatch.firstInstallTime > packageInfo.firstInstallTime))) {
                        bestMatch = packageInfo;
                    }
                } catch (NameNotFoundException e) {
                }
            }
        }
        String defaultPackage = Secure.getString(this.mContentResolver, "emergency_assistance_application");
        boolean defaultMissing = !TextUtils.isEmpty(defaultPackage) ? !packages.contains(defaultPackage) : true;
        if (bestMatch != null && defaultMissing) {
            Secure.putString(this.mContentResolver, "emergency_assistance_application", bestMatch.packageName);
        }
        return packages;
    }

    public static boolean isAvailable(Context context) {
        if (!isCapable(context) || context.getPackageManager().resolveActivity(QUERY_INTENT, 0) == null) {
            return false;
        }
        return true;
    }

    public static boolean isCapable(Context context) {
        return false;
    }

    private static boolean isSystemApp(PackageInfo info) {
        if (info.applicationInfo == null || (info.applicationInfo.flags & 1) == 0) {
            return false;
        }
        return true;
    }
}
