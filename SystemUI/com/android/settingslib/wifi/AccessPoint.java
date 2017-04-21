package com.android.settingslib.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.IWifiManager.Stub;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan.VerbatimBuilder;
import android.util.Log;
import android.util.LruCache;
import com.android.settingslib.R$array;
import com.android.settingslib.R$string;
import com.mediatek.settingslib.wifi.AccessPointExt;

public class AccessPoint implements Comparable<AccessPoint>, Cloneable {
    public AccessPointExt mAccessPointExt;
    private AccessPointListener mAccessPointListener;
    private WifiConfiguration mConfig;
    private final Context mContext;
    private WifiInfo mInfo;
    private NetworkInfo mNetworkInfo;
    private int mRssi = Integer.MAX_VALUE;
    public LruCache<String, ScanResult> mScanResultCache = new LruCache(32);
    private long mSeen = 0;
    private boolean mWpsAvailable = false;
    private int networkId = -1;
    private int pskType = 0;
    private int security;
    private String ssid;

    public interface AccessPointListener {
        void onAccessPointChanged(AccessPoint accessPoint);

        void onLevelChanged(AccessPoint accessPoint);
    }

    AccessPoint(Context context, ScanResult result) {
        this.mContext = context;
        this.mAccessPointExt = new AccessPointExt(context);
        initWithScanResult(result);
    }

    AccessPoint(Context context, WifiConfiguration config) {
        this.mContext = context;
        this.mAccessPointExt = new AccessPointExt(context);
        loadConfig(config);
    }

    public Object clone() {
        AccessPoint object = null;
        try {
            return (AccessPoint) super.clone();
        } catch (CloneNotSupportedException e) {
            Log.e("SettingsLib.AccessPoint", "CloneNotSupportedException happens in clone()");
            return object;
        }
    }

    public int compareTo(AccessPoint other) {
        if (isActive() && !other.isActive()) {
            return -1;
        }
        if (!isActive() && other.isActive()) {
            return 1;
        }
        if (this.mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        if (this.mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) {
            return 1;
        }
        if (this.networkId != -1 && other.networkId == -1) {
            return -1;
        }
        if (this.networkId == -1 && other.networkId != -1) {
            return 1;
        }
        int difference = WifiManager.compareSignalLevel(other.mRssi, this.mRssi);
        if (difference != 0) {
            return difference;
        }
        int securityDiff = other.security - this.security;
        if (securityDiff != 0) {
            return securityDiff;
        }
        return this.ssid.compareToIgnoreCase(other.ssid);
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof AccessPoint)) {
            return false;
        }
        if (compareTo((AccessPoint) other) == 0) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        int result = 0;
        if (this.mInfo != null) {
            result = (this.mInfo.hashCode() * 13) + 0;
        }
        return ((result + (this.mRssi * 19)) + (this.networkId * 23)) + (this.ssid.hashCode() * 29);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder().append("AccessPoint(").append(this.ssid);
        if (isSaved()) {
            builder.append(',').append("saved");
        }
        if (isActive()) {
            builder.append(',').append("active");
        }
        if (isEphemeral()) {
            builder.append(',').append("ephemeral");
        }
        if (isConnectable()) {
            builder.append(',').append("connectable");
        }
        if (this.security != 0) {
            builder.append(',').append(securityToString(this.security, this.pskType));
        }
        return builder.append(')').toString();
    }

    public boolean matches(ScanResult result) {
        return this.ssid.equals(result.SSID) && this.security == getSecurity(result);
    }

    public boolean matches(WifiConfiguration config) {
        boolean z = false;
        if (config.isPasspoint() && this.mConfig != null && this.mConfig.isPasspoint()) {
            return config.FQDN.equals(this.mConfig.providerFriendlyName);
        }
        if (this.ssid.equals(removeDoubleQuotes(config.SSID)) && this.security == getSecurity(config)) {
            z = true;
        }
        return z;
    }

    public WifiConfiguration getConfig() {
        return this.mConfig;
    }

    public void clearConfig() {
        this.mConfig = null;
        this.networkId = -1;
    }

    public int getLevel() {
        if (this.mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(this.mRssi, 4);
    }

    public int getRssi() {
        int rssi = Integer.MIN_VALUE;
        for (ScanResult result : this.mScanResultCache.snapshot().values()) {
            if (result.level > rssi) {
                rssi = result.level;
            }
        }
        return rssi;
    }

    public long getSeen() {
        long seen = 0;
        for (ScanResult result : this.mScanResultCache.snapshot().values()) {
            if (result.timestamp > seen) {
                seen = result.timestamp;
            }
        }
        return seen;
    }

    public int getSecurity() {
        return this.security;
    }

    public String getSsidStr() {
        return this.ssid;
    }

    public CharSequence getSsid() {
        SpannableString str = new SpannableString(this.ssid);
        str.setSpan(new VerbatimBuilder(this.ssid).build(), 0, this.ssid.length(), 18);
        return str;
    }

    public DetailedState getDetailedState() {
        return this.mNetworkInfo != null ? this.mNetworkInfo.getDetailedState() : null;
    }

    public String getSummary() {
        return getSettingsSummary();
    }

    public String getSettingsSummary() {
        StringBuilder summary = new StringBuilder();
        if (isActive() && this.mConfig != null && this.mConfig.isPasspoint()) {
            summary.append(getSummary(this.mContext, getDetailedState(), false, this.mConfig.providerFriendlyName));
        } else if (isActive()) {
            summary.append(getSummary(this.mContext, getDetailedState(), this.mInfo != null ? this.mInfo.isEphemeral() : false));
        } else if (this.mConfig != null && this.mConfig.isPasspoint()) {
            summary.append(String.format(this.mContext.getString(R$string.available_via_passpoint), new Object[]{this.mConfig.providerFriendlyName}));
        } else if (this.mConfig == null || !this.mConfig.hasNoInternetAccess()) {
            if (this.mConfig != null && ((this.mConfig.status == 1 && this.mConfig.disableReason != 0) || this.mConfig.autoJoinStatus >= 128)) {
                if (this.mConfig.autoJoinStatus < 128) {
                    switch (this.mConfig.disableReason) {
                        case 0:
                        case 4:
                            summary.append(this.mContext.getString(R$string.wifi_disabled_generic));
                            break;
                        case 1:
                        case 2:
                            summary.append(this.mContext.getString(R$string.wifi_disabled_network_failure));
                            break;
                        case 3:
                            summary.append(this.mContext.getString(R$string.wifi_disabled_password_failure));
                            break;
                        default:
                            break;
                    }
                } else if (this.mConfig.disableReason == 2) {
                    summary.append(this.mContext.getString(R$string.wifi_disabled_network_failure));
                } else if (this.mConfig.disableReason == 3) {
                    summary.append(this.mContext.getString(R$string.wifi_disabled_password_failure));
                } else {
                    this.mAccessPointExt.appendApSummary(summary, this.mConfig.autoJoinStatus, this.mContext.getString(R$string.wifi_disabled_wifi_failure), this.mContext.getString(R$string.wifi_disabled_generic));
                }
            } else if (this.mRssi == Integer.MAX_VALUE) {
                summary.append(this.mContext.getString(R$string.wifi_not_in_range));
            } else if (this.mConfig != null) {
                summary.append(this.mContext.getString(R$string.wifi_remembered));
            }
        } else {
            summary.append(this.mContext.getString(R$string.wifi_no_internet));
        }
        if (WifiTracker.sVerboseLogging > 0) {
            if (!(this.mInfo == null || this.mNetworkInfo == null)) {
                summary.append(" f=").append(Integer.toString(this.mInfo.getFrequency()));
            }
            summary.append(" ").append(getVisibilityStatus());
            if (this.mConfig != null && this.mConfig.autoJoinStatus > 0) {
                summary.append(" (").append(this.mConfig.autoJoinStatus);
                if (this.mConfig.blackListTimestamp > 0) {
                    long diff = (System.currentTimeMillis() - this.mConfig.blackListTimestamp) / 1000;
                    long sec = diff % 60;
                    long min = (diff / 60) % 60;
                    long hour = (min / 60) % 60;
                    summary.append(", ");
                    if (hour > 0) {
                        summary.append(Long.toString(hour)).append("h ");
                    }
                    summary.append(Long.toString(min)).append("m ");
                    summary.append(Long.toString(sec)).append("s ");
                }
                summary.append(")");
            }
            if (this.mConfig != null && this.mConfig.numIpConfigFailures > 0) {
                summary.append(" ipf=").append(this.mConfig.numIpConfigFailures);
            }
            if (this.mConfig != null && this.mConfig.numConnectionFailures > 0) {
                summary.append(" cf=").append(this.mConfig.numConnectionFailures);
            }
            if (this.mConfig != null && this.mConfig.numAuthFailures > 0) {
                summary.append(" authf=").append(this.mConfig.numAuthFailures);
            }
            if (this.mConfig != null && this.mConfig.numNoInternetAccessReports > 0) {
                summary.append(" noInt=").append(this.mConfig.numNoInternetAccessReports);
            }
        }
        return summary.toString();
    }

    private String getVisibilityStatus() {
        StringBuilder visibility = new StringBuilder();
        StringBuilder scans24GHz = null;
        StringBuilder scans5GHz = null;
        Object obj = null;
        long now = System.currentTimeMillis();
        if (this.mInfo != null) {
            obj = this.mInfo.getBSSID();
            if (obj != null) {
                visibility.append(" ").append(obj);
            }
            visibility.append(" rssi=").append(this.mInfo.getRssi());
            visibility.append(" ");
            visibility.append(" score=").append(this.mInfo.score);
            visibility.append(String.format(" tx=%.1f,", new Object[]{Double.valueOf(this.mInfo.txSuccessRate)}));
            visibility.append(String.format("%.1f,", new Object[]{Double.valueOf(this.mInfo.txRetriesRate)}));
            visibility.append(String.format("%.1f ", new Object[]{Double.valueOf(this.mInfo.txBadRate)}));
            visibility.append(String.format("rx=%.1f", new Object[]{Double.valueOf(this.mInfo.rxSuccessRate)}));
        }
        int rssi5 = WifiConfiguration.INVALID_RSSI;
        int rssi24 = WifiConfiguration.INVALID_RSSI;
        int num5 = 0;
        int num24 = 0;
        int numBlackListed = 0;
        int n24 = 0;
        int n5 = 0;
        for (ScanResult result : this.mScanResultCache.snapshot().values()) {
            if (result.seen != 0) {
                if (result.autoJoinStatus != 0) {
                    numBlackListed++;
                }
                if (result.frequency >= 4900 && result.frequency <= 5900) {
                    num5++;
                } else if (result.frequency >= 2400 && result.frequency <= 2500) {
                    num24++;
                }
                if (now - result.seen <= 20000) {
                    if (result.frequency >= 4900 && result.frequency <= 5900) {
                        if (result.level > rssi5) {
                            rssi5 = result.level;
                        }
                        if (n5 < 4) {
                            if (scans5GHz == null) {
                                scans5GHz = new StringBuilder();
                            }
                            scans5GHz.append(" \n{").append(result.BSSID);
                            if (obj != null && result.BSSID.equals(obj)) {
                                scans5GHz.append("*");
                            }
                            scans5GHz.append("=").append(result.frequency);
                            scans5GHz.append(",").append(result.level);
                            if (result.autoJoinStatus != 0) {
                                scans5GHz.append(",st=").append(result.autoJoinStatus);
                            }
                            if (result.numIpConfigFailures != 0) {
                                scans5GHz.append(",ipf=").append(result.numIpConfigFailures);
                            }
                            scans5GHz.append("}");
                            n5++;
                        }
                    } else if (result.frequency >= 2400 && result.frequency <= 2500) {
                        if (result.level > rssi24) {
                            rssi24 = result.level;
                        }
                        if (n24 < 4) {
                            if (scans24GHz == null) {
                                scans24GHz = new StringBuilder();
                            }
                            scans24GHz.append(" \n{").append(result.BSSID);
                            if (obj != null && result.BSSID.equals(obj)) {
                                scans24GHz.append("*");
                            }
                            scans24GHz.append("=").append(result.frequency);
                            scans24GHz.append(",").append(result.level);
                            if (result.autoJoinStatus != 0) {
                                scans24GHz.append(",st=").append(result.autoJoinStatus);
                            }
                            if (result.numIpConfigFailures != 0) {
                                scans24GHz.append(",ipf=").append(result.numIpConfigFailures);
                            }
                            scans24GHz.append("}");
                            n24++;
                        }
                    }
                }
            }
        }
        visibility.append(" [");
        if (num24 > 0) {
            visibility.append("(").append(num24).append(")");
            if (n24 > 4) {
                visibility.append("max=").append(rssi24);
                if (scans24GHz != null) {
                    visibility.append(",").append(scans24GHz.toString());
                }
            } else if (scans24GHz != null) {
                visibility.append(scans24GHz.toString());
            }
        }
        visibility.append(";");
        if (num5 > 0) {
            visibility.append("(").append(num5).append(")");
            if (n5 > 4) {
                visibility.append("max=").append(rssi5);
                if (scans5GHz != null) {
                    visibility.append(",").append(scans5GHz.toString());
                }
            } else if (scans5GHz != null) {
                visibility.append(scans5GHz.toString());
            }
        }
        if (numBlackListed > 0) {
            visibility.append("!").append(numBlackListed);
        }
        visibility.append("]");
        return visibility.toString();
    }

    public boolean isActive() {
        if (this.mNetworkInfo != null) {
            return (this.networkId == -1 && this.mNetworkInfo.getState() == State.DISCONNECTED) ? false : true;
        } else {
            return false;
        }
    }

    public boolean isConnectable() {
        return getLevel() != -1 && getDetailedState() == null;
    }

    public boolean isEphemeral() {
        if (this.mInfo == null || !this.mInfo.isEphemeral() || this.mNetworkInfo == null || this.mNetworkInfo.getState() == State.DISCONNECTED) {
            return false;
        }
        return true;
    }

    public boolean isPasspoint() {
        return this.mConfig != null ? this.mConfig.isPasspoint() : false;
    }

    private boolean isInfoForThisAccessPoint(WifiConfiguration config, WifiInfo info) {
        if (!isPasspoint() && this.networkId != -1) {
            return this.networkId == info.getNetworkId();
        } else if (config != null) {
            return matches(config);
        } else {
            return this.ssid.equals(removeDoubleQuotes(info.getSSID()));
        }
    }

    public boolean isSaved() {
        return this.networkId != -1;
    }

    public void generateOpenNetworkConfig() {
        if (this.security != 0) {
            throw new IllegalStateException();
        } else if (this.mConfig == null) {
            this.mConfig = new WifiConfiguration();
            this.mConfig.SSID = convertToQuotedString(this.ssid);
            this.mConfig.allowedKeyManagement.set(0);
        }
    }

    void loadConfig(WifiConfiguration config) {
        if (config.isPasspoint()) {
            this.ssid = config.providerFriendlyName;
        } else {
            this.ssid = config.SSID == null ? "" : removeDoubleQuotes(config.SSID);
        }
        this.security = getSecurity(config);
        this.networkId = config.networkId;
        this.mConfig = config;
    }

    private void initWithScanResult(ScanResult result) {
        this.ssid = result.SSID;
        this.security = getSecurity(result);
        this.mWpsAvailable = this.security != 3 ? result.capabilities.contains("WPS") : false;
        if (this.security == 2) {
            this.pskType = getPskType(result);
        }
        this.mRssi = result.level;
        this.mSeen = result.timestamp;
    }

    boolean update(ScanResult result) {
        if (!matches(result)) {
            return false;
        }
        this.mScanResultCache.get(result.BSSID);
        this.mScanResultCache.put(result.BSSID, result);
        int oldLevel = getLevel();
        int oldRssi = getRssi();
        this.mSeen = getSeen();
        this.mRssi = (getRssi() + oldRssi) / 2;
        int newLevel = getLevel();
        if (!(newLevel <= 0 || newLevel == oldLevel || this.mAccessPointListener == null)) {
            this.mAccessPointListener.onLevelChanged(this);
        }
        if (this.security == 2) {
            this.pskType = getPskType(result);
        }
        if (this.mAccessPointListener != null) {
            this.mAccessPointListener.onAccessPointChanged(this);
        }
        return true;
    }

    boolean update(WifiConfiguration config, WifiInfo info, NetworkInfo networkInfo) {
        boolean reorder = false;
        if (info != null && isInfoForThisAccessPoint(config, info)) {
            reorder = this.mInfo == null;
            this.mRssi = info.getRssi();
            this.mInfo = info;
            this.mNetworkInfo = networkInfo;
            if (this.mAccessPointListener != null) {
                this.mAccessPointListener.onAccessPointChanged(this);
            }
        } else if (this.mInfo != null) {
            reorder = true;
            this.mInfo = null;
            this.mNetworkInfo = null;
            if (this.mAccessPointListener != null) {
                this.mAccessPointListener.onAccessPointChanged(this);
            }
        }
        return reorder;
    }

    void update(WifiConfiguration config) {
        this.mConfig = config;
        this.networkId = config.networkId;
        if (this.mAccessPointListener != null) {
            this.mAccessPointListener.onAccessPointChanged(this);
        }
    }

    public static String getSummary(Context context, String ssid, DetailedState state, boolean isEphemeral, String passpointProvider) {
        if (state == DetailedState.CONNECTED && ssid == null) {
            if (!TextUtils.isEmpty(passpointProvider)) {
                return String.format(context.getString(R$string.connected_via_passpoint), new Object[]{passpointProvider});
            } else if (isEphemeral) {
                return context.getString(R$string.connected_via_wfa);
            }
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (state == DetailedState.CONNECTED && AccessPointExt.getWifiPlugin(context).shouldCheckNetworkCapabilities()) {
            Network currentNetwork;
            try {
                currentNetwork = Stub.asInterface(ServiceManager.getService("wifi")).getCurrentNetwork();
            } catch (RemoteException e) {
                currentNetwork = null;
            }
            NetworkCapabilities nc = cm.getNetworkCapabilities(currentNetwork);
            if (!(nc == null || nc.hasCapability(16))) {
                return context.getString(R$string.wifi_connected_no_internet);
            }
        }
        String[] formats = context.getResources().getStringArray(ssid == null ? R$array.wifi_status : R$array.wifi_status_with_ssid);
        int index = state.ordinal();
        if (index >= formats.length || formats[index].length() == 0) {
            return "";
        }
        return String.format(formats[index], new Object[]{ssid});
    }

    public static String getSummary(Context context, DetailedState state, boolean isEphemeral) {
        return getSummary(context, null, state, isEphemeral, null);
    }

    public static String getSummary(Context context, DetailedState state, boolean isEphemeral, String passpointProvider) {
        return getSummary(context, null, state, isEphemeral, passpointProvider);
    }

    public static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    private static int getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return 3;
        }
        if (wpa2) {
            return 2;
        }
        if (wpa) {
            return 1;
        }
        Log.w("SettingsLib.AccessPoint", "Received abnormal flag string: " + result.capabilities);
        return 0;
    }

    private static int getSecurity(ScanResult result) {
        int security = AccessPointExt.getSecurity(result);
        if (security != -1) {
            return security;
        }
        if (result.capabilities.contains("WEP")) {
            return 1;
        }
        if (result.capabilities.contains("PSK")) {
            return 2;
        }
        if (result.capabilities.contains("EAP")) {
            return 3;
        }
        return 0;
    }

    static int getSecurity(WifiConfiguration config) {
        int i = 1;
        if (config.allowedKeyManagement.get(1)) {
            return 2;
        }
        if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            return 3;
        }
        int security = AccessPointExt.getSecurity(config);
        if (security != -1) {
            return security;
        }
        if (config.wepKeys[0] == null) {
            i = 0;
        }
        return i;
    }

    public static String securityToString(int security, int pskType) {
        if (security == 1) {
            return "WEP";
        }
        if (security == 2) {
            if (pskType == 1) {
                return "WPA";
            }
            if (pskType == 2) {
                return "WPA2";
            }
            if (pskType == 3) {
                return "WPA_WPA2";
            }
            return "PSK";
        } else if (security == 3) {
            return "EAP";
        } else {
            return "NONE";
        }
    }

    static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
