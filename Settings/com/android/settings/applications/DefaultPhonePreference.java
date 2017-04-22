package com.android.settings.applications;

import android.content.Context;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.settings.AppListPreference;
import java.util.List;
import java.util.Objects;

public class DefaultPhonePreference extends AppListPreference {
    private final Context mContext;

    public DefaultPhonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context.getApplicationContext();
        if (isAvailable(context)) {
            loadDialerApps();
        }
    }

    protected boolean persistString(String value) {
        if (!(TextUtils.isEmpty(value) || Objects.equals(value, getDefaultPackage()))) {
            TelecomManager.from(this.mContext).setDefaultDialer(value);
        }
        setSummary(getEntry());
        return true;
    }

    private void loadDialerApps() {
        List<String> dialerPackages = DefaultDialerManager.getInstalledDialerApplications(getContext());
        String[] dialers = new String[dialerPackages.size()];
        for (int i = 0; i < dialerPackages.size(); i++) {
            dialers[i] = (String) dialerPackages.get(i);
        }
        setPackageNames(dialers, getDefaultPackage());
    }

    private String getDefaultPackage() {
        return DefaultDialerManager.getDefaultDialerApplication(getContext());
    }

    public static boolean isAvailable(Context context) {
        boolean z = false;
        if (!((TelephonyManager) context.getSystemService("phone")).isVoiceCapable()) {
            return false;
        }
        if (!((UserManager) context.getSystemService("user")).hasUserRestriction("no_outgoing_calls")) {
            z = true;
        }
        return z;
    }
}
