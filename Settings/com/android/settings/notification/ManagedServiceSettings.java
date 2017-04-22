package com.android.settings.notification;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageItemInfo.DisplayNameComparator;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.ServiceListing.Callback;
import java.util.Collections;
import java.util.List;

public abstract class ManagedServiceSettings extends SettingsPreferenceFragment {
    private final Config mConfig = getConfig();
    private Context mContext;
    private TextView mEmpty;
    private PackageManager mPM;
    private ServiceListing mServiceListing;

    class C04311 implements Callback {
        C04311() {
        }

        public void onServicesReloaded(List<ServiceInfo> services) {
            ManagedServiceSettings.this.updateList(services);
        }
    }

    protected static class Config {
        int emptyText;
        String intentAction;
        String noun;
        String permission;
        String setting;
        String tag;
        int warningDialogSummary;
        int warningDialogTitle;

        protected Config() {
        }
    }

    public class ScaryWarningDialogFragment extends DialogFragment {

        class C04342 implements OnClickListener {
            C04342() {
            }

            public void onClick(DialogInterface dialog, int id) {
            }
        }

        public ScaryWarningDialogFragment setServiceInfo(ComponentName cn, String label) {
            Bundle args = new Bundle();
            args.putString("c", cn.flattenToString());
            args.putString("l", label);
            setArguments(args);
            return this;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            String label = args.getString("l");
            final ComponentName cn = ComponentName.unflattenFromString(args.getString("c"));
            return new Builder(ManagedServiceSettings.this.mContext).setMessage(getResources().getString(ManagedServiceSettings.this.mConfig.warningDialogSummary, new Object[]{label})).setTitle(getResources().getString(ManagedServiceSettings.this.mConfig.warningDialogTitle, new Object[]{label})).setCancelable(true).setPositiveButton(R.string.allow, new OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ManagedServiceSettings.this.mServiceListing.setEnabled(cn, true);
                }
            }).setNegativeButton(R.string.deny, new C04342()).create();
        }
    }

    protected abstract Config getConfig();

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mPM = this.mContext.getPackageManager();
        this.mServiceListing = new ServiceListing(this.mContext, this.mConfig);
        this.mServiceListing.addCallback(new C04311());
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this.mContext));
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.managed_service_settings, container, false);
        this.mEmpty = (TextView) v.findViewById(16908292);
        this.mEmpty.setText(this.mConfig.emptyText);
        ((ListView) v.findViewById(16908298)).setEmptyView(this.mEmpty);
        return v;
    }

    public void onResume() {
        super.onResume();
        this.mServiceListing.reload();
        this.mServiceListing.setListening(true);
    }

    public void onPause() {
        super.onPause();
        this.mServiceListing.setListening(false);
    }

    private void updateList(List<ServiceInfo> services) {
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        Collections.sort(services, new DisplayNameComparator(this.mPM));
        for (ServiceInfo service : services) {
            final ComponentName cn = new ComponentName(service.packageName, service.name);
            final String title = service.loadLabel(this.mPM).toString();
            SwitchPreference pref = new SwitchPreference(this.mContext);
            pref.setPersistent(false);
            pref.setIcon(service.loadIcon(this.mPM));
            pref.setTitle(title);
            pref.setChecked(this.mServiceListing.isEnabled(cn));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return ManagedServiceSettings.this.setEnabled(cn, title, ((Boolean) newValue).booleanValue());
                }
            });
            screen.addPreference(pref);
        }
    }

    private boolean setEnabled(ComponentName service, String title, boolean enable) {
        if (!enable) {
            this.mServiceListing.setEnabled(service, false);
            return true;
        } else if (this.mServiceListing.isEnabled(service)) {
            return true;
        } else {
            new ScaryWarningDialogFragment().setServiceInfo(service, title).show(getFragmentManager(), "dialog");
            return false;
        }
    }
}
