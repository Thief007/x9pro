package com.mediatek.settings.inputmethod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.ChooseLockSettingsHelper;

public class VowKeyguardConfirm extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("VowKeyguardConfirm", "onCreate");
        if (!runKeyguardConfirmation(55)) {
            setResult(-1);
            finish();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("VowKeyguardConfirm", "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (requestCode == 55) {
            if (resultCode == -1) {
                setResult(-1);
                finish();
            } else {
                setResult(0);
                finish();
            }
        }
    }

    private boolean runKeyguardConfirmation(int request) {
        return new ChooseLockSettingsHelper(this).launchConfirmationActivity(request, getIntent().getCharSequenceExtra("title"), null, null, 0);
    }
}
