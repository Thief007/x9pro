package com.android.systemui.statusbar.policy;

public interface SecurityController {

    public interface SecurityControllerCallback {
        void onStateChanged();
    }

    void addCallback(SecurityControllerCallback securityControllerCallback);

    String getDeviceOwnerName();

    String getPrimaryVpnName();

    String getProfileOwnerName();

    String getProfileVpnName();

    boolean hasDeviceOwner();

    boolean hasProfileOwner();

    boolean isVpnEnabled();

    void removeCallback(SecurityControllerCallback securityControllerCallback);
}
