package com.android.settings.vpn2;

import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IConnectivityManager;
import android.net.IConnectivityManager.Stub;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.security.KeyStore;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VpnSettings extends SettingsPreferenceFragment implements Callback, OnPreferenceClickListener {
    private static final NetworkRequest VPN_REQUEST = new Builder().removeCapability(15).removeCapability(13).removeCapability(14).build();
    private HashMap<String, AppPreference> mAppPreferences = new HashMap();
    private HashMap<String, ConfigPreference> mConfigPreferences = new HashMap();
    private LegacyVpnInfo mConnectedLegacyVpn;
    private ConnectivityManager mConnectivityManager;
    private final IConnectivityManager mConnectivityService = Stub.asInterface(ServiceManager.getService("connectivity"));
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private OnClickListener mManageListener = new C05721();
    private NetworkCallback mNetworkCallback = new C05732();
    private boolean mUnavailable;
    private Handler mUpdater;
    private UserManager mUserManager;

    class C05721 implements OnClickListener {
        C05721() {
        }

        public void onClick(View view) {
            ManageablePreference tag = view.getTag();
            if (tag instanceof ConfigPreference) {
                ConfigDialogFragment.show(VpnSettings.this, ((ConfigPreference) tag).getProfile(), true, true);
            } else if (tag instanceof AppPreference) {
                AppPreference pref = (AppPreference) tag;
                AppDialogFragment.show(VpnSettings.this, pref.getPackageInfo(), pref.getLabel(), true, pref.getState() == 3);
            }
        }
    }

    class C05732 extends NetworkCallback {
        C05732() {
        }

        public void onAvailable(Network network) {
            if (VpnSettings.this.mUpdater != null) {
                VpnSettings.this.mUpdater.sendEmptyMessage(0);
            }
        }

        public void onLost(Network network) {
            if (VpnSettings.this.mUpdater != null) {
                VpnSettings.this.mUpdater.sendEmptyMessage(0);
            }
        }
    }

    protected int getMetricsCategory() {
        return 100;
    }

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        this.mUserManager = (UserManager) getSystemService("user");
        if (this.mUserManager.hasUserRestriction("no_config_vpn")) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            setHasOptionsMenu(false);
            return;
        }
        this.mConnectivityManager = (ConnectivityManager) getSystemService("connectivity");
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.vpn_settings2);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.vpn, menu);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (SystemProperties.getBoolean("persist.radio.imsregrequired", false)) {
            menu.findItem(R.id.vpn_lockdown).setVisible(false);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.vpn_create:
                long millis = System.currentTimeMillis();
                while (this.mConfigPreferences.containsKey(Long.toHexString(millis))) {
                    millis++;
                }
                ConfigDialogFragment.show(this, new VpnProfile(Long.toHexString(millis)), true, false);
                return true;
            case R.id.vpn_lockdown:
                LockdownConfigFragment.show(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onResume() {
        super.onResume();
        if (this.mUnavailable) {
            TextView emptyView = (TextView) getView().findViewById(16908292);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.vpn_settings_not_available);
            }
            return;
        }
        if (getActivity().getIntent().getBooleanExtra("android.net.vpn.PICK_LOCKDOWN", false)) {
            LockdownConfigFragment.show(this);
        }
        this.mConnectivityManager.registerNetworkCallback(VPN_REQUEST, this.mNetworkCallback);
        if (this.mUpdater == null) {
            this.mUpdater = new Handler(this);
        }
        this.mUpdater.sendEmptyMessage(0);
    }

    public void onPause() {
        if (this.mUnavailable) {
            super.onPause();
            return;
        }
        this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
        if (this.mUpdater != null) {
            this.mUpdater.removeCallbacksAndMessages(null);
        }
        super.onPause();
    }

    public boolean handleMessage(Message message) {
        this.mUpdater.removeMessages(0);
        PreferenceGroup vpnGroup = getPreferenceScreen();
        vpnGroup.removeAll();
        this.mConfigPreferences.clear();
        this.mAppPreferences.clear();
        for (VpnProfile profile : loadVpnProfiles(this.mKeyStore, new int[0])) {
            ConfigPreference pref = new ConfigPreference(getActivity(), this.mManageListener, profile);
            pref.setOnPreferenceClickListener(this);
            this.mConfigPreferences.put(profile.key, pref);
            vpnGroup.addPreference(pref);
        }
        for (PackageOps pkg : getVpnApps()) {
            String key = getVpnIdentifier(UserHandle.getUserId(pkg.getUid()), pkg.getPackageName());
            AppPreference pref2 = new AppPreference(getActivity(), this.mManageListener, pkg.getPackageName(), pkg.getUid());
            pref2.setOnPreferenceClickListener(this);
            this.mAppPreferences.put(key, pref2);
            vpnGroup.addPreference(pref2);
        }
        try {
            this.mConnectedLegacyVpn = null;
            LegacyVpnInfo info = this.mConnectivityService.getLegacyVpnInfo(UserHandle.myUserId());
            if (info != null) {
                ConfigPreference preference = (ConfigPreference) this.mConfigPreferences.get(info.key);
                if (preference != null) {
                    preference.setState(info.state);
                    this.mConnectedLegacyVpn = info;
                }
            }
            for (UserHandle profile2 : this.mUserManager.getUserProfiles()) {
                VpnConfig cfg = this.mConnectivityService.getVpnConfig(profile2.getIdentifier());
                if (cfg != null) {
                    AppPreference preference2 = (AppPreference) this.mAppPreferences.get(getVpnIdentifier(profile2.getIdentifier(), cfg.user));
                    if (preference2 != null) {
                        preference2.setState(3);
                    }
                }
            }
        } catch (RemoteException e) {
        }
        this.mUpdater.sendEmptyMessageDelayed(0, 1000);
        return true;
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof ConfigPreference) {
            VpnProfile profile = ((ConfigPreference) preference).getProfile();
            if (this.mConnectedLegacyVpn != null && profile.key.equals(this.mConnectedLegacyVpn.key) && this.mConnectedLegacyVpn.state == 3) {
                try {
                    if (this.mConnectedLegacyVpn.intent == null) {
                        Toast.makeText(getActivity(), R.string.lockdown_vpn_already_connected, 1).show();
                    } else {
                        this.mConnectedLegacyVpn.intent.send();
                    }
                    return true;
                } catch (Exception e) {
                }
            }
            ConfigDialogFragment.show(this, profile, false, true);
            return true;
        } else if (!(preference instanceof AppPreference)) {
            return false;
        } else {
            AppPreference pref = (AppPreference) preference;
            boolean connected = pref.getState() == 3;
            if (!connected) {
                try {
                    UserHandle user = new UserHandle(UserHandle.getUserId(pref.getUid()));
                    Context userContext = getActivity().createPackageContextAsUser(getActivity().getPackageName(), 0, user);
                    Intent appIntent = userContext.getPackageManager().getLaunchIntentForPackage(pref.getPackageName());
                    if (appIntent != null) {
                        userContext.startActivityAsUser(appIntent, user);
                        return true;
                    }
                } catch (NameNotFoundException e2) {
                }
            }
            AppDialogFragment.show(this, pref.getPackageInfo(), pref.getLabel(), false, connected);
            return true;
        }
    }

    private static String getVpnIdentifier(int userId, String packageName) {
        return Integer.toString(userId) + "_" + packageName;
    }

    protected int getHelpResource() {
        return R.string.help_url_vpn;
    }

    private List<PackageOps> getVpnApps() {
        List<PackageOps> result = Lists.newArrayList();
        SparseArray<Boolean> currentProfileIds = new SparseArray();
        for (UserHandle profile : this.mUserManager.getUserProfiles()) {
            currentProfileIds.put(profile.getIdentifier(), Boolean.TRUE);
        }
        List<PackageOps> apps = ((AppOpsManager) getSystemService("appops")).getPackagesForOps(new int[]{47});
        if (apps != null) {
            for (PackageOps pkg : apps) {
                if (currentProfileIds.get(UserHandle.getUserId(pkg.getUid())) != null) {
                    boolean allowed = false;
                    for (OpEntry op : pkg.getOps()) {
                        if (op.getOp() == 47 && op.getMode() == 0) {
                            allowed = true;
                        }
                    }
                    if (allowed) {
                        result.add(pkg);
                    }
                }
            }
        }
        return result;
    }

    protected static List<VpnProfile> loadVpnProfiles(KeyStore keyStore, int... excludeTypes) {
        ArrayList<VpnProfile> result = Lists.newArrayList();
        if (!keyStore.isUnlocked()) {
            return result;
        }
        for (String key : keyStore.list("VPN_")) {
            VpnProfile profile = VpnProfile.decode(key, keyStore.get("VPN_" + key));
            if (!(profile == null || ArrayUtils.contains(excludeTypes, profile.type))) {
                result.add(profile);
            }
        }
        return result;
    }
}
