package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.ext.IStatusBarPlugin;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.extcb.SvLteController;
import com.mediatek.systemui.statusbar.networktype.NetworkTypeUtils;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Objects;

public class MobileSignalController extends SignalController<MobileState, MobileIconGroup> {
    private Config mConfig;
    private int mDataNetType = 0;
    private int mDataState = 0;
    private MobileIconGroup mDefaultIcons;
    private final SubscriptionDefaults mDefaults;
    private int mLastIndex = 0;
    private int[] mLevelArray = new int[]{65535, 65535, 65535};
    private IMobileIconExt mMobileIconExt;
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    final SparseArray<MobileIconGroup> mNetworkToIconLookup = new SparseArray();
    private final TelephonyManager mPhone;
    final PhoneStateListener mPhoneStateListener;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private IStatusBarPlugin mStatusBarExt;
    SubscriptionInfo mSubscriptionInfo;
    public SvLteController mSvLteController;

    static class MobileIconGroup extends IconGroup {
        final int mDataContentDescription;
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;

        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc, int sbNullState, int qsNullState, int sbDiscState, int qsDiscState, int discContentDesc, int dataContentDesc, int dataType, boolean isWide, int qsDataType) {
            super(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState, qsDiscState, discContentDesc);
            this.mDataContentDescription = dataContentDesc;
            this.mDataType = dataType;
            this.mIsWide = isWide;
            this.mQsDataType = qsDataType;
        }
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(int subId, Looper looper) {
            super(subId, looper);
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (MobileSignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "onSignalStrengthsChanged signalStrength=" + signalStrength + (signalStrength == null ? "" : " level=" + signalStrength.getLevel()));
            }
            MobileSignalController.this.mSignalStrength = signalStrength;
            MobileSignalController.this.mSvLteController.onSignalStrengthsChanged(signalStrength);
            MobileSignalController.this.updateTelephony();
        }

        public void onServiceStateChanged(ServiceState state) {
            if (MobileSignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "onServiceStateChanged voiceState=" + state.getVoiceRegState() + " dataState=" + state.getDataRegState());
            }
            MobileSignalController.this.mServiceState = state;
            MobileSignalController.this.mSvLteController.onServiceStateChanged(state);
            MobileSignalController.this.mDataNetType = NetworkTypeUtils.getDataNetTypeFromServiceState(MobileSignalController.this.mDataNetType, MobileSignalController.this.mServiceState);
            MobileSignalController.this.updateTelephony();
        }

        public void onDataConnectionStateChanged(int state, int networkType) {
            if (MobileSignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "onDataConnectionStateChanged: state=" + state + " type=" + networkType);
            }
            MobileSignalController.this.mDataState = state;
            MobileSignalController.this.mDataNetType = networkType;
            MobileSignalController.this.mDataNetType = NetworkTypeUtils.getDataNetTypeFromServiceState(MobileSignalController.this.mDataNetType, MobileSignalController.this.mServiceState);
            MobileSignalController.this.mSvLteController.onDataConnectionStateChanged(state, networkType);
            MobileSignalController.this.updateTelephony();
        }

        public void onPreciseDataConnectionStateChanged(PreciseDataConnectionState state) {
            if (PluginFactory.getStatusBarPlugin(MobileSignalController.this.mContext).customizeBehaviorSet() == BehaviorSet.OP01_BS) {
                MobileSignalController.this.mSvLteController.onPreciseDataConnectionStateChanged(state);
                ((MobileState) MobileSignalController.this.mCurrentState).mShowDataActivityIcon = MobileSignalController.this.mSvLteController.isShowDataActivityIcon();
                MobileSignalController.this.notifyListenersIfNecessary();
            }
        }

        public void onDataActivity(int direction) {
            if (MobileSignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "onDataActivity: direction=" + direction);
            }
            MobileSignalController.this.mSvLteController.onDataActivity(direction);
            MobileSignalController.this.setActivity(direction);
        }

        public void onCarrierNetworkChange(boolean active) {
            if (MobileSignalController.DEBUG) {
                Log.d(MobileSignalController.this.mTag, "onCarrierNetworkChange: active=" + active);
            }
            ((MobileState) MobileSignalController.this.mCurrentState).carrierNetworkChangeMode = active;
            MobileSignalController.this.updateTelephony();
        }
    }

    static class MobileState extends State {
        boolean airplaneMode;
        boolean carrierNetworkChangeMode;
        int customizedState;
        boolean dataConnected;
        boolean dataSim;
        boolean isDefault;
        boolean isEmergency;
        int networkIcon;
        String networkName;
        String networkNameData;

        MobileState() {
        }

        public void copyFrom(State s) {
            super.copyFrom(s);
            MobileState state = (MobileState) s;
            this.dataSim = state.dataSim;
            this.networkName = state.networkName;
            this.networkNameData = state.networkNameData;
            this.dataConnected = state.dataConnected;
            this.isDefault = state.isDefault;
            this.isEmergency = state.isEmergency;
            this.airplaneMode = state.airplaneMode;
            this.carrierNetworkChangeMode = state.carrierNetworkChangeMode;
            this.networkIcon = state.networkIcon;
            this.customizedState = state.customizedState;
        }

        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',');
            builder.append("dataSim=").append(this.dataSim).append(',');
            builder.append("networkName=").append(this.networkName).append(',');
            builder.append("networkNameData=").append(this.networkNameData).append(',');
            builder.append("dataConnected=").append(this.dataConnected).append(',');
            builder.append("isDefault=").append(this.isDefault).append(',');
            builder.append("isEmergency=").append(this.isEmergency).append(',');
            builder.append("airplaneMode=").append(this.airplaneMode).append(',');
            builder.append("networkIcon").append(this.networkIcon).append(',');
            builder.append("customizedState=").append(this.customizedState).append(',');
            builder.append("carrierNetworkChangeMode=").append(this.carrierNetworkChangeMode);
        }

        public boolean equals(Object o) {
            if (!super.equals(o) || !Objects.equals(((MobileState) o).networkName, this.networkName) || !Objects.equals(((MobileState) o).networkNameData, this.networkNameData) || ((MobileState) o).dataSim != this.dataSim || ((MobileState) o).dataConnected != this.dataConnected || ((MobileState) o).isEmergency != this.isEmergency || ((MobileState) o).airplaneMode != this.airplaneMode || ((MobileState) o).carrierNetworkChangeMode != this.carrierNetworkChangeMode || ((MobileState) o).networkIcon != this.networkIcon || ((MobileState) o).customizedState != this.customizedState) {
                return false;
            }
            if (((MobileState) o).isDefault == this.isDefault) {
                return true;
            }
            return false;
        }
    }

    public MobileSignalController(Context context, Config config, boolean hasMobileData, TelephonyManager phone, CallbackHandler callbackHandler, NetworkControllerImpl networkController, SubscriptionInfo info, SubscriptionDefaults defaults, Looper receiverLooper) {
        String networkName;
        super("MobileSignalController(" + info.getSubscriptionId() + ")", context, 0, callbackHandler, networkController);
        this.mConfig = config;
        this.mPhone = phone;
        this.mDefaults = defaults;
        this.mSubscriptionInfo = info;
        this.mMobileIconExt = PluginManager.getMobileIconExt(context);
        this.mStatusBarExt = PluginManager.getSystemUIStatusBarExt(context);
        this.mPhoneStateListener = new MobilePhoneStateListener(info.getSubscriptionId(), receiverLooper);
        this.mNetworkNameSeparator = getStringIfExists(R.string.status_bar_network_name_separator);
        this.mNetworkNameDefault = getStringIfExists(17039971);
        this.mSvLteController = new SvLteController(this.mContext, info);
        mapIconSets();
        if (info.getCarrierName() != null) {
            networkName = info.getCarrierName().toString();
        } else {
            networkName = this.mNetworkNameDefault;
        }
        MobileState mobileState = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).networkName = networkName;
        mobileState.networkName = networkName;
        mobileState = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).networkNameData = networkName;
        mobileState.networkNameData = networkName;
        mobileState = (MobileState) this.mLastState;
        ((MobileState) this.mCurrentState).enabled = hasMobileData;
        mobileState.enabled = hasMobileData;
        mobileState = (MobileState) this.mLastState;
        IconGroup iconGroup = this.mDefaultIcons;
        ((MobileState) this.mCurrentState).iconGroup = iconGroup;
        mobileState.iconGroup = iconGroup;
        updateDataSim();
    }

    public void setConfiguration(Config config) {
        this.mConfig = config;
        mapIconSets();
        updateTelephony();
    }

    public void setAirplaneMode(boolean airplaneMode) {
        ((MobileState) this.mCurrentState).airplaneMode = airplaneMode;
        notifyListenersIfNecessary();
    }

    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        boolean isValidated = validatedTransports.get(this.mTransportType);
        ((MobileState) this.mCurrentState).isDefault = connectedTransports.get(this.mTransportType);
        MobileState mobileState = (MobileState) this.mCurrentState;
        int i = (isValidated || !((MobileState) this.mCurrentState).isDefault) ? 1 : 0;
        mobileState.inetCondition = i;
        Log.d(this.mTag, "mCurrentState.inetCondition = " + ((MobileState) this.mCurrentState).inetCondition);
        ((MobileState) this.mCurrentState).inetCondition = this.mMobileIconExt.customizeMobileNetCondition(((MobileState) this.mCurrentState).inetCondition);
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean carrierNetworkChangeMode) {
        ((MobileState) this.mCurrentState).carrierNetworkChangeMode = carrierNetworkChangeMode;
        updateTelephony();
    }

    public void registerListener() {
        this.mPhone.listen(this.mPhoneStateListener, 70113);
    }

    public void unregisterListener() {
        this.mPhone.listen(this.mPhoneStateListener, 0);
    }

    private void mapIconSets() {
        this.mNetworkToIconLookup.clear();
        this.mNetworkToIconLookup.put(5, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(6, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(12, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(14, TelephonyIcons.THREE_G);
        this.mNetworkToIconLookup.put(3, TelephonyIcons.THREE_G);
        if (this.mConfig.showAtLeast3G) {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.THREE_G);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.THREE_G);
            this.mDefaultIcons = TelephonyIcons.THREE_G;
        } else {
            this.mNetworkToIconLookup.put(0, TelephonyIcons.UNKNOWN);
            this.mNetworkToIconLookup.put(2, TelephonyIcons.E);
            this.mNetworkToIconLookup.put(4, TelephonyIcons.ONE_X);
            this.mNetworkToIconLookup.put(7, TelephonyIcons.ONE_X);
            this.mDefaultIcons = TelephonyIcons.G;
        }
        MobileIconGroup hGroup = TelephonyIcons.THREE_G;
        MobileIconGroup hGroupPlus = TelephonyIcons.THREE_G;
        if (this.mConfig.hspaDataDistinguishable) {
            hGroup = TelephonyIcons.H;
            hGroupPlus = TelephonyIcons.H_PLUS;
        }
        this.mNetworkToIconLookup.put(8, hGroup);
        this.mNetworkToIconLookup.put(9, hGroup);
        this.mNetworkToIconLookup.put(10, hGroup);
        this.mNetworkToIconLookup.put(15, hGroupPlus);
        if (this.mConfig.show4gForLte) {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.FOUR_G);
        } else {
            this.mNetworkToIconLookup.put(13, TelephonyIcons.LTE);
        }
        this.mNetworkToIconLookup.put(18, TelephonyIcons.WFC);
        this.mNetworkToIconLookup.put(139, TelephonyIcons.FOUR_GA);
    }

    public void notifyListeners() {
        boolean z;
        boolean z2;
        MobileIconGroup icons = (MobileIconGroup) getIcons();
        String contentDescription = getStringIfExists(getContentDescription());
        String dataContentDescription = getStringIfExists(icons.mDataContentDescription);
        boolean showDataIcon = !((MobileState) this.mCurrentState).dataConnected ? ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.ROAMING : true;
        boolean z3 = ((MobileState) this.mCurrentState).enabled && !((MobileState) this.mCurrentState).airplaneMode;
        IconState statusIcon = new IconState(z3, getCurrentIconId(), contentDescription);
        int qsTypeIcon = 0;
        IconState qsIcon = null;
        String description = null;
        if (((MobileState) this.mCurrentState).dataSim) {
            qsTypeIcon = showDataIcon ? icons.mQsDataType : 0;
            z3 = ((MobileState) this.mCurrentState).enabled ? !((MobileState) this.mCurrentState).isEmergency : false;
            qsIcon = new IconState(z3, getQsCurrentIconId(), contentDescription);
            description = ((MobileState) this.mCurrentState).isEmergency ? null : ((MobileState) this.mCurrentState).networkName;
        }
        if (!((MobileState) this.mCurrentState).dataConnected || ((MobileState) this.mCurrentState).carrierNetworkChangeMode) {
            z = false;
        } else {
            z = ((MobileState) this.mCurrentState).activityIn;
        }
        if (!((MobileState) this.mCurrentState).dataConnected || ((MobileState) this.mCurrentState).carrierNetworkChangeMode) {
            z2 = false;
        } else {
            z2 = ((MobileState) this.mCurrentState).activityOut;
        }
        int i = !((MobileState) this.mCurrentState).isDefault ? ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.ROAMING ? 1 : 0 : 1;
        int typeIcon = showDataIcon & i ? icons.mDataType : 0;
        int networkIcon = ((MobileState) this.mCurrentState).networkIcon;
        int dataActivity = 0;
        if (((MobileState) this.mCurrentState).dataConnected) {
            if (((MobileState) this.mCurrentState).activityIn && ((MobileState) this.mCurrentState).activityOut) {
                dataActivity = 3;
            } else if (((MobileState) this.mCurrentState).activityIn) {
                dataActivity = 1;
            } else if (((MobileState) this.mCurrentState).activityOut) {
                dataActivity = 2;
            }
        }
        int primarySimIcon = TelephonyIcons.getPrimarySimIcon(isRoaming(), this.mSubscriptionInfo.getSubscriptionId());
        if (FeatureOptionUtils.isMTK_CT6M_SUPPORT() && ((MobileState) this.mCurrentState).iconGroup == TelephonyIcons.ROAMING) {
            typeIcon = ((MobileState) this.mCurrentState).dataConnected ? ((MobileIconGroup) this.mNetworkToIconLookup.get(this.mDataNetType)).mDataType : 0;
        }
        this.mCallbackHandler.setMobileDataIndicators(statusIcon, qsIcon, typeIcon, networkIcon, qsTypeIcon, z, z2, dataActivity, primarySimIcon, dataContentDescription, description, icons.mIsWide, this.mSubscriptionInfo.getSubscriptionId());
        updateVolte();
        this.mNetworkController.refreshPlmnCarrierLabel();
    }

    protected MobileState cleanState() {
        return new MobileState();
    }

    private boolean hasService() {
        boolean z = true;
        if (SvLteController.isMediatekSVLteDcSupport(this.mSubscriptionInfo)) {
            return this.mSvLteController.hasService();
        }
        if (this.mServiceState == null) {
            return false;
        }
        switch (this.mServiceState.getVoiceRegState()) {
            case 1:
            case 2:
                if (this.mServiceState.getDataRegState() != 0) {
                    z = false;
                }
                return z;
            case 3:
                return false;
            default:
                return true;
        }
    }

    private boolean isCdma() {
        return (this.mSignalStrength == null || this.mSignalStrength.isGsm()) ? false : true;
    }

    public boolean isEmergencyOnly() {
        if (SvLteController.isMediatekSVLteDcSupport(this.mSubscriptionInfo)) {
            return this.mSvLteController.isEmergencyOnly();
        }
        return this.mServiceState != null ? this.mServiceState.isEmergencyOnly() : false;
    }

    private boolean isRoaming() {
        boolean z = true;
        boolean z2 = false;
        if (isCdma()) {
            int iconMode = this.mServiceState.getCdmaEriIconMode();
            if (this.mServiceState.getCdmaEriIconIndex() == 1) {
                z = false;
            } else if (!(iconMode == 0 || iconMode == 1)) {
                z = false;
            }
            return z;
        }
        if (this.mServiceState != null) {
            z2 = this.mServiceState.getRoaming();
        }
        return z2;
    }

    private void updateVolte() {
        if (this.mNetworkController.getImsRegState() == 0 && this.mSubscriptionInfo.getSubscriptionId() == this.mNetworkController.getImsSubId() && !this.mNetworkController.isImsRegOverWfc()) {
            int slotId = this.mSubscriptionInfo.getSimSlotIndex();
            int iconId = isLteNetWork() ? this.mNetworkController.getVolteIconId(slotId) : 0;
            Log.d(this.mTag, "updateVolte: slotId: " + slotId + " iconId: " + iconId);
            this.mCallbackHandler.setVolteStatusIcon(iconId);
        }
    }

    public boolean isLteNetWork() {
        if (this.mDataNetType == 13 || this.mDataNetType == 139) {
            return true;
        }
        return false;
    }

    private boolean isCarrierNetworkChangeActive() {
        return ((MobileState) this.mCurrentState).carrierNetworkChangeMode;
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.provider.Telephony.SPN_STRINGS_UPDATED")) {
            updateNetworkName(intent.getBooleanExtra("showSpn", false), intent.getStringExtra("spn"), intent.getStringExtra("spnData"), intent.getBooleanExtra("showPlmn", false), intent.getStringExtra("plmn"));
            notifyListenersIfNecessary();
        } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
            updateDataSim();
            notifyListenersIfNecessary();
        }
    }

    private void updateDataSim() {
        boolean z = true;
        int defaultDataSub = this.mDefaults.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSub)) {
            MobileState mobileState = (MobileState) this.mCurrentState;
            if (defaultDataSub != this.mSubscriptionInfo.getSubscriptionId()) {
                z = false;
            }
            mobileState.dataSim = z;
            return;
        }
        ((MobileState) this.mCurrentState).dataSim = true;
    }

    void updateNetworkName(boolean showSpn, String spn, String dataSpn, boolean showPlmn, String plmn) {
        if (CHATTY) {
            Log.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn + " spn=" + spn + " dataSpn=" + dataSpn + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        StringBuilder strData = new StringBuilder();
        if (showPlmn && plmn != null) {
            str.append(plmn);
            strData.append(plmn);
        }
        if (showSpn && spn != null) {
            if (str.length() != 0) {
                str.append(this.mNetworkNameSeparator);
            }
            str.append(spn);
        }
        if (str.length() != 0) {
            ((MobileState) this.mCurrentState).networkName = str.toString();
        } else {
            ((MobileState) this.mCurrentState).networkName = this.mNetworkNameDefault;
        }
        if (showSpn && dataSpn != null) {
            if (strData.length() != 0) {
                strData.append(this.mNetworkNameSeparator);
            }
            strData.append(dataSpn);
        }
        if (strData.length() != 0) {
            ((MobileState) this.mCurrentState).networkNameData = strData.toString();
            return;
        }
        ((MobileState) this.mCurrentState).networkNameData = this.mNetworkNameDefault;
    }

    private int getSignalLevel() {
        int level;
        if (this.mSignalStrength.isGsm() || !this.mConfig.alwaysShowCdmaRssi) {
            level = this.mSignalStrength.getLevel();
        } else {
            level = this.mSignalStrength.getCdmaLevel();
        }
        this.mLevelArray[this.mLastIndex] = level;
        this.mLastIndex = (this.mLastIndex + 1) % 3;
        int cnt = 0;
        int tmp = 0;
        for (int i = 0; i < 3; i++) {
            if (this.mLevelArray[i] != 65535) {
                tmp += this.mLevelArray[i];
                cnt++;
            }
        }
        if (cnt > 0) {
            level = ((tmp + cnt) - 1) / cnt;
        }
        Log.d(this.mTag, "last level(" + this.mLevelArray[0] + "," + this.mLevelArray[1] + "," + this.mLevelArray[2] + "), mLastIndex:" + this.mLastIndex + ",current level:" + level);
        return level;
    }

    private final void updateTelephony() {
        boolean z;
        boolean z2 = false;
        if (DEBUG) {
            Log.d(this.mTag, "updateTelephonySignalStrength: hasService=" + hasService() + " ss=" + this.mSignalStrength);
        }
        MobileState mobileState = (MobileState) this.mCurrentState;
        if (!hasService() || this.mSignalStrength == null) {
            z = false;
        } else {
            z = true;
        }
        mobileState.connected = z;
        handleIWLANNetwork();
        if (((MobileState) this.mCurrentState).connected) {
            ((MobileState) this.mCurrentState).level = getSignalLevel();
        }
        if (this.mNetworkToIconLookup.indexOfKey(this.mDataNetType) >= 0) {
            ((MobileState) this.mCurrentState).iconGroup = (IconGroup) this.mNetworkToIconLookup.get(this.mDataNetType);
        } else {
            ((MobileState) this.mCurrentState).iconGroup = this.mDefaultIcons;
        }
        mobileState = (MobileState) this.mCurrentState;
        if (((MobileState) this.mCurrentState).connected && this.mDataState == 2) {
            z2 = true;
        }
        mobileState.dataConnected = z2;
        ((MobileState) this.mCurrentState).customizedState = PluginFactory.getStatusBarPlugin(this.mContext).customizeMobileState(this.mServiceState, ((MobileState) this.mCurrentState).customizedState);
        if (isCarrierNetworkChangeActive()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isRoaming()) {
            ((MobileState) this.mCurrentState).iconGroup = TelephonyIcons.ROAMING;
        }
        if (isEmergencyOnly() != ((MobileState) this.mCurrentState).isEmergency) {
            ((MobileState) this.mCurrentState).isEmergency = isEmergencyOnly();
            this.mNetworkController.recalculateEmergency();
        }
        if (!(((MobileState) this.mCurrentState).networkName != this.mNetworkNameDefault || this.mServiceState == null || TextUtils.isEmpty(this.mServiceState.getOperatorAlphaShort()))) {
            ((MobileState) this.mCurrentState).networkName = this.mServiceState.getOperatorAlphaShort();
        }
        ((MobileState) this.mCurrentState).networkIcon = NetworkTypeUtils.getNetworkTypeIcon(this.mServiceState, this.mConfig, hasService());
        notifyListenersIfNecessary();
    }

    private void handleIWLANNetwork() {
        if (((MobileState) this.mCurrentState).connected && this.mServiceState.getDataNetworkType() == 18 && this.mServiceState.getVoiceNetworkType() == 0) {
            Log.d(this.mTag, "Current is IWLAN network only, no cellular network available");
            ((MobileState) this.mCurrentState).connected = false;
        }
        ((MobileState) this.mCurrentState).connected = this.mStatusBarExt.updateSignalStrengthWifiOnlyMode(this.mServiceState, ((MobileState) this.mCurrentState).connected);
    }

    void setActivity(int activity) {
        boolean z;
        boolean z2 = true;
        MobileState mobileState = (MobileState) this.mCurrentState;
        if (activity == 3) {
            z = true;
        } else if (activity == 1) {
            z = true;
        } else {
            z = false;
        }
        mobileState.activityIn = z;
        mobileState = (MobileState) this.mCurrentState;
        if (!(activity == 3 || activity == 2)) {
            z2 = false;
        }
        mobileState.activityOut = z2;
        notifyListenersIfNecessary();
    }

    public void dump(PrintWriter pw) {
        super.dump(pw);
        pw.println("  mSubscription=" + this.mSubscriptionInfo + ",");
        pw.println("  mServiceState=" + this.mServiceState + ",");
        pw.println("  mSignalStrength=" + this.mSignalStrength + ",");
        pw.println("  mDataState=" + this.mDataState + ",");
        pw.println("  mDataNetType=" + this.mDataNetType + ",");
    }

    public SubscriptionInfo getControllerSubInfo() {
        return this.mSubscriptionInfo;
    }

    public boolean getControllserHasService() {
        return hasService();
    }

    public boolean getControllserIsRoaming() {
        return isRoaming();
    }

    public boolean getControllserIsCdma() {
        return isCdma();
    }

    public int getControllserDataNetType() {
        return this.mDataNetType;
    }

    public ServiceState getControllserServiceState() {
        return this.mServiceState;
    }

    public int getControllserDataState() {
        return this.mDataState;
    }
}
