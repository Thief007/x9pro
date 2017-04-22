package com.android.settings.fingerprint;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;

public class FingerprintEnrollFinish extends FingerprintEnrollBase {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_finish);
        setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);
        Button addButton = (Button) findViewById(R.id.add_another_button);
        if (((FingerprintManager) getSystemService("fingerprint")).getEnrolledFingerprints().size() >= getResources().getInteger(17694871)) {
            addButton.setVisibility(4);
        } else {
            addButton.setOnClickListener(this);
        }
    }

    protected void onNextButtonClick() {
        setResult(1);
        finish();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.add_another_button) {
            Intent intent = getEnrollingIntent();
            intent.addFlags(33554432);
            startActivity(intent);
            finish();
        }
        super.onClick(v);
    }

    protected int getMetricsCategory() {
        return 242;
    }
}
