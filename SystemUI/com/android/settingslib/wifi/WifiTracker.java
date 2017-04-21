package com.android.settingslib.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import com.android.settingslib.R$string;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WifiTracker {
    private static final ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
    public static int sVerboseLogging = 0;
    private ArrayList<AccessPoint> mAccessPoints;
    private final AtomicBoolean mConnected;
    private final Context mContext;
    private final IntentFilter mFilter;
    private final boolean mIncludePasspoints;
    private final boolean mIncludeSaved;
    private final boolean mIncludeScans;
    private WifiInfo mLastInfo;
    private NetworkInfo mLastNetworkInfo;
    private final WifiListener mListener;
    private final MainHandler mMainHandler;
    final BroadcastReceiver mReceiver;
    private boolean mRegistered;
    private boolean mSavedNetworksExist;
    private Integer mScanId;
    private HashMap<String, ScanResult> mScanResultCache;
    Scanner mScanner;
    private HashMap<String, Integer> mSeenBssids;
    private final WifiManager mWifiManager;
    private final WorkHandler mWorkHandler;

    private final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (WifiTracker.this.mListener != null) {
                switch (msg.what) {
                    case 0:
                        WifiTracker.this.mListener.onConnectedChanged();
                        break;
                    case 1:
                        WifiTracker.this.mListener.onWifiStateChanged(msg.arg1);
                        break;
                    case 2:
                        WifiTracker.this.mListener.onAccessPointsChanged();
                        break;
                    case 3:
                        if (WifiTracker.this.mScanner != null) {
                            WifiTracker.this.mScanner.resume();
                            break;
                        }
                        break;
                    case 4:
                        if (WifiTracker.this.mScanner != null) {
                            WifiTracker.this.mScanner.pause();
                            break;
                        }
                        break;
                }
            }
        }
    }

    private static class Multimap<K, V> {
        private final HashMap<K, List<V>> store;

        private Multimap() {
            this.store = new HashMap();
        }

        List<V> getAll(K key) {
            List<V> values = (List) this.store.get(key);
            return values != null ? values : Collections.emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = (List) this.store.get(key);
            if (curVals == null) {
                curVals = new ArrayList(3);
                this.store.put(key, curVals);
            }
            curVals.add(val);
        }
    }

    class Scanner extends Handler {
        private int mRetry = 0;

        Scanner() {
        }

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            this.mRetry = 0;
            removeMessages(0);
        }

        public void handleMessage(Message message) {
            if (message.what == 0) {
                if (WifiTracker.this.mWifiManager.startScan()) {
                    this.mRetry = 0;
                } else {
                    int i = this.mRetry + 1;
                    this.mRetry = i;
                    if (i >= 3) {
                        this.mRetry = 0;
                        if (WifiTracker.this.mContext != null) {
                            Toast.makeText(WifiTracker.this.mContext, R$string.wifi_fail_to_scan, 1).show();
                        }
                        return;
                    }
                }
                sendEmptyMessageDelayed(0, 6000);
            }
        }
    }

    public interface WifiListener {
        void onAccessPointsChanged();

        void onConnectedChanged();

        void onWifiStateChanged(int i);
    }

    private final class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    WifiTracker.this.updateAccessPoints();
                    return;
                case 1:
                    WifiTracker.this.updateNetworkInfo((NetworkInfo) msg.obj);
                    return;
                case 2:
                    WifiTracker.this.handleResume();
                    return;
                default:
                    return;
            }
        }
    }

    public WifiTracker(Context context, WifiListener wifiListener, Looper workerLooper, boolean includeSaved, boolean includeScans) {
        this(context, wifiListener, workerLooper, includeSaved, includeScans, false);
    }

    public WifiTracker(Context context, WifiListener wifiListener, Looper workerLooper, boolean includeSaved, boolean includeScans, boolean includePasspoints) {
        this(context, wifiListener, workerLooper, includeSaved, includeScans, includePasspoints, (WifiManager) context.getSystemService("wifi"), Looper.myLooper());
    }

    WifiTracker(Context context, WifiListener wifiListener, Looper workerLooper, boolean includeSaved, boolean includeScans, boolean includePasspoints, WifiManager wifiManager, Looper currentLooper) {
        this.mConnected = new AtomicBoolean(false);
        this.mAccessPoints = new ArrayList();
        this.mSeenBssids = new HashMap();
        this.mScanResultCache = new HashMap();
        this.mScanId = Integer.valueOf(0);
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    WifiTracker.this.updateWifiState(intent.getIntExtra("wifi_state", 4));
                } else if ("android.net.wifi.SCAN_RESULTS".equals(action) || "android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action) || "android.net.wifi.LINK_CONFIGURATION_CHANGED".equals(action)) {
                    WifiTracker.this.mWorkHandler.sendEmptyMessage(0);
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    WifiTracker.this.mConnected.set(info.isConnected());
                    WifiTracker.this.mMainHandler.sendEmptyMessage(0);
                    WifiTracker.this.mWorkHandler.sendEmptyMessage(0);
                    WifiTracker.this.mWorkHandler.obtainMessage(1, info).sendToTarget();
                } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                    WifiTracker.this.mWorkHandler.sendEmptyMessage(1);
                }
            }
        };
        if (includeSaved || includeScans) {
            this.mContext = context;
            if (currentLooper == null) {
                currentLooper = Looper.getMainLooper();
            }
            this.mMainHandler = new MainHandler(currentLooper);
            if (workerLooper == null) {
                workerLooper = currentLooper;
            }
            this.mWorkHandler = new WorkHandler(workerLooper);
            this.mWifiManager = wifiManager;
            this.mIncludeSaved = includeSaved;
            this.mIncludeScans = includeScans;
            this.mIncludePasspoints = includePasspoints;
            this.mListener = wifiListener;
            sVerboseLogging = this.mWifiManager.getVerboseLoggingLevel();
            this.mFilter = new IntentFilter();
            this.mFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            this.mFilter.addAction("android.net.wifi.SCAN_RESULTS");
            this.mFilter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
            this.mFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
            this.mFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
            this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
            this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
            this.mFilter.addAction("android.net.wifi.RSSI_CHANGED");
            return;
        }
        throw new IllegalArgumentException("Must include either saved or scans");
    }

    public void forceScan() {
        if (this.mWifiManager.isWifiEnabled() && this.mScanner != null) {
            this.mScanner.forceScan();
        }
    }

    public void pauseScanning() {
        if (this.mScanner != null) {
            this.mScanner.pause();
            this.mScanner = null;
        }
    }

    public void resumeScanning() {
        if (this.mScanner == null) {
            this.mScanner = new Scanner();
        }
        this.mWorkHandler.sendEmptyMessage(2);
        if (this.mWifiManager.isWifiEnabled()) {
            this.mScanner.resume();
        }
        this.mWorkHandler.sendEmptyMessage(0);
    }

    public void startTracking() {
        resumeScanning();
        if (!this.mRegistered) {
            this.mContext.registerReceiver(this.mReceiver, this.mFilter);
            this.mRegistered = true;
        }
    }

    public void stopTracking() {
        if (this.mRegistered) {
            this.mWorkHandler.removeMessages(0);
            this.mWorkHandler.removeMessages(1);
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mRegistered = false;
        }
        pauseScanning();
    }

    public List<AccessPoint> getAccessPoints() {
        ArrayList<AccessPoint> cachedaccessPoints;
        synchronized (this.mAccessPoints) {
            cachedaccessPoints = new ArrayList();
            for (AccessPoint accessPoint : this.mAccessPoints) {
                cachedaccessPoints.add((AccessPoint) accessPoint.clone());
            }
        }
        return cachedaccessPoints;
    }

    public WifiManager getManager() {
        return this.mWifiManager;
    }

    public void dump(PrintWriter pw) {
        pw.println("  - wifi tracker ------");
        for (AccessPoint accessPoint : getAccessPoints()) {
            pw.println("  " + accessPoint);
        }
    }

    private void handleResume() {
        this.mScanResultCache.clear();
        this.mSeenBssids.clear();
        this.mScanId = Integer.valueOf(0);
    }

    private Collection<ScanResult> fetchScanResults() {
        this.mScanId = Integer.valueOf(this.mScanId.intValue() + 1);
        for (ScanResult newResult : this.mWifiManager.getScanResults()) {
            this.mScanResultCache.put(newResult.BSSID, newResult);
            this.mSeenBssids.put(newResult.BSSID, this.mScanId);
        }
        if (this.mScanId.intValue() > 3) {
            Integer threshold = Integer.valueOf(this.mScanId.intValue() - 3);
            Iterator<Entry<String, Integer>> it = this.mSeenBssids.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Integer> e = (Entry) it.next();
                if (((Integer) e.getValue()).intValue() < threshold.intValue()) {
                    ScanResult result = (ScanResult) this.mScanResultCache.get(e.getKey());
                    this.mScanResultCache.remove(e.getKey());
                    it.remove();
                }
            }
        }
        return this.mScanResultCache.values();
    }

    private WifiConfiguration getWifiConfigurationForNetworkId(int networkId) {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (this.mLastInfo != null && networkId == config.networkId) {
                    if (!config.selfAdded || config.numAssociation != 0) {
                        return config;
                    }
                }
            }
        }
        return null;
    }

    private void updateAccessPoints() {
        AccessPoint accessPoint;
        List<AccessPoint> cachedAccessPoints = getAccessPoints();
        ArrayList<AccessPoint> accessPoints = new ArrayList();
        for (AccessPoint accessPoint2 : cachedAccessPoints) {
            accessPoint2.clearConfig();
        }
        mReadWriteLock.readLock().lock();
        try {
            WifiConfiguration config;
            boolean found;
            Multimap<String, AccessPoint> apMap = new Multimap();
            WifiConfiguration connectionConfig = null;
            if (this.mLastInfo != null) {
                connectionConfig = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
            }
            List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
            if (configs != null) {
                this.mSavedNetworksExist = configs.size() != 0;
                for (WifiConfiguration config2 : configs) {
                    if (!config2.selfAdded || config2.numAssociation != 0) {
                        accessPoint2 = getCachedOrCreate(config2, (List) cachedAccessPoints);
                        if (!(this.mLastInfo == null || this.mLastNetworkInfo == null || config2.isPasspoint())) {
                            accessPoint2.update(connectionConfig, this.mLastInfo, this.mLastNetworkInfo);
                        }
                        if (this.mIncludeSaved) {
                            if (!config2.isPasspoint() || this.mIncludePasspoints) {
                                accessPoints.add(accessPoint2);
                            }
                            if (!config2.isPasspoint()) {
                                apMap.put(accessPoint2.getSsidStr(), accessPoint2);
                            }
                        } else {
                            cachedAccessPoints.add(accessPoint2);
                        }
                    }
                }
            }
            Collection<ScanResult> results = fetchScanResults();
            if (results != null) {
                for (ScanResult result : results) {
                    if (!(result.SSID == null || result.SSID.length() == 0 || result.capabilities.contains("[IBSS]"))) {
                        found = false;
                        for (AccessPoint accessPoint22 : apMap.getAll(result.SSID)) {
                            if (accessPoint22.update(result)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found && this.mIncludeScans) {
                            accessPoint22 = getCachedOrCreate(result, (List) cachedAccessPoints);
                            if (!(this.mLastInfo == null || this.mLastNetworkInfo == null)) {
                                accessPoint22.update(connectionConfig, this.mLastInfo, this.mLastNetworkInfo);
                            }
                            if (result.isPasspointNetwork()) {
                                config2 = this.mWifiManager.getMatchingWifiConfig(result);
                                if (config2 != null) {
                                    accessPoint22.update(config2);
                                }
                            }
                            if (!(this.mLastInfo == null || this.mLastInfo.getBSSID() == null || !this.mLastInfo.getBSSID().equals(result.BSSID) || connectionConfig == null || !connectionConfig.isPasspoint())) {
                                accessPoint22.update(connectionConfig);
                            }
                            accessPoints.add(accessPoint22);
                            apMap.put(accessPoint22.getSsidStr(), accessPoint22);
                        }
                    }
                }
            }
            mReadWriteLock.readLock().unlock();
            Collections.sort(accessPoints);
            for (AccessPoint prevAccessPoint : this.mAccessPoints) {
                if (prevAccessPoint.getSsid() != null) {
                    String prevSsid = prevAccessPoint.getSsidStr();
                    found = false;
                    for (AccessPoint newAccessPoint : accessPoints) {
                        if (newAccessPoint.getSsid() != null && newAccessPoint.getSsid().equals(prevSsid)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                    }
                }
            }
            this.mAccessPoints = accessPoints;
            this.mMainHandler.sendEmptyMessage(2);
        } catch (Throwable th) {
            mReadWriteLock.readLock().unlock();
        }
    }

    private AccessPoint getCachedOrCreate(ScanResult result, List<AccessPoint> cache) {
        int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (((AccessPoint) cache.get(i)).matches(result)) {
                AccessPoint ret = (AccessPoint) cache.remove(i);
                ret.update(result);
                return ret;
            }
        }
        return new AccessPoint(this.mContext, result);
    }

    private AccessPoint getCachedOrCreate(WifiConfiguration config, List<AccessPoint> cache) {
        int N = cache.size();
        for (int i = 0; i < N; i++) {
            if (((AccessPoint) cache.get(i)).matches(config)) {
                AccessPoint ret = (AccessPoint) cache.remove(i);
                ret.loadConfig(config);
                return ret;
            }
        }
        return new AccessPoint(this.mContext, config);
    }

    private void updateNetworkInfo(NetworkInfo networkInfo) {
        if (this.mWifiManager.isWifiEnabled()) {
            if (networkInfo == null || networkInfo.getDetailedState() != DetailedState.OBTAINING_IPADDR) {
                this.mMainHandler.sendEmptyMessage(3);
            } else {
                this.mMainHandler.sendEmptyMessage(4);
            }
            mReadWriteLock.writeLock().lock();
            try {
                this.mLastInfo = this.mWifiManager.getConnectionInfo();
                if (networkInfo != null) {
                    this.mLastNetworkInfo = networkInfo;
                }
                mReadWriteLock.writeLock().unlock();
                boolean reorder = false;
                mReadWriteLock.readLock().lock();
                WifiConfiguration connectionConfig = null;
                try {
                    if (this.mLastInfo != null) {
                        connectionConfig = getWifiConfigurationForNetworkId(this.mLastInfo.getNetworkId());
                    }
                    for (int i = this.mAccessPoints.size() - 1; i >= 0; i--) {
                        if (((AccessPoint) this.mAccessPoints.get(i)).update(connectionConfig, this.mLastInfo, this.mLastNetworkInfo)) {
                            reorder = true;
                        }
                    }
                    if (reorder) {
                        synchronized (this.mAccessPoints) {
                            Collections.sort(this.mAccessPoints);
                        }
                        this.mMainHandler.sendEmptyMessage(2);
                    }
                } finally {
                    mReadWriteLock.readLock().unlock();
                }
            } catch (Throwable th) {
                mReadWriteLock.writeLock().unlock();
            }
        } else {
            this.mMainHandler.sendEmptyMessage(4);
        }
    }

    private void updateWifiState(int state) {
        if (state != 3) {
            mReadWriteLock.writeLock().lock();
            try {
                this.mLastInfo = null;
                this.mLastNetworkInfo = null;
                if (this.mScanner != null) {
                    this.mScanner.pause();
                }
            } finally {
                mReadWriteLock.writeLock().unlock();
            }
        } else if (this.mScanner != null) {
            this.mScanner.resume();
        }
        this.mMainHandler.obtainMessage(1, state, 0).sendToTarget();
    }
}
