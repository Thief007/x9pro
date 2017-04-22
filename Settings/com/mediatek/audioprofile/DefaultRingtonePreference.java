package com.mediatek.audioprofile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IAudioProfileExt;

public class DefaultRingtonePreference extends RingtonePreference {
    private IAudioProfileExt mExt;
    private String mKey;
    private boolean mNoNeedSIMSelector = false;
    private final AudioProfileManager mProfileManager;
    private long mSimId = -1;
    private String mStreamType;

    public void setSimId(long simId) {
        Editor editor = getContext().getSharedPreferences("DefaultRingtonePreference", 0).edit();
        editor.putLong("SimIdValume", simId);
        editor.commit();
        this.mSimId = simId;
        Log.d("@M_Settings/Rt_Pref", "setSimId   simId= " + simId + " this.mSimId = " + this.mSimId);
    }

    public DefaultRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mProfileManager = (AudioProfileManager) context.getSystemService("audioprofile");
        this.mExt = UtilsExt.getAudioProfilePlugin(context);
    }

    public void setProfile(String key) {
        this.mKey = key;
    }

    public void setStreamType(String streamType) {
        this.mStreamType = streamType;
    }

    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        ringtonePickerIntent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", false);
        if (this.mStreamType.equals("RING")) {
            ringtonePickerIntent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", false);
        }
        this.mExt.setRingtonePickerParams(ringtonePickerIntent);
    }

    protected void onSaveRingtone(Uri ringtoneUri) {
        this.mSimId = getContext().getSharedPreferences("DefaultRingtonePreference", 0).getLong("SimIdValume", -1);
        this.mProfileManager.setRingtoneUri(this.mKey, getRingtoneType(), this.mSimId, ringtoneUri);
    }

    protected Uri onRestoreRingtone() {
        int type = getRingtoneType();
        Log.d("@M_Settings/Rt_Pref", "onRestoreRingtone: type = " + type + " mKey = " + this.mKey + "  mSimId= " + this.mSimId);
        Uri uri = this.mProfileManager.getRingtoneUri(this.mKey, type, this.mSimId);
        Log.d("@M_Settings/Rt_Pref", "onRestoreRingtone: uri = " + (uri == null ? "null" : uri.toString()));
        if (!RingtoneManager.isRingtoneExist(getContext(), uri)) {
            Log.d("yjp", " not   is exist ");
            String uriString = null;
            if (type == 1) {
                uriString = System.getString(getContext().getContentResolver(), "mtk_audioprofile_default_ringtone");
            } else if (type == 32) {
                uriString = System.getString(getContext().getContentResolver(), "mtk_audioprofile_default_ringtone_sim2");
            }
            if (uriString != null) {
                return Uri.parse(uriString);
            }
        }
        return uri;
    }

    protected void onClick() {
        TelephonyManager mTeleManager = (TelephonyManager) getContext().getSystemService("phone");
        int simNum = SubscriptionManager.from(getContext()).getActiveSubscriptionInfoCount();
        Log.d("@M_Settings/Rt_Pref", "onClick  : isNoNeedSIMSelector = " + isNoNeedSIMSelector() + "simNum <= SINGLE_SIMCARD: simNum = " + simNum);
        if (FeatureOption.MTK_MULTISIM_RINGTONE_SUPPORT && simNum == 1) {
            setSimId((long) SubscriptionManager.from(getContext()).getActiveSubscriptionIdList()[0]);
        }
        if (isNoNeedSIMSelector() || simNum <= 1) {
            super.onClick();
        }
    }

    void simSelectorOnClick() {
        Log.d("@M_Settings/Rt_Pref", "onClick  : simSelectorOnClick  ");
        super.onClick();
    }

    public boolean isNoNeedSIMSelector() {
        return this.mNoNeedSIMSelector;
    }

    public void setNoNeedSIMSelector(boolean mNoNeedSIMSelector) {
        this.mNoNeedSIMSelector = mNoNeedSIMSelector;
    }
}
