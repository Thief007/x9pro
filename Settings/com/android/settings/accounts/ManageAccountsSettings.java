package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.AccountPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.accounts.AuthenticatorHelper.OnAccountsUpdateListener;
import com.android.settings.location.LocationSettings;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class ManageAccountsSettings extends AccountPreferenceBase implements OnAccountsUpdateListener {
    private String mAccountType;
    private String[] mAuthorities;
    private TextView mErrorInfoView;
    private Account mFirstAccount;

    private class FragmentStarter implements OnPreferenceClickListener {
        private final String mClass;
        private final int mTitleRes;

        public FragmentStarter(String className, int title) {
            this.mClass = className;
            this.mTitleRes = title;
        }

        public boolean onPreferenceClick(Preference preference) {
            ((SettingsActivity) ManageAccountsSettings.this.getActivity()).startPreferencePanel(this.mClass, null, this.mTitleRes, null, null, 0);
            if (this.mClass.equals(LocationSettings.class.getName())) {
                ManageAccountsSettings.this.getActivity().sendBroadcast(new Intent("com.android.settings.accounts.LAUNCHING_LOCATION_SETTINGS"), "android.permission.WRITE_SECURE_SETTINGS");
            }
            return true;
        }
    }

    public /* bridge */ /* synthetic */ PreferenceScreen addPreferencesForType(String accountType, PreferenceScreen parent) {
        return super.addPreferencesForType(accountType, parent);
    }

    public /* bridge */ /* synthetic */ ArrayList getAuthoritiesForAccountType(String type) {
        return super.getAuthoritiesForAccountType(type);
    }

    public /* bridge */ /* synthetic */ void updateAuthDescriptions() {
        super.updateAuthDescriptions();
    }

    protected int getMetricsCategory() {
        return 11;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle args = getArguments();
        if (args != null && args.containsKey("account_type")) {
            this.mAccountType = args.getString("account_type");
        }
        addPreferencesFromResource(R.xml.manage_accounts_settings);
        setHasOptionsMenu(true);
    }

    public void onResume() {
        super.onResume();
        this.mAuthenticatorHelper.listenToAccountUpdates();
        updateAuthDescriptions();
        showAccountsIfNeeded();
        showSyncState();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.manage_accounts_screen, container, false);
        Utils.prepareCustomPreferencesList(container, view, (ListView) view.findViewById(16908298), false);
        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        this.mErrorInfoView = (TextView) getView().findViewById(R.id.sync_settings_error_info);
        this.mErrorInfoView.setVisibility(8);
        this.mAuthorities = activity.getIntent().getStringArrayExtra("authorities");
        Bundle args = getArguments();
        if (args != null && args.containsKey("account_label")) {
            getActivity().setTitle(args.getString("account_label"));
        }
    }

    public void onPause() {
        super.onPause();
        this.mAuthenticatorHelper.stopListeningToAccountUpdates();
    }

    public void onStop() {
        super.onStop();
        Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, 16);
        activity.getActionBar().setCustomView(null);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferences, Preference preference) {
        if (!(preference instanceof AccountPreference)) {
            return false;
        }
        startAccountSettings((AccountPreference) preference);
        return true;
    }

    private void startAccountSettings(AccountPreference acctPref) {
        Bundle args = new Bundle();
        args.putParcelable("account", acctPref.getAccount());
        args.putParcelable("android.intent.extra.USER", this.mUserHandle);
        ((SettingsActivity) getActivity()).startPreferencePanel(AccountSyncSettings.class.getCanonicalName(), args, R.string.account_sync_settings_title, acctPref.getAccount().name, this, 1);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, getString(R.string.sync_menu_sync_now)).setIcon(R.drawable.ic_menu_refresh_holo_dark);
        menu.add(0, 2, 0, getString(R.string.sync_menu_sync_cancel)).setIcon(17301560);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        boolean z;
        boolean z2 = false;
        super.onPrepareOptionsMenu(menu);
        boolean syncActive = !ContentResolver.getCurrentSyncsAsUser(this.mUserHandle.getIdentifier()).isEmpty();
        MenuItem findItem = menu.findItem(1);
        if (syncActive || this.mFirstAccount == null) {
            z = false;
        } else {
            z = true;
        }
        findItem.setVisible(z);
        MenuItem findItem2 = menu.findItem(2);
        if (syncActive && this.mFirstAccount != null) {
            z2 = true;
        }
        findItem2.setVisible(z2);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                requestOrCancelSyncForAccounts(true);
                return true;
            case 2:
                requestOrCancelSyncForAccounts(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestOrCancelSyncForAccounts(boolean sync) {
        int userId = this.mUserHandle.getIdentifier();
        SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(userId);
        Bundle extras = new Bundle();
        extras.putBoolean("force", true);
        int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof AccountPreference) {
                Account account = ((AccountPreference) pref).getAccount();
                for (int j = 0; j < syncAdapters.length; j++) {
                    SyncAdapterType sa = syncAdapters[j];
                    if (syncAdapters[j].accountType.equals(this.mAccountType) && ContentResolver.getSyncAutomaticallyAsUser(account, sa.authority, userId)) {
                        if (sync) {
                            ContentResolver.requestSyncAsUser(account, sa.authority, userId, extras);
                        } else {
                            ContentResolver.cancelSyncAsUser(account, sa.authority, userId);
                        }
                    }
                }
            }
        }
    }

    protected void onSyncStateUpdated() {
        showSyncState();
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private void showSyncState() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            int i;
            int userId = this.mUserHandle.getIdentifier();
            List<SyncInfo> currentSyncs = ContentResolver.getCurrentSyncsAsUser(userId);
            boolean anySyncFailed = false;
            Date date = new Date();
            SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypesAsUser(userId);
            HashSet<String> userFacing = new HashSet();
            for (SyncAdapterType sa : syncAdapters) {
                if (sa.isUserVisible()) {
                    userFacing.add(sa.authority);
                }
            }
            int count = getPreferenceScreen().getPreferenceCount();
            for (int i2 = 0; i2 < count; i2++) {
                Preference pref = getPreferenceScreen().getPreference(i2);
                if (pref instanceof AccountPreference) {
                    AccountPreference accountPref = (AccountPreference) pref;
                    Account account = accountPref.getAccount();
                    int syncCount = 0;
                    long lastSuccessTime = 0;
                    boolean syncIsFailing = false;
                    ArrayList<String> authorities = accountPref.getAuthorities();
                    boolean syncingNow = false;
                    if (authorities != null) {
                        for (String authority : authorities) {
                            SyncStatusInfo status = ContentResolver.getSyncStatusAsUser(account, authority, userId);
                            boolean syncEnabled = isSyncEnabled(userId, account, authority);
                            boolean authorityIsPending = ContentResolver.isSyncPending(account, authority);
                            boolean activelySyncing = isSyncing(currentSyncs, account, authority);
                            boolean lastSyncFailed = (status == null || !syncEnabled || status.lastFailureTime == 0) ? false : status.getLastFailureMesgAsInt(0) != 1;
                            if (!(!lastSyncFailed || activelySyncing || authorityIsPending)) {
                                syncIsFailing = true;
                                anySyncFailed = true;
                            }
                            syncingNow |= activelySyncing;
                            if (status != null && lastSuccessTime < status.lastSuccessTime) {
                                lastSuccessTime = status.lastSuccessTime;
                            }
                            if (syncEnabled && userFacing.contains(authority)) {
                                i = 1;
                            } else {
                                i = 0;
                            }
                            syncCount += i;
                        }
                    } else if (Log.isLoggable("AccountSettings", 2)) {
                        Log.v("AccountSettings", "no syncadapters found for " + account);
                    }
                    if (syncIsFailing) {
                        accountPref.setSyncStatus(2, true);
                    } else if (syncCount == 0) {
                        accountPref.setSyncStatus(1, true);
                    } else if (syncCount <= 0) {
                        accountPref.setSyncStatus(1, true);
                    } else if (syncingNow) {
                        accountPref.setSyncStatus(3, true);
                    } else {
                        accountPref.setSyncStatus(0, true);
                        if (lastSuccessTime > 0) {
                            accountPref.setSyncStatus(0, false);
                            date.setTime(lastSuccessTime);
                            String timeString = formatSyncDate(date);
                            accountPref.setSummary(getResources().getString(R.string.last_synced, new Object[]{timeString}));
                        }
                    }
                }
            }
            TextView textView = this.mErrorInfoView;
            if (anySyncFailed) {
                i = 0;
            } else {
                i = 8;
            }
            textView.setVisibility(i);
        }
    }

    private boolean isSyncing(List<SyncInfo> currentSyncs, Account account, String authority) {
        int count = currentSyncs.size();
        for (int i = 0; i < count; i++) {
            SyncInfo syncInfo = (SyncInfo) currentSyncs.get(i);
            if (syncInfo.account.equals(account) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSyncEnabled(int userId, Account account, String authority) {
        if (ContentResolver.getSyncAutomaticallyAsUser(account, authority, userId) && ContentResolver.getMasterSyncAutomaticallyAsUser(userId) && ContentResolver.getIsSyncableAsUser(account, authority, userId) > 0) {
            return true;
        }
        return false;
    }

    public void onAccountsUpdate(UserHandle userHandle) {
        showAccountsIfNeeded();
        onSyncStateUpdated();
    }

    private void showAccountsIfNeeded() {
        if (getActivity() != null) {
            Account[] accounts = AccountManager.get(getActivity()).getAccountsAsUser(this.mUserHandle.getIdentifier());
            getPreferenceScreen().removeAll();
            this.mFirstAccount = null;
            addPreferencesFromResource(R.xml.manage_accounts_settings);
            for (Account account : accounts) {
                if (this.mAccountType == null || account.type.equals(this.mAccountType)) {
                    ArrayList<String> auths = getAuthoritiesForAccountType(account.type);
                    boolean showAccount = true;
                    if (this.mAuthorities != null && auths != null) {
                        showAccount = false;
                        for (String requestedAuthority : this.mAuthorities) {
                            if (auths.contains(requestedAuthority)) {
                                showAccount = true;
                                break;
                            }
                        }
                    }
                    if (showAccount) {
                        getPreferenceScreen().addPreference(new AccountPreference(getActivity(), account, getDrawableForType(account.type), auths, false));
                        if (this.mFirstAccount == null) {
                            this.mFirstAccount = account;
                        }
                    }
                }
            }
            if (this.mAccountType == null || this.mFirstAccount == null) {
                finish();
            } else {
                addAuthenticatorSettings();
            }
        }
    }

    private void addAuthenticatorSettings() {
        PreferenceScreen prefs = addPreferencesForType(this.mAccountType, getPreferenceScreen());
        if (prefs != null) {
            updatePreferenceIntents(prefs);
        }
    }

    private void updatePreferenceIntents(PreferenceScreen prefs) {
        final PackageManager pm = getActivity().getPackageManager();
        int i = 0;
        while (i < prefs.getPreferenceCount()) {
            Preference pref = prefs.getPreference(i);
            Intent intent = pref.getIntent();
            if (intent != null) {
                if (intent.getAction().equals("android.settings.LOCATION_SOURCE_SETTINGS")) {
                    pref.setOnPreferenceClickListener(new FragmentStarter(LocationSettings.class.getName(), R.string.location_settings_title));
                } else if (pm.resolveActivityAsUser(intent, 65536, this.mUserHandle.getIdentifier()) == null) {
                    prefs.removePreference(pref);
                } else {
                    intent.putExtra("account", this.mFirstAccount);
                    intent.setFlags(intent.getFlags() | 268435456);
                    pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            Intent prefIntent = preference.getIntent();
                            if (ManageAccountsSettings.this.isSafeIntent(pm, prefIntent)) {
                                ManageAccountsSettings.this.getActivity().startActivityAsUser(prefIntent, ManageAccountsSettings.this.mUserHandle);
                            } else {
                                Log.e("AccountSettings", "Refusing to launch authenticator intent becauseit exploits Settings permissions: " + prefIntent);
                            }
                            return true;
                        }
                    });
                }
            }
            i++;
        }
    }

    private boolean isSafeIntent(PackageManager pm, Intent intent) {
        boolean z = true;
        AuthenticatorDescription authDesc = this.mAuthenticatorHelper.getAccountTypeDescription(this.mAccountType);
        ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        ActivityInfo resolvedActivityInfo = resolveInfo.activityInfo;
        ApplicationInfo resolvedAppInfo = resolvedActivityInfo.applicationInfo;
        try {
            ApplicationInfo authenticatorAppInf = pm.getApplicationInfo(authDesc.packageName, 0);
            if (!(resolvedActivityInfo.exported || resolvedAppInfo.uid == authenticatorAppInf.uid)) {
                z = false;
            }
            return z;
        } catch (NameNotFoundException e) {
            Log.e("AccountSettings", "Intent considered unsafe due to exception.", e);
            return false;
        }
    }

    protected void onAuthDescriptionsUpdated() {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (pref instanceof AccountPreference) {
                AccountPreference accPref = (AccountPreference) pref;
                accPref.setSummary(getLabelForType(accPref.getAccount().type));
            }
        }
    }
}
