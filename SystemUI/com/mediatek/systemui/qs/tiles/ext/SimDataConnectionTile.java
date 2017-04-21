package com.mediatek.systemui.qs.tiles.ext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.keyguard.R$styleable;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.QSTileView;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.util.List;

public class SimDataConnectionTile extends QSTile<BooleanState> {
    private static final String TAG = "SimDataConnectionTile";
    private final Icon mSimConnectionIcon = new QsIconWrapper(this.mSimConnectionIconWrapper);
    private final IconIdWrapper mSimConnectionIconWrapper = new IconIdWrapper();
    private SimDataSwitchStateMachine mSimDataSwitchStateMachine;

    private class FullFillQSTileView extends QSTileView {
        public FullFillQSTileView(Context context) {
            super(context);
        }

        private int exactly(int size) {
            return MeasureSpec.makeMeasureSpec(size, 1073741824);
        }

        private View labelView() {
            return getDual() ? getDualLabel() : getLabel();
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = MeasureSpec.getSize(heightMeasureSpec);
            int iconSpec = exactly(getIconSizePx());
            getQSIcon().measure(MeasureSpec.makeMeasureSpec(w, Integer.MIN_VALUE), h);
            labelView().measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, Integer.MIN_VALUE));
            if (getDual()) {
                getDivider().measure(widthMeasureSpec, exactly(getDivider().getLayoutParams().height));
            }
            getTopBackgroundView().measure(widthMeasureSpec, exactly((getIconSizePx() + getTilePaddingBelowIconPx()) + getTilePaddingTopPx()));
            setMeasuredDimension(w, h);
        }

        protected void updateRippleSize(int width, int height) {
            int cy;
            int cx = width / 2;
            if (getDual()) {
                cy = getQSIcon().getTop() + (getIconSizePx() / 2);
            } else {
                cy = height / 2;
            }
            int rad = (int) (((float) getIconSizePx()) * 1.25f);
            getRipple().setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
        }
    }

    public enum SIMConnState {
        SIM1_E_D,
        SIM1_E_E,
        SIM1_D_D,
        SIM1_D_E,
        SIM2_E_D,
        SIM2_E_E,
        SIM2_D_D,
        SIM2_D_E,
        NO_SIM,
        SIM1_E_F,
        SIM1_D_F,
        SIM2_E_F,
        SIM2_D_F
    }

    private class SimDataSwitchStateMachine {
        private static /* synthetic */ int[] -com_mediatek_systemui_qs_tiles_ext_SimDataConnectionTile$SIMConnStateSwitchesValues = null;
        private static final int EVENT_SWITCH_TIME_OUT = 2000;
        private static final int SWITCH_TIME_OUT_LENGTH = 30000;
        private static final String TRANSACTION_START = "com.android.mms.transaction.START";
        private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
        final /* synthetic */ int[] $SWITCH_TABLE$com$mediatek$systemui$qs$tiles$ext$SimDataConnectionTile$SIMConnState;
        private SIMConnState mCurrentSimConnState = SIMConnState.NO_SIM;
        private Handler mDataTimerHandler = new Handler() {
            public void handleMessage(Message msg) {
                int simFrom = msg.arg1;
                int simTo = msg.arg2;
                switch (msg.what) {
                    case SimDataSwitchStateMachine.EVENT_SWITCH_TIME_OUT /*2000*/:
                        Log.d(SimDataConnectionTile.TAG, "switching time out..... switch from " + simFrom + " to " + simTo);
                        if (!SimDataConnectionTile.this.isWifiOnlyDevice()) {
                            SimDataSwitchStateMachine.this.refresh();
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        private boolean mIsAirlineMode;
        protected boolean mIsUserSwitching;
        boolean mMmsOngoing;
        private PhoneStateListener[] mPhoneStateListener;
        boolean mSimConnStateTrackerReady;
        private BroadcastReceiver mSimStateIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int i = 0;
                String action = intent.getAction();
                Log.d(SimDataConnectionTile.TAG, "onReceive action is " + action);
                if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    boolean enabled = intent.getBooleanExtra("state", false);
                    Log.d(SimDataConnectionTile.TAG, "airline mode changed: state is " + enabled);
                    if (SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                        SimDataSwitchStateMachine.this.setAirplaneMode(enabled);
                    }
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                } else if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                    DataState state = SimDataSwitchStateMachine.this.getMobileDataState(intent);
                    boolean isApnTypeChange = false;
                    String types = intent.getStringExtra("apnType");
                    if (types != null) {
                        String[] typeArray = types.split(",");
                        int length = typeArray.length;
                        while (i < length) {
                            if ("default".equals(typeArray[i])) {
                                isApnTypeChange = true;
                                break;
                            }
                            i++;
                        }
                    }
                    if (!isApnTypeChange) {
                        return;
                    }
                    if ((state == DataState.CONNECTED || state == DataState.DISCONNECTED) && !SimDataSwitchStateMachine.this.isMmsOngoing()) {
                        SimDataSwitchStateMachine.this.updateSimConnTile();
                    }
                } else if (action.equals("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE")) {
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                } else if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                    SimDataSwitchStateMachine.this.unRegisterPhoneStateListener();
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                    SimDataSwitchStateMachine.this.registerPhoneStateListener();
                } else if (action.equals(SimDataSwitchStateMachine.TRANSACTION_START)) {
                    if (!SimDataConnectionTile.this.isWifiOnlyDevice() && SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                        SimDataSwitchStateMachine.this.setIsMmsOnging(true);
                        SimDataSwitchStateMachine.this.updateSimConnTile();
                    }
                } else if (action.equals(SimDataSwitchStateMachine.TRANSACTION_STOP) && !SimDataConnectionTile.this.isWifiOnlyDevice() && SimDataSwitchStateMachine.this.mSimConnStateTrackerReady) {
                    SimDataSwitchStateMachine.this.setIsMmsOnging(false);
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                }
            }
        };
        private int mSlotCount = 0;
        TelephonyManager mTelephonyManager;

        private static /* synthetic */ int[] -getcom_mediatek_systemui_qs_tiles_ext_SimDataConnectionTile$SIMConnStateSwitchesValues() {
            if (-com_mediatek_systemui_qs_tiles_ext_SimDataConnectionTile$SIMConnStateSwitchesValues != null) {
                return -com_mediatek_systemui_qs_tiles_ext_SimDataConnectionTile$SIMConnStateSwitchesValues;
            }
            int[] iArr = new int[SIMConnState.values().length];
            try {
                iArr[SIMConnState.NO_SIM.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[SIMConnState.SIM1_D_D.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[SIMConnState.SIM1_D_E.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[SIMConnState.SIM1_D_F.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[SIMConnState.SIM1_E_D.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[SIMConnState.SIM1_E_E.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[SIMConnState.SIM1_E_F.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[SIMConnState.SIM2_D_D.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[SIMConnState.SIM2_D_E.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                iArr[SIMConnState.SIM2_D_F.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                iArr[SIMConnState.SIM2_E_D.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                iArr[SIMConnState.SIM2_E_E.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                iArr[SIMConnState.SIM2_E_F.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            -com_mediatek_systemui_qs_tiles_ext_SimDataConnectionTile$SIMConnStateSwitchesValues = iArr;
            return iArr;
        }

        public SIMConnState getCurrentSimConnState() {
            return this.mCurrentSimConnState;
        }

        public SimDataSwitchStateMachine() {
            this.mTelephonyManager = (TelephonyManager) SimDataConnectionTile.this.mContext.getSystemService("phone");
            IntentFilter simIntentFilter = new IntentFilter();
            simIntentFilter.addAction("android.intent.action.AIRPLANE_MODE");
            simIntentFilter.addAction(TRANSACTION_START);
            simIntentFilter.addAction(TRANSACTION_STOP);
            simIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
            simIntentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
            simIntentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            simIntentFilter.addAction("android.intent.action.ANY_DATA_STATE");
            SimDataConnectionTile.this.mContext.registerReceiver(this.mSimStateIntentReceiver, simIntentFilter);
            this.mSlotCount = SIMHelper.getSlotCount();
            this.mPhoneStateListener = new PhoneStateListener[this.mSlotCount];
        }

        private void addConnTile() {
            this.mSimConnStateTrackerReady = true;
        }

        private void updateSimConnTile() {
            onActualStateChange(SimDataConnectionTile.this.mContext, null);
            SimDataConnectionTile.this.refreshState();
        }

        public void refresh() {
            onActualStateChange(SimDataConnectionTile.this.mContext, null);
            setUserSwitching(false);
        }

        public void onActualStateChange(Context context, Intent intent) {
            List<SubscriptionInfo> infos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
            boolean sim1Enable = isSimEnable(infos, 0);
            boolean sim2Enable = isSimEnable(infos, 1);
            boolean sim1Conn = false;
            boolean sim2Conn = false;
            int dataConnectionId = SubscriptionManager.getDefaultDataSubId();
            if (SubscriptionManager.getSlotId(dataConnectionId) == 0) {
                sim1Conn = true;
                sim2Conn = false;
            } else if (SubscriptionManager.getSlotId(dataConnectionId) == 1) {
                sim1Conn = false;
                sim2Conn = true;
            }
            Log.d(SimDataConnectionTile.TAG, "SimConnStateTracker onActualStateChange sim1Enable = " + sim1Enable + ", sim2Enable = " + sim2Enable);
            if (sim1Enable || sim2Enable) {
                boolean dataConnected = isDataConnected();
                Log.d(SimDataConnectionTile.TAG, "onActualStateChange, dataConnected = " + dataConnected + ", sim1Enable = " + sim1Enable + ", sim2Enable = " + sim2Enable + ", sim1Conn = " + sim1Conn + ", sim2Conn = " + sim2Conn);
                if (dataConnected) {
                    if (sim1Enable && sim2Enable) {
                        if (sim1Conn) {
                            this.mCurrentSimConnState = SIMConnState.SIM1_E_E;
                        } else {
                            this.mCurrentSimConnState = SIMConnState.SIM2_E_E;
                        }
                    } else if (sim1Enable || !sim2Enable) {
                        if (sim1Enable && !sim2Enable) {
                            if (isSimInsertedWithUnAvaliable(infos, 1) && sim2Conn) {
                                this.mCurrentSimConnState = SIMConnState.SIM2_E_F;
                            } else {
                                this.mCurrentSimConnState = SIMConnState.SIM1_D_E;
                            }
                        }
                    } else if (isSimInsertedWithUnAvaliable(infos, 0) && sim1Conn) {
                        this.mCurrentSimConnState = SIMConnState.SIM1_E_F;
                    } else {
                        this.mCurrentSimConnState = SIMConnState.SIM2_D_E;
                    }
                } else if (sim1Enable && sim2Enable) {
                    if (sim1Conn) {
                        this.mCurrentSimConnState = SIMConnState.SIM1_E_D;
                    } else {
                        this.mCurrentSimConnState = SIMConnState.SIM2_E_D;
                    }
                } else if (sim1Enable || !sim2Enable) {
                    if (sim1Enable && !sim2Enable) {
                        if (isSimInsertedWithUnAvaliable(infos, 1) && sim2Conn) {
                            this.mCurrentSimConnState = SIMConnState.SIM2_E_F;
                        } else {
                            this.mCurrentSimConnState = SIMConnState.SIM1_D_D;
                        }
                    }
                } else if (isSimInsertedWithUnAvaliable(infos, 0) && sim1Conn) {
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_F;
                } else {
                    this.mCurrentSimConnState = SIMConnState.SIM2_D_D;
                }
            } else if (isSimInsertedWithUnAvaliable(infos, 0) && sim1Conn) {
                this.mCurrentSimConnState = SIMConnState.SIM1_D_F;
            } else if (isSimInsertedWithUnAvaliable(infos, 1) && sim2Conn) {
                this.mCurrentSimConnState = SIMConnState.SIM2_D_F;
            } else {
                this.mCurrentSimConnState = SIMConnState.NO_SIM;
            }
            setUserSwitching(false);
        }

        private boolean isSimEnable(List<SubscriptionInfo> infos, int slotId) {
            if (!SimDataConnectionTile.this.isSimInsertedBySlot(infos, slotId) || isAirplaneMode() || !isRadioOn(slotId) || isSimLocked(slotId)) {
                return false;
            }
            return true;
        }

        private boolean isSimInsertedWithUnAvaliable(List<SubscriptionInfo> infos, int slotId) {
            if (SimDataConnectionTile.this.isSimInsertedBySlot(infos, slotId)) {
                return (!isRadioOn(slotId) || isAirplaneMode()) ? true : isSimLocked(slotId);
            } else {
                return false;
            }
        }

        private boolean isRadioOn(int slotId) {
            return SIMHelper.isRadioOn(SIMHelper.getFirstSubInSlot(slotId));
        }

        private boolean isSimLocked(int slotId) {
            int simState = TelephonyManager.getDefault().getSimState(slotId);
            boolean bSimLocked = (simState == 2 || simState == 3) ? true : simState == 4;
            Log.d(SimDataConnectionTile.TAG, "isSimLocked, slotId=" + slotId + " simState=" + simState + " bSimLocked= " + bSimLocked);
            return bSimLocked;
        }

        public void toggleState(Context context) {
            enterNextState(this.mCurrentSimConnState);
        }

        private void enterNextState(SIMConnState state) {
            Log.d(SimDataConnectionTile.TAG, "enterNextState state is " + state);
            switch (-getcom_mediatek_systemui_qs_tiles_ext_SimDataConnectionTile$SIMConnStateSwitchesValues()[state.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 8:
                case 9:
                case 10:
                    Log.d(SimDataConnectionTile.TAG, "No Sim or one Sim do nothing!");
                    return;
                case 5:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM2_E_D;
                    switchDataDefaultSIM(1);
                    return;
                case 6:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM2_E_E;
                    switchDataDefaultSIM(1);
                    return;
                case 7:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim1 to Sim2! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    switchDataDefaultSIM(1);
                    return;
                case 11:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_D;
                    switchDataDefaultSIM(0);
                    return;
                case R$styleable.GlowPadView_feedbackCount /*12*/:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    this.mCurrentSimConnState = SIMConnState.SIM1_E_E;
                    switchDataDefaultSIM(0);
                    return;
                case R$styleable.GlowPadView_alwaysTrackFinger /*13*/:
                    Log.d(SimDataConnectionTile.TAG, "Try to switch from Sim2 to Sim1! mSimCurrentCurrentState=" + this.mCurrentSimConnState);
                    switchDataDefaultSIM(0);
                    return;
                default:
                    return;
            }
        }

        private void switchDataDefaultSIM(int slotId) {
            if (!SimDataConnectionTile.this.isWifiOnlyDevice()) {
                setUserSwitching(true);
                handleDataConnectionChange(slotId);
            }
        }

        private void handleDataConnectionChange(int newSlot) {
            Log.d(SimDataConnectionTile.TAG, "handleDataConnectionChange, newSlot=" + newSlot);
            if (SubscriptionManager.getSlotId(SubscriptionManager.getDefaultDataSubId()) != newSlot) {
                this.mDataTimerHandler.sendEmptyMessageDelayed(EVENT_SWITCH_TIME_OUT, 30000);
                List<SubscriptionInfo> si = SubscriptionManager.from(SimDataConnectionTile.this.mContext).getActiveSubscriptionInfoList();
                if (si != null && si.size() > 0) {
                    boolean dataEnabled = this.mTelephonyManager.getDataEnabled();
                    for (int i = 0; i < si.size(); i++) {
                        SubscriptionInfo subInfo = (SubscriptionInfo) si.get(i);
                        int subId = subInfo.getSubscriptionId();
                        if (newSlot == subInfo.getSimSlotIndex()) {
                            Log.d(SimDataConnectionTile.TAG, "handleDataConnectionChange. newSlot = " + newSlot + " subId = " + subId);
                            SubscriptionManager.from(SimDataConnectionTile.this.mContext).setDefaultDataSubId(subId);
                            if (dataEnabled) {
                                this.mTelephonyManager.setDataEnabled(subId, true);
                            }
                        } else if (dataEnabled) {
                            this.mTelephonyManager.setDataEnabled(subId, false);
                        }
                    }
                }
            }
        }

        public boolean isClickable() {
            List<SubscriptionInfo> infos = SubscriptionManager.from(SimDataConnectionTile.this.mContext).getActiveSubscriptionInfoList();
            if (!SimDataConnectionTile.this.isSimInsertedBySlot(infos, 0) && !SimDataConnectionTile.this.isSimInsertedBySlot(infos, 1)) {
                return false;
            }
            if ((!isRadioOn(0) && !isRadioOn(1)) || isAirplaneMode() || isMmsOngoing() || isUserSwitching()) {
                return false;
            }
            return true;
        }

        private boolean isDataConnected() {
            return TelephonyManager.getDefault().getDataState() == 2;
        }

        private void setIsMmsOnging(boolean ongoing) {
            this.mMmsOngoing = ongoing;
        }

        private boolean isMmsOngoing() {
            return this.mMmsOngoing;
        }

        private void setAirplaneMode(boolean airplaneMode) {
            this.mIsAirlineMode = airplaneMode;
        }

        private boolean isAirplaneMode() {
            return this.mIsAirlineMode;
        }

        private void setUserSwitching(boolean userSwitching) {
            this.mIsUserSwitching = userSwitching;
        }

        private boolean isUserSwitching() {
            return this.mIsUserSwitching;
        }

        private DataState getMobileDataState(Intent intent) {
            String str = intent.getStringExtra("state");
            if (str != null) {
                return (DataState) Enum.valueOf(DataState.class, str);
            }
            return DataState.DISCONNECTED;
        }

        private void registerPhoneStateListener() {
            for (int i = 0; i < this.mSlotCount; i++) {
                int subId = SIMHelper.getFirstSubInSlot(i);
                if (subId >= 0) {
                    this.mPhoneStateListener[i] = getPhoneStateListener(subId, i);
                    this.mTelephonyManager.listen(this.mPhoneStateListener[i], 1);
                } else {
                    this.mPhoneStateListener[i] = null;
                }
            }
        }

        private PhoneStateListener getPhoneStateListener(int subId, final int slotId) {
            return new PhoneStateListener(subId) {
                public void onServiceStateChanged(ServiceState state) {
                    Log.d(SimDataConnectionTile.TAG, "PhoneStateListener:onServiceStateChanged, slot " + slotId + " servicestate = " + state);
                    SimDataSwitchStateMachine.this.updateSimConnTile();
                }
            };
        }

        private void unRegisterPhoneStateListener() {
            for (int i = 0; i < this.mSlotCount; i++) {
                if (this.mPhoneStateListener[i] != null) {
                    this.mTelephonyManager.listen(this.mPhoneStateListener[i], 0);
                }
            }
        }
    }

    public SimDataConnectionTile(Host host) {
        super(host);
        host.getNetworkController();
        init();
    }

    public QSTileView createTileView(Context context) {
        return new FullFillQSTileView(context);
    }

    private void init() {
        this.mSimDataSwitchStateMachine = new SimDataSwitchStateMachine();
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    protected void handleClick() {
        if (this.mSimDataSwitchStateMachine.isClickable()) {
            this.mSimDataSwitchStateMachine.toggleState(this.mContext);
        }
        refreshState();
    }

    public void setListening(boolean listening) {
    }

    public int getMetricsCategory() {
        return 111;
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
        PluginFactory.getQuickSettingsPlugin(this.mContext).customizeSimDataConnectionTile(this.mSimDataSwitchStateMachine.getCurrentSimConnState().ordinal(), this.mSimConnectionIconWrapper);
        state.icon = this.mSimConnectionIcon;
    }

    private boolean isWifiOnlyDevice() {
        if (((ConnectivityManager) this.mContext.getSystemService("connectivity")).isNetworkSupported(0)) {
            return false;
        }
        return true;
    }

    private boolean isSimInsertedBySlot(List<SubscriptionInfo> infos, int slotId) {
        if (slotId >= SIMHelper.getSlotCount()) {
            return false;
        }
        if (infos == null || infos.size() <= 0) {
            Log.d(TAG, "isSimInsertedBySlot, SubscriptionInfo is null");
            return false;
        }
        for (SubscriptionInfo info : infos) {
            if (info.getSimSlotIndex() == slotId) {
                return true;
            }
        }
        return false;
    }
}
