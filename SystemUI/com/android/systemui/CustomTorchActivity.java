package com.android.systemui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.FlashlightController.FlashlightListener;

public class CustomTorchActivity extends Activity implements FlashlightListener, OnClickListener {
    private boolean isFlashRun = false;
    private LinearLayout linearLayout;
    private FlashlightController mFlashlightController;
    private ImageButton mTorchOnOff;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            CustomTorchActivity.this.isFlashRun = false;
            CustomTorchActivity.this.stopFlash();
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("fenghaitao", "==CustomTorchActivity===onCreate====");
        setContentView(R.layout.torch_activity_custom);
        getWindow().addFlags(2621440);
        initView();
        this.isFlashRun = true;
        registerReceiver(this.receiver, new IntentFilter("com.android.vanzo.action.power"));
    }

    private void initView() {
        this.mTorchOnOff = (ImageButton) findViewById(R.id.bt_torch);
        this.linearLayout = (LinearLayout) findViewById(R.id.cutom_torch);
        this.linearLayout.setBackgroundResource(R.drawable.bulb_off);
        this.mTorchOnOff.setBackgroundResource(R.drawable.bulb_off_button);
        if (this.mTorchOnOff != null) {
            this.mTorchOnOff.setOnClickListener(this);
        }
        this.mFlashlightController = new FlashlightController(getApplicationContext());
        this.mFlashlightController.addListener(this);
    }

    private void stopFlash() {
        Log.i("fenghaitao", "====电源按键===");
        this.mFlashlightController.setFlashlight(false);
        this.mFlashlightController.removeListener(this);
        unregisterReceiver(this.receiver);
        finish();
    }

    private void refreshState() {
        if (this.mFlashlightController.isEnabled()) {
            this.linearLayout.setBackgroundResource(R.drawable.bulb_on);
            this.mTorchOnOff.setBackgroundResource(R.drawable.bulb_on_button);
            return;
        }
        this.linearLayout.setBackgroundResource(R.drawable.bulb_off);
        this.mTorchOnOff.setBackgroundResource(R.drawable.bulb_off_button);
    }

    protected void onResume() {
        super.onResume();
        Log.i("fenghaitao", "==onResume===");
        refreshState();
    }

    protected void onStop() {
        super.onStop();
        if (this.receiver == null) {
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        this.isFlashRun = false;
        Log.i("fenghaitao", "==onDestroy==");
        this.mFlashlightController.removeListener(this);
    }

    public void onFlashlightChanged(boolean enabled) {
        Log.i("fenghaitao", "==onFlashlightChanged===");
        final boolean enable = enabled;
        runOnUiThread(new Runnable() {
            public void run() {
                if (CustomTorchActivity.this.isFlashRun) {
                    CustomTorchActivity.this.refreshState();
                }
                if (!enable) {
                    CustomTorchActivity.this.finish();
                }
            }
        });
    }

    public void onFlashlightError() {
    }

    public void onFlashlightAvailabilityChanged(boolean available) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 26) {
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_torch:
                Log.i("fenghaitao", "===onclick===" + this.mFlashlightController.isEnabled());
                if (this.mFlashlightController.isEnabled()) {
                    this.mFlashlightController.setFlashlight(false);
                    this.linearLayout.setBackgroundResource(R.drawable.bulb_off);
                    this.mTorchOnOff.setBackgroundResource(R.drawable.bulb_off_button);
                    return;
                }
                this.mFlashlightController.setFlashlight(true);
                this.linearLayout.setBackgroundResource(R.drawable.bulb_on);
                this.mTorchOnOff.setBackgroundResource(R.drawable.bulb_on_button);
                return;
            default:
                return;
        }
    }
}
