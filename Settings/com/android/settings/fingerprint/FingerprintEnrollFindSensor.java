package com.android.settings.fingerprint;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;

public class FingerprintEnrollFindSensor extends FingerprintEnrollBase {
    private FingerprintLocationAnimationView mAnimation;
    private boolean mLaunchedConfirmLock;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_find_sensor);
        setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title);
        this.mLaunchedConfirmLock = savedInstanceState != null ? savedInstanceState.getBoolean("launched_confirm_lock") : false;
        if (this.mToken == null && !this.mLaunchedConfirmLock) {
            launchConfirmLock();
        }
        this.mAnimation = (FingerprintLocationAnimationView) findViewById(R.id.fingerprint_sensor_location_animation);
    }

    protected void onStart() {
        super.onStart();
        this.mAnimation.startAnimation();
    }

    protected void onStop() {
        super.onStop();
        this.mAnimation.stopAnimation();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("launched_confirm_lock", this.mLaunchedConfirmLock);
    }

    protected void onNextButtonClick() {
        startActivityForResult(getEnrollingIntent(), 2);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == -1) {
                this.mToken = data.getByteArrayExtra("hw_auth_token");
                overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
                return;
            }
            finish();
        } else if (requestCode != 2) {
            super.onActivityResult(requestCode, resultCode, data);
        } else if (resultCode == 1) {
            setResult(1);
            finish();
        } else if (resultCode == 2) {
            setResult(2);
            finish();
        } else if (resultCode == 3) {
            setResult(3);
            finish();
        } else if (((FingerprintManager) getSystemService(FingerprintManager.class)).getEnrolledFingerprints().size() >= getResources().getInteger(17694871)) {
            finish();
        }
    }

    private void launchConfirmLock() {
        if (new ChooseLockSettingsHelper(this).launchConfirmationActivity(1, getString(R.string.security_settings_fingerprint_preference_title), null, null, ((FingerprintManager) getSystemService(FingerprintManager.class)).preEnroll())) {
            this.mLaunchedConfirmLock = true;
        } else {
            finish();
        }
    }

    protected int getMetricsCategory() {
        return 241;
    }
}
