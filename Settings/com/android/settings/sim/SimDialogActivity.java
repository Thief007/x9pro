package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.IRCSSettings;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimDialogActivity extends Activity {
    public static String DIALOG_TYPE_KEY = "dialog_type";
    public static String PREFERRED_SIM = "preferred_sim";
    private static String TAG = "SimDialogActivity";
    private Dialog mDialog;
    private ISettingsMiscExt mMiscExt;
    private IRCSSettings mRCSExt;
    private BroadcastReceiver mReceiver = new C05111();
    private SimHotSwapHandler mSimHotSwapHandler;
    private ISimManagementExt mSimManagementExt;

    class C05111 extends BroadcastReceiver {
        C05111() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.d(SimDialogActivity.TAG, "onReceive, action = " + intent.getAction());
            SimDialogActivity.this.finish();
        }
    }

    class C05133 implements OnClickListener {
        C05133() {
        }

        public void onClick(DialogInterface dialog, int id) {
            SimDialogActivity.this.finish();
        }
    }

    class C05144 implements OnDismissListener {
        C05144() {
        }

        public void onDismiss(DialogInterface dialog) {
            SimDialogActivity.this.finish();
        }
    }

    class C05166 implements OnKeyListener {
        C05166() {
        }

        public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
            if (keyCode == 4) {
                SimDialogActivity.this.finish();
            }
            return true;
        }
    }

    class C05177 implements OnCancelListener {
        C05177() {
        }

        public void onCancel(DialogInterface dialogInterface) {
            SimDialogActivity.this.finish();
        }
    }

    class C05188 implements OnSimHotSwapListener {
        C05188() {
        }

        public void onSimHotSwap() {
            Log.d(SimDialogActivity.TAG, "onSimHotSwap, finish Activity");
            SimDialogActivity.this.finish();
        }
    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private final float OPACITY = 0.54f;
        private Context mContext;
        private int mDialogId;
        private int mResId;
        private List<SubscriptionInfo> mSubInfoList;

        private class ViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            private ViewHolder() {
            }
        }

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList, Context context, int resource, String[] arr, int dialogId) {
            super(context, resource, arr);
            this.mContext = context;
            this.mResId = resource;
            this.mDialogId = dialogId;
            this.mSubInfoList = subInfoList;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView;
            ViewHolder holder;
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            if (convertView == null) {
                rowView = inflater.inflate(this.mResId, null);
                holder = new ViewHolder();
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }
            SubscriptionInfo sir = (SubscriptionInfo) this.mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText((CharSequence) getItem(position));
                holder.summary.setText("");
                if (this.mDialogId == 1) {
                    setPhoneAccountIcon(holder, position);
                } else {
                    holder.icon.setImageDrawable(SimDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
                }
                SimDialogActivity.this.mSimManagementExt.setSmsAutoItemIcon(holder.icon, this.mDialogId, position);
                holder.icon.setAlpha(0.54f);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(this.mContext));
                holder.icon.setAlpha(1.0f);
            }
            return rowView;
        }

        private void setPhoneAccountIcon(ViewHolder holder, int location) {
            Log.d(SimDialogActivity.TAG, "setSipAccountBitmap()... location: " + location);
            String askFirst = SimDialogActivity.this.getResources().getString(R.string.sim_calls_ask_first_prefs_title);
            String lableString = (String) getItem(location);
            TelecomManager telecomManager = TelecomManager.from(this.mContext);
            List<PhoneAccountHandle> phoneAccountHandles = telecomManager.getCallCapablePhoneAccounts();
            if (askFirst.equals(lableString)) {
                holder.icon.setImageDrawable(SimDialogActivity.this.getResources().getDrawable(R.drawable.ic_live_help));
                return;
            }
            if (phoneAccountHandles.size() > 1) {
                location--;
            }
            PhoneAccount phoneAccount = null;
            if (location >= 0 && location < phoneAccountHandles.size()) {
                phoneAccount = telecomManager.getPhoneAccount((PhoneAccountHandle) phoneAccountHandles.get(location));
            }
            Log.d(SimDialogActivity.TAG, "setSipAccountBitmap()... position: " + location + " account: " + phoneAccount);
            if (phoneAccount != null) {
                holder.icon.setImageDrawable(phoneAccount.getIcon().loadDrawable(this.mContext));
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setSimStateCheck();
        this.mSimManagementExt = UtilsExt.getSimManagmentExtPlugin(getApplicationContext());
        this.mMiscExt = UtilsExt.getMiscPlugin(getApplicationContext());
        this.mRCSExt = UtilsExt.getRcsSettingsPlugin(getApplicationContext());
        int dialogType = extras.getInt(DIALOG_TYPE_KEY, -1);
        switch (dialogType) {
            case 0:
            case 1:
            case 2:
                this.mDialog = createDialog(this, dialogType);
                this.mDialog.show();
                break;
            case 3:
                List<SubscriptionInfo> subs = SubscriptionManager.from(this).getActiveSubscriptionInfoList();
                if (subs != null && subs.size() == 1) {
                    displayPreferredDialog(extras.getInt(PREFERRED_SIM));
                    break;
                }
                Log.w(TAG, "Subscription count is not 1, skip preferred SIM dialog");
                finish();
                return;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }
    }

    private void displayPreferredDialog(int slotId) {
        Resources res = getResources();
        final Context context = getApplicationContext();
        final SubscriptionInfo sir = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (sir != null) {
            Builder alertDialogBuilder = new Builder(this);
            int subId = SubscriptionManager.getSubIdUsingPhoneId(slotId);
            String title = this.mMiscExt.customizeSimDisplayString(res.getString(R.string.sim_preferred_title), subId);
            String message = this.mMiscExt.customizeSimDisplayString(res.getString(R.string.sim_preferred_message, new Object[]{sir.getDisplayName()}), subId);
            alertDialogBuilder.setTitle(title);
            alertDialogBuilder.setMessage(message);
            alertDialogBuilder.setPositiveButton(R.string.yes, new OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    int subId = sir.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandle = SimDialogActivity.this.subscriptionIdToPhoneAccountHandle(subId);
                    SimDialogActivity.this.setDefaultDataSubId(context, subId);
                    SimDialogActivity.setDefaultSmsSubId(context, subId);
                    SimDialogActivity.this.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                    SimDialogActivity.this.finish();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.no, new C05133());
            alertDialogBuilder.setOnDismissListener(new C05144());
            this.mDialog = alertDialogBuilder.create();
            this.mDialog.show();
            return;
        }
        finish();
    }

    private void setDefaultDataSubId(Context context, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        this.mSimManagementExt.setDataState(subId);
        if (FeatureOption.MTK_CT6M_SUPPORT) {
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            boolean enableBefore = telephonyManager.getDataEnabled();
            int preSubId = SubscriptionManager.getDefaultDataSubId();
            Log.d(TAG, "data flag: " + enableBefore + ", subId: " + subId + ", preSubId: " + preSubId);
            if (SubscriptionManager.isValidSubscriptionId(subId) && subId != preSubId) {
                subscriptionManager.setDefaultDataSubId(subId);
                if (enableBefore) {
                    telephonyManager.setDataEnabled(subId, true);
                    telephonyManager.setDataEnabled(preSubId, false);
                }
            }
        } else {
            subscriptionManager.setDefaultDataSubId(subId);
        }
        this.mSimManagementExt.setDataStateEnable(subId);
        Toast.makeText(context, this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.data_switch_started), subId), 1).show();
    }

    private static void setDefaultSmsSubId(Context context, int subId) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        if (!CdmaUtils.shouldSwitchCapability(context, subId)) {
            subscriptionManager.setDefaultSmsSubId(subId);
        } else if (TelecomManager.from(context).isInCall()) {
            Toast.makeText(context, R.string.default_sms_switch_err_msg1, 0).show();
        } else if (TelephonyUtils.setRadioCapability(context, subId)) {
            subscriptionManager.setDefaultSmsSubId(subId);
        }
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        TelecomManager telecomManager = TelecomManager.from(this);
        if (!CdmaUtils.shouldSwichCapabilityForCalls(this, phoneAccount)) {
            telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
        } else if (telecomManager.isInCall()) {
            Toast.makeText(this, R.string.default_calls_switch_err_msg1, 0).show();
        } else {
            int subId = TelephonyUtils.phoneAccountHandleTosubscriptionId(this, phoneAccount);
            if (SubscriptionManager.isValidSubscriptionId(subId) && TelephonyUtils.setRadioCapability(this, subId)) {
                telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
            }
        }
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(int subId) {
        TelecomManager telecomManager = TelecomManager.from(this);
        TelephonyManager telephonyManager = TelephonyManager.from(this);
        Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
        while (phoneAccounts.hasNext()) {
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) phoneAccounts.next();
            if (subId == telephonyManager.getSubIdForPhoneAccount(telecomManager.getPhoneAccount(phoneAccountHandle))) {
                return phoneAccountHandle;
            }
        }
        return null;
    }

    public Dialog createDialog(Context context, int id) {
        ArrayList<String> list = new ArrayList();
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();
        final int i = id;
        final List<SubscriptionInfo> list2 = subInfoList;
        final Context context2 = context;
        OnClickListener c05155 = new OnClickListener() {
            public void onClick(DialogInterface dialog, int value) {
                PhoneAccountHandle phoneAccountHandle = null;
                switch (i) {
                    case 0:
                        Integer valueOf;
                        SubscriptionInfo sir = (SubscriptionInfo) list2.get(value);
                        if (sir != null) {
                            valueOf = Integer.valueOf(sir.getSubscriptionId());
                        }
                        int targetSub = valueOf.intValue();
                        if (!SimDialogActivity.this.mSimManagementExt.switchDefaultDataSub(context2, targetSub)) {
                            if (!CdmaUtils.isCdmaCardCompetionForData(context2)) {
                                if (!CdmaUtils.isSwitchCdmaCardToGsmCard(context2, targetSub)) {
                                    SimDialogActivity.this.setDefaultDataSubId(context2, targetSub);
                                    break;
                                } else {
                                    CdmaUtils.startAlertCdmaDialog(context2, targetSub, i);
                                    break;
                                }
                            } else if (!SimDialogActivity.isEqualDefaultValue(context2, i, targetSub)) {
                                if (!TelecomManager.from(context2).isInCall()) {
                                    CdmaUtils.startAlertCdmaDialog(context2, targetSub, i);
                                    break;
                                } else {
                                    Toast.makeText(context2, R.string.default_data_switch_err_msg1, 0).show();
                                    break;
                                }
                            }
                        }
                        break;
                    case 1:
                        List<PhoneAccountHandle> phoneAccountsList = TelecomManager.from(context2).getCallCapablePhoneAccounts();
                        value = SimDialogActivity.this.mSimManagementExt.customizeValue(value);
                        if (value <= phoneAccountsList.size()) {
                            if (CdmaUtils.isCdmaCardCompetionForCalls(context2, value) && !SimDialogActivity.isEqualDefaultValue(context2, i, value)) {
                                if (!TelecomManager.from(context2).isInCall()) {
                                    CdmaUtils.startAlertCdmaDialog(context2, value, i);
                                    break;
                                } else {
                                    Toast.makeText(context2, R.string.default_calls_switch_err_msg1, 0).show();
                                    break;
                                }
                            }
                            SimDialogActivity simDialogActivity = SimDialogActivity.this;
                            if (value >= 1) {
                                phoneAccountHandle = (PhoneAccountHandle) phoneAccountsList.get(value - 1);
                            }
                            simDialogActivity.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                            break;
                        }
                        Log.w(SimDialogActivity.TAG, "phone account changed, do noting! value = " + value + ", phone account size = " + phoneAccountsList.size());
                        break;
                        break;
                    case 2:
                        int subId = SimDialogActivity.this.getPickSmsDefaultSub(list2, value);
                        if (CdmaUtils.isCdmaCardCompetionForSms(context2, subId)) {
                            if (!SimDialogActivity.isEqualDefaultValue(context2, i, subId)) {
                                if (!TelecomManager.from(context2).isInCall()) {
                                    CdmaUtils.startAlertCdmaDialog(context2, subId, i);
                                    break;
                                } else {
                                    Toast.makeText(context2, R.string.default_sms_switch_err_msg1, 0).show();
                                    break;
                                }
                            }
                        }
                        SimDialogActivity.setDefaultSmsSubId(context2, subId);
                        break;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid dialog type " + i + " in SIM dialog.");
                }
                SimDialogActivity.this.finish();
            }
        };
        OnKeyListener c05166 = new C05166();
        ArrayList<SubscriptionInfo> callsSubInfoList = new ArrayList();
        ArrayList<SubscriptionInfo> smsSubInfoList = new ArrayList();
        if (id == 1) {
            TelecomManager telecomManager = TelecomManager.from(context);
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            Iterator<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts().listIterator();
            if (telecomManager.getCallCapablePhoneAccounts().size() > 1) {
                list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
                callsSubInfoList.add(null);
            }
            while (phoneAccounts.hasNext()) {
                PhoneAccount phoneAccount = telecomManager.getPhoneAccount((PhoneAccountHandle) phoneAccounts.next());
                if (phoneAccount != null) {
                    list.add((String) phoneAccount.getLabel());
                    int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
                    if (subId != -1) {
                        callsSubInfoList.add(SubscriptionManager.from(context).getActiveSubscriptionInfo(subId));
                    } else {
                        callsSubInfoList.add(null);
                    }
                }
            }
            this.mSimManagementExt.customizeListArray(list);
            this.mSimManagementExt.customizeSubscriptionInfoArray(callsSubInfoList);
        } else if (id == 2) {
            setupSmsSubInfoList(list, subInfoList, selectableSubInfoLength, smsSubInfoList);
        } else {
            for (int i2 = 0; i2 < selectableSubInfoLength; i2++) {
                CharSequence displayName = ((SubscriptionInfo) subInfoList.get(i2)).getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        }
        String[] arr = (String[]) list.toArray(new String[0]);
        Builder builder = new Builder(context);
        ListAdapter adapter = new SelectAccountListAdapter(getAdapterData(id, subInfoList, callsSubInfoList, smsSubInfoList), builder.getContext(), R.layout.select_account_list_item, arr, id);
        switch (id) {
            case 0:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case 1:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case 2:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
        changeDialogTitle(builder, id);
        Dialog dialog = builder.setAdapter(adapter, c05155).create();
        dialog.setOnKeyListener(c05166);
        dialog.setOnCancelListener(new C05177());
        return dialog;
    }

    private void setSimStateCheck() {
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C05188());
        registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
    }

    private void unsetSimStateCheck() {
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        unregisterReceiver(this.mReceiver);
    }

    protected void onDestroy() {
        unsetSimStateCheck();
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        super.onDestroy();
    }

    private int getPickSmsDefaultSub(List<SubscriptionInfo> subInfoList, int value) {
        int subId;
        value = this.mSimManagementExt.customizeValue(value);
        if (value < 1) {
            if ((subInfoList == null ? 0 : subInfoList.size()) == 1) {
                subId = ((SubscriptionInfo) subInfoList.get(value)).getSubscriptionId();
            } else {
                subId = -2;
            }
        } else if (value < 1 || value >= subInfoList.size() + 1) {
            subId = this.mSimManagementExt.getDefaultSmsSubIdForAuto();
        } else {
            subId = ((SubscriptionInfo) subInfoList.get(value - 1)).getSubscriptionId();
        }
        subId = this.mRCSExt.getDefaultSmsClickContentExt(subInfoList, value, subId);
        Log.d(TAG, "getPickSmsDefaultSub, value: " + value + ", subId: " + subId);
        return subId;
    }

    private void setupSmsSubInfoList(ArrayList<String> list, List<SubscriptionInfo> subInfoList, int selectableSubInfoLength, ArrayList<SubscriptionInfo> smsSubInfoList) {
        if (selectableSubInfoLength > 1 && this.mRCSExt.isNeedAskFirstItemForSms()) {
            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            smsSubInfoList.add(null);
        }
        for (int i = 0; i < selectableSubInfoLength; i++) {
            SubscriptionInfo sir = (SubscriptionInfo) subInfoList.get(i);
            smsSubInfoList.add(sir);
            CharSequence displayName = sir.getDisplayName();
            if (displayName == null) {
                displayName = "";
            }
            list.add(displayName.toString());
        }
        this.mSimManagementExt.customizeListArray(list);
        this.mSimManagementExt.customizeSubscriptionInfoArray(smsSubInfoList);
        this.mSimManagementExt.initAutoItemForSms(list, smsSubInfoList);
    }

    private List<SubscriptionInfo> getAdapterData(int id, List<SubscriptionInfo> subInfoList, ArrayList<SubscriptionInfo> callsSubInfoList, ArrayList<SubscriptionInfo> smsSubInfoList) {
        switch (id) {
            case 0:
                return subInfoList;
            case 1:
                return callsSubInfoList;
            case 2:
                return smsSubInfoList;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
    }

    private void changeDialogTitle(Builder builder, int id) {
        switch (id) {
            case 0:
                builder.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.select_sim_for_data), -1));
                return;
            case 1:
                builder.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.select_sim_for_calls), -1));
                return;
            case 2:
                builder.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_card_select_title), -1));
                return;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + id + " in SIM dialog.");
        }
    }

    public static boolean isEqualDefaultValue(Context context, int dialogType, int targetValue) {
        boolean isEqual = false;
        switch (dialogType) {
            case 0:
                if (SubscriptionManager.getDefaultDataSubId() != targetValue) {
                    isEqual = false;
                    break;
                }
                isEqual = true;
                break;
            case 1:
                TelecomManager telecomManager = TelecomManager.from(context);
                PhoneAccountHandle phoneAccountHandle = targetValue < 1 ? null : (PhoneAccountHandle) telecomManager.getCallCapablePhoneAccounts().get(targetValue - 1);
                PhoneAccountHandle defaultHandle = telecomManager.getUserSelectedOutgoingPhoneAccount();
                if (phoneAccountHandle == null) {
                    if (defaultHandle != null) {
                        isEqual = false;
                        break;
                    }
                    isEqual = true;
                    break;
                }
                isEqual = phoneAccountHandle.equals(defaultHandle);
                break;
            case 2:
                if (SubscriptionManager.getDefaultSmsSubId() != targetValue) {
                    isEqual = false;
                    break;
                }
                isEqual = true;
                break;
        }
        Log.d(TAG, "isEqualDefaultValue()... isEqual: " + isEqual + ", dialogType: " + dialogType + " targetValue: " + targetValue);
        return isEqual;
    }
}
