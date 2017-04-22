package com.android.settings;

import android.app.Activity;
import android.app.QueuedWork;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class RadioInfo extends Activity {
    private static /* synthetic */ int[] f7xa7a8920;
    private final String TAG = "phone";
    private TextView attempts;
    private TextView callState;
    private Button cellInfoListRateButton;
    private TextView dBm;
    private TextView disconnects;
    private TextView dnsCheckState;
    private Button dnsCheckToggleButton;
    private TextView gprsState;
    private TextView gsmState;
    private Button imsRegRequiredButton;
    private Button imsVoLteProvisionedButton;
    private Button lteRamDumpButton;
    private TextView mCellInfo;
    CellInfoListRateHandler mCellInfoListRateHandler = new CellInfoListRateHandler();
    private List<CellInfo> mCellInfoValue;
    private TextView mCfi;
    private boolean mCfiValue = false;
    private TextView mDcRtInfoTv;
    private TextView mDeviceId;
    OnClickListener mDnsCheckButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo.this.phone.disableDnsCheck(!RadioInfo.this.phone.isDnsCheckDisabled());
            RadioInfo.this.updateDnsCheckState();
        }
    };
    private OnMenuItemClickListener mGetPdpList = new C01686();
    private Handler mHandler = new C01642();
    private TextView mHttpClientTest;
    private String mHttpClientTestResult;
    OnClickListener mImsRegRequiredHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo radioInfo = RadioInfo.this;
            String str = "toggle %s: currently %s";
            Object[] objArr = new Object[2];
            objArr[0] = "persist.radio.imsregrequired";
            objArr[1] = RadioInfo.this.isImsRegRequired() ? "on" : "off";
            radioInfo.log(String.format(str, objArr));
            SystemProperties.set("persist.radio.imsregrequired", !RadioInfo.this.isImsRegRequired() ? "1" : "0");
            RadioInfo.this.updateImsRegRequiredState();
        }
    };
    OnClickListener mImsVoLteProvisionedHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo radioInfo = RadioInfo.this;
            String str = "toggle VoLTE provisioned: currently %s";
            Object[] objArr = new Object[1];
            objArr[0] = RadioInfo.this.isImsVoLteProvisioned() ? "on" : "off";
            radioInfo.log(String.format(str, objArr));
            final boolean newValue = !RadioInfo.this.isImsVoLteProvisioned();
            if (RadioInfo.this.phone != null) {
                final ImsManager imsManager = ImsManager.getInstance(RadioInfo.this.phone.getContext(), RadioInfo.this.phone.getSubId());
                if (imsManager != null) {
                    QueuedWork.singleThreadExecutor().submit(new Runnable() {
                        public void run() {
                            try {
                                imsManager.getConfigInterface().setProvisionedValue(10, newValue ? 1 : 0);
                            } catch (ImsException e) {
                                Log.e("phone", "setImsVoLteProvisioned() exception:", e);
                            }
                        }
                    });
                }
            }
            RadioInfo.this.updateImsVoLteProvisionedState();
        }
    };
    private TextView mLocation;
    OnClickListener mLteRamDumpHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo radioInfo = RadioInfo.this;
            String str = "toggle %s: currently %s";
            Object[] objArr = new Object[2];
            objArr[0] = "persist.radio.ramdump";
            objArr[1] = RadioInfo.this.isSmsOverImsEnabled() ? "on" : "off";
            radioInfo.log(String.format(str, objArr));
            SystemProperties.set("persist.radio.ramdump", !RadioInfo.this.isLteRamDumpEnabled() ? "1" : "0");
            RadioInfo.this.updateLteRamDumpState();
        }
    };
    private TextView mMwi;
    private boolean mMwiValue = false;
    private TextView mNeighboringCids;
    OnClickListener mOemInfoButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            try {
                RadioInfo.this.startActivity(new Intent("com.android.settings.OEM_RADIO_INFO"));
            } catch (ActivityNotFoundException ex) {
                RadioInfo.this.log("OEM-specific Info/Settings Activity Not Found : " + ex);
            }
        }
    };
    private PhoneStateListener mPhoneStateListener = new C01631();
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    OnClickListener mPingButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo.this.updatePingState();
        }
    };
    private TextView mPingHostname;
    private String mPingHostnameResult;
    private TextView mPingIpAddr;
    private String mPingIpAddrResult;
    OnClickListener mPowerButtonHandler = new C01719();
    OnItemSelectedListener mPreferredNetworkHandler = new OnItemSelectedListener() {
        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            Message msg = RadioInfo.this.mHandler.obtainMessage(1001);
            if (pos >= 0 && pos <= RadioInfo.this.mPreferredNetworkLabels.length - 2) {
                RadioInfo.this.phone.setPreferredNetworkType(pos, msg);
            }
        }

        public void onNothingSelected(AdapterView parent) {
        }
    };
    private String[] mPreferredNetworkLabels = new String[]{"WCDMA preferred", "GSM only", "WCDMA only", "GSM auto (PRL)", "CDMA auto (PRL)", "CDMA only", "EvDo only", "GSM/CDMA auto (PRL)", "LTE/CDMA auto (PRL)", "LTE/GSM auto (PRL)", "LTE/GSM/CDMA auto (PRL)", "LTE only", "Unknown"};
    OnClickListener mRefreshSmscButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo.this.refreshSmsc();
        }
    };
    private OnMenuItemClickListener mSelectBandCallback = new C01697();
    OnClickListener mSmsOverImsHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo radioInfo = RadioInfo.this;
            String str = "toggle %s: currently %s";
            Object[] objArr = new Object[2];
            objArr[0] = "persist.radio.imsallowmtsms";
            objArr[1] = RadioInfo.this.isSmsOverImsEnabled() ? "on" : "off";
            radioInfo.log(String.format(str, objArr));
            SystemProperties.set("persist.radio.imsallowmtsms", !RadioInfo.this.isSmsOverImsEnabled() ? "1" : "0");
            RadioInfo.this.updateSmsOverImsState();
        }
    };
    private TelephonyManager mTelephonyManager;
    private OnMenuItemClickListener mToggleData = new C01708();
    OnClickListener mUpdateSmscButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            RadioInfo.this.updateSmscButton.setEnabled(false);
            RadioInfo.this.phone.setSmscAddress(RadioInfo.this.smsc.getText().toString(), RadioInfo.this.mHandler.obtainMessage(1006));
        }
    };
    private OnMenuItemClickListener mViewADNCallback = new C01653();
    private OnMenuItemClickListener mViewFDNCallback = new C01664();
    private OnMenuItemClickListener mViewSDNCallback = new C01675();
    private TextView network;
    private TextView number;
    private Button oemInfoButton;
    private TextView operatorName;
    private Phone phone = null;
    private Button pingTestButton;
    private Spinner preferredNetworkType;
    private Button radioPowerButton;
    private TextView received;
    private Button refreshSmscButton;
    private TextView resets;
    private TextView roamingState;
    private TextView sent;
    private TextView sentSinceReceived;
    private Button smsOverImsButton;
    private EditText smsc;
    private TextView successes;
    private Button updateSmscButton;

    class C01631 extends PhoneStateListener {
        C01631() {
        }

        public void onDataConnectionStateChanged(int state) {
            RadioInfo.this.updateDataState();
            RadioInfo.this.updateDataStats();
            RadioInfo.this.updatePdpList();
            RadioInfo.this.updateNetworkType();
        }

        public void onDataActivity(int direction) {
            RadioInfo.this.updateDataStats2();
        }

        public void onCellLocationChanged(CellLocation location) {
            RadioInfo.this.updateLocation(location);
        }

        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            RadioInfo.this.mMwiValue = mwi;
            RadioInfo.this.updateMessageWaiting();
        }

        public void onCallForwardingIndicatorChanged(boolean cfi) {
            RadioInfo.this.mCfiValue = cfi;
            RadioInfo.this.updateCallRedirect();
        }

        public void onCellInfoChanged(List<CellInfo> arrayCi) {
            RadioInfo.this.log("onCellInfoChanged: arrayCi=" + arrayCi);
            RadioInfo.this.updateCellInfoTv(arrayCi);
        }

        public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
            RadioInfo.this.log("onDataConnectionRealTimeInfoChanged: dcRtInfo=" + dcRtInfo);
            RadioInfo.this.updateDcRtInfoTv(dcRtInfo);
        }
    }

    class C01642 extends Handler {
        C01642() {
        }

        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case 100:
                    RadioInfo.this.updatePhoneState();
                    return;
                case 200:
                    RadioInfo.this.updateSignalStrength();
                    return;
                case 300:
                    RadioInfo.this.updateServiceState();
                    RadioInfo.this.updatePowerState();
                    RadioInfo.this.updateImsVoLteProvisionedState();
                    return;
                case 1000:
                    ar = msg.obj;
                    if (ar.exception == null) {
                        int type = ((int[]) ar.result)[0];
                        if (type >= RadioInfo.this.mPreferredNetworkLabels.length) {
                            RadioInfo.this.log("EVENT_QUERY_PREFERRED_TYPE_DONE: unknown type=" + type);
                            type = RadioInfo.this.mPreferredNetworkLabels.length - 1;
                        }
                        RadioInfo.this.preferredNetworkType.setSelection(type, true);
                        return;
                    }
                    RadioInfo.this.preferredNetworkType.setSelection(RadioInfo.this.mPreferredNetworkLabels.length - 1, true);
                    return;
                case 1001:
                    if (((AsyncResult) msg.obj).exception != null) {
                        RadioInfo.this.phone.getPreferredNetworkType(obtainMessage(1000));
                        return;
                    }
                    return;
                case 1002:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        RadioInfo.this.updateNeighboringCids((ArrayList) ar.result);
                        return;
                    } else {
                        RadioInfo.this.mNeighboringCids.setText("unknown");
                        return;
                    }
                case 1005:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        RadioInfo.this.smsc.setText("refresh error");
                        return;
                    } else {
                        RadioInfo.this.smsc.setText((String) ar.result);
                        return;
                    }
                case 1006:
                    RadioInfo.this.updateSmscButton.setEnabled(true);
                    if (((AsyncResult) msg.obj).exception != null) {
                        RadioInfo.this.smsc.setText("update error");
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    class C01653 implements OnMenuItemClickListener {
        C01653() {
        }

        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setClassName("com.android.phone", "com.android.phone.SimContacts");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    }

    class C01664 implements OnMenuItemClickListener {
        C01664() {
        }

        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setClassName("com.android.phone", "com.android.phone.settings.fdn.FdnList");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    }

    class C01675 implements OnMenuItemClickListener {
        C01675() {
        }

        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("content://icc/sdn"));
            intent.setClassName("com.android.phone", "com.android.phone.ADNList");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    }

    class C01686 implements OnMenuItemClickListener {
        C01686() {
        }

        public boolean onMenuItemClick(MenuItem item) {
            RadioInfo.this.phone.getDataCallList(null);
            return true;
        }
    }

    class C01697 implements OnMenuItemClickListener {
        C01697() {
        }

        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent();
            intent.setClass(RadioInfo.this, BandMode.class);
            RadioInfo.this.startActivity(intent);
            return true;
        }
    }

    class C01708 implements OnMenuItemClickListener {
        C01708() {
        }

        public boolean onMenuItemClick(MenuItem item) {
            switch (RadioInfo.this.mTelephonyManager.getDataState()) {
                case 0:
                    RadioInfo.this.phone.setDataEnabled(true);
                    break;
                case 2:
                    RadioInfo.this.phone.setDataEnabled(false);
                    break;
            }
            return true;
        }
    }

    class C01719 implements OnClickListener {
        C01719() {
        }

        public void onClick(View v) {
            RadioInfo.this.phone.setRadioPower(!RadioInfo.this.isRadioOn());
        }
    }

    class CellInfoListRateHandler implements OnClickListener {
        int index = 0;
        int[] rates = new int[]{Integer.MAX_VALUE, 0, 1000};

        CellInfoListRateHandler() {
        }

        public int getRate() {
            return this.rates[this.index];
        }

        public void onClick(View v) {
            this.index++;
            if (this.index >= this.rates.length) {
                this.index = 0;
            }
            RadioInfo.this.phone.setCellInfoListRate(this.rates[this.index]);
            RadioInfo.this.updateCellInfoListRate();
        }
    }

    private static /* synthetic */ int[] m3x8c95e0c4() {
        if (f7xa7a8920 != null) {
            return f7xa7a8920;
        }
        int[] iArr = new int[State.values().length];
        try {
            iArr[State.IDLE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[State.OFFHOOK.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[State.RINGING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f7xa7a8920 = iArr;
        return iArr;
    }

    private void httpClientTest() {
        /* JADX: method processing error */
/*
Error: java.util.NoSuchElementException
	at java.util.HashMap$HashIterator.nextNode(Unknown Source)
	at java.util.HashMap$KeyIterator.next(Unknown Source)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.applyRemove(BlockFinallyExtract.java:535)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.extractFinally(BlockFinallyExtract.java:175)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.processExceptionHandler(BlockFinallyExtract.java:79)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:51)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r6 = this;
        r3 = 0;
        r2 = new java.net.URL;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r4 = "https://www.google.com";	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r2.<init>(r4);	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r4 = r2.openConnection();	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r0 = r4;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r0 = (java.net.HttpURLConnection) r0;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r3 = r0;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r4 = r3.getResponseCode();	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r5 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        if (r4 != r5) goto L_0x0024;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
    L_0x0019:
        r4 = "Pass";	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r6.mHttpClientTestResult = r4;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
    L_0x001e:
        if (r3 == 0) goto L_0x0023;
    L_0x0020:
        r3.disconnect();
    L_0x0023:
        return;
    L_0x0024:
        r4 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r4.<init>();	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r5 = "Fail: Code: ";	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r4 = r4.append(r5);	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r5 = r3.getResponseMessage();	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r4 = r4.append(r5);	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r4 = r4.toString();	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r6.mHttpClientTestResult = r4;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        goto L_0x001e;
    L_0x003f:
        r1 = move-exception;
        r4 = "Fail: IOException";	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        r6.mHttpClientTestResult = r4;	 Catch:{ IOException -> 0x003f, all -> 0x004b }
        if (r3 == 0) goto L_0x0023;
    L_0x0047:
        r3.disconnect();
        goto L_0x0023;
    L_0x004b:
        r4 = move-exception;
        if (r3 == 0) goto L_0x0051;
    L_0x004e:
        r3.disconnect();
    L_0x0051:
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settings.RadioInfo.httpClientTest():void");
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.radio_info);
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.phone = PhoneFactory.getDefaultPhone();
        this.mDeviceId = (TextView) findViewById(R.id.imei);
        this.number = (TextView) findViewById(R.id.number);
        this.callState = (TextView) findViewById(R.id.call);
        this.operatorName = (TextView) findViewById(R.id.operator);
        this.roamingState = (TextView) findViewById(R.id.roaming);
        this.gsmState = (TextView) findViewById(R.id.gsm);
        this.gprsState = (TextView) findViewById(R.id.gprs);
        this.network = (TextView) findViewById(R.id.network);
        this.dBm = (TextView) findViewById(R.id.dbm);
        this.mMwi = (TextView) findViewById(R.id.mwi);
        this.mCfi = (TextView) findViewById(R.id.cfi);
        this.mLocation = (TextView) findViewById(R.id.location);
        this.mNeighboringCids = (TextView) findViewById(R.id.neighboring);
        this.mCellInfo = (TextView) findViewById(R.id.cellinfo);
        this.mDcRtInfoTv = (TextView) findViewById(R.id.dcrtinfo);
        this.resets = (TextView) findViewById(R.id.resets);
        this.attempts = (TextView) findViewById(R.id.attempts);
        this.successes = (TextView) findViewById(R.id.successes);
        this.disconnects = (TextView) findViewById(R.id.disconnects);
        this.sentSinceReceived = (TextView) findViewById(R.id.sentSinceReceived);
        this.sent = (TextView) findViewById(R.id.sent);
        this.received = (TextView) findViewById(R.id.received);
        this.smsc = (EditText) findViewById(R.id.smsc);
        this.dnsCheckState = (TextView) findViewById(R.id.dnsCheckState);
        this.mPingIpAddr = (TextView) findViewById(R.id.pingIpAddr);
        this.mPingHostname = (TextView) findViewById(R.id.pingHostname);
        this.mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);
        this.preferredNetworkType = (Spinner) findViewById(R.id.preferredNetworkType);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, 17367048, this.mPreferredNetworkLabels);
        adapter.setDropDownViewResource(17367049);
        this.preferredNetworkType.setAdapter(adapter);
        this.preferredNetworkType.setOnItemSelectedListener(this.mPreferredNetworkHandler);
        this.radioPowerButton = (Button) findViewById(R.id.radio_power);
        this.radioPowerButton.setOnClickListener(this.mPowerButtonHandler);
        this.cellInfoListRateButton = (Button) findViewById(R.id.cell_info_list_rate);
        this.cellInfoListRateButton.setOnClickListener(this.mCellInfoListRateHandler);
        this.imsRegRequiredButton = (Button) findViewById(R.id.ims_reg_required);
        this.imsRegRequiredButton.setOnClickListener(this.mImsRegRequiredHandler);
        this.imsVoLteProvisionedButton = (Button) findViewById(R.id.volte_provisioned_flag);
        this.imsVoLteProvisionedButton.setOnClickListener(this.mImsVoLteProvisionedHandler);
        this.smsOverImsButton = (Button) findViewById(R.id.sms_over_ims);
        this.smsOverImsButton.setOnClickListener(this.mSmsOverImsHandler);
        this.lteRamDumpButton = (Button) findViewById(R.id.lte_ram_dump);
        this.lteRamDumpButton.setOnClickListener(this.mLteRamDumpHandler);
        this.pingTestButton = (Button) findViewById(R.id.ping_test);
        this.pingTestButton.setOnClickListener(this.mPingButtonHandler);
        this.updateSmscButton = (Button) findViewById(R.id.update_smsc);
        this.updateSmscButton.setOnClickListener(this.mUpdateSmscButtonHandler);
        this.refreshSmscButton = (Button) findViewById(R.id.refresh_smsc);
        this.refreshSmscButton.setOnClickListener(this.mRefreshSmscButtonHandler);
        this.dnsCheckToggleButton = (Button) findViewById(R.id.dns_check_toggle);
        this.dnsCheckToggleButton.setOnClickListener(this.mDnsCheckButtonHandler);
        this.oemInfoButton = (Button) findViewById(R.id.oem_info);
        this.oemInfoButton.setOnClickListener(this.mOemInfoButtonHandler);
        if (getPackageManager().queryIntentActivities(new Intent("com.android.settings.OEM_RADIO_INFO"), 0).size() == 0) {
            this.oemInfoButton.setEnabled(false);
        }
        this.mPhoneStateReceiver = new PhoneStateIntentReceiver(this, this.mHandler);
        this.mPhoneStateReceiver.notifySignalStrength(200);
        this.mPhoneStateReceiver.notifyServiceState(300);
        this.mPhoneStateReceiver.notifyPhoneCallState(100);
        this.phone.getPreferredNetworkType(this.mHandler.obtainMessage(1000));
        this.phone.getNeighboringCids(this.mHandler.obtainMessage(1002));
        CellLocation.requestLocationUpdate();
        this.mCellInfoValue = this.mTelephonyManager.getAllCellInfo();
        log("onCreate: mCellInfoValue=" + this.mCellInfoValue);
    }

    protected void onResume() {
        super.onResume();
        updatePhoneState();
        updateSignalStrength();
        updateMessageWaiting();
        updateCallRedirect();
        updateServiceState();
        updateLocation(this.mTelephonyManager.getCellLocation());
        updateDataState();
        updateDataStats();
        updateDataStats2();
        updatePowerState();
        updateCellInfoListRate();
        updateImsRegRequiredState();
        updateImsVoLteProvisionedState();
        updateSmsOverImsState();
        updateLteRamDumpState();
        updateProperties();
        updateDnsCheckState();
        log("onResume: register phone & data intents");
        this.mPhoneStateReceiver.registerIntent();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 9436);
    }

    public void onPause() {
        super.onPause();
        log("onPause: unregister phone & data intents");
        this.mPhoneStateReceiver.unregisterIntent();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.radio_info_band_mode_label).setOnMenuItemClickListener(this.mSelectBandCallback).setAlphabeticShortcut('b');
        menu.add(1, 1, 0, R.string.radioInfo_menu_viewADN).setOnMenuItemClickListener(this.mViewADNCallback);
        menu.add(1, 2, 0, R.string.radioInfo_menu_viewFDN).setOnMenuItemClickListener(this.mViewFDNCallback);
        menu.add(1, 3, 0, R.string.radioInfo_menu_viewSDN).setOnMenuItemClickListener(this.mViewSDNCallback);
        menu.add(1, 4, 0, R.string.radioInfo_menu_getPDP).setOnMenuItemClickListener(this.mGetPdpList);
        menu.add(1, 5, 0, "Disable data connection").setOnMenuItemClickListener(this.mToggleData);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(5);
        boolean visible = true;
        switch (this.mTelephonyManager.getDataState()) {
            case 0:
                item.setTitle("Enable data connection");
                break;
            case 2:
            case 3:
                item.setTitle("Disable data connection");
                break;
            default:
                visible = false;
                break;
        }
        item.setVisible(visible);
        return true;
    }

    private boolean isRadioOn() {
        return this.phone.getServiceState().getState() != 3;
    }

    private void updatePowerState() {
        String buttonText;
        if (isRadioOn()) {
            buttonText = getString(R.string.turn_off_radio);
        } else {
            buttonText = getString(R.string.turn_on_radio);
        }
        this.radioPowerButton.setText(buttonText);
    }

    private void updateCellInfoListRate() {
        this.cellInfoListRateButton.setText("CellInfoListRate " + this.mCellInfoListRateHandler.getRate());
        updateCellInfoTv(this.mTelephonyManager.getAllCellInfo());
    }

    private void updateDnsCheckState() {
        this.dnsCheckState.setText(this.phone.isDnsCheckDisabled() ? "0.0.0.0 allowed" : "0.0.0.0 not allowed");
    }

    private final void updateSignalStrength() {
        int state = this.mPhoneStateReceiver.getServiceState().getState();
        Resources r = getResources();
        if (1 == state || 3 == state) {
            this.dBm.setText("0");
        }
        int signalDbm = this.mPhoneStateReceiver.getSignalStrengthDbm();
        if (-1 == signalDbm) {
            signalDbm = 0;
        }
        int signalAsu = this.mPhoneStateReceiver.getSignalStrengthLevelAsu();
        if (-1 == signalAsu) {
            signalAsu = 0;
        }
        this.dBm.setText(String.valueOf(signalDbm) + " " + r.getString(R.string.radioInfo_display_dbm) + "   " + String.valueOf(signalAsu) + " " + r.getString(R.string.radioInfo_display_asu));
    }

    private final void updateLocation(CellLocation location) {
        Resources r = getResources();
        if (location instanceof GsmCellLocation) {
            GsmCellLocation loc = (GsmCellLocation) location;
            int lac = loc.getLac();
            int cid = loc.getCid();
            this.mLocation.setText(r.getString(R.string.radioInfo_lac) + " = " + (lac == -1 ? "unknown" : Integer.toHexString(lac)) + "   " + r.getString(R.string.radioInfo_cid) + " = " + (cid == -1 ? "unknown" : Integer.toHexString(cid)));
        } else if (location instanceof CdmaCellLocation) {
            String str;
            CdmaCellLocation loc2 = (CdmaCellLocation) location;
            int bid = loc2.getBaseStationId();
            int sid = loc2.getSystemId();
            int nid = loc2.getNetworkId();
            int lat = loc2.getBaseStationLatitude();
            int lon = loc2.getBaseStationLongitude();
            TextView textView = this.mLocation;
            StringBuilder append = new StringBuilder().append("BID = ").append(bid == -1 ? "unknown" : Integer.toHexString(bid)).append("   ").append("SID = ").append(sid == -1 ? "unknown" : Integer.toHexString(sid)).append("   ").append("NID = ");
            if (nid == -1) {
                str = "unknown";
            } else {
                str = Integer.toHexString(nid);
            }
            append = append.append(str).append("\n").append("LAT = ");
            if (lat == -1) {
                str = "unknown";
            } else {
                str = Integer.toHexString(lat);
            }
            append = append.append(str).append("   ").append("LONG = ");
            if (lon == -1) {
                str = "unknown";
            } else {
                str = Integer.toHexString(lon);
            }
            textView.setText(append.append(str).toString());
        } else {
            this.mLocation.setText("unknown");
        }
    }

    private final void updateNeighboringCids(ArrayList<NeighboringCellInfo> cids) {
        StringBuilder sb = new StringBuilder();
        if (cids == null) {
            sb.append("unknown");
        } else if (cids.isEmpty()) {
            sb.append("no neighboring cells");
        } else {
            for (NeighboringCellInfo cell : cids) {
                sb.append(cell.toString()).append(" ");
            }
        }
        this.mNeighboringCids.setText(sb.toString());
    }

    private final void updateCellInfoTv(List<CellInfo> arrayCi) {
        this.mCellInfoValue = arrayCi;
        StringBuilder value = new StringBuilder();
        if (this.mCellInfoValue != null) {
            int index = 0;
            for (CellInfo ci : this.mCellInfoValue) {
                value.append('[');
                value.append(index);
                value.append("]=");
                value.append(ci.toString());
                index++;
                if (index < this.mCellInfoValue.size()) {
                    value.append("\n");
                }
            }
        }
        this.mCellInfo.setText(value.toString());
    }

    private final void updateDcRtInfoTv(DataConnectionRealTimeInfo dcRtInfo) {
        this.mDcRtInfoTv.setText(dcRtInfo.toString());
    }

    private final void updateMessageWaiting() {
        this.mMwi.setText(String.valueOf(this.mMwiValue));
    }

    private final void updateCallRedirect() {
        this.mCfi.setText(String.valueOf(this.mCfiValue));
    }

    private final void updateServiceState() {
        ServiceState serviceState = this.mPhoneStateReceiver.getServiceState();
        int state = serviceState.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = r.getString(R.string.radioInfo_service_in);
                break;
            case 1:
            case 2:
                display = r.getString(R.string.radioInfo_service_emergency);
                break;
            case 3:
                display = r.getString(R.string.radioInfo_service_off);
                break;
        }
        this.gsmState.setText(display);
        if (serviceState.getRoaming()) {
            this.roamingState.setText(R.string.radioInfo_roaming_in);
        } else {
            this.roamingState.setText(R.string.radioInfo_roaming_not);
        }
        this.operatorName.setText(serviceState.getOperatorAlphaLong());
    }

    private final void updatePhoneState() {
        State state = this.mPhoneStateReceiver.getPhoneState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (m3x8c95e0c4()[state.ordinal()]) {
            case 1:
                display = r.getString(R.string.radioInfo_phone_idle);
                break;
            case 2:
                display = r.getString(R.string.radioInfo_phone_offhook);
                break;
            case 3:
                display = r.getString(R.string.radioInfo_phone_ringing);
                break;
        }
        this.callState.setText(display);
    }

    private final void updateDataState() {
        int state = this.mTelephonyManager.getDataState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = r.getString(R.string.radioInfo_data_disconnected);
                break;
            case 1:
                display = r.getString(R.string.radioInfo_data_connecting);
                break;
            case 2:
                display = r.getString(R.string.radioInfo_data_connected);
                break;
            case 3:
                display = r.getString(R.string.radioInfo_data_suspended);
                break;
        }
        this.gprsState.setText(display);
    }

    private final void updateNetworkType() {
        this.network.setText(SystemProperties.get("gsm.network.type", getResources().getString(R.string.radioInfo_unknown)));
    }

    private final void updateProperties() {
        Resources r = getResources();
        String s = this.phone.getDeviceId();
        if (s == null) {
            s = r.getString(R.string.radioInfo_unknown);
        }
        this.mDeviceId.setText(s);
        s = this.phone.getLine1Number();
        if (s == null) {
            s = r.getString(R.string.radioInfo_unknown);
        }
        this.number.setText(s);
    }

    private final void updateDataStats() {
        this.resets.setText(SystemProperties.get("net.gsm.radio-reset", "0"));
        this.attempts.setText(SystemProperties.get("net.gsm.attempt-gprs", "0"));
        this.successes.setText(SystemProperties.get("net.gsm.succeed-gprs", "0"));
        this.sentSinceReceived.setText(SystemProperties.get("net.ppp.reset-by-timeout", "0"));
    }

    private final void updateDataStats2() {
        Resources r = getResources();
        long txPackets = TrafficStats.getMobileTxPackets();
        long rxPackets = TrafficStats.getMobileRxPackets();
        long txBytes = TrafficStats.getMobileTxBytes();
        long rxBytes = TrafficStats.getMobileRxBytes();
        String packets = r.getString(R.string.radioInfo_display_packets);
        String bytes = r.getString(R.string.radioInfo_display_bytes);
        this.sent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
        this.received.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
    }

    private final void pingIpAddr() {
        try {
            if (Runtime.getRuntime().exec("ping -c 1 " + "74.125.47.104").waitFor() == 0) {
                this.mPingIpAddrResult = "Pass";
            } else {
                this.mPingIpAddrResult = "Fail: IP addr not reachable";
            }
        } catch (IOException e) {
            this.mPingIpAddrResult = "Fail: IOException";
        } catch (InterruptedException e2) {
            this.mPingIpAddrResult = "Fail: InterruptedException";
        }
    }

    private final void pingHostname() {
        try {
            if (Runtime.getRuntime().exec("ping -c 1 www.google.com").waitFor() == 0) {
                this.mPingHostnameResult = "Pass";
            } else {
                this.mPingHostnameResult = "Fail: Host unreachable";
            }
        } catch (UnknownHostException e) {
            this.mPingHostnameResult = "Fail: Unknown Host";
        } catch (IOException e2) {
            this.mPingHostnameResult = "Fail: IOException";
        } catch (InterruptedException e3) {
            this.mPingHostnameResult = "Fail: InterruptedException";
        }
    }

    private void refreshSmsc() {
        this.phone.getSmscAddress(this.mHandler.obtainMessage(1005));
    }

    private final void updatePingState() {
        final Handler handler = new Handler();
        this.mPingIpAddrResult = getResources().getString(R.string.radioInfo_unknown);
        this.mPingHostnameResult = getResources().getString(R.string.radioInfo_unknown);
        this.mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);
        this.mPingIpAddr.setText(this.mPingIpAddrResult);
        this.mPingHostname.setText(this.mPingHostnameResult);
        this.mHttpClientTest.setText(this.mHttpClientTestResult);
        final Runnable updatePingResults = new Runnable() {
            public void run() {
                RadioInfo.this.mPingIpAddr.setText(RadioInfo.this.mPingIpAddrResult);
                RadioInfo.this.mPingHostname.setText(RadioInfo.this.mPingHostnameResult);
                RadioInfo.this.mHttpClientTest.setText(RadioInfo.this.mHttpClientTestResult);
            }
        };
        new Thread() {
            public void run() {
                RadioInfo.this.pingIpAddr();
                handler.post(updatePingResults);
            }
        }.start();
        new Thread() {
            public void run() {
                RadioInfo.this.pingHostname();
                handler.post(updatePingResults);
            }
        }.start();
        new Thread() {
            public void run() {
                RadioInfo.this.httpClientTest();
                handler.post(updatePingResults);
            }
        }.start();
    }

    private final void updatePdpList() {
        this.disconnects.setText(new StringBuilder("========DATA=======\n").toString());
    }

    private boolean isImsRegRequired() {
        return SystemProperties.getBoolean("persist.radio.imsregrequired", false);
    }

    private void updateImsRegRequiredState() {
        String buttonText;
        log("updateImsRegRequiredState isImsRegRequired()=" + isImsRegRequired());
        if (isImsRegRequired()) {
            buttonText = getString(R.string.ims_reg_required_off);
        } else {
            buttonText = getString(R.string.ims_reg_required_on);
        }
        this.imsRegRequiredButton.setText(buttonText);
    }

    private boolean isSmsOverImsEnabled() {
        return SystemProperties.getBoolean("persist.radio.imsallowmtsms", false);
    }

    private boolean isImsVoLteProvisioned() {
        if (this.phone == null) {
            return false;
        }
        ImsManager imsManager = ImsManager.getInstance(this.phone.getContext(), this.phone.getSubId());
        return ImsManager.isVolteProvisionedOnDevice(this.phone.getContext());
    }

    private void updateImsVoLteProvisionedState() {
        String buttonText;
        log("updateImsVoLteProvisionedState isImsVoLteProvisioned()=" + isImsVoLteProvisioned());
        if (isImsVoLteProvisioned()) {
            buttonText = getString(R.string.volte_provisioned_flag_off);
        } else {
            buttonText = getString(R.string.volte_provisioned_flag_on);
        }
        this.imsVoLteProvisionedButton.setText(buttonText);
    }

    private void updateSmsOverImsState() {
        String buttonText;
        log("updateSmsOverImsState isSmsOverImsEnabled()=" + isSmsOverImsEnabled());
        if (isSmsOverImsEnabled()) {
            buttonText = getString(R.string.sms_over_ims_off);
        } else {
            buttonText = getString(R.string.sms_over_ims_on);
        }
        this.smsOverImsButton.setText(buttonText);
    }

    private boolean isLteRamDumpEnabled() {
        return SystemProperties.getBoolean("persist.radio.ramdump", false);
    }

    private void updateLteRamDumpState() {
        String buttonText;
        log("updateLteRamDumpState isLteRamDumpEnabled()=" + isLteRamDumpEnabled());
        if (isLteRamDumpEnabled()) {
            buttonText = getString(R.string.lte_ram_dump_off);
        } else {
            buttonText = getString(R.string.lte_ram_dump_on);
        }
        this.lteRamDumpButton.setText(buttonText);
    }

    private void log(String s) {
        Log.d("phone", "[RadioInfo] " + s);
    }
}
