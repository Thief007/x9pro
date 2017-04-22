package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;
import com.android.settings.AppListPreference;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISmsPreferenceExt;
import java.util.Collection;
import java.util.Objects;

public class DefaultSmsPreference extends AppListPreference {
    private ISmsPreferenceExt mExt = UtilsExt.getSmsPreferencePlugin(getContext());

    public DefaultSmsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mExt.createBroadcastReceiver(getContext(), this);
        loadSmsApps();
    }

    private void loadSmsApps() {
        Collection<SmsApplicationData> smsApplications = SmsApplication.getApplicationCollection(getContext());
        String[] packageNames = new String[smsApplications.size()];
        int i = 0;
        for (SmsApplicationData smsApplicationData : smsApplications) {
            int i2 = i + 1;
            packageNames[i] = smsApplicationData.mPackageName;
            i = i2;
        }
        setPackageNames(packageNames, getDefaultPackage());
    }

    private String getDefaultPackage() {
        ComponentName appName = SmsApplication.getDefaultSmsApplication(getContext(), true);
        if (appName != null) {
            return appName.getPackageName();
        }
        return null;
    }

    protected boolean persistString(String value) {
        if (!(TextUtils.isEmpty(value) || Objects.equals(value, getDefaultPackage()) || !this.mExt.getBroadcastIntent(getContext(), value))) {
            SmsApplication.setDefaultApplication(value, getContext());
        }
        if (this.mExt.canSetSummary()) {
            setSummary(getEntry());
        }
        return true;
    }

    public static boolean isAvailable(Context context) {
        return ((TelephonyManager) context.getSystemService("phone")).isSmsCapable();
    }
}
