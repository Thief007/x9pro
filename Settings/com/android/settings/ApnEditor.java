package com.android.settings;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.provider.Telephony.Carriers;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.mediatek.apn.ApnTypePreference;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx.Stub;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaApnSetting;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.telephony.TelephonyManagerEx;
import java.util.HashSet;
import java.util.Set;

public class ApnEditor extends InstrumentedPreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
    private static final String TAG = ApnEditor.class.getSimpleName();
    private static String sNotSet;
    private static String[] sProjection = new String[]{"_id", "name", "apn", "proxy", "port", "user", "server", "password", "mmsc", "mcc", "mnc", "numeric", "mmsproxy", "mmsport", "authtype", "type", "protocol", "carrier_enabled", "bearer", "bearer_bitmask", "roaming_protocol", "mvno_type", "mvno_match_data", "sourcetype"};
    private EditTextPreference mApn;
    private IApnSettingsExt mApnExt;
    private ApnTypePreference mApnTypeList;
    private ListPreference mAuthType;
    private int mBearerInitialVal = 0;
    private MultiSelectListPreference mBearerMulti;
    private SwitchPreference mCarrierEnabled;
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            Log.d(ApnEditor.TAG, "background changed apn ");
            ApnEditor.this.mFirstTime = true;
            try {
                ApnEditor.this.stopManagingCursor(ApnEditor.this.mCursor);
                ApnEditor.this.mCursor = ApnEditor.this.managedQuery(ApnEditor.this.mUri, ApnEditor.sProjection, null, null);
                ApnEditor.this.mCursor.moveToFirst();
                ApnEditor.this.fillUi();
            } finally {
                if (ApnEditor.this.mCursor != null) {
                    ApnEditor.this.mCursor.close();
                }
            }
        }
    };
    private String mCurMcc;
    private String mCurMnc;
    private Cursor mCursor;
    private boolean mFirstTime;
    private EditTextPreference mMcc;
    private EditTextPreference mMmsPort;
    private EditTextPreference mMmsProxy;
    private EditTextPreference mMmsc;
    private EditTextPreference mMnc;
    private EditTextPreference mMvnoMatchData;
    private String mMvnoMatchDataStr;
    private ListPreference mMvnoType;
    private String mMvnoTypeStr;
    private EditTextPreference mName;
    private boolean mNewApn;
    private EditTextPreference mPassword;
    private EditTextPreference mPort;
    private ListPreference mProtocol;
    private EditTextPreference mProxy;
    private boolean mReadOnlyMode = false;
    private final BroadcastReceiver mReceiver = new C00381();
    private Resources mRes;
    private ListPreference mRoamingProtocol;
    private EditTextPreference mServer;
    private SimHotSwapHandler mSimHotSwapHandler;
    private int mSourceType = 0;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private Uri mUri;
    private EditTextPreference mUser;

    class C00381 extends BroadcastReceiver {
        C00381() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (intent.getBooleanExtra("state", false)) {
                    Log.d(ApnEditor.TAG, "receiver: ACTION_AIRPLANE_MODE_CHANGED in ApnEditor");
                    ApnEditor.this.exitWithoutSave();
                }
            } else if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                String apnType = intent.getStringExtra("apnType");
                Log.d(ApnEditor.TAG, "Receiver,send MMS status, get type = " + apnType);
                if ("mms".equals(apnType)) {
                    ApnEditor.this.updateScreenEnableState();
                }
            } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                Log.d(ApnEditor.TAG, "receiver: ACTION_SIM_STATE_CHANGED");
                ApnEditor.this.updateScreenEnableState();
            }
        }
    }

    class C00403 implements OnSimHotSwapListener {
        C00403() {
        }

        public void onSimHotSwap() {
            Log.d(ApnEditor.TAG, "onSimHotSwap, finish Activity~~");
            ApnEditor.this.finish();
        }
    }

    class C00414 implements OnClickListener {
        C00414() {
        }

        public void onClick(DialogInterface dialog, int which) {
            if (ApnEditor.this.validateAndSave(false)) {
                ApnEditor.this.finish();
            }
        }
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.apn_editor);
        sNotSet = getResources().getString(R.string.apn_not_set);
        this.mName = (EditTextPreference) findPreference("apn_name");
        this.mApn = (EditTextPreference) findPreference("apn_apn");
        this.mProxy = (EditTextPreference) findPreference("apn_http_proxy");
        this.mPort = (EditTextPreference) findPreference("apn_http_port");
        this.mUser = (EditTextPreference) findPreference("apn_user");
        this.mServer = (EditTextPreference) findPreference("apn_server");
        this.mPassword = (EditTextPreference) findPreference("apn_password");
        this.mMmsProxy = (EditTextPreference) findPreference("apn_mms_proxy");
        this.mMmsPort = (EditTextPreference) findPreference("apn_mms_port");
        this.mMmsc = (EditTextPreference) findPreference("apn_mmsc");
        this.mMcc = (EditTextPreference) findPreference("apn_mcc");
        this.mMnc = (EditTextPreference) findPreference("apn_mnc");
        this.mApnTypeList = (ApnTypePreference) findPreference("apn_type_list");
        this.mApnTypeList.setOnPreferenceChangeListener(this);
        this.mAuthType = (ListPreference) findPreference("auth_type");
        this.mAuthType.setOnPreferenceChangeListener(this);
        this.mProtocol = (ListPreference) findPreference("apn_protocol");
        this.mProtocol.setOnPreferenceChangeListener(this);
        this.mRoamingProtocol = (ListPreference) findPreference("apn_roaming_protocol");
        this.mRoamingProtocol.setOnPreferenceChangeListener(this);
        this.mCarrierEnabled = (SwitchPreference) findPreference("carrier_enabled");
        this.mBearerMulti = (MultiSelectListPreference) findPreference("bearer_multi");
        this.mBearerMulti.setOnPreferenceChangeListener(this);
        this.mMvnoType = (ListPreference) findPreference("mvno_type");
        this.mMvnoType.setOnPreferenceChangeListener(this);
        this.mMvnoMatchData = (EditTextPreference) findPreference("mvno_match_data");
        this.mRes = getResources();
        Intent intent = getIntent();
        String action = intent.getAction();
        this.mSubId = intent.getIntExtra("sub_id", -1);
        this.mFirstTime = icicle == null;
        this.mApnExt = UtilsExt.getApnSettingsPlugin(this);
        if (action.equals("android.intent.action.EDIT")) {
            this.mUri = intent.getData();
            this.mReadOnlyMode = intent.getBooleanExtra("readOnly", false);
            Log.d(TAG, "Read only mode : " + this.mReadOnlyMode);
        } else if (action.equals("android.intent.action.INSERT")) {
            if (this.mFirstTime || icicle.getInt("pos") == 0) {
                this.mUri = getContentResolver().insert(intent.getData(), new ContentValues());
                this.mUri = this.mApnExt.getUriFromIntent(this.mUri, getApplicationContext(), intent);
            } else {
                this.mUri = ContentUris.withAppendedId(Carriers.CONTENT_URI, (long) icicle.getInt("pos"));
            }
            this.mNewApn = true;
            try {
                ITelephonyEx telephony = Stub.asInterface(ServiceManager.getService("phoneEx"));
                this.mMvnoTypeStr = telephony.getMvnoMatchType(this.mSubId);
                this.mMvnoMatchDataStr = telephony.getMvnoPattern(this.mSubId, this.mMvnoTypeStr);
            } catch (RemoteException e) {
                Log.d(TAG, "RemoteException " + e);
            }
            if (this.mUri == null) {
                Log.w(TAG, "Failed to insert new telephony provider into " + getIntent().getData());
                finish();
                return;
            }
            setResult(-1, new Intent().setAction(this.mUri.toString()));
        } else {
            finish();
            return;
        }
        sProjection = this.mApnExt.customizeApnProjection(sProjection);
        this.mApnExt.customizePreference(this.mSubId, getPreferenceScreen());
        this.mCursor = managedQuery(this.mUri, sProjection, null, null);
        this.mCursor.moveToFirst();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        fillUi();
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C00403());
        if (!this.mNewApn) {
            getContentResolver().registerContentObserver(this.mUri, true, this.mContentObserver);
        }
    }

    protected int getMetricsCategory() {
        return 13;
    }

    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        IntentFilter filter = new IntentFilter("android.intent.action.ANY_DATA_STATE");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        registerReceiver(this.mReceiver, filter);
        updateScreenEnableState();
    }

    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(this.mReceiver);
        super.onPause();
    }

    private void fillUi() {
        if (this.mCursor.getCount() == 0) {
            Log.w(TAG, "fillUi(), cursor count is 0, finish~~");
            finish();
            return;
        }
        if (this.mFirstTime) {
            this.mFirstTime = false;
            this.mName.setText(this.mCursor.getString(1));
            this.mApn.setText(this.mCursor.getString(2));
            this.mProxy.setText(this.mCursor.getString(3));
            this.mPort.setText(this.mCursor.getString(4));
            this.mUser.setText(this.mCursor.getString(5));
            this.mServer.setText(this.mCursor.getString(6));
            this.mPassword.setText(this.mCursor.getString(7));
            this.mMmsProxy.setText(this.mCursor.getString(12));
            this.mMmsPort.setText(this.mCursor.getString(13));
            this.mMmsc.setText(this.mCursor.getString(8));
            this.mMcc.setText(this.mCursor.getString(9));
            this.mMnc.setText(this.mCursor.getString(10));
            String strType = this.mCursor.getString(15);
            this.mApnTypeList.setSummary(checkNull(strType));
            this.mApnTypeList.initCheckedState(strType);
            this.mSourceType = this.mCursor.getInt(23);
            if (this.mNewApn) {
                String numeric = updateMccMncForSvlte(this.mSubId, this.mTelephonyManager.getSimOperator(this.mSubId));
                if (numeric != null && numeric.length() > 4) {
                    String mcc = numeric.substring(0, 3);
                    String mnc = numeric.substring(3);
                    this.mMcc.setText(mcc);
                    this.mMnc.setText(mnc);
                    this.mCurMnc = mnc;
                    this.mCurMcc = mcc;
                }
                if ("tethering".equals(getIntent().getStringExtra("apn_type"))) {
                    this.mApnTypeList.setSummary("tethering");
                    this.mApnTypeList.initCheckedState("tethering");
                } else {
                    this.mApnTypeList.setSummary("default");
                    this.mApnTypeList.initCheckedState("default");
                }
                this.mSourceType = 1;
            }
            int authVal = this.mCursor.getInt(14);
            if (authVal != -1) {
                this.mAuthType.setValueIndex(authVal);
            } else {
                this.mAuthType.setValue(null);
            }
            this.mProtocol.setValue(this.mCursor.getString(16));
            this.mRoamingProtocol.setValue(this.mCursor.getString(20));
            this.mCarrierEnabled.setChecked(this.mCursor.getInt(17) == 1);
            this.mBearerInitialVal = this.mCursor.getInt(18);
            HashSet<String> bearers = new HashSet();
            int bearerBitmask = this.mCursor.getInt(19);
            if (bearerBitmask != 0) {
                int i = 1;
                while (bearerBitmask != 0) {
                    if ((bearerBitmask & 1) == 1) {
                        bearers.add("" + i);
                    }
                    bearerBitmask >>= 1;
                    i++;
                }
            } else if (this.mBearerInitialVal == 0) {
                bearers.add("0");
            }
            if (!(this.mBearerInitialVal == 0 || bearers.contains("" + this.mBearerInitialVal))) {
                bearers.add("" + this.mBearerInitialVal);
            }
            this.mBearerMulti.setValues(bearers);
            this.mMvnoType.setValue(this.mCursor.getString(21));
            this.mMvnoMatchData.setEnabled(false);
            this.mMvnoMatchData.setText(this.mCursor.getString(22));
            if (!(!this.mNewApn || this.mMvnoTypeStr == null || this.mMvnoMatchDataStr == null)) {
                this.mMvnoType.setValue(this.mMvnoTypeStr);
                this.mMvnoMatchData.setText(this.mMvnoMatchDataStr);
            }
            this.mApnExt.setPreferenceTextAndSummary(this.mSubId, this.mCursor.getString(sProjection.length - 1));
        }
        this.mName.setSummary(checkNull(this.mName.getText()));
        this.mApn.setSummary(checkNull(this.mApn.getText()));
        this.mProxy.setSummary(checkNull(this.mProxy.getText()));
        this.mPort.setSummary(checkNull(this.mPort.getText()));
        this.mUser.setSummary(checkNull(this.mUser.getText()));
        this.mServer.setSummary(checkNull(this.mServer.getText()));
        this.mPassword.setSummary(starify(this.mPassword.getText()));
        this.mMmsProxy.setSummary(checkNull(this.mMmsProxy.getText()));
        this.mMmsPort.setSummary(checkNull(this.mMmsPort.getText()));
        this.mMmsc.setSummary(checkNull(this.mMmsc.getText()));
        this.mMcc.setSummary(checkNull(this.mMcc.getText()));
        this.mMnc.setSummary(checkNull(this.mMnc.getText()));
        String authVal2 = this.mAuthType.getValue();
        if (authVal2 != null) {
            int authValIndex = Integer.parseInt(authVal2);
            this.mAuthType.setValueIndex(authValIndex);
            this.mAuthType.setSummary(this.mRes.getStringArray(R.array.apn_auth_entries)[authValIndex]);
        } else {
            this.mAuthType.setSummary(sNotSet);
        }
        this.mProtocol.setSummary(checkNull(protocolDescription(this.mProtocol.getValue(), this.mProtocol)));
        this.mRoamingProtocol.setSummary(checkNull(protocolDescription(this.mRoamingProtocol.getValue(), this.mRoamingProtocol)));
        this.mBearerMulti.setSummary(checkNull(bearerMultiDescription(this.mBearerMulti.getValues())));
        this.mMvnoType.setSummary(checkNull(mvnoDescription(this.mMvnoType.getValue())));
        this.mMvnoMatchData.setSummary(checkNull(this.mMvnoMatchData.getText()));
        if (getResources().getBoolean(R.bool.config_allow_edit_carrier_enabled)) {
            this.mCarrierEnabled.setEnabled(true);
        } else {
            this.mCarrierEnabled.setEnabled(false);
        }
    }

    private String protocolDescription(String raw, ListPreference protocol) {
        int protocolIndex = protocol.findIndexOfValue(raw);
        if (protocolIndex == -1) {
            return null;
        }
        try {
            return this.mRes.getStringArray(R.array.apn_protocol_entries)[protocolIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private String bearerMultiDescription(Set<String> raw) {
        String[] values = this.mRes.getStringArray(R.array.bearer_entries);
        StringBuilder retVal = new StringBuilder();
        boolean first = true;
        for (String bearer : raw) {
            int bearerIndex = this.mBearerMulti.findIndexOfValue(bearer);
            if (first) {
                try {
                    retVal.append(values[bearerIndex]);
                    first = false;
                } catch (ArrayIndexOutOfBoundsException e) {
                }
            } else {
                retVal.append(", ").append(values[bearerIndex]);
            }
        }
        String val = retVal.toString();
        if (TextUtils.isEmpty(val)) {
            return null;
        }
        return val;
    }

    private String mvnoDescription(String newValue) {
        int mvnoIndex = this.mMvnoType.findIndexOfValue(newValue);
        String oldValue = this.mMvnoType.getValue();
        if (mvnoIndex == -1) {
            return null;
        }
        String[] values = this.mRes.getStringArray(R.array.ext_mvno_type_entries);
        if (values[mvnoIndex].equals("None")) {
            this.mMvnoMatchData.setEnabled(false);
        } else {
            this.mMvnoMatchData.setEnabled(true);
        }
        if (!(newValue == null || newValue.equals(oldValue))) {
            if (values[mvnoIndex].equals("SPN")) {
                this.mMvnoMatchData.setText(this.mTelephonyManager.getSimOperatorName());
            } else if (values[mvnoIndex].equals("IMSI")) {
                this.mMvnoMatchData.setText(this.mTelephonyManager.getSimOperator(this.mSubId) + "x");
            } else if (values[mvnoIndex].equals("GID")) {
                this.mMvnoMatchData.setText(this.mTelephonyManager.getGroupIdLevel1());
            }
        }
        try {
            return values[mvnoIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if ("auth_type".equals(key)) {
            try {
                int index = Integer.parseInt((String) newValue);
                this.mAuthType.setValueIndex(index);
                this.mAuthType.setSummary(this.mRes.getStringArray(R.array.apn_auth_entries)[index]);
            } catch (NumberFormatException e) {
                return false;
            }
        } else if ("apn_protocol".equals(key)) {
            protocol = protocolDescription((String) newValue, this.mProtocol);
            if (protocol == null) {
                return false;
            }
            this.mProtocol.setSummary(protocol);
            this.mProtocol.setValue((String) newValue);
        } else if ("apn_roaming_protocol".equals(key)) {
            protocol = protocolDescription((String) newValue, this.mRoamingProtocol);
            if (protocol == null) {
                return false;
            }
            this.mRoamingProtocol.setSummary(protocol);
            this.mRoamingProtocol.setValue((String) newValue);
        } else if ("bearer_multi".equals(key)) {
            String bearer = bearerMultiDescription((Set) newValue);
            if (bearer == null) {
                return false;
            }
            this.mBearerMulti.setValues((Set) newValue);
            this.mBearerMulti.setSummary(bearer);
        } else if ("mvno_type".equals(key)) {
            String mvno = mvnoDescription((String) newValue);
            if (mvno == null) {
                return false;
            }
            this.mMvnoType.setValue((String) newValue);
            this.mMvnoType.setSummary(mvno);
        } else if ("apn_type_list".equals(key)) {
            this.mApnTypeList.setSummary(checkNull(this.mApnTypeList.getTypeString()));
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (this.mReadOnlyMode) {
            return true;
        }
        if (!(this.mNewApn || this.mSourceType == 0)) {
            menu.add(0, 1, 0, R.string.menu_delete).setIcon(R.drawable.ic_menu_delete);
        }
        menu.add(0, 2, 0, R.string.menu_save).setIcon(17301582);
        menu.add(0, 3, 0, R.string.menu_cancel).setIcon(17301560);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                deleteApn();
                return true;
            case 2:
                if (this.mSourceType == 0) {
                    showDialog(1);
                } else if (validateAndSave(false)) {
                    finish();
                }
                return true;
            case 3:
                if (this.mNewApn && this.mUri != null) {
                    getContentResolver().delete(this.mUri, null, null);
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
                if (validateAndSave(false)) {
                    finish();
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        if (validateAndSave(true) && this.mCursor != null && this.mCursor.getCount() != 0) {
            icicle.putInt("pos", this.mCursor.getInt(0));
        }
    }

    private boolean validateAndSave(boolean force) {
        String name = checkNotSet(this.mName.getText());
        String apn = checkNotSet(this.mApn.getText());
        String mcc = checkNotSet(this.mMcc.getText());
        String mnc = checkNotSet(this.mMnc.getText());
        if (getErrorMsg() != null && !force) {
            showDialog(0);
            return false;
        } else if (!this.mCursor.moveToFirst() && !this.mNewApn) {
            Log.w(TAG, "Could not go to the first row in the Cursor when saving data.");
            return false;
        } else if (!force || !this.mNewApn || name.length() >= 1 || apn.length() >= 1 || this.mUri == null) {
            int bearerVal;
            ContentValues values = new ContentValues();
            String str = "name";
            if (name.length() < 1) {
                name = getResources().getString(R.string.untitled_apn);
            }
            values.put(str, name);
            values.put("apn", apn);
            values.put("proxy", checkNotSet(this.mProxy.getText()));
            values.put("port", checkNotSet(this.mPort.getText()));
            values.put("mmsproxy", checkNotSet(this.mMmsProxy.getText()));
            values.put("mmsport", checkNotSet(this.mMmsPort.getText()));
            values.put("user", checkNotSet(this.mUser.getText()));
            values.put("server", checkNotSet(this.mServer.getText()));
            values.put("password", checkNotSet(this.mPassword.getText()));
            values.put("mmsc", checkNotSet(this.mMmsc.getText()));
            String authVal = this.mAuthType.getValue();
            if (authVal != null) {
                values.put("authtype", Integer.valueOf(Integer.parseInt(authVal)));
            }
            values.put("protocol", checkNotSet(this.mProtocol.getValue()));
            values.put("roaming_protocol", checkNotSet(this.mRoamingProtocol.getValue()));
            values.put("type", checkNotSet(this.mApnTypeList.getTypeString()));
            values.put("mcc", mcc);
            values.put("mnc", mnc);
            values.put("numeric", mcc + mnc);
            if (this.mCurMnc != null && this.mCurMcc != null && this.mCurMnc.equals(mnc) && this.mCurMcc.equals(mcc)) {
                values.put("current", Integer.valueOf(1));
            }
            int bearerBitmask = 0;
            for (String bearer : this.mBearerMulti.getValues()) {
                if (Integer.parseInt(bearer) == 0) {
                    bearerBitmask = 0;
                    break;
                }
                bearerBitmask |= ServiceState.getBitmaskForTech(Integer.parseInt(bearer));
            }
            values.put("bearer_bitmask", Integer.valueOf(bearerBitmask));
            if (bearerBitmask == 0 || this.mBearerInitialVal == 0) {
                bearerVal = 0;
            } else if (ServiceState.bitmaskHasTech(bearerBitmask, this.mBearerInitialVal)) {
                bearerVal = this.mBearerInitialVal;
            } else {
                bearerVal = 0;
            }
            values.put("bearer", Integer.valueOf(bearerVal));
            values.put("mvno_type", checkNotSet(this.mMvnoType.getValue()));
            values.put("mvno_match_data", checkNotSet(this.mMvnoMatchData.getText()));
            values.put("carrier_enabled", Integer.valueOf(this.mCarrierEnabled.isChecked() ? 1 : 0));
            values.put("sourcetype", Integer.valueOf(this.mSourceType));
            this.mApnExt.saveApnValues(values);
            if (this.mUri == null) {
                Log.i(TAG, "former inserted URI was already deleted, insert a new one");
                this.mUri = getContentResolver().insert(getIntent().getData(), new ContentValues());
            } else {
                getContentResolver().update(this.mUri, values, null, null);
            }
            return true;
        } else {
            getContentResolver().delete(this.mUri, null, null);
            this.mUri = null;
            return false;
        }
    }

    private String getErrorMsg() {
        String name = checkNotSet(this.mName.getText());
        String apn = checkNotSet(this.mApn.getText());
        String mcc = checkNotSet(this.mMcc.getText());
        String mnc = checkNotSet(this.mMnc.getText());
        String apnType = this.mApnTypeList.getTypeString();
        if (name.length() < 1) {
            return this.mRes.getString(R.string.error_name_empty);
        }
        if ((apnType == null || !apnType.contains("ia")) && apn.length() < 1) {
            return this.mRes.getString(R.string.error_apn_empty);
        }
        if (mcc.length() != 3) {
            return this.mRes.getString(R.string.error_mcc_not3);
        }
        if ((mnc.length() & 65534) != 2) {
            return this.mRes.getString(R.string.error_mnc_not23);
        }
        return null;
    }

    protected Dialog onCreateDialog(int id) {
        if (id != 0) {
            return id == 1 ? new Builder(this).setIcon(17301543).setTitle(R.string.error_title).setMessage(getString(R.string.apn_predefine_change_dialog_notice)).setPositiveButton(17039370, new C00414()).setNegativeButton(R.string.cancel, null).create() : super.onCreateDialog(id);
        } else {
            return new Builder(this).setTitle(R.string.error_title).setPositiveButton(17039370, null).setMessage(getErrorMsg()).create();
        }
    }

    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (id == 0) {
            String msg = getErrorMsg();
            if (msg != null) {
                ((AlertDialog) dialog).setMessage(msg);
            }
        }
    }

    private void deleteApn() {
        if (this.mUri != null) {
            getContentResolver().delete(this.mUri, null, null);
        }
        finish();
    }

    private String starify(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        }
        char[] password = new char[value.length()];
        for (int i = 0; i < password.length; i++) {
            password[i] = '*';
        }
        return new String(password);
    }

    private String checkNull(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        }
        return value;
    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        }
        return value;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        if (pref.equals(this.mPassword)) {
            pref.setSummary(starify(sharedPreferences.getString(key, "")));
        } else if (pref.equals(this.mCarrierEnabled)) {
            pref.setSummary(checkNull(String.valueOf(sharedPreferences.getBoolean(key, true))));
        } else if (pref.equals(this.mPort)) {
            String portStr = sharedPreferences.getString(key, "");
            if (!portStr.equals("")) {
                try {
                    int portNum = Integer.parseInt(portStr);
                    if (portNum > 65535 || portNum <= 0) {
                        Toast.makeText(this, getString(R.string.apn_port_warning), 1).show();
                        ((EditTextPreference) pref).setText("");
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, getString(R.string.apn_port_warning), 1).show();
                    ((EditTextPreference) pref).setText("");
                }
            }
            pref.setSummary(checkNull(sharedPreferences.getString(key, "")));
        } else if (!pref.equals(this.mCarrierEnabled) && !pref.equals(this.mBearerMulti)) {
            pref.setSummary(checkNull(sharedPreferences.getString(key, "")));
        }
    }

    public void onDestroy() {
        if (!this.mNewApn) {
            getContentResolver().unregisterContentObserver(this.mContentObserver);
        }
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        super.onDestroy();
        this.mApnExt.onDestroy();
    }

    private void exitWithoutSave() {
        if (this.mNewApn && this.mUri != null) {
            getContentResolver().delete(this.mUri, null, null);
        }
        finish();
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        super.onMenuOpened(featureId, menu);
        if (menu != null) {
            boolean z;
            if (!isSimReadyAndRadioOn() || this.mReadOnlyMode) {
                z = false;
            } else {
                z = this.mApnExt.getScreenEnableState(this.mSubId, this);
            }
            menu.setGroupEnabled(0, z);
        }
        return true;
    }

    private void updateScreenEnableState() {
        boolean z;
        boolean enable = isSimReadyAndRadioOn();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (!enable || this.mReadOnlyMode) {
            z = false;
        } else {
            z = this.mApnExt.getScreenEnableState(this.mSubId, this);
        }
        preferenceScreen.setEnabled(z);
        this.mApnExt.setApnTypePreferenceState(this.mApnTypeList, this.mApnTypeList.getTypeString());
        this.mApnExt.updateFieldsStatus(this.mSubId, this.mSourceType, getPreferenceScreen());
        this.mApnExt.setMvnoPreferenceState(this.mMvnoType, this.mMvnoMatchData);
    }

    private boolean isSimReadyAndRadioOn() {
        boolean simReady = 5 == TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotId(this.mSubId));
        boolean airplaneModeEnabled = System.getInt(getContentResolver(), "airplane_mode_on", -1) == 1;
        boolean isMultiSimMode = System.getInt(getContentResolver(), "msim_mode_setting", -1) != 0;
        boolean z = (airplaneModeEnabled || !simReady) ? false : isMultiSimMode;
        Log.d(TAG, "updateScreenEnableState(), subId = " + this.mSubId + " ,airplaneModeEnabled = " + airplaneModeEnabled + " ,simReady = " + simReady + " , isMultiSimMode = " + isMultiSimMode);
        return z;
    }

    private String updateMccMncForSvlte(int subId, String iccNumeric) {
        String apnNumeric = iccNumeric;
        if (!FeatureOption.MTK_SVLTE_SUPPORT) {
            return apnNumeric;
        }
        int c2kSlot = CdmaUtils.getExternalModemSlot();
        int svlteRatMode = Global.getInt(getContentResolver(), TelephonyManagerEx.getDefault().getCdmaRatModeKey(subId), 0);
        if (SubscriptionManager.getSlotId(subId) == c2kSlot && svlteRatMode != 2 && iccNumeric != null && (iccNumeric.equals("46003") || iccNumeric.equals("46011"))) {
            String operatorNumeric = SystemProperties.get("gsm.operator.numeric");
            if (operatorNumeric != null && operatorNumeric.contains("46011")) {
                apnNumeric = "46011";
            } else if (operatorNumeric == null || !operatorNumeric.contains("46003")) {
                apnNumeric = TelephonyManager.getDefault().getNetworkOperatorForSubscription(subId);
                Log.d(TAG, "plmnNumeric not contains 46003 or 46011, as ROAMING mumeric: " + apnNumeric);
            } else if (CdmaApnSetting.getNetworkType(subId) == 14) {
                apnNumeric = "46011";
            } else {
                apnNumeric = "46003";
            }
            Log.d(TAG, "updateMccMncForSvlte iccNumeric=" + iccNumeric + ", apnNumeric=" + apnNumeric);
        }
        return apnNumeric;
    }
}
