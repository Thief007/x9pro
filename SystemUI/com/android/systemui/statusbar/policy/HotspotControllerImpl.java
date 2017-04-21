package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.Log;
import com.android.keyguard.R$styleable;
import com.android.settingslib.TetherUtil;
import com.android.systemui.statusbar.policy.HotspotController.Callback;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class HotspotControllerImpl implements HotspotController {
    private static final boolean DEBUG = Log.isLoggable("HotspotController", 3);
    private static final Intent TETHER_SERVICE_INTENT = new Intent().putExtra("extraAddTetherType", 0).putExtra("extraSetAlarm", true).putExtra("extraRunProvision", true).putExtra("extraEnableWifiTether", true).setComponent(TetherUtil.TETHER_SERVICE);
    private final ArrayList<Callback> mCallbacks = new ArrayList();
    private final Context mContext;
    private int mHotspotState;
    private final Receiver mReceiver = new Receiver();

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        private Receiver() {
        }

        public void setListening(boolean listening) {
            if (listening && !this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Registering receiver");
                }
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
                HotspotControllerImpl.this.mContext.registerReceiver(this, filter);
                this.mRegistered = true;
            } else if (!listening && this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Unregistering receiver");
                }
                HotspotControllerImpl.this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }

        public void onReceive(Context context, Intent intent) {
            boolean z;
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onReceive " + intent.getAction());
            }
            HotspotControllerImpl.this.mHotspotState = intent.getIntExtra("wifi_state", 14);
            HotspotControllerImpl hotspotControllerImpl = HotspotControllerImpl.this;
            if (HotspotControllerImpl.this.mHotspotState == 13) {
                z = true;
            } else {
                z = false;
            }
            hotspotControllerImpl.fireCallback(z);
        }
    }

    public HotspotControllerImpl(Context context) {
        this.mContext = context;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HotspotController state:");
        pw.print("  mHotspotEnabled=");
        pw.println(stateToString(this.mHotspotState));
    }

    private static String stateToString(int hotspotState) {
        switch (hotspotState) {
            case 10:
                return "DISABLING";
            case 11:
                return "DISABLED";
            case R$styleable.GlowPadView_feedbackCount /*12*/:
                return "ENABLING";
            case R$styleable.GlowPadView_alwaysTrackFinger /*13*/:
                return "ENABLED";
            case R$styleable.GlowPadView_allowScaling /*14*/:
                return "FAILED";
            default:
                return null;
        }
    }

    public void addCallback(Callback callback) {
        if (callback != null && !this.mCallbacks.contains(callback)) {
            if (DEBUG) {
                Log.d("HotspotController", "addCallback " + callback);
            }
            this.mCallbacks.add(callback);
            this.mReceiver.setListening(!this.mCallbacks.isEmpty());
        }
    }

    public void removeCallback(Callback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("HotspotController", "removeCallback " + callback);
            }
            this.mCallbacks.remove(callback);
            this.mReceiver.setListening(!this.mCallbacks.isEmpty());
        }
    }

    public boolean isHotspotEnabled() {
        return this.mHotspotState == 13;
    }

    public boolean isHotspotSupported() {
        return TetherUtil.isTetheringSupported(this.mContext);
    }

    public void setHotspotEnabled(boolean enabled) {
        if (enabled && TetherUtil.isProvisioningNeeded(this.mContext)) {
            this.mContext.startServiceAsUser(TETHER_SERVICE_INTENT, UserHandle.CURRENT);
        } else {
            TetherUtil.setWifiTethering(enabled, this.mContext);
        }
    }

    private void fireCallback(boolean isEnabled) {
        for (Callback callback : this.mCallbacks) {
            callback.onHotspotChanged(isEnabled);
        }
    }
}
