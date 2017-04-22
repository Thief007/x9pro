package com.android.settings.wfd;

import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.media.MediaRouter;
import android.media.MediaRouter.Callback;
import android.media.MediaRouter.RouteInfo;
import android.media.MediaRouter.SimpleCallback;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.app.MediaRouteDialogPresenter;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.wfd.WfdSettingsExt;

public final class WifiDisplaySettings extends SettingsPreferenceFragment {
    private boolean mAutoGO;
    private PreferenceGroup mCertCategory;
    private DisplayManager mDisplayManager;
    private TextView mEmptyView;
    private WfdSettingsExt mExt;
    private final Handler mHandler = new Handler();
    private boolean mListen;
    private int mListenChannel;
    private int mOperatingChannel;
    private int mPendingChanges;
    private final BroadcastReceiver mReceiver = new C05752();
    private MediaRouter mRouter;
    private final Callback mRouterCallback = new C05774();
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange, Uri uri) {
            WifiDisplaySettings.this.scheduleUpdate(1);
        }
    };
    private boolean mStarted;
    private final Runnable mUpdateRunnable = new C05741();
    private boolean mWifiDisplayCertificationOn;
    private boolean mWifiDisplayOnSetting;
    private WifiDisplayStatus mWifiDisplayStatus;
    private Channel mWifiP2pChannel;
    private WifiP2pManager mWifiP2pManager;
    private int mWpsConfig = 4;

    class C05741 implements Runnable {
        C05741() {
        }

        public void run() {
            int changes = WifiDisplaySettings.this.mPendingChanges;
            WifiDisplaySettings.this.mPendingChanges = 0;
            WifiDisplaySettings.this.update(changes);
        }
    }

    class C05752 extends BroadcastReceiver {
        C05752() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED")) {
                WifiDisplaySettings.this.scheduleUpdate(4);
            }
        }
    }

    class C05774 extends SimpleCallback {
        C05774() {
        }

        public void onRouteAdded(MediaRouter router, RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        public void onRouteChanged(MediaRouter router, RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        public void onRouteRemoved(MediaRouter router, RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }

        public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
            WifiDisplaySettings.this.scheduleUpdate(2);
        }
    }

    private class RoutePreference extends Preference implements OnPreferenceClickListener {
        private final RouteInfo mRoute;

        public RoutePreference(Context context, RouteInfo route) {
            CharSequence deviceAddress;
            super(context);
            this.mRoute = route;
            if (TextUtils.isEmpty(route.getName())) {
                deviceAddress = route.getDeviceAddress();
            } else {
                deviceAddress = route.getName();
            }
            setTitle(deviceAddress);
            setSummary(route.getDescription());
            setEnabled(route.isEnabled());
            if (route.isSelected()) {
                setOrder(2);
                if (route.isConnecting()) {
                    setSummary(R.string.wifi_display_status_connecting);
                } else if (route.getStatusCode() == 6) {
                    setSummary(R.string.wifi_display_status_connected);
                } else {
                    setSummary(17040563);
                }
            } else if (isEnabled()) {
                setOrder(3);
                if (route.getStatusCode() == 6) {
                    setSummary(R.string.wifi_display_status_connected);
                }
            } else {
                setOrder(4);
                if (route.getStatusCode() == 5) {
                    setSummary(R.string.wifi_display_status_in_use);
                } else {
                    setSummary(R.string.wifi_display_status_not_available);
                }
            }
            setOnPreferenceClickListener(this);
        }

        public boolean onPreferenceClick(Preference preference) {
            WifiDisplaySettings.this.toggleRoute(this.mRoute);
            return true;
        }
    }

    private class UnpairedWifiDisplayPreference extends Preference implements OnPreferenceClickListener {
        private final WifiDisplay mDisplay;

        public UnpairedWifiDisplayPreference(Context context, WifiDisplay display) {
            CharSequence deviceAddress;
            super(context);
            this.mDisplay = display;
            if (TextUtils.isEmpty(display.getFriendlyDisplayName())) {
                deviceAddress = display.getDeviceAddress();
            } else {
                deviceAddress = display.getFriendlyDisplayName();
            }
            setTitle(deviceAddress);
            setSummary(17040563);
            setEnabled(display.canConnect());
            if (isEnabled()) {
                setOrder(3);
            } else {
                setOrder(4);
                setSummary(R.string.wifi_display_status_in_use);
            }
            setOnPreferenceClickListener(this);
        }

        public boolean onPreferenceClick(Preference preference) {
            WifiDisplaySettings.this.pairWifiDisplay(this.mDisplay);
            return true;
        }
    }

    private class WifiDisplayRoutePreference extends RoutePreference implements OnClickListener {
        private final WifiDisplay mDisplay;

        public WifiDisplayRoutePreference(Context context, RouteInfo route, WifiDisplay display) {
            super(context, route);
            if (route.getDescription() != null && route.getDescription().equals(getSummary())) {
                setSummary(17040563);
            }
            this.mDisplay = display;
            setWidgetLayoutResource(R.layout.wifi_display_preference);
        }

        protected void onBindView(View view) {
            super.onBindView(view);
            ImageView deviceDetails = (ImageView) view.findViewById(R.id.deviceDetails);
            if (deviceDetails != null) {
                deviceDetails.setOnClickListener(this);
                if (!isEnabled()) {
                    TypedValue value = new TypedValue();
                    getContext().getTheme().resolveAttribute(16842803, value, true);
                    deviceDetails.setImageAlpha((int) (value.getFloat() * 255.0f));
                    deviceDetails.setEnabled(true);
                }
            }
        }

        public void onClick(View v) {
            WifiDisplaySettings.this.showWifiDisplayOptionsDialog(this.mDisplay);
        }
    }

    protected int getMetricsCategory() {
        return 102;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (FeatureOption.MTK_WFD_SUPPORT) {
            this.mExt = new WfdSettingsExt(getActivity());
        }
        Context context = getActivity();
        this.mRouter = (MediaRouter) context.getSystemService("media_router");
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mWifiP2pManager = (WifiP2pManager) context.getSystemService("wifip2p");
        this.mWifiP2pChannel = this.mWifiP2pManager.initialize(context, Looper.getMainLooper(), null);
        addPreferencesFromResource(R.xml.wifi_display_settings);
        setHasOptionsMenu(true);
    }

    protected int getHelpResource() {
        return R.string.help_url_remote_display;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mEmptyView = (TextView) getView().findViewById(16908292);
        this.mEmptyView.setText(R.string.wifi_display_no_devices_found);
        getListView().setEmptyView(this.mEmptyView);
    }

    public void onStart() {
        super.onStart();
        this.mStarted = true;
        Context context = getActivity();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
        context.registerReceiver(this.mReceiver, filter);
        getContentResolver().registerContentObserver(Global.getUriFor("wifi_display_on"), false, this.mSettingsObserver);
        getContentResolver().registerContentObserver(Global.getUriFor("wifi_display_certification_on"), false, this.mSettingsObserver);
        getContentResolver().registerContentObserver(Global.getUriFor("wifi_display_wps_config"), false, this.mSettingsObserver);
        this.mRouter.addCallback(4, this.mRouterCallback, 1);
        update(-1);
        if (this.mExt != null) {
            this.mExt.onStart();
        }
    }

    public void onStop() {
        super.onStop();
        this.mStarted = false;
        if (this.mExt != null) {
            this.mExt.onStop();
        }
        getActivity().unregisterReceiver(this.mReceiver);
        getContentResolver().unregisterContentObserver(this.mSettingsObserver);
        this.mRouter.removeCallback(this.mRouterCallback);
        unscheduleUpdate();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!(this.mWifiDisplayStatus == null || this.mWifiDisplayStatus.getFeatureState() == 0)) {
            MenuItem item = menu.add(0, 1, 0, R.string.wifi_display_enable_menu_item);
            item.setCheckable(true);
            item.setChecked(this.mWifiDisplayOnSetting);
        }
        if (this.mExt != null) {
            this.mExt.onCreateOptionMenu(menu, this.mWifiDisplayStatus);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int i = 0;
        switch (item.getItemId()) {
            case 1:
                boolean z;
                if (item.isChecked()) {
                    z = false;
                } else {
                    z = true;
                }
                this.mWifiDisplayOnSetting = z;
                item.setChecked(this.mWifiDisplayOnSetting);
                ContentResolver contentResolver = getContentResolver();
                String str = "wifi_display_on";
                if (this.mWifiDisplayOnSetting) {
                    i = 1;
                }
                Global.putInt(contentResolver, str, i);
                return true;
            default:
                if (this.mExt == null || !this.mExt.onOptionMenuSelected(item, getFragmentManager())) {
                    return super.onOptionsItemSelected(item);
                }
                return true;
        }
    }

    private void scheduleUpdate(int changes) {
        if (this.mStarted) {
            if (this.mPendingChanges == 0) {
                this.mHandler.post(this.mUpdateRunnable);
            }
            this.mPendingChanges |= changes;
        }
    }

    private void unscheduleUpdate() {
        if (this.mPendingChanges != 0) {
            this.mPendingChanges = 0;
            this.mHandler.removeCallbacks(this.mUpdateRunnable);
        }
    }

    private void update(int changes) {
        boolean invalidateOptions = false;
        if ((changes & 1) != 0) {
            this.mWifiDisplayOnSetting = Global.getInt(getContentResolver(), "wifi_display_on", 0) != 0;
            this.mWifiDisplayCertificationOn = Global.getInt(getContentResolver(), "wifi_display_certification_on", 0) != 0;
            this.mWpsConfig = Global.getInt(getContentResolver(), "wifi_display_wps_config", 4);
            invalidateOptions = true;
        }
        if ((changes & 4) != 0) {
            this.mWifiDisplayStatus = this.mDisplayManager.getWifiDisplayStatus();
            invalidateOptions = true;
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        PreferenceCategory preferenceCategory = null;
        if (this.mExt != null) {
            WfdSettingsExt wfdSettingsExt = this.mExt;
            boolean z = this.mWifiDisplayStatus != null ? this.mWifiDisplayStatus.getFeatureState() == 3 : false;
            if (wfdSettingsExt.addAdditionalPreference(preferenceScreen, z)) {
                Preference pre = preferenceScreen.getPreference(preferenceScreen.getPreferenceCount() - 1);
                if (pre != null && (pre instanceof PreferenceCategory)) {
                    preferenceCategory = (PreferenceCategory) pre;
                }
            }
            if ((changes & 4) != 0) {
                this.mExt.handleWfdStatusChanged(this.mWifiDisplayStatus);
            }
        }
        int routeCount = this.mRouter.getRouteCount();
        for (int i = 0; i < routeCount; i++) {
            RouteInfo route = this.mRouter.getRouteAt(i);
            if (route.matchesTypes(4)) {
                if (preferenceCategory == null) {
                    preferenceScreen.addPreference(createRoutePreference(route));
                } else {
                    preferenceCategory.addPreference(createRoutePreference(route));
                }
            }
        }
        if (this.mWifiDisplayStatus != null && this.mWifiDisplayStatus.getFeatureState() == 3) {
            for (WifiDisplay display : this.mWifiDisplayStatus.getDisplays()) {
                if (!(display.isRemembered() || !display.isAvailable() || display.equals(this.mWifiDisplayStatus.getActiveDisplay()))) {
                    if (preferenceCategory == null) {
                        preferenceScreen.addPreference(new UnpairedWifiDisplayPreference(getActivity(), display));
                    } else {
                        preferenceCategory.addPreference(new UnpairedWifiDisplayPreference(getActivity(), display));
                    }
                }
            }
            if (this.mWifiDisplayCertificationOn) {
                buildCertificationMenu(preferenceScreen);
            }
        }
        if (invalidateOptions) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private RoutePreference createRoutePreference(RouteInfo route) {
        WifiDisplay display = findWifiDisplay(route.getDeviceAddress());
        if (display != null) {
            return new WifiDisplayRoutePreference(getActivity(), route, display);
        }
        return new RoutePreference(getActivity(), route);
    }

    private WifiDisplay findWifiDisplay(String deviceAddress) {
        if (!(this.mWifiDisplayStatus == null || deviceAddress == null)) {
            for (WifiDisplay display : this.mWifiDisplayStatus.getDisplays()) {
                if (display.getDeviceAddress().equals(deviceAddress)) {
                    return display;
                }
            }
        }
        return null;
    }

    private void buildCertificationMenu(PreferenceScreen preferenceScreen) {
        if (this.mCertCategory == null) {
            this.mCertCategory = new PreferenceCategory(getActivity());
            this.mCertCategory.setTitle(R.string.wifi_display_certification_heading);
            this.mCertCategory.setOrder(1);
        } else {
            this.mCertCategory.removeAll();
        }
        preferenceScreen.addPreference(this.mCertCategory);
        if (!this.mWifiDisplayStatus.getSessionInfo().getGroupId().isEmpty()) {
            Preference p = new Preference(getActivity());
            p.setTitle(R.string.wifi_display_session_info);
            p.setSummary(this.mWifiDisplayStatus.getSessionInfo().toString());
            this.mCertCategory.addPreference(p);
            if (this.mWifiDisplayStatus.getSessionInfo().getSessionId() != 0) {
                this.mCertCategory.addPreference(new Preference(getActivity()) {

                    class C05781 implements OnClickListener {
                        C05781() {
                        }

                        public void onClick(View v) {
                            WifiDisplaySettings.this.mDisplayManager.pauseWifiDisplay();
                        }
                    }

                    class C05792 implements OnClickListener {
                        C05792() {
                        }

                        public void onClick(View v) {
                            WifiDisplaySettings.this.mDisplayManager.resumeWifiDisplay();
                        }
                    }

                    public View getView(View convertView, ViewGroup parent) {
                        View v;
                        if (convertView == null) {
                            v = ((LayoutInflater) WifiDisplaySettings.this.getActivity().getSystemService("layout_inflater")).inflate(R.layout.two_buttons_panel, null);
                        } else {
                            v = convertView;
                        }
                        Button b = (Button) v.findViewById(R.id.left_button);
                        b.setText(R.string.wifi_display_pause);
                        b.setOnClickListener(new C05781());
                        b = (Button) v.findViewById(R.id.right_button);
                        b.setText(R.string.wifi_display_resume);
                        b.setOnClickListener(new C05792());
                        return v;
                    }
                });
            }
        }
        SwitchPreference pref = new SwitchPreference(getActivity()) {
            protected void onClick() {
                WifiDisplaySettings.this.mListen = !WifiDisplaySettings.this.mListen;
                WifiDisplaySettings.this.setListenMode(WifiDisplaySettings.this.mListen);
                setChecked(WifiDisplaySettings.this.mListen);
            }
        };
        pref.setTitle(R.string.wifi_display_listen_mode);
        pref.setChecked(this.mListen);
        this.mCertCategory.addPreference(pref);
        pref = new SwitchPreference(getActivity()) {
            protected void onClick() {
                WifiDisplaySettings.this.mAutoGO = !WifiDisplaySettings.this.mAutoGO;
                if (WifiDisplaySettings.this.mAutoGO) {
                    WifiDisplaySettings.this.startAutoGO();
                } else {
                    WifiDisplaySettings.this.stopAutoGO();
                }
                setChecked(WifiDisplaySettings.this.mAutoGO);
            }
        };
        pref.setTitle(R.string.wifi_display_autonomous_go);
        pref.setChecked(this.mAutoGO);
        this.mCertCategory.addPreference(pref);
        ListPreference lp = new ListPreference(getActivity()) {
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    WifiDisplaySettings.this.mWpsConfig = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    WifiDisplaySettings.this.getActivity().invalidateOptionsMenu();
                    Global.putInt(WifiDisplaySettings.this.getActivity().getContentResolver(), "wifi_display_wps_config", WifiDisplaySettings.this.mWpsConfig);
                }
            }
        };
        this.mWpsConfig = Global.getInt(getActivity().getContentResolver(), "wifi_display_wps_config", 4);
        String[] wpsEntries = new String[]{"Default", "PBC", "KEYPAD", "DISPLAY"};
        String[] wpsValues = new String[]{"4", "0", "2", "1"};
        lp.setTitle(R.string.wifi_display_wps_config);
        lp.setEntries(wpsEntries);
        lp.setEntryValues(wpsValues);
        lp.setValue("" + this.mWpsConfig);
        lp.setSummary("%1$s");
        this.mCertCategory.addPreference(lp);
        lp = new ListPreference(getActivity()) {
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    WifiDisplaySettings.this.mListenChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    WifiDisplaySettings.this.getActivity().invalidateOptionsMenu();
                    WifiDisplaySettings.this.setWifiP2pChannels(WifiDisplaySettings.this.mListenChannel, WifiDisplaySettings.this.mOperatingChannel);
                }
            }
        };
        String[] lcEntries = new String[]{"Auto", "1", "6", "11"};
        String[] lcValues = new String[]{"0", "1", "6", "11"};
        lp.setTitle(R.string.wifi_display_listen_channel);
        lp.setEntries(lcEntries);
        lp.setEntryValues(lcValues);
        lp.setValue("" + this.mListenChannel);
        lp.setSummary("%1$s");
        this.mCertCategory.addPreference(lp);
        lp = new ListPreference(getActivity()) {
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    WifiDisplaySettings.this.mOperatingChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    WifiDisplaySettings.this.getActivity().invalidateOptionsMenu();
                    WifiDisplaySettings.this.setWifiP2pChannels(WifiDisplaySettings.this.mListenChannel, WifiDisplaySettings.this.mOperatingChannel);
                }
            }
        };
        String[] ocEntries = new String[]{"Auto", "1", "6", "11", "36"};
        String[] ocValues = new String[]{"0", "1", "6", "11", "36"};
        lp.setTitle(R.string.wifi_display_operating_channel);
        lp.setEntries(ocEntries);
        lp.setEntryValues(ocValues);
        lp.setValue("" + this.mOperatingChannel);
        lp.setSummary("%1$s");
        this.mCertCategory.addPreference(lp);
    }

    private void startAutoGO() {
        this.mWifiP2pManager.createGroup(this.mWifiP2pChannel, new ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to start AutoGO with reason " + reason + ".");
            }
        });
    }

    private void stopAutoGO() {
        this.mWifiP2pManager.removeGroup(this.mWifiP2pChannel, new ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to stop AutoGO with reason " + reason + ".");
            }
        });
    }

    private void setListenMode(final boolean enable) {
        this.mWifiP2pManager.listen(this.mWifiP2pChannel, enable, new ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to " + (enable ? "entered" : "exited") + " listen mode with reason " + reason + ".");
            }
        });
    }

    private void setWifiP2pChannels(int lc, int oc) {
        this.mWifiP2pManager.setWifiP2pChannels(this.mWifiP2pChannel, lc, oc, new ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Slog.e("WifiDisplaySettings", "Failed to set wifi p2p channels with reason " + reason + ".");
            }
        });
    }

    private void toggleRoute(RouteInfo route) {
        int state = route.getStatusCode();
        if (state == 6 || state == 2) {
            MediaRouteDialogPresenter.showDialogFragment(getActivity(), 4, null);
            return;
        }
        if (this.mExt != null) {
            this.mExt.prepareWfdConnect();
        }
        route.select();
    }

    private void pairWifiDisplay(WifiDisplay display) {
        if (display.canConnect()) {
            if (this.mExt != null) {
                this.mExt.prepareWfdConnect();
            }
            this.mDisplayManager.connectWifiDisplay(display.getDeviceAddress());
        }
    }

    private void showWifiDisplayOptionsDialog(final WifiDisplay display) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.wifi_display_options, null);
        final EditText nameEditText = (EditText) view.findViewById(R.id.name);
        nameEditText.setText(display.getFriendlyDisplayName());
        DialogInterface.OnClickListener done = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String name = nameEditText.getText().toString().trim();
                if (name.isEmpty() || name.equals(display.getDeviceName())) {
                    name = null;
                }
                WifiDisplaySettings.this.mDisplayManager.renameWifiDisplay(display.getDeviceAddress(), name);
            }
        };
        new Builder(getActivity()).setCancelable(true).setTitle(R.string.wifi_display_options_title).setView(view).setPositiveButton(R.string.wifi_display_options_done, done).setNegativeButton(R.string.wifi_display_options_forget, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                WifiDisplaySettings.this.mDisplayManager.forgetWifiDisplay(display.getDeviceAddress());
            }
        }).create().show();
    }
}
