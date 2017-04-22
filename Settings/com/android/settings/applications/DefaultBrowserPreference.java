package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.util.AttributeSet;
import com.android.settings.AppListPreference;
import java.util.ArrayList;
import java.util.List;

public class DefaultBrowserPreference extends AppListPreference {
    private final PackageManager mPm;

    public DefaultBrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPm = context.getPackageManager();
        refreshBrowserApps();
    }

    public void refreshBrowserApps() {
        List<String> browsers = resolveBrowserApps();
        setPackageNames((CharSequence[]) browsers.toArray(new String[browsers.size()]), null);
    }

    private List<String> resolveBrowserApps() {
        List<String> result = new ArrayList();
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.BROWSABLE");
        intent.setData(Uri.parse("http:"));
        List<ResolveInfo> list = this.mPm.queryIntentActivitiesAsUser(intent, 131072, UserHandle.myUserId());
        int count = list.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = (ResolveInfo) list.get(i);
            if (!(info.activityInfo == null || result.contains(info.activityInfo.packageName) || !info.handleAllWebDataURI)) {
                result.add(info.activityInfo.packageName);
            }
        }
        return result;
    }
}
