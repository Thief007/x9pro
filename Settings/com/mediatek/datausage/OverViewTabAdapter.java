package com.mediatek.datausage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory.Entry;
import android.net.NetworkTemplate;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.net.ChartData;
import com.android.settings.net.NetworkPolicyEditor;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.common.operamax.ILoaderService;
import com.mediatek.common.operamax.ILoaderStateListener.Stub;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OverViewTabAdapter extends BaseExpandableListAdapter {
    private BidiFormatter mBidiFormatter;
    private Context mContext;
    private Switch mDataSavingSwitch;
    private View mDataSavingView;
    private LinearLayout mDataSavingZone;
    private List<String> mGroup;
    private long[] mLimitBytes;
    private int mMobilePos;
    private long[] mMobileTotal;
    private NetworkPolicyEditor mPolicyEditor;
    private NetworkPolicyManager mPolicyManager;
    public ILoaderService mSavingService;
    private int mSimNum;
    private INetworkStatsSession mStatsSession;
    private List<SubscriptionInfo> mSublist;
    public Handler mUiHandler;
    private int mWifiPos;
    private long mWifiToday;
    private long mWifiTotal;
    private ServiceConnection serviceConnection = new C06992();
    private Stub stateListener = new C06981();

    class C06981 extends Stub {
        C06981() {
        }

        public void onTunnelState(int state) throws RemoteException {
            Log.d("OverViewTabAdapter", "Loader service onTunnelState");
        }

        public void onSavingState(int state) throws RemoteException {
            Log.d("OverViewTabAdapter", "Loader service onSavingState");
            OverViewTabAdapter.this.mUiHandler.sendEmptyMessage(0);
        }
    }

    class C06992 implements ServiceConnection {
        C06992() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("OverViewTabAdapter", "Loader service Connected");
            OverViewTabAdapter.this.mSavingService = ILoaderService.Stub.asInterface(service);
            try {
                OverViewTabAdapter.this.mSavingService.registerStateListener(OverViewTabAdapter.this.stateListener);
            } catch (RemoteException e) {
                Log.d("OverViewTabAdapter", "Exception happened! " + e.getMessage());
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d("OverViewTabAdapter", "Loader service DisConnected");
            try {
                OverViewTabAdapter.this.mSavingService.unregisterStateListener(OverViewTabAdapter.this.stateListener);
            } catch (RemoteException e) {
                Log.d("OverViewTabAdapter", "Exception happened! " + e.getMessage());
            }
            OverViewTabAdapter.this.mSavingService = null;
        }
    }

    class C07014 implements OnCheckedChangeListener {
        C07014() {
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d("OverViewTabAdapter", "Data Saving isChecked = " + isChecked);
            try {
                if (OverViewTabAdapter.this.mSavingService == null) {
                    return;
                }
                if (isChecked) {
                    OverViewTabAdapter.this.mSavingService.startSaving();
                } else {
                    OverViewTabAdapter.this.mSavingService.stopSaving();
                }
            } catch (RemoteException e) {
                Log.d("OverViewTabAdapter", "Exception happened! " + e.getMessage());
            }
        }
    }

    class C07025 implements OnClickListener {
        C07025() {
        }

        public void onClick(View v) {
            Log.d("OverViewTabAdapter", "launch data saving activity");
            try {
                if (OverViewTabAdapter.this.mSavingService != null) {
                    OverViewTabAdapter.this.mSavingService.launchOperaMAX();
                }
            } catch (RemoteException e) {
                Log.d("OverViewTabAdapter", "Exception happened! " + e.getMessage());
            }
        }
    }

    public OverViewTabAdapter(Context context, INetworkStatsSession statsSession) {
        Log.d("OverViewTabAdapter", "OverViewTabAdapter()");
        this.mContext = context;
        this.mStatsSession = statsSession;
        this.mBidiFormatter = BidiFormatter.getInstance();
        initPolicy();
        getMobileState();
        initData();
        this.mUiHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message inputMessage) {
                int state;
                boolean z = true;
                try {
                    state = System.getInt(OverViewTabAdapter.this.mContext.getContentResolver(), "data_saving_key");
                } catch (SettingNotFoundException e) {
                    state = 0;
                }
                if (OverViewTabAdapter.this.mDataSavingSwitch != null) {
                    Switch -get1 = OverViewTabAdapter.this.mDataSavingSwitch;
                    if (state != 1) {
                        z = false;
                    }
                    -get1.setChecked(z);
                }
            }
        };
    }

    private void getMobileState() {
        Log.d("OverViewTabAdapter", "getMobileState()");
        this.mSublist = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoList();
        this.mSimNum = this.mSublist == null ? 0 : this.mSublist.size();
        this.mMobileTotal = new long[this.mSimNum];
        this.mLimitBytes = new long[this.mSimNum];
    }

    private void getWifiData() {
        Log.d("OverViewTabAdapter", "getWifiData()");
        ChartData data = new ChartData();
        try {
            data.network = this.mStatsSession.getHistoryForNetwork(NetworkTemplate.buildTemplateWifiWildcard(), 10);
        } catch (RemoteException e) {
            Log.d("OverViewTabAdapter", "Remote Exception happens");
        }
        long historyStart = data.network.getStart();
        long historyEnd = data.network.getEnd();
        long now = System.currentTimeMillis();
        Log.d("OverViewTabAdapter", "historyStart = " + historyStart + " historyEnd = " + historyEnd + " now = " + now);
        long cycleEnd = historyEnd > now ? historyEnd : now;
        long cycleEndBak = cycleEnd;
        long cycleStart = historyStart;
        while (cycleEnd > historyStart) {
            cycleStart = cycleEnd - 2419200000L;
            if (cycleStart <= now && now <= cycleEnd) {
                Log.d("OverViewTabAdapter", "cycleStart <= now && now <= cycleEnd");
                break;
            } else {
                cycleEndBak = cycleEnd;
                cycleEnd = cycleStart;
            }
        }
        Entry entry = data.network.getValues(cycleStart, cycleEndBak, now, null);
        Log.d("OverViewTabAdapter", "cycleStart = " + cycleStart + " cycleEndBak = " + cycleEndBak);
        this.mWifiTotal = entry != null ? entry.rxBytes + entry.txBytes : 0;
        entry = data.network.getValues(getUtcDateMillis(), now, now, null);
        this.mWifiToday = entry != null ? entry.rxBytes + entry.txBytes : 0;
        Log.d("OverViewTabAdapter", "mWifiTotal = " + this.mWifiTotal + " mWifiToday = " + this.mWifiToday);
    }

    private int calcWifiTodayProgress(long todayUsage, long totalUsage) {
        double per;
        Log.d("OverViewTabAdapter", "calcWifiTodayProgress() todayUsage : " + todayUsage + " totalUsage : " + totalUsage);
        if (todayUsage == 0) {
            per = 0.0d;
        } else {
            per = ((double) todayUsage) / ((double) totalUsage);
            if (per > 0.0d && per < 0.01d) {
                per = 0.01d;
            }
        }
        int value = (int) (100.0d * per);
        Log.d("OverViewTabAdapter", "calcWifiTodayProgress() value : " + value);
        return value;
    }

    private int calcWifiTotalProgress(long todayUsage) {
        return todayUsage == 0 ? 0 : 100;
    }

    private int calcMobileProgress(long totalUsage, long limitUsage) {
        double per;
        Log.d("OverViewTabAdapter", "calcMobileProgress() totalUsage = " + totalUsage + " limitUsage = " + limitUsage);
        if (limitUsage < 0) {
            per = (double) (totalUsage == 0 ? 0 : 1);
        } else if (totalUsage <= limitUsage) {
            per = ((double) totalUsage) / ((double) limitUsage);
            Log.d("OverViewTabAdapter", "limitUsage >=  totalUsage  per = " + per);
        } else {
            per = 1.0d;
            Log.d("OverViewTabAdapter", "limitUsage < totalUsage ,so set per = 1");
        }
        if (per > 0.0d && per < 0.01d) {
            per = 0.01d;
        }
        int value = (int) (100.0d * per);
        Log.d("OverViewTabAdapter", "calcMobileProgress value " + value);
        return value;
    }

    private void getMobileData() {
        Log.d("OverViewTabAdapter", "getMobileData()");
        if (this.mSublist != null) {
            ChartData data = new ChartData();
            int count = 0;
            for (SubscriptionInfo subInfo : this.mSublist) {
                NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(TelephonyManager.from(this.mContext).getSubscriberId(subInfo.getSubscriptionId()));
                CdmaUtils.fillTemplateForCdmaLte(template, subInfo.getSubscriptionId());
                try {
                    long j;
                    data.network = this.mStatsSession.getHistoryForNetwork(template, 10);
                    this.mLimitBytes[count] = this.mPolicyEditor.getPolicyLimitBytes(template);
                    long historyStart = data.network.getStart();
                    long historyEnd = data.network.getEnd();
                    long now = System.currentTimeMillis();
                    NetworkPolicy policy = this.mPolicyEditor.getPolicy(template);
                    long cycleEnd = historyEnd;
                    long cycleEndBak = historyEnd;
                    long cycleStart = historyStart;
                    if (policy == null) {
                        while (cycleEnd > historyStart) {
                            cycleStart = cycleEnd - 2419200000L;
                            if (cycleStart <= now && now <= cycleEnd) {
                                break;
                            }
                            cycleEndBak = cycleEnd;
                            cycleEnd = cycleStart;
                        }
                    } else {
                        cycleEnd = NetworkPolicyManager.computeNextCycleBoundary(historyEnd, policy);
                        while (cycleEnd > historyStart) {
                            cycleStart = NetworkPolicyManager.computeLastCycleBoundary(cycleEnd, policy);
                            if (cycleStart <= now && now <= cycleEnd) {
                                Log.d("OverViewTabAdapter", "cycleStart <= now && now <= cycleEnd");
                                break;
                            } else {
                                cycleEndBak = cycleEnd;
                                cycleEnd = cycleStart;
                            }
                        }
                    }
                    Log.d("OverViewTabAdapter", "cycleEndBak=" + cycleEndBak + "cycleStart=" + cycleStart);
                    Entry entry = data.network.getValues(cycleStart, cycleEndBak, now, null);
                    long[] jArr = this.mMobileTotal;
                    if (entry != null) {
                        j = entry.rxBytes + entry.txBytes;
                    } else {
                        j = 0;
                    }
                    jArr[count] = j;
                    Log.d("OverViewTabAdapter", "mMobileTotal[" + count + "]=" + this.mMobileTotal[count] + "mLimitBytes" + "[" + count + "]=" + this.mLimitBytes[count]);
                    count++;
                } catch (Exception e) {
                    this.mLimitBytes[count] = -2;
                    count++;
                }
            }
        }
    }

    private void initPolicy() {
        Log.d("OverViewTabAdapter", "initPolicy()");
        if (this.mPolicyManager == null && this.mPolicyEditor == null) {
            this.mPolicyManager = NetworkPolicyManager.from(this.mContext);
            this.mPolicyEditor = new NetworkPolicyEditor(this.mPolicyManager);
            this.mPolicyEditor.read();
        }
    }

    private void initData() {
        Log.d("OverViewTabAdapter", "initData()");
        this.mGroup = new ArrayList();
        if (this.mSimNum != 0) {
            this.mGroup.add(this.mContext.getString(R.string.datausage_overview_mobile_title));
            this.mMobilePos = 0;
            this.mGroup.add(this.mBidiFormatter.unicodeWrap(this.mContext.getString(R.string.wifi_settings)));
            this.mWifiPos = 1;
            return;
        }
        this.mGroup.add(this.mBidiFormatter.unicodeWrap(this.mContext.getString(R.string.wifi_settings)));
        this.mWifiPos = 0;
    }

    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    public long getChildId(int arg0, int arg1) {
        return (long) arg1;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View view;
        TextView textTitle;
        TextView textUsage;
        ProgressBar progress;
        if (groupPosition == this.mWifiPos) {
            view = LayoutInflater.from(this.mContext).inflate(R.layout.data_usage_exp_child_wifi, null);
            textTitle = (TextView) view.findViewById(R.id.wifi_title);
            textUsage = (TextView) view.findViewById(R.id.wifi_usage);
            progress = (ProgressBar) view.findViewById(R.id.wifi_progressbar);
            getWifiData();
            String usage;
            if (childPosition == 0) {
                textTitle.setText(this.mContext.getString(R.string.datausage_overview_wifi_today));
                usage = Formatter.formatFileSize(this.mContext, this.mWifiToday);
                Log.d("OverViewTabAdapter", "childPosition=" + childPosition + " and usage= " + usage);
                textUsage.setText(usage);
                progress.setSecondaryProgress(calcWifiTodayProgress(this.mWifiToday, this.mWifiTotal));
                return view;
            }
            textTitle.setText(this.mContext.getString(R.string.datausage_overview_wifi_total));
            usage = Formatter.formatFileSize(this.mContext, this.mWifiTotal);
            Log.d("OverViewTabAdapter", "childPosition = " + childPosition + " and usage= " + usage);
            textUsage.setText(usage);
            progress.setSecondaryProgress(calcWifiTotalProgress(this.mWifiTotal));
            return view;
        }
        if (groupPosition != this.mMobilePos) {
            return null;
        }
        LayoutInflater inflate = LayoutInflater.from(this.mContext);
        int childCount = getChildrenCount(groupPosition);
        Log.d("OverViewTabAdapter", "childCount: " + childCount + ", mSimSum: " + this.mSimNum);
        if (childCount == this.mSimNum + 1 && childPosition == childCount - 1) {
            view = inflate.inflate(R.layout.data_saving_screen, null);
            this.mDataSavingZone = (LinearLayout) view.findViewById(R.id.data_saving_zone);
            inflateLockScreenView(inflate);
            return view;
        }
        getMobileData();
        if (this.mSublist == null) {
            return null;
        }
        int[] tintArr = this.mContext.getResources().getIntArray(17235978);
        int simColor = ((SubscriptionInfo) this.mSublist.get(childPosition)).getIconTint();
        Arrays.sort(tintArr);
        int index = Arrays.binarySearch(tintArr, simColor);
        Log.d("OverViewTabAdapter", "usage : " + this.mMobileTotal[childPosition] + " limit : " + this.mLimitBytes[childPosition] + ", index: " + index);
        for (int i : tintArr) {
            Log.d("OverViewTabAdapter", "i: " + i);
        }
        Log.d("OverViewTabAdapter", ",simColor: " + simColor);
        if (this.mLimitBytes[childPosition] >= 0 && this.mMobileTotal[childPosition] > this.mLimitBytes[childPosition]) {
            index = 5;
        }
        switch (index) {
            case 0:
                view = inflate.inflate(R.layout.data_usage_exp_child_mobile_color_teal, null);
                break;
            case 1:
                view = inflate.inflate(R.layout.data_usage_exp_child_mobile_color_blue, null);
                break;
            case 2:
                view = inflate.inflate(R.layout.data_usage_exp_child_mobile_color_indigo, null);
                break;
            case 3:
                view = inflate.inflate(R.layout.data_usage_exp_child_mobile_color_purple, null);
                break;
            case 4:
                view = inflate.inflate(R.layout.data_usage_exp_child_mobile_color_pink, null);
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                view = inflate.inflate(R.layout.data_usage_exp_child_mobile_color_red, null);
                break;
            default:
                view = inflate.inflate(R.layout.data_usage_exp_child_mobile_color_teal, null);
                break;
        }
        textTitle = (TextView) view.findViewById(R.id.mobile_title);
        textUsage = (TextView) view.findViewById(R.id.mobile_usage);
        int progressBarColor = ((SubscriptionInfo) this.mSublist.get(childPosition)).getIconTint();
        progress = (ProgressBar) view.findViewById(R.id.mobile_progressbar);
        textTitle.setText(((SubscriptionInfo) this.mSublist.get(childPosition)).getDisplayName());
        if (this.mLimitBytes[childPosition] <= -1) {
            textUsage.setText(Formatter.formatFileSize(this.mContext, this.mMobileTotal[childPosition]));
        } else if (this.mLimitBytes[childPosition] < 0 || this.mMobileTotal[childPosition] <= this.mLimitBytes[childPosition]) {
            r18 = this.mContext;
            r19 = new Object[2];
            r19[0] = Formatter.formatFileSize(this.mContext, this.mMobileTotal[childPosition]);
            r19[1] = Formatter.formatFileSize(this.mContext, this.mLimitBytes[childPosition]);
            textUsage.setText(r18.getString(R.string.datausage_overview_mobile_usage, r19));
        } else {
            Log.d("OverViewTabAdapter", "Usage bytes is bigger than the limit bytes , show warning");
            r18 = this.mContext;
            r19 = new Object[1];
            r19[0] = Formatter.formatFileSize(this.mContext, this.mMobileTotal[childPosition] - this.mLimitBytes[childPosition]);
            textUsage.setText(r18.getString(R.string.datausage_overview_mobile_usage_warning, r19));
        }
        progress.setProgress(calcMobileProgress(this.mMobileTotal[childPosition], this.mLimitBytes[childPosition]));
        return view;
    }

    public int getChildrenCount(int groupPosition) {
        if (groupPosition == this.mWifiPos) {
            return 2;
        }
        if (groupPosition != this.mMobilePos) {
            return 0;
        }
        if (!UtilsExt.isPackageExist(this.mContext, "com.opera.max.loader")) {
            return this.mSimNum;
        }
        Log.d("OverViewTabAdapter", "add Data Saving item");
        return this.mSimNum + 1;
    }

    public Object getGroup(int arg0) {
        return null;
    }

    public int getGroupCount() {
        return this.mGroup.size();
    }

    public long getGroupId(int arg0) {
        return (long) arg0;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        TextView item;
        if (convertView == null || !(convertView instanceof TextView)) {
            item = (TextView) LayoutInflater.from(this.mContext).inflate(R.layout.list_group_header, null);
        } else {
            item = (TextView) convertView;
        }
        item.setText((CharSequence) this.mGroup.get(groupPosition));
        return item;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable(int arg0, int arg1) {
        return false;
    }

    public long getUtcDateMillis() {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
        int year = Integer.valueOf(date.substring(0, 4)).intValue();
        int month = Integer.valueOf(date.substring(4, 6)).intValue();
        int day = Integer.valueOf(date.substring(6, 8)).intValue();
        Calendar gc = Calendar.getInstance(TimeZone.getDefault());
        gc.set(year, month - 1, day, 0, 0, 0);
        return (gc.getTimeInMillis() / 1000) * 1000;
    }

    public void updateAdapter() {
        Log.d("OverViewTabAdapter", "updateAdapter()");
        if (this.mPolicyEditor != null) {
            this.mPolicyEditor.read();
        }
        getMobileState();
        initData();
    }

    private void inflateLockScreenView(LayoutInflater inflater) {
        boolean z = true;
        if (this.mDataSavingZone != null) {
            int state;
            this.mDataSavingSwitch = new Switch(inflater.getContext());
            this.mDataSavingView = inflatePreference(inflater, this.mDataSavingZone, this.mDataSavingSwitch);
            this.mDataSavingView.setClickable(true);
            this.mDataSavingView.setFocusable(true);
            try {
                state = System.getInt(this.mContext.getContentResolver(), "data_saving_key");
                Log.d("OverViewTabAdapter", "get state from provider = " + state);
            } catch (SettingNotFoundException e) {
                state = 0;
                Log.d("OverViewTabAdapter", "Get data from provider failure");
            }
            Switch switchR = this.mDataSavingSwitch;
            if (state != 1) {
                z = false;
            }
            switchR.setChecked(z);
            this.mDataSavingSwitch.setOnCheckedChangeListener(new C07014());
            ((TextView) this.mDataSavingView.findViewById(16908310)).setText(R.string.data_saving_title);
            this.mDataSavingZone.addView(this.mDataSavingView);
            this.mDataSavingView.setOnClickListener(new C07025());
        }
    }

    private static View inflatePreference(LayoutInflater inflater, ViewGroup root, View widget) {
        View view = inflater.inflate(R.layout.preference, root, false);
        ((LinearLayout) view.findViewById(16908312)).addView(widget, new LayoutParams(-2, -2));
        return view;
    }

    public void bindOperaMaxLoader() {
        if (UtilsExt.isPackageExist(this.mContext, "com.opera.max.loader")) {
            Log.d("OverViewTabAdapter", "bindOperaMaxLoader");
            Intent intent = new Intent();
            intent.setClassName("com.opera.max.loader", "com.opera.max.loader.LoaderService");
            this.mContext.startService(intent);
            this.mContext.bindService(intent, this.serviceConnection, 1);
        }
    }

    public void unbindOperaMaxLoader() {
        if (this.mSavingService != null) {
            Log.d("OverViewTabAdapter", "unbindOperaMaxLoader");
            try {
                this.mSavingService.unregisterStateListener(this.stateListener);
            } catch (RemoteException e) {
                Log.d("OverViewTabAdapter", "unregisterStateListener error");
            }
            this.mContext.unbindService(this.serviceConnection);
        }
    }
}
