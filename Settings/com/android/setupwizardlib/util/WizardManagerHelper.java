package com.android.setupwizardlib.util;

import android.content.Intent;

public class WizardManagerHelper {
    public static boolean isLightTheme(Intent intent, boolean def) {
        String theme = intent.getStringExtra("theme");
        if ("holo_light".equals(theme) || "material_light".equals(theme) || "material_blue_light".equals(theme)) {
            return true;
        }
        if ("holo".equals(theme) || "material".equals(theme) || "material_blue".equals(theme)) {
            return false;
        }
        return def;
    }
}
