package com.android.settings.print;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import java.util.ArrayList;
import java.util.List;

public class PrintSettingsUtils {
    private PrintSettingsUtils() {
    }

    public static List<ComponentName> readEnabledPrintServices(Context context) {
        List<ComponentName> enabledServices = new ArrayList();
        String enabledServicesSetting = Secure.getString(context.getContentResolver(), "enabled_print_services");
        if (TextUtils.isEmpty(enabledServicesSetting)) {
            return enabledServices;
        }
        SimpleStringSplitter colonSplitter = new SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            ComponentName enabledService = ComponentName.unflattenFromString(colonSplitter.next());
            if (enabledService != null) {
                enabledServices.add(enabledService);
            }
        }
        return enabledServices;
    }

    public static void writeEnabledPrintServices(Context context, List<ComponentName> services) {
        StringBuilder builder = new StringBuilder();
        int serviceCount = services.size();
        for (int i = 0; i < serviceCount; i++) {
            ComponentName service = (ComponentName) services.get(i);
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(service.flattenToString());
        }
        Secure.putString(context.getContentResolver(), "enabled_print_services", builder.toString());
    }
}
