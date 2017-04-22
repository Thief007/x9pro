package com.android.settings.applications;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.AppHeader;
import com.android.settings.SettingsPreferenceFragment;

public abstract class AppInfoWithHeader extends AppInfoBase {
    private boolean mCreated;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        this.mCreated = true;
        if (this.mPackageInfo != null) {
            AppHeader.createAppHeader((SettingsPreferenceFragment) this, this.mPackageInfo.applicationInfo.loadIcon(this.mPm), this.mPackageInfo.applicationInfo.loadLabel(this.mPm), getInfoIntent(this, this.mPackageName), 0);
        }
    }

    public static Intent getInfoIntent(Fragment fragment, String packageName) {
        Bundle args = fragment.getArguments();
        Intent intent = fragment.getActivity().getIntent();
        boolean showInfo = true;
        if (args != null && args.getBoolean("hideInfoButton", false)) {
            showInfo = false;
        }
        if (intent != null && intent.getBooleanExtra("hideInfoButton", false)) {
            showInfo = false;
        }
        if (!showInfo) {
            return null;
        }
        Intent infoIntent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        infoIntent.setData(Uri.fromParts("package", packageName, null));
        return infoIntent;
    }
}
