package com.android.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import java.io.File;

public class SettingsLicenseActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = SystemProperties.get("ro.config.license_path", "/system/etc/NOTICE.html.gz");
        if (TextUtils.isEmpty(path)) {
            Log.e("SettingsLicenseActivity", "The system property for the license file is empty");
            showErrorAndFinish();
            return;
        }
        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            Log.e("SettingsLicenseActivity", "License file " + path + " does not exist");
            showErrorAndFinish();
            return;
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(Uri.fromFile(file), "text/html");
        intent.putExtra("android.intent.extra.TITLE", getString(R.string.settings_license_activity_title));
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setPackage("com.android.htmlviewer");
        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            Log.e("SettingsLicenseActivity", "Failed to find viewer", e);
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_license_activity_unavailable, 1).show();
        finish();
    }
}
