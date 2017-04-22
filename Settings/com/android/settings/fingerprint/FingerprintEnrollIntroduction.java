package com.android.settings.fingerprint;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.view.View;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.HelpUtils;
import com.android.settings.R;

public class FingerprintEnrollIntroduction extends FingerprintEnrollBase {
    private boolean mHasPassword;

    protected void onCreate(Bundle savedInstanceState) {
        boolean z = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_introduction);
        setHeaderText(R.string.security_settings_fingerprint_enroll_introduction_title);
        findViewById(R.id.cancel_button).setOnClickListener(this);
        View learnMoreButton = findViewById(R.id.learn_more_button);
        learnMoreButton.setOnClickListener(this);
        if (Global.getInt(getContentResolver(), "device_provisioned", 0) == 0) {
            learnMoreButton.setVisibility(8);
        }
        if (((double) new ChooseLockSettingsHelper(this).utils().getActivePasswordQuality(UserHandle.myUserId())) != 0.0d) {
            z = true;
        }
        this.mHasPassword = z;
    }

    protected void onNextButtonClick() {
        Intent intent;
        if (this.mHasPassword) {
            intent = getFindSensorIntent();
        } else {
            intent = getOnboardIntent();
        }
        startActivityForResult(intent, 0);
    }

    protected Intent getOnboardIntent() {
        return new Intent(this, FingerprintEnrollOnboard.class);
    }

    protected Intent getFindSensorIntent() {
        return new Intent(this, FingerprintEnrollFindSensor.class);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1) {
            setResult(-1);
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.cancel_button) {
            finish();
        }
        if (v.getId() == R.id.learn_more_button) {
            launchFingerprintHelp();
        }
        super.onClick(v);
    }

    private void launchFingerprintHelp() {
        Intent helpIntent = HelpUtils.getHelpIntent(this, getString(R.string.help_url_fingerprint), getClass().getName());
        if (helpIntent != null) {
            startActivity(helpIntent);
        }
    }

    protected int getMetricsCategory() {
        return 243;
    }
}
