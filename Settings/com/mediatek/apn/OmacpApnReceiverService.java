package com.mediatek.apn;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.Carriers;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ITelephonyEx.Stub;
import com.mediatek.settings.UtilsExt;
import java.util.ArrayList;
import java.util.HashMap;

public class OmacpApnReceiverService extends IntentService {
    private static int sAuthType = -1;
    private String mApn;
    private String mApnId;
    private String mAuthType;
    private ArrayList<Intent> mIntentList;
    private boolean mIsMmsApn = false;
    private String mMcc;
    private String mMmsPort;
    private String mMmsProxy;
    private String mMmsc;
    private String mMnc;
    private String mName;
    private String mNapId;
    private String mNumeric;
    private String mPassword;
    private String mPort;
    private Uri mPreferedUri;
    private String mProxy;
    private String mProxyId;
    private boolean mResult = true;
    private String mServer;
    private int mSubId;
    private String mType;
    private Uri mUri;
    private String mUserName;

    public OmacpApnReceiverService() {
        super("OmacpApnReceiverService");
    }

    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d("OmacpApnReceiverService", "get action = " + action);
        if ("com.mediatek.apn.action.start.omacpservice".equals(action)) {
            this.mIntentList = ((Intent) intent.getParcelableExtra("android.intent.extra.INTENT")).getParcelableArrayListExtra("apn_setting_intent");
            if (this.mIntentList == null) {
                this.mResult = false;
                sendFeedback(this);
                Log.e("OmacpApnReceiverService", "mIntentList == null");
                return;
            }
            int sizeIntent = this.mIntentList.size();
            Log.d("OmacpApnReceiverService", "apn list size is " + sizeIntent);
            if (sizeIntent <= 0) {
                this.mResult = false;
                sendFeedback(this);
                Log.e("OmacpApnReceiverService", "Intent list size is wrong");
            } else if (initState((Intent) this.mIntentList.get(0))) {
                this.mUri = Carriers.CONTENT_URI;
                Log.d("OmacpApnReceiverService", "mUri = " + this.mUri + " mNumeric = " + this.mNumeric + " mPreferedUri = " + this.mPreferedUri);
                int k = 0;
                while (this.mResult && k < sizeIntent) {
                    extractAPN((Intent) this.mIntentList.get(k), this);
                    ContentValues values = new ContentValues();
                    validateProfile(values);
                    updateApn(this, this.mUri, this.mApn, this.mApnId, this.mName, values, this.mNumeric, this.mPreferedUri);
                    k++;
                }
                sendFeedback(this);
            } else {
                sendFeedback(this);
                Log.e("OmacpApnReceiverService", "Can not get MCC+MNC");
            }
        }
    }

    private void sendFeedback(Context context) {
        Intent it = new Intent();
        it.setAction("com.mediatek.omacp.settings.result");
        it.putExtra("appId", "apn");
        it.putExtra("result", this.mResult);
        context.sendBroadcast(it);
    }

    private void validateProfile(ContentValues values) {
        values.put(ApnUtils.PROJECTION[1], this.mName);
        values.put(ApnUtils.PROJECTION[2], ApnUtils.checkNotSet(this.mApn));
        values.put(ApnUtils.PROJECTION[3], ApnUtils.checkNotSet(this.mProxy));
        values.put(ApnUtils.PROJECTION[4], ApnUtils.checkNotSet(this.mPort));
        values.put(ApnUtils.PROJECTION[5], ApnUtils.checkNotSet(this.mUserName));
        values.put(ApnUtils.PROJECTION[6], ApnUtils.checkNotSet(this.mServer));
        values.put(ApnUtils.PROJECTION[7], ApnUtils.checkNotSet(this.mPassword));
        values.put(ApnUtils.PROJECTION[8], ApnUtils.checkNotSet(this.mMmsc));
        values.put(ApnUtils.PROJECTION[9], this.mMcc);
        values.put(ApnUtils.PROJECTION[10], this.mMnc);
        values.put(ApnUtils.PROJECTION[12], ApnUtils.checkNotSet(this.mMmsProxy));
        values.put(ApnUtils.PROJECTION[13], ApnUtils.checkNotSet(this.mMmsPort));
        values.put(ApnUtils.PROJECTION[14], Integer.valueOf(sAuthType));
        values.put(ApnUtils.PROJECTION[15], ApnUtils.checkNotSet(this.mType));
        values.put(ApnUtils.PROJECTION[16], Integer.valueOf(2));
        values.put(ApnUtils.PROJECTION[11], this.mNumeric);
    }

    private boolean verifyMccMnc() {
        if (this.mNumeric == null || this.mNumeric.length() <= 4) {
            this.mResult = false;
            Log.d("OmacpApnReceiverService", "mcc&mnc is wrong , set mResult = false");
        } else {
            String mcc = this.mNumeric.substring(0, 3);
            String mnc = this.mNumeric.substring(3);
            this.mMcc = mcc;
            this.mMnc = mnc;
            Log.d("OmacpApnReceiverService", "mcc&mnc is right , mMcc = " + this.mMcc + " mMnc = " + this.mMnc);
        }
        return this.mResult;
    }

    private void getPort(Intent intent) {
        this.mPort = null;
        ArrayList<HashMap<String, String>> portList = (ArrayList) intent.getExtra("PORT");
        if (portList != null && portList.size() > 0) {
            this.mPort = (String) ((HashMap) portList.get(0)).get("PORTNBR");
        }
    }

    private void getNapAuthInfo(Intent intent) {
        this.mUserName = null;
        this.mPassword = null;
        this.mAuthType = null;
        sAuthType = -1;
        ArrayList<HashMap<String, String>> napAuthInfo = (ArrayList) intent.getExtra("NAPAUTHINFO");
        if (napAuthInfo != null && napAuthInfo.size() > 0) {
            HashMap<String, String> napAuthInfoMap = (HashMap) napAuthInfo.get(0);
            this.mUserName = (String) napAuthInfoMap.get("AUTHNAME");
            this.mPassword = (String) napAuthInfoMap.get("AUTHSECRET");
            this.mAuthType = (String) napAuthInfoMap.get("AUTHTYPE");
            if (this.mAuthType == null) {
                return;
            }
            if ("PAP".equalsIgnoreCase(this.mAuthType)) {
                sAuthType = 1;
            } else if ("CHAP".equalsIgnoreCase(this.mAuthType)) {
                sAuthType = 2;
            } else {
                sAuthType = 3;
            }
        }
    }

    private void extractAPN(Intent intent, Context context) {
        this.mName = intent.getStringExtra("NAP-NAME");
        if (this.mName == null || this.mName.length() < 1) {
            this.mName = context.getResources().getString(R.string.untitled_apn);
        }
        this.mApn = intent.getStringExtra("NAP-ADDRESS");
        this.mProxy = intent.getStringExtra("PXADDR");
        getPort(intent);
        getNapAuthInfo(intent);
        this.mServer = intent.getStringExtra("SERVER");
        this.mMmsc = intent.getStringExtra("MMSC");
        this.mMmsProxy = intent.getStringExtra("MMS-PROXY");
        this.mMmsPort = intent.getStringExtra("MMS-PORT");
        this.mType = intent.getStringExtra("APN-TYPE");
        this.mApnId = intent.getStringExtra("APN-ID");
        this.mNapId = intent.getStringExtra("NAPID");
        this.mProxyId = intent.getStringExtra("PROXY-ID");
        this.mIsMmsApn = "mms".equalsIgnoreCase(this.mType);
        Log.d("OmacpApnReceiverService", "extractAPN: mName: " + this.mName + " | mApn: " + this.mApn + " | mProxy: " + this.mProxy + " | mServer: " + this.mServer + " | mMmsc: " + this.mMmsc + " | mMmsProxy: " + this.mMmsProxy + " | mMmsPort: " + this.mMmsPort + " | mType: " + this.mType + " | mApnId: " + this.mApnId + " | mNapId: " + this.mNapId + " | mMmsPort: " + this.mMmsPort + " | mProxyId: " + this.mProxyId + " | mIsMmsApn: " + this.mIsMmsApn);
    }

    private boolean setCurrentApn(Context context, long apnToUseId, Uri preferedUri) {
        int row = 0;
        ContentValues values = new ContentValues();
        values.put("apn_id", Long.valueOf(apnToUseId));
        try {
            row = context.getContentResolver().update(preferedUri, values, null, null);
            Log.d("OmacpApnReceiverService", "update preferred uri ,row = " + row);
        } catch (SQLException e) {
            Log.d("OmacpApnReceiverService", "SetCurrentApn SQLException happened!");
        }
        if (row > 0) {
            return true;
        }
        return false;
    }

    private void updateApn(Context context, Uri uri, String apn, String apnId, String name, ContentValues values, String numeric, Uri peferredUri) {
        long replaceNum = UtilsExt.getApnSettingsPlugin(context).replaceApn(replaceApn(context, peferredUri, apn, apnId, name, values, numeric), context, peferredUri, apn, name, values, numeric);
        Log.d("OmacpApnReceiverService", "replace number = " + replaceNum);
        long insertNum = replaceNum;
        if (replaceNum == -1) {
            try {
                Uri newRow = context.getContentResolver().insert(uri, addMVNOItem(values));
                if (newRow != null) {
                    Log.d("OmacpApnReceiverService", "uri = " + newRow);
                    if (newRow.getPathSegments().size() == 2) {
                        insertNum = Long.parseLong(newRow.getLastPathSegment());
                        Log.d("OmacpApnReceiverService", "insert row id = " + insertNum);
                    }
                }
            } catch (SQLException e) {
                Log.d("OmacpApnReceiverService", "insert SQLException happened!");
                this.mResult = false;
            }
        }
        Log.d("OmacpApnReceiverService", "insert number = " + insertNum);
        if (this.mIsMmsApn) {
            if (insertNum == -1) {
                this.mResult = false;
                Log.d("OmacpApnReceiverService", "mms, insertNum is APN_NO_UPDATE ,mResult = false");
            }
        } else if (insertNum == -1) {
            this.mResult = false;
            Log.d("OmacpApnReceiverService", "not mms, insertNum is APN_NO_UPDATE, mResult = false");
        } else if (insertNum == 0) {
            this.mResult = true;
            Log.d("OmacpApnReceiverService", "not mms, insertNum is APN_EXIST, mResult = true");
        } else {
            this.mResult = setCurrentApn(context, insertNum, peferredUri);
            Log.d("OmacpApnReceiverService", "set current apn result, mResult = " + this.mResult);
        }
    }

    ContentValues addMVNOItem(ContentValues values) {
        try {
            ITelephonyEx telephony = Stub.asInterface(ServiceManager.getService("phoneEx"));
            String mvnoType = telephony.getMvnoMatchType(this.mSubId);
            String mvnoPattern = telephony.getMvnoPattern(this.mSubId, mvnoType);
            values.put("mvno_type", ApnUtils.checkNotSet(mvnoType));
            values.put("mvno_match_data", ApnUtils.checkNotSet(mvnoPattern));
        } catch (RemoteException e) {
            Log.e("OmacpApnReceiverService", "RemoteException " + e);
        }
        return values;
    }

    private boolean initState(Intent intent) {
        this.mSubId = intent.getIntExtra("subId", -1);
        if (this.mSubId == -1) {
            Log.w("OmacpApnReceiverService", "Need to check reason not pass subId");
            this.mSubId = SubscriptionManager.getDefaultSubId();
        }
        this.mNumeric = TelephonyManager.getDefault().getSimOperator(this.mSubId);
        this.mPreferedUri = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers/preferapn/subId/"), (long) this.mSubId);
        Log.d("OmacpApnReceiverService", "initState: mSimId: " + this.mSubId + " | mNumeric: " + this.mNumeric + " | mPreferedUri: " + this.mPreferedUri);
        return verifyMccMnc();
    }

    public long replaceApn(Context context, Uri uri, String apn, String apnId, String name, ContentValues values, String numeric) {
        long numReplaced = -1;
        String where = "numeric=\"" + numeric + "\"" + " and omacpid<>''";
        Log.d("OmacpApnReceiverService", "name " + name + " numeric = " + numeric + " apnId = " + apnId);
        Cursor cursor = null;
        try {
            Uri uri2 = uri;
            cursor = context.getContentResolver().query(uri2, new String[]{"_id", "omacpid"}, where, null, "name ASC");
            if (cursor == null || cursor.getCount() == 0) {
                Log.d("OmacpApnReceiverService", "cursor is null , or cursor.getCount() == 0 return");
                return -1;
            }
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Log.d("OmacpApnReceiverService", "apnId " + apnId + " getApnId = " + cursor.getString(1));
                if (apnId.equals(cursor.getString(1))) {
                    numReplaced = 0;
                    break;
                }
                cursor.moveToNext();
            }
            if (cursor != null) {
                cursor.close();
            }
            return numReplaced;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
