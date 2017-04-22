package com.android.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings.System;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx.Stub;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.PhoneServiceStateHandler;
import com.mediatek.settings.sim.PhoneServiceStateHandler.Listener;
import com.mediatek.settings.sim.RadioPowerPreference;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable, Listener {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C05262();
    private List<SubscriptionInfo> mAvailableSubInfos = null;
    private Context mContext;
    private boolean mIsAirplaneModeOn = false;
    private ISettingsMiscExt mMiscExt;
    private int mNumSlots;
    private final OnSubscriptionsChangedListener mOnSubscriptionsChangeListener = new C05251();
    private BroadcastReceiver mReceiver = new C05273();
    private List<SubscriptionInfo> mSelectableSubInfos = null;
    private PreferenceScreen mSimCards = null;
    private SimHotSwapHandler mSimHotSwapHandler;
    private ISimManagementExt mSimManagementExt;
    private PhoneServiceStateHandler mStateHandler;
    private List<SubscriptionInfo> mSubInfoList = null;
    private SubscriptionManager mSubscriptionManager;
    private ITelephonyEx mTelephonyEx;

    class C05251 extends OnSubscriptionsChangedListener {
        C05251() {
        }

        public void onSubscriptionsChanged() {
            SimSettings.this.log("onSubscriptionsChanged:");
            SimSettings.this.updateSubscriptions();
        }
    }

    static class C05262 extends BaseSearchIndexProvider {
        C05262() {
        }

        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            ArrayList<SearchIndexableResource> result = new ArrayList();
            if (Utils.showSimCardTile(context)) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.sim_settings;
                result.add(sir);
            }
            return result;
        }
    }

    class C05273 extends BroadcastReceiver {
        C05273() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("SimSettings", "mReceiver action = " + action);
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                SimSettings.this.handleAirplaneModeChange(intent);
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                SimSettings.this.updateCellularDataValues();
            } else if (action.equals("android.telecom.action.DEFAULT_ACCOUNT_CHANGED") || action.equals("android.telecom.action.PHONE_ACCOUNT_CHANGED")) {
                SimSettings.this.updateCallValues();
            } else if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") || action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED")) {
                SimSettings.this.updateCellularDataValues();
            } else if (action.equals("android.intent.action.PHONE_STATE")) {
                SimSettings.this.updateCellularDataValues();
            }
        }
    }

    class C05284 implements OnSimHotSwapListener {
        C05284() {
        }

        public void onSimHotSwap() {
            if (SimSettings.this.getActivity() != null) {
                SimSettings.this.log("onSimHotSwap, finish Activity~~");
                SimSettings.this.getActivity().finish();
            }
        }
    }

    private class SimPreference extends RadioPowerPreference {
        Context mContext;
        private int mSlotId;
        private SubscriptionInfo mSubInfoRecord;

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);
            this.mContext = context;
            this.mSubInfoRecord = subInfoRecord;
            this.mSlotId = slotId;
            setKey("sim" + this.mSlotId);
            update();
        }

        public void update() {
            boolean z = false;
            Resources res = this.mContext.getResources();
            setTitle(String.format(this.mContext.getResources().getString(R.string.sim_editor_title), new Object[]{Integer.valueOf(this.mSlotId + 1)}));
            customizePreferenceTitle();
            if (this.mSubInfoRecord != null) {
                if (TextUtils.isEmpty(SimSettings.this.getPhoneNumber(this.mSubInfoRecord))) {
                    setSummary(this.mSubInfoRecord.getDisplayName());
                } else {
                    setSummary(this.mSubInfoRecord.getDisplayName() + " - " + SimSettings.this.getPhoneNumber(this.mSubInfoRecord));
                    setEnabled(true);
                }
                setIcon(new BitmapDrawable(res, this.mSubInfoRecord.createIconBitmap(this.mContext)));
                if (!SimSettings.this.mIsAirplaneModeOn) {
                    z = SimSettings.this.isRadioSwitchComplete(this.mSubInfoRecord.getSubscriptionId());
                }
                setRadioEnabled(z);
                setRadioOn(TelephonyUtils.isRadioOn(this.mSubInfoRecord.getSubscriptionId(), getContext()));
                return;
            }
            setSummary(R.string.sim_slot_empty);
            setFragment(null);
            setEnabled(false);
        }

        private int getSlotId() {
            return this.mSlotId;
        }

        private void customizePreferenceTitle() {
            int subId = -1;
            if (this.mSubInfoRecord != null) {
                subId = this.mSubInfoRecord.getSubscriptionId();
            }
            setTitle(String.format(SimSettings.this.mMiscExt.customizeSimDisplayString(this.mContext.getResources().getString(R.string.sim_editor_title), subId), new Object[]{Integer.valueOf(this.mSlotId + 1)}));
        }
    }

    public SimSettings() {
        super("no_config_sim");
    }

    protected int getMetricsCategory() {
        return 88;
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        addPreferencesFromResource(R.xml.sim_settings);
        this.mNumSlots = tm.getSimCount();
        this.mSimCards = (PreferenceScreen) findPreference("sim_cards");
        this.mAvailableSubInfos = new ArrayList(this.mNumSlots);
        this.mSelectableSubInfos = new ArrayList();
        SimSelectNotification.cancelNotification(getActivity());
        initForSimStateChange();
        this.mSimManagementExt = UtilsExt.getSimManagmentExtPlugin(getActivity());
        this.mMiscExt = UtilsExt.getMiscPlugin(getActivity());
    }

    private void updateSubscriptions() {
        int i;
        this.mSubInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        for (i = 0; i < this.mNumSlots; i++) {
            Preference pref = this.mSimCards.findPreference("sim" + i);
            if (pref instanceof SimPreference) {
                this.mSimCards.removePreference(pref);
            }
        }
        this.mAvailableSubInfos.clear();
        this.mSelectableSubInfos.clear();
        for (i = 0; i < this.mNumSlots; i++) {
            int i2;
            SubscriptionInfo sir = this.mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i);
            SimPreference simPreference = new SimPreference(this.mContext, sir, i);
            simPreference.setOrder(i - this.mNumSlots);
            if (sir == null) {
                i2 = -1;
            } else {
                i2 = sir.getSubscriptionId();
            }
            simPreference.bindRadioPowerState(i2);
            this.mSimCards.addPreference(simPreference);
            this.mAvailableSubInfos.add(sir);
            if (sir != null) {
                this.mSelectableSubInfos.add(sir);
            }
        }
        updateAllOptions();
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        int prefSize = this.mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; i++) {
            Preference pref = this.mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference) pref).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
        updateSimPref();
    }

    private void updateSmsValues() {
        boolean z = true;
        Preference simPref = findPreference("sim_sms");
        if (simPref != null) {
            SubscriptionInfo sir = this.mSubscriptionManager.getDefaultSmsSubscriptionInfo();
            simPref.setTitle(R.string.sms_messages_title);
            log("[updateSmsValues] mSubInfoList=" + this.mSubInfoList);
            sir = this.mSimManagementExt.setDefaultSubId(getActivity(), sir, "sim_sms");
            if (sir != null) {
                simPref.setSummary(sir.getDisplayName());
            } else if (sir == null) {
                simPref.setSummary(R.string.sim_calls_ask_first_prefs_title);
                this.mSimManagementExt.updateDefaultSmsSummary(simPref);
            }
            if (!shouldDisableActivitesCategory(getActivity())) {
                if (this.mSelectableSubInfos.size() < 1) {
                    z = false;
                }
                simPref.setEnabled(z);
                this.mSimManagementExt.configSimPreferenceScreen(simPref, "sim_sms", this.mSelectableSubInfos.size());
            }
        }
    }

    private void updateCellularDataValues() {
        Preference simPref = findPreference("sim_cellular_data");
        if (simPref != null) {
            SubscriptionInfo sir = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
            simPref.setTitle(R.string.cellular_data_title);
            log("[updateCellularDataValues] mSubInfoList=" + this.mSubInfoList);
            sir = this.mSimManagementExt.setDefaultSubId(getActivity(), sir, "sim_cellular_data");
            if (sir != null) {
                simPref.setSummary(sir.getDisplayName());
            } else if (sir == null) {
                simPref.setSummary(R.string.sim_selection_required_pref);
            }
            simPref.setEnabled(isDataPrefEnable());
            this.mSimManagementExt.configSimPreferenceScreen(simPref, "sim_cellular_data", -1);
        }
    }

    private void updateCallValues() {
        Preference simPref = findPreference("sim_calls");
        if (simPref != null) {
            CharSequence string;
            TelecomManager telecomManager = TelecomManager.from(this.mContext);
            PhoneAccountHandle phoneAccount = telecomManager.getUserSelectedOutgoingPhoneAccount();
            List<PhoneAccountHandle> allPhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
            phoneAccount = this.mSimManagementExt.setDefaultCallValue(phoneAccount);
            simPref.setTitle(R.string.calls_title);
            PhoneAccount defaultAccount = phoneAccount == null ? null : telecomManager.getPhoneAccount(phoneAccount);
            if (defaultAccount == null) {
                string = this.mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title);
            } else {
                String str = (String) defaultAccount.getLabel();
            }
            simPref.setSummary(string);
            if (!shouldDisableActivitesCategory(getActivity())) {
                simPref.setEnabled(allPhoneAccounts.size() > 1);
                this.mSimManagementExt.configSimPreferenceScreen(simPref, "sim_calls", allPhoneAccounts.size());
            }
        }
    }

    public void onResume() {
        super.onResume();
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        removeItemsForTablet();
        updateSubscriptions();
        customizeSimDisplay();
        this.mSimManagementExt.onResume(getActivity());
    }

    public void onPause() {
        super.onPause();
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        this.mSimManagementExt.onPause();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Context context = this.mContext;
        Intent intent = new Intent(context, SimDialogActivity.class);
        intent.addFlags(402653184);
        if (preference instanceof SimPreference) {
            Intent newIntent = new Intent(context, SimPreferenceDialog.class);
            newIntent.putExtra("slot_id", ((SimPreference) preference).getSlotId());
            startActivity(newIntent);
        } else if (findPreference("sim_cellular_data") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 0);
            context.startActivity(intent);
        } else if (findPreference("sim_calls") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 1);
            context.startActivity(intent);
        } else if (findPreference("sim_sms") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 2);
            context.startActivity(intent);
        }
        return true;
    }

    private String getPhoneNumber(SubscriptionInfo info) {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).getLine1NumberForSubscriber(info.getSubscriptionId());
    }

    private void log(String s) {
        Log.d("SimSettings", s);
    }

    public void onServiceStateChanged(ServiceState state, int subId) {
        Log.d("SimSettings", "PhoneStateListener:onServiceStateChanged: subId: " + subId + ", state: " + state);
        if (isRadioSwitchComplete(subId)) {
            handleRadioPowerSwitchComplete();
        }
        updateSimPref();
    }

    private void initForSimStateChange() {
        this.mTelephonyEx = Stub.asInterface(ServiceManager.getService("phoneEx"));
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C05284());
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity().getApplicationContext());
        Log.d("SimSettings", "init()... air plane mode is: " + this.mIsAirplaneModeOn);
        this.mStateHandler = new PhoneServiceStateHandler(getActivity().getApplicationContext());
        this.mStateHandler.registerOnPhoneServiceStateChange(this);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.telecom.action.DEFAULT_ACCOUNT_CHANGED");
        intentFilter.addAction("android.telecom.action.PHONE_ACCOUNT_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        getActivity().registerReceiver(this.mReceiver, intentFilter);
    }

    private boolean isRadioSwitchComplete(int subId) {
        if (getActivity() == null) {
            Log.d("SimSettings", "isRadioSwitchComplete()... activity is null");
            return false;
        }
        int slotId = SubscriptionManager.getSlotId(subId);
        boolean expectedRadioOn = ((1 << slotId) & System.getInt(getActivity().getContentResolver(), "msim_mode_setting", -1)) != 0;
        Log.d("SimSettings", "soltId: " + slotId + ", expectedRadioOn : " + expectedRadioOn);
        return (expectedRadioOn && TelephonyUtils.isRadioOn(subId, getActivity())) || !TelephonyUtils.isRadioOn(subId, this.mContext);
    }

    private void handleRadioPowerSwitchComplete() {
        if (isResumed()) {
            updateSimSlotValues();
        }
        this.mSimManagementExt.showChangeDataConnDialog(this, isResumed());
    }

    private void handleAirplaneModeChange(Intent intent) {
        this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.d("SimSettings", "air plane mode is = " + this.mIsAirplaneModeOn);
        updateSimSlotValues();
        updateCellularDataValues();
        updateSimPref();
        removeItemsForTablet();
    }

    private void removeItemsForTablet() {
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            Preference sim_call_Pref = findPreference("sim_calls");
            Preference sim_sms_Pref = findPreference("sim_sms");
            Preference sim_data_Pref = findPreference("sim_cellular_data");
            PreferenceCategory mPreferenceCategoryActivities = (PreferenceCategory) findPreference("sim_activities");
            TelephonyManager tm = TelephonyManager.from(getActivity());
            if (!(tm.isSmsCapable() || sim_sms_Pref == null)) {
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!(tm.isMultiSimEnabled() || sim_data_Pref == null || sim_sms_Pref == null)) {
                mPreferenceCategoryActivities.removePreference(sim_data_Pref);
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!tm.isVoiceCapable() && sim_call_Pref != null) {
                mPreferenceCategoryActivities.removePreference(sim_call_Pref);
            }
        }
    }

    public void onDestroy() {
        Log.d("SimSettings", "onDestroy()");
        getActivity().unregisterReceiver(this.mReceiver);
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        this.mStateHandler.unregisterOnPhoneServiceStateChange();
        super.onDestroy();
    }

    private void customizeSimDisplay() {
        if (this.mSimCards != null) {
            this.mSimCards.setTitle(this.mMiscExt.customizeSimDisplayString(getString(R.string.sim_settings_title), -1));
        }
        getActivity().setTitle(this.mMiscExt.customizeSimDisplayString(getString(R.string.sim_settings_title), -1));
    }

    private void updateSimPref() {
        boolean z = false;
        if (shouldDisableActivitesCategory(getActivity())) {
            Preference simCallsPref = findPreference("sim_calls");
            if (simCallsPref != null) {
                int accoutSum = TelecomManager.from(getActivity()).getCallCapablePhoneAccounts().size();
                Log.d("SimSettings", "accountSum: " + accoutSum);
                boolean z2 = (accoutSum <= 1 || TelephonyUtils.isCapabilitySwitching()) ? false : !this.mIsAirplaneModeOn;
                simCallsPref.setEnabled(z2);
            }
            Preference simSmsPref = findPreference("sim_sms");
            if (simSmsPref != null) {
                if (!(this.mSelectableSubInfos.size() <= 1 || TelephonyUtils.isCapabilitySwitching() || this.mIsAirplaneModeOn)) {
                    z = true;
                }
                simSmsPref.setEnabled(z);
            }
        }
    }

    private boolean isDataPrefEnable() {
        boolean ecbMode = SystemProperties.getBoolean("ril.cdma.inecmmode", false);
        log("isEcbMode()... isEcbMode: " + ecbMode);
        if (this.mSelectableSubInfos.size() < 1 || TelephonyUtils.isCapabilitySwitching() || this.mIsAirplaneModeOn || TelecomManager.from(this.mContext).isInCall() || ecbMode) {
            return false;
        }
        return true;
    }

    private static boolean shouldDisableActivitesCategory(Context context) {
        boolean shouldDisable;
        if (CdmaUtils.isCdmaCardCompetion(context)) {
            shouldDisable = true;
        } else {
            shouldDisable = CdmaUtils.isCdamCardAndGsmCard(context);
        }
        Log.d("SimSettings", "shouldDisableActivitesCategory() .. shouldDisable :" + shouldDisable);
        return shouldDisable;
    }
}
