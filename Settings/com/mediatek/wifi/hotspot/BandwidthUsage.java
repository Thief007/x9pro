package com.mediatek.wifi.hotspot;

import android.animation.LayoutTransition;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.mediatek.widget.ChartBandwidthUsageView;
import com.mediatek.widget.ChartBandwidthUsageView.BandwidthChartListener;
import java.net.NetworkInterface;
import java.net.SocketException;

public class BandwidthUsage extends Fragment {
    private static final int EVENT_TICK = 1;
    private static final long GB_IN_BYTES = 1073741824;
    private static final String IFACE = "ap0";
    private static final long KB_IN_BYTES = 1024;
    private static final int LIMIT_MAX_SIZE = 10;
    private static final long MB_IN_BYTES = 1048576;
    private static final String NETWORK_INFO = "network_info";
    private static final String NETWORK_LIMIT = "network_limit";
    private static final String TAG = "BandwidthUsage";
    private static final String TAG_LIMIT_EDITOR = "limitEditor";
    private ChartBandwidthUsageView mChart;
    private BandwidthChartListener mChartListener = new C07734();
    private IConnectivityManager mConnManager;
    private CheckBox mEnableThrottling;
    private View mEnableThrottlingView;
    private OnGlobalLayoutListener mFirstLayoutListener = new C07723();
    private Handler mHandler = new C07701();
    private IntentFilter mIntentFilter;
    private INetworkManagementService mNetworkService;
    private OnClickListener mOnEnableCheckBoxClick = new C07745();
    private final BroadcastReceiver mReceiver = new C07712();
    private long mStartTime = 0;
    private View mTotalDataView;
    private View mTotalTimeView;

    class C07701 extends Handler {
        C07701() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BandwidthUsage.this.updateBandwidthUsage();
                    sendEmptyMessageDelayed(1, 1000);
                    return;
                default:
                    return;
            }
        }
    }

    class C07712 extends BroadcastReceiver {
        C07712() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                BandwidthUsage.this.mHandler.removeMessages(1);
                if (System.getInt(BandwidthUsage.this.getActivity().getContentResolver(), "airplane_mode_on", 0) != 0) {
                    BandwidthUsage.this.mEnableThrottlingView.setEnabled(false);
                    BandwidthUsage.this.getActivity().onBackPressed();
                }
            } else if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action) && intent.getIntExtra("wifi_state", 14) != 13) {
                BandwidthUsage.this.getActivity().finish();
            }
        }
    }

    class C07723 implements OnGlobalLayoutListener {
        C07723() {
        }

        public void onGlobalLayout() {
            LayoutTransition chartTransition = BandwidthUsage.this.buildLayoutTransition();
            chartTransition.setStartDelay(2, 0);
            chartTransition.setStartDelay(3, 0);
            chartTransition.setAnimator(2, null);
            chartTransition.setAnimator(3, null);
            BandwidthUsage.this.mChart.setLayoutTransition(chartTransition);
        }
    }

    class C07734 implements BandwidthChartListener {
        C07734() {
        }

        public void onLimitChanging() {
            BandwidthUsage.this.mHandler.removeMessages(1);
        }

        public void onLimitChanged() {
            BandwidthUsage.this.setLimitData(true);
            BandwidthUsage.this.mHandler.sendEmptyMessageDelayed(1, 1000);
        }

        public void requestLimitEdit() {
            LimitEditorFragment.show(BandwidthUsage.this, BandwidthUsage.this.mChart.getLimitBytes());
        }
    }

    class C07745 implements OnClickListener {
        C07745() {
        }

        public void onClick(View v) {
            if (BandwidthUsage.this.mEnableThrottling.isChecked()) {
                BandwidthUsage.this.mEnableThrottling.setChecked(false);
                BandwidthUsage.this.setThrottleEnabled(false);
                BandwidthUsage.this.mChart.setLimitState(false);
                BandwidthUsage.this.setLimitData(false);
                return;
            }
            BandwidthUsage.this.mEnableThrottling.setChecked(true);
            BandwidthUsage.this.setThrottleEnabled(true);
            BandwidthUsage.this.mChart.setLimitState(true);
            long value = BandwidthUsage.this.getActivity().getSharedPreferences(BandwidthUsage.NETWORK_INFO, 1).getLong(BandwidthUsage.NETWORK_LIMIT, 1);
            Log.d("@M_BandwidthUsage", "init limit value=" + value);
            BandwidthUsage.this.mChart.setLimitBytes(value);
            BandwidthUsage.this.setLimitData(true);
        }
    }

    public static class LimitEditorFragment extends DialogFragment {
        private static final String KEY_LIMIT_BYTES = "limit_bytes";

        public static void show(BandwidthUsage parent, long limitBytes) {
            LimitEditorFragment dialog = new LimitEditorFragment();
            Bundle bundle = new Bundle();
            bundle.putLong(KEY_LIMIT_BYTES, limitBytes);
            dialog.setArguments(bundle);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), BandwidthUsage.TAG_LIMIT_EDITOR);
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final BandwidthUsage target = (BandwidthUsage) getTargetFragment();
            Builder builder = new Builder(getActivity());
            View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.band_width_usage_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);
            long limitBytes = getArguments().getLong(KEY_LIMIT_BYTES, 0);
            bytesPicker.setMaxValue(BandwidthUsage.LIMIT_MAX_SIZE);
            bytesPicker.setMinValue(0);
            bytesPicker.setValue((int) (limitBytes / BandwidthUsage.MB_IN_BYTES));
            bytesPicker.setWrapSelectorWheel(false);
            ((TextView) view.findViewById(R.id.text)).setText(R.string.wifi_ap_bandwidth_megabyteShort);
            builder.setTitle(R.string.data_usage_limit_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bytesPicker.clearFocus();
                    long bytes = ((long) bytesPicker.getValue()) * BandwidthUsage.MB_IN_BYTES;
                    target.mChart.setLimitBytes(bytes);
                    Log.d("@M_BandwidthUsage", "set Limit Bytes=" + bytes);
                    target.mChart.focusSweepLimit();
                    target.mChart.updateVertAxisBounds(null);
                    target.setLimitData(true);
                }
            });
            return builder.create();
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mNetworkService = Stub.asInterface(ServiceManager.getService("network_management"));
        this.mConnManager = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("@M_BandwidthUsage", "onCreateView");
        Context context = inflater.getContext();
        View view = inflater.inflate(R.layout.wifi_ap_bandwidth_usage, container, false);
        LinearLayout enableLayout = (LinearLayout) view.findViewById(R.id.enable_throttling);
        this.mEnableThrottling = new CheckBox(inflater.getContext());
        this.mEnableThrottling.setClickable(false);
        this.mEnableThrottling.setFocusable(false);
        this.mEnableThrottlingView = inflatePreference(inflater, enableLayout, this.mEnableThrottling);
        this.mEnableThrottlingView.setClickable(true);
        this.mEnableThrottlingView.setFocusable(true);
        this.mEnableThrottlingView.setOnClickListener(this.mOnEnableCheckBoxClick);
        enableLayout.addView(this.mEnableThrottlingView);
        setPreferenceTitle(this.mEnableThrottlingView, R.string.wifi_ap_bandwidth_enable);
        this.mChart = (ChartBandwidthUsageView) view.findViewById(R.id.chart);
        this.mChart.setListener(this.mChartListener);
        LinearLayout timeLayout = (LinearLayout) view.findViewById(R.id.time);
        this.mTotalTimeView = inflater.inflate(R.layout.preference, timeLayout, false);
        this.mTotalTimeView.setClickable(false);
        this.mTotalTimeView.setFocusable(false);
        timeLayout.addView(this.mTotalTimeView);
        LinearLayout dataLayout = (LinearLayout) view.findViewById(R.id.data);
        this.mTotalDataView = inflater.inflate(R.layout.preference, dataLayout, false);
        this.mTotalDataView.setClickable(false);
        this.mTotalDataView.setFocusable(false);
        dataLayout.addView(this.mTotalDataView);
        return view;
    }

    public void onResume() {
        Log.d("@M_BandwidthUsage", "onResume");
        super.onResume();
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
        if (System.getInt(getActivity().getContentResolver(), "airplane_mode_on", 0) != 0) {
            getActivity().onBackPressed();
        }
        this.mHandler.sendEmptyMessageDelayed(1, 1000);
        boolean enable = Secure.getInt(getActivity().getContentResolver(), "interface_throttle_enable", 0) == 1;
        Log.d("@M_BandwidthUsage", "onResume,getInterfaceRxThrottle=" + enable);
        this.mEnableThrottling.setChecked(enable);
        this.mChart.setLimitState(enable);
        long value = getActivity().getSharedPreferences(NETWORK_INFO, 1).getLong(NETWORK_LIMIT, 0);
        Log.d("@M_BandwidthUsage", "init limit value=" + value);
        this.mChart.setLimitBytes(value);
        this.mChart.updateVertAxisBounds(null);
        this.mStartTime = System.getLong(getActivity().getContentResolver(), "wifi_hotspot_start_time", 0);
        Log.d("@M_BandwidthUsage", "mStartTime:" + this.mStartTime);
        refreshTimeAndData();
    }

    public void onPause() {
        Log.d("@M_BandwidthUsage", "onPause");
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
        this.mHandler.removeMessages(1);
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mEnableThrottlingView = null;
    }

    public void onDestroy() {
        Log.d("@M_BandwidthUsage", "onDestory");
        getActivity().setRequestedOrientation(4);
        super.onDestroy();
    }

    private void setLimitData(boolean enable) {
        try {
            NetworkInterface ni = NetworkInterface.getByName(IFACE);
            if (ni == null || !ni.isUp()) {
                Log.d("@M_BandwidthUsage", "Network interface has been removed, setLimitData() return");
                return;
            }
            if (enable) {
                try {
                    long limit = this.mChart.getLimitBytes();
                    int rxBytes = limit == 0 ? 1 : ((int) ((8 * limit) * 2)) / 3072;
                    int txBytes = limit == 0 ? 1 : ((int) (8 * limit)) / 3072;
                    Log.d("@M_BandwidthUsage", "setLimitData,setInterfaceThrottle,rxBytes=" + rxBytes + ",txBytes=" + txBytes);
                    this.mNetworkService.setInterfaceThrottle(IFACE, rxBytes, txBytes);
                    Editor editor = getActivity().getSharedPreferences(NETWORK_INFO, 2).edit();
                    String str = NETWORK_LIMIT;
                    if (limit == 0) {
                        limit = 1;
                    }
                    editor.putLong(str, limit);
                    editor.commit();
                } catch (RemoteException e) {
                    Log.d("@M_BandwidthUsage", " RemoteException happens when setInterfaceRxThrottle");
                }
            } else {
                this.mNetworkService.setInterfaceThrottle(IFACE, -1, -1);
            }
        } catch (SocketException e2) {
            Log.d("@M_BandwidthUsage", "SocketException happens when getNetworkInterface return");
        }
    }

    private LayoutTransition buildLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setAnimateParentHierarchy(false);
        return transition;
    }

    private View inflatePreference(LayoutInflater inflater, ViewGroup root, View widget) {
        View view = inflater.inflate(R.layout.preference, root, false);
        ((LinearLayout) view.findViewById(16908312)).addView(widget, new LayoutParams(-2, -2));
        return view;
    }

    private void setPreferenceTitle(View parent, int resId) {
        ((TextView) parent.findViewById(16908310)).setText(resId);
    }

    private void setPreferenceTitle(View parent, int resId, String data) {
        ((TextView) parent.findViewById(16908310)).setText(getActivity().getString(resId, new Object[]{data}));
    }

    private void refreshTimeAndData() {
        String unit;
        long usedTime = 0;
        if (this.mStartTime != 0) {
            usedTime = System.currentTimeMillis() - this.mStartTime;
        }
        if (usedTime < 0) {
            System.putLong(getActivity().getContentResolver(), "wifi_hotspot_start_time", System.currentTimeMillis());
            this.mStartTime = System.currentTimeMillis();
            usedTime = 0;
        }
        setPreferenceTitle(this.mTotalTimeView, R.string.wifi_ap_time_duration, " " + Utils.formatElapsedTime(getActivity(), (double) usedTime, true));
        long totalData = this.mChart.getTotalUsedData();
        if (totalData < MB_IN_BYTES) {
            totalData /= KB_IN_BYTES;
            unit = " KB";
        } else if (totalData < GB_IN_BYTES) {
            totalData /= MB_IN_BYTES;
            unit = " M";
        } else {
            totalData /= GB_IN_BYTES;
            unit = " G";
        }
        setPreferenceTitle(this.mTotalDataView, R.string.wifi_ap_total_data, " " + String.valueOf(totalData) + unit);
    }

    private void updateBandwidthUsage() {
        try {
            this.mChart.setNetworkStates(this.mNetworkService.getNetworkStatsTethering());
            refreshTimeAndData();
        } catch (RemoteException e) {
            Log.d("@M_BandwidthUsage", "RemoteException happens");
        }
    }

    private void setThrottleEnabled(boolean enable) {
        Log.d("@M_BandwidthUsage", "setThrottleEnabled:" + enable);
        Secure.putInt(getActivity().getContentResolver(), "interface_throttle_enable", enable ? 1 : 0);
    }
}
