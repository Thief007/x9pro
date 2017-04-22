package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.InstrumentedPreferenceActivity;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaSimStatus;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import java.util.List;

public class SimStatus extends InstrumentedPreferenceActivity {
    private BroadcastReceiver mAreaInfoReceiver = new C03341();
    private CdmaSimStatus mCdmaSimStatus;
    private String mDefaultText;
    private TabContentFactory mEmptyTabContent = new C03363();
    private ListView mListView;
    private Phone mPhone = null;
    private PhoneStateListener mPhoneStateListener;
    private Resources mRes;
    private List<SubscriptionInfo> mSelectableSubInfos;
    private boolean mShowLatestAreaInfo;
    private Preference mSignalStrength;
    private SimHotSwapHandler mSimHotSwapHandler;
    private SubscriptionInfo mSir;
    private TabHost mTabHost;
    private OnTabChangeListener mTabListener = new C03352();
    private TabWidget mTabWidget;
    private TelephonyManager mTelephonyManager;

    class C03341 extends BroadcastReceiver {
        C03341() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED".equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                    if (cbMessage != null && cbMessage.getServiceCategory() == 50 && SimStatus.this.mSir.getSubscriptionId() == cbMessage.getSubId()) {
                        SimStatus.this.updateAreaInfo(cbMessage.getMessageBody());
                    }
                }
            }
        }
    }

    class C03352 implements OnTabChangeListener {
        C03352() {
        }

        public void onTabChanged(String tabId) {
            SimStatus.this.mSir = (SubscriptionInfo) SimStatus.this.mSelectableSubInfos.get(Integer.parseInt(tabId));
            SimStatus.this.mCdmaSimStatus.setSubscriptionInfo(SimStatus.this.mSir);
            SimStatus.this.updatePhoneInfos();
            SimStatus.this.mTelephonyManager.listen(SimStatus.this.mPhoneStateListener, 321);
            SimStatus.this.updateDataState();
            SimStatus.this.updateNetworkType();
            SimStatus.this.updatePreference();
        }
    }

    class C03363 implements TabContentFactory {
        C03363() {
        }

        public View createTabContent(String tag) {
            return new View(SimStatus.this.mTabHost.getContext());
        }
    }

    class C03374 implements OnSimHotSwapListener {
        C03374() {
        }

        public void onSimHotSwap() {
            Log.d("SimStatus", "onSimHotSwap, finish Activity~~");
            SimStatus.this.finish();
        }
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mSelectableSubInfos = SubscriptionManager.from(this).getActiveSubscriptionInfoList();
        addPreferencesFromResource(R.xml.device_info_sim_status);
        this.mRes = getResources();
        this.mDefaultText = this.mRes.getString(R.string.device_info_default);
        this.mSignalStrength = findPreference("signal_strength");
        this.mCdmaSimStatus = new CdmaSimStatus(this, null);
        if (this.mSelectableSubInfos == null) {
            this.mSir = null;
            this.mCdmaSimStatus.setSubscriptionInfo(this.mSir);
        } else {
            SubscriptionInfo subscriptionInfo;
            if (this.mSelectableSubInfos.size() > 0) {
                subscriptionInfo = (SubscriptionInfo) this.mSelectableSubInfos.get(0);
            } else {
                subscriptionInfo = null;
            }
            this.mSir = subscriptionInfo;
            this.mCdmaSimStatus.setSubscriptionInfo(this.mSir);
            if (this.mSelectableSubInfos.size() > 1) {
                setContentView(17367108);
                this.mTabHost = (TabHost) findViewById(16908306);
                this.mTabWidget = (TabWidget) findViewById(16908307);
                this.mListView = (ListView) findViewById(16908298);
                this.mTabHost.setup();
                this.mTabHost.setOnTabChangedListener(this.mTabListener);
                this.mTabHost.clearAllTabs();
                for (int i = 0; i < this.mSelectableSubInfos.size(); i++) {
                    this.mTabHost.addTab(buildTabSpec(String.valueOf(i), String.valueOf(((SubscriptionInfo) this.mSelectableSubInfos.get(i)).getDisplayName())));
                }
            }
        }
        updatePhoneInfos();
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C03374());
        customizeTitle();
    }

    protected int getMetricsCategory() {
        return 43;
    }

    protected void onResume() {
        super.onResume();
        if (this.mPhone != null) {
            updatePreference();
            updateSignalStrength(this.mPhone.getSignalStrength());
            updateServiceState(this.mPhone.getServiceState());
            updateDataState();
            this.mTelephonyManager.listen(this.mPhoneStateListener, 321);
            if (this.mShowLatestAreaInfo) {
                registerReceiver(this.mAreaInfoReceiver, new IntentFilter("android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED"), "android.permission.RECEIVE_EMERGENCY_BROADCAST", null);
                sendBroadcastAsUser(new Intent("android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO"), UserHandle.ALL, "android.permission.RECEIVE_EMERGENCY_BROADCAST");
            }
        }
    }

    public void onPause() {
        super.onPause();
        if (this.mPhone != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        if (this.mShowLatestAreaInfo) {
            unregisterReceiver(this.mAreaInfoReceiver);
        }
    }

    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setSummaryText(String key, String text) {
        if (TextUtils.isEmpty(text)) {
            text = this.mDefaultText;
        }
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(text);
        }
    }

    private void updateNetworkType() {
        String networktype = null;
        int subId = this.mSir.getSubscriptionId();
        int actualDataNetworkType = this.mTelephonyManager.getDataNetworkType(this.mSir.getSubscriptionId());
        int actualVoiceNetworkType = this.mTelephonyManager.getVoiceNetworkType(this.mSir.getSubscriptionId());
        Log.d("SimStatus", "updateNetworkType(), actualDataNetworkType = " + actualDataNetworkType + "actualVoiceNetworkType = " + actualVoiceNetworkType);
        TelephonyManager telephonyManager;
        if (actualDataNetworkType != 0) {
            telephonyManager = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (actualVoiceNetworkType != 0) {
            telephonyManager = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }
        boolean show4GForLTE = false;
        try {
            Context con = createPackageContext("com.android.systemui", 0);
            show4GForLTE = con.getResources().getBoolean(con.getResources().getIdentifier("config_show4GForLTE", "bool", "com.android.systemui"));
        } catch (NameNotFoundException e) {
            Log.e("SimStatus", "NameNotFoundException for show4GFotLTE");
        }
        if (networktype != null && networktype.equals("LTE") && r6) {
            networktype = "4G";
        }
        setSummaryText("network_type", networktype);
        this.mCdmaSimStatus.updateNetworkType("network_type", networktype);
    }

    private void updateDataState() {
        int state = DefaultPhoneNotifier.convertDataState(this.mPhone.getDataConnectionState());
        String display = this.mRes.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = this.mRes.getString(R.string.radioInfo_data_disconnected);
                break;
            case 1:
                display = this.mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case 2:
                display = this.mRes.getString(R.string.radioInfo_data_connected);
                break;
            case 3:
                display = this.mRes.getString(R.string.radioInfo_data_suspended);
                break;
        }
        setSummaryText("data_state", display);
    }

    private void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = this.mRes.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = this.mRes.getString(R.string.radioInfo_service_in);
                break;
            case 1:
                this.mSignalStrength.setSummary("0");
                break;
            case 2:
                break;
            case 3:
                display = this.mRes.getString(R.string.radioInfo_service_off);
                this.mSignalStrength.setSummary("0");
                break;
        }
        display = this.mRes.getString(R.string.radioInfo_service_out);
        setSummaryText("service_state", display);
        if (serviceState.getRoaming()) {
            setSummaryText("roaming_state", this.mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText("roaming_state", this.mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText("operator_name", serviceState.getOperatorAlphaLong());
        this.mCdmaSimStatus.setServiceState(serviceState);
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText("latest_area_info", areaInfo);
        }
    }

    void updateSignalStrength(SignalStrength signalStrength) {
        if (this.mSignalStrength != null) {
            int state = this.mPhone.getServiceState().getState();
            Resources r = getResources();
            if (1 == state || 3 == state) {
                this.mSignalStrength.setSummary("0");
                return;
            }
            int signalDbm = signalStrength.getDbm();
            int signalAsu = signalStrength.getAsuLevel();
            if (-1 == signalDbm) {
                signalDbm = 0;
            }
            if (-1 == signalAsu) {
                signalAsu = 0;
            }
            Log.d("SimStatus", "updateSignalStrength(), signalDbm = " + signalDbm + " signalAsu = " + signalAsu);
            this.mSignalStrength.setSummary(r.getString(R.string.sim_signal_strength, new Object[]{Integer.valueOf(signalDbm), Integer.valueOf(signalAsu)}));
            this.mCdmaSimStatus.updateSignalStrength(signalStrength, this.mSignalStrength);
        }
    }

    private void updatePreference() {
        String deviceId;
        if (this.mPhone.getPhoneType() != 2 && "br".equals(this.mTelephonyManager.getSimCountryIso(this.mSir.getSubscriptionId()))) {
            this.mShowLatestAreaInfo = true;
        }
        String rawNumber = this.mTelephonyManager.getLine1NumberForSubscriber(this.mSir.getSubscriptionId());
        String formattedNumber = null;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        }
        setSummaryText("number", formattedNumber);
        if (this.mPhone.getPhoneType() != 2) {
            deviceId = this.mPhone.getImei();
        } else {
            deviceId = this.mPhone.getMeid();
        }
        setSummaryText("imei", deviceId);
        setSummaryText("imei_sv", this.mPhone.getDeviceSvn());
        Preference preference = findPreference("imei");
        if (preference != null) {
            int titleResId;
            if (this.mPhone.getPhoneType() == 2) {
                titleResId = R.string.status_meid_number;
            } else {
                titleResId = R.string.status_imei;
            }
            preference.setTitle(titleResId);
        }
        if (!this.mShowLatestAreaInfo) {
            removePreferenceFromScreen("latest_area_info");
        }
        this.mCdmaSimStatus.updateCdmaPreference(this, this.mSir);
    }

    private void updatePhoneInfos() {
        if (this.mSir != null) {
            Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(this.mSir.getSubscriptionId()));
            if (UserHandle.myUserId() == 0 && SubscriptionManager.isValidSubscriptionId(this.mSir.getSubscriptionId())) {
                if (phone == null) {
                    Log.e("SimStatus", "Unable to locate a phone object for the given Subscription ID.");
                    return;
                }
                this.mPhone = phone;
                this.mCdmaSimStatus.setPhoneInfos(this.mPhone);
                if (this.mPhoneStateListener != null) {
                    Log.d("SimStatus", "remove the phone state listener mPhoneStateListener = " + this.mPhoneStateListener);
                    this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
                }
                this.mPhoneStateListener = new PhoneStateListener(this.mSir.getSubscriptionId()) {
                    public void onDataConnectionStateChanged(int state) {
                        SimStatus.this.updateDataState();
                        SimStatus.this.updateNetworkType();
                    }

                    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                        SimStatus.this.updateSignalStrength(signalStrength);
                    }

                    public void onServiceStateChanged(ServiceState serviceState) {
                        Log.d("SimStatus", "onServiceStateChanged subId = " + SimStatus.this.mSir.getSubscriptionId());
                        SimStatus.this.updateServiceState(serviceState);
                    }
                };
            }
        }
    }

    private TabSpec buildTabSpec(String tag, String title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }

    protected void onDestroy() {
        super.onDestroy();
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
    }

    private void customizeTitle() {
        String title = getTitle().toString();
        Log.d("SimStatus", "title = " + title);
        if (title.equals(getString(R.string.sim_status_title))) {
            title = UtilsExt.getMiscPlugin(this).customizeSimDisplayString(getTitle().toString(), -1);
            Log.d("SimStatus", "title = " + title);
            setTitle(title);
        }
    }
}
