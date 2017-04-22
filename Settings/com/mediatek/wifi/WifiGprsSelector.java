package com.mediatek.wifi;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.provider.Telephony.Carriers;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settings.ApnPreference;
import com.android.settings.R;
import com.android.settings.wifi.WifiSettings;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.internal.telephony.CellConnMgr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiGprsSelector extends WifiSettings implements OnPreferenceChangeListener {
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static final String[] PROJECTION_ARRAY = new String[]{"_id", "name", "apn", "type", "sourcetype"};
    private Preference mAddWifiNetwork;
    private boolean mAirplaneModeEnabled = false;
    private PreferenceCategory mApnList;
    private CellConnMgr mCellConnMgr;
    private CheckBoxPreference mDataEnabler;
    private Preference mDataEnablerGemini;
    ContentObserver mGprsConnectObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Log.i("@M_WifiGprsSelector", "Gprs connection changed");
            WifiGprsSelector.this.mSubId = WifiGprsSelector.this.getSubId();
            WifiGprsSelector.this.updateDataEnabler();
        }
    };
    private int mInitValue;
    private boolean mIsCallStateIdle = true;
    private boolean mIsGprsSwitching = false;
    private boolean mIsSIMExist = true;
    private IntentFilter mMobileStateFilter;
    private final BroadcastReceiver mMobileStateReceiver = new C07521();
    private PhoneStateListener mPhoneStateListener = new C07543();
    private Uri mRestoreCarrierUri;
    private boolean mScreenEnable = true;
    private int mSelectedDataSubId = -1;
    private String mSelectedKey;
    private Runnable mServiceComplete = new C07554();
    private Map<Integer, SubscriptionInfo> mSimMap;
    private List<Integer> mSimMapKeyList = null;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    Handler mTimeHandler = new C07565();
    private Uri mUri;
    private WifiManager mWifiManager;

    class C07521 extends BroadcastReceiver {
        C07521() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean z = false;
            String action = intent.getAction();
            if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                Log.d("@M_WifiGprsSelector", "changed default data subId: " + intent.getLongExtra("subscription", -1));
                WifiGprsSelector.this.mTimeHandler.removeMessages(2001);
                if (WifiGprsSelector.this.isResumed()) {
                    WifiGprsSelector.this.removeDialog(1001);
                }
                WifiGprsSelector.this.mIsGprsSwitching = false;
                WifiGprsSelector.this.updateDataEnabler();
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                WifiGprsSelector.this.mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                Log.d("@M_WifiGprsSelector", "AIRPLANE_MODE state changed: " + WifiGprsSelector.this.mAirplaneModeEnabled + ";");
                PreferenceCategory -get1 = WifiGprsSelector.this.mApnList;
                if (!WifiGprsSelector.this.mAirplaneModeEnabled) {
                    z = true;
                }
                -get1.setEnabled(z);
                WifiGprsSelector.this.updateDataEnabler();
            } else if (action.equals("com.android.mms.transaction.START")) {
                Log.d("@M_WifiGprsSelector", "ssr: TRANSACTION_START in ApnSettings;");
                WifiGprsSelector.this.mScreenEnable = false;
                r5 = WifiGprsSelector.this.mApnList;
                if (!WifiGprsSelector.this.mAirplaneModeEnabled) {
                    z = WifiGprsSelector.this.mScreenEnable;
                }
                r5.setEnabled(z);
            } else if (action.equals("com.android.mms.transaction.STOP")) {
                Log.d("@M_WifiGprsSelector", "ssr: TRANSACTION_STOP in ApnSettings;");
                WifiGprsSelector.this.mScreenEnable = true;
                r5 = WifiGprsSelector.this.mApnList;
                if (!WifiGprsSelector.this.mAirplaneModeEnabled) {
                    z = WifiGprsSelector.this.mScreenEnable;
                }
                r5.setEnabled(z);
            } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                WifiGprsSelector.this.handleWifiStateChanged(intent.getIntExtra("wifi_state", 4));
            } else if ("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE".equals(action)) {
                Log.d("@M_WifiGprsSelector", "receive ACTION_SIM_INFO_UPDATE");
                if (SubscriptionManager.from(WifiGprsSelector.this.getActivity()).getActiveSubscriptionInfoList() != null) {
                    WifiGprsSelector.this.mSubId = WifiGprsSelector.this.getSubId();
                    WifiGprsSelector.this.updateDataEnabler();
                }
            }
        }
    }

    class C07543 extends PhoneStateListener {
        C07543() {
        }

        public void onServiceStateChanged(ServiceState serviceState) {
            boolean z = false;
            super.onServiceStateChanged(serviceState);
            WifiGprsSelector wifiGprsSelector = WifiGprsSelector.this;
            if (WifiGprsSelector.this.mTelephonyManager.getCallState() == 0) {
                z = true;
            }
            wifiGprsSelector.mIsCallStateIdle = z;
        }
    }

    class C07554 implements Runnable {
        C07554() {
        }

        public void run() {
        }
    }

    class C07565 extends Handler {
        C07565() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2000:
                    Log.d("@M_WifiGprsSelector", "detach time out......");
                    if (WifiGprsSelector.this.isResumed()) {
                        WifiGprsSelector.this.removeDialog(1001);
                    }
                    WifiGprsSelector.this.mIsGprsSwitching = false;
                    WifiGprsSelector.this.updateDataEnabler();
                    return;
                case 2001:
                    Log.d("@M_WifiGprsSelector", "attach time out......");
                    if (WifiGprsSelector.this.isResumed()) {
                        WifiGprsSelector.this.removeDialog(1001);
                    }
                    WifiGprsSelector.this.mIsGprsSwitching = false;
                    WifiGprsSelector.this.updateDataEnabler();
                    return;
                default:
                    return;
            }
        }
    }

    class C07587 implements OnClickListener {
        C07587() {
        }

        public void onClick(DialogInterface dialog, int which) {
        }
    }

    class C07598 implements OnClickListener {
        C07598() {
        }

        public void onClick(DialogInterface dialog, int which) {
            WifiGprsSelector.this.mCellConnMgr.handleRequest(WifiGprsSelector.this.mSelectedDataSubId, 4);
        }
    }

    class C07609 implements OnClickListener {
        C07609() {
        }

        public void onClick(DialogInterface dialog, int which) {
        }
    }

    class SelectionListAdapter extends BaseAdapter {
        List<SimItem> mSimItemList;

        class ViewHolder {
            RadioButton mCkRadioOn;
            RelativeLayout mImageSim;
            ImageView mImageStatus;
            TextView mTextName;
            TextView mTextNum;
            TextView mTextNumFormat;

            ViewHolder() {
            }
        }

        public SelectionListAdapter(List<SimItem> simItemList) {
            this.mSimItemList = simItemList;
        }

        public int getCount() {
            return this.mSimItemList.size();
        }

        public Object getItem(int position) {
            return this.mSimItemList.get(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            boolean z;
            if (convertView == null) {
                convertView = LayoutInflater.from(WifiGprsSelector.this.getActivity()).inflate(R.layout.preference_sim_default_select, null);
                holder = new ViewHolder();
                setViewHolderId(holder, convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            SimItem simItem = (SimItem) getItem(position);
            setNameAndNum(holder.mTextName, holder.mTextNum, simItem);
            setImageSim(holder.mImageSim, simItem);
            setImageStatus(holder.mImageStatus, simItem);
            setTextNumFormat(holder.mTextNumFormat, simItem);
            RadioButton radioButton = holder.mCkRadioOn;
            if (WifiGprsSelector.this.mInitValue == position) {
                z = true;
            } else {
                z = false;
            }
            radioButton.setChecked(z);
            if (simItem.mState == 1) {
                convertView.setEnabled(false);
                holder.mTextName.setEnabled(false);
                holder.mTextNum.setEnabled(false);
                holder.mCkRadioOn.setEnabled(false);
            } else {
                convertView.setEnabled(true);
                holder.mTextName.setEnabled(true);
                holder.mTextNum.setEnabled(true);
                holder.mCkRadioOn.setEnabled(true);
            }
            return convertView;
        }

        private void setTextNumFormat(TextView textNumFormat, SimItem simItem) {
            if (simItem.mIsSim && simItem.mNumber != null) {
                switch (simItem.mDispalyNumberFormat) {
                    case 0:
                        textNumFormat.setVisibility(8);
                        return;
                    case 1:
                        textNumFormat.setVisibility(0);
                        if (simItem.mNumber.length() >= 4) {
                            textNumFormat.setText(simItem.mNumber.substring(0, 4));
                            return;
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                            return;
                        }
                    case 2:
                        textNumFormat.setVisibility(0);
                        if (simItem.mNumber.length() >= 4) {
                            textNumFormat.setText(simItem.mNumber.substring(simItem.mNumber.length() - 4));
                            return;
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                            return;
                        }
                    default:
                        return;
                }
            }
        }

        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            if (simItem.mIsSim) {
                int res = WifiGprsSelector.this.getStatusResource(simItem.mState);
                if (res == -1) {
                    imageStatus.setVisibility(8);
                    return;
                }
                imageStatus.setVisibility(0);
                imageStatus.setImageResource(res);
            }
        }

        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if (simItem.mIsSim) {
                Bitmap resColor = simItem.mSimIconBitmap;
                if (resColor != null) {
                    Drawable drawable = new BitmapDrawable(resColor);
                    imageSim.setVisibility(0);
                    imageSim.setBackground(drawable);
                }
            } else if (simItem.mColor == 8) {
                imageSim.setVisibility(0);
                imageSim.setBackgroundResource(134349031);
            } else {
                imageSim.setVisibility(8);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.mTextName = (TextView) convertView.findViewById(R.id.simNameSel);
            holder.mTextNum = (TextView) convertView.findViewById(R.id.simNumSel);
            holder.mImageStatus = (ImageView) convertView.findViewById(R.id.simStatusSel);
            holder.mTextNumFormat = (TextView) convertView.findViewById(R.id.simNumFormatSel);
            holder.mCkRadioOn = (RadioButton) convertView.findViewById(R.id.Enable_select);
            holder.mImageSim = (RelativeLayout) convertView.findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName, TextView textNum, SimItem simItem) {
            if (simItem.mName == null) {
                textName.setVisibility(8);
            } else {
                textName.setVisibility(0);
                textName.setText(simItem.mName);
            }
            if (!simItem.mIsSim || simItem.mNumber == null || simItem.mNumber.length() == 0) {
                textNum.setVisibility(8);
                return;
            }
            textNum.setVisibility(0);
            textNum.setText(simItem.mNumber);
        }
    }

    static class SimItem {
        public int mColor;
        public int mDispalyNumberFormat;
        public boolean mIsSim;
        public String mName;
        public String mNumber;
        public Bitmap mSimIconBitmap;
        public int mSlot;
        public int mState;
        public int mSubId;
        private WifiGprsSelector mWifiGprsSeletor;

        public SimItem(SubscriptionInfo subinfo, WifiGprsSelector wifiGprsSelector) {
            this.mIsSim = true;
            this.mName = null;
            this.mNumber = null;
            this.mDispalyNumberFormat = 0;
            this.mColor = -1;
            this.mSlot = -1;
            this.mState = 5;
            this.mSubId = -1;
            this.mSimIconBitmap = null;
            this.mWifiGprsSeletor = null;
            this.mIsSim = true;
            this.mName = subinfo.getDisplayName().toString();
            this.mNumber = subinfo.getNumber();
            this.mColor = subinfo.getIconTint();
            this.mSlot = subinfo.getSimSlotIndex();
            this.mSubId = subinfo.getSubscriptionId();
            this.mWifiGprsSeletor = wifiGprsSelector;
            this.mSimIconBitmap = subinfo.createIconBitmap(wifiGprsSelector.getActivity());
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("@M_WifiGprsSelector", "onActivityCreated()");
        addPreferencesFromResource(R.xml.wifi_access_points_and_gprs);
        this.mApnList = (PreferenceCategory) findPreference("apn_list");
        this.mAddWifiNetwork = findPreference("add_network");
        PreferenceCategory dataEnableCategory = (PreferenceCategory) findPreference("data_enabler_category");
        if (isGeminiSupport()) {
            this.mDataEnablerGemini = findPreference("data_enabler_gemini");
            if (dataEnableCategory != null) {
                dataEnableCategory.removePreference(findPreference("data_enabler"));
            }
        } else {
            this.mDataEnabler = (CheckBoxPreference) findPreference("data_enabler");
            this.mDataEnabler.setOnPreferenceChangeListener(this);
            if (dataEnableCategory != null) {
                dataEnableCategory.removePreference(findPreference("data_enabler_gemini"));
            }
        }
        initPhoneState();
        this.mMobileStateFilter = new IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mMobileStateFilter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mMobileStateFilter.addAction("com.android.mms.transaction.START");
        this.mMobileStateFilter.addAction("com.android.mms.transaction.STOP");
        this.mMobileStateFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mMobileStateFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mMobileStateFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        getActivity().setTitle(R.string.wifi_gprs_selector_title);
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        init();
        setHasOptionsMenu(false);
    }

    public void onResume() {
        boolean z = true;
        Log.d("@M_WifiGprsSelector", "onResume");
        super.onResume();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
        getActivity().registerReceiver(this.mMobileStateReceiver, this.mMobileStateFilter);
        if (Global.getInt(getActivity().getContentResolver(), "airplane_mode_on", 0) == 0) {
            z = false;
        }
        this.mAirplaneModeEnabled = z;
        this.mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService("wifi");
        handleWifiStateChanged(this.mWifiManager.getWifiState());
        this.mScreenEnable = isMMSNotTransaction();
        fillList(this.mSubId);
        updateDataEnabler();
        if (isGeminiSupport()) {
            this.mCellConnMgr = new CellConnMgr(getActivity());
            getContentResolver().registerContentObserver(System.getUriFor("gprs_connection_sim_setting"), false, this.mGprsConnectObserver);
        }
        if (this.mIsGprsSwitching) {
            showDialog(1001);
        }
    }

    private boolean isMMSNotTransaction() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        if (cm == null) {
            return true;
        }
        NetworkInfo networkInfo = cm.getNetworkInfo(2);
        if (networkInfo == null) {
            return true;
        }
        State state = networkInfo.getState();
        Log.d("@M_WifiGprsSelector", "mms state = " + state);
        if (state != State.CONNECTING) {
            return state != State.CONNECTED;
        } else {
            return false;
        }
    }

    private boolean init() {
        Log.d("@M_WifiGprsSelector", "init()");
        this.mIsSIMExist = this.mTelephonyManager.hasIccCard();
        return true;
    }

    public void onPause() {
        Log.d("@M_WifiGprsSelector", "onPause");
        super.onPause();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        getActivity().unregisterReceiver(this.mMobileStateReceiver);
        if (isGeminiSupport()) {
            getContentResolver().unregisterContentObserver(this.mGprsConnectObserver);
        }
        if (this.mIsGprsSwitching) {
            removeDialog(1001);
        }
    }

    public void onDestroy() {
        this.mTimeHandler.removeMessages(2001);
        this.mTimeHandler.removeMessages(2000);
        super.onDestroy();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    private void initPhoneState() {
        Log.d("@M_WifiGprsSelector", "initPhoneState()");
        this.mSubId = getActivity().getIntent().getIntExtra("simId", -1);
        this.mSimMap = new HashMap();
        initSimMap();
        if (this.mSubId == -1) {
            this.mSubId = getSubId();
        }
        Log.d("@M_WifiGprsSelector", "GEMINI_SIM_ID_KEY = " + this.mSubId + ";");
    }

    private void fillList(int subId) {
        this.mApnList.removeAll();
        if (subId >= 0 && subId <= 2) {
            Log.d("@M_WifiGprsSelector", "fillList(), subId=" + subId + ";");
            String where = ("numeric=\"" + getQueryWhere(subId) + "\"") + " AND NOT (type='ia' AND (apn='' OR apn IS NULL))";
            if (!SystemProperties.get("ro.mtk_volte_support").equals("1")) {
                where = where + " AND NOT type='ims'";
            }
            Log.d("@M_WifiGprsSelector", "where = " + where + ";");
            Cursor cursor = getActivity().managedQuery(this.mUri, PROJECTION_ARRAY, where, "name ASC");
            ArrayList<Preference> mmsApnList = new ArrayList();
            boolean keySetChecked = false;
            this.mSelectedKey = getSelectedApnKey();
            Log.d("@M_WifiGprsSelector", "mSelectedKey = " + this.mSelectedKey + ";");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(1);
                String apn = cursor.getString(2);
                String key = cursor.getString(0);
                String type = cursor.getString(3);
                int sourcetype = cursor.getInt(4);
                ApnPreference pref = new ApnPreference(getActivity());
                pref.setSubId(subId);
                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                boolean selectable = type != null ? (type.equals("mms") || type.equals("cmmail")) ? false : !type.equals("ims") : true;
                pref.setSelectable(selectable);
                if (selectable) {
                    if (this.mSelectedKey != null && this.mSelectedKey.equals(key)) {
                        setSelectedApnKey(key);
                        pref.setChecked();
                        keySetChecked = true;
                        Log.d("@M_WifiGprsSelector", "apn key: " + key + " set." + ";");
                    }
                    Log.d("@M_WifiGprsSelector", "key:  " + key + " added!" + ";");
                    this.mApnList.addPreference(pref);
                    if (isGeminiSupport()) {
                        pref.setDependency("data_enabler_gemini");
                    } else {
                        pref.setDependency("data_enabler");
                    }
                } else {
                    mmsApnList.add(pref);
                }
                cursor.moveToNext();
            }
            int mSelectableApnCount = this.mApnList.getPreferenceCount();
            if (!keySetChecked && mSelectableApnCount > 0) {
                ApnPreference apnPref = (ApnPreference) this.mApnList.getPreference(0);
                if (apnPref != null) {
                    setSelectedApnKey(apnPref.getKey());
                    apnPref.setChecked();
                    Log.d("@M_WifiGprsSelector", "Key does not match.Set key: " + apnPref.getKey() + ".");
                }
            }
            this.mIsCallStateIdle = this.mTelephonyManager.getCallState() == 0;
            boolean z = 5 == this.mTelephonyManager.getSimState(SubscriptionManager.getSlotId(subId));
            PreferenceCategory preferenceCategory = this.mApnList;
            if (!(this.mScreenEnable && this.mIsCallStateIdle && !this.mAirplaneModeEnabled)) {
                z = false;
            }
            preferenceCategory.setEnabled(z);
        }
    }

    private String getQueryWhere(int subId) {
        String where = "";
        where = TelephonyManager.getDefault().getSimOperator(subId);
        this.mUri = Carriers.CONTENT_URI.buildUpon().appendPath("subId").appendPath(Integer.toString(subId)).build();
        this.mRestoreCarrierUri = PREFERAPN_URI.buildUpon().appendPath("subId").appendPath(Integer.toString(subId)).build();
        Log.d("@M_WifiGprsSelector", "where = " + where + ";");
        Log.d("@M_WifiGprsSelector", "mUri = " + this.mUri + ";");
        return where;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("@M_WifiGprsSelector", "onPreferenceChange(): Preference - " + preference + ", newValue - " + newValue + ", newValue type - " + newValue.getClass());
        if ("data_enabler".equals(preference == null ? "" : preference.getKey())) {
            boolean checked = ((Boolean) newValue).booleanValue();
            Log.d("@M_WifiGprsSelector", "Data connection enabled?" + checked);
            dealWithConnChange(checked);
        } else if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        String key = preference.getKey();
        if ("add_network".equals(key)) {
            if (this.mWifiManager.isWifiEnabled()) {
                Log.d("@M_WifiGprsSelector", "add network");
                super.addNetworkForSelector();
            }
        } else if (!"data_enabler_gemini".equals(key)) {
            return super.onPreferenceTreeClick(screen, preference);
        } else {
            final List<SimItem> simItemList = new ArrayList();
            for (Integer simid : this.mSimMapKeyList) {
                SubscriptionInfo subinfo = (SubscriptionInfo) this.mSimMap.get(simid);
                if (subinfo != null) {
                    SimItem simitem = new SimItem(subinfo, this);
                    simitem.mState = this.mTelephonyManager.getSimState(subinfo.getSimSlotIndex());
                    simItemList.add(simitem);
                }
            }
            int simListSize = simItemList.size();
            Log.d("@M_WifiGprsSelector", "simListSize = " + simListSize);
            int offItem = simListSize - 1;
            int index = -1;
            int dataConnectId = SubscriptionManager.getDefaultDataSubId();
            Log.d("@M_WifiGprsSelector", "getSimSlot,dataConnectId = " + dataConnectId);
            for (int i = 0; i < offItem; i++) {
                if (((SimItem) simItemList.get(i)).mSubId == dataConnectId) {
                    index = i;
                }
            }
            if (index != -1) {
                offItem = index;
            }
            this.mInitValue = offItem;
            Log.d("@M_WifiGprsSelector", "mInitValue = " + this.mInitValue);
            SelectionListAdapter mAdapter = new SelectionListAdapter(simItemList);
            new Builder(getActivity()).setSingleChoiceItems(mAdapter, this.mInitValue, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("@M_WifiGprsSelector", "which = " + which);
                    SimItem simItem = (SimItem) simItemList.get(which);
                    WifiGprsSelector.this.mSubId = simItem.mSubId;
                    Log.d("@M_WifiGprsSelector", "mSubId = " + WifiGprsSelector.this.mSubId);
                    Log.d("@M_WifiGprsSelector", "mIsSim=" + simItem.mIsSim + ",mState=" + simItem.mState + ",SIM_INDICATOR_LOCKED=" + 1);
                    if (simItem.mIsSim) {
                        int state = WifiGprsSelector.this.mCellConnMgr.getCurrentState(simItem.mSubId, 4);
                        if (WifiGprsSelector.this.mCellConnMgr == null || state != 4) {
                            WifiGprsSelector.this.switchGprsDefautlSIM(simItem.mSubId);
                        } else {
                            Log.d("@M_WifiGprsSelector", "mCellConnMgr.handleCellConn");
                            WifiGprsSelector.this.showDialog(1002);
                        }
                    } else {
                        WifiGprsSelector.this.switchGprsDefautlSIM(0);
                    }
                    dialog.dismiss();
                }
            }).setTitle(R.string.data_conn_category_title).setNegativeButton(17039369, new C07587()).create().show();
        }
        return true;
    }

    private int getSubId() {
        return SubscriptionManager.getDefaultDataSubId();
    }

    private void handleWifiStateChanged(int state) {
        Log.d("@M_WifiGprsSelector", "handleWifiStateChanged(), new state=" + state + ";");
        Log.d("@M_WifiGprsSelector", "[0- stoping 1-stoped 2-starting 3-started 4-unknown]");
        if (state == 3) {
            this.mAddWifiNetwork.setEnabled(true);
        } else {
            this.mAddWifiNetwork.setEnabled(false);
        }
    }

    private void setSelectedApnKey(String key) {
        this.mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put("apn_id", this.mSelectedKey);
        resolver.update(this.mRestoreCarrierUri, values, null, null);
    }

    private String getSelectedApnKey() {
        Cursor cursor = getActivity().managedQuery(this.mRestoreCarrierUri, new String[]{"_id"}, null, "name ASC");
        if (cursor.getCount() <= 0) {
            return null;
        }
        cursor.moveToFirst();
        return cursor.getString(0);
    }

    private void updateDataEnabler() {
        boolean z = false;
        if (isGeminiSupport()) {
            Log.d("@M_WifiGprsSelector", "updateDataEnabler, mSubId=" + this.mSubId);
            fillList(this.mSubId);
            Preference preference = this.mDataEnablerGemini;
            if (this.mIsSIMExist && !this.mAirplaneModeEnabled) {
                z = true;
            }
            preference.setEnabled(z);
            return;
        }
        boolean enabled = this.mTelephonyManager.getDataEnabled();
        Log.d("@M_WifiGprsSelector", "updateDataEnabler(), current state=" + enabled);
        this.mDataEnabler.setChecked(enabled);
        Log.d("@M_WifiGprsSelector", "single card mDataEnabler, true");
        CheckBoxPreference checkBoxPreference = this.mDataEnabler;
        if (this.mIsSIMExist && !this.mAirplaneModeEnabled) {
            z = true;
        }
        checkBoxPreference.setEnabled(z);
    }

    private void dealWithConnChange(boolean enabled) {
        if (isGeminiSupport()) {
            Log.d("@M_WifiGprsSelector", "only sigle SIM load can controling data connection");
            return;
        }
        Log.d("@M_WifiGprsSelector", "dealWithConnChange(),new request state is enabled?" + enabled + ";");
        this.mTelephonyManager.setDataEnabled(enabled);
        showDialog(1001);
        this.mIsGprsSwitching = true;
        if (enabled) {
            this.mTimeHandler.sendEmptyMessageDelayed(2001, 30000);
        } else {
            this.mTimeHandler.sendEmptyMessageDelayed(2000, 10000);
        }
    }

    public Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        if (id == 1001) {
            dialog.setMessage(getResources().getString(R.string.data_enabler_waiting_message));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        } else if (id != 1002) {
            return super.onCreateDialog(id);
        } else {
            Builder builder = new Builder(getActivity());
            ArrayList<String> simStatusStrings = new ArrayList();
            simStatusStrings = this.mCellConnMgr.getStringUsingState(this.mSelectedDataSubId, 4);
            builder.setTitle((CharSequence) simStatusStrings.get(0));
            builder.setMessage((CharSequence) simStatusStrings.get(1));
            builder.setPositiveButton((CharSequence) simStatusStrings.get(2), new C07598());
            builder.setNegativeButton((CharSequence) simStatusStrings.get(3), new C07609());
            return builder.create();
        }
    }

    private void initSimMap() {
        List<SubscriptionInfo> simList = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoList();
        if (simList != null) {
            this.mSimMap.clear();
            Log.i("@M_WifiGprsSelector", "sim number is " + simList.size());
            for (SubscriptionInfo subinfo : simList) {
                this.mSimMap.put(Integer.valueOf(subinfo.getSubscriptionId()), subinfo);
            }
            this.mSimMapKeyList = new ArrayList(this.mSimMap.keySet());
        }
    }

    private void switchGprsDefautlSIM(int subId) {
        if (subId >= 0 && SubscriptionManager.isValidSubscriptionId(subId) && subId != SubscriptionManager.getDefaultDataSubId()) {
            SubscriptionManager.from(getActivity()).setDefaultDataSubId(subId);
            showDialog(1001);
            this.mIsGprsSwitching = true;
            if (subId > 0) {
                this.mTimeHandler.sendEmptyMessageDelayed(2001, 30000);
                Log.d("@M_WifiGprsSelector", "set ATTACH_TIME_OUT");
            } else {
                this.mTimeHandler.sendEmptyMessageDelayed(2000, 10000);
                Log.d("@M_WifiGprsSelector", "set DETACH_TIME_OUT");
            }
        }
    }

    public int getStatusResource(int state) {
        switch (state) {
            case 1:
                return 134349080;
            case 2:
                return 134349061;
            case 3:
                return 134349054;
            case 4:
                return 134349087;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                return 134349085;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                return 134349041;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                return 134349086;
            default:
                return -1;
        }
    }

    private boolean isGeminiSupport() {
        MultiSimVariants config = TelephonyManager.getDefault().getMultiSimConfiguration();
        if (config == MultiSimVariants.DSDS || config == MultiSimVariants.DSDA) {
            return true;
        }
        return false;
    }
}
