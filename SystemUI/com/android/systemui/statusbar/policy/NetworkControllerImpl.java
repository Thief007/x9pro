package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.MathUtils;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MobileDataControllerImpl.Callback;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.mediatek.systemui.statusbar.defaultaccount.DefaultAccountStatus;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.INetworkControllerExt;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.extcb.SvLteController;
import com.mediatek.systemui.statusbar.networktype.NetworkTypeUtils;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class NetworkControllerImpl extends BroadcastReceiver implements NetworkController, DemoMode {
    static final boolean CHATTY = Log.isLoggable("NetworkControllerChat", 3);
    static final boolean DEBUG = Log.isLoggable("NetworkController", 3);
    private final AccessPointControllerImpl mAccessPoints;
    private boolean mAirplaneMode;
    private final CallbackHandler mCallbackHandler;
    private Config mConfig;
    private final BitSet mConnectedTransports;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private List<SubscriptionInfo> mCurrentSubscriptions;
    private int mCurrentUserId;
    private MobileSignalController mDefaultSignalController;
    private boolean mDemoInetCondition;
    private boolean mDemoMode;
    private WifiState mDemoWifiState;
    private int mEmergencySource;
    final EthernetSignalController mEthernetSignalController;
    private final boolean mHasMobileDataFeature;
    private boolean mHasNoSims;
    int mImsRegState;
    int mImsSubId;
    private boolean mInetCondition;
    private boolean mIsEmergency;
    boolean mIsImsOverWfc;
    ServiceState mLastServiceState;
    boolean mListening;
    private Locale mLocale;
    private final MobileDataControllerImpl mMobileDataController;
    final Map<Integer, MobileSignalController> mMobileSignalControllers;
    private final NetworkControllerExt mNetworkControllerExt;
    String[] mNetworkName;
    private final TelephonyManager mPhone;
    private final Handler mReceiverHandler;
    private final Runnable mRegisterListeners;
    int mSlotCount;
    private final SubscriptionDefaults mSubDefaults;
    private OnSubscriptionsChangedListener mSubscriptionListener;
    private final SubscriptionManager mSubscriptionManager;
    private final BitSet mValidatedTransports;
    private final WifiManager mWifiManager;
    final WifiSignalController mWifiSignalController;

    public interface EmergencyListener {
        void setEmergencyCallsOnly(boolean z);
    }

    public static class Config {
        public boolean alwaysShowCdmaRssi = false;
        public boolean hspaDataDistinguishable;
        public boolean show4gForLte = false;
        public boolean showAtLeast3G = false;

        static Config readConfig(Context context) {
            Config config = new Config();
            Resources res = context.getResources();
            config.showAtLeast3G = res.getBoolean(R.bool.config_showMin3G);
            config.alwaysShowCdmaRssi = res.getBoolean(17956958);
            config.show4gForLte = res.getBoolean(R.bool.config_show4GForLTE);
            config.hspaDataDistinguishable = res.getBoolean(R.bool.config_hspa_data_distinguishable);
            return config;
        }
    }

    private class NetworkControllerExt implements INetworkControllerExt {
        private final Config mConfig;
        private boolean mDebug;

        public NetworkControllerExt(Config config) {
            this.mDebug = !FeatureOptionUtils.isUserLoad();
            this.mConfig = config;
            config.hspaDataDistinguishable = PluginFactory.getStatusBarPlugin(NetworkControllerImpl.this.mContext).customizeHspaDistinguishable(config.hspaDataDistinguishable);
            DataType.mapDataTypeSets(config.showAtLeast3G, config.show4gForLte, config.hspaDataDistinguishable);
            NetworkType.mapNetworkTypeSets(config.showAtLeast3G, config.show4gForLte, config.hspaDataDistinguishable);
        }

        public boolean isShowAtLeast3G() {
            return this.mConfig.showAtLeast3G;
        }

        public boolean isHspaDataDistinguishable() {
            return this.mConfig.hspaDataDistinguishable;
        }

        public boolean hasMobileDataFeature() {
            return NetworkControllerImpl.this.mHasMobileDataFeature;
        }

        public Resources getResources() {
            return NetworkControllerImpl.this.mContext.getResources();
        }

        public void getDefaultSignalNullIcon(IconIdWrapper icon) {
            icon.setResources(NetworkControllerImpl.this.mContext.getResources());
            icon.setIconId(R.drawable.stat_sys_signal_null);
        }

        public void getDefaultRoamingIcon(IconIdWrapper icon) {
            icon.setResources(NetworkControllerImpl.this.mContext.getResources());
            icon.setIconId(R.drawable.stat_sys_data_fully_connected_roam);
        }

        public boolean hasService(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            return controller != null ? controller.getControllserHasService() : false;
        }

        public boolean isDataConnected(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            return controller != null ? ((MobileState) controller.getState()).dataConnected : false;
        }

        public boolean isRoaming(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            return controller != null ? controller.getControllserIsRoaming() : false;
        }

        public int getSignalStrengthLevel(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            if (controller != null) {
                return ((MobileState) controller.mCurrentState).level;
            }
            return 0;
        }

        public NetworkType getNetworkType(int subId) {
            return NetworkType.get(getDataNetworkType(subId));
        }

        public DataType getDataType(int subId) {
            int dataNetType;
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            if (controller != null) {
                dataNetType = controller.getControllserDataNetType();
            } else {
                dataNetType = 0;
            }
            return DataType.get(dataNetType);
        }

        public int getDataActivity(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            if (controller == null || !((MobileState) controller.mCurrentState).dataConnected) {
                return 0;
            }
            if (((MobileState) controller.mCurrentState).activityIn && ((MobileState) controller.mCurrentState).activityOut) {
                return 3;
            }
            if (((MobileState) controller.mCurrentState).activityIn) {
                return 2;
            }
            if (((MobileState) controller.mCurrentState).activityOut) {
                return 2;
            }
            return 0;
        }

        public boolean isLteTddSingleDataMode(int subId) {
            return false;
        }

        public SvLteController getSvLteController(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            if (controller != null) {
                return controller.mSvLteController;
            }
            return null;
        }

        public boolean isEmergencyOnly(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            return controller != null ? controller.isEmergencyOnly() : false;
        }

        public boolean isOffline(int subId) {
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            if (controller == null) {
                return false;
            }
            if (SvLteController.isMediatekSVLteDcSupport(controller.getControllerSubInfo()) || SvLteController.isMediatekSRLteDcSupport(controller.getControllerSubInfo())) {
                return controller.mSvLteController.isOffline(getNetworkType(subId));
            }
            return controller.isEmergencyOnly();
        }

        public boolean isRoamingGGMode() {
            boolean isRoamingGGMode = false;
            if (isBehaviorSet(BehaviorSet.OP09_BS)) {
                SubscriptionInfo info = SIMHelper.getSubInfoBySlot(NetworkControllerImpl.this.mContext, 0);
                if (info != null) {
                    MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(info.getSubscriptionId());
                    isRoamingGGMode = (controller == null || controller.getControllserIsCdma()) ? false : true;
                }
            }
            if (this.mDebug) {
                Log.d("StatusBar.NetworkControllerExt", "isRoamingGGMode, slotId = 0, isRoamingGGMode = " + isRoamingGGMode);
            }
            return isRoamingGGMode;
        }

        private final int getDataNetworkType(int subId) {
            int dataNetType = 0;
            MobileSignalController controller = NetworkControllerImpl.this.getMobileSignalController(subId);
            if (controller != null) {
                if (this.mDebug) {
                    Log.d("StatusBar.NetworkControllerExt", "getDataNetworkType(), DataState = " + controller.getControllserDataState() + ", ServiceState = " + controller.getControllserServiceState());
                }
                int networkTypeData = controller.getControllserDataNetType();
                ServiceState serviceState = controller.getControllserServiceState();
                if (serviceState != null) {
                    if (this.mDebug) {
                        Log.d("StatusBar.NetworkControllerExt", "getDataNetworkType: DataNetType = " + controller.getControllserDataNetType() + " / " + serviceState.getDataNetworkType());
                    }
                    if (controller.getControllserDataState() == -1 || controller.getControllserDataState() == 0) {
                        networkTypeData = serviceState.getDataNetworkType();
                    }
                    int cs = serviceState.getVoiceNetworkType();
                    int ps = networkTypeData;
                    if (this.mDebug) {
                        Log.d("StatusBar.NetworkControllerExt", "getNWTypeByPriority(), CS = " + cs + ", PS = " + ps);
                    }
                    dataNetType = getNWTypeByPriority(cs, ps);
                } else {
                    dataNetType = networkTypeData;
                }
                if (SvLteController.isMediatekSVLteDcSupport(controller.getControllerSubInfo())) {
                    dataNetType = controller.mSvLteController.getDataNetTypeWithLTEService(dataNetType);
                }
                Log.d("StatusBar.NetworkControllerExt", "getDataNetworkType: DataNetType=" + controller.getControllserDataNetType() + " / " + dataNetType);
            }
            return dataNetType;
        }

        private final int getNWTypeByPriority(int cs, int ps) {
            if (TelephonyManager.getNetworkClass(cs) > TelephonyManager.getNetworkClass(ps)) {
                return cs;
            }
            return ps;
        }

        private final boolean isBehaviorSet(BehaviorSet behaviorSet) {
            return PluginFactory.getStatusBarPlugin(NetworkControllerImpl.this.mContext).customizeBehaviorSet() == behaviorSet;
        }
    }

    private class SubListener extends OnSubscriptionsChangedListener {
        private SubListener() {
        }

        public void onSubscriptionsChanged() {
            NetworkControllerImpl.this.updateMobileControllers();
        }
    }

    public static class SubscriptionDefaults {
        public int getDefaultVoiceSubId() {
            return SubscriptionManager.getDefaultVoiceSubId();
        }

        public int getDefaultDataSubId() {
            return SubscriptionManager.getDefaultDataSubId();
        }
    }

    public NetworkControllerImpl(Context context, Looper bgLooper) {
        this(context, (ConnectivityManager) context.getSystemService("connectivity"), (TelephonyManager) context.getSystemService("phone"), (WifiManager) context.getSystemService("wifi"), SubscriptionManager.from(context), Config.readConfig(context), bgLooper, new CallbackHandler(), new AccessPointControllerImpl(context, bgLooper), new MobileDataControllerImpl(context), new SubscriptionDefaults());
        this.mReceiverHandler.post(this.mRegisterListeners);
    }

    NetworkControllerImpl(Context context, ConnectivityManager connectivityManager, TelephonyManager telephonyManager, WifiManager wifiManager, SubscriptionManager subManager, Config config, Looper bgLooper, CallbackHandler callbackHandler, AccessPointControllerImpl accessPointController, MobileDataControllerImpl mobileDataController, SubscriptionDefaults defaultsHandler) {
        this.mMobileSignalControllers = new HashMap();
        this.mConnectedTransports = new BitSet();
        this.mValidatedTransports = new BitSet();
        this.mAirplaneMode = false;
        this.mLocale = null;
        this.mCurrentSubscriptions = new ArrayList();
        this.mSlotCount = 0;
        this.mRegisterListeners = new Runnable() {
            public void run() {
                NetworkControllerImpl.this.registerListeners();
            }
        };
        this.mContext = context;
        this.mConfig = config;
        this.mReceiverHandler = new Handler(bgLooper);
        this.mCallbackHandler = callbackHandler;
        this.mSubscriptionManager = subManager;
        this.mSubDefaults = defaultsHandler;
        this.mConnectivityManager = connectivityManager;
        this.mHasMobileDataFeature = this.mConnectivityManager.isNetworkSupported(0);
        this.mNetworkControllerExt = new NetworkControllerExt(this.mConfig);
        this.mSlotCount = SIMHelper.getSlotCount();
        this.mNetworkName = new String[this.mSlotCount];
        this.mPhone = telephonyManager;
        this.mWifiManager = wifiManager;
        this.mLocale = this.mContext.getResources().getConfiguration().locale;
        this.mAccessPoints = accessPointController;
        this.mMobileDataController = mobileDataController;
        this.mMobileDataController.setNetworkController(this);
        this.mMobileDataController.setCallback(new Callback() {
            public void onMobileDataEnabled(boolean enabled) {
                NetworkControllerImpl.this.mCallbackHandler.setMobileDataEnabled(enabled);
            }
        });
        this.mWifiSignalController = new WifiSignalController(this.mContext, this.mHasMobileDataFeature, this.mCallbackHandler, this);
        this.mEthernetSignalController = new EthernetSignalController(this.mContext, this.mCallbackHandler, this);
        updateAirplaneMode(true);
    }

    private void registerListeners() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.registerListener();
        }
        if (this.mSubscriptionListener == null) {
            this.mSubscriptionListener = new SubListener();
        }
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.RSSI_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction("android.provider.Telephony.SPN_STRINGS_UPDATED");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.conn.INET_CONDITION_ACTION");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        addCustomizedAction(filter);
        this.mContext.registerReceiver(this, filter, null, this.mReceiverHandler);
        this.mListening = true;
        updateMobileControllers();
    }

    private void addCustomizedAction(IntentFilter filter) {
        filter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        filter.addAction("com.android.ims.IMS_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
    }

    private void unregisterListeners() {
        this.mListening = false;
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.unregisterListener();
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mSubscriptionListener);
        this.mContext.unregisterReceiver(this);
    }

    public AccessPointController getAccessPointController() {
        return this.mAccessPoints;
    }

    public MobileDataController getMobileDataController() {
        return this.mMobileDataController;
    }

    public void addEmergencyListener(EmergencyListener listener) {
        this.mCallbackHandler.setListening(listener, true);
        this.mCallbackHandler.setEmergencyCallsOnly(isEmergencyOnly());
    }

    public boolean hasMobileDataFeature() {
        return this.mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return this.mPhone.getPhoneType() != 0;
    }

    private MobileSignalController getDataController() {
        int dataSubId = this.mSubDefaults.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(dataSubId)) {
            if (DEBUG) {
                Log.e("NetworkController", "No data sim selected");
            }
            return this.mDefaultSignalController;
        } else if (this.mMobileSignalControllers.containsKey(Integer.valueOf(dataSubId))) {
            return (MobileSignalController) this.mMobileSignalControllers.get(Integer.valueOf(dataSubId));
        } else {
            if (DEBUG) {
                Log.e("NetworkController", "Cannot find controller for data sub: " + dataSubId);
            }
            return this.mDefaultSignalController;
        }
    }

    public String getMobileDataNetworkName() {
        MobileSignalController controller = getDataController();
        return controller != null ? ((MobileState) controller.getState()).networkNameData : "";
    }

    public boolean isEmergencyOnly() {
        if (this.mMobileSignalControllers.size() == 0) {
            this.mEmergencySource = 0;
            return this.mLastServiceState != null ? this.mLastServiceState.isEmergencyOnly() : false;
        }
        int voiceSubId = this.mSubDefaults.getDefaultVoiceSubId();
        if (!SubscriptionManager.isValidSubscriptionId(voiceSubId)) {
            for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
                if (!((MobileState) mobileSignalController.getState()).isEmergency) {
                    this.mEmergencySource = mobileSignalController.mSubscriptionInfo.getSubscriptionId() + 100;
                    if (DEBUG) {
                        Log.d("NetworkController", "Found emergency " + mobileSignalController.mTag);
                    }
                    return false;
                }
            }
        }
        if (this.mMobileSignalControllers.containsKey(Integer.valueOf(voiceSubId))) {
            this.mEmergencySource = voiceSubId + 200;
            if (DEBUG) {
                Log.d("NetworkController", "Getting emergency from " + voiceSubId);
            }
            return ((MobileState) ((MobileSignalController) this.mMobileSignalControllers.get(Integer.valueOf(voiceSubId))).getState()).isEmergency;
        }
        if (DEBUG) {
            Log.e("NetworkController", "Cannot find controller for voice sub: " + voiceSubId);
        }
        this.mEmergencySource = voiceSubId + 300;
        return true;
    }

    void recalculateEmergency() {
        this.mIsEmergency = isEmergencyOnly();
        this.mCallbackHandler.setEmergencyCallsOnly(this.mIsEmergency);
    }

    public void addSignalCallback(SignalCallback cb) {
        this.mCallbackHandler.setListening(cb, true);
        this.mCallbackHandler.setSubs(this.mCurrentSubscriptions);
        this.mCallbackHandler.setIsAirplaneMode(new IconState(this.mAirplaneMode, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode, this.mContext));
        this.mCallbackHandler.setNoSims(this.mHasNoSims);
        this.mWifiSignalController.notifyListeners();
        this.mEthernetSignalController.notifyListeners();
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
    }

    public void removeSignalCallback(SignalCallback cb) {
        this.mCallbackHandler.setListening(cb, false);
    }

    public void setWifiEnabled(final boolean enabled) {
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... args) {
                int wifiApState = NetworkControllerImpl.this.mWifiManager.getWifiApState();
                if (enabled && (wifiApState == 12 || wifiApState == 13)) {
                    NetworkControllerImpl.this.mWifiManager.setWifiApEnabled(null, false);
                }
                NetworkControllerImpl.this.mWifiManager.setWifiEnabled(enabled);
                return null;
            }
        }.execute(new Void[0]);
    }

    public void onReceive(Context context, Intent intent) {
        if (CHATTY) {
            Log.d("NetworkController", "onReceive: intent=" + intent);
        }
        String action = intent.getAction();
        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE") || action.equals("android.net.conn.INET_CONDITION_ACTION")) {
            updateConnectivity();
        } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
            refreshLocale();
            updateAirplaneMode(intent.getBooleanExtra("state", false), false);
        } else if (action.equals("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED")) {
            recalculateEmergency();
        } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
            for (MobileSignalController controller : this.mMobileSignalControllers.values()) {
                controller.handleBroadcast(intent);
            }
        } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
            updateMobileControllers();
        } else if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
            updateMobileControllersEx(intent);
            refreshPlmnCarrierLabel();
        } else if (action.equals("android.intent.action.SERVICE_STATE")) {
            this.mLastServiceState = ServiceState.newFromBundle(intent.getExtras());
            if (this.mMobileSignalControllers.size() == 0) {
                recalculateEmergency();
            }
        } else if (action.equals("com.android.ims.IMS_STATE_CHANGED")) {
            handleIMSAction(intent);
        } else if (action.equals("android.intent.action.ACTION_PREBOOT_IPO")) {
            updateAirplaneMode(false);
        } else {
            int subId = intent.getIntExtra("subscription", -1);
            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                this.mWifiSignalController.handleBroadcast(intent);
            } else if (this.mMobileSignalControllers.containsKey(Integer.valueOf(subId))) {
                ((MobileSignalController) this.mMobileSignalControllers.get(Integer.valueOf(subId))).handleBroadcast(intent);
            } else {
                updateMobileControllers();
            }
        }
    }

    public void onConfigurationChanged() {
        this.mConfig = Config.readConfig(this.mContext);
        this.mReceiverHandler.post(new Runnable() {
            public void run() {
                NetworkControllerImpl.this.handleConfigurationChanged();
            }
        });
    }

    void handleConfigurationChanged() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.setConfiguration(this.mConfig);
        }
        refreshLocale();
    }

    private void updateMobileControllersEx(Intent intent) {
        int detectedType = 4;
        if (intent != null) {
            detectedType = intent.getIntExtra("simDetectStatus", 0);
            Log.d("NetworkController", "updateMobileControllers detectedType: " + detectedType);
        }
        if (detectedType != 3) {
            updateNoSims();
        } else {
            updateMobileControllers();
        }
    }

    private void updateMobileControllers() {
        SIMHelper.updateSIMInfos(this.mContext);
        if (this.mListening) {
            doUpdateMobileControllers();
            return;
        }
        if (DEBUG) {
            Log.d("NetworkController", "updateMobileControllers: it's not listening");
        }
    }

    void doUpdateMobileControllers() {
        List<SubscriptionInfo> subscriptions = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            Log.d("NetworkController", "subscriptions is null");
            subscriptions = Collections.emptyList();
        }
        if (hasCorrectMobileControllers(subscriptions)) {
            updateNoSims();
            return;
        }
        setCurrentSubscriptions(subscriptions);
        updateNoSims();
        recalculateEmergency();
    }

    protected void updateNoSims() {
        boolean hasNoSims = this.mHasMobileDataFeature && this.mMobileSignalControllers.size() == 0;
        if (hasNoSims != this.mHasNoSims) {
            this.mHasNoSims = hasNoSims;
            this.mCallbackHandler.setNoSims(this.mHasNoSims);
        }
    }

    void setCurrentSubscriptions(List<SubscriptionInfo> subscriptions) {
        Collections.sort(subscriptions, new Comparator<SubscriptionInfo>() {
            public int compare(SubscriptionInfo lhs, SubscriptionInfo rhs) {
                if (lhs.getSimSlotIndex() == rhs.getSimSlotIndex()) {
                    return lhs.getSubscriptionId() - rhs.getSubscriptionId();
                }
                return lhs.getSimSlotIndex() - rhs.getSimSlotIndex();
            }
        });
        this.mCurrentSubscriptions = subscriptions;
        HashMap<Integer, MobileSignalController> cachedControllers = new HashMap(this.mMobileSignalControllers);
        this.mMobileSignalControllers.clear();
        int num = subscriptions.size();
        for (int i = 0; i < num; i++) {
            int subId = ((SubscriptionInfo) subscriptions.get(i)).getSubscriptionId();
            if (cachedControllers.containsKey(Integer.valueOf(subId))) {
                MobileSignalController msc = (MobileSignalController) cachedControllers.remove(Integer.valueOf(subId));
                msc.mSubscriptionInfo = (SubscriptionInfo) subscriptions.get(i);
                this.mMobileSignalControllers.put(Integer.valueOf(subId), msc);
            } else {
                MobileSignalController controller = new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mCallbackHandler, this, (SubscriptionInfo) subscriptions.get(i), this.mSubDefaults, this.mReceiverHandler.getLooper());
                this.mMobileSignalControllers.put(Integer.valueOf(subId), controller);
                if (((SubscriptionInfo) subscriptions.get(i)).getSimSlotIndex() == 0) {
                    this.mDefaultSignalController = controller;
                }
                if (this.mListening) {
                    controller.registerListener();
                }
            }
        }
        if (this.mListening) {
            for (Integer key : cachedControllers.keySet()) {
                if (cachedControllers.get(key) == this.mDefaultSignalController) {
                    this.mDefaultSignalController = null;
                }
                ((MobileSignalController) cachedControllers.get(key)).unregisterListener();
            }
        }
        this.mCallbackHandler.setSubs(subscriptions);
        notifyAllListeners();
        pushConnectivityToSignals();
        updateAirplaneMode(true);
    }

    boolean hasCorrectMobileControllers(List<SubscriptionInfo> allSubscriptions) {
        if (allSubscriptions.size() != this.mMobileSignalControllers.size()) {
            Log.d("NetworkController", "size not equals, reset subInfo");
            return false;
        }
        for (SubscriptionInfo info : allSubscriptions) {
            MobileSignalController msc = (MobileSignalController) this.mMobileSignalControllers.get(Integer.valueOf(info.getSubscriptionId()));
            if (msc != null) {
                if (msc.mSubscriptionInfo.getSimSlotIndex() != info.getSimSlotIndex()) {
                }
            }
            Log.d("NetworkController", "info_subId = " + info.getSubscriptionId() + " info_slotId = " + info.getSimSlotIndex());
            return false;
        }
        return true;
    }

    private void updateAirplaneMode(boolean force) {
        updateAirplaneMode(Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1, force);
    }

    private void updateAirplaneMode(boolean airplaneMode, boolean force) {
        if (airplaneMode != this.mAirplaneMode || force) {
            this.mAirplaneMode = airplaneMode;
            for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
                mobileSignalController.setAirplaneMode(this.mAirplaneMode);
            }
            notifyListeners();
        }
    }

    private void refreshLocale() {
        Locale current = this.mContext.getResources().getConfiguration().locale;
        if (!current.equals(this.mLocale)) {
            this.mLocale = current;
            notifyAllListeners();
        }
    }

    private void notifyAllListeners() {
        notifyListeners();
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
        this.mWifiSignalController.notifyListeners();
        this.mEthernetSignalController.notifyListeners();
    }

    private void notifyListeners() {
        this.mCallbackHandler.setIsAirplaneMode(new IconState(this.mAirplaneMode, R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode, this.mContext));
        this.mCallbackHandler.setNoSims(this.mHasNoSims);
    }

    private void updateConnectivity() {
        boolean z = false;
        this.mConnectedTransports.clear();
        this.mValidatedTransports.clear();
        for (NetworkCapabilities nc : this.mConnectivityManager.getDefaultNetworkCapabilitiesForUser(this.mCurrentUserId)) {
            for (int transportType : nc.getTransportTypes()) {
                this.mConnectedTransports.set(transportType);
                if (nc.hasCapability(16)) {
                    this.mValidatedTransports.set(transportType);
                }
            }
        }
        if (CHATTY) {
            Log.d("NetworkController", "updateConnectivity: mConnectedTransports=" + this.mConnectedTransports);
            Log.d("NetworkController", "updateConnectivity: mValidatedTransports=" + this.mValidatedTransports);
        }
        if (!this.mValidatedTransports.isEmpty()) {
            z = true;
        }
        this.mInetCondition = z;
        pushConnectivityToSignals();
    }

    private void pushConnectivityToSignals() {
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        }
        this.mWifiSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
        this.mEthernetSignalController.updateConnectivity(this.mConnectedTransports, this.mValidatedTransports);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");
        pw.println("  - telephony ------");
        pw.print("  hasVoiceCallingFeature()=");
        pw.println(hasVoiceCallingFeature());
        pw.println("  - connectivity ------");
        pw.print("  mConnectedTransports=");
        pw.println(this.mConnectedTransports);
        pw.print("  mValidatedTransports=");
        pw.println(this.mValidatedTransports);
        pw.print("  mInetCondition=");
        pw.println(this.mInetCondition);
        pw.print("  mAirplaneMode=");
        pw.println(this.mAirplaneMode);
        pw.print("  mLocale=");
        pw.println(this.mLocale);
        pw.print("  mLastServiceState=");
        pw.println(this.mLastServiceState);
        pw.print("  mIsEmergency=");
        pw.println(this.mIsEmergency);
        pw.print("  mEmergencySource=");
        pw.println(emergencyToString(this.mEmergencySource));
        for (MobileSignalController mobileSignalController : this.mMobileSignalControllers.values()) {
            mobileSignalController.dump(pw);
        }
        this.mWifiSignalController.dump(pw);
        this.mEthernetSignalController.dump(pw);
        this.mAccessPoints.dump(pw);
    }

    private static final String emergencyToString(int emergencySource) {
        if (emergencySource > 300) {
            return "NO_SUB(" + (emergencySource - 300) + ")";
        }
        if (emergencySource > 200) {
            return "VOICE_CONTROLLER(" + (emergencySource - 200) + ")";
        }
        if (emergencySource > 100) {
            return "FIRST_CONTROLLER(" + (emergencySource - 100) + ")";
        }
        if (emergencySource == 0) {
            return "NO_CONTROLLERS";
        }
        return "UNKNOWN_SOURCE";
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            if (DEBUG) {
                Log.d("NetworkController", "Entering demo mode");
            }
            unregisterListeners();
            this.mDemoMode = true;
            this.mDemoInetCondition = this.mInetCondition;
            this.mDemoWifiState = (WifiState) this.mWifiSignalController.getState();
        } else if (this.mDemoMode && command.equals("exit")) {
            if (DEBUG) {
                Log.d("NetworkController", "Exiting demo mode");
            }
            this.mDemoMode = false;
            updateMobileControllers();
            for (MobileSignalController controller : this.mMobileSignalControllers.values()) {
                controller.resetLastState();
            }
            this.mWifiSignalController.resetLastState();
            this.mReceiverHandler.post(this.mRegisterListeners);
            notifyAllListeners();
        } else if (this.mDemoMode && command.equals("network")) {
            boolean show;
            String level;
            List<SubscriptionInfo> subs;
            String airplane = args.getString("airplane");
            if (airplane != null) {
                this.mCallbackHandler.setIsAirplaneMode(new IconState(airplane.equals("show"), R.drawable.stat_sys_airplane_mode, R.string.accessibility_airplane_mode, this.mContext));
            }
            String fully = args.getString("fully");
            if (fully != null) {
                this.mDemoInetCondition = Boolean.parseBoolean(fully);
                BitSet connected = new BitSet();
                if (this.mDemoInetCondition) {
                    connected.set(this.mWifiSignalController.mTransportType);
                }
                this.mWifiSignalController.updateConnectivity(connected, connected);
                for (MobileSignalController controller2 : this.mMobileSignalControllers.values()) {
                    if (this.mDemoInetCondition) {
                        connected.set(controller2.mTransportType);
                    }
                    controller2.updateConnectivity(connected, connected);
                }
            }
            String wifi = args.getString("wifi");
            if (wifi != null) {
                show = wifi.equals("show");
                level = args.getString("level");
                if (level != null) {
                    int i;
                    WifiState wifiState = this.mDemoWifiState;
                    if (level.equals("null")) {
                        i = -1;
                    } else {
                        i = Math.min(Integer.parseInt(level), WifiIcons.WIFI_LEVEL_COUNT - 1);
                    }
                    wifiState.level = i;
                    this.mDemoWifiState.connected = this.mDemoWifiState.level >= 0;
                }
                this.mDemoWifiState.enabled = show;
                this.mWifiSignalController.notifyListeners();
            }
            String sims = args.getString("sims");
            if (sims != null) {
                int num = MathUtils.constrain(Integer.parseInt(sims), 1, 8);
                subs = new ArrayList();
                if (num != this.mMobileSignalControllers.size()) {
                    this.mMobileSignalControllers.clear();
                    int start = this.mSubscriptionManager.getActiveSubscriptionInfoCountMax();
                    for (int i2 = start; i2 < start + num; i2++) {
                        subs.add(addSignalController(i2, i2));
                    }
                    this.mCallbackHandler.setSubs(subs);
                }
            }
            String nosim = args.getString("nosim");
            if (nosim != null) {
                this.mHasNoSims = nosim.equals("show");
                this.mCallbackHandler.setNoSims(this.mHasNoSims);
            }
            String mobile = args.getString("mobile");
            if (mobile != null) {
                MobileState mobileState;
                show = mobile.equals("show");
                String datatype = args.getString("datatype");
                String slotString = args.getString("slot");
                int slot = MathUtils.constrain(TextUtils.isEmpty(slotString) ? 0 : Integer.parseInt(slotString), 0, 8);
                subs = new ArrayList();
                while (this.mMobileSignalControllers.size() <= slot) {
                    int nextSlot = this.mMobileSignalControllers.size();
                    subs.add(addSignalController(nextSlot, nextSlot));
                }
                if (!subs.isEmpty()) {
                    this.mCallbackHandler.setSubs(subs);
                }
                controller2 = ((MobileSignalController[]) this.mMobileSignalControllers.values().toArray(new MobileSignalController[0]))[slot];
                ((MobileState) controller2.getState()).dataSim = datatype != null;
                if (datatype != null) {
                    IconGroup iconGroup;
                    mobileState = (MobileState) controller2.getState();
                    if (datatype.equals("1x")) {
                        iconGroup = TelephonyIcons.ONE_X;
                    } else if (datatype.equals("3g")) {
                        iconGroup = TelephonyIcons.THREE_G;
                    } else if (datatype.equals("4g")) {
                        iconGroup = TelephonyIcons.FOUR_G;
                    } else if (datatype.equals("e")) {
                        iconGroup = TelephonyIcons.E;
                    } else if (datatype.equals("g")) {
                        iconGroup = TelephonyIcons.G;
                    } else if (datatype.equals("h")) {
                        iconGroup = TelephonyIcons.H;
                    } else if (datatype.equals("lte")) {
                        iconGroup = TelephonyIcons.LTE;
                    } else if (datatype.equals("roam")) {
                        iconGroup = TelephonyIcons.ROAMING;
                    } else {
                        iconGroup = TelephonyIcons.UNKNOWN;
                    }
                    mobileState.iconGroup = iconGroup;
                }
                int[][] icons = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH;
                level = args.getString("level");
                if (level != null) {
                    int i3;
                    mobileState = (MobileState) controller2.getState();
                    if (level.equals("null")) {
                        i3 = -1;
                    } else {
                        i3 = Math.min(Integer.parseInt(level), icons[0].length - 1);
                    }
                    mobileState.level = i3;
                    ((MobileState) controller2.getState()).connected = ((MobileState) controller2.getState()).level >= 0;
                }
                ((MobileState) controller2.getState()).enabled = show;
                controller2.notifyListeners();
            }
            String carrierNetworkChange = args.getString("carriernetworkchange");
            if (carrierNetworkChange != null) {
                show = carrierNetworkChange.equals("show");
                for (MobileSignalController controller22 : this.mMobileSignalControllers.values()) {
                    controller22.setCarrierNetworkChangeMode(show);
                }
            }
        }
    }

    private SubscriptionInfo addSignalController(int id, int simSlotIndex) {
        SubscriptionInfo info = new SubscriptionInfo(id, "", simSlotIndex, "", "", 0, 0, "", 0, null, 0, 0, "");
        this.mMobileSignalControllers.put(Integer.valueOf(id), new MobileSignalController(this.mContext, this.mConfig, this.mHasMobileDataFeature, this.mPhone, this.mCallbackHandler, this, info, this.mSubDefaults, this.mReceiverHandler.getLooper()));
        return info;
    }

    public void showDefaultAccountStatus(DefaultAccountStatus status) {
        this.mCallbackHandler.setDefaultAccountStatus(status);
    }

    private void handleIMSAction(Intent intent) {
        this.mImsRegState = intent.getIntExtra("android:regState", 1);
        int phoneId = intent.getIntExtra("android:phone_id", -1);
        this.mImsSubId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        Log.d("NetworkController", "handleIMSAction mImsRegState = " + this.mImsRegState + " phoneId = " + phoneId + "mImsSubId = " + this.mImsSubId);
        int iconId = 0;
        for (Integer subId : this.mMobileSignalControllers.keySet()) {
            MobileSignalController signalController = (MobileSignalController) this.mMobileSignalControllers.get(subId);
            if (subId.intValue() == this.mImsSubId) {
                if (isImsOverWfc(intent)) {
                    iconId = 0;
                    this.mIsImsOverWfc = true;
                    Log.d("NetworkController", "WFC reset ims register state and remove volte icon");
                    if (SystemProperties.get("persist.radio.multisim.config", "ss").equals("dsds")) {
                        iconId = getWfcIconId(phoneId);
                    }
                    Log.d("NetworkController", "Set IMS regState with iconId = " + iconId);
                } else {
                    iconId = (this.mImsRegState == 0 && signalController.isLteNetWork()) ? getVolteIconId(phoneId) : 0;
                    this.mIsImsOverWfc = false;
                    Log.d("NetworkController", "Set IMS regState with iconId = " + iconId);
                }
            }
        }
        this.mCallbackHandler.setVolteStatusIcon(iconId);
    }

    public int getImsSubId() {
        return this.mImsSubId;
    }

    public int getImsRegState() {
        return this.mImsRegState;
    }

    public boolean isImsRegOverWfc() {
        return this.mIsImsOverWfc;
    }

    public int getWfcIconId(int slotId) {
        if (((TelephonyManager) this.mContext.getSystemService("phone")).getSimCount() <= 1 || slotId >= NetworkTypeUtils.WFCICON.length) {
            return R.drawable.stat_sys_wfc;
        }
        return NetworkTypeUtils.WFCICON[slotId];
    }

    public int getVolteIconId(int slotId) {
        if (((TelephonyManager) this.mContext.getSystemService("phone")).getSimCount() <= 1 || slotId >= NetworkTypeUtils.VOLTEICON.length) {
            return R.drawable.stat_sys_volte;
        }
        return NetworkTypeUtils.VOLTEICON[slotId];
    }

    private boolean isImsOverWfc(Intent intent) {
        boolean[] enabledFeatures = intent.getBooleanArrayExtra("android:enablecap");
        boolean wfcCapabilities = false;
        if (enabledFeatures != null && enabledFeatures.length > 1) {
            wfcCapabilities = enabledFeatures[2];
        }
        Log.d("NetworkController", "wfcCapabilities = " + wfcCapabilities);
        return wfcCapabilities;
    }

    public void refreshPlmnCarrierLabel() {
        for (int i = 0; i < this.mSlotCount; i++) {
            boolean found = false;
            for (Entry<Integer, MobileSignalController> entry : this.mMobileSignalControllers.entrySet()) {
                int subId = ((Integer) entry.getKey()).intValue();
                int slotId = -1;
                MobileSignalController controller = (MobileSignalController) entry.getValue();
                if (controller.getControllerSubInfo() != null) {
                    slotId = controller.getControllerSubInfo().getSimSlotIndex();
                    continue;
                }
                if (i == slotId) {
                    this.mNetworkName[slotId] = ((MobileState) controller.mCurrentState).networkName;
                    PluginFactory.getStatusBarPlmnPlugin(this.mContext).updateCarrierLabel(i, true, controller.getControllserHasService(), this.mNetworkName);
                    found = true;
                    break;
                }
            }
            if (!found) {
                this.mNetworkName[i] = this.mContext.getString(17039971);
                PluginFactory.getStatusBarPlmnPlugin(this.mContext).updateCarrierLabel(i, false, false, this.mNetworkName);
            }
        }
    }

    private final MobileSignalController getMobileSignalController(int subId) {
        if (this.mMobileSignalControllers.containsKey(Integer.valueOf(subId))) {
            return (MobileSignalController) this.mMobileSignalControllers.get(Integer.valueOf(subId));
        }
        Log.e("NetworkController", "Cannot find controller for sub: " + subId);
        return null;
    }

    public final NetworkControllerExt getNetworkControllerExt() {
        return this.mNetworkControllerExt;
    }
}
