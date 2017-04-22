package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WpsCallback;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.setupwizardlib.R$styleable;
import java.util.Timer;
import java.util.TimerTask;

public class WpsDialog extends AlertDialog {
    private Button mButton;
    private Context mContext;
    DialogState mDialogState = DialogState.WPS_INIT;
    private final IntentFilter mFilter;
    private Handler mHandler = new Handler();
    private String mMsgString = "";
    private ProgressBar mProgressBar;
    private BroadcastReceiver mReceiver;
    private TextView mTextView;
    private ProgressBar mTimeoutBar;
    private Timer mTimer;
    private View mView;
    private WifiManager mWifiManager;
    private WpsCallback mWpsListener;
    private int mWpsSetup;

    class C06291 extends BroadcastReceiver {
        C06291() {
        }

        public void onReceive(Context context, Intent intent) {
            WpsDialog.this.handleEvent(context, intent);
        }
    }

    class C06302 implements OnClickListener {
        C06302() {
        }

        public void onClick(View v) {
            WpsDialog.this.dismiss();
        }
    }

    class C06323 extends TimerTask {

        class C06311 implements Runnable {
            C06311() {
            }

            public void run() {
                WpsDialog.this.mTimeoutBar.incrementProgressBy(1);
            }
        }

        C06323() {
        }

        public void run() {
            WpsDialog.this.mHandler.post(new C06311());
        }
    }

    private enum DialogState {
        WPS_INIT,
        WPS_START,
        WPS_COMPLETE,
        CONNECTED,
        WPS_FAILED
    }

    public WpsDialog(Context context, int wpsSetup) {
        super(context);
        this.mContext = context;
        this.mWpsSetup = wpsSetup;
        this.mWpsListener = new WpsCallback() {
            public void onStarted(String pin) {
                if (pin != null) {
                    WpsDialog.this.updateDialog(DialogState.WPS_START, String.format(WpsDialog.this.mContext.getString(R.string.wifi_wps_onstart_pin), new Object[]{pin}));
                    return;
                }
                WpsDialog.this.updateDialog(DialogState.WPS_START, WpsDialog.this.mContext.getString(R.string.wifi_wps_onstart_pbc));
            }

            public void onSucceeded() {
                WpsDialog.this.updateDialog(DialogState.WPS_COMPLETE, WpsDialog.this.mContext.getString(R.string.wifi_wps_complete));
            }

            public void onFailed(int reason) {
                String msg;
                switch (reason) {
                    case 1:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_in_progress);
                        break;
                    case 3:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_overlap);
                        break;
                    case 4:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_wep);
                        break;
                    case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_tkip);
                        break;
                    default:
                        msg = WpsDialog.this.mContext.getString(R.string.wifi_wps_failed_generic);
                        break;
                }
                WpsDialog.this.updateDialog(DialogState.WPS_FAILED, msg);
            }
        };
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mReceiver = new C06291();
        setCanceledOnTouchOutside(false);
    }

    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();
        bundle.putString("android:dialogState", this.mDialogState.toString());
        bundle.putString("android:dialogMsg", this.mMsgString.toString());
        return bundle;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            super.onRestoreInstanceState(savedInstanceState);
            DialogState dialogState = this.mDialogState;
            updateDialog(DialogState.valueOf(savedInstanceState.getString("android:dialogState")), savedInstanceState.getString("android:dialogMsg"));
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.d("WpsDialog", "onCreate");
        this.mView = getLayoutInflater().inflate(R.layout.wifi_wps_dialog, null);
        this.mTextView = (TextView) this.mView.findViewById(R.id.wps_dialog_txt);
        this.mTextView.setText(R.string.wifi_wps_setup_msg);
        this.mTimeoutBar = (ProgressBar) this.mView.findViewById(R.id.wps_timeout_bar);
        this.mTimeoutBar.setMax(120);
        this.mTimeoutBar.setProgress(0);
        this.mProgressBar = (ProgressBar) this.mView.findViewById(R.id.wps_progress_bar);
        this.mProgressBar.setVisibility(8);
        this.mButton = (Button) this.mView.findViewById(R.id.wps_dialog_btn);
        this.mButton.setText(R.string.wifi_cancel);
        this.mButton.setOnClickListener(new C06302());
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        setView(this.mView);
        if (savedInstanceState == null) {
            WpsInfo wpsConfig = new WpsInfo();
            wpsConfig.setup = this.mWpsSetup;
            this.mWifiManager.startWps(wpsConfig, this.mWpsListener);
        }
        super.onCreate(savedInstanceState);
    }

    protected void onStart() {
        Log.d("WpsDialog", "onStart");
        this.mTimer = new Timer(false);
        this.mTimer.schedule(new C06323(), 1000, 1000);
        this.mContext.registerReceiver(this.mReceiver, this.mFilter);
    }

    protected void onStop() {
        Log.d("WpsDialog", "onStop");
        if (this.mDialogState != DialogState.WPS_COMPLETE) {
            this.mWifiManager.cancelWps(null);
        }
        if (this.mReceiver != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
        if (this.mTimer != null) {
            this.mTimer.cancel();
        }
    }

    private void updateDialog(final DialogState state, final String msg) {
        if (this.mDialogState.ordinal() < state.ordinal()) {
            this.mDialogState = state;
            this.mMsgString = msg;
            this.mHandler.post(new Runnable() {
                private static /* synthetic */ int[] -com_android_settings_wifi_WpsDialog$DialogStateSwitchesValues;

                private static /* synthetic */ int[] m9x6323e566() {
                    if (-com_android_settings_wifi_WpsDialog$DialogStateSwitchesValues != null) {
                        return -com_android_settings_wifi_WpsDialog$DialogStateSwitchesValues;
                    }
                    int[] iArr = new int[DialogState.values().length];
                    try {
                        iArr[DialogState.CONNECTED.ordinal()] = 1;
                    } catch (NoSuchFieldError e) {
                    }
                    try {
                        iArr[DialogState.WPS_COMPLETE.ordinal()] = 2;
                    } catch (NoSuchFieldError e2) {
                    }
                    try {
                        iArr[DialogState.WPS_FAILED.ordinal()] = 3;
                    } catch (NoSuchFieldError e3) {
                    }
                    try {
                        iArr[DialogState.WPS_INIT.ordinal()] = 4;
                    } catch (NoSuchFieldError e4) {
                    }
                    try {
                        iArr[DialogState.WPS_START.ordinal()] = 5;
                    } catch (NoSuchFieldError e5) {
                    }
                    -com_android_settings_wifi_WpsDialog$DialogStateSwitchesValues = iArr;
                    return iArr;
                }

                public void run() {
                    switch (C06334.m9x6323e566()[state.ordinal()]) {
                        case 1:
                        case 3:
                            WpsDialog.this.mButton.setText(WpsDialog.this.mContext.getString(R.string.dlg_ok));
                            WpsDialog.this.mTimeoutBar.setVisibility(8);
                            WpsDialog.this.mProgressBar.setVisibility(8);
                            if (WpsDialog.this.mReceiver != null) {
                                WpsDialog.this.mContext.unregisterReceiver(WpsDialog.this.mReceiver);
                                WpsDialog.this.mReceiver = null;
                                break;
                            }
                            break;
                        case 2:
                            WpsDialog.this.mTimeoutBar.setVisibility(8);
                            WpsDialog.this.mProgressBar.setVisibility(0);
                            break;
                    }
                    WpsDialog.this.mTextView.setText(msg);
                }
            });
        }
    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.net.wifi.STATE_CHANGE".equals(action)) {
            if (((NetworkInfo) intent.getParcelableExtra("networkInfo")).getDetailedState() == DetailedState.CONNECTED && this.mDialogState == DialogState.WPS_COMPLETE && this.mWifiManager.getConnectionInfo() != null) {
                updateDialog(DialogState.CONNECTED, String.format(this.mContext.getString(R.string.wifi_wps_connected), new Object[]{wifiInfo.getSSID()}));
            }
        } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action) && intent.getIntExtra("wifi_state", 4) == 1) {
            Log.d("WpsDialog", "handleEvent, wifi disabled");
            dismiss();
        }
    }
}
