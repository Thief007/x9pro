package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings.System;
import android.provider.Telephony.Carriers;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaApnSetting;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.ext.IRcseOnlyApnExt;
import com.mediatek.settings.ext.IRcseOnlyApnExt.OnRcseOnlyApnStateChangedListener;
import com.mediatek.settings.sim.MsimRadioValueObserver;
import com.mediatek.settings.sim.MsimRadioValueObserver.Listener;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.ArrayList;

public class ApnSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final Uri DEFAULTAPN_URI = Uri.parse("content://telephony/carriers/restore");
    private static final Uri PREFERAPN_URI = Uri.parse("content://telephony/carriers/preferapn");
    private static boolean mRestoreDefaultApnMode;
    private IApnSettingsExt mApnExt;
    private IntentFilter mMobileStateFilter;
    private final BroadcastReceiver mMobileStateReceiver = new C00421();
    private String mMvnoMatchData;
    private String mMvnoType;
    private MsimRadioValueObserver mRadioValueObserver;
    private IRcseOnlyApnExt mRcseApnExt;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private String mSelectedKey;
    private SimHotSwapHandler mSimHotSwapHandler;
    private SubscriptionInfo mSubscriptionInfo;
    private UiccController mUiccController;
    private UserManager mUm;
    private boolean mUnavailable;

    class C00421 extends BroadcastReceiver {
        private static /* synthetic */ int[] f5x3cb49f96;

        private static /* synthetic */ int[] m1xc53853a() {
            if (f5x3cb49f96 != null) {
                return f5x3cb49f96;
            }
            int[] iArr = new int[DataState.values().length];
            try {
                iArr[DataState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[DataState.CONNECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[DataState.DISCONNECTED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[DataState.SUSPENDED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            f5x3cb49f96 = iArr;
            return iArr;
        }

        C00421() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.ANY_DATA_STATE")) {
                DataState state = ApnSettings.getMobileDataState(intent);
                Log.d("ApnSettings", "onReceive ACTION_ANY_DATA_CONNECTION_STATE_CHANGED,state = " + state);
                switch (C00421.m1xc53853a()[state.ordinal()]) {
                    case 1:
                        if (!ApnSettings.mRestoreDefaultApnMode) {
                            ApnSettings.this.fillList();
                            break;
                        }
                        break;
                }
                ApnSettings.this.updateScreenForDataStateChange(context, intent);
            } else if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                ApnSettings.this.updateScreenEnableState(context);
            }
        }
    }

    class C00432 implements OnSimHotSwapListener {
        C00432() {
        }

        public void onSimHotSwap() {
            Log.d("ApnSettings", "onSimHotSwap, finish activity");
            if (ApnSettings.this.getActivity() != null) {
                ApnSettings.this.getActivity().finish();
            }
        }
    }

    class C00443 implements OnRcseOnlyApnStateChangedListener {
        C00443() {
        }

        public void OnRcseOnlyApnStateChanged() {
            Log.d("ApnSettings", "OnRcseOnlyApnStateChanged()");
            ApnSettings.this.fillList();
        }
    }

    class C00454 implements Listener {
        C00454() {
        }

        public void onChange(int msimModevalue, boolean selfChange) {
            if (ApnSettings.this.getActivity() != null) {
                ApnSettings.this.updateScreenEnableState(ApnSettings.this.getActivity());
                ApnSettings.this.getActivity().invalidateOptionsMenu();
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d("ApnSettings", "restore APN start~~");
                    ApnSettings.this.getContentResolver().delete(ApnSettings.this.getDefaultApnUri(ApnSettings.this.mSubscriptionInfo.getSubscriptionId()), null, null);
                    this.mRestoreApnUiHandler.sendEmptyMessage(2);
                    return;
                default:
                    return;
            }
        }
    }

    private class RestoreApnUiHandler extends Handler {
        private RestoreApnUiHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    Log.d("ApnSettings", "restore APN complete~~");
                    Activity activity = ApnSettings.this.getActivity();
                    if (activity != null) {
                        ApnSettings.this.fillList();
                        ApnSettings.this.updateScreenEnableState(activity);
                        ApnSettings.mRestoreDefaultApnMode = false;
                        ApnSettings.this.removeDialog(1001);
                        Toast.makeText(activity, ApnSettings.this.getResources().getString(R.string.restore_default_apn_completed), 1).show();
                        break;
                    }
                    ApnSettings.mRestoreDefaultApnMode = false;
                    return;
            }
        }
    }

    private void updatePreferApnForSvlte(java.lang.String r17, android.content.ContentResolver r18, android.content.ContentValues r19) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x00e3 in list [B:9:0x00df]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:42)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r16 = this;
        r11 = 0;
        r2 = r16.getContentResolver();	 Catch:{ all -> 0x0144 }
        r3 = android.provider.Telephony.Carriers.CONTENT_URI;	 Catch:{ all -> 0x0144 }
        r4 = 2;	 Catch:{ all -> 0x0144 }
        r4 = new java.lang.String[r4];	 Catch:{ all -> 0x0144 }
        r5 = "_id";	 Catch:{ all -> 0x0144 }
        r6 = 0;	 Catch:{ all -> 0x0144 }
        r4[r6] = r5;	 Catch:{ all -> 0x0144 }
        r5 = "sourcetype";	 Catch:{ all -> 0x0144 }
        r6 = 1;	 Catch:{ all -> 0x0144 }
        r4[r6] = r5;	 Catch:{ all -> 0x0144 }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0144 }
        r5.<init>();	 Catch:{ all -> 0x0144 }
        r6 = "_id = ";	 Catch:{ all -> 0x0144 }
        r5 = r5.append(r6);	 Catch:{ all -> 0x0144 }
        r0 = r17;	 Catch:{ all -> 0x0144 }
        r5 = r5.append(r0);	 Catch:{ all -> 0x0144 }
        r5 = r5.toString();	 Catch:{ all -> 0x0144 }
        r6 = 0;	 Catch:{ all -> 0x0144 }
        r7 = 0;	 Catch:{ all -> 0x0144 }
        r11 = r2.query(r3, r4, r5, r6, r7);	 Catch:{ all -> 0x0144 }
        r0 = r16;	 Catch:{ all -> 0x0144 }
        r2 = r0.mSubscriptionInfo;	 Catch:{ all -> 0x0144 }
        r10 = r2.getSubscriptionId();	 Catch:{ all -> 0x0144 }
        r2 = "ApnSettings";	 Catch:{ all -> 0x0144 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0144 }
        r3.<init>();	 Catch:{ all -> 0x0144 }
        r4 = "setSelectedApnKey cursor.getCount() - ";	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r4);	 Catch:{ all -> 0x0144 }
        r4 = r11.getCount();	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r4);	 Catch:{ all -> 0x0144 }
        r3 = r3.toString();	 Catch:{ all -> 0x0144 }
        android.util.Log.d(r2, r3);	 Catch:{ all -> 0x0144 }
        r2 = r11.getCount();	 Catch:{ all -> 0x0144 }
        if (r2 <= 0) goto L_0x00dd;	 Catch:{ all -> 0x0144 }
    L_0x005e:
        r8 = com.mediatek.internal.telephony.ltedc.svlte.SvlteModeController.getActiveSvlteModeSlotId();	 Catch:{ all -> 0x0144 }
        r2 = "ApnSettings";	 Catch:{ all -> 0x0144 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0144 }
        r3.<init>();	 Catch:{ all -> 0x0144 }
        r4 = "setSelectedApnKey(), currentSub = ";	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r4);	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r10);	 Catch:{ all -> 0x0144 }
        r4 = ", c2kSlot = ";	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r4);	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r8);	 Catch:{ all -> 0x0144 }
        r3 = r3.toString();	 Catch:{ all -> 0x0144 }
        android.util.Log.d(r2, r3);	 Catch:{ all -> 0x0144 }
        r9 = android.telephony.SubscriptionManager.getSlotId(r10);	 Catch:{ all -> 0x0144 }
        r2 = "ApnSettings";	 Catch:{ all -> 0x0144 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0144 }
        r3.<init>();	 Catch:{ all -> 0x0144 }
        r4 = "setSelectedApnKey currentSlot";	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r4);	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r9);	 Catch:{ all -> 0x0144 }
        r3 = r3.toString();	 Catch:{ all -> 0x0144 }
        android.util.Log.d(r2, r3);	 Catch:{ all -> 0x0144 }
        r11.moveToFirst();	 Catch:{ all -> 0x0144 }
        r2 = 1;	 Catch:{ all -> 0x0144 }
        r14 = r11.getInt(r2);	 Catch:{ all -> 0x0144 }
        r2 = 1;	 Catch:{ all -> 0x0144 }
        if (r14 != r2) goto L_0x00e4;	 Catch:{ all -> 0x0144 }
    L_0x00b0:
        if (r8 != r9) goto L_0x00e4;	 Catch:{ all -> 0x0144 }
    L_0x00b2:
        r2 = "ApnSettings";	 Catch:{ all -> 0x0144 }
        r3 = "setSelectedApnKey sourceType == 1 && c2kSlot == currentSlot";	 Catch:{ all -> 0x0144 }
        android.util.Log.d(r2, r3);	 Catch:{ all -> 0x0144 }
        r0 = r16;	 Catch:{ all -> 0x0144 }
        r2 = r0.getPreferApnUri(r10);	 Catch:{ all -> 0x0144 }
        r3 = 0;	 Catch:{ all -> 0x0144 }
        r4 = 0;	 Catch:{ all -> 0x0144 }
        r0 = r18;	 Catch:{ all -> 0x0144 }
        r1 = r19;	 Catch:{ all -> 0x0144 }
        r0.update(r2, r1, r3, r4);	 Catch:{ all -> 0x0144 }
        r2 = com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils.getLteDcSubId(r8);	 Catch:{ all -> 0x0144 }
        r0 = r16;	 Catch:{ all -> 0x0144 }
        r2 = r0.getPreferApnUri(r2);	 Catch:{ all -> 0x0144 }
        r3 = 0;	 Catch:{ all -> 0x0144 }
        r4 = 0;	 Catch:{ all -> 0x0144 }
        r0 = r18;	 Catch:{ all -> 0x0144 }
        r1 = r19;	 Catch:{ all -> 0x0144 }
        r0.update(r2, r1, r3, r4);	 Catch:{ all -> 0x0144 }
    L_0x00dd:
        if (r11 == 0) goto L_0x00e3;
    L_0x00df:
        r11.close();
        r11 = 0;
    L_0x00e3:
        return;
    L_0x00e4:
        r15 = new android.content.ContentValues;	 Catch:{ all -> 0x0144 }
        r15.<init>();	 Catch:{ all -> 0x0144 }
        r2 = "apn_id";	 Catch:{ all -> 0x0144 }
        r3 = -1;	 Catch:{ all -> 0x0144 }
        r3 = java.lang.Integer.valueOf(r3);	 Catch:{ all -> 0x0144 }
        r15.put(r2, r3);	 Catch:{ all -> 0x0144 }
        r2 = r16.getActivity();	 Catch:{ all -> 0x0144 }
        r13 = com.mediatek.settings.cdma.CdmaApnSetting.getPreferredSubId(r2, r10);	 Catch:{ all -> 0x0144 }
        r12 = com.mediatek.internal.telephony.ltedc.svlte.SvlteUtils.getLteDcSubId(r8);	 Catch:{ all -> 0x0144 }
        r2 = "ApnSettings";	 Catch:{ all -> 0x0144 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0144 }
        r3.<init>();	 Catch:{ all -> 0x0144 }
        r4 = "setSelectedApnKey(), preferredSub =  ";	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r4);	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r13);	 Catch:{ all -> 0x0144 }
        r4 = ", lteDcSub = ";	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r4);	 Catch:{ all -> 0x0144 }
        r3 = r3.append(r12);	 Catch:{ all -> 0x0144 }
        r3 = r3.toString();	 Catch:{ all -> 0x0144 }
        android.util.Log.d(r2, r3);	 Catch:{ all -> 0x0144 }
        r0 = r16;	 Catch:{ all -> 0x0144 }
        r2 = r0.getPreferApnUri(r13);	 Catch:{ all -> 0x0144 }
        r3 = 0;	 Catch:{ all -> 0x0144 }
        r4 = 0;	 Catch:{ all -> 0x0144 }
        r0 = r18;	 Catch:{ all -> 0x0144 }
        r1 = r19;	 Catch:{ all -> 0x0144 }
        r0.update(r2, r1, r3, r4);	 Catch:{ all -> 0x0144 }
        if (r13 != r12) goto L_0x014c;	 Catch:{ all -> 0x0144 }
    L_0x0136:
        r0 = r16;	 Catch:{ all -> 0x0144 }
        r2 = r0.getPreferApnUri(r10);	 Catch:{ all -> 0x0144 }
        r3 = 0;	 Catch:{ all -> 0x0144 }
        r4 = 0;	 Catch:{ all -> 0x0144 }
        r0 = r18;	 Catch:{ all -> 0x0144 }
        r0.update(r2, r15, r3, r4);	 Catch:{ all -> 0x0144 }
        goto L_0x00dd;
    L_0x0144:
        r2 = move-exception;
        if (r11 == 0) goto L_0x014b;
    L_0x0147:
        r11.close();
        r11 = 0;
    L_0x014b:
        throw r2;
    L_0x014c:
        r10 = r12;
        goto L_0x0136;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settings.ApnSettings.updatePreferApnForSvlte(java.lang.String, android.content.ContentResolver, android.content.ContentValues):void");
    }

    private static DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra("state");
        if (str != null) {
            return (DataState) Enum.valueOf(DataState.class, str);
        }
        return DataState.DISCONNECTED;
    }

    protected int getMetricsCategory() {
        return 12;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Activity activity = getActivity();
        int subId = activity.getIntent().getIntExtra("sub_id", -1);
        this.mUm = (UserManager) getSystemService("user");
        this.mMobileStateFilter = new IntentFilter("android.intent.action.ANY_DATA_STATE");
        this.mMobileStateFilter.addAction("android.intent.action.AIRPLANE_MODE");
        if (!this.mUm.hasUserRestriction("no_config_mobile_networks")) {
            setHasOptionsMenu(true);
        }
        this.mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(subId);
        this.mUiccController = UiccController.getInstance();
        if (this.mSubscriptionInfo == null) {
            Log.d("ApnSettings", "onCreate()... Invalid subId: " + subId);
            getActivity().finish();
        }
        this.mRadioValueObserver = new MsimRadioValueObserver(getActivity());
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C00432());
        this.mApnExt = UtilsExt.getApnSettingsPlugin(activity);
        this.mApnExt.initTetherField(this);
        this.mRcseApnExt = UtilsExt.getRcseApnPlugin(activity);
        this.mRcseApnExt.onCreate(new C00443(), subId);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TextView empty = (TextView) getView().findViewById(16908292);
        if (empty != null) {
            empty.setText(R.string.apn_settings_not_available);
            getListView().setEmptyView(empty);
        }
        if (this.mUm.hasUserRestriction("no_config_mobile_networks") || UserHandle.myUserId() != 0) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        addPreferencesFromResource(R.xml.apn_settings);
        getListView().setItemsCanFocus(true);
    }

    public void onResume() {
        super.onResume();
        if (!this.mUnavailable) {
            this.mRadioValueObserver.registerMsimObserver(new C00454());
            getActivity().registerReceiver(this.mMobileStateReceiver, this.mMobileStateFilter);
            if (!mRestoreDefaultApnMode) {
                fillList();
                removeDialog(1001);
            }
            this.mApnExt.updateTetherState();
        }
    }

    public void onPause() {
        super.onPause();
        if (!this.mUnavailable) {
            getActivity().unregisterReceiver(this.mMobileStateReceiver);
            this.mRadioValueObserver.ungisterMsimObserver();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mRestoreDefaultApnThread != null) {
            this.mRestoreDefaultApnThread.quit();
        }
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        this.mApnExt.onDestroy();
        this.mRcseApnExt.onDestory();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void fillList() {
        String mccmnc;
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        if (this.mSubscriptionInfo == null) {
            mccmnc = "";
        } else {
            mccmnc = tm.getSimOperator(this.mSubscriptionInfo.getSubscriptionId());
        }
        Log.d("ApnSettings", "mccmnc = " + mccmnc);
        String where = "numeric=\"" + mccmnc + "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL))";
        if (this.mSubscriptionInfo != null) {
            int subId = this.mSubscriptionInfo.getSubscriptionId();
            if (CdmaUtils.isCdmaCardType(SubscriptionManager.getSlotId(subId))) {
                where = CdmaApnSetting.customizeQuerySelectionforCdma(where, mccmnc, subId);
            }
        }
        where = this.mApnExt.getFillListQuery(where, mccmnc);
        if (!FeatureOption.MTK_VOLTE_SUPPORT) {
            where = where + " AND NOT type='ims'";
        }
        Log.d("ApnSettings", "fillList where: " + where);
        String order = this.mApnExt.getApnSortOrder("name ASC");
        Log.d("ApnSettings", "fillList sort: " + order);
        Cursor cursor = getContentResolver().query(Carriers.CONTENT_URI, new String[]{"_id", "name", "apn", "type", "mvno_type", "mvno_match_data", "sourcetype"}, where, null, order);
        if (cursor != null) {
            Log.d("ApnSettings", "fillList, cursor count: " + cursor.getCount());
            IccRecords r = null;
            if (!(this.mUiccController == null || this.mSubscriptionInfo == null)) {
                r = this.mUiccController.getIccRecords(SubscriptionManager.getPhoneId(this.mSubscriptionInfo.getSubscriptionId()), 1);
            }
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();
            ArrayList<Preference> mnoApnList = new ArrayList();
            ArrayList<Preference> mvnoApnList = new ArrayList();
            ArrayList<Preference> mnoMmsApnList = new ArrayList();
            ArrayList<Preference> mvnoMmsApnList = new ArrayList();
            this.mSelectedKey = getSelectedApnKey();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(1);
                String apn = cursor.getString(2);
                String key = cursor.getString(0);
                String type = cursor.getString(3);
                String mvnoType = cursor.getString(4);
                String mvnoMatchData = cursor.getString(5);
                int sourcetype = cursor.getInt(6);
                if (shouldSkipApn(type)) {
                    cursor.moveToNext();
                } else {
                    boolean z;
                    name = this.mApnExt.updateApnName(name, sourcetype);
                    ApnPreference pref = new ApnPreference(getActivity());
                    pref.setKey(key);
                    pref.setTitle(name);
                    pref.setSummary(apn);
                    pref.setPersistent(false);
                    pref.setOnPreferenceChangeListener(this);
                    pref.setApnEditable(true);
                    pref.setSubId((this.mSubscriptionInfo == null ? null : Integer.valueOf(this.mSubscriptionInfo.getSubscriptionId())).intValue());
                    if (type != null) {
                        if (!type.equals("mms")) {
                            if (!type.equals("ia")) {
                            }
                        }
                        z = false;
                        pref.setSelectable(z);
                        Log.d("ApnSettings", "mSelectedKey = " + this.mSelectedKey + " key = " + key + " name = " + name);
                        if (z) {
                            addApnToList(pref, mnoMmsApnList, mvnoMmsApnList, r, mvnoType, mvnoMatchData);
                            this.mApnExt.customizeUnselectableApn(mnoMmsApnList, mvnoMmsApnList, (this.mSubscriptionInfo != null ? null : Integer.valueOf(this.mSubscriptionInfo.getSubscriptionId())).intValue());
                        } else {
                            addApnToList(pref, mnoApnList, mvnoApnList, r, mvnoType, mvnoMatchData);
                        }
                        cursor.moveToNext();
                    }
                    z = this.mApnExt.isSelectable(type);
                    pref.setSelectable(z);
                    Log.d("ApnSettings", "mSelectedKey = " + this.mSelectedKey + " key = " + key + " name = " + name);
                    if (z) {
                        addApnToList(pref, mnoMmsApnList, mvnoMmsApnList, r, mvnoType, mvnoMatchData);
                        if (this.mSubscriptionInfo != null) {
                        }
                        this.mApnExt.customizeUnselectableApn(mnoMmsApnList, mvnoMmsApnList, (this.mSubscriptionInfo != null ? null : Integer.valueOf(this.mSubscriptionInfo.getSubscriptionId())).intValue());
                    } else {
                        addApnToList(pref, mnoApnList, mvnoApnList, r, mvnoType, mvnoMatchData);
                    }
                    cursor.moveToNext();
                }
            }
            cursor.close();
            if (!mvnoApnList.isEmpty()) {
                mnoApnList = mvnoApnList;
                mnoMmsApnList = mvnoMmsApnList;
            }
            for (Preference preference : mnoApnList) {
                apnList.addPreference(preference);
            }
            for (Preference preference2 : mnoMmsApnList) {
                apnList.addPreference(preference2);
            }
            setPreferApnChecked(mnoApnList);
            updateScreenEnableState(getActivity());
        }
    }

    private void addApnToList(ApnPreference pref, ArrayList<Preference> mnoList, ArrayList<Preference> mvnoList, IccRecords r, String mvnoType, String mvnoMatchData) {
        if (r == null || TextUtils.isEmpty(mvnoType) || TextUtils.isEmpty(mvnoMatchData)) {
            mnoList.add(pref);
        } else if (ApnSetting.mvnoMatches(r, mvnoType, mvnoMatchData)) {
            mvnoList.add(pref);
            this.mMvnoType = mvnoType;
            this.mMvnoMatchData = mvnoMatchData;
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        int subscriptionId;
        if (!this.mUnavailable) {
            menu.add(0, 1, 0, getResources().getString(R.string.menu_new)).setIcon(17301555).setShowAsAction(1);
            menu.add(0, 2, 0, getResources().getString(R.string.menu_restore)).setIcon(17301589);
        }
        IApnSettingsExt iApnSettingsExt = this.mApnExt;
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        if (this.mSubscriptionInfo != null) {
            subscriptionId = this.mSubscriptionInfo.getSubscriptionId();
        } else {
            subscriptionId = -1;
        }
        iApnSettingsExt.updateMenu(menu, 1, 2, telephonyManager.getSimOperator(subscriptionId));
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                addNewApn();
                return true;
            case 2:
                restoreDefaultApn();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addNewApn() {
        int subId;
        Intent intent = new Intent("android.intent.action.INSERT", Carriers.CONTENT_URI);
        if (this.mSubscriptionInfo != null) {
            subId = this.mSubscriptionInfo.getSubscriptionId();
        } else {
            subId = -1;
        }
        intent.putExtra("sub_id", subId);
        if (!(TextUtils.isEmpty(this.mMvnoType) || TextUtils.isEmpty(this.mMvnoMatchData))) {
            intent.putExtra("mvno_type", this.mMvnoType);
            intent.putExtra("mvno_match_data", this.mMvnoMatchData);
        }
        this.mApnExt.addApnTypeExtra(intent);
        startActivity(intent);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String str = "android.intent.action.EDIT";
        startActivity(new Intent(str, ContentUris.withAppendedId(Carriers.CONTENT_URI, (long) Integer.parseInt(preference.getKey()))));
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("ApnSettings", "onPreferenceChange(): Preference - " + preference + ", newValue - " + newValue + ", newValue type - " + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }
        return true;
    }

    private void setSelectedApnKey(String key) {
        this.mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put("apn_id", this.mSelectedKey);
        if (FeatureOption.MTK_SVLTE_SUPPORT) {
            updatePreferApnForSvlte(key, resolver, values);
        } else {
            resolver.update(getPreferApnUri(this.mSubscriptionInfo.getSubscriptionId()), values, null, null);
        }
    }

    private String getSelectedApnKey() {
        String key = null;
        int subId = this.mSubscriptionInfo.getSubscriptionId();
        ContentResolver contentResolver = getContentResolver();
        if (FeatureOption.MTK_SVLTE_SUPPORT) {
            subId = CdmaApnSetting.getPreferredSubId(getActivity(), subId);
        }
        Cursor cursor = contentResolver.query(getPreferApnUri(subId), new String[]{"_id"}, null, null, "name ASC");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(0);
        }
        cursor.close();
        Log.d("ApnSettings", "getSelectedApnKey(), key = " + key);
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(1001);
        mRestoreDefaultApnMode = true;
        if (this.mRestoreApnUiHandler == null) {
            this.mRestoreApnUiHandler = new RestoreApnUiHandler();
        }
        if (this.mRestoreApnProcessHandler == null || this.mRestoreDefaultApnThread == null) {
            this.mRestoreDefaultApnThread = new HandlerThread("Restore default APN Handler: Process Thread");
            this.mRestoreDefaultApnThread.start();
            this.mRestoreApnProcessHandler = new RestoreApnProcessHandler(this.mRestoreDefaultApnThread.getLooper(), this.mRestoreApnUiHandler);
        }
        this.mRestoreApnProcessHandler.sendEmptyMessage(1);
        return true;
    }

    public Dialog onCreateDialog(int id) {
        if (id != 1001) {
            return null;
        }
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getResources().getString(R.string.restore_default_apn));
        dialog.setCancelable(false);
        return dialog;
    }

    private void updateScreenForDataStateChange(Context context, Intent intent) {
        String apnType = intent.getStringExtra("apnType");
        Log.d("ApnSettings", "Receiver,send MMS status, get type = " + apnType);
        if ("mms".equals(apnType)) {
            boolean z;
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (isMmsInTransaction(context)) {
                z = false;
            } else {
                z = this.mApnExt.getScreenEnableState(this.mSubscriptionInfo.getSubscriptionId(), getActivity());
            }
            preferenceScreen.setEnabled(z);
        }
    }

    private void updateScreenEnableState(Context context) {
        boolean z = false;
        int subId = this.mSubscriptionInfo.getSubscriptionId();
        boolean simReady = 5 == TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotId(subId));
        boolean airplaneModeEnabled = System.getInt(context.getContentResolver(), "airplane_mode_on", -1) == 1;
        boolean isMultiSimMode = System.getInt(context.getContentResolver(), "msim_mode_setting", -1) != 0;
        boolean z2 = (airplaneModeEnabled || !simReady) ? false : isMultiSimMode;
        Log.d("ApnSettings", "updateScreenEnableState(), subId = " + subId + " ,airplaneModeEnabled = " + airplaneModeEnabled + " ,simReady = " + simReady + " , isMultiSimMode = " + isMultiSimMode);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (z2) {
            z = this.mApnExt.getScreenEnableState(subId, getActivity());
        }
        preferenceScreen.setEnabled(z);
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private boolean isMmsInTransaction(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (cm == null) {
            return false;
        }
        NetworkInfo networkInfo = cm.getNetworkInfo(2);
        if (networkInfo == null) {
            return false;
        }
        State state = networkInfo.getState();
        Log.d("ApnSettings", "mms state = " + state);
        if (state != State.CONNECTING) {
            return state == State.CONNECTED;
        } else {
            return true;
        }
    }

    public boolean shouldSkipApn(String type) {
        return "cmmail".equals(type) || !this.mRcseApnExt.isRcseOnlyApnEnabled(type);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        int size = menu.size();
        boolean isAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        Log.d("ApnSettings", "onPrepareOptionsMenu isAirplaneModeOn = " + isAirplaneModeOn);
        for (int i = 0; i < size; i++) {
            menu.getItem(i).setEnabled(!isAirplaneModeOn);
        }
        super.onPrepareOptionsMenu(menu);
    }

    private Uri getPreferApnUri(int subId) {
        Uri preferredUri = Uri.withAppendedPath(Uri.parse("content://telephony/carriers/preferapn"), "/subId/" + subId);
        Log.d("ApnSettings", "getPreferredApnUri: " + preferredUri);
        return this.mApnExt.getPreferCarrierUri(preferredUri, subId);
    }

    private Uri getDefaultApnUri(int subId) {
        return Uri.withAppendedPath(DEFAULTAPN_URI, "/subId/" + subId);
    }

    private void setPreferApnChecked(ArrayList<Preference> apnList) {
        if (apnList != null && !apnList.isEmpty()) {
            String str = null;
            if (this.mSelectedKey != null) {
                for (Preference pref : apnList) {
                    if (this.mSelectedKey.equals(pref.getKey())) {
                        ((ApnPreference) pref).setChecked();
                        str = this.mSelectedKey;
                    }
                }
            }
            if (str == null && apnList.get(0) != null) {
                ((ApnPreference) apnList.get(0)).setChecked();
                str = ((Preference) apnList.get(0)).getKey();
            }
            if (!(str == null || str == this.mSelectedKey)) {
                setSelectedApnKey(str);
                this.mSelectedKey = str;
            }
            Log.d("ApnSettings", "setPreferApnChecked, APN = " + this.mSelectedKey);
        }
    }
}
