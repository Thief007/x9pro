package com.android.settings;

import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.settings.utils.VoiceSettingsActivity;

public class AirplaneModeVoiceActivity extends VoiceSettingsActivity {
    protected boolean onVoiceSettingInteraction(Intent intent) {
        int i = 0;
        if (intent.hasExtra("airplane_mode_enabled")) {
            ContentResolver contentResolver = getContentResolver();
            String str = "airplane_mode_on";
            if (intent.getBooleanExtra("airplane_mode_enabled", false)) {
                i = 1;
            }
            Global.putInt(contentResolver, str, i);
        } else {
            Log.v("AirplaneModeVoiceActivity", "Missing airplane mode extra");
        }
        return true;
    }
}
