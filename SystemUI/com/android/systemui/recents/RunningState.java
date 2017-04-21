package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.Debug.MemoryInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.assis.app.MAIN.CONSTANT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class RunningState {
    static Object sGlobalLock = new Object();
    static RunningState sInstance;
    final ArrayList<ProcessItem> mAllProcessItems = new ArrayList();
    final ActivityManager mAm;
    final Context mApplicationContext;
    final BackgroundHandler mBackgroundHandler;
    ArrayList<MergedItem> mBackgroundItems = new ArrayList();
    long mBackgroundProcessMemory;
    final HandlerThread mBackgroundThread;
    long mForegroundProcessMemory;
    final Handler mHandler = new Handler() {
        int mNextUpdate = 0;

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    int i;
                    if (msg.arg1 != 0) {
                        i = 2;
                    } else {
                        i = 1;
                    }
                    this.mNextUpdate = i;
                    break;
                case 3:
                    synchronized (RunningState.this.mLock) {
                        if (!RunningState.this.mResumed) {
                            Log.d("RunningStateTAG", "No resumed ,return in handler");
                            return;
                        }
                    }
                    break;
            }
        }
    };
    boolean mHaveData;
    final ArrayList<ProcessItem> mInterestingProcesses = new ArrayList();
    ArrayList<BaseItem> mItems = new ArrayList();
    private Locale mLastLocale;
    final Object mLock = new Object();
    ArrayList<MergedItem> mMergedItems = new ArrayList();
    int mNumBackgroundProcesses;
    int mNumForegroundProcesses;
    int mNumServiceProcesses;
    final PackageManager mPm;
    final ArrayList<ProcessItem> mProcessItems = new ArrayList();
    OnRefreshUiListener mRefreshUiListener;
    boolean mResumed;
    final SparseArray<ProcessItem> mRunningProcesses = new SparseArray();
    int mSequence = 0;
    final ServiceProcessComparator mServiceProcessComparator = new ServiceProcessComparator();
    long mServiceProcessMemory;
    final SparseArray<HashMap<String, ProcessItem>> mServiceProcessesByName = new SparseArray();
    final SparseArray<ProcessItem> mServiceProcessesByPid = new SparseArray();
    boolean mWatchingBackgroundItems;

    final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (RunningState.this.mLock) {
                        if (!RunningState.this.mResumed) {
                            return;
                        }
                    }
                    break;
            }
        }
    }

    static class BaseItem {
        long mActiveSince;
        boolean mBackground;
        int mCurSeq;
        CharSequence mDisplayLabel;
        final boolean mIsProcess;
        String mLabel;
        boolean mNeedDivider;
        PackageItemInfo mPackageInfo;
        long mSize;
        String mSizeStr;

        public BaseItem(boolean isProcess) {
            this.mIsProcess = isProcess;
        }
    }

    static class MergedItem extends BaseItem {
        private int mLastNumProcesses = -1;
        private int mLastNumServices = -1;
        final ArrayList<ProcessItem> mOtherProcesses = new ArrayList();
        ProcessItem mProcess;
        final ArrayList<ServiceItem> mServices = new ArrayList();

        MergedItem() {
            super(false);
        }

        boolean update(Context context, boolean background) {
            this.mPackageInfo = this.mProcess.mPackageInfo;
            this.mDisplayLabel = this.mProcess.mDisplayLabel;
            this.mLabel = this.mProcess.mLabel;
            this.mBackground = background;
            if (!this.mBackground) {
                int i;
                if (this.mProcess.mPid > 0) {
                    i = 1;
                } else {
                    i = 0;
                }
                int numProcesses = i + this.mOtherProcesses.size();
                int numServices = this.mServices.size();
                if (!(this.mLastNumProcesses == numProcesses && this.mLastNumServices == numServices)) {
                    this.mLastNumProcesses = numProcesses;
                    this.mLastNumServices = numServices;
                }
            }
            this.mActiveSince = -1;
            for (int i2 = 0; i2 < this.mServices.size(); i2++) {
                ServiceItem si = (ServiceItem) this.mServices.get(i2);
                if (si.mActiveSince >= 0 && this.mActiveSince < si.mActiveSince) {
                    this.mActiveSince = si.mActiveSince;
                }
            }
            return false;
        }

        boolean updateSize(Context context) {
            this.mSize = this.mProcess.mSize;
            for (int i = 0; i < this.mOtherProcesses.size(); i++) {
                this.mSize = ((ProcessItem) this.mOtherProcesses.get(i)).mSize + this.mSize;
            }
            String sizeStr = Formatter.formatShortFileSize(context, this.mSize);
            if (sizeStr.equals(this.mSizeStr)) {
                return false;
            }
            this.mSizeStr = sizeStr;
            return false;
        }
    }

    interface OnRefreshUiListener {
        void onRefreshUi(int i);
    }

    static class ProcessItem extends BaseItem {
        long mActiveSince;
        ProcessItem mClient;
        final SparseArray<ProcessItem> mDependentProcesses = new SparseArray();
        boolean mIsStarted;
        boolean mIsSystem;
        int mLastNumDependentProcesses;
        MergedItem mMergedItem;
        int mPid;
        final String mProcessName;
        RunningAppProcessInfo mRunningProcessInfo;
        int mRunningSeq;
        final HashMap<ComponentName, ServiceItem> mServices = new HashMap();
        final int mUid;

        public ProcessItem(Context context, int uid, String processName) {
            super(true);
            this.mUid = uid;
            this.mProcessName = processName;
        }

        void ensureLabel(PackageManager pm) {
            if (this.mLabel == null) {
                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(this.mProcessName, 0);
                    if (ai.uid == this.mUid) {
                        this.mDisplayLabel = ai.loadLabel(pm);
                        this.mLabel = this.mDisplayLabel.toString();
                        this.mPackageInfo = ai;
                        return;
                    }
                } catch (NameNotFoundException e) {
                    Log.d("RunningStateTAG", "catch exception, process name not found");
                }
                String[] pkgs = pm.getPackagesForUid(this.mUid);
                if (pkgs != null) {
                    if (pkgs.length == 1) {
                        try {
                            ai = pm.getApplicationInfo(pkgs[0], 0);
                            this.mDisplayLabel = ai.loadLabel(pm);
                            this.mLabel = this.mDisplayLabel.toString();
                            this.mPackageInfo = ai;
                            return;
                        } catch (NameNotFoundException e2) {
                            Log.d("RunningStateTAG", "catch exception, process name not found");
                        }
                    }
                    for (String name : pkgs) {
                        try {
                            PackageInfo pi = pm.getPackageInfo(name, 0);
                            if (pi.sharedUserLabel != 0) {
                                CharSequence nm = pm.getText(name, pi.sharedUserLabel, pi.applicationInfo);
                                if (nm != null) {
                                    this.mDisplayLabel = nm;
                                    this.mLabel = nm.toString();
                                    this.mPackageInfo = pi.applicationInfo;
                                    return;
                                }
                            } else {
                                continue;
                            }
                        } catch (NameNotFoundException e3) {
                            Log.d("RunningStateTAG", "catch exception, process name not found");
                        }
                    }
                    if (this.mServices.size() > 0) {
                        this.mPackageInfo = ((ServiceItem) this.mServices.values().iterator().next()).mServiceInfo.applicationInfo;
                        this.mDisplayLabel = this.mPackageInfo.loadLabel(pm);
                        this.mLabel = this.mDisplayLabel.toString();
                        return;
                    }
                    try {
                        ai = pm.getApplicationInfo(pkgs[0], 0);
                        this.mDisplayLabel = ai.loadLabel(pm);
                        this.mLabel = this.mDisplayLabel.toString();
                        this.mPackageInfo = ai;
                    } catch (NameNotFoundException e4) {
                        Log.d("RunningStateTAG", "catch exception, process name not found");
                    }
                }
            }
        }

        boolean updateService(Context context, RunningServiceInfo service) {
            long activeSince;
            String str = null;
            PackageManager pm = context.getPackageManager();
            boolean changed = false;
            ServiceItem si = (ServiceItem) this.mServices.get(service.service);
            if (si == null) {
                changed = true;
                si = new ServiceItem();
                si.mRunningService = service;
                try {
                    si.mServiceInfo = pm.getServiceInfo(service.service, 0);
                } catch (NameNotFoundException e) {
                    Log.d("RunningStateTAG", "catch exception, process name not found");
                }
                si.mDisplayLabel = RunningState.makeLabel(pm, si.mRunningService.service.getClassName(), si.mServiceInfo);
                if (this.mDisplayLabel != null) {
                    str = this.mDisplayLabel.toString();
                }
                this.mLabel = str;
                si.mPackageInfo = si.mServiceInfo.applicationInfo;
                this.mServices.put(service.service, si);
            }
            si.mCurSeq = this.mCurSeq;
            si.mRunningService = service;
            if (service.restarting == 0) {
                activeSince = service.activeSince;
            } else {
                activeSince = -1;
            }
            if (si.mActiveSince != activeSince) {
                si.mActiveSince = activeSince;
                changed = true;
            }
            if (service.clientPackage == null || service.clientLabel == 0) {
                if (si.mShownAsStarted) {
                    return changed;
                }
                si.mShownAsStarted = true;
                return true;
            } else if (!si.mShownAsStarted) {
                return changed;
            } else {
                si.mShownAsStarted = false;
                return true;
            }
        }

        boolean updateSize(Context context, MemoryInfo mem, int curSeq) {
            this.mSize = ((long) mem.getTotalPrivateDirty()) * CONSTANT.ONE_KILO;
            if (this.mCurSeq == curSeq) {
                String sizeStr = Formatter.formatShortFileSize(context, this.mSize);
                if (!sizeStr.equals(this.mSizeStr)) {
                    this.mSizeStr = sizeStr;
                    return false;
                }
            }
            return false;
        }

        boolean buildDependencyChain(Context context, PackageManager pm, int curSeq) {
            int np = this.mDependentProcesses.size();
            boolean changed = false;
            for (int i = 0; i < np; i++) {
                ProcessItem proc = (ProcessItem) this.mDependentProcesses.valueAt(i);
                if (proc.mClient != this) {
                    changed = true;
                    proc.mClient = this;
                }
                proc.mCurSeq = curSeq;
                proc.ensureLabel(pm);
                changed |= proc.buildDependencyChain(context, pm, curSeq);
            }
            if (this.mLastNumDependentProcesses == this.mDependentProcesses.size()) {
                return changed;
            }
            this.mLastNumDependentProcesses = this.mDependentProcesses.size();
            return true;
        }

        void addDependentProcesses(ArrayList<BaseItem> dest, ArrayList<ProcessItem> destProc) {
            int np = this.mDependentProcesses.size();
            for (int i = 0; i < np; i++) {
                ProcessItem proc = (ProcessItem) this.mDependentProcesses.valueAt(i);
                proc.addDependentProcesses(dest, destProc);
                dest.add(proc);
                if (proc.mPid > 0) {
                    destProc.add(proc);
                }
            }
        }
    }

    static class ServiceItem extends BaseItem {
        MergedItem mMergedItem;
        RunningServiceInfo mRunningService;
        ServiceInfo mServiceInfo;
        boolean mShownAsStarted;

        public ServiceItem() {
            super(false);
        }
    }

    static class ServiceProcessComparator implements Comparator<ProcessItem> {
        ServiceProcessComparator() {
        }

        public int compare(ProcessItem object1, ProcessItem object2) {
            int i = 1;
            int i2 = -1;
            if (object1.mIsStarted != object2.mIsStarted) {
                if (!object1.mIsStarted) {
                    i2 = 1;
                }
                return i2;
            } else if (object1.mIsSystem != object2.mIsSystem) {
                if (!object1.mIsSystem) {
                    i = -1;
                }
                return i;
            } else if (object1.mActiveSince == object2.mActiveSince) {
                return 0;
            } else {
                if (object1.mActiveSince <= object2.mActiveSince) {
                    i2 = 1;
                }
                return i2;
            }
        }
    }

    static CharSequence makeLabel(PackageManager pm, String className, PackageItemInfo item) {
        if (!(item == null || (item.labelRes == 0 && item.nonLocalizedLabel == null))) {
            CharSequence label = item.loadLabel(pm);
            if (label != null) {
                return label;
            }
        }
        String label2 = className;
        int tail = label2.lastIndexOf(46);
        if (tail >= 0) {
            label2 = label2.substring(tail + 1, label2.length());
        }
        return label2;
    }

    static RunningState getInstance(Context context) {
        RunningState runningState;
        synchronized (sGlobalLock) {
            if (sInstance == null) {
                sInstance = new RunningState(context);
            }
            runningState = sInstance;
        }
        return runningState;
    }

    private RunningState(Context context) {
        this.mApplicationContext = context.getApplicationContext();
        IntentFilter langChangedFilter = new IntentFilter();
        langChangedFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mLastLocale = Locale.getDefault();
        this.mApplicationContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.CONFIGURATION_CHANGED".equals(intent.getAction()) && !RunningState.this.mLastLocale.equals(Locale.getDefault())) {
                    RunningState.this.clearData();
                    RunningState.this.mLastLocale = Locale.getDefault();
                    RunningState.this.updateNow();
                }
            }
        }, langChangedFilter);
        this.mAm = (ActivityManager) this.mApplicationContext.getSystemService("activity");
        this.mPm = this.mApplicationContext.getPackageManager();
        this.mResumed = false;
        this.mBackgroundThread = new HandlerThread("RunningState:Background");
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new BackgroundHandler(this.mBackgroundThread.getLooper());
        update(this.mApplicationContext, this.mAm);
    }

    void clearData() {
        this.mHaveData = false;
        this.mItems.clear();
        this.mMergedItems.clear();
        this.mBackgroundItems.clear();
        this.mProcessItems.clear();
    }

    public void updateNow() {
        synchronized (this.mLock) {
            this.mBackgroundHandler.removeMessages(1);
            this.mBackgroundHandler.sendEmptyMessage(1);
        }
    }

    private boolean isInterestingProcess(RunningAppProcessInfo pi) {
        if ((pi.flags & 1) != 0) {
            return true;
        }
        return (pi.flags & 2) == 0 && pi.importance == 100 && pi.importanceReasonCode == 0;
    }

    private boolean updateServiceProcess(Context context, ActivityManager am) {
        boolean changed = false;
        this.mServiceProcessesByName.clear();
        this.mServiceProcessesByPid.clear();
        List<RunningServiceInfo> services = am.getRunningServices(100);
        int ns = services != null ? services.size() : 0;
        for (int i = 0; i < ns; i++) {
            RunningServiceInfo si = (RunningServiceInfo) services.get(i);
            if ((si.started || si.clientLabel != 0) && (si.flags & 8) == 0) {
                HashMap<String, ProcessItem> procs = (HashMap) this.mServiceProcessesByName.get(si.uid);
                if (procs == null) {
                    procs = new HashMap();
                    this.mServiceProcessesByName.put(si.uid, procs);
                }
                ProcessItem proc = (ProcessItem) procs.get(si.process);
                if (proc == null) {
                    changed = true;
                    proc = new ProcessItem(context, si.uid, si.process);
                    procs.put(si.process, proc);
                }
                if (proc.mCurSeq != this.mSequence) {
                    int pid = si.restarting == 0 ? si.pid : 0;
                    if (pid != proc.mPid) {
                        changed = true;
                        if (proc.mPid != pid) {
                            if (proc.mPid != 0) {
                                this.mServiceProcessesByPid.remove(proc.mPid);
                            }
                            if (pid != 0) {
                                this.mServiceProcessesByPid.put(pid, proc);
                            }
                            proc.mPid = pid;
                        }
                    }
                    proc.mDependentProcesses.clear();
                    proc.mCurSeq = this.mSequence;
                }
                changed |= proc.updateService(context, si);
            }
        }
        return changed;
    }

    private boolean updateRunningProcess(Context context, ActivityManager am) {
        int i;
        boolean changed = false;
        PackageManager pm = context.getPackageManager();
        List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        int np = processes != null ? processes.size() : 0;
        this.mInterestingProcesses.clear();
        this.mRunningProcesses.clear();
        for (i = 0; i < np; i++) {
            RunningAppProcessInfo pi = (RunningAppProcessInfo) processes.get(i);
            ProcessItem proc = (ProcessItem) this.mServiceProcessesByPid.get(pi.pid);
            if (proc == null) {
                proc = (ProcessItem) this.mRunningProcesses.get(pi.pid);
                if (proc == null) {
                    changed = true;
                    proc = new ProcessItem(context, pi.uid, pi.processName);
                    proc.mPid = pi.pid;
                    this.mRunningProcesses.put(pi.pid, proc);
                }
                proc.mDependentProcesses.clear();
            }
            if (isInterestingProcess(pi)) {
                if (!this.mInterestingProcesses.contains(proc)) {
                    changed = true;
                    this.mInterestingProcesses.add(proc);
                }
                proc.mCurSeq = this.mSequence;
                proc.ensureLabel(pm);
            }
            proc.mRunningSeq = this.mSequence;
            proc.mRunningProcessInfo = pi;
        }
        int nrp = this.mRunningProcesses.size();
        for (i = 0; i < nrp; i++) {
            proc = (ProcessItem) this.mRunningProcesses.valueAt(i);
            if (proc == null || proc.mRunningSeq != this.mSequence) {
                changed = true;
                this.mRunningProcesses.remove(this.mRunningProcesses.keyAt(i));
            } else {
                int clientPid = proc.mRunningProcessInfo.importanceReasonPid;
                if (clientPid != 0) {
                    ProcessItem client = (ProcessItem) this.mServiceProcessesByPid.get(clientPid);
                    if (client == null) {
                        client = (ProcessItem) this.mRunningProcesses.get(clientPid);
                    }
                    if (client != null) {
                        client.mDependentProcesses.put(proc.mPid, proc);
                    }
                } else {
                    proc.mClient = null;
                }
            }
        }
        int nhp = this.mInterestingProcesses.size();
        i = 0;
        while (i < nhp) {
            if (this.mRunningProcesses.get(((ProcessItem) this.mInterestingProcesses.get(i)).mPid) == null) {
                changed = true;
                this.mInterestingProcesses.remove(i);
                i--;
                nhp--;
            }
            i++;
        }
        return changed;
    }

    private boolean removeDeadProcess(Context context, ActivityManager am) {
        int i;
        boolean changed = false;
        PackageManager pm = context.getPackageManager();
        int nap = this.mServiceProcessesByPid.size();
        for (i = 0; i < nap; i++) {
            ProcessItem proc = (ProcessItem) this.mServiceProcessesByPid.valueAt(i);
            if (proc.mCurSeq == this.mSequence) {
                changed |= proc.buildDependencyChain(context, pm, this.mSequence);
            }
        }
        for (i = 0; i < this.mServiceProcessesByName.size(); i++) {
            HashMap<String, ProcessItem> procs = (HashMap) this.mServiceProcessesByName.valueAt(i);
            Iterator<ProcessItem> pit = procs.values().iterator();
            while (pit.hasNext()) {
                ProcessItem pi = (ProcessItem) pit.next();
                if (pi.mCurSeq == this.mSequence) {
                    pi.ensureLabel(pm);
                    if (pi.mPid == 0) {
                        pi.mDependentProcesses.clear();
                    }
                    Iterator<ServiceItem> sit = pi.mServices.values().iterator();
                    while (sit.hasNext()) {
                        if (((ServiceItem) sit.next()).mCurSeq != this.mSequence) {
                            changed = true;
                            sit.remove();
                        }
                    }
                } else {
                    changed = true;
                    pit.remove();
                    if (procs.size() == 0) {
                        this.mServiceProcessesByName.remove(this.mServiceProcessesByName.keyAt(i));
                    }
                    if (pi.mPid != 0) {
                        this.mServiceProcessesByPid.remove(pi.mPid);
                    }
                }
            }
        }
        return changed;
    }

    private void updateDisplayProcess(Context context) {
        int i;
        ArrayList<ProcessItem> sortedProcesses = new ArrayList();
        for (i = 0; i < this.mServiceProcessesByName.size(); i++) {
            for (ProcessItem pi : ((HashMap) this.mServiceProcessesByName.valueAt(i)).values()) {
                ProcessItem pi2;
                pi2.mIsSystem = false;
                pi2.mIsStarted = true;
                pi2.mActiveSince = Long.MAX_VALUE;
                for (ServiceItem si : pi2.mServices.values()) {
                    if (!(si.mServiceInfo == null || (si.mServiceInfo.applicationInfo.flags & 1) == 0)) {
                        pi2.mIsSystem = true;
                    }
                    if (!(si.mRunningService == null || si.mRunningService.clientLabel == 0)) {
                        pi2.mIsStarted = false;
                        if (pi2.mActiveSince > si.mRunningService.activeSince) {
                            pi2.mActiveSince = si.mRunningService.activeSince;
                        }
                    }
                }
                sortedProcesses.add(pi2);
            }
        }
        Collections.sort(sortedProcesses, this.mServiceProcessComparator);
        ArrayList<BaseItem> newItems = new ArrayList();
        ArrayList<MergedItem> newMergedItems = new ArrayList();
        this.mProcessItems.clear();
        for (i = 0; i < sortedProcesses.size(); i++) {
            pi2 = (ProcessItem) sortedProcesses.get(i);
            pi2.mNeedDivider = false;
            int firstProc = this.mProcessItems.size();
            pi2.addDependentProcesses(newItems, this.mProcessItems);
            newItems.add(pi2);
            if (pi2.mPid > 0) {
                this.mProcessItems.add(pi2);
            }
            MergedItem mergedItem = null;
            boolean haveAllMerged = false;
            boolean needDivider = false;
            for (ServiceItem si2 : pi2.mServices.values()) {
                si2.mNeedDivider = needDivider;
                needDivider = true;
                newItems.add(si2);
                if (si2.mMergedItem != null) {
                    if (!(mergedItem == null || mergedItem == si2.mMergedItem)) {
                        haveAllMerged = false;
                    }
                    mergedItem = si2.mMergedItem;
                } else {
                    haveAllMerged = false;
                }
            }
            if (!haveAllMerged || mergedItem == null || mergedItem.mServices.size() != pi2.mServices.size()) {
                mergedItem = new MergedItem();
                for (ServiceItem si22 : pi2.mServices.values()) {
                    mergedItem.mServices.add(si22);
                    si22.mMergedItem = mergedItem;
                }
                mergedItem.mProcess = pi2;
                mergedItem.mOtherProcesses.clear();
                for (int mpi = firstProc; mpi < this.mProcessItems.size() - 1; mpi++) {
                    mergedItem.mOtherProcesses.add((ProcessItem) this.mProcessItems.get(mpi));
                }
            }
            mergedItem.update(context, false);
            newMergedItems.add(mergedItem);
        }
        int nhp = this.mInterestingProcesses.size();
        for (i = 0; i < nhp; i++) {
            ProcessItem proc = (ProcessItem) this.mInterestingProcesses.get(i);
            if (proc.mClient == null && proc.mServices.size() <= 0) {
                if (proc.mMergedItem == null) {
                    proc.mMergedItem = new MergedItem();
                    proc.mMergedItem.mProcess = proc;
                }
                proc.mMergedItem.update(context, false);
                newMergedItems.add(0, proc.mMergedItem);
                this.mProcessItems.add(proc);
            }
        }
        synchronized (this.mLock) {
            this.mItems = newItems;
            this.mMergedItems = newMergedItems;
        }
    }

    private boolean updateMemoryInfo(Context context) {
        int i;
        ArrayList<MergedItem> newBackgroundItems;
        int bgi;
        this.mAllProcessItems.clear();
        this.mAllProcessItems.addAll(this.mProcessItems);
        boolean z = false;
        int numBackgroundProcesses = 0;
        int numForegroundProcesses = 0;
        int numServiceProcesses = 0;
        int nrp = this.mRunningProcesses.size();
        for (i = 0; i < nrp; i++) {
            ProcessItem proc = (ProcessItem) this.mRunningProcesses.valueAt(i);
            if (proc.mCurSeq == this.mSequence) {
                numServiceProcesses++;
            } else if (proc.mRunningProcessInfo.importance >= 400) {
                numBackgroundProcesses++;
                this.mAllProcessItems.add(proc);
            } else if (proc.mRunningProcessInfo.importance <= 200) {
                numForegroundProcesses++;
                this.mAllProcessItems.add(proc);
            } else {
                Log.d("RunningState", "Unknown non-service process: " + proc.mProcessName + " #" + proc.mPid);
            }
        }
        long backgroundProcessMemory = 0;
        long foregroundProcessMemory = 0;
        long serviceProcessMemory = 0;
        ArrayList arrayList = null;
        int numProc = this.mAllProcessItems.size();
        int[] pids = new int[numProc];
        for (i = 0; i < numProc; i++) {
            pids[i] = ((ProcessItem) this.mAllProcessItems.get(i)).mPid;
        }
        MemoryInfo[] mem = ActivityManagerNative.getDefault().getProcessMemoryInfo(pids);
        int bgIndex = 0;
        i = 0;
        ArrayList<MergedItem> newBackgroundItems2 = null;
        while (i < pids.length) {
            proc = (ProcessItem) this.mAllProcessItems.get(i);
            z |= proc.updateSize(context, mem[i], this.mSequence);
            if (proc.mCurSeq == this.mSequence) {
                serviceProcessMemory += proc.mSize;
                newBackgroundItems = newBackgroundItems2;
            } else if (proc.mRunningProcessInfo.importance >= 400) {
                MergedItem mergedItem;
                backgroundProcessMemory += proc.mSize;
                if (newBackgroundItems2 != null) {
                    proc.mMergedItem = new MergedItem();
                    mergedItem = proc.mMergedItem;
                    proc.mMergedItem.mProcess = proc;
                    newBackgroundItems2.add(mergedItem);
                    newBackgroundItems = newBackgroundItems2;
                } else {
                    try {
                        if (bgIndex >= this.mBackgroundItems.size() || ((MergedItem) this.mBackgroundItems.get(bgIndex)).mProcess != proc) {
                            arrayList = new ArrayList(numBackgroundProcesses);
                            for (bgi = 0; bgi < bgIndex; bgi++) {
                                arrayList.add((MergedItem) this.mBackgroundItems.get(bgi));
                            }
                            proc.mMergedItem = new MergedItem();
                            mergedItem = proc.mMergedItem;
                            proc.mMergedItem.mProcess = proc;
                            arrayList.add(mergedItem);
                        } else {
                            mergedItem = (MergedItem) this.mBackgroundItems.get(bgIndex);
                            newBackgroundItems = newBackgroundItems2;
                        }
                    } catch (RemoteException e) {
                        newBackgroundItems = newBackgroundItems2;
                    }
                }
                try {
                    mergedItem.update(context, true);
                    mergedItem.updateSize(context);
                    bgIndex++;
                } catch (RemoteException e2) {
                }
            } else if (proc.mRunningProcessInfo.importance <= 200) {
                foregroundProcessMemory += proc.mSize;
                newBackgroundItems = newBackgroundItems2;
            } else {
                newBackgroundItems = newBackgroundItems2;
            }
            i++;
            newBackgroundItems2 = newBackgroundItems;
        }
        newBackgroundItems = newBackgroundItems2;
        if (arrayList == null && this.mBackgroundItems.size() > numBackgroundProcesses) {
            arrayList = new ArrayList(numBackgroundProcesses);
            for (bgi = 0; bgi < numBackgroundProcesses; bgi++) {
                arrayList.add((MergedItem) this.mBackgroundItems.get(bgi));
            }
        }
        for (i = 0; i < this.mMergedItems.size(); i++) {
            ((MergedItem) this.mMergedItems.get(i)).updateSize(context);
        }
        synchronized (this.mLock) {
            this.mNumBackgroundProcesses = numBackgroundProcesses;
            this.mNumForegroundProcesses = numForegroundProcesses;
            this.mNumServiceProcesses = numServiceProcesses;
            this.mBackgroundProcessMemory = backgroundProcessMemory;
            this.mForegroundProcessMemory = foregroundProcessMemory;
            this.mServiceProcessMemory = serviceProcessMemory;
            if (arrayList != null) {
                this.mBackgroundItems = arrayList;
                if (this.mWatchingBackgroundItems) {
                    z = true;
                }
            }
            if (!this.mHaveData) {
                this.mHaveData = true;
                this.mLock.notifyAll();
            }
        }
        return z;
        Log.d("RunningStateTAG", "catch remote exception");
        arrayList = new ArrayList(numBackgroundProcesses);
        for (bgi = 0; bgi < numBackgroundProcesses; bgi++) {
            arrayList.add((MergedItem) this.mBackgroundItems.get(bgi));
        }
        for (i = 0; i < this.mMergedItems.size(); i++) {
            ((MergedItem) this.mMergedItems.get(i)).updateSize(context);
        }
        synchronized (this.mLock) {
            this.mNumBackgroundProcesses = numBackgroundProcesses;
            this.mNumForegroundProcesses = numForegroundProcesses;
            this.mNumServiceProcesses = numServiceProcesses;
            this.mBackgroundProcessMemory = backgroundProcessMemory;
            this.mForegroundProcessMemory = foregroundProcessMemory;
            this.mServiceProcessMemory = serviceProcessMemory;
            if (arrayList != null) {
                this.mBackgroundItems = arrayList;
                if (this.mWatchingBackgroundItems) {
                    z = true;
                }
            }
            if (this.mHaveData) {
                this.mHaveData = true;
                this.mLock.notifyAll();
            }
        }
        return z;
    }

    private boolean update(Context context, ActivityManager am) {
        this.mSequence++;
        boolean changed = (updateServiceProcess(context, am) | updateRunningProcess(context, am)) | removeDeadProcess(context, am);
        if (changed) {
            updateDisplayProcess(context);
        }
        return changed | updateMemoryInfo(context);
    }
}
