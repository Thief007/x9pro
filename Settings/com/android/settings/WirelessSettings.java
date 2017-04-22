package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Global;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.ims.ImsManager;
import com.android.settings.nfc.NfcEnabler;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.IWfcSettingsExt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WirelessSettings extends SettingsPreferenceFragment implements Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C02071();
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;
    private PreferenceScreen mButtonWfc;
    private ConnectivityManager mCm;
    private String mManageMobilePlanMessage;
    private PreferenceScreen mNetworkSettingsPreference;
    private NfcAdapter mNfcAdapter;
    private NfcEnabler mNfcEnabler;
    private NsdEnabler mNsdEnabler;
    private PhoneStateListener mPhoneStateListener = new C02082();
    private PackageManager mPm;
    private final BroadcastReceiver mReceiver = new C02093();
    private TelephonyManager mTm;
    private UserManager mUm;
    IWfcSettingsExt mWfcExt;

    static class C02071 extends BaseSearchIndexProvider {
        C02071() {
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            new SearchIndexableResource(context).xmlResId = R.xml.wireless_settings;
            return Arrays.asList(new SearchIndexableResource[]{sir});
        }

        public List<String> getNonIndexableKeys(Context context) {
            boolean isWimaxEnabled;
            ArrayList<String> result = new ArrayList();
            result.add("toggle_nsd");
            UserManager um = (UserManager) context.getSystemService("user");
            boolean isSecondaryUser = UserHandle.myUserId() != 0;
            if (isSecondaryUser) {
                isWimaxEnabled = false;
            } else {
                isWimaxEnabled = context.getResources().getBoolean(17956963);
            }
            if (!isWimaxEnabled || um.hasUserRestriction("no_config_mobile_networks")) {
                result.add("wimax_settings");
            }
            if (isSecondaryUser) {
                result.add("vpn_settings");
            }
            NfcManager manager = (NfcManager) context.getSystemService("nfc");
            if (manager != null) {
                if (manager.getDefaultAdapter() == null) {
                    result.add("toggle_nfc");
                    result.add("android_beam_settings");
                    result.add("toggle_mtk_nfc");
                } else if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
                    result.add("toggle_nfc");
                    result.add("android_beam_settings");
                } else {
                    result.add("toggle_mtk_nfc");
                }
            }
            if (isSecondaryUser || Utils.isWifiOnly(context)) {
                result.add("mobile_network_settings");
                result.add("manage_mobile_plan");
            }
            if (!context.getResources().getBoolean(R.bool.config_show_mobile_plan)) {
                result.add("manage_mobile_plan");
            }
            PackageManager pm = context.getPackageManager();
            if (pm.hasSystemFeature("android.hardware.type.television")) {
                result.add("toggle_airplane");
            }
            result.add("proxy_settings");
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
            if (isSecondaryUser || !cm.isTetheringSupported()) {
                result.add("tether_settings");
            }
            boolean isCellBroadcastAppLinkEnabled = context.getResources().getBoolean(17956972);
            if (isCellBroadcastAppLinkEnabled) {
                try {
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                        isCellBroadcastAppLinkEnabled = false;
                    }
                } catch (IllegalArgumentException e) {
                    isCellBroadcastAppLinkEnabled = false;
                }
            }
            if (isSecondaryUser || !r3) {
                result.add("cell_broadcast_settings");
            }
            if (!WirelessSettings.isAPKInstalled(context, "com.mediatek.rcse.RCSE_SETTINGS")) {
                result.add("rcse_settings");
            }
            return result;
        }
    }

    class C02082 extends PhoneStateListener {
        C02082() {
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d("WirelessSettings", "PhoneStateListener, new state=" + state);
            if (state == 0 && WirelessSettings.this.getActivity() != null) {
                WirelessSettings.this.updateMobileNetworkEnabled();
            }
        }
    }

    class C02093 extends BroadcastReceiver {
        C02093() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(action)) {
                Log.d("WirelessSettings", "ACTION_SIM_INFO_UPDATE received");
                WirelessSettings.this.updateMobileNetworkEnabled();
            } else if ("android.telephony.action.CARRIER_CONFIG_CHANGED".equals(action)) {
                Log.d("WirelessSettings", "carrier config changed...");
                if (WirelessSettings.this.mButtonWfc == null) {
                    return;
                }
                if (ImsManager.isWfcEnabledByPlatform(context)) {
                    Log.d("WirelessSettings", "wfc enabled, add WCF setting");
                    WirelessSettings.this.getPreferenceScreen().addPreference(WirelessSettings.this.mButtonWfc);
                    WirelessSettings.this.mWfcExt.initPlugin(WirelessSettings.this);
                    WirelessSettings.this.mButtonWfc.setSummary(WirelessSettings.this.mWfcExt.getWfcSummary(context, WifiCallingSettings.getWfcModeSummary(context, ImsManager.getWfcMode(context))));
                    WirelessSettings.this.mWfcExt.customizedWfcPreference(WirelessSettings.this.getActivity(), WirelessSettings.this.getPreferenceScreen());
                    WirelessSettings.this.mWfcExt.onWirelessSettingsEvent(4);
                    return;
                }
                Log.d("WirelessSettings", "wfc disabled, remove WCF setting");
                WirelessSettings.this.mWfcExt.onWirelessSettingsEvent(4);
                WirelessSettings.this.getPreferenceScreen().removePreference(WirelessSettings.this.mButtonWfc);
            }
        }
    }

    class C02104 implements OnClickListener {
        C02104() {
        }

        public void onClick(DialogInterface dialog, int id) {
            WirelessSettings.this.log("MANAGE_MOBILE_PLAN_DIALOG.onClickListener id=" + id);
            WirelessSettings.this.mManageMobilePlanMessage = null;
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        log("onPreferenceTreeClick: preference=" + preference);
        if (preference == this.mAirplaneModePreference && Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
            startActivityForResult(new Intent("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", null), 1);
            return true;
        }
        if (preference == findPreference("manage_mobile_plan")) {
            onManageMobilePlanClick();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onManageMobilePlanClick() {
        log("onManageMobilePlanClick:");
        this.mManageMobilePlanMessage = null;
        Resources resources = getActivity().getResources();
        NetworkInfo ni = this.mCm.getActiveNetworkInfo();
        if (this.mTm.hasIccCard() && ni != null) {
            Intent provisioningIntent = new Intent("android.intent.action.ACTION_CARRIER_SETUP");
            List<String> carrierPackages = this.mTm.getCarrierPackageNamesForIntent(provisioningIntent);
            if (carrierPackages == null || carrierPackages.isEmpty()) {
                String url = this.mCm.getMobileProvisioningUrl();
                if (TextUtils.isEmpty(url)) {
                    if (TextUtils.isEmpty(this.mTm.getSimOperatorName())) {
                        if (TextUtils.isEmpty(this.mTm.getNetworkOperatorName())) {
                            this.mManageMobilePlanMessage = resources.getString(R.string.mobile_unknown_sim_operator);
                        } else {
                            this.mManageMobilePlanMessage = resources.getString(R.string.mobile_no_provisioning_url, new Object[]{this.mTm.getNetworkOperatorName()});
                        }
                    } else {
                        this.mManageMobilePlanMessage = resources.getString(R.string.mobile_no_provisioning_url, new Object[]{this.mTm.getSimOperatorName()});
                    }
                } else {
                    Intent intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER");
                    intent.setData(Uri.parse(url));
                    intent.setFlags(272629760);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.w("WirelessSettings", "onManageMobilePlanClick: startActivity failed" + e);
                    }
                }
            } else {
                if (carrierPackages.size() != 1) {
                    Log.w("WirelessSettings", "Multiple matching carrier apps found, launching the first.");
                }
                provisioningIntent.setPackage((String) carrierPackages.get(0));
                startActivity(provisioningIntent);
                return;
            }
        } else if (this.mTm.hasIccCard()) {
            this.mManageMobilePlanMessage = resources.getString(R.string.mobile_connect_to_internet);
        } else {
            this.mManageMobilePlanMessage = resources.getString(R.string.mobile_insert_sim_card);
        }
        if (!TextUtils.isEmpty(this.mManageMobilePlanMessage)) {
            log("onManageMobilePlanClick: message=" + this.mManageMobilePlanMessage);
            showDialog(1);
        }
    }

    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case 1:
                return new Builder(getActivity()).setMessage(this.mManageMobilePlanMessage).setCancelable(false).setPositiveButton(17039370, new C02104()).create();
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    private void log(String s) {
        Log.d("WirelessSettings", s);
    }

    protected int getMetricsCategory() {
        return 110;
    }

    public void onCreate(Bundle savedInstanceState) {
        PreferenceScreen root;
        Preference ps;
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mManageMobilePlanMessage = savedInstanceState.getString("mManageMobilePlanMessage");
        }
        log("onCreate: mManageMobilePlanMessage=" + this.mManageMobilePlanMessage);
        this.mCm = (ConnectivityManager) getSystemService("connectivity");
        this.mTm = (TelephonyManager) getSystemService("phone");
        this.mPm = getPackageManager();
        this.mUm = (UserManager) getSystemService("user");
        addPreferencesFromResource(R.xml.wireless_settings);
        boolean isSecondaryUser = UserHandle.myUserId() != 0;
        Activity activity = getActivity();
        this.mAirplaneModePreference = (SwitchPreference) findPreference("toggle_airplane");
        Preference nfc = (SwitchPreference) findPreference("toggle_nfc");
        PreferenceScreen androidBeam = (PreferenceScreen) findPreference("android_beam_settings");
        Preference nsd = (SwitchPreference) findPreference("toggle_nsd");
        PreferenceScreen mtkNfc = (PreferenceScreen) findPreference("toggle_mtk_nfc");
        this.mAirplaneModeEnabler = new AirplaneModeEnabler(activity, this.mAirplaneModePreference);
        this.mNetworkSettingsPreference = (PreferenceScreen) findPreference("mobile_network_settings");
        this.mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam);
        this.mButtonWfc = (PreferenceScreen) findPreference("wifi_calling_settings");
        getPreferenceScreen().removePreference(nsd);
        String toggleable = Global.getString(activity.getContentResolver(), "airplane_mode_toggleable_radios");
        boolean z = !isSecondaryUser ? getResources().getBoolean(17956963) : false;
        if (!z || this.mUm.hasUserRestriction("no_config_mobile_networks")) {
            root = getPreferenceScreen();
            ps = findPreference("wimax_settings");
            if (ps != null) {
                root.removePreference(ps);
            }
        } else if (toggleable == null || (!toggleable.contains("wimax") && z)) {
            findPreference("wimax_settings").setDependency("toggle_airplane");
        }
        if (toggleable == null || !toggleable.contains("wifi")) {
            findPreference("vpn_settings").setDependency("toggle_airplane");
        }
        if (isSecondaryUser || this.mUm.hasUserRestriction("no_config_vpn")) {
            removePreference("vpn_settings");
        }
        boolean isCellBroadcastAppLinkEnabled;
        if (toggleable == null || toggleable.contains("bluetooth")) {
            if (toggleable == null || !toggleable.contains("nfc")) {
                findPreference("toggle_nfc").setDependency("toggle_airplane");
                findPreference("android_beam_settings").setDependency("toggle_airplane");
                findPreference("toggle_mtk_nfc").setDependency("toggle_airplane");
            }
            this.mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
            if (this.mNfcAdapter == null) {
                getPreferenceScreen().removePreference(nfc);
                getPreferenceScreen().removePreference(androidBeam);
                this.mNfcEnabler = null;
                getPreferenceScreen().removePreference(mtkNfc);
            } else if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
                getPreferenceScreen().removePreference(mtkNfc);
            } else {
                getPreferenceScreen().removePreference(nfc);
                getPreferenceScreen().removePreference(androidBeam);
                this.mNfcEnabler = null;
            }
            if (isSecondaryUser || Utils.isWifiOnly(getActivity()) || this.mUm.hasUserRestriction("no_config_mobile_networks")) {
                removePreference("mobile_network_settings");
                removePreference("manage_mobile_plan");
            }
            if (!(getResources().getBoolean(R.bool.config_show_mobile_plan) || findPreference("manage_mobile_plan") == null)) {
                removePreference("manage_mobile_plan");
            }
            if (this.mPm.hasSystemFeature("android.hardware.type.television")) {
                removePreference("toggle_airplane");
            }
            Preference mGlobalProxy = findPreference("proxy_settings");
            DevicePolicyManager mDPM = (DevicePolicyManager) activity.getSystemService("device_policy");
            getPreferenceScreen().removePreference(mGlobalProxy);
            mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() != null);
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService("connectivity");
            if (isSecondaryUser && cm.isTetheringSupported() && !this.mUm.hasUserRestriction("no_config_tethering")) {
                Preference p = findPreference("tether_settings");
                p.setTitle(Utils.getTetheringLabel(cm));
                p.setEnabled(!TetherSettings.isProvisioningNeededButUnavailable(getActivity()));
            } else {
                getPreferenceScreen().removePreference(findPreference("tether_settings"));
            }
            isCellBroadcastAppLinkEnabled = getResources().getBoolean(17956972);
            if (isCellBroadcastAppLinkEnabled) {
                try {
                    if (this.mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                        isCellBroadcastAppLinkEnabled = false;
                    }
                } catch (IllegalArgumentException e) {
                    isCellBroadcastAppLinkEnabled = false;
                }
            }
            if (isSecondaryUser || !r8 || this.mUm.hasUserRestriction("no_config_cell_broadcasts")) {
                root = getPreferenceScreen();
                ps = findPreference("cell_broadcast_settings");
                if (ps != null) {
                    root.removePreference(ps);
                }
            }
            if (isAPKInstalled(activity, "com.mediatek.rcse.RCSE_SETTINGS")) {
                Log.d("WirelessSettings", "com.mediatek.rcse.RCSE_SETTINGS is not installed");
                getPreferenceScreen().removePreference(findPreference("rcse_settings"));
            } else {
                findPreference("rcse_settings").setIntent(new Intent("com.mediatek.rcse.RCSE_SETTINGS"));
            }
            UtilsExt.getRcsSettingsPlugin(getActivity()).addRCSPreference(getActivity(), getPreferenceScreen());
            this.mWfcExt = UtilsExt.getWfcSettingsPlugin(getActivity());
        }
        findPreference("toggle_nfc").setDependency("toggle_airplane");
        findPreference("android_beam_settings").setDependency("toggle_airplane");
        findPreference("toggle_mtk_nfc").setDependency("toggle_airplane");
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (this.mNfcAdapter == null) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            this.mNfcEnabler = null;
            getPreferenceScreen().removePreference(mtkNfc);
        } else if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
            getPreferenceScreen().removePreference(mtkNfc);
        } else {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            this.mNfcEnabler = null;
        }
        removePreference("mobile_network_settings");
        removePreference("manage_mobile_plan");
        removePreference("manage_mobile_plan");
        if (this.mPm.hasSystemFeature("android.hardware.type.television")) {
            removePreference("toggle_airplane");
        }
        Preference mGlobalProxy2 = findPreference("proxy_settings");
        DevicePolicyManager mDPM2 = (DevicePolicyManager) activity.getSystemService("device_policy");
        getPreferenceScreen().removePreference(mGlobalProxy2);
        if (mDPM2.getGlobalProxyAdmin() != null) {
        }
        mGlobalProxy2.setEnabled(mDPM2.getGlobalProxyAdmin() != null);
        ConnectivityManager cm2 = (ConnectivityManager) activity.getSystemService("connectivity");
        if (isSecondaryUser) {
        }
        getPreferenceScreen().removePreference(findPreference("tether_settings"));
        isCellBroadcastAppLinkEnabled = getResources().getBoolean(17956972);
        if (isCellBroadcastAppLinkEnabled) {
            if (this.mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                isCellBroadcastAppLinkEnabled = false;
            }
        }
        root = getPreferenceScreen();
        ps = findPreference("cell_broadcast_settings");
        if (ps != null) {
            root.removePreference(ps);
        }
        if (isAPKInstalled(activity, "com.mediatek.rcse.RCSE_SETTINGS")) {
            Log.d("WirelessSettings", "com.mediatek.rcse.RCSE_SETTINGS is not installed");
            getPreferenceScreen().removePreference(findPreference("rcse_settings"));
        } else {
            findPreference("rcse_settings").setIntent(new Intent("com.mediatek.rcse.RCSE_SETTINGS"));
        }
        UtilsExt.getRcsSettingsPlugin(getActivity()).addRCSPreference(getActivity(), getPreferenceScreen());
        this.mWfcExt = UtilsExt.getWfcSettingsPlugin(getActivity());
    }

    public void onResume() {
        super.onResume();
        this.mAirplaneModeEnabler.resume();
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.resume();
        }
        if (this.mNsdEnabler != null) {
            this.mNsdEnabler.resume();
        }
        Context context = getActivity();
        this.mWfcExt.initPlugin(this);
        if (ImsManager.isWfcEnabledByPlatform(context)) {
            getPreferenceScreen().addPreference(this.mButtonWfc);
            this.mButtonWfc.setSummary(WifiCallingSettings.getWfcModeSummary(context, ImsManager.getWfcMode(context)));
            this.mButtonWfc.setSummary(this.mWfcExt.getWfcSummary(context, WifiCallingSettings.getWfcModeSummary(context, ImsManager.getWfcMode(context))));
            this.mWfcExt.customizedWfcPreference(getActivity(), getPreferenceScreen());
        } else {
            removePreference("wifi_calling_settings");
        }
        ((TelephonyManager) getSystemService("phone")).listen(this.mPhoneStateListener, 32);
        updateMobileNetworkEnabled();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        getActivity().registerReceiver(this.mReceiver, intentFilter);
        this.mWfcExt.onWirelessSettingsEvent(0);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!TextUtils.isEmpty(this.mManageMobilePlanMessage)) {
            outState.putString("mManageMobilePlanMessage", this.mManageMobilePlanMessage);
        }
    }

    public void onPause() {
        super.onPause();
        this.mAirplaneModeEnabler.pause();
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.pause();
        }
        if (this.mNsdEnabler != null) {
            this.mNsdEnabler.pause();
        }
        ((TelephonyManager) getSystemService("phone")).listen(this.mPhoneStateListener, 0);
        getActivity().unregisterReceiver(this.mReceiver);
        this.mWfcExt.onWirelessSettingsEvent(1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            this.mAirplaneModeEnabler.setAirplaneModeInECM(Boolean.valueOf(data.getBooleanExtra("exit_ecm_result", false)).booleanValue(), this.mAirplaneModePreference.isChecked());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }

    private static boolean isAPKInstalled(Context context, String action) {
        List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(new Intent(action), 0);
        if (apps == null || apps.size() == 0) {
            return false;
        }
        return true;
    }

    private void updateMobileNetworkEnabled() {
        boolean z = true;
        ISettingsMiscExt miscExt = UtilsExt.getMiscPlugin(getActivity());
        int callState = ((TelephonyManager) getSystemService("phone")).getCallState();
        int simNum = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoCount();
        Log.d("WirelessSettings", "callstate = " + callState + " simNum = " + simNum);
        if (simNum > 0 && callState == 0 && !miscExt.isWifiOnlyModeSet()) {
            this.mNetworkSettingsPreference.setEnabled(true);
        } else if (CdmaFeatureOptionUtils.isCT6MSupport()) {
            this.mNetworkSettingsPreference.setEnabled(CdmaFeatureOptionUtils.isCTLteTddTestSupport());
        } else {
            PreferenceScreen preferenceScreen = this.mNetworkSettingsPreference;
            if (!UtilsExt.getSimManagmentExtPlugin(getActivity()).useCtTestcard()) {
                z = false;
            }
            preferenceScreen.setEnabled(z);
        }
    }
}
