package com.android.settings.notification;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo.DisplayNameComparator;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZenAccessSettings extends SettingsPreferenceFragment {
    private Context mContext;
    private TextView mEmpty;
    private NotificationManager mNoMan;
    private final SettingObserver mObserver = new SettingObserver();
    private PackageManager mPkgMan;

    public static class ScaryWarningDialogFragment extends DialogFragment {

        class C04602 implements OnClickListener {
            C04602() {
            }

            public void onClick(DialogInterface dialog, int id) {
            }
        }

        public ScaryWarningDialogFragment setPkgInfo(String pkg, CharSequence label) {
            Bundle args = new Bundle();
            args.putString("p", pkg);
            String str = "l";
            if (!TextUtils.isEmpty(label)) {
                pkg = label.toString();
            }
            args.putString(str, pkg);
            setArguments(args);
            return this;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            final String pkg = args.getString("p");
            String label = args.getString("l");
            return new Builder(getContext()).setMessage(getResources().getString(R.string.zen_access_warning_dialog_summary)).setTitle(getResources().getString(R.string.zen_access_warning_dialog_title, new Object[]{label})).setCancelable(true).setPositiveButton(R.string.allow, new OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ZenAccessSettings.setAccess(ScaryWarningDialogFragment.this.getContext(), pkg, true);
                }
            }).setNegativeButton(R.string.deny, new C04602()).create();
        }
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void onChange(boolean selfChange, Uri uri) {
            ZenAccessSettings.this.reloadList();
        }
    }

    protected int getMetricsCategory() {
        return 180;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mPkgMan = this.mContext.getPackageManager();
        this.mNoMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this.mContext));
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.managed_service_settings, container, false);
        this.mEmpty = (TextView) v.findViewById(16908292);
        this.mEmpty.setText(R.string.zen_access_empty_text);
        return v;
    }

    public void onResume() {
        super.onResume();
        reloadList();
        getContentResolver().registerContentObserver(Secure.getUriFor("enabled_notification_policy_access_packages"), false, this.mObserver);
    }

    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(this.mObserver);
    }

    private void reloadList() {
        int i = 0;
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        ArrayList<ApplicationInfo> apps = new ArrayList();
        ArraySet<String> requesting = this.mNoMan.getPackagesRequestingNotificationPolicyAccess();
        if (!(requesting == null || requesting.isEmpty())) {
            List<ApplicationInfo> installed = this.mPkgMan.getInstalledApplications(0);
            if (installed != null) {
                for (ApplicationInfo app : installed) {
                    if (requesting.contains(app.packageName)) {
                        apps.add(app);
                    }
                }
            }
        }
        Collections.sort(apps, new DisplayNameComparator(this.mPkgMan));
        for (ApplicationInfo app2 : apps) {
            final String pkg = app2.packageName;
            final CharSequence label = app2.loadLabel(this.mPkgMan);
            SwitchPreference pref = new SwitchPreference(this.mContext);
            pref.setPersistent(false);
            pref.setIcon(app2.loadIcon(this.mPkgMan));
            pref.setTitle(label);
            pref.setChecked(hasAccess(pkg));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean access = ((Boolean) newValue).booleanValue();
                    if (access) {
                        new ScaryWarningDialogFragment().setPkgInfo(pkg, label).show(ZenAccessSettings.this.getFragmentManager(), "dialog");
                        return false;
                    }
                    ZenAccessSettings.setAccess(ZenAccessSettings.this.mContext, pkg, access);
                    return true;
                }
            });
            screen.addPreference(pref);
        }
        TextView textView = this.mEmpty;
        if (!apps.isEmpty()) {
            i = 8;
        }
        textView.setVisibility(i);
    }

    private boolean hasAccess(String pkg) {
        return this.mNoMan.isNotificationPolicyAccessGrantedForPackage(pkg);
    }

    private static void setAccess(final Context context, final String pkg, final boolean access) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ((NotificationManager) context.getSystemService(NotificationManager.class)).setNotificationPolicyAccessGranted(pkg, access);
            }
        });
    }
}
