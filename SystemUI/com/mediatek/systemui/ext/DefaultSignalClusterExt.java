package com.mediatek.systemui.ext;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.DefaultEmptyNetworkControllerExt;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.INetworkControllerExt;
import com.mediatek.systemui.statusbar.extcb.ISignalClusterInfo;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.PhoneStateExt;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.util.ArrayList;
import java.util.List;

public abstract class DefaultSignalClusterExt implements ISignalClusterExt {
    private static final String ACTION_SHOW_DISMISS_HDVOICE_ICON = "com.android.incallui.ACTION_SHOW_DISMISS_HD_ICON";
    protected static final boolean DEBUG = (!FeatureOptionUtils.isUserLoad());
    private static final String TAG = "BaseSignalClusterExt";
    protected Context mContext;
    protected List<SubscriptionInfo> mCurrentSubscriptions = new ArrayList();
    protected IconIdWrapper mDefaultRoamingIconId;
    protected IconIdWrapper mDefaultSignalNullIconId;
    protected ImageView mHDVoiceIcon;
    protected boolean mIsAirplaneMode = false;
    protected LinearLayout mMobileSignalGroup;
    protected INetworkControllerExt mNetworkControllerExt;
    protected ImageView mNoSimsView;
    protected boolean mNoSimsVisible = false;
    protected final BasePhoneStateExt[] mPhoneStates;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DefaultSignalClusterExt.ACTION_SHOW_DISMISS_HDVOICE_ICON)) {
                boolean isShowHdVoiceIcon = intent.getBooleanExtra("FLAG_KEY_VISIBILITY", false);
                Log.d(DefaultSignalClusterExt.TAG, "receive ACTION_SHOW_DISMISS_HD_ICON, isShow: " + isShowHdVoiceIcon);
                DefaultSignalClusterExt.this.updateHdVoiceIcon(isShowHdVoiceIcon);
            }
        }
    };
    protected int mSecondaryTelephonyPadding = 0;
    protected boolean mShouldShowDataActivityIcon = true;
    protected ISignalClusterInfo mSignalClusterInfo;
    protected int mSlotCount = 0;
    protected IStatusBarPlugin mStatusBarPlugin;
    protected ImageView mVoLTEIcon;
    protected int mWideTypeIconStartPadding = 0;
    protected boolean mWifiVisible = false;

    protected class BasePhoneStateExt extends PhoneStateExt {
        private static final String TAG = "BaseSignalClusterExt.PhoneStateExt";
        protected int mDataActivity;
        protected boolean mDataConnectioned;
        protected DataType mDataType;
        protected IconIdWrapper mHDVoiceIconId = new IconIdWrapper();
        protected boolean mHasSimService;
        protected boolean mIsSimAvailable;
        protected boolean mIsSimInserted;
        protected boolean mIsSimOffline;
        protected ImageView mMobileDataActivity = null;
        protected IconIdWrapper mMobileDataActivityIconId = new IconIdWrapper();
        protected IconIdWrapper mMobileDataTypeIconId = new IconIdWrapper();
        protected FrameLayout mMobileNetworkDataGroup = new FrameLayout(DefaultSignalClusterExt.this.mContext);
        protected IconIdWrapper mMobileNetworkTypeIconId = new IconIdWrapper();
        protected ImageView mMobileRoamingIndicator = null;
        protected ImageView mMobileSlotIndicator = null;
        protected IconIdWrapper mMobileSlotIndicatorIconId = new IconIdWrapper();
        protected IconIdWrapper mMobileStrengthIconId = new IconIdWrapper();
        protected IconIdWrapper mMobileStrengthNullIconId = new IconIdWrapper();
        protected IconIdWrapper mMobileStrengthOfflineIconId = new IconIdWrapper();
        protected NetworkType mNetworkType;
        protected boolean mRoaming;
        protected int mSignalStrengthLevel;

        public BasePhoneStateExt(int slotId, int subId) {
            super(slotId, subId);
        }

        public void addToSignalGroup() {
            if (DefaultSignalClusterExt.this.mMobileSignalGroup != null) {
                if (this.mSignalClusterCombo.getParent() != null) {
                    ((ViewGroup) this.mSignalClusterCombo.getParent()).removeView(this.mSignalClusterCombo);
                }
                DefaultSignalClusterExt.this.mMobileSignalGroup.addView(this.mSignalClusterCombo);
            }
        }

        public boolean apply() {
            Log.d(TAG, "apply(), State=" + toString());
            if (DefaultSignalClusterExt.this.mIsAirplaneMode) {
                this.mMobileNetworkDataGroup.setVisibility(8);
                this.mMobileGroup.setVisibility(8);
            } else {
                customizeIcons();
                applyMobileSignalStrength();
                applyMobileRoamingIndicator();
                applyMobileSlotIndicator();
                applyNetworkDataSwitch();
                applyNetworkDataType();
                applyMobileDataActivity();
            }
            return this.mMobileVisible;
        }

        protected boolean isNormalVisible() {
            return (!this.mMobileVisible || DefaultSignalClusterExt.this.mIsAirplaneMode) ? false : this.mIsSimAvailable;
        }

        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',').append("mIsSimInserted=").append(this.mIsSimInserted).append(',').append("mHasSimService=").append(this.mHasSimService).append(',').append("mIsSimOffline=").append(this.mIsSimOffline).append(',').append("mDataConnectioned=").append(this.mDataConnectioned).append(',').append("mNetworkType=").append(this.mNetworkType).append(',').append("mDataType=").append(this.mDataType).append(',');
        }

        protected void customizeIcons() {
            if (DefaultSignalClusterExt.DEBUG) {
                Log.d(TAG, "customizeIcons(), mSlotId = " + this.mSlotId + ", mSubId = " + this.mSubId);
            }
            this.mMobileStrengthIconId.setIconId(this.mMobileStrengthIcon);
            this.mMobileStrengthIconId.setResources(DefaultSignalClusterExt.this.mNetworkControllerExt.getResources());
            DefaultSignalClusterExt.this.mStatusBarPlugin.customizeSignalStrengthIcon(this.mSignalStrengthLevel, this.mRoaming, this.mMobileStrengthIconId);
            DefaultSignalClusterExt.this.mStatusBarPlugin.customizeSignalStrengthNullIcon(this.mSlotId, this.mMobileStrengthNullIconId);
            DefaultSignalClusterExt.this.mStatusBarPlugin.customizeSignalStrengthOfflineIcon(this.mSlotId, this.mMobileStrengthOfflineIconId);
            DefaultSignalClusterExt.this.mStatusBarPlugin.customizeDataTypeIcon(this.mMobileDataTypeIconId, this.mRoaming, this.mDataType);
            if (DefaultSignalClusterExt.this.mStatusBarPlugin.customizeBehaviorSet() == BehaviorSet.OP09_BS) {
                DefaultSignalClusterExt.this.mStatusBarPlugin.customizeDataNetworkTypeIcon(this.mMobileNetworkTypeIconId, this.mRoaming, this.mNetworkType, DefaultSignalClusterExt.this.mNetworkControllerExt.getSvLteController(this.mSubId));
            } else {
                DefaultSignalClusterExt.this.mStatusBarPlugin.customizeDataNetworkTypeIcon(this.mMobileNetworkTypeIconId, this.mRoaming, this.mNetworkType);
            }
            DefaultSignalClusterExt.this.mStatusBarPlugin.customizeDataActivityIcon(this.mMobileDataActivityIconId, this.mDataActivity);
            DefaultSignalClusterExt.this.mStatusBarPlugin.customizeSignalIndicatorIcon(this.mSlotId, this.mMobileSlotIndicatorIconId);
            DefaultSignalClusterExt.this.mStatusBarPlugin.customizeHDVoiceIcon(this.mHDVoiceIconId);
            DefaultSignalClusterExt.setImage(this.mMobileType, this.mMobileDataTypeIconId);
            DefaultSignalClusterExt.setImage(this.mMobileNetworkType, this.mMobileNetworkTypeIconId);
            DefaultSignalClusterExt.setImage(this.mMobileDataActivity, this.mMobileDataActivityIconId);
            DefaultSignalClusterExt.setImage(this.mMobileRoamingIndicator, DefaultSignalClusterExt.this.mDefaultRoamingIconId);
            DefaultSignalClusterExt.setImage(this.mMobileSlotIndicator, this.mMobileSlotIndicatorIconId);
            DefaultSignalClusterExt.setImage(DefaultSignalClusterExt.this.mHDVoiceIcon, this.mHDVoiceIconId);
        }

        protected boolean isSignalStrengthNullIcon() {
            return this.mMobileStrengthIcon == DefaultSignalClusterExt.this.mDefaultSignalNullIconId.getIconId();
        }

        protected void applyMobileSignalStrength() {
        }

        protected void applyMobileSlotIndicator() {
        }

        protected void applyMobileRoamingIndicator() {
            if (this.mMobileRoamingIndicator != null) {
                if (DefaultSignalClusterExt.DEBUG) {
                    Log.d(TAG, "applyMobileRoamingIndicator(), mSlotId = " + this.mSlotId + ", mSubId = " + this.mSubId + ", mRoaming = " + this.mRoaming);
                }
                if (this.mRoaming && isNormalVisible() && !isSignalStrengthNullIcon()) {
                    this.mMobileRoamingIndicator.setPaddingRelative(DefaultSignalClusterExt.this.mWideTypeIconStartPadding, this.mMobileRoamingIndicator.getPaddingTop(), this.mMobileRoamingIndicator.getPaddingRight(), this.mMobileRoamingIndicator.getPaddingBottom());
                    this.mMobileRoamingIndicator.setVisibility(0);
                    return;
                }
                this.mMobileRoamingIndicator.setVisibility(4);
            }
        }

        protected void applyMobileDataActivity() {
            if (this.mMobileDataActivity == null) {
                return;
            }
            if (this.mDataConnectioned && isNormalVisible() && !isSignalStrengthNullIcon() && DefaultSignalClusterExt.this.mShouldShowDataActivityIcon) {
                if (DefaultSignalClusterExt.DEBUG) {
                    Log.d(TAG, "applyMobileDataActivity(), mMobileDataActivity is VISIBLE");
                }
                this.mMobileDataActivity.setVisibility(0);
                return;
            }
            if (DefaultSignalClusterExt.DEBUG) {
                Log.d(TAG, "applyMobileDataActivity(), mMobileDataActivity is GONE");
            }
            this.mMobileDataActivity.setVisibility(4);
        }

        protected void applyNetworkDataSwitch() {
            boolean z = true;
            if (DefaultSignalClusterExt.DEBUG) {
                Log.d(TAG, "applyNetworkDataSwitch(), mDataConnectioned = " + this.mDataConnectioned);
            }
            if (this.mMobileNetworkType != null && this.mMobileType != null) {
                if (!isNormalVisible() || isSignalStrengthNullIcon()) {
                    if (DefaultSignalClusterExt.DEBUG) {
                        Log.d(TAG, "applyNetworkDataSwitch(), No SIM inserted/Service or Signal Strength Null: Hide network type icon and data icon");
                    }
                    this.mMobileNetworkDataGroup.setVisibility(8);
                    this.mMobileNetworkType.setVisibility(8);
                    this.mMobileType.setVisibility(8);
                } else {
                    if (DefaultSignalClusterExt.this.mWifiVisible) {
                        if (DefaultSignalClusterExt.DEBUG) {
                            Log.d(TAG, "applyNetworkDataSwitch(), mWifiVisible = true, Show network type icon, Hide data type icon");
                        }
                        this.mMobileNetworkType.setVisibility(0);
                        this.mMobileType.setVisibility(4);
                    } else if (!this.mDataConnectioned || this.mMobileDataTypeIconId.getIconId() <= 0 || this.mMobileDataTypeIcon <= 0) {
                        this.mMobileNetworkType.setVisibility(0);
                        this.mMobileType.setVisibility(4);
                    } else {
                        this.mMobileNetworkType.setVisibility(4);
                        this.mMobileType.setVisibility(0);
                    }
                    this.mMobileNetworkDataGroup.setVisibility(0);
                }
                if (DefaultSignalClusterExt.DEBUG) {
                    boolean z2;
                    String str = TAG;
                    StringBuilder append = new StringBuilder().append("applyNetworkDataSwitch(), mMobileNetworkType isVisible: ");
                    if (this.mMobileNetworkType.getVisibility() == 0) {
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    StringBuilder append2 = append.append(z2).append(", mMobileDataType isVisible: ");
                    if (this.mMobileType.getVisibility() != 0) {
                        z = false;
                    }
                    Log.d(str, append2.append(z).toString());
                }
            }
        }

        protected void applyNetworkDataType() {
        }

        protected boolean shouldShowOffline() {
            return false;
        }
    }

    protected abstract BasePhoneStateExt createPhoneState(int i, int i2, ViewGroup viewGroup, ImageView imageView, ViewGroup viewGroup2, ImageView imageView2, ImageView imageView3);

    public DefaultSignalClusterExt(Context context, IStatusBarPlugin statusBarPlugin) {
        this.mContext = context;
        this.mStatusBarPlugin = statusBarPlugin;
        this.mSlotCount = SIMHelper.getSlotCount();
        this.mPhoneStates = new BasePhoneStateExt[this.mSlotCount];
        this.mDefaultSignalNullIconId = new IconIdWrapper();
        this.mDefaultRoamingIconId = new IconIdWrapper();
        this.mNetworkControllerExt = new DefaultEmptyNetworkControllerExt();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter(ACTION_SHOW_DISMISS_HDVOICE_ICON));
    }

    public void setSignalClusterInfo(ISignalClusterInfo signalClusterInfo) {
        this.mSignalClusterInfo = signalClusterInfo;
    }

    public void setNetworkControllerExt(INetworkControllerExt networkControllerExt) {
        if (networkControllerExt != null) {
            this.mNetworkControllerExt = networkControllerExt;
            this.mNetworkControllerExt.getDefaultSignalNullIcon(this.mDefaultSignalNullIconId);
            this.mNetworkControllerExt.getDefaultRoamingIcon(this.mDefaultRoamingIconId);
        }
    }

    public void customizeVoLTEIcon(ImageView voLTEIcon, int resId) {
        this.mVoLTEIcon = voLTEIcon;
        IconIdWrapper voLTEIconId = new IconIdWrapper();
        this.mStatusBarPlugin.customizeVoLTEIconId(voLTEIconId);
        setImage(this.mVoLTEIcon, voLTEIconId);
    }

    public void setSubs(List<SubscriptionInfo> subs, PhoneStateExt[] orgStates) {
        Log.d(TAG, "setSubs(), subs = " + subs + ", orgStates = " + orgStates);
        this.mCurrentSubscriptions = subs;
        for (int i = 0; i < this.mSlotCount; i++) {
            if (orgStates[i] != null) {
                if (DEBUG) {
                    Log.d(TAG, "setSubs(), inflatePhoneState orgStates = " + orgStates[i]);
                }
                this.mPhoneStates[i] = inflatePhoneState(orgStates[i]);
            } else {
                if (DEBUG) {
                    Log.d(TAG, "setSubs(), createDefaultPhoneState IfNecessary");
                }
                this.mPhoneStates[i] = createDefaultPhoneState(i);
            }
        }
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.removeAllViews();
        }
        for (BasePhoneStateExt state : this.mPhoneStates) {
            if (state != null) {
                state.addToSignalGroup();
            }
        }
    }

    public void setHDVoiceIcon(ImageView hDVoiceIcon) {
        this.mHDVoiceIcon = hDVoiceIcon;
    }

    private void updateHdVoiceIcon(boolean isShowHdVoiceIcon) {
        if (this.mHDVoiceIcon == null) {
            return;
        }
        if (isShowHdVoiceIcon) {
            this.mHDVoiceIcon.setVisibility(0);
        } else {
            this.mHDVoiceIcon.setVisibility(8);
        }
    }

    private BasePhoneStateExt inflatePhoneState(PhoneStateExt orgState) {
        return inflatePhoneState(orgState.mSlotId, orgState.mSubId, orgState.mSignalClusterCombo, orgState.mMobileNetworkType, orgState.mMobileGroup, orgState.mMobileStrength, orgState.mMobileType);
    }

    private BasePhoneStateExt inflatePhoneState(int slotId, int subId, ViewGroup signalClusterCombo, ImageView mobileNetworkType, ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType) {
        if (SubscriptionManager.isValidSlotId(slotId)) {
            this.mPhoneStates[slotId] = createPhoneState(slotId, subId, signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength, mobileType);
            if (DEBUG) {
                Log.d(TAG, "inflatePhoneState(), slotId = " + slotId + ", subId = " + subId + " state = " + this.mPhoneStates[slotId]);
            }
            return this.mPhoneStates[slotId];
        }
        if (DEBUG) {
            Log.d(TAG, "inflatePhoneState(), slotId = " + slotId + ", subId = " + subId + new IllegalArgumentException("INVALID_SIM_SLOT_ID"));
        }
        return null;
    }

    private BasePhoneStateExt getOrInflatePhoneState(int subId, ViewGroup signalClusterCombo, ImageView mobileNetworkType, ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return null;
        }
        int slotId = getSlotId(subId);
        if (!SubscriptionManager.isValidSlotId(slotId)) {
            return null;
        }
        BasePhoneStateExt state = getState(slotId);
        if (state == null) {
            return inflatePhoneState(slotId, subId, signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength, mobileType);
        }
        return state;
    }

    public void setMobileDataIndicators(int subId, boolean mobileVisible, ViewGroup signalClusterCombo, ImageView mobileNetworkType, ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType, int mobileStrengthIconId, int mobileDataTypeIconId, String mobileDescription, String mobileTypeDescription, boolean isMobileTypeIconWide) {
        PhoneStateExt state = getOrInflatePhoneState(subId, signalClusterCombo, mobileNetworkType, mobileGroup, mobileStrength, mobileType);
        if (state != null) {
            state.mMobileVisible = mobileVisible;
            state.mMobileStrengthIcon = mobileStrengthIconId;
            state.mMobileDataTypeIcon = mobileDataTypeIconId;
            state.mMobileDescription = mobileDescription;
            state.mMobileTypeDescription = mobileTypeDescription;
            state.mIsMobileTypeIconWide = isMobileTypeIconWide;
        } else if (!(signalClusterCombo == null || signalClusterCombo.getParent() == null)) {
            ((ViewGroup) signalClusterCombo.getParent()).removeView(signalClusterCombo);
        }
        if (DEBUG) {
            Log.d(TAG, "setMobileDataIndicators(), state = " + state);
        }
    }

    public void onAttachedToWindow(LinearLayout mobileSignalGroup, ImageView noSimsView) {
        this.mMobileSignalGroup = mobileSignalGroup;
        this.mNoSimsView = noSimsView;
        for (BasePhoneStateExt state : this.mPhoneStates) {
            if (state != null) {
                state.addToSignalGroup();
            }
        }
    }

    @SuppressLint({"MissingSuperCall"})
    public void onDetachedFromWindow() {
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.removeAllViews();
            this.mMobileSignalGroup = null;
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
    }

    public void apply() {
        if (this.mMobileSignalGroup != null) {
            this.mNoSimsVisible = this.mSignalClusterInfo.isNoSimsVisible();
            this.mWifiVisible = this.mSignalClusterInfo.isWifiIndicatorsVisible();
            this.mIsAirplaneMode = this.mSignalClusterInfo.isAirplaneMode();
            this.mWideTypeIconStartPadding = this.mSignalClusterInfo.getWideTypeIconStartPadding();
            this.mSecondaryTelephonyPadding = this.mSignalClusterInfo.getSecondaryTelephonyPadding();
            for (BasePhoneStateExt state : this.mPhoneStates) {
                if (state == null) {
                    Log.d(TAG, "apply(), state == null");
                } else {
                    boolean z;
                    state.mIsSimInserted = isSimInsertedBySlot(state.mSlotId);
                    state.mHasSimService = this.mNetworkControllerExt.hasService(state.mSubId);
                    state.mIsSimAvailable = state.mIsSimInserted ? state.mHasSimService : false;
                    if (this.mNetworkControllerExt.isOffline(state.mSubId)) {
                        z = true;
                    } else {
                        z = state.shouldShowOffline();
                    }
                    state.mIsSimOffline = z;
                    state.mDataConnectioned = this.mNetworkControllerExt.isDataConnected(state.mSubId);
                    state.mRoaming = this.mNetworkControllerExt.isRoaming(state.mSubId);
                    state.mSignalStrengthLevel = this.mNetworkControllerExt.getSignalStrengthLevel(state.mSubId);
                    state.mDataType = this.mNetworkControllerExt.getDataType(state.mSubId);
                    state.mNetworkType = this.mNetworkControllerExt.getNetworkType(state.mSubId);
                    state.mDataActivity = this.mNetworkControllerExt.getDataActivity(state.mSubId);
                    if (this.mStatusBarPlugin.customizeBehaviorSet() == BehaviorSet.OP01_BS && this.mNetworkControllerExt.getSvLteController(state.mSubId) != null) {
                        this.mShouldShowDataActivityIcon = this.mNetworkControllerExt.getSvLteController(state.mSubId).isShowDataActivityIcon();
                    }
                    state.apply();
                }
            }
        }
    }

    protected BasePhoneStateExt getState(int slotId) {
        if (slotId < 0 || slotId >= this.mSlotCount) {
            return null;
        }
        return this.mPhoneStates[slotId];
    }

    protected BasePhoneStateExt createDefaultPhoneState(int slotId) {
        return null;
    }

    protected final int getSlotId(int subId) {
        for (SubscriptionInfo subInfo : this.mCurrentSubscriptions) {
            if (subInfo.getSubscriptionId() == subId) {
                return subInfo.getSimSlotIndex();
            }
        }
        return -1;
    }

    protected final int getSubId(int slotId) {
        for (SubscriptionInfo subInfo : this.mCurrentSubscriptions) {
            if (subInfo.getSimSlotIndex() == slotId) {
                return subInfo.getSubscriptionId();
            }
        }
        return -1;
    }

    protected static final void setImage(ImageView imageView, IconIdWrapper icon) {
        if (imageView != null && icon != null) {
            if (icon.getResources() != null) {
                imageView.setImageDrawable(icon.getDrawable());
            } else if (icon.getIconId() == 0) {
                imageView.setImageDrawable(null);
            } else {
                imageView.setImageResource(icon.getIconId());
            }
        }
    }

    protected static final String toString(IconIdWrapper icon) {
        if (icon == null) {
            return "null";
        }
        return icon.toString();
    }

    protected boolean isMultiSlot() {
        return this.mSlotCount > 1;
    }

    protected final boolean isSimInsertedBySlot(int slotId) {
        return SIMHelper.isSimInsertedBySlot(this.mContext, slotId);
    }
}
