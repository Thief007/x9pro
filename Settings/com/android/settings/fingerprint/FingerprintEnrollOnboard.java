package com.android.settings.fingerprint;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.R;

public class FingerprintEnrollOnboard extends FingerprintEnrollBase {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_onboard);
        setHeaderText(R.string.security_settings_fingerprint_enroll_onboard_title);
    }

    protected void onNextButtonClick() {
        launchChooseLock();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 1) {
            byte[] token = data.getByteArrayExtra("hw_auth_token");
            setResult(1);
            launchFindSensor(token);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void launchChooseLock() {
        Intent intent = getChooseLockIntent();
        long challenge = ((FingerprintManager) getSystemService(FingerprintManager.class)).preEnroll();
        intent.putExtra("minimum_quality", 65536);
        intent.putExtra("hide_disabled_prefs", true);
        intent.putExtra("has_challenge", true);
        intent.putExtra("challenge", challenge);
        intent.putExtra("for_fingerprint", true);
        startActivityForResult(intent, 1);
    }

    protected Intent getChooseLockIntent() {
        return new Intent(this, ChooseLockGeneric.class);
    }

    private void launchFindSensor(byte[] token) {
        Intent intent = getFindSensorIntent();
        intent.putExtra("hw_auth_token", token);
        startActivity(intent);
        finish();
    }

    protected Intent getFindSensorIntent() {
        return new Intent(this, FingerprintEnrollFindSensor.class);
    }

    protected int getMetricsCategory() {
        return 244;
    }
}
