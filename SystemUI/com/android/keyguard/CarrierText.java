package com.android.keyguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.settingslib.WirelessUtils;
import com.mediatek.keyguard.Plugin.KeyguardPluginFactory;
import com.mediatek.keyguard.ext.ICarrierTextExt;
import com.mediatek.keyguard.ext.IOperatorSIMString;
import com.mediatek.keyguard.ext.IOperatorSIMString.SIMChangedTag;
import java.util.List;
import java.util.Locale;

public class CarrierText extends TextView {
    private static /* synthetic */ int[] -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;
    private static /* synthetic */ int[] -com_android_keyguard_CarrierText$StatusModeSwitchesValues;
    private static CharSequence mSeparator;
    private final BroadcastReceiver mBroadcastReceiver;
    private KeyguardUpdateMonitorCallback mCallback;
    private String[] mCarrier;
    private boolean[] mCarrierNeedToShow;
    private ICarrierTextExt mCarrierTextExt;
    private Context mContext;
    private IOperatorSIMString mIOperatorSIMString;
    private final boolean mIsEmergencyCallCapable;
    private boolean mIsLockedCard;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private int mNumOfPhone;
    private StatusMode[] mStatusMode;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private WifiManager mWifiManager;

    private class CarrierTextTransformationMethod extends SingleLineTransformationMethod {
        private final boolean mAllCaps;
        private final Locale mLocale;

        public CarrierTextTransformationMethod(Context context, boolean allCaps) {
            this.mLocale = context.getResources().getConfiguration().locale;
            this.mAllCaps = allCaps;
        }

        public CharSequence getTransformation(CharSequence source, View view) {
            source = super.getTransformation(source, view);
            if (!this.mAllCaps || source == null) {
                return source;
            }
            return source.toString().toUpperCase(this.mLocale);
        }
    }

    private enum StatusMode {
        Normal,
        NetworkLocked,
        SimMissing,
        SimMissingLocked,
        SimPukLocked,
        SimLocked,
        SimPermDisabled,
        SimNotReady,
        SimUnknown,
        NetworkSearching
    }

    private static /* synthetic */ int[] -getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues() {
        if (-com_android_internal_telephony_IccCardConstants$StateSwitchesValues != null) {
            return -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;
        }
        int[] iArr = new int[State.values().length];
        try {
            iArr[State.ABSENT.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[State.CARD_IO_ERROR.ordinal()] = 17;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[State.NETWORK_LOCKED.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[State.NOT_READY.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[State.PERM_DISABLED.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[State.PIN_REQUIRED.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[State.PUK_REQUIRED.ordinal()] = 6;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[State.READY.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[State.UNKNOWN.ordinal()] = 8;
        } catch (NoSuchFieldError e9) {
        }
        -com_android_internal_telephony_IccCardConstants$StateSwitchesValues = iArr;
        return iArr;
    }

    private static /* synthetic */ int[] -getcom_android_keyguard_CarrierText$StatusModeSwitchesValues() {
        if (-com_android_keyguard_CarrierText$StatusModeSwitchesValues != null) {
            return -com_android_keyguard_CarrierText$StatusModeSwitchesValues;
        }
        int[] iArr = new int[StatusMode.values().length];
        try {
            iArr[StatusMode.NetworkLocked.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[StatusMode.NetworkSearching.ordinal()] = 17;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[StatusMode.Normal.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[StatusMode.SimLocked.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[StatusMode.SimMissing.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[StatusMode.SimMissingLocked.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[StatusMode.SimNotReady.ordinal()] = 6;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[StatusMode.SimPermDisabled.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[StatusMode.SimPukLocked.ordinal()] = 8;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[StatusMode.SimUnknown.ordinal()] = 18;
        } catch (NoSuchFieldError e10) {
        }
        -com_android_keyguard_CarrierText$StatusModeSwitchesValues = iArr;
        return iArr;
    }

    private void initMembers() {
        this.mNumOfPhone = KeyguardUtils.getNumOfPhone();
        this.mCarrier = new String[this.mNumOfPhone];
        this.mCarrierNeedToShow = new boolean[this.mNumOfPhone];
        this.mStatusMode = new StatusMode[this.mNumOfPhone];
        for (int i = 0; i < this.mNumOfPhone; i++) {
            this.mStatusMode[i] = StatusMode.Normal;
        }
    }

    public CarrierText(Context context) {
        this(context, null);
        initMembers();
    }

    public CarrierText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsLockedCard = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(intent.getAction())) {
                    Log.w("CarrierText", "receive IPO_SHUTDOWN & clear carrier text.");
                    CarrierText.this.setText("");
                }
            }
        };
        this.mCallback = new KeyguardUpdateMonitorCallback() {
            public void onRefreshCarrierInfo() {
                CarrierText.this.updateCarrierText();
            }

            public void onSimStateChangedUsingPhoneId(int phoneId, State simState) {
                CarrierText.this.updateCarrierText();
            }

            public void onFinishedGoingToSleep(int why) {
                CarrierText.this.setSelected(false);
            }

            public void onStartedWakingUp() {
                CarrierText.this.setSelected(true);
            }

            public void onCDMACardTypeChanges(boolean isLockedCard) {
                Log.d("CarrierText", "onCDMACardTypeChanges(isLockedCard = " + isLockedCard + ")");
                CarrierText.this.mIsLockedCard = isLockedCard;
                CarrierText.this.updateCarrierText();
            }
        };
        this.mContext = context;
        this.mIsEmergencyCallCapable = context.getResources().getBoolean(17956947);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        initMembers();
        this.mIOperatorSIMString = KeyguardPluginFactory.getOperatorSIMString(this.mContext);
        this.mCarrierTextExt = KeyguardPluginFactory.getCarrierTextExt(this.mContext);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R$styleable.CarrierText, 0, 0);
        try {
            boolean useAllCaps = a.getBoolean(R$styleable.CarrierText_allCaps, false);
            setTransformationMethod(new CarrierTextTransformationMethod(this.mContext, useAllCaps));
            this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        } finally {
            a.recycle();
        }
    }

    protected void updateCarrierText() {
        if (isWifiOnlyDevice()) {
            Log.d("CarrierText", "updateCarrierText() - WifiOnly deivce, not show carrier text.");
            setText("");
            return;
        }
        boolean allSimsMissing = showOrHideCarrier();
        for (int phoneId = 0; phoneId < this.mNumOfPhone; phoneId++) {
            int subId = KeyguardUtils.getSubIdUsingPhoneId(phoneId);
            State simState = this.mKeyguardUpdateMonitor.getSimStateOfPhoneId(phoneId);
            SubscriptionInfo subInfo = this.mKeyguardUpdateMonitor.getSubscriptionInfoForSubId(subId);
            CharSequence carrierName = subInfo == null ? null : subInfo.getCarrierName();
            Log.d("CarrierText", "updateCarrierText(): subId = " + subId + " , phoneId = " + phoneId + ", simState = " + simState + ", carrierName = " + carrierName);
            CharSequence carrierTextForSimState = getCarrierTextForSimState(phoneId, simState, carrierName, null, null);
            Log.d("CarrierText", "updateCarrierText(): carrierTextForSimState = " + carrierTextForSimState);
            if (carrierTextForSimState != null) {
                carrierTextForSimState = this.mIOperatorSIMString.getOperatorSIMString(carrierTextForSimState.toString(), phoneId, SIMChangedTag.DELSIM, this.mContext);
                if (carrierTextForSimState != null) {
                    carrierTextForSimState = this.mCarrierTextExt.customizeCarrierTextCapital(carrierTextForSimState.toString()).toString();
                } else {
                    carrierTextForSimState = null;
                }
                Log.d("CarrierText", "updateCarrierText() - after customizeCarrierTextCapital, carrierTextForSimState = " + carrierTextForSimState);
            }
            if (carrierTextForSimState != null) {
                this.mCarrier[phoneId] = carrierTextForSimState.toString();
            } else {
                this.mCarrier[phoneId] = null;
            }
        }
        CharSequence carrierFinalContent = null;
        String divider = this.mCarrierTextExt.customizeCarrierTextDivider(mSeparator.toString());
        int i = 0;
        while (i < this.mNumOfPhone) {
            Log.d("CarrierText", "updateCarrierText() - mCarrierNeedToShow[i] = " + this.mCarrierNeedToShow[i] + " mCarrier[i] = " + this.mCarrier[i]);
            if (this.mCarrierNeedToShow[i] && this.mCarrier[i] != null) {
                if (carrierFinalContent == null) {
                    carrierFinalContent = this.mCarrier[i];
                } else {
                    carrierFinalContent = divider + this.mCarrier[i];
                }
            }
            i++;
        }
        Log.d("CarrierText", "updateCarrierText() - after combination, carrierFinalContent = " + carrierFinalContent);
        if (WirelessUtils.isAirplaneModeOn(this.mContext)) {
            carrierFinalContent = getContext().getString(R$string.airplane_mode);
        }
        if (!false && WirelessUtils.isAirplaneModeOn(this.mContext)) {
            carrierFinalContent = getContext().getString(R$string.airplane_mode);
        }
        Log.d("CarrierText", "updateCarrierText()2 - after combination, carrierFinalContent = " + carrierFinalContent);
        setText(carrierFinalContent);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mSeparator = getResources().getString(17040612);
        setSelected(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
        setLayerType(2, null);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d("CarrierText", "onAttachedToWindow called.");
        if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
            this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
            this.mKeyguardUpdateMonitor.registerCallback(this.mCallback);
        } else {
            this.mKeyguardUpdateMonitor = null;
            setText("");
        }
        registerBroadcastReceiver();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("CarrierText", "onDetachedFromWindow called.");
        if (this.mKeyguardUpdateMonitor != null) {
            this.mKeyguardUpdateMonitor.removeCallback(this.mCallback);
        }
        unregisterBroadcastReceiver();
    }

    private CharSequence getCarrierTextForSimState(int phoneId, State simState, CharSequence text, CharSequence hnbName, CharSequence csgId) {
        CharSequence carrierText;
        switch (-getcom_android_keyguard_CarrierText$StatusModeSwitchesValues()[getStatusForIccState(simState).ordinal()]) {
            case 1:
                carrierText = makeCarrierStringOnEmergencyCapable(this.mContext.getText(R$string.keyguard_network_locked_message), text, hnbName, csgId);
                break;
            case 2:
                carrierText = text;
                break;
            case 3:
                carrierText = makeCarrierStringOnEmergencyCapable(getContext().getText(R$string.keyguard_sim_locked_message), text, hnbName, csgId);
                break;
            case 4:
                CharSequence simMessage = getContext().getText(R$string.keyguard_missing_sim_message_short);
                carrierText = this.mCarrierTextExt.customizeCarrierTextWhenSimMissing(this.mCarrierTextExt.customizeCarrierText(makeCarrierStringOnEmergencyCapable(simMessage, text, hnbName, csgId), simMessage, phoneId));
                break;
            case 5:
                carrierText = null;
                break;
            case 6:
                carrierText = "";
                carrierText = null;
                break;
            case 7:
                carrierText = getContext().getText(R$string.keyguard_permanent_disabled_sim_message_short);
                break;
            case 8:
                carrierText = makeCarrierStringOnEmergencyCapable(getContext().getText(R$string.keyguard_sim_puk_locked_message), text, hnbName, csgId);
                break;
            default:
                carrierText = text;
                break;
        }
        if (carrierText != null) {
            carrierText = this.mCarrierTextExt.customizeCarrierTextWhenCardTypeLocked(carrierText, this.mContext, phoneId, this.mIsLockedCard).toString();
        }
        Log.d("CarrierText", "getCarrierTextForSimState simState=" + simState + " text(carrierName)=" + text + " HNB=" + hnbName + " CSG=" + csgId + " carrierText=" + carrierText);
        return carrierText;
    }

    private CharSequence makeCarrierStringOnEmergencyCapable(CharSequence simMessage, CharSequence emergencyCallMessage, CharSequence hnbName, CharSequence csgId) {
        CharSequence emergencyCallMessageExtend = emergencyCallMessage;
        if (!TextUtils.isEmpty(emergencyCallMessage)) {
            emergencyCallMessageExtend = appendCsgInfo(emergencyCallMessage, hnbName, csgId);
        }
        if (this.mIsEmergencyCallCapable) {
            return concatenate(simMessage, emergencyCallMessageExtend);
        }
        return simMessage;
    }

    private StatusMode getStatusForIccState(State simState) {
        if (simState == null) {
            return StatusMode.SimUnknown;
        }
        boolean missingAndNotProvisioned = !KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceProvisioned() ? simState != State.ABSENT ? simState == State.PERM_DISABLED : true : false;
        if (missingAndNotProvisioned) {
            return StatusMode.SimMissingLocked;
        }
        switch (-getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues()[simState.ordinal()]) {
            case 1:
                return StatusMode.SimMissing;
            case 2:
                return StatusMode.NetworkLocked;
            case 3:
                return StatusMode.SimNotReady;
            case 4:
                return StatusMode.SimPermDisabled;
            case 5:
                return StatusMode.SimLocked;
            case 6:
                return StatusMode.SimPukLocked;
            case 7:
                return StatusMode.Normal;
            case 8:
                return StatusMode.SimUnknown;
            default:
                return StatusMode.SimMissing;
        }
    }

    private static CharSequence concatenate(CharSequence plmn, CharSequence spn) {
        boolean plmnValid = !TextUtils.isEmpty(plmn);
        boolean spnValid = !TextUtils.isEmpty(spn);
        if (plmnValid && spnValid) {
            return plmn + mSeparator + spn;
        }
        if (plmnValid) {
            return plmn;
        }
        if (spnValid) {
            return spn;
        }
        return "";
    }

    private boolean isWifiOnlyDevice() {
        if (((ConnectivityManager) getContext().getSystemService("connectivity")).isNetworkSupported(0)) {
            return false;
        }
        return true;
    }

    private boolean showOrHideCarrier() {
        int i;
        int mNumOfSIM = 0;
        for (i = 0; i < this.mNumOfPhone; i++) {
            StatusMode statusMode = getStatusForIccState(this.mKeyguardUpdateMonitor.getSimStateOfPhoneId(i));
            boolean simMissing = (statusMode == StatusMode.SimMissing || statusMode == StatusMode.SimMissingLocked) ? true : statusMode == StatusMode.SimUnknown;
            Log.d("CarrierText", "showOrHideCarrier() - before showCarrierTextWhenSimMissing,phone#" + i + " simMissing = " + simMissing);
            simMissing = this.mCarrierTextExt.showCarrierTextWhenSimMissing(simMissing, i);
            Log.d("CarrierText", "showOrHideCarrier() - after showCarrierTextWhenSimMissing,phone#" + i + " simMissing = " + simMissing);
            if (simMissing) {
                this.mCarrierNeedToShow[i] = false;
            } else {
                this.mCarrierNeedToShow[i] = true;
                mNumOfSIM++;
            }
        }
        List<SubscriptionInfo> subs = this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        if (mNumOfSIM == 0) {
            String defaultPlmn = this.mUpdateMonitor.getDefaultPlmn().toString();
            int index = 0;
            for (i = 0; i < subs.size(); i++) {
                SubscriptionInfo info = (SubscriptionInfo) subs.get(i);
                int subId = info.getSubscriptionId();
                int phoneId = info.getSimSlotIndex();
                CharSequence carrierName = info.getCarrierName();
                if (carrierName != null && !defaultPlmn.contentEquals(carrierName)) {
                    index = phoneId;
                    break;
                }
            }
            this.mCarrierNeedToShow[index] = true;
            Log.d("CarrierText", "updateOperatorInfo, No SIM cards, force slotId " + index + " to visible.");
        }
        if (mNumOfSIM == 0) {
            return true;
        }
        return false;
    }

    private CharSequence appendCsgInfo(CharSequence srcText, CharSequence hnbName, CharSequence csgId) {
        CharSequence outText = srcText;
        if (!TextUtils.isEmpty(hnbName)) {
            return concatenate(srcText, hnbName);
        }
        if (TextUtils.isEmpty(csgId)) {
            return outText;
        }
        return concatenate(srcText, csgId);
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }
}
