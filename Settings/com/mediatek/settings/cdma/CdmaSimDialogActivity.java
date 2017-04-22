package com.mediatek.settings.cdma;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.settings.sim.TelephonyUtils;

public class CdmaSimDialogActivity extends Activity {
    private int mActionType = -1;
    private PhoneAccountHandle mHandle = null;
    private IntentFilter mIntentFilter;
    private SimHotSwapHandler mSimHotSwapHandler;
    private BroadcastReceiver mSubReceiver = new C07241();
    private int mTargetSubId = -1;

    class C07241 extends BroadcastReceiver {
        C07241() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.d("CdmaSimDialogActivity", "mSubReceiver action = " + intent.getAction());
            CdmaSimDialogActivity.this.finish();
        }
    }

    class C07252 implements OnSimHotSwapListener {
        C07252() {
        }

        public void onSimHotSwap() {
            Log.d("CdmaSimDialogActivity", "onSimHotSwap, finish Activity~~");
            CdmaSimDialogActivity.this.finish();
        }
    }

    class C07263 implements OnClickListener {
        C07263() {
        }

        public void onClick(DialogInterface dialog, int which) {
            if (dialog != null) {
                dialog.dismiss();
            }
            CdmaSimDialogActivity.this.finish();
        }
    }

    class C07274 implements OnCancelListener {
        C07274() {
        }

        public void onCancel(DialogInterface dialog) {
            if (dialog != null) {
                dialog.dismiss();
            }
            CdmaSimDialogActivity.this.finish();
        }
    }

    class C07285 implements OnKeyListener {
        C07285() {
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == 4) {
                CdmaSimDialogActivity.this.finish();
            }
            return true;
        }
    }

    class C07318 implements OnKeyListener {
        C07318() {
        }

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == 4) {
                CdmaSimDialogActivity.this.finish();
            }
            return true;
        }
    }

    class C07329 implements OnCancelListener {
        C07329() {
        }

        public void onCancel(DialogInterface dialog) {
            CdmaSimDialogActivity.this.finish();
        }
    }

    private void init() {
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C07252());
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mSubReceiver, this.mIntentFilter);
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.d("CdmaSimDialogActivity", "onCreate");
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        init();
        if (extras != null) {
            int dialogType = extras.getInt("dialog_type", -1);
            this.mTargetSubId = extras.getInt("target_subid", -1);
            this.mActionType = extras.getInt("action_type", -1);
            Log.d("CdmaSimDialogActivity", "dialogType: " + dialogType + " argetSubId: " + this.mTargetSubId + " actionType: " + this.mActionType);
            switch (dialogType) {
                case 0:
                    createTwoCdmaCardDialog();
                    return;
                case 1:
                    displayAlertCdmaDialog();
                    return;
                default:
                    throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
            }
        }
        Log.e("CdmaSimDialogActivity", "unexpect happend");
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        unregisterReceiver(this.mSubReceiver);
    }

    private void createTwoCdmaCardDialog() {
        Log.d("CdmaSimDialogActivity", "createTwoCdmaCardDialog...");
        Builder alertDialogBuilder = new Builder(this);
        alertDialogBuilder.setMessage(R.string.two_cdma_dialog_msg);
        alertDialogBuilder.setPositiveButton(17039370, new C07263());
        alertDialogBuilder.setOnCancelListener(new C07274());
        alertDialogBuilder.setOnKeyListener(new C07285());
        alertDialogBuilder.create().show();
    }

    private void displayAlertCdmaDialog() {
        Log.d("CdmaSimDialogActivity", "displayAlertCdmaDialog()... + c2K support: " + FeatureOption.MTK_C2K_SLOT2_SUPPORT);
        final Context context = getApplicationContext();
        if (this.mActionType == 1) {
            this.mHandle = this.mTargetSubId < 1 ? null : (PhoneAccountHandle) TelecomManager.from(context).getCallCapablePhoneAccounts().get(this.mTargetSubId - 1);
            this.mTargetSubId = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, this.mHandle);
            Log.d("CdmaSimDialogActivity", "convert " + this.mHandle + " to subId: " + this.mTargetSubId);
        } else if (this.mActionType == 0 || this.mActionType == 2) {
            this.mHandle = TelephonyUtils.subscriptionIdToPhoneAccountHandle(context, this.mTargetSubId);
        }
        SubscriptionInfo targetSir = SubscriptionManager.from(context).getActiveSubscriptionInfo(this.mTargetSubId);
        SubscriptionInfo defaultSir = null;
        for (int i : SubscriptionManager.from(context).getActiveSubscriptionIdList()) {
            if (i != this.mTargetSubId) {
                defaultSir = SubscriptionManager.from(context).getActiveSubscriptionInfo(i);
            }
        }
        Builder dialog = new Builder(this);
        String cdmaCardCompetionMessage = "";
        String gsmCdamCardMesage = "";
        if (!(defaultSir == null || targetSir == null)) {
            cdmaCardCompetionMessage = context.getResources().getString(R.string.c2k_cdma_card_competion_message, new Object[]{defaultSir.getDisplayName(), defaultSir.getDisplayName(), targetSir.getDisplayName()});
            gsmCdamCardMesage = context.getResources().getString(R.string.c2k_gsm_cdma_sim_message, new Object[]{targetSir.getDisplayName(), defaultSir.getDisplayName(), defaultSir.getDisplayName(), targetSir.getDisplayName()});
        }
        dialog.setMessage(!CdmaUtils.isSwitchCdmaCardToGsmCard(context, this.mTargetSubId) ? cdmaCardCompetionMessage : gsmCdamCardMesage);
        dialog.setPositiveButton(!CdmaUtils.isSwitchCdmaCardToGsmCard(context, this.mTargetSubId) ? 17039370 : R.string.yes, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int subIdCalls = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, TelecomManager.from(context).getUserSelectedOutgoingPhoneAccount());
                int subIdSms = SubscriptionManager.getDefaultSmsSubId();
                if (CdmaSimDialogActivity.this.mActionType == 0) {
                    if (TelecomManager.from(context).isInCall()) {
                        Toast.makeText(context, R.string.default_data_switch_err_msg1, 0).show();
                    } else {
                        if (SubscriptionManager.isValidSubscriptionId(subIdCalls)) {
                            CdmaSimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(CdmaSimDialogActivity.this.mHandle);
                        }
                        if (SubscriptionManager.isValidSubscriptionId(subIdSms)) {
                            CdmaSimDialogActivity.this.setDefaultSmsSubId(context, CdmaSimDialogActivity.this.mTargetSubId);
                        }
                        if (SubscriptionManager.isValidSubscriptionId(CdmaSimDialogActivity.this.mTargetSubId)) {
                            CdmaSimDialogActivity.this.setDefaultDataSubId(context, CdmaSimDialogActivity.this.mTargetSubId);
                        }
                    }
                } else if (CdmaSimDialogActivity.this.mActionType == 2) {
                    if (TelecomManager.from(context).isInCall()) {
                        Toast.makeText(context, R.string.default_sms_switch_err_msg1, 0).show();
                    } else {
                        if (SubscriptionManager.isValidSubscriptionId(subIdCalls)) {
                            CdmaSimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(CdmaSimDialogActivity.this.mHandle);
                        }
                        if (SubscriptionManager.isValidSubscriptionId(CdmaSimDialogActivity.this.mTargetSubId)) {
                            CdmaSimDialogActivity.this.setDefaultSmsSubId(context, CdmaSimDialogActivity.this.mTargetSubId);
                            CdmaSimDialogActivity.this.setDefaultDataSubId(context, CdmaSimDialogActivity.this.mTargetSubId);
                        }
                    }
                } else if (CdmaSimDialogActivity.this.mActionType == 1) {
                    if (TelecomManager.from(context).isInCall()) {
                        Toast.makeText(context, R.string.default_calls_switch_err_msg1, 0).show();
                    } else {
                        CdmaSimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(CdmaSimDialogActivity.this.mHandle);
                        if (SubscriptionManager.isValidSubscriptionId(subIdSms)) {
                            CdmaSimDialogActivity.this.setDefaultSmsSubId(context, CdmaSimDialogActivity.this.mTargetSubId);
                        }
                        if (SubscriptionManager.isValidSubscriptionId(CdmaSimDialogActivity.this.mTargetSubId)) {
                            CdmaSimDialogActivity.this.setDefaultDataSubId(context, CdmaSimDialogActivity.this.mTargetSubId);
                        }
                    }
                }
                Log.d("CdmaSimDialogActivity", "subIdCalls: " + subIdCalls + " subIdSms: " + subIdSms + " mTargetSubId: " + CdmaSimDialogActivity.this.mTargetSubId);
                CdmaSimDialogActivity.this.finish();
            }
        });
        dialog.setNegativeButton(!CdmaUtils.isSwitchCdmaCardToGsmCard(context, this.mTargetSubId) ? 17039360 : R.string.no, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (!FeatureOption.MTK_C2K_SLOT2_SUPPORT && CdmaUtils.isSwitchCdmaCardToGsmCard(context, CdmaSimDialogActivity.this.mTargetSubId)) {
                    TelephonyUtils.setDefaultDataSubIdWithoutCapabilitySwitch(context, CdmaSimDialogActivity.this.mTargetSubId);
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
                CdmaSimDialogActivity.this.finish();
            }
        });
        dialog.setOnKeyListener(new C07318());
        dialog.setOnCancelListener(new C07329());
        dialog.show();
    }

    private void setDefaultDataSubId(Context context, int subId) {
        SubscriptionManager.from(context).setDefaultDataSubId(subId);
        if (this.mActionType == 0) {
            Toast.makeText(context, R.string.data_switch_started, 1).show();
        }
    }

    private void setDefaultSmsSubId(Context context, int subId) {
        SubscriptionManager.from(context).setDefaultSmsSubId(subId);
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        TelecomManager.from(this).setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }
}
