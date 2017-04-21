package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IConnectivityManager;
import android.net.IConnectivityManager.Stub;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SecurityControllerImpl implements SecurityController {
    private static final boolean DEBUG = Log.isLoggable("SecurityController", 3);
    private static final NetworkRequest REQUEST = new Builder().removeCapability(15).removeCapability(13).removeCapability(14).build();
    private final ArrayList<SecurityControllerCallback> mCallbacks = new ArrayList();
    private final ConnectivityManager mConnectivityManager;
    private final IConnectivityManager mConnectivityManagerService;
    private final Context mContext;
    private int mCurrentUserId;
    private SparseArray<VpnConfig> mCurrentVpns = new SparseArray();
    private final DevicePolicyManager mDevicePolicyManager;
    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        public void onAvailable(Network network) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onAvailable " + network.netId);
            }
            SecurityControllerImpl.this.updateState();
            SecurityControllerImpl.this.fireCallbacks();
        }

        public void onLost(Network network) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onLost " + network.netId);
            }
            SecurityControllerImpl.this.updateState();
            SecurityControllerImpl.this.fireCallbacks();
        }
    };
    private final UserManager mUserManager;
    private int mVpnUserId;

    public SecurityControllerImpl(Context context) {
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mConnectivityManagerService = Stub.asInterface(ServiceManager.getService("connectivity"));
        this.mUserManager = (UserManager) context.getSystemService(FeatureOptionUtils.BUILD_TYPE_USER);
        this.mConnectivityManager.registerNetworkCallback(REQUEST, this.mNetworkCallback);
        onUserSwitched(ActivityManager.getCurrentUser());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SecurityController state:");
        pw.print("  mCurrentVpns={");
        for (int i = 0; i < this.mCurrentVpns.size(); i++) {
            if (i > 0) {
                pw.print(", ");
            }
            pw.print(this.mCurrentVpns.keyAt(i));
            pw.print('=');
            pw.print(((VpnConfig) this.mCurrentVpns.valueAt(i)).user);
        }
        pw.println("}");
    }

    public boolean hasDeviceOwner() {
        return !TextUtils.isEmpty(this.mDevicePolicyManager.getDeviceOwner());
    }

    public String getDeviceOwnerName() {
        return this.mDevicePolicyManager.getDeviceOwnerName();
    }

    public boolean hasProfileOwner() {
        return this.mDevicePolicyManager.getProfileOwnerAsUser(this.mCurrentUserId) != null;
    }

    public String getProfileOwnerName() {
        for (UserInfo profile : this.mUserManager.getProfiles(this.mCurrentUserId)) {
            String name = this.mDevicePolicyManager.getProfileOwnerNameAsUser(profile.id);
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    public String getPrimaryVpnName() {
        VpnConfig cfg = (VpnConfig) this.mCurrentVpns.get(this.mVpnUserId);
        if (cfg != null) {
            return getNameForVpnConfig(cfg, new UserHandle(this.mVpnUserId));
        }
        return null;
    }

    public String getProfileVpnName() {
        for (UserInfo profile : this.mUserManager.getProfiles(this.mVpnUserId)) {
            if (profile.id != this.mVpnUserId) {
                VpnConfig cfg = (VpnConfig) this.mCurrentVpns.get(profile.id);
                if (cfg != null) {
                    return getNameForVpnConfig(cfg, profile.getUserHandle());
                }
            }
        }
        return null;
    }

    public boolean isVpnEnabled() {
        for (UserInfo profile : this.mUserManager.getProfiles(this.mVpnUserId)) {
            if (this.mCurrentVpns.get(profile.id) != null) {
                return true;
            }
        }
        return false;
    }

    public void removeCallback(SecurityControllerCallback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("SecurityController", "removeCallback " + callback);
            }
            this.mCallbacks.remove(callback);
        }
    }

    public void addCallback(SecurityControllerCallback callback) {
        if (callback != null && !this.mCallbacks.contains(callback)) {
            if (DEBUG) {
                Log.d("SecurityController", "addCallback " + callback);
            }
            this.mCallbacks.add(callback);
        }
    }

    public void onUserSwitched(int newUserId) {
        this.mCurrentUserId = newUserId;
        if (this.mUserManager.getUserInfo(newUserId).isRestricted()) {
            this.mVpnUserId = 0;
        } else {
            this.mVpnUserId = this.mCurrentUserId;
        }
        fireCallbacks();
    }

    private String getNameForVpnConfig(VpnConfig cfg, UserHandle user) {
        if (cfg.legacy) {
            return this.mContext.getString(R.string.legacy_vpn_name);
        }
        String vpnPackage = cfg.user;
        try {
            return VpnConfig.getVpnLabel(this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user), vpnPackage).toString();
        } catch (NameNotFoundException nnfe) {
            Log.e("SecurityController", "Package " + vpnPackage + " is not present", nnfe);
            return null;
        }
    }

    private void fireCallbacks() {
        for (SecurityControllerCallback callback : this.mCallbacks) {
            callback.onStateChanged();
        }
    }

    private void updateState() {
        SparseArray<VpnConfig> vpns = new SparseArray();
        try {
            for (UserInfo user : this.mUserManager.getUsers()) {
                VpnConfig cfg = this.mConnectivityManagerService.getVpnConfig(user.id);
                if (cfg != null) {
                    if (cfg.legacy) {
                        LegacyVpnInfo legacyVpn = this.mConnectivityManagerService.getLegacyVpnInfo(user.id);
                        if (legacyVpn != null) {
                            if (legacyVpn.state != 3) {
                            }
                        }
                    }
                    vpns.put(user.id, cfg);
                }
            }
            this.mCurrentVpns = vpns;
        } catch (RemoteException rme) {
            Log.e("SecurityController", "Unable to list active VPNs", rme);
        }
    }
}
