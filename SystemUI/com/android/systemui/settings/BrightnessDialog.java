package com.android.systemui.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;

public class BrightnessDialog extends Activity {
    private BrightnessController mBrightnessController;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setGravity(48);
        window.clearFlags(2);
        window.requestFeature(1);
        setContentView(R.layout.quick_settings_brightness_dialog);
        this.mBrightnessController = new BrightnessController(this, (ImageView) findViewById(R.id.brightness_icon), (ToggleSlider) findViewById(R.id.brightness_slider));
    }

    protected void onStart() {
        super.onStart();
        this.mBrightnessController.registerCallbacks();
        MetricsLogger.visible(this, 220);
    }

    protected void onStop() {
        super.onStop();
        MetricsLogger.hidden(this, 220);
        this.mBrightnessController.unregisterCallbacks();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!(keyCode == 25 || keyCode == 24)) {
            if (keyCode == 164) {
            }
            return super.onKeyDown(keyCode, event);
        }
        finish();
        return super.onKeyDown(keyCode, event);
    }
}
