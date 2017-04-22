package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.util.WizardManagerHelper;

public class SetupWizardUtils {
    public static int getTheme(Intent intent) {
        if (WizardManagerHelper.isLightTheme(intent, true)) {
            return R.style.SetupWizardTheme.Light;
        }
        return R.style.SetupWizardTheme;
    }

    public static void setImmersiveMode(Activity activity) {
        if (activity.getIntent().getBooleanExtra("useImmersiveMode", false)) {
            SystemBarHelper.hideSystemBars(activity.getWindow());
        }
    }

    public static void applyImmersiveFlags(Dialog dialog) {
        SystemBarHelper.hideSystemBars(dialog);
    }

    public static void copySetupExtras(Intent fromIntent, Intent toIntent) {
        toIntent.putExtra("theme", fromIntent.getStringExtra("theme"));
        toIntent.putExtra("useImmersiveMode", fromIntent.getBooleanExtra("useImmersiveMode", false));
    }
}
