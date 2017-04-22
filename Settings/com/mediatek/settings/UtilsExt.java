package com.mediatek.settings;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.common.MPlugin;
import com.mediatek.settings.ext.DefaultApnSettingsExt;
import com.mediatek.settings.ext.DefaultAppListExt;
import com.mediatek.settings.ext.DefaultAppsExt;
import com.mediatek.settings.ext.DefaultAudioProfileExt;
import com.mediatek.settings.ext.DefaultDataProtectionExt;
import com.mediatek.settings.ext.DefaultDataUsageSummaryExt;
import com.mediatek.settings.ext.DefaultDevExt;
import com.mediatek.settings.ext.DefaultDeviceInfoSettingsExt;
import com.mediatek.settings.ext.DefaultDisplaySettingsExt;
import com.mediatek.settings.ext.DefaultMdmPermControlExt;
import com.mediatek.settings.ext.DefaultPermissionControlExt;
import com.mediatek.settings.ext.DefaultPplSettingsEntryExt;
import com.mediatek.settings.ext.DefaultRCSSettings;
import com.mediatek.settings.ext.DefaultRcseOnlyApnExt;
import com.mediatek.settings.ext.DefaultSettingsMiscExt;
import com.mediatek.settings.ext.DefaultSimManagementExt;
import com.mediatek.settings.ext.DefaultSimRoamingExt;
import com.mediatek.settings.ext.DefaultSmsDialogExt;
import com.mediatek.settings.ext.DefaultSmsPreferenceExt;
import com.mediatek.settings.ext.DefaultStatusBarPlmnDisplayExt;
import com.mediatek.settings.ext.DefaultStatusExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.DefaultWifiApDialogExt;
import com.mediatek.settings.ext.DefaultWifiExt;
import com.mediatek.settings.ext.DefaultWifiSettingsExt;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.ext.IAppListExt;
import com.mediatek.settings.ext.IAppsExt;
import com.mediatek.settings.ext.IAudioProfileExt;
import com.mediatek.settings.ext.IDataProtectionExt;
import com.mediatek.settings.ext.IDataUsageSummaryExt;
import com.mediatek.settings.ext.IDevExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;
import com.mediatek.settings.ext.IDisplaySettingsExt;
import com.mediatek.settings.ext.IMdmPermissionControlExt;
import com.mediatek.settings.ext.IPermissionControlExt;
import com.mediatek.settings.ext.IPplSettingsEntryExt;
import com.mediatek.settings.ext.IRCSSettings;
import com.mediatek.settings.ext.IRcseOnlyApnExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.settings.ext.ISmsDialogExt;
import com.mediatek.settings.ext.ISmsPreferenceExt;
import com.mediatek.settings.ext.IStatusBarPlmnDisplayExt;
import com.mediatek.settings.ext.IStatusExt;
import com.mediatek.settings.ext.IWfcSettingsExt;
import com.mediatek.settings.ext.IWifiApDialogExt;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.settings.ext.IWifiSettingsExt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class UtilsExt {
    public static ArrayList<String> disableAppList = readFile("/system/etc/disableapplist.txt");

    public static String getWifiIpAddresses() {
        String addresses = "";
        try {
            NetworkInterface wifiNetwork = NetworkInterface.getByName("wlan0");
            if (wifiNetwork == null) {
                Log.d("UtilsExt", "wifiNetwork is null");
                return null;
            }
            Enumeration<InetAddress> enumeration = wifiNetwork.getInetAddresses();
            if (enumeration == null) {
                Log.d("UtilsExt", "enumeration is null");
                return null;
            }
            while (enumeration.hasMoreElements()) {
                InetAddress inet = (InetAddress) enumeration.nextElement();
                String hostAddress = inet.getHostAddress();
                if (hostAddress.contains("%")) {
                    hostAddress = hostAddress.substring(0, hostAddress.indexOf("%"));
                }
                Log.d("UtilsExt", "InetAddress = " + inet.toString());
                Log.d("UtilsExt", "hostAddress = " + hostAddress);
                if (inet instanceof Inet6Address) {
                    Log.d("UtilsExt", "IPV6 address = " + hostAddress);
                    addresses = addresses + hostAddress + "; ";
                } else if (inet instanceof Inet4Address) {
                    Log.d("UtilsExt", "IPV4 address = " + hostAddress);
                    addresses = hostAddress + ", " + addresses;
                }
            }
            Log.d("UtilsExt", "IP addresses = " + addresses);
            if (!"".equals(addresses) && (addresses.endsWith(", ") || addresses.endsWith("; "))) {
                addresses = addresses.substring(0, addresses.length() - 2);
            } else if ("".equals(addresses)) {
                addresses = null;
            }
            Log.d("UtilsExt", "The result of IP addresses = " + addresses);
            return addresses;
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ISettingsMiscExt getMiscPlugin(Context context) {
        ISettingsMiscExt ext = (ISettingsMiscExt) MPlugin.createInstance(ISettingsMiscExt.class.getName(), context);
        if (ext == null) {
            return new DefaultSettingsMiscExt(context);
        }
        return ext;
    }

    public static IWifiExt getWifiPlugin(Context context) {
        IWifiExt ext = (IWifiExt) MPlugin.createInstance(IWifiExt.class.getName(), context);
        if (ext == null) {
            return new DefaultWifiExt(context);
        }
        return ext;
    }

    public static IWifiSettingsExt getWifiSettingsPlugin(Context context) {
        IWifiSettingsExt ext = (IWifiSettingsExt) MPlugin.createInstance(IWifiSettingsExt.class.getName(), context);
        if (ext == null) {
            return new DefaultWifiSettingsExt();
        }
        return ext;
    }

    public static IWifiApDialogExt getWifiApDialogPlugin(Context context) {
        IWifiApDialogExt ext = (IWifiApDialogExt) MPlugin.createInstance(IWifiApDialogExt.class.getName(), context);
        if (ext == null) {
            return new DefaultWifiApDialogExt();
        }
        return ext;
    }

    public static ISmsDialogExt getSMSApDialogPlugin(Context context) {
        ISmsDialogExt ext = (ISmsDialogExt) MPlugin.createInstance(ISmsDialogExt.class.getName(), context);
        if (ext == null) {
            return new DefaultSmsDialogExt(context);
        }
        return ext;
    }

    public static ISmsPreferenceExt getSmsPreferencePlugin(Context context) {
        ISmsPreferenceExt ext = (ISmsPreferenceExt) MPlugin.createInstance(ISmsPreferenceExt.class.getName(), context);
        if (ext == null) {
            return new DefaultSmsPreferenceExt();
        }
        return ext;
    }

    public static IAppListExt getAppListPlugin(Context context) {
        IAppListExt ext = (IAppListExt) MPlugin.createInstance(IAppListExt.class.getName(), context);
        if (ext == null) {
            return new DefaultAppListExt(context);
        }
        return ext;
    }

    public static IDevExt getDevExtPlugin(Context context) {
        IDevExt ext = (IDevExt) MPlugin.createInstance(IDevExt.class.getName(), context);
        if (ext == null) {
            return new DefaultDevExt(context);
        }
        return ext;
    }

    public static IDeviceInfoSettingsExt getDeviceInfoSettingsPlugin(Context context) {
        IDeviceInfoSettingsExt ext = (IDeviceInfoSettingsExt) MPlugin.createInstance(IDeviceInfoSettingsExt.class.getName(), context);
        if (ext == null) {
            return new DefaultDeviceInfoSettingsExt();
        }
        return ext;
    }

    public static boolean isPackageExist(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 1);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static IAudioProfileExt getAudioProfilePlugin(Context context) {
        IAudioProfileExt ext = (IAudioProfileExt) MPlugin.createInstance(IAudioProfileExt.class.getName(), context);
        if (ext == null) {
            return new DefaultAudioProfileExt(context);
        }
        return ext;
    }

    public static IRCSSettings getRcsSettingsPlugin(Context context) {
        IRCSSettings ext = (IRCSSettings) MPlugin.createInstance(IRCSSettings.class.getName(), context);
        if (ext == null) {
            return new DefaultRCSSettings();
        }
        return ext;
    }

    public static IDataProtectionExt getDataProectExtPlugin(Context context) {
        IDataProtectionExt ext = (IDataProtectionExt) MPlugin.createInstance(IDataProtectionExt.class.getName(), context);
        if (ext == null) {
            return new DefaultDataProtectionExt(context);
        }
        return ext;
    }

    public static IPplSettingsEntryExt getPrivacyProtectionLockExtPlugin(Context context) {
        IPplSettingsEntryExt ext = (IPplSettingsEntryExt) MPlugin.createInstance(IPplSettingsEntryExt.class.getName(), context);
        if (ext == null) {
            return new DefaultPplSettingsEntryExt(context);
        }
        return ext;
    }

    public static IMdmPermissionControlExt getMdmPermControlExtPlugin(Context context) {
        IMdmPermissionControlExt ext = (IMdmPermissionControlExt) MPlugin.createInstance(IMdmPermissionControlExt.class.getName(), context);
        if (ext == null) {
            return new DefaultMdmPermControlExt(context);
        }
        return ext;
    }

    public static IPermissionControlExt getPermControlExtPlugin(Context context) {
        IPermissionControlExt ext = (IPermissionControlExt) MPlugin.createInstance(IPermissionControlExt.class.getName(), context);
        if (ext == null) {
            return new DefaultPermissionControlExt(context);
        }
        return ext;
    }

    public static ISimManagementExt getSimManagmentExtPlugin(Context context) {
        ISimManagementExt ext = (ISimManagementExt) MPlugin.createInstance(ISimManagementExt.class.getName(), context);
        if (ext == null) {
            return new DefaultSimManagementExt();
        }
        return ext;
    }

    public static IApnSettingsExt getApnSettingsPlugin(Context context) {
        IApnSettingsExt ext = (IApnSettingsExt) MPlugin.createInstance(IApnSettingsExt.class.getName(), context);
        if (ext == null) {
            return new DefaultApnSettingsExt();
        }
        return ext;
    }

    public static IDataUsageSummaryExt getDataUsageSummaryPlugin(Context context) {
        IDataUsageSummaryExt ext = (IDataUsageSummaryExt) MPlugin.createInstance(IDataUsageSummaryExt.class.getName(), context);
        if (ext == null) {
            return new DefaultDataUsageSummaryExt(context);
        }
        return ext;
    }

    public static ISimRoamingExt getSimRoamingExtPlugin(Context context) {
        ISimRoamingExt ext = (ISimRoamingExt) MPlugin.createInstance(ISimRoamingExt.class.getName(), context);
        if (ext == null) {
            return new DefaultSimRoamingExt();
        }
        return ext;
    }

    public static IRcseOnlyApnExt getRcseApnPlugin(Context context) {
        IRcseOnlyApnExt ext = (IRcseOnlyApnExt) MPlugin.createInstance(IRcseOnlyApnExt.class.getName(), context);
        if (ext == null) {
            return new DefaultRcseOnlyApnExt();
        }
        return ext;
    }

    public static IStatusExt getStatusExtPlugin(Context context) {
        IStatusExt ext = (IStatusExt) MPlugin.createInstance(IStatusExt.class.getName(), context);
        if (ext == null) {
            return new DefaultStatusExt();
        }
        return ext;
    }

    public static IStatusBarPlmnDisplayExt getStatusBarPlmnPlugin(Context context) {
        IStatusBarPlmnDisplayExt ext = (IStatusBarPlmnDisplayExt) MPlugin.createInstance(IStatusBarPlmnDisplayExt.class.getName(), context);
        if (ext == null) {
            return new DefaultStatusBarPlmnDisplayExt(context);
        }
        return ext;
    }

    public static IAppsExt getAppsExtPlugin(Context context) {
        IAppsExt ext = (IAppsExt) MPlugin.createInstance(IAppsExt.class.getName(), context);
        if (ext == null) {
            return new DefaultAppsExt(context);
        }
        return ext;
    }

    public static IWfcSettingsExt getWfcSettingsPlugin(Context context) {
        IWfcSettingsExt ext = (IWfcSettingsExt) MPlugin.createInstance(IWfcSettingsExt.class.getName(), context);
        if (ext == null) {
            return new DefaultWfcSettingsExt();
        }
        return ext;
    }

    public static IDisplaySettingsExt getDisplaySettingsPlugin(Context context) {
        IDisplaySettingsExt ext = (IDisplaySettingsExt) MPlugin.createInstance(IDisplaySettingsExt.class.getName(), context);
        if (ext == null) {
            return new DefaultDisplaySettingsExt(context);
        }
        return ext;
    }

    public static ArrayList<String> readFile(String path) {
        IOException io;
        Throwable th;
        ArrayList<String> appsList = new ArrayList();
        appsList.clear();
        File file = new File(path);
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            if (file.exists()) {
                FileReader fr = new FileReader(file);
                try {
                    BufferedReader br = new BufferedReader(fr);
                    while (true) {
                        try {
                            String line = br.readLine();
                            if (line == null) {
                                break;
                            }
                            Log.d("UtilsExt", " read line " + line);
                            appsList.add(line);
                        } catch (IOException e) {
                            io = e;
                            bufferedReader = br;
                            fileReader = fr;
                        } catch (Throwable th2) {
                            th = th2;
                            bufferedReader = br;
                            fileReader = fr;
                        }
                    }
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException io2) {
                            io2.printStackTrace();
                        }
                    }
                    if (fr != null) {
                        fr.close();
                    }
                    return appsList;
                } catch (IOException e2) {
                    io2 = e2;
                    fileReader = fr;
                    try {
                        Log.d("UtilsExt", "IOException");
                        io2.printStackTrace();
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException io22) {
                                io22.printStackTrace();
                                return null;
                            }
                        }
                        if (fileReader != null) {
                            fileReader.close();
                        }
                        return null;
                    } catch (Throwable th3) {
                        th = th3;
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException io222) {
                                io222.printStackTrace();
                                throw th;
                            }
                        }
                        if (fileReader != null) {
                            fileReader.close();
                        }
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    fileReader = fr;
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (fileReader != null) {
                        fileReader.close();
                    }
                    throw th;
                }
            }
            Log.d("UtilsExt", "file in " + path + " does not exist!");
            return null;
        } catch (IOException e3) {
            io222 = e3;
            Log.d("UtilsExt", "IOException");
            io222.printStackTrace();
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileReader != null) {
                fileReader.close();
            }
            return null;
        }
    }

    public static boolean shouldDisableForAutoSanity() {
        boolean autoSanity = SystemProperties.get("ro.mtk.autosanity").equals("1");
        String buildType = SystemProperties.get("ro.build.type", "");
        Log.d("UtilsExt", "autoSanity: " + autoSanity + " buildType: " + buildType);
        if (!autoSanity || TextUtils.isEmpty(buildType) || !buildType.endsWith("eng")) {
            return false;
        }
        Log.d("UtilsExt", "ShouldDisableForAutoSanity()...");
        return true;
    }

    public static boolean isGmsBuild(Context context) {
        boolean isGms = false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo("com.google.android.gms", 0);
            if (info == null || (info.flags & 1) == 0) {
                isGms = false;
            } else {
                isGms = true;
            }
        } catch (NameNotFoundException e) {
        }
        Log.d("UtilsExt", "Is GMS Build? " + isGms);
        return isGms;
    }
}
