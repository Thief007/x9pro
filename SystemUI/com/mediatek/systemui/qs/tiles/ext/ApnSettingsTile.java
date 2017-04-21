package com.mediatek.systemui.qs.tiles.ext;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.QSTile.State;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.util.SIMHelper;

public class ApnSettingsTile extends QSTile<State> {
    private static final Intent APN_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$ApnSettingsActivity"));
    private static final boolean DEBUG = true;
    private static final String TAG = "ApnSettingsTile";
    private boolean mApnSettingsEnabled = false;
    private final Icon mApnStateIcon = new QsIconWrapper(this.mApnStateIconWrapper);
    private final IconIdWrapper mApnStateIconWrapper = new IconIdWrapper();
    private String mApnStateLabel = "";
    private boolean mIsAirlineMode = false;
    private boolean mIsWifiOnly;
    private boolean mListening;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d(ApnSettingsTile.TAG, "onCallStateChanged call state is " + state);
            switch (state) {
                case 0:
                    ApnSettingsTile.this.updateState();
                    return;
                default:
                    return;
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(ApnSettingsTile.TAG, "onReceive(), action: " + action);
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                boolean enabled = intent.getBooleanExtra("state", false);
                Log.d(ApnSettingsTile.TAG, "onReceive(), airline mode changed: state is " + enabled);
                ApnSettingsTile.this.mIsAirlineMode = enabled;
                ApnSettingsTile.this.updateState();
            } else if (action.equals("android.intent.action.ACTION_EF_CSP_CONTENT_NOTIFY") || action.equals("android.intent.action.MSIM_MODE") || action.equals("android.intent.action.ACTION_MD_TYPE_CHANGE") || action.equals("mediatek.intent.action.LOCATED_PLMN_CHANGED") || action.equals("android.intent.action.ACTION_SET_PHONE_RAT_FAMILY_DONE") || action.equals("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE") || action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                ApnSettingsTile.this.updateState();
            }
        }
    };
    private final SubscriptionManager mSubscriptionManager = SubscriptionManager.from(this.mContext);
    private final UserManager mUm = ((UserManager) this.mContext.getSystemService(FeatureOptionUtils.BUILD_TYPE_USER));

    public ApnSettingsTile(Host host) {
        boolean z;
        super(host);
        if (((ConnectivityManager) this.mContext.getSystemService("connectivity")).isNetworkSupported(0)) {
            z = false;
        } else {
            z = DEBUG;
        }
        this.mIsWifiOnly = z;
        updateState();
    }

    protected State newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        Log.d(TAG, "setListening(), listening = " + listening);
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                IntentFilter mIntentFilter = new IntentFilter();
                mIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
                mIntentFilter.addAction("android.intent.action.ACTION_EF_CSP_CONTENT_NOTIFY");
                mIntentFilter.addAction("android.intent.action.MSIM_MODE");
                mIntentFilter.addAction("android.intent.action.ACTION_MD_TYPE_CHANGE");
                mIntentFilter.addAction("mediatek.intent.action.LOCATED_PLMN_CHANGED");
                mIntentFilter.addAction("android.intent.action.ACTION_SET_PHONE_RAT_FAMILY_DONE");
                mIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
                mIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
                this.mContext.registerReceiver(this.mReceiver, mIntentFilter);
                TelephonyManager.getDefault().listen(this.mPhoneStateListener, 32);
            } else {
                this.mContext.unregisterReceiver(this.mReceiver);
                TelephonyManager.getDefault().listen(this.mPhoneStateListener, 0);
            }
        }
    }

    public int getMetricsCategory() {
        return 111;
    }

    protected void handleClick() {
        Log.d(TAG, "handleClick(), mApnSettingsEnabled = " + this.mApnSettingsEnabled);
        if (this.mApnSettingsEnabled) {
            APN_SETTINGS.putExtra("sub_id", SubscriptionManager.getDefaultDataSubId());
            Log.d(TAG, "handleClick(), " + APN_SETTINGS);
            this.mHost.startActivityDismissingKeyguard(APN_SETTINGS);
        }
        updateState();
    }

    protected void handleUpdateState(State state, Object arg) {
        state.visible = DEBUG;
        state.icon = this.mApnStateIcon;
        state.label = this.mApnStateLabel;
        state.contentDescription = this.mApnStateLabel;
    }

    private final void updateState() {
        boolean enabled = false;
        boolean isSecondaryUser = UserHandle.myUserId() != 0 ? DEBUG : false;
        boolean isRestricted = this.mUm.hasUserRestriction("no_config_mobile_networks");
        if (this.mIsWifiOnly || isSecondaryUser || isRestricted) {
            enabled = false;
            Log.d(TAG, "updateState(), isSecondaryUser = " + isSecondaryUser + ", mIsWifiOnly = " + this.mIsWifiOnly + ", isRestricted = " + isRestricted);
        } else {
            int simNum = this.mSubscriptionManager.getActiveSubscriptionInfoCount();
            int callState = TelephonyManager.getDefault().getCallState();
            boolean isIdle = callState == 0 ? DEBUG : false;
            if (!this.mIsAirlineMode && simNum > 0 && isIdle && !isAllRadioOff()) {
                enabled = DEBUG;
            }
            Log.d(TAG, "updateState(), mIsAirlineMode = " + this.mIsAirlineMode + ", simNum = " + simNum + ", callstate = " + callState + ", isIdle = " + isIdle);
        }
        this.mApnSettingsEnabled = enabled;
        Log.d(TAG, "updateState(), mApnSettingsEnabled = " + this.mApnSettingsEnabled);
        updateStateResources();
        refreshState();
    }

    private final void updateStateResources() {
        this.mApnStateLabel = PluginFactory.getQuickSettingsPlugin(this.mContext).customizeApnSettingsTile(this.mApnSettingsEnabled, this.mApnStateIconWrapper, this.mApnStateLabel);
    }

    private boolean isAllRadioOff() {
        int[] subIds = this.mSubscriptionManager.getActiveSubscriptionIdList();
        if (subIds == null || subIds.length <= 0) {
            return DEBUG;
        }
        for (int subId : subIds) {
            if (SIMHelper.isRadioOn(subId)) {
                return false;
            }
        }
        return DEBUG;
    }
}
