package com.android.settings;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.widget.LockPatternUtils;
import java.util.Locale;

public class CryptKeeperConfirm extends InstrumentedFragment {
    private View mContentView;
    private Context mContext;
    private Button mFinalButton;
    private OnClickListener mFinalClickListener = new C00871();

    class C00871 implements OnClickListener {
        C00871() {
        }

        public void onClick(View v) {
            boolean z = true;
            if (!Utils.isMonkeyRunning()) {
                if (CryptKeeperConfirm.this.mContext != null) {
                    CryptKeeperConfirm.this.mContext.sendBroadcast(new Intent("notify.deskclock.reset.alarms"), null);
                }
                LockPatternUtils utils = new LockPatternUtils(CryptKeeperConfirm.this.getActivity());
                utils.setVisiblePatternEnabled(utils.isVisiblePatternEnabled(0), 0);
                if (utils.isOwnerInfoEnabled(0)) {
                    utils.setOwnerInfo(utils.getOwnerInfo(0), 0);
                }
                if (System.getInt(CryptKeeperConfirm.this.getContext().getContentResolver(), "show_password", 1) == 0) {
                    z = false;
                }
                utils.setVisiblePasswordEnabled(z, 0);
                Intent intent = new Intent(CryptKeeperConfirm.this.getActivity(), Blank.class);
                intent.putExtras(CryptKeeperConfirm.this.getArguments());
                CryptKeeperConfirm.this.startActivity(intent);
                try {
                    Stub.asInterface(ServiceManager.getService("mount")).setField("SystemLocale", Locale.getDefault().toLanguageTag());
                } catch (Exception e) {
                    Log.e("CryptKeeperConfirm", "Error storing locale for decryption UI", e);
                }
            }
        }
    }

    public static class Blank extends Activity {
        private Handler mHandler = new Handler();

        class C00881 implements Runnable {
            C00881() {
            }

            public void run() {
                IBinder service = ServiceManager.getService("mount");
                if (service == null) {
                    Log.e("CryptKeeper", "Failed to find the mount service");
                    Blank.this.finish();
                    return;
                }
                IMountService mountService = Stub.asInterface(service);
                try {
                    Bundle args = Blank.this.getIntent().getExtras();
                    mountService.encryptStorage(args.getInt("type", -1), args.getString("password"));
                } catch (Exception e) {
                    Log.e("CryptKeeper", "Error while encrypting...", e);
                }
            }
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.crypt_keeper_blank);
            if (Utils.isMonkeyRunning()) {
                finish();
            }
            ((StatusBarManager) getSystemService("statusbar")).disable(58130432);
            this.mHandler.postDelayed(new C00881(), 3000);
        }
    }

    protected int getMetricsCategory() {
        return 33;
    }

    private void establishFinalConfirmationState() {
        this.mFinalButton = (Button) this.mContentView.findViewById(R.id.execute_encrypt);
        this.mFinalButton.setOnClickListener(this.mFinalClickListener);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mContext = getActivity().getApplicationContext();
        this.mContentView = inflater.inflate(R.layout.crypt_keeper_confirm, null);
        establishFinalConfirmationState();
        return this.mContentView;
    }
}
