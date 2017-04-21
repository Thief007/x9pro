package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.IUserSwitchObserver.Stub;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.hardware.display.WifiDisplayStatus;
import android.media.AudioManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.systemui.R;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BluetoothController.Callback;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.CastController.CastDevice;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;

public class PhoneStatusBarPolicy implements Callback {
    private static final boolean DEBUG = Log.isLoggable("PhoneStatusBarPolicy", 3);
    private BroadcastReceiver mAlarmIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("PhoneStatusBarPolicy", "onReceive:" + action);
            if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                PhoneStatusBarPolicy.this.updateAlarm();
            }
        }
    };
    private final AlarmManager mAlarmManager;
    private BluetoothController mBluetooth;
    private final CastController mCast;
    private final CastController.Callback mCastCallback = new CastController.Callback() {
        public void onCastDevicesChanged() {
            PhoneStatusBarPolicy.this.updateCast();
        }

        public void onWfdStatusChanged(WifiDisplayStatus status, boolean sinkMode) {
        }

        public void onWifiP2pDeviceChanged(WifiP2pDevice device) {
        }
    };
    private final Context mContext;
    private boolean mCurrentUserSetup;
    private final Handler mHandler = new Handler();
    private final HotspotController mHotspot;
    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        public void onHotspotChanged(boolean enabled) {
            PhoneStatusBarPolicy.this.mService.setIconVisibility("hotspot", enabled);
        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("PhoneStatusBarPolicy", "action = " + action);
            if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")) {
                PhoneStatusBarPolicy.this.updateAlarm();
            } else if (action.equals("android.media.RINGER_MODE_CHANGED") || action.equals("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")) {
                PhoneStatusBarPolicy.this.updateVolumeZen();
            } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                PhoneStatusBarPolicy.this.updateSimState(intent);
            } else if (action.equals("android.telecom.action.CURRENT_TTY_MODE_CHANGED")) {
                PhoneStatusBarPolicy.this.updateTTY(intent);
            } else if (action.equals("android.intent.action.HEADSET_PLUG")) {
                PhoneStatusBarPolicy.this.updateHeadset(intent);
            } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                PhoneStatusBarPolicy.this.updateAlarm();
                PhoneStatusBarPolicy.this.registerAlarmClockChanged(intent.getIntExtra("android.intent.extra.user_handle", -1), true);
            }
        }
    };
    private boolean mIsPluginWithMic;
    private boolean mIsPluginWithoutMic;
    private boolean mKeyguardVisible = true;
    private boolean mManagedProfileFocused = false;
    private boolean mManagedProfileIconVisible = true;
    private Runnable mRemoveCastIconRunnable = new Runnable() {
        public void run() {
            if (PhoneStatusBarPolicy.DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon NOW");
            }
            PhoneStatusBarPolicy.this.mService.setIconVisibility("cast", false);
        }
    };
    private final StatusBarManager mService;
    State mSimState = State.READY;
    private final UserInfoController mUserInfoController;
    private final Stub mUserSwitchListener = new Stub() {
        public void onUserSwitching(int newUserId, IRemoteCallback reply) {
            PhoneStatusBarPolicy.this.mUserInfoController.reloadUserInfo();
        }

        public void onUserSwitchComplete(int newUserId) throws RemoteException {
            PhoneStatusBarPolicy.this.updateAlarm();
            PhoneStatusBarPolicy.this.profileChanged(newUserId);
        }

        public void onForegroundProfileSwitch(int newProfileId) {
            PhoneStatusBarPolicy.this.profileChanged(newProfileId);
        }
    };
    private boolean mVolumeVisible;
    private int mZen;
    private boolean mZenVisible;

    public PhoneStatusBarPolicy(Context context, CastController cast, HotspotController hotspot, UserInfoController userInfoController, BluetoothController bluetooth) {
        this.mContext = context;
        this.mCast = cast;
        this.mHotspot = hotspot;
        this.mBluetooth = bluetooth;
        this.mBluetooth.addStateChangedCallback(this);
        this.mService = (StatusBarManager) context.getSystemService("statusbar");
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mUserInfoController = userInfoController;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        registerAlarmClockChanged(0, false);
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(this.mUserSwitchListener);
        } catch (RemoteException e) {
        }
        this.mService.setIcon("tty", R.drawable.stat_sys_tty_mode, 0, null);
        this.mService.setIconVisibility("tty", false);
        updateBluetooth();
        this.mService.setIcon("alarm_clock", R.drawable.stat_sys_alarm, 0, null);
        this.mService.setIconVisibility("alarm_clock", false);
        this.mService.setIcon("zen", R.drawable.stat_sys_zen_important, 0, null);
        this.mService.setIconVisibility("zen", false);
        this.mService.setIcon("volume", R.drawable.stat_sys_ringer_vibrate, 0, null);
        this.mService.setIconVisibility("volume", false);
        updateVolumeZen();
        if (this.mCast != null) {
            this.mService.setIcon("cast", R.drawable.stat_sys_cast, 0, null);
            this.mService.setIconVisibility("cast", false);
            this.mCast.addCallback(this.mCastCallback);
        }
        this.mService.setIcon("hotspot", R.drawable.stat_sys_hotspot, 0, this.mContext.getString(R.string.accessibility_status_bar_hotspot));
        this.mService.setIconVisibility("hotspot", this.mHotspot.isHotspotEnabled());
        this.mHotspot.addCallback(this.mHotspotCallback);
        this.mService.setIcon("managed_profile", R.drawable.stat_sys_managed_profile_status, 0, this.mContext.getString(R.string.accessibility_managed_profile));
        this.mService.setIconVisibility("managed_profile", false);
    }

    public void setZenMode(int zen) {
        this.mZen = zen;
        updateVolumeZen();
    }

    private void updateAlarm() {
        int i;
        AlarmClockInfo alarm = this.mAlarmManager.getNextAlarmClock(-2);
        boolean hasAlarm = alarm != null && alarm.getTriggerTime() > 0;
        boolean zenNone = this.mZen == 2;
        StatusBarManager statusBarManager = this.mService;
        String str = "alarm_clock";
        if (zenNone) {
            i = R.drawable.stat_sys_alarm_dim;
        } else {
            i = R.drawable.stat_sys_alarm;
        }
        statusBarManager.setIcon(str, i, 0, null);
        StatusBarManager statusBarManager2 = this.mService;
        String str2 = "alarm_clock";
        if (!this.mCurrentUserSetup) {
            hasAlarm = false;
        }
        statusBarManager2.setIconVisibility(str2, hasAlarm);
    }

    private final void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stateExtra)) {
            this.mSimState = State.ABSENT;
        } else if ("CARD_IO_ERROR".equals(stateExtra)) {
            this.mSimState = State.CARD_IO_ERROR;
        } else if ("READY".equals(stateExtra)) {
            this.mSimState = State.READY;
        } else if ("LOCKED".equals(stateExtra)) {
            String lockedReason = intent.getStringExtra("reason");
            if ("PIN".equals(lockedReason)) {
                this.mSimState = State.PIN_REQUIRED;
            } else if ("PUK".equals(lockedReason)) {
                this.mSimState = State.PUK_REQUIRED;
            } else {
                this.mSimState = State.NETWORK_LOCKED;
            }
        } else {
            this.mSimState = State.UNKNOWN;
        }
    }

    private final void updateVolumeZen() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        boolean zenVisible = false;
        int zenIconId = 0;
        String zenDescription = null;
        boolean volumeVisible = false;
        int volumeIconId = 0;
        String volumeDescription = null;
        if (DndTile.isVisible(this.mContext) || DndTile.isCombinedIcon(this.mContext)) {
            zenVisible = this.mZen != 0;
            zenIconId = this.mZen == 2 ? R.drawable.stat_sys_dnd_total_silence : R.drawable.stat_sys_dnd;
            zenDescription = this.mContext.getString(R.string.quick_settings_dnd_label);
        } else if (this.mZen == 2) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_zen_none;
            zenDescription = this.mContext.getString(R.string.interruption_level_none);
        } else if (this.mZen == 1) {
            zenVisible = true;
            zenIconId = R.drawable.stat_sys_zen_important;
            zenDescription = this.mContext.getString(R.string.interruption_level_priority);
        }
        if (DndTile.isVisible(this.mContext) && !DndTile.isCombinedIcon(this.mContext) && audioManager.getRingerModeInternal() == 0) {
            volumeVisible = true;
            volumeIconId = R.drawable.stat_sys_ringer_silent;
            volumeDescription = this.mContext.getString(R.string.accessibility_ringer_silent);
        } else if (!(this.mZen == 2 || this.mZen == 3 || audioManager.getRingerModeInternal() != 1)) {
            volumeVisible = true;
            volumeIconId = R.drawable.stat_sys_ringer_vibrate;
            volumeDescription = this.mContext.getString(R.string.accessibility_ringer_vibrate);
        }
        if (zenVisible) {
            this.mService.setIcon("zen", zenIconId, 0, zenDescription);
        }
        if (zenVisible != this.mZenVisible) {
            this.mService.setIconVisibility("zen", zenVisible);
            this.mZenVisible = zenVisible;
        }
        if (volumeVisible) {
            this.mService.setIcon("volume", volumeIconId, 0, volumeDescription);
        }
        if (volumeVisible != this.mVolumeVisible) {
            this.mService.setIconVisibility("volume", volumeVisible);
            this.mVolumeVisible = volumeVisible;
        }
        updateAlarm();
    }

    public void onBluetoothDevicesChanged() {
        updateBluetooth();
    }

    public void onBluetoothStateChange(boolean enabled) {
        updateBluetooth();
    }

    private final void updateBluetooth() {
        int iconId = R.drawable.stat_sys_data_bluetooth;
        String contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_on);
        boolean z = false;
        if (this.mBluetooth != null) {
            z = this.mBluetooth.isBluetoothEnabled();
            if (this.mBluetooth.isBluetoothConnected()) {
                iconId = R.drawable.stat_sys_data_bluetooth_connected;
                contentDescription = this.mContext.getString(R.string.accessibility_bluetooth_connected);
            }
        }
        this.mService.setIcon("bluetooth", iconId, 0, contentDescription);
        this.mService.setIconVisibility("bluetooth", z);
    }

    private final void updateTTY(Intent intent) {
        boolean enabled = intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0) != 0;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: enabled: " + enabled);
        }
        if (enabled) {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY on");
            }
            this.mService.setIcon("tty", R.drawable.stat_sys_tty_mode, 0, this.mContext.getString(R.string.accessibility_tty_enabled));
            this.mService.setIconVisibility("tty", true);
            return;
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY off");
        }
        this.mService.setIconVisibility("tty", false);
    }

    private void updateCast() {
        boolean isCasting = false;
        for (CastDevice device : this.mCast.getCastDevices()) {
            if (device.state != 1) {
                if (device.state == 2) {
                }
            }
            isCasting = true;
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateCast: isCasting: " + isCasting);
        }
        this.mHandler.removeCallbacks(this.mRemoveCastIconRunnable);
        if (isCasting) {
            this.mService.setIcon("cast", R.drawable.stat_sys_cast, 0, this.mContext.getString(R.string.accessibility_casting));
            this.mService.setIconVisibility("cast", true);
            return;
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon in 3 sec...");
        }
        this.mHandler.postDelayed(this.mRemoveCastIconRunnable, 3000);
    }

    private void profileChanged(int userId) {
        UserManager userManager = (UserManager) this.mContext.getSystemService(FeatureOptionUtils.BUILD_TYPE_USER);
        UserInfo user = null;
        if (userId == -2) {
            try {
                user = ActivityManagerNative.getDefault().getCurrentUser();
            } catch (RemoteException e) {
            }
        } else {
            user = userManager.getUserInfo(userId);
        }
        this.mManagedProfileFocused = user != null ? user.isManagedProfile() : false;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "profileChanged: mManagedProfileFocused: " + this.mManagedProfileFocused);
        }
    }

    private void updateManagedProfile() {
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateManagedProfile: mManagedProfileFocused: " + this.mManagedProfileFocused + " mKeyguardVisible: " + this.mKeyguardVisible);
        }
        boolean showIcon = this.mManagedProfileFocused && !this.mKeyguardVisible;
        if (this.mManagedProfileIconVisible != showIcon) {
            this.mService.setIconVisibility("managed_profile", showIcon);
            this.mManagedProfileIconVisible = showIcon;
        }
    }

    public void appTransitionStarting(long startTime, long duration) {
        updateManagedProfile();
    }

    public void setKeyguardShowing(boolean visible) {
        this.mKeyguardVisible = visible;
        updateManagedProfile();
    }

    public void setCurrentUserSetup(boolean userSetup) {
        if (this.mCurrentUserSetup != userSetup) {
            this.mCurrentUserSetup = userSetup;
            updateAlarm();
        }
    }

    protected void updateHeadset(Intent intent) {
        int state = intent.getIntExtra("state", -1);
        int mic = intent.getIntExtra("microphone", -1);
        Log.d("PhoneStatusBarPolicy", "updateHeadSet, state = " + state + ", mic = " + mic);
        if (state == 1) {
            int iconId;
            if (mic == 1) {
                this.mIsPluginWithMic = true;
            } else {
                this.mIsPluginWithoutMic = true;
            }
            if (mic == 1) {
                iconId = R.drawable.stat_sys_headset_with_mic;
            } else {
                iconId = R.drawable.stat_sys_headset_without_mic;
            }
            this.mService.setIcon("headset", iconId, 0, null);
            this.mService.setIconVisibility("headset", true);
            Log.d("PhoneStatusBarPolicy", "updateHeadSet mIsPluginWithMic = " + this.mIsPluginWithMic + ", mIsPluginWithoutMic = " + this.mIsPluginWithoutMic);
        } else if (mic == 0 && this.mIsPluginWithMic && this.mIsPluginWithoutMic) {
            this.mIsPluginWithMic = false;
            this.mIsPluginWithoutMic = false;
            Log.d("PhoneStatusBarPolicy", "Reset the flag, and do not hide the icons");
        } else {
            this.mIsPluginWithMic = false;
            this.mIsPluginWithoutMic = false;
            this.mService.setIconVisibility("headset", false);
        }
    }

    private void registerAlarmClockChanged(int newUserId, boolean userSwitch) {
        if (userSwitch) {
            this.mContext.unregisterReceiver(this.mAlarmIntentReceiver);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        Log.d("PhoneStatusBarPolicy", "registerAlarmClockChanged:" + newUserId);
        this.mContext.registerReceiverAsUser(this.mAlarmIntentReceiver, new UserHandle(newUserId), filter, null, this.mHandler);
    }
}
