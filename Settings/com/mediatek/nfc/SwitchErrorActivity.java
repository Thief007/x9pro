package com.mediatek.nfc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController.AlertParams;
import com.android.settings.R;

public class SwitchErrorActivity extends AlertActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        String action = i.getAction();
        if ("android.nfc.action.SWITCH_FAIL_DIALOG_REQUEST".equals(action)) {
            String mode = i.getStringExtra("mode");
            Log.d("@M_SwitchErrorActivity", "switch fail mode is " + mode);
            showErrorDialog(mode);
        } else if ("android.nfc.action.NOT_NFC_SIM_DIALOG_REQUEST".equals(action)) {
            String sim = i.getStringExtra("android.nfc.extra.WHAT_SIM");
            Log.d("@M_SwitchErrorActivity", "show not support dialog, sim is " + sim);
            showNotSupportDialog(sim);
        } else if ("android.nfc.action.NOT_NFC_TWO_SIM_DIALOG_REQUEST".equals(action)) {
            Log.d("@M_SwitchErrorActivity", "show not support dialog for SIM1 and SIM2");
            showTwoSimNotSupportDialog();
        } else {
            Log.e("@M_SwitchErrorActivity", "Error: this activity may be started only with intent android.nfc.action.SWITCH_FAIL_DIALOG_REQUEST " + action);
            finish();
        }
    }

    private void showErrorDialog(String errorMode) {
        AlertParams p = this.mAlertParams;
        p.mIconId = 17301543;
        p.mTitle = getString(R.string.card_emulation_switch_error_title);
        p.mMessage = getString(R.string.card_emulation_switch_error_message, new Object[]{errorMode});
        p.mPositiveButtonText = getString(17039370);
        setupAlert();
    }

    private void showNotSupportDialog(String simDescription) {
        AlertParams p = this.mAlertParams;
        p.mIconId = 17301543;
        p.mTitle = getString(R.string.card_emulation_switch_error_title);
        p.mMessage = getString(R.string.card_emulation_sim_not_supported_message, new Object[]{simDescription});
        p.mPositiveButtonText = getString(17039370);
        setupAlert();
    }

    private void showTwoSimNotSupportDialog() {
        AlertParams p = this.mAlertParams;
        p.mIconId = 17301543;
        p.mTitle = getString(R.string.card_emulation_switch_error_title);
        p.mMessage = getString(R.string.card_emulation_two_sim_not_supported_message);
        p.mPositiveButtonText = getString(17039370);
        setupAlert();
    }
}
