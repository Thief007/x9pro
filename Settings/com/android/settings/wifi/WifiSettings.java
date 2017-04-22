package com.android.settings.wifi;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.AppGlobals;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.ActionListener;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.wifi.AccessPointPreference.UserBadgeCache;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPoint.AccessPointListener;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTracker.WifiListener;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.settings.FeatureOption;
import com.mediatek.wifi.WifiSettingsExt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WifiSettings extends RestrictedSettingsFragment implements OnClickListener, Indexable, WifiListener, AccessPointListener {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C06121();
    private static boolean savedNetworksExist;
    private Bundle mAccessPointSavedState;
    private HandlerThread mBgThread;
    private ActionListener mConnectListener;
    private WifiDialog mDialog;
    private AccessPoint mDlgAccessPoint;
    private boolean mDlgEdit;
    private boolean mDlgModify;
    private TextView mEmptyView;
    private boolean mEnableNextOnConnection;
    private final IntentFilter mFilter = new IntentFilter();
    private ActionListener mForgetListener;
    private String mOpenSsid;
    private ProgressBar mProgressHeader;
    private final BroadcastReceiver mReceiver;
    private ActionListener mSaveListener;
    private AccessPoint mSelectedAccessPoint;
    private UserBadgeCache mUserBadgeCache;
    private WifiEnabler mWifiEnabler;
    protected WifiManager mWifiManager;
    private Bundle mWifiNfcDialogSavedState;
    private WifiSettingsExt mWifiSettingsExt;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;
    private WifiTracker mWifiTracker;

    static class C06121 extends BaseSearchIndexProvider {
        C06121() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.wifi_settings);
            data.screenTitle = res.getString(R.string.wifi_settings);
            data.keywords = res.getString(R.string.keywords_wifi);
            result.add(data);
            for (AccessPoint accessPoint : WifiTracker.getCurrentAccessPoints(context, true, false, false)) {
                data = new SearchIndexableRaw(context);
                data.title = accessPoint.getSsidStr();
                data.screenTitle = res.getString(R.string.wifi_settings);
                data.enabled = enabled;
                result.add(data);
            }
            return result;
        }
    }

    class C06132 extends BroadcastReceiver {
        C06132() {
        }

        public void onReceive(Context context, Intent intent) {
            WifiSettings.this.handleEvent(intent);
        }
    }

    class C06143 implements ActionListener {
        C06143() {
        }

        public void onSuccess() {
            WifiSettings.this.mWifiSettingsExt.updatePriority();
        }

        public void onFailure(int reason) {
            Activity activity = WifiSettings.this.getActivity();
            if (activity != null) {
                Toast.makeText(activity, R.string.wifi_failed_connect_message, 0).show();
            }
        }
    }

    class C06154 implements ActionListener {
        C06154() {
        }

        public void onSuccess() {
            WifiSettings.this.mWifiSettingsExt.updatePriority();
        }

        public void onFailure(int reason) {
            Activity activity = WifiSettings.this.getActivity();
            if (activity != null) {
                Toast.makeText(activity, R.string.wifi_failed_save_message, 0).show();
            }
        }
    }

    class C06165 implements ActionListener {
        C06165() {
        }

        public void onSuccess() {
            WifiSettings.this.mWifiSettingsExt.updatePriority();
        }

        public void onFailure(int reason) {
            Activity activity = WifiSettings.this.getActivity();
            if (activity != null) {
                Toast.makeText(activity, R.string.wifi_failed_forget_message, 0).show();
            }
        }
    }

    class C06176 implements LinkifyUtils.OnClickListener {
        C06176() {
        }

        public void onClick() {
            ((SettingsActivity) WifiSettings.this.getActivity()).startPreferencePanel(ScanningSettings.class.getName(), null, R.string.location_scanning_screen_title, null, null, 0);
        }
    }

    public WifiSettings() {
        super("no_config_wifi");
        this.mFilter.addAction("android.net.wifi.NO_CERTIFICATION");
        this.mReceiver = new C06132();
    }

    private void handleEvent(Intent intent) {
        String action = intent.getAction();
        Log.d("WifiSettings", "handleEvent(), action = " + action);
        if ("android.net.wifi.NO_CERTIFICATION".equals(action)) {
            String apSSID = "";
            if (this.mSelectedAccessPoint != null) {
                apSSID = "[" + this.mSelectedAccessPoint.getSsidStr() + "] ";
            }
            Log.i("WifiSettings", "Receive  no certification broadcast for AP " + apSSID);
            Toast.makeText(getActivity(), getResources().getString(R.string.wifi_no_cert_for_wapi) + apSSID, 1).show();
        }
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            this.mProgressHeader = (ProgressBar) setPinnedHeaderView((int) R.layout.wifi_progress_header);
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_settings);
        this.mUserBadgeCache = new UserBadgeCache(getPackageManager());
        this.mBgThread = new HandlerThread("WifiSettings", 10);
        this.mBgThread.start();
        this.mWifiSettingsExt = new WifiSettingsExt(getActivity());
        this.mWifiSettingsExt.onCreate();
    }

    public void onDestroy() {
        this.mBgThread.quit();
        this.mWifiSettingsExt.unregisterPriorityObserver(getContentResolver());
        super.onDestroy();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mWifiTracker = new WifiTracker(getActivity(), this, this.mBgThread.getLooper(), true, true, false);
        this.mWifiManager = this.mWifiTracker.getManager();
        this.mConnectListener = new C06143();
        this.mSaveListener = new C06154();
        this.mForgetListener = new C06165();
        if (savedInstanceState != null) {
            this.mDlgEdit = savedInstanceState.getBoolean("edit_mode");
            this.mDlgModify = savedInstanceState.getBoolean("modify_mode");
            if (savedInstanceState.containsKey("wifi_ap_state")) {
                this.mAccessPointSavedState = savedInstanceState.getBundle("wifi_ap_state");
            }
            if (savedInstanceState.containsKey("wifi_nfc_dlg_state")) {
                this.mWifiNfcDialogSavedState = savedInstanceState.getBundle("wifi_nfc_dlg_state");
            }
        }
        Intent intent = getActivity().getIntent();
        this.mEnableNextOnConnection = intent.getBooleanExtra("wifi_enable_next_on_connect", false);
        if (this.mEnableNextOnConnection && hasNextButton()) {
            ConnectivityManager connectivity = (ConnectivityManager) getActivity().getSystemService("connectivity");
            if (connectivity != null) {
                changeNextButtonState(connectivity.getNetworkInfo(1).isConnected());
            }
        }
        this.mEmptyView = initEmptyView();
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
        this.mWifiSettingsExt.onActivityCreated(this, this.mWifiManager);
        if (intent.hasExtra("wifi_start_connect_ssid")) {
            this.mOpenSsid = intent.getStringExtra("wifi_start_connect_ssid");
            onAccessPointsChanged();
        }
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.teardownSwitchBar();
        }
    }

    public void onStart() {
        super.onStart();
        this.mWifiEnabler = createWifiEnabler();
    }

    WifiEnabler createWifiEnabler() {
        SettingsActivity activity = (SettingsActivity) getActivity();
        return new WifiEnabler(activity, activity.getSwitchBar());
    }

    public void onResume() {
        Activity activity = getActivity();
        super.onResume();
        removePreference("dummy");
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.resume(activity);
        }
        this.mWifiTracker.startTracking();
        activity.registerReceiver(this.mReceiver, this.mFilter);
        this.mWifiSettingsExt.onResume();
    }

    public void onPause() {
        super.onPause();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.pause();
        }
        getActivity().unregisterReceiver(this.mReceiver);
        this.mWifiTracker.stopTracking();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isUiRestricted()) {
            addOptionsMenuItems(menu);
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    void addOptionsMenuItems(Menu menu) {
        boolean wifiIsEnabled = this.mWifiTracker.isWifiEnabled();
        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(new int[]{R.attr.ic_menu_add, R.attr.ic_wps});
        menu.add(0, 4, 0, R.string.wifi_add_network).setIcon(ta.getDrawable(0)).setEnabled(wifiIsEnabled).setShowAsAction(0);
        if (savedNetworksExist) {
            menu.add(0, 3, 0, R.string.wifi_saved_access_points_label).setIcon(ta.getDrawable(0)).setEnabled(wifiIsEnabled).setShowAsAction(0);
        }
        menu.add(0, 6, 0, R.string.menu_stats_refresh).setEnabled(wifiIsEnabled).setShowAsAction(0);
        menu.add(0, 5, 0, R.string.wifi_menu_advanced).setShowAsAction(0);
        ta.recycle();
    }

    protected int getMetricsCategory() {
        return 103;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            outState.putBoolean("edit_mode", this.mDlgEdit);
            outState.putBoolean("modify_mode", this.mDlgModify);
            if (this.mDlgAccessPoint != null) {
                this.mAccessPointSavedState = new Bundle();
                this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
                outState.putBundle("wifi_ap_state", this.mAccessPointSavedState);
            }
        }
        if (this.mWifiToNfcDialog != null && this.mWifiToNfcDialog.isShowing()) {
            Bundle savedState = new Bundle();
            this.mWifiToNfcDialog.saveState(savedState);
            outState.putBundle("wifi_nfc_dlg_state", savedState);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (isUiRestricted()) {
            return false;
        }
        switch (item.getItemId()) {
            case 1:
                showDialog(2);
                return true;
            case 2:
                showDialog(3);
                return true;
            case 3:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(SavedAccessPointsWifiSettings.class.getCanonicalName(), null, R.string.wifi_saved_access_points_titlebar, null, this, 0);
                } else {
                    startFragment(this, SavedAccessPointsWifiSettings.class.getCanonicalName(), R.string.wifi_saved_access_points_titlebar, -1, null);
                }
                return true;
            case 4:
                if (this.mWifiTracker.isWifiEnabled()) {
                    onAddNetworkPressed();
                }
                return true;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(AdvancedWifiSettings.class.getCanonicalName(), null, R.string.wifi_advanced_titlebar, null, this, 0);
                } else {
                    startFragment(this, AdvancedWifiSettings.class.getCanonicalName(), R.string.wifi_advanced_titlebar, -1, null);
                }
                return true;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                MetricsLogger.action(getActivity(), 136);
                this.mWifiTracker.forceScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
        if (info instanceof AdapterContextMenuInfo) {
            Preference preference = (Preference) getListView().getItemAtPosition(((AdapterContextMenuInfo) info).position);
            if (preference instanceof AccessPointPreference) {
                this.mSelectedAccessPoint = ((AccessPointPreference) preference).getAccessPoint();
                menu.setHeaderTitle(this.mSelectedAccessPoint.getSsid());
                if (this.mSelectedAccessPoint.isConnectable()) {
                    menu.add(0, 7, 0, R.string.wifi_menu_connect);
                }
                if (!isEditabilityLockedDown(getActivity(), this.mSelectedAccessPoint.getConfig())) {
                    if (this.mSelectedAccessPoint.isSaved() || this.mSelectedAccessPoint.isEphemeral()) {
                        menu.add(0, 8, 0, R.string.wifi_menu_forget);
                    }
                    this.mWifiSettingsExt.onCreateContextMenu(menu, this.mSelectedAccessPoint.getDetailedState(), this.mSelectedAccessPoint);
                    if (this.mSelectedAccessPoint.isSaved()) {
                        menu.add(0, 9, 0, R.string.wifi_menu_modify);
                        try {
                            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
                            if (nfcAdapter != null && nfcAdapter.isEnabled() && nfcAdapter.getModeFlag(1) == 1 && this.mSelectedAccessPoint.getSecurity() != 0) {
                                menu.add(0, 10, 0, R.string.wifi_menu_write_to_nfc);
                            }
                        } catch (UnsupportedOperationException e) {
                            Log.d("WifiSettings", "this device doesn't support NFC");
                        }
                    }
                }
            }
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (this.mSelectedAccessPoint == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                if (this.mSelectedAccessPoint.isSaved()) {
                    connect(this.mSelectedAccessPoint.getConfig());
                } else if (this.mSelectedAccessPoint.getSecurity() == 0) {
                    this.mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(this.mSelectedAccessPoint.getConfig());
                } else {
                    this.mDlgModify = false;
                    showDialog(this.mSelectedAccessPoint, true);
                }
                return true;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                forget();
                return true;
            case 9:
                this.mDlgModify = true;
                showDialog(this.mSelectedAccessPoint, true);
                return true;
            case 10:
                showDialog(6);
                return true;
            default:
                if (this.mWifiSettingsExt == null || this.mSelectedAccessPoint == null) {
                    return super.onContextItemSelected(item);
                }
                return this.mWifiSettingsExt.onContextItemSelected(item, this.mSelectedAccessPoint.getConfig());
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (!(preference instanceof AccessPointPreference)) {
            return super.onPreferenceTreeClick(screen, preference);
        }
        this.mSelectedAccessPoint = ((AccessPointPreference) preference).getAccessPoint();
        if (this.mSelectedAccessPoint.getSecurity() != 0 || this.mSelectedAccessPoint.isSaved() || this.mSelectedAccessPoint.isActive()) {
            if (this.mSelectedAccessPoint.isSaved()) {
                this.mDlgModify = false;
                showDialog(this.mSelectedAccessPoint, false);
            } else {
                this.mDlgModify = false;
                showDialog(this.mSelectedAccessPoint, true);
            }
        } else if (FeatureOption.MTK_OPEN_AP_WPS_SUPPORT && this.mSelectedAccessPoint.getWpsAvailable()) {
            showDialog(this.mSelectedAccessPoint, false);
        } else {
            this.mSelectedAccessPoint.generateOpenNetworkConfig();
            if (!savedNetworksExist) {
                savedNetworksExist = true;
                getActivity().invalidateOptionsMenu();
            }
            connect(this.mSelectedAccessPoint.getConfig());
        }
        return true;
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (accessPoint != null) {
            WifiConfiguration config = accessPoint.getConfig();
            if (isEditabilityLockedDown(getActivity(), config) && accessPoint.isActive()) {
                int userId = UserHandle.getUserId(config.creatorUid);
                PackageManager pm = getActivity().getPackageManager();
                IPackageManager ipm = AppGlobals.getPackageManager();
                String appName = pm.getNameForUid(config.creatorUid);
                try {
                    CharSequence label = pm.getApplicationLabel(ipm.getApplicationInfo(appName, 0, userId));
                    if (label != null) {
                        appName = label.toString();
                    }
                } catch (RemoteException e) {
                }
                new Builder(getActivity()).setTitle(accessPoint.getSsid()).setMessage(getString(R.string.wifi_alert_lockdown_by_device_owner, new Object[]{appName})).setPositiveButton(17039370, null).show();
                return;
            }
        }
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint;
        this.mDlgEdit = edit;
        showDialog(1);
    }

    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case 1:
                AccessPoint ap = this.mDlgAccessPoint;
                if (ap == null && this.mAccessPointSavedState != null) {
                    ap = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                    this.mDlgAccessPoint = ap;
                    this.mAccessPointSavedState = null;
                }
                this.mSelectedAccessPoint = ap;
                if (this.mSelectedAccessPoint != null) {
                    this.mWifiSettingsExt.recordPriority(this.mSelectedAccessPoint.getConfig());
                }
                this.mDialog = new WifiDialog(getActivity(), this, ap, this.mDlgEdit, this.mDlgModify, false, ap != null ? isEditabilityLockedDown(getActivity(), ap.getConfig()) : true);
                return this.mDialog;
            case 2:
                return new WpsDialog(getActivity(), 0);
            case 3:
                return new WpsDialog(getActivity(), 1);
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                if (this.mSelectedAccessPoint != null) {
                    this.mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(), this.mSelectedAccessPoint.getConfig().networkId, this.mSelectedAccessPoint.getSecurity(), this.mWifiManager);
                } else if (this.mWifiNfcDialogSavedState != null) {
                    this.mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(), this.mWifiNfcDialogSavedState, this.mWifiManager);
                }
                return this.mWifiToNfcDialog;
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    public void onAccessPointsChanged() {
        boolean z = false;
        if (getActivity() != null) {
            if (isUiRestricted()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
                return;
            }
            switch (this.mWifiManager.getWifiState()) {
                case 0:
                    addMessagePreference(R.string.wifi_stopping);
                    setProgressBarVisible(true);
                    break;
                case 1:
                    setOffMessage();
                    setProgressBarVisible(false);
                    break;
                case 2:
                    this.mWifiSettingsExt.emptyScreen(getPreferenceScreen());
                    setProgressBarVisible(true);
                    break;
                case 3:
                    Collection<AccessPoint> accessPoints = this.mWifiTracker.getAccessPoints();
                    this.mWifiSettingsExt.emptyCategory(getPreferenceScreen());
                    boolean hasAvailableAccessPoints = false;
                    Log.d("WifiSettings", "accessPoints.size() = " + accessPoints.size());
                    int index = 0;
                    for (AccessPoint accessPoint : accessPoints) {
                        if (accessPoint.getLevel() != -1) {
                            hasAvailableAccessPoints = true;
                            int index2;
                            WifiSettingsExt wifiSettingsExt;
                            PreferenceScreen preferenceScreen;
                            boolean z2;
                            if (accessPoint.getTag() != null) {
                                Preference pref = (Preference) accessPoint.getTag();
                                index2 = index + 1;
                                pref.setOrder(index);
                                wifiSettingsExt = this.mWifiSettingsExt;
                                preferenceScreen = getPreferenceScreen();
                                if (accessPoint.getConfig() != null) {
                                    z2 = true;
                                } else {
                                    z2 = false;
                                }
                                wifiSettingsExt.addPreference(preferenceScreen, pref, z2);
                                index = index2;
                            } else {
                                AccessPointPreference preference = new AccessPointPreference(accessPoint, getActivity(), this.mUserBadgeCache, false);
                                index2 = index + 1;
                                preference.setOrder(index);
                                if (!(this.mOpenSsid == null || !this.mOpenSsid.equals(accessPoint.getSsidStr()) || accessPoint.isSaved() || accessPoint.getSecurity() == 0)) {
                                    onPreferenceTreeClick(getPreferenceScreen(), preference);
                                    this.mOpenSsid = null;
                                }
                                wifiSettingsExt = this.mWifiSettingsExt;
                                preferenceScreen = getPreferenceScreen();
                                if (accessPoint.getConfig() != null) {
                                    z2 = true;
                                } else {
                                    z2 = false;
                                }
                                wifiSettingsExt.addPreference(preferenceScreen, preference, z2);
                                accessPoint.setListener(this);
                                index = index2;
                            }
                        }
                    }
                    if (hasAvailableAccessPoints) {
                        setProgressBarVisible(false);
                    } else {
                        setProgressBarVisible(true);
                        addMessagePreference(R.string.wifi_empty_list_wifi_on);
                    }
                    this.mWifiSettingsExt.refreshCategory(getPreferenceScreen());
                    break;
            }
            if (savedNetworksExist != this.mWifiTracker.doSavedNetworksExist()) {
                if (!savedNetworksExist) {
                    z = true;
                }
                savedNetworksExist = z;
                getActivity().invalidateOptionsMenu();
            }
        }
    }

    protected TextView initEmptyView() {
        TextView emptyView = (TextView) getActivity().findViewById(16908292);
        emptyView.setGravity(8388627);
        getListView().setEmptyView(emptyView);
        return emptyView;
    }

    private void setOffMessage() {
        if (this.mEmptyView != null) {
            CharSequence briefText = getText(R.string.wifi_empty_list_wifi_off);
            boolean wifiScanningMode = Global.getInt(getActivity().getContentResolver(), "wifi_scan_always_enabled", 0) == 1;
            if (isUiRestricted() || !wifiScanningMode) {
                this.mEmptyView.setText(briefText, BufferType.SPANNABLE);
            } else {
                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append(briefText);
                contentBuilder.append("\n\n");
                contentBuilder.append(getText(R.string.wifi_scan_notify_text));
                LinkifyUtils.linkify(this.mEmptyView, contentBuilder, new C06176());
            }
            ((Spannable) this.mEmptyView.getText()).setSpan(new TextAppearanceSpan(getActivity(), 16973892), 0, briefText.length(), 33);
            this.mWifiSettingsExt.emptyScreen(getPreferenceScreen());
        }
    }

    private void addMessagePreference(int messageId) {
        if (this.mEmptyView != null) {
            this.mEmptyView.setText(messageId);
        }
        this.mWifiSettingsExt.emptyScreen(getPreferenceScreen());
    }

    protected void setProgressBarVisible(boolean visible) {
        if (this.mProgressHeader != null) {
            this.mProgressHeader.setVisibility(visible ? 0 : 8);
        }
    }

    public void onWifiStateChanged(int state) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
            switch (state) {
                case 1:
                    setOffMessage();
                    setProgressBarVisible(false);
                    break;
                case 2:
                    addMessagePreference(R.string.wifi_starting);
                    setProgressBarVisible(true);
                    break;
            }
        }
    }

    public void onConnectedChanged() {
        changeNextButtonState(this.mWifiTracker.isConnected());
    }

    private void changeNextButtonState(boolean enabled) {
        if (this.mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(enabled);
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -3 && this.mSelectedAccessPoint != null) {
            forget();
        } else if (button == -1 && this.mDialog != null) {
            submit(this.mDialog.getController());
        }
    }

    void submit(WifiConfigController configController) {
        DetailedState detailedState = null;
        WifiConfiguration config = configController.getConfig();
        Log.d("WifiSettings", "submit, config = " + config);
        if (this.mSelectedAccessPoint != null) {
            WifiSettingsExt wifiSettingsExt = this.mWifiSettingsExt;
            AccessPoint accessPoint = this.mSelectedAccessPoint;
            if (this.mSelectedAccessPoint.getNetworkInfo() != null) {
                detailedState = this.mSelectedAccessPoint.getNetworkInfo().getDetailedState();
            }
            wifiSettingsExt.submit(config, accessPoint, detailedState);
        }
        if (config == null) {
            if (this.mSelectedAccessPoint != null && this.mSelectedAccessPoint.isSaved()) {
                connect(this.mSelectedAccessPoint.getConfig());
            }
        } else if (configController.isModify()) {
            this.mWifiManager.save(config, this.mSaveListener);
        } else {
            this.mWifiManager.save(config, this.mSaveListener);
            if (this.mSelectedAccessPoint != null) {
                connect(config);
            }
        }
        this.mWifiTracker.resumeScanning();
    }

    void forget() {
        MetricsLogger.action(getActivity(), 137);
        if (this.mSelectedAccessPoint.isSaved()) {
            this.mWifiManager.forget(this.mSelectedAccessPoint.getConfig().networkId, this.mForgetListener);
        } else if (this.mSelectedAccessPoint.getNetworkInfo() == null || this.mSelectedAccessPoint.getNetworkInfo().getState() == State.DISCONNECTED) {
            Log.e("WifiSettings", "Failed to forget invalid network " + this.mSelectedAccessPoint.getConfig());
            return;
        } else {
            this.mWifiManager.disableEphemeralNetwork(AccessPoint.convertToQuotedString(this.mSelectedAccessPoint.getSsidStr()));
        }
        this.mWifiTracker.resumeScanning();
        changeNextButtonState(false);
        this.mWifiSettingsExt.updatePriority();
    }

    protected void connect(WifiConfiguration config) {
        MetricsLogger.action(getActivity(), 135);
        this.mWifiManager.connect(config, this.mConnectListener);
    }

    void onAddNetworkPressed() {
        MetricsLogger.action(getActivity(), 134);
        this.mSelectedAccessPoint = null;
        showDialog(null, true);
    }

    protected int getHelpResource() {
        return R.string.help_url_wifi;
    }

    public void onAccessPointChanged(AccessPoint accessPoint) {
        ((AccessPointPreference) accessPoint.getTag()).refresh();
    }

    public void onLevelChanged(AccessPoint accessPoint) {
        ((AccessPointPreference) accessPoint.getTag()).onLevelChanged();
    }

    static boolean isEditabilityLockedDown(Context context, WifiConfiguration config) {
        return !canModifyNetwork(context, config);
    }

    static boolean canModifyNetwork(Context context, WifiConfiguration config) {
        boolean z = false;
        if (config == null) {
            return true;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature("android.software.device_admin") && dpm == null) {
            return false;
        }
        boolean isConfigEligibleForLockdown = false;
        if (dpm != null) {
            String deviceOwnerPackageName = dpm.getDeviceOwner();
            if (deviceOwnerPackageName != null) {
                try {
                    isConfigEligibleForLockdown = pm.getPackageUid(deviceOwnerPackageName, 0) == config.creatorUid;
                } catch (NameNotFoundException e) {
                }
            }
        }
        if (!isConfigEligibleForLockdown) {
            return true;
        }
        if (!(Global.getInt(context.getContentResolver(), "wifi_device_owner_configs_lockdown", 0) != 0)) {
            z = true;
        }
        return z;
    }

    public void addNetworkForSelector() {
        MetricsLogger.action(getActivity(), 134);
        this.mSelectedAccessPoint = null;
        showDialog(null, true);
    }
}
