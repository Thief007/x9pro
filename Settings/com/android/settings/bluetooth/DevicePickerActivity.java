package com.android.settings.bluetooth;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;

public final class DevicePickerActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_device_picker);
    }

    public void onBackPressed() {
        setResult(0);
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            Log.w("DevicePickerActivity", "IllegalStateException");
        }
    }
}
