package com.android.settings.fuelgauge;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.setupwizardlib.R$styleable;
import java.util.ArrayList;
import java.util.HashMap;

public class BatteryEntry {
    private static /* synthetic */ int[] -com_android_internal_os_BatterySipper$DrainTypeSwitchesValues;
    static final ArrayList<BatteryEntry> mRequestQueue = new ArrayList();
    private static NameAndIconLoader mRequestThread;
    static Handler sHandler;
    static final HashMap<String, UidToDetail> sUidCache = new HashMap();
    public final Context context;
    public String defaultPackageName;
    public Drawable icon;
    public int iconId;
    public String name;
    public final BatterySipper sipper;

    private static class NameAndIconLoader extends Thread {
        private boolean mAbort = false;

        public NameAndIconLoader() {
            super("BatteryUsage Icon Loader");
        }

        public void abort() {
            this.mAbort = true;
        }

        public void run() {
            while (true) {
                BatteryEntry be;
                synchronized (BatteryEntry.mRequestQueue) {
                    if (!BatteryEntry.mRequestQueue.isEmpty() && !this.mAbort) {
                        be = (BatteryEntry) BatteryEntry.mRequestQueue.remove(0);
                    }
                }
                be.loadNameAndIcon();
            }
            if (BatteryEntry.sHandler != null) {
                BatteryEntry.sHandler.sendEmptyMessage(2);
            }
            BatteryEntry.mRequestQueue.clear();
        }
    }

    static class UidToDetail {
        Drawable icon;
        String name;
        String packageName;

        UidToDetail() {
        }
    }

    private static /* synthetic */ int[] m7x12ff44d2() {
        if (-com_android_internal_os_BatterySipper$DrainTypeSwitchesValues != null) {
            return -com_android_internal_os_BatterySipper$DrainTypeSwitchesValues;
        }
        int[] iArr = new int[DrainType.values().length];
        try {
            iArr[DrainType.APP.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DrainType.BLUETOOTH.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DrainType.CAMERA.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[DrainType.CELL.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DrainType.FLASHLIGHT.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[DrainType.IDLE.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[DrainType.OVERCOUNTED.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[DrainType.PHONE.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[DrainType.SCREEN.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[DrainType.UNACCOUNTED.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[DrainType.USER.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[DrainType.WIFI.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        -com_android_internal_os_BatterySipper$DrainTypeSwitchesValues = iArr;
        return iArr;
    }

    public static void startRequestQueue() {
        if (sHandler != null) {
            synchronized (mRequestQueue) {
                if (!mRequestQueue.isEmpty()) {
                    if (mRequestThread != null) {
                        mRequestThread.abort();
                    }
                    mRequestThread = new NameAndIconLoader();
                    mRequestThread.setPriority(1);
                    mRequestThread.start();
                    mRequestQueue.notify();
                }
            }
        }
    }

    public static void stopRequestQueue() {
        synchronized (mRequestQueue) {
            if (mRequestThread != null) {
                mRequestThread.abort();
                mRequestThread = null;
                sHandler = null;
            }
        }
    }

    public static void clearUidCache() {
        sUidCache.clear();
    }

    public BatteryEntry(Context context, Handler handler, UserManager um, BatterySipper sipper) {
        sHandler = handler;
        this.context = context;
        this.sipper = sipper;
        switch (m7x12ff44d2()[sipper.drainType.ordinal()]) {
            case 1:
                this.name = sipper.packageWithHighestDrain;
                break;
            case 2:
                this.name = context.getResources().getString(R.string.power_bluetooth);
                this.iconId = R.drawable.ic_settings_bluetooth;
                break;
            case 3:
                this.name = context.getResources().getString(R.string.power_camera);
                this.iconId = R.drawable.ic_settings_camera;
                break;
            case 4:
                this.name = context.getResources().getString(R.string.power_cell);
                this.iconId = R.drawable.ic_settings_cell_standby;
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                this.name = context.getResources().getString(R.string.power_flashlight);
                this.iconId = R.drawable.ic_settings_display;
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                this.name = context.getResources().getString(R.string.power_idle);
                this.iconId = R.drawable.ic_settings_phone_idle;
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                this.name = context.getResources().getString(R.string.power_overcounted);
                this.iconId = R.drawable.ic_power_system;
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                this.name = context.getResources().getString(R.string.power_phone);
                this.iconId = R.drawable.ic_settings_voice_calls;
                break;
            case 9:
                this.name = context.getResources().getString(R.string.power_screen);
                this.iconId = R.drawable.ic_settings_display;
                break;
            case 10:
                this.name = context.getResources().getString(R.string.power_unaccounted);
                this.iconId = R.drawable.ic_power_system;
                break;
            case 11:
                UserInfo info = um.getUserInfo(sipper.userId);
                if (info == null) {
                    this.icon = null;
                    this.name = context.getResources().getString(R.string.running_process_item_removed_user_label);
                    break;
                }
                this.icon = Utils.getUserIcon(context, um, info);
                this.name = Utils.getUserLabel(context, info);
                break;
            case 12:
                this.name = context.getResources().getString(R.string.power_wifi);
                this.iconId = R.drawable.ic_settings_wireless;
                break;
        }
        if (this.iconId > 0) {
            this.icon = context.getDrawable(this.iconId);
        }
        if ((this.name == null || this.iconId == 0) && this.sipper.uidObj != null) {
            getQuickNameIconForUid(this.sipper.uidObj.getUid());
        }
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public String getLabel() {
        return this.name;
    }

    void getQuickNameIconForUid(int uid) {
        String uidString = Integer.toString(uid);
        if (sUidCache.containsKey(uidString)) {
            UidToDetail utd = (UidToDetail) sUidCache.get(uidString);
            this.defaultPackageName = utd.packageName;
            this.name = utd.name;
            this.icon = utd.icon;
            return;
        }
        PackageManager pm = this.context.getPackageManager();
        this.icon = pm.getDefaultActivityIcon();
        if (pm.getPackagesForUid(uid) == null) {
            if (uid == 0) {
                this.name = this.context.getResources().getString(R.string.process_kernel_label);
            } else if ("mediaserver".equals(this.name)) {
                this.name = this.context.getResources().getString(R.string.process_mediaserver_label);
            } else if ("dex2oat".equals(this.name)) {
                this.name = this.context.getResources().getString(R.string.process_dex2oat_label);
            }
            this.iconId = R.drawable.ic_power_system;
            this.icon = this.context.getDrawable(this.iconId);
        }
        if (sHandler != null) {
            synchronized (mRequestQueue) {
                mRequestQueue.add(this);
            }
        }
    }

    public void loadNameAndIcon() {
        if (this.sipper.uidObj != null) {
            PackageManager pm = this.context.getPackageManager();
            int uid = this.sipper.uidObj.getUid();
            this.sipper.mPackages = pm.getPackagesForUid(uid);
            if (this.sipper.mPackages != null) {
                String[] packageLabels = new String[this.sipper.mPackages.length];
                System.arraycopy(this.sipper.mPackages, 0, packageLabels, 0, this.sipper.mPackages.length);
                IPackageManager ipm = AppGlobals.getPackageManager();
                int userId = UserHandle.getUserId(uid);
                for (int i = 0; i < packageLabels.length; i++) {
                    try {
                        ApplicationInfo ai = ipm.getApplicationInfo(packageLabels[i], 0, userId);
                        if (ai == null) {
                            Log.d("PowerUsageSummary", "Retrieving null app info for package " + packageLabels[i] + ", user " + userId);
                        } else {
                            CharSequence label = ai.loadLabel(pm);
                            if (label != null) {
                                packageLabels[i] = label.toString();
                            }
                            if (ai.icon != 0) {
                                this.defaultPackageName = this.sipper.mPackages[i];
                                this.icon = ai.loadIcon(pm);
                                break;
                            }
                        }
                    } catch (RemoteException e) {
                        Log.d("PowerUsageSummary", "Error while retrieving app info for package " + packageLabels[i] + ", user " + userId, e);
                    }
                }
                if (packageLabels.length == 1) {
                    this.name = packageLabels[0];
                } else {
                    for (String pkgName : this.sipper.mPackages) {
                        try {
                            PackageInfo pi = ipm.getPackageInfo(pkgName, 0, userId);
                            if (pi == null) {
                                Log.d("PowerUsageSummary", "Retrieving null package info for package " + pkgName + ", user " + userId);
                            } else if (pi.sharedUserLabel != 0) {
                                CharSequence nm = pm.getText(pkgName, pi.sharedUserLabel, pi.applicationInfo);
                                if (nm != null) {
                                    this.name = nm.toString();
                                    if (pi.applicationInfo.icon != 0) {
                                        this.defaultPackageName = pkgName;
                                        this.icon = pi.applicationInfo.loadIcon(pm);
                                    }
                                }
                            } else {
                                continue;
                            }
                        } catch (RemoteException e2) {
                            Log.d("PowerUsageSummary", "Error while retrieving package info for package " + pkgName + ", user " + userId, e2);
                        }
                    }
                }
            }
            String uidString = Integer.toString(uid);
            if (this.name == null) {
                this.name = uidString;
            }
            if (this.icon == null) {
                this.icon = pm.getDefaultActivityIcon();
            }
            UidToDetail utd = new UidToDetail();
            utd.name = this.name;
            utd.icon = this.icon;
            utd.packageName = this.defaultPackageName;
            sUidCache.put(uidString, utd);
            if (sHandler != null) {
                sHandler.sendMessage(sHandler.obtainMessage(1, this));
            }
        }
    }
}
