package com.android.systemui.statusbar.policy;

import android.os.Handler;
import android.os.Message;
import android.telephony.SubscriptionInfo;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.EmergencyListener;
import com.mediatek.systemui.statusbar.defaultaccount.DefaultAccountStatus;
import java.util.ArrayList;
import java.util.List;

public class CallbackHandler extends Handler implements EmergencyListener, SignalCallback {
    private final ArrayList<EmergencyListener> mEmergencyListeners = new ArrayList();
    private final ArrayList<SignalCallback> mSignalCallbacks = new ArrayList();

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                for (EmergencyListener listener : this.mEmergencyListeners) {
                    listener.setEmergencyCallsOnly(msg.arg1 != 0);
                }
                return;
            case 1:
                for (SignalCallback signalCluster : this.mSignalCallbacks) {
                    signalCluster.setSubs((List) msg.obj);
                }
                return;
            case 2:
                for (SignalCallback signalCluster2 : this.mSignalCallbacks) {
                    signalCluster2.setNoSims(msg.arg1 != 0);
                }
                return;
            case 3:
                for (SignalCallback signalCluster22 : this.mSignalCallbacks) {
                    signalCluster22.setEthernetIndicators((IconState) msg.obj);
                }
                return;
            case 4:
                for (SignalCallback signalCluster222 : this.mSignalCallbacks) {
                    signalCluster222.setIsAirplaneMode((IconState) msg.obj);
                }
                return;
            case 5:
                for (SignalCallback signalCluster2222 : this.mSignalCallbacks) {
                    signalCluster2222.setMobileDataEnabled(msg.arg1 != 0);
                }
                return;
            case 6:
                if (msg.arg1 != 0) {
                    this.mEmergencyListeners.add((EmergencyListener) msg.obj);
                    return;
                } else {
                    this.mEmergencyListeners.remove((EmergencyListener) msg.obj);
                    return;
                }
            case 7:
                if (msg.arg1 != 0) {
                    this.mSignalCallbacks.add((SignalCallback) msg.obj);
                    return;
                } else {
                    this.mSignalCallbacks.remove((SignalCallback) msg.obj);
                    return;
                }
            default:
                return;
        }
    }

    public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
        final boolean z = enabled;
        final IconState iconState = statusIcon;
        final IconState iconState2 = qsIcon;
        final boolean z2 = activityIn;
        final boolean z3 = activityOut;
        final String str = description;
        post(new Runnable() {
            public void run() {
                for (SignalCallback callback : CallbackHandler.this.mSignalCallbacks) {
                    callback.setWifiIndicators(z, iconState, iconState2, z2, z3, str);
                }
            }
        });
    }

    public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon, int statusType, int networkIcon, int qsType, boolean activityIn, boolean activityOut, int dataActivity, int primarySimIcon, String typeContentDescription, String description, boolean isWide, int subId) {
        final IconState iconState = statusIcon;
        final IconState iconState2 = qsIcon;
        final int i = statusType;
        final int i2 = networkIcon;
        final int i3 = qsType;
        final boolean z = activityIn;
        final boolean z2 = activityOut;
        final int i4 = dataActivity;
        final int i5 = primarySimIcon;
        final String str = typeContentDescription;
        final String str2 = description;
        final boolean z3 = isWide;
        final int i6 = subId;
        post(new Runnable() {
            public void run() {
                for (SignalCallback signalCluster : CallbackHandler.this.mSignalCallbacks) {
                    signalCluster.setMobileDataIndicators(iconState, iconState2, i, i2, i3, z, z2, i4, i5, str, str2, z3, i6);
                }
            }
        });
    }

    public void setSubs(List<SubscriptionInfo> subs) {
        obtainMessage(1, subs).sendToTarget();
    }

    public void setNoSims(boolean show) {
        int i;
        if (show) {
            i = 1;
        } else {
            i = 0;
        }
        obtainMessage(2, i, 0).sendToTarget();
    }

    public void setMobileDataEnabled(boolean enabled) {
        int i;
        if (enabled) {
            i = 1;
        } else {
            i = 0;
        }
        obtainMessage(5, i, 0).sendToTarget();
    }

    public void setEmergencyCallsOnly(boolean emergencyOnly) {
        int i;
        if (emergencyOnly) {
            i = 1;
        } else {
            i = 0;
        }
        obtainMessage(0, i, 0).sendToTarget();
    }

    public void setEthernetIndicators(IconState icon) {
        obtainMessage(3, icon).sendToTarget();
    }

    public void setIsAirplaneMode(IconState icon) {
        obtainMessage(4, icon).sendToTarget();
    }

    public void setListening(EmergencyListener listener, boolean listening) {
        int i;
        if (listening) {
            i = 1;
        } else {
            i = 0;
        }
        obtainMessage(6, i, 0, listener).sendToTarget();
    }

    public void setListening(SignalCallback listener, boolean listening) {
        int i;
        if (listening) {
            i = 1;
        } else {
            i = 0;
        }
        obtainMessage(7, i, 0, listener).sendToTarget();
    }

    public void setDefaultAccountStatus(DefaultAccountStatus status) {
        for (SignalCallback signalCluster : this.mSignalCallbacks) {
            signalCluster.setDefaultAccountStatus(status);
        }
    }

    public void setVolteStatusIcon(final int iconId) {
        post(new Runnable() {
            public void run() {
                for (SignalCallback signalCluster : CallbackHandler.this.mSignalCallbacks) {
                    signalCluster.setVolteStatusIcon(iconId);
                }
            }
        });
    }
}
