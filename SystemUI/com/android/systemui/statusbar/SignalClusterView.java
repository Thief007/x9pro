package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionInfo;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.mediatek.systemui.ext.ISignalClusterExt;
import com.mediatek.systemui.statusbar.defaultaccount.DefaultAccountStatus;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.ISignalClusterInfo;
import com.mediatek.systemui.statusbar.extcb.PhoneStateExt;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import com.mediatek.telephony.TelephonyManagerEx;
import java.util.ArrayList;
import java.util.List;

public class SignalClusterView extends LinearLayout implements SignalCallback, SecurityControllerCallback, Tunable {
    static final int[] DATA_ACTIVITY = new int[]{R.drawable.stat_sys_signal_in, R.drawable.stat_sys_signal_out, R.drawable.stat_sys_signal_inout};
    static final boolean DEBUG = Log.isLoggable("SignalClusterView", 3);
    ImageView mAirplane;
    private String mAirplaneContentDescription;
    private int mAirplaneIconId;
    private boolean mBlockAirplane;
    private boolean mBlockEthernet;
    private boolean mBlockMobile;
    private boolean mBlockWifi;
    private float mDarkIntensity;
    private ImageView mDefAccountIcon;
    private int mEndPadding;
    private int mEndPaddingNothingVisible;
    ImageView mEthernet;
    ImageView mEthernetDark;
    private String mEthernetDescription;
    ViewGroup mEthernetGroup;
    private int mEthernetIconId;
    private boolean mEthernetVisible;
    private ImageView mHDVoiceIcon;
    private int mIconTint;
    private boolean mIsAirplaneMode;
    private int mLastAirplaneIconId;
    private int mLastEthernetIconId;
    private int mLastWifiStrengthId;
    LinearLayout mMobileSignalGroup;
    NetworkControllerImpl mNC;
    ImageView mNoSims;
    View mNoSimsCombo;
    ImageView mNoSimsDark;
    private boolean mNoSimsVisible;
    private ArrayList<PhoneState> mPhoneStates;
    SecurityController mSC;
    private int mSecondaryTelephonyPadding;
    private ISignalClusterExt mSignalClusterExt;
    private ImageView mVolteIcon;
    ImageView mVpn;
    private boolean mVpnVisible;
    private int mWideTypeIconStartPadding;
    ImageView mWifi;
    View mWifiAirplaneSpacer;
    ImageView mWifiDark;
    private String mWifiDescription;
    ViewGroup mWifiGroup;
    View mWifiSignalSpacer;
    private int mWifiStrengthId;
    private boolean mWifiVisible;

    private class PhoneState {
        private int mDataActivity;
        private int mDataActivityId = 0;
        private ImageView mDataActivityInOut;
        private boolean mIsMobileTypeIconWide;
        private ImageView mMobile;
        private ImageView mMobileDark;
        private ImageView mMobileDataActivity;
        private String mMobileDescription;
        private ViewGroup mMobileGroup;
        private FrameLayout mMobileNetworkDataGroup;
        private int mMobileStrengthId = 0;
        private ImageView mMobileType;
        private String mMobileTypeDescription;
        private int mMobileTypeId = 0;
        private boolean mMobileVisible = false;
        private int mNetworkIcon = 0;
        private ImageView mNetworkType;
        private ImageView mPrimarySimCard;
        private int mPrimarySimIconId = 0;
        private int mSignalBackgroundIconId;
        private final int mSubId;

        public PhoneState(int subId, Context context) {
            if (FeatureOptionUtils.isMTK_CT6M_SUPPORT()) {
                this.mMobileDataActivity = new ImageView(context);
                this.mPrimarySimCard = new ImageView(context);
                this.mMobileNetworkDataGroup = new FrameLayout(context);
                this.mMobileNetworkDataGroup.setLayoutParams(new LayoutParams(-2, -2));
            }
            setViews((ViewGroup) LayoutInflater.from(context).inflate(R.layout.mobile_signal_group_ext, null));
            this.mSubId = subId;
        }

        public void setViews(ViewGroup root) {
            this.mMobileGroup = root;
            this.mMobile = (ImageView) root.findViewById(R.id.mobile_signal);
            this.mMobileDark = (ImageView) root.findViewById(R.id.mobile_signal_dark);
            this.mMobileType = (ImageView) root.findViewById(R.id.mobile_type);
            this.mNetworkType = (ImageView) root.findViewById(R.id.network_type);
            if (FeatureOptionUtils.isMTK_CT6M_SUPPORT()) {
                if (this.mMobileType.getParent() != null) {
                    ((ViewGroup) this.mMobileType.getParent()).addView(this.mPrimarySimCard, new LayoutParams(-2, -2));
                }
                if (this.mMobileType.getParent() != null) {
                    ((ViewGroup) this.mMobileType.getParent()).removeView(this.mMobileType);
                }
                this.mMobileNetworkDataGroup.addView(this.mMobileType, new LayoutParams(-2, -2, 17));
                this.mMobileNetworkDataGroup.addView(this.mMobileDataActivity, new LayoutParams(-2, -2, 17));
                int addViewIndex = this.mMobileGroup.indexOfChild(this.mNetworkType);
                if (addViewIndex >= 0) {
                    this.mMobileGroup.addView(this.mMobileNetworkDataGroup, addViewIndex);
                }
            }
            this.mDataActivityInOut = (ImageView) root.findViewById(R.id.data_inout);
        }

        public boolean apply(boolean isSecondaryIcon) {
            int -get2;
            int i = 0;
            if (!this.mMobileVisible || SignalClusterView.this.mIsAirplaneMode) {
                this.mMobileGroup.setVisibility(8);
            } else {
                Animatable ad;
                this.mMobile.setImageResource(this.mMobileStrengthId);
                Drawable mobileDrawable = this.mMobile.getDrawable();
                if (mobileDrawable instanceof Animatable) {
                    ad = (Animatable) mobileDrawable;
                    if (!ad.isRunning()) {
                        ad.start();
                    }
                }
                this.mMobileDark.setImageResource(this.mMobileStrengthId);
                Drawable mobileDarkDrawable = this.mMobileDark.getDrawable();
                if (mobileDarkDrawable instanceof Animatable) {
                    ad = (Animatable) mobileDarkDrawable;
                    if (!ad.isRunning()) {
                        ad.start();
                    }
                }
                if (FeatureOptionUtils.isMTK_CT6M_SUPPORT()) {
                    this.mMobileDataActivity.setImageResource(SignalClusterView.getDataActivityIcon(this.mDataActivity));
                    this.mPrimarySimCard.setImageResource(this.mPrimarySimIconId);
                }
                this.mDataActivityInOut.setImageDrawable(null);
                this.mDataActivityInOut.setVisibility(8);
                this.mMobileType.setImageResource(this.mMobileTypeId);
                this.mMobileGroup.setContentDescription(this.mMobileTypeDescription + " " + this.mMobileDescription);
                this.mMobileGroup.setVisibility(0);
            }
            setCustomizeViewProperty();
            ViewGroup viewGroup = this.mMobileGroup;
            if (isSecondaryIcon) {
                -get2 = SignalClusterView.this.mSecondaryTelephonyPadding;
            } else {
                -get2 = 0;
            }
            viewGroup.setPaddingRelative(-get2, 0, 0, 0);
            ImageView imageView = this.mMobile;
            if (this.mIsMobileTypeIconWide) {
                -get2 = SignalClusterView.this.mWideTypeIconStartPadding;
            } else {
                -get2 = 0;
            }
            imageView.setPaddingRelative(-get2, 0, 0, 0);
            imageView = this.mMobileDark;
            if (this.mIsMobileTypeIconWide) {
                -get2 = SignalClusterView.this.mWideTypeIconStartPadding;
            } else {
                -get2 = 0;
            }
            imageView.setPaddingRelative(-get2, 0, 0, 0);
            if (SignalClusterView.DEBUG) {
                String str = "SignalClusterView";
                String str2 = "mobile: %s sig=%d typ=%d";
                Object[] objArr = new Object[3];
                objArr[0] = this.mMobileVisible ? "VISIBLE" : "GONE";
                objArr[1] = Integer.valueOf(this.mMobileStrengthId);
                objArr[2] = Integer.valueOf(this.mMobileTypeId);
                Log.d(str, String.format(str2, objArr));
            }
            imageView = this.mMobileType;
            if (this.mMobileTypeId != 0) {
                -get2 = 0;
            } else {
                -get2 = 8;
            }
            imageView.setVisibility(-get2);
            if (FeatureOptionUtils.isMTK_CT6M_SUPPORT()) {
                if (this.mDataActivity == 0 || this.mMobileType.getVisibility() != 0) {
                    this.mMobileDataActivity.setVisibility(8);
                } else {
                    this.mMobileDataActivity.setVisibility(0);
                }
                ImageView imageView2 = this.mPrimarySimCard;
                if (this.mPrimarySimIconId == 0) {
                    i = 8;
                }
                imageView2.setVisibility(i);
            }
            return this.mMobileVisible;
        }

        public void populateAccessibilityEvent(AccessibilityEvent event) {
            if (this.mMobileVisible && this.mMobileGroup != null && this.mMobileGroup.getContentDescription() != null) {
                event.getText().add(this.mMobileGroup.getContentDescription());
            }
        }

        public void setIconTint(int tint, float darkIntensity) {
            SignalClusterView.this.applyDarkIntensity(darkIntensity, this.mMobile, this.mMobileDark);
            SignalClusterView.this.setTint(this.mMobileType, tint);
        }

        private void setCustomizeViewProperty() {
            setNetworkIcon();
            setIndicatorUnderSignalIcon(this.mSignalBackgroundIconId);
        }

        private void setNetworkIcon() {
            if (FeatureOptions.MTK_CTA_SET) {
            }
            if (this.mNetworkIcon == 0) {
                this.mNetworkType.setVisibility(8);
                return;
            }
            this.mNetworkType.setImageResource(this.mNetworkIcon);
            this.mNetworkType.setVisibility(0);
        }

        private void setIndicatorUnderSignalIcon(int iconId) {
            if (iconId == 0) {
                this.mMobileGroup.setBackgroundDrawable(null);
            } else {
                this.mMobileGroup.setBackgroundResource(iconId);
            }
        }
    }

    private class SignalClusterInfo implements ISignalClusterInfo {
        private SignalClusterInfo() {
        }

        public boolean isWifiIndicatorsVisible() {
            return SignalClusterView.this.mWifiVisible;
        }

        public boolean isNoSimsVisible() {
            return SignalClusterView.this.mNoSimsVisible;
        }

        public boolean isAirplaneMode() {
            return SignalClusterView.this.mIsAirplaneMode;
        }

        public int getWideTypeIconStartPadding() {
            return SignalClusterView.this.mWideTypeIconStartPadding;
        }

        public int getSecondaryTelephonyPadding() {
            return SignalClusterView.this.mSecondaryTelephonyPadding;
        }
    }

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mNoSimsVisible = false;
        this.mVpnVisible = false;
        this.mEthernetVisible = false;
        this.mEthernetIconId = 0;
        this.mLastEthernetIconId = -1;
        this.mWifiVisible = false;
        this.mWifiStrengthId = 0;
        this.mLastWifiStrengthId = -1;
        this.mIsAirplaneMode = false;
        this.mAirplaneIconId = 0;
        this.mLastAirplaneIconId = -1;
        this.mPhoneStates = new ArrayList();
        this.mIconTint = -1;
        this.mSignalClusterExt = null;
        this.mSignalClusterExt = PluginFactory.getStatusBarPlugin(getContext()).customizeSignalCluster();
        this.mSignalClusterExt.setSignalClusterInfo(new SignalClusterInfo());
    }

    public void onTuningChanged(String key, String newValue) {
        if ("icon_blacklist".equals(key)) {
            ArraySet<String> blockList = StatusBarIconController.getIconBlacklist(newValue);
            boolean blockAirplane = blockList.contains("airplane");
            boolean blockMobile = blockList.contains("mobile");
            boolean blockWifi = blockList.contains("wifi");
            boolean blockEthernet = blockList.contains("ethernet");
            if (blockAirplane == this.mBlockAirplane && blockMobile == this.mBlockMobile && blockEthernet == this.mBlockEthernet) {
                if (blockWifi != this.mBlockWifi) {
                }
            }
            this.mBlockAirplane = blockAirplane;
            this.mBlockMobile = blockMobile;
            this.mBlockEthernet = blockEthernet;
            this.mBlockWifi = blockWifi;
            this.mNC.removeSignalCallback(this);
            this.mNC.addSignalCallback(this);
        }
    }

    public void setNetworkController(NetworkControllerImpl nc) {
        if (DEBUG) {
            Log.d("SignalClusterView", "NetworkController=" + nc);
        }
        this.mNC = nc;
        this.mSignalClusterExt.setNetworkControllerExt(nc.getNetworkControllerExt());
    }

    public void setSecurityController(SecurityController sc) {
        if (DEBUG) {
            Log.d("SignalClusterView", "SecurityController=" + sc);
        }
        this.mSC = sc;
        this.mSC.addCallback(this);
        this.mVpnVisible = this.mSC.isVpnEnabled();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mWideTypeIconStartPadding = getContext().getResources().getDimensionPixelSize(R.dimen.wide_type_icon_start_padding);
        this.mSecondaryTelephonyPadding = getContext().getResources().getDimensionPixelSize(R.dimen.secondary_telephony_padding);
        this.mEndPadding = getContext().getResources().getDimensionPixelSize(R.dimen.signal_cluster_battery_padding);
        this.mEndPaddingNothingVisible = getContext().getResources().getDimensionPixelSize(R.dimen.no_signal_cluster_battery_padding);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mVpn = (ImageView) findViewById(R.id.vpn);
        this.mEthernetGroup = (ViewGroup) findViewById(R.id.ethernet_combo);
        this.mEthernet = (ImageView) findViewById(R.id.ethernet);
        this.mEthernetDark = (ImageView) findViewById(R.id.ethernet_dark);
        this.mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);
        this.mWifi = (ImageView) findViewById(R.id.wifi_signal);
        this.mWifiDark = (ImageView) findViewById(R.id.wifi_signal_dark);
        this.mAirplane = (ImageView) findViewById(R.id.airplane);
        this.mNoSims = (ImageView) findViewById(R.id.no_sims);
        this.mNoSimsDark = (ImageView) findViewById(R.id.no_sims_dark);
        this.mNoSimsCombo = findViewById(R.id.no_sims_combo);
        this.mWifiAirplaneSpacer = findViewById(R.id.wifi_airplane_spacer);
        this.mWifiSignalSpacer = findViewById(R.id.wifi_signal_spacer);
        this.mMobileSignalGroup = (LinearLayout) findViewById(R.id.mobile_signal_group);
        for (PhoneState state : this.mPhoneStates) {
            this.mMobileSignalGroup.addView(state.mMobileGroup);
        }
        TunerService.get(this.mContext).addTunable((Tunable) this, "icon_blacklist");
        this.mDefAccountIcon = (ImageView) findViewById(R.id.default_sim_type);
        this.mVolteIcon = (ImageView) findViewById(R.id.volte_indicator);
        this.mHDVoiceIcon = (ImageView) findViewById(R.id.hd_voice_icon);
        this.mHDVoiceIcon.setVisibility(8);
        this.mSignalClusterExt.onAttachedToWindow(this.mMobileSignalGroup, this.mNoSims);
        apply();
        applyIconTint();
    }

    protected void onDetachedFromWindow() {
        this.mVpn = null;
        this.mEthernetGroup = null;
        this.mEthernet = null;
        this.mWifiGroup = null;
        this.mWifi = null;
        this.mAirplane = null;
        this.mMobileSignalGroup.removeAllViews();
        this.mMobileSignalGroup = null;
        TunerService.get(this.mContext).removeTunable(this);
        this.mSignalClusterExt.onDetachedFromWindow();
        super.onDetachedFromWindow();
    }

    public void onStateChanged() {
        post(new Runnable() {
            public void run() {
                SignalClusterView.this.mVpnVisible = SignalClusterView.this.mSC.isVpnEnabled();
                SignalClusterView.this.apply();
            }
        });
    }

    public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
        boolean z = false;
        if (statusIcon.visible && !this.mBlockWifi) {
            z = true;
        }
        this.mWifiVisible = z;
        this.mWifiStrengthId = statusIcon.icon;
        this.mWifiDescription = statusIcon.contentDescription;
        apply();
    }

    public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType, int networkType, int qsType, boolean activityIn, boolean activityOut, int dataActivity, int primarySimIcon, String typeContentDescription, String description, boolean isWide, int subId) {
        PhoneState state = getState(subId);
        if (state != null) {
            boolean z = statusIcon.visible && !this.mBlockMobile;
            state.mMobileVisible = z;
            state.mMobileStrengthId = statusIcon.icon;
            state.mMobileTypeId = statusType;
            state.mMobileDescription = statusIcon.contentDescription;
            state.mMobileTypeDescription = typeContentDescription;
            if (statusType == 0) {
                isWide = false;
            }
            state.mIsMobileTypeIconWide = isWide;
            state.mNetworkIcon = networkType;
            state.mDataActivity = dataActivity;
            state.mPrimarySimIconId = primarySimIcon;
            this.mSignalClusterExt.setMobileDataIndicators(subId, state.mMobileVisible, state.mMobileGroup, state.mNetworkType, (ViewGroup) state.mMobile.getParent(), state.mMobile, state.mMobileType, state.mMobileStrengthId, state.mMobileTypeId, state.mMobileDescription, state.mMobileTypeDescription, state.mIsMobileTypeIconWide);
            setDataActivityMTK(activityIn, activityOut, subId);
            apply();
        }
    }

    public void setEthernetIndicators(IconState state) {
        boolean z = false;
        if (state.visible && !this.mBlockEthernet) {
            z = true;
        }
        this.mEthernetVisible = z;
        this.mEthernetIconId = state.icon;
        this.mEthernetDescription = state.contentDescription;
        apply();
    }

    public void setNoSims(boolean show) {
        boolean z = false;
        if (show && !this.mBlockMobile) {
            z = true;
        }
        this.mNoSimsVisible = z;
        this.mNoSimsVisible = PluginFactory.getStatusBarPlugin(this.mContext).customizeHasNoSims(this.mNoSimsVisible);
        apply();
    }

    public void setSubs(List<SubscriptionInfo> subs) {
        if (!hasCorrectSubs(subs)) {
            this.mPhoneStates.clear();
            if (this.mMobileSignalGroup != null) {
                this.mMobileSignalGroup.removeAllViews();
            }
            int n = subs.size();
            for (int i = 0; i < n; i++) {
                inflatePhoneState(((SubscriptionInfo) subs.get(i)).getSubscriptionId());
            }
            if (isAttachedToWindow()) {
                applyIconTint();
            }
            this.mSignalClusterExt.setSubs(subs, inflatePhoneStateExt((List) subs));
        }
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> subs) {
        int N = subs.size();
        if (N != this.mPhoneStates.size()) {
            return false;
        }
        for (int i = 0; i < N; i++) {
            if (((PhoneState) this.mPhoneStates.get(i)).mSubId != ((SubscriptionInfo) subs.get(i)).getSubscriptionId()) {
                return false;
            }
        }
        return true;
    }

    private PhoneState getState(int subId) {
        for (PhoneState state : this.mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        Log.e("SignalClusterView", "Unexpected subscription " + subId);
        return null;
    }

    private PhoneState inflatePhoneState(int subId) {
        PhoneState state = new PhoneState(subId, this.mContext);
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.addView(state.mMobileGroup);
        }
        this.mPhoneStates.add(state);
        return state;
    }

    public void setIsAirplaneMode(IconState icon) {
        boolean z = false;
        if (icon.visible && !this.mBlockAirplane) {
            z = true;
        }
        this.mIsAirplaneMode = z;
        this.mAirplaneIconId = icon.icon;
        this.mAirplaneContentDescription = icon.contentDescription;
        apply();
    }

    public void setMobileDataEnabled(boolean enabled) {
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (!(!this.mEthernetVisible || this.mEthernetGroup == null || this.mEthernetGroup.getContentDescription() == null)) {
            event.getText().add(this.mEthernetGroup.getContentDescription());
        }
        if (!(!this.mWifiVisible || this.mWifiGroup == null || this.mWifiGroup.getContentDescription() == null)) {
            event.getText().add(this.mWifiGroup.getContentDescription());
        }
        for (PhoneState state : this.mPhoneStates) {
            state.populateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEventInternal(event);
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (this.mEthernet != null) {
            this.mEthernet.setImageDrawable(null);
            this.mEthernetDark.setImageDrawable(null);
            this.mLastEthernetIconId = -1;
        }
        if (this.mWifi != null) {
            this.mWifi.setImageDrawable(null);
            this.mWifiDark.setImageDrawable(null);
            this.mLastWifiStrengthId = -1;
        }
        for (PhoneState state : this.mPhoneStates) {
            if (state.mMobile != null) {
                state.mMobile.setImageDrawable(null);
            }
            if (state.mMobileType != null) {
                state.mMobileType.setImageDrawable(null);
            }
        }
        if (this.mAirplane != null) {
            this.mAirplane.setImageDrawable(null);
            this.mLastAirplaneIconId = -1;
        }
        this.mSignalClusterExt.onRtlPropertiesChanged(layoutDirection);
        apply();
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    private void apply() {
        if (this.mWifiGroup != null) {
            int i;
            String str;
            String str2;
            Object[] objArr;
            boolean anyMobileVisible;
            int firstMobileTypeId;
            boolean anythingVisible;
            ImageView imageView = this.mVpn;
            if (this.mVpnVisible) {
                i = 0;
            } else {
                i = 8;
            }
            imageView.setVisibility(i);
            if (DEBUG) {
                str = "SignalClusterView";
                str2 = "vpn: %s";
                objArr = new Object[1];
                objArr[0] = this.mVpnVisible ? "VISIBLE" : "GONE";
                Log.d(str, String.format(str2, objArr));
            }
            if (this.mEthernetVisible) {
                if (this.mLastEthernetIconId != this.mEthernetIconId) {
                    this.mEthernet.setImageResource(this.mEthernetIconId);
                    this.mEthernetDark.setImageResource(this.mEthernetIconId);
                    this.mLastEthernetIconId = this.mEthernetIconId;
                }
                this.mEthernetGroup.setContentDescription(this.mEthernetDescription);
                this.mEthernetGroup.setVisibility(0);
            } else {
                this.mEthernetGroup.setVisibility(8);
            }
            if (DEBUG) {
                str = "SignalClusterView";
                str2 = "ethernet: %s";
                objArr = new Object[1];
                objArr[0] = this.mEthernetVisible ? "VISIBLE" : "GONE";
                Log.d(str, String.format(str2, objArr));
            }
            if (this.mWifiVisible) {
                if (this.mWifiStrengthId != this.mLastWifiStrengthId) {
                    this.mWifi.setImageResource(this.mWifiStrengthId);
                    this.mWifiDark.setImageResource(this.mWifiStrengthId);
                    this.mLastWifiStrengthId = this.mWifiStrengthId;
                }
                this.mWifiGroup.setContentDescription(this.mWifiDescription);
                this.mWifiGroup.setVisibility(0);
            } else {
                this.mWifiGroup.setVisibility(8);
            }
            if (DEBUG) {
                str = "SignalClusterView";
                str2 = "wifi: %s sig=%d";
                objArr = new Object[2];
                objArr[0] = this.mWifiVisible ? "VISIBLE" : "GONE";
                objArr[1] = Integer.valueOf(this.mWifiStrengthId);
                Log.d(str, String.format(str2, objArr));
            }
            if (FeatureOptions.MTK_CTA_SET) {
                anyMobileVisible = true;
                firstMobileTypeId = 0;
            } else {
                anyMobileVisible = true;
                firstMobileTypeId = 0;
            }
            for (PhoneState state : this.mPhoneStates) {
                if (state.apply(anyMobileVisible) && !anyMobileVisible) {
                    firstMobileTypeId = state.mMobileTypeId;
                    anyMobileVisible = true;
                }
            }
            if (this.mIsAirplaneMode) {
                if (this.mLastAirplaneIconId != this.mAirplaneIconId) {
                    this.mAirplane.setImageResource(this.mAirplaneIconId);
                    this.mLastAirplaneIconId = this.mAirplaneIconId;
                }
                this.mAirplane.setContentDescription(this.mAirplaneContentDescription);
                this.mAirplane.setVisibility(0);
                if (!TelephonyManagerEx.getDefault().isWifiCallingEnabled(this.mNC.getImsSubId())) {
                    this.mVolteIcon.setVisibility(8);
                }
            } else {
                this.mAirplane.setVisibility(8);
            }
            this.mSignalClusterExt.setHDVoiceIcon(this.mHDVoiceIcon);
            if (this.mIsAirplaneMode && this.mWifiVisible) {
                this.mWifiAirplaneSpacer.setVisibility(0);
            } else {
                this.mWifiAirplaneSpacer.setVisibility(8);
            }
            if (((!anyMobileVisible || firstMobileTypeId == 0) && !this.mNoSimsVisible) || !this.mWifiVisible) {
                this.mWifiSignalSpacer.setVisibility(8);
            } else {
                this.mWifiSignalSpacer.setVisibility(0);
            }
            View view = this.mNoSimsCombo;
            if (this.mNoSimsVisible) {
                i = 0;
            } else {
                i = 8;
            }
            view.setVisibility(i);
            if (FeatureOptionUtils.isMTK_CT6M_SUPPORT() && this.mIsAirplaneMode) {
                this.mNoSimsCombo.setVisibility(8);
            }
            if (this.mNoSimsVisible || this.mWifiVisible || this.mIsAirplaneMode || anyMobileVisible || this.mVpnVisible) {
                anythingVisible = true;
            } else {
                anythingVisible = this.mEthernetVisible;
            }
            setPaddingRelative(0, 0, anythingVisible ? this.mEndPadding : this.mEndPaddingNothingVisible, 0);
            this.mSignalClusterExt.apply();
        }
    }

    public void setIconTint(int tint, float darkIntensity) {
        boolean changed = (tint == this.mIconTint && darkIntensity == this.mDarkIntensity) ? false : true;
        this.mIconTint = tint;
        this.mDarkIntensity = darkIntensity;
        if (changed && isAttachedToWindow()) {
            applyIconTint();
        }
    }

    public void setDataActivityMTK(boolean in, boolean out, int subId) {
        int imgDataActivityID;
        Log.d("SignalClusterView", "setDataActivityMTK(in= " + in + "), out= " + out);
        if (in && out) {
            imgDataActivityID = DATA_ACTIVITY[2];
        } else if (out) {
            imgDataActivityID = DATA_ACTIVITY[1];
        } else if (in) {
            imgDataActivityID = DATA_ACTIVITY[0];
        } else {
            imgDataActivityID = 0;
        }
        getState(subId).mDataActivityId = imgDataActivityID;
    }

    private void applyIconTint() {
        setTint(this.mVpn, this.mIconTint);
        setTint(this.mAirplane, this.mIconTint);
        applyDarkIntensity(this.mDarkIntensity, this.mNoSims, this.mNoSimsDark);
        applyDarkIntensity(this.mDarkIntensity, this.mWifi, this.mWifiDark);
        applyDarkIntensity(this.mDarkIntensity, this.mEthernet, this.mEthernetDark);
        for (int i = 0; i < this.mPhoneStates.size(); i++) {
            ((PhoneState) this.mPhoneStates.get(i)).setIconTint(this.mIconTint, this.mDarkIntensity);
        }
    }

    private void applyDarkIntensity(float darkIntensity, View lightIcon, View darkIcon) {
        lightIcon.setAlpha(1.0f - darkIntensity);
        darkIcon.setAlpha(darkIntensity);
    }

    private void setTint(ImageView v, int tint) {
        v.setImageTintList(ColorStateList.valueOf(tint));
    }

    public void setDefaultAccountStatus(DefaultAccountStatus status) {
        if (status == null) {
            hideAccountStatus();
            return;
        }
        int iconId = status.getAccountStatusIconId();
        if (iconId == 0) {
            this.mDefAccountIcon.setVisibility(8);
            setIndicatorUnderSignalIcon(status);
        } else {
            setDefaultAccountStatusIcon(iconId);
            hideSignalIconIndicator();
        }
    }

    private void setIndicatorUnderSignalIcon(DefaultAccountStatus status) {
        for (PhoneState state : this.mPhoneStates) {
            state.mSignalBackgroundIconId = state.mSubId == status.getSubId() ? status.getDefSignalBackgroundIconId() : 0;
            state.apply(false);
        }
    }

    private void setDefaultAccountStatusIcon(int iconId) {
        this.mDefAccountIcon.setImageResource(iconId);
        if (this.mIsAirplaneMode) {
            this.mDefAccountIcon.setVisibility(8);
        } else {
            this.mDefAccountIcon.setVisibility(0);
        }
    }

    private void hideAccountStatus() {
        hideSignalIconIndicator();
        this.mDefAccountIcon.setVisibility(8);
    }

    private void hideSignalIconIndicator() {
        for (PhoneState state : this.mPhoneStates) {
            state.mSignalBackgroundIconId = 0;
            state.apply(false);
        }
    }

    public void setVolteStatusIcon(int iconId) {
        int i = 0;
        if (iconId > 0) {
            this.mSignalClusterExt.customizeVoLTEIcon(this.mVolteIcon, iconId);
        }
        ImageView imageView = this.mVolteIcon;
        if (iconId <= 0) {
            i = 8;
        }
        imageView.setVisibility(i);
    }

    private final PhoneStateExt[] inflatePhoneStateExt(List<SubscriptionInfo> subs) {
        int slotCount = SIMHelper.getSlotCount();
        PhoneStateExt[] phoneStateExts = new PhoneStateExt[slotCount];
        for (int i = 0; i < slotCount; i++) {
            for (SubscriptionInfo subInfo : subs) {
                if (subInfo.getSimSlotIndex() == i) {
                    phoneStateExts[i] = inflatePhoneStateExt(subInfo);
                    break;
                }
            }
        }
        return phoneStateExts;
    }

    private final PhoneStateExt inflatePhoneStateExt(SubscriptionInfo subInfo) {
        int slotId = subInfo.getSimSlotIndex();
        int subId = subInfo.getSubscriptionId();
        PhoneState state = getOrInflateState(subId);
        PhoneStateExt phoneStateExt = new PhoneStateExt(slotId, subId);
        phoneStateExt.setViews(state.mMobileGroup, state.mNetworkType, (ViewGroup) state.mMobile.getParent(), state.mMobile, state.mMobileType);
        return phoneStateExt;
    }

    private PhoneState getOrInflateState(int subId) {
        for (PhoneState state : this.mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        return inflatePhoneState(subId);
    }

    public static int getDataActivityIcon(int dataActivity) {
        switch (dataActivity) {
            case 1:
                return R.drawable.ct_stat_sys_signal_in;
            case 2:
                return R.drawable.ct_stat_sys_signal_out;
            case 3:
                return R.drawable.ct_stat_sys_signal_inout;
            default:
                return R.drawable.ct_stat_sys_signal_not_inout;
        }
    }
}
