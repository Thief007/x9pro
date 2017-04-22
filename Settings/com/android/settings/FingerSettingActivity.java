package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import com.android.settings.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.fingerprint.FingerprintSettings;
import java.util.List;

public class FingerSettingActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        String clazz;
        super.onCreate(savedInstanceState);
        FingerprintManager fpm = (FingerprintManager) getSystemService("fingerprint");
        Intent intent = new Intent();
        List<Fingerprint> items = fpm.getEnrolledFingerprints();
        if ((items != null ? items.size() : 0) > 0) {
            clazz = FingerprintSettings.class.getName();
        } else {
            clazz = FingerprintEnrollIntroduction.class.getName();
        }
        intent.setClassName("com.android.settings", clazz);
        startActivity(intent);
        finish();
    }
}
