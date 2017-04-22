package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.SystemProperties;
import android.os.UserManager;
import com.android.setupwizardlib.R$styleable;

public class UsbBackend {
    private static boolean sBicrSupport = "yes".equals(SystemProperties.get("ro.sys.usb.bicr", "no"));
    private static boolean sUmsSupport = SystemProperties.get("ro.sys.usb.storage.type", "mtp").equals("mtp,mass_storage");
    private boolean mIsUnlocked;
    private UsbPort mPort;
    private UsbPortStatus mPortStatus;
    private final boolean mRestricted = this.mUserManager.hasUserRestriction("no_usb_file_transfer");
    private UsbManager mUsbManager;
    private UserManager mUserManager;

    public UsbBackend(Context context) {
        this.mIsUnlocked = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE")).getBooleanExtra("unlocked", false);
        this.mUserManager = UserManager.get(context);
        this.mUsbManager = (UsbManager) context.getSystemService(UsbManager.class);
        UsbPort[] ports = this.mUsbManager.getPorts();
        int N = ports.length;
        for (int i = 0; i < N; i++) {
            UsbPortStatus status = this.mUsbManager.getPortStatus(ports[i]);
            if (status.isConnected()) {
                this.mPort = ports[i];
                this.mPortStatus = status;
                return;
            }
        }
    }

    public int getCurrentMode() {
        int i = 0;
        if (this.mPort == null) {
            return getUsbDataMode() | 0;
        }
        int power = this.mPortStatus.getCurrentPowerRole() == 1 ? 1 : 0;
        if (this.mPortStatus.getCurrentDataRole() == 2) {
            i = getUsbDataMode();
        }
        return i | power;
    }

    public int getUsbDataMode() {
        if (!this.mIsUnlocked) {
            return 0;
        }
        if (this.mUsbManager.isFunctionEnabled("mtp")) {
            return 2;
        }
        if (this.mUsbManager.isFunctionEnabled("ptp")) {
            return 4;
        }
        if (this.mUsbManager.isFunctionEnabled("midi")) {
            return 6;
        }
        if (this.mUsbManager.isFunctionEnabled("mass_storage")) {
            return 8;
        }
        if (this.mUsbManager.isFunctionEnabled("bicr")) {
            return 10;
        }
        return 0;
    }

    private void setUsbFunction(int mode) {
        switch (mode) {
            case 2:
                this.mUsbManager.setCurrentFunction("mtp");
                this.mUsbManager.setUsbDataUnlocked(true);
                return;
            case 4:
                this.mUsbManager.setCurrentFunction("ptp");
                this.mUsbManager.setUsbDataUnlocked(true);
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                this.mUsbManager.setCurrentFunction("midi");
                this.mUsbManager.setUsbDataUnlocked(true);
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                this.mUsbManager.setCurrentFunction("mass_storage");
                this.mUsbManager.setUsbDataUnlocked(true);
                return;
            case 10:
                this.mUsbManager.setCurrentFunction("bicr");
                this.mUsbManager.setUsbDataUnlocked(true);
                return;
            default:
                this.mUsbManager.setCurrentFunction(null);
                this.mUsbManager.setUsbDataUnlocked(false);
                return;
        }
    }

    public void setMode(int mode) {
        if (this.mPort != null) {
            int powerRole = modeToPower(mode);
            int dataRole = ((mode & 14) == 0 && this.mPortStatus.isRoleCombinationSupported(powerRole, 1)) ? 1 : 2;
            this.mUsbManager.setPortRoles(this.mPort, powerRole, dataRole);
        }
        setUsbFunction(mode & 14);
    }

    private int modeToPower(int mode) {
        if ((mode & 1) == 1) {
            return 1;
        }
        return 2;
    }

    public boolean isModeSupported(int mode) {
        boolean z = true;
        if (this.mRestricted && (mode & 14) != 0) {
            return false;
        }
        if (this.mPort != null) {
            int power = modeToPower(mode);
            if ((mode & 14) != 0) {
                return this.mPortStatus.isRoleCombinationSupported(power, 2);
            }
            if (!this.mPortStatus.isRoleCombinationSupported(power, 2)) {
                z = this.mPortStatus.isRoleCombinationSupported(power, 1);
            }
            return z;
        }
        boolean added = true;
        switch (mode & 14) {
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                added = sUmsSupport;
                break;
            case 10:
                added = sBicrSupport;
                break;
        }
        if (!added || (mode & 1) == 1) {
            z = false;
        }
        return z;
    }
}
