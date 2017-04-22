package com.android.settings.vpn2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.preference.Preference;
import android.view.View.OnClickListener;
import com.android.internal.net.VpnConfig;
import com.android.settings.R;

public class AppPreference extends ManageablePreference {
    private String mName;
    private String mPackageName;
    private int mState = 0;
    private int mUid;

    public AppPreference(Context context, OnClickListener onManage, String packageName, int uid) {
        super(context, null, onManage);
        this.mPackageName = packageName;
        this.mUid = uid;
        update();
    }

    public PackageInfo getPackageInfo() {
        UserHandle user = new UserHandle(UserHandle.getUserId(this.mUid));
        try {
            return getUserContext().getPackageManager().getPackageInfo(this.mPackageName, 0);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public String getLabel() {
        return this.mName;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getUid() {
        return this.mUid;
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int state) {
        this.mState = state;
        update();
    }

    private void update() {
        setSummary(this.mState != 0 ? getContext().getResources().getStringArray(R.array.vpn_states)[this.mState] : "");
        this.mName = this.mPackageName;
        Drawable drawable = null;
        try {
            Context userContext = getUserContext();
            PackageManager pm = userContext.getPackageManager();
            try {
                PackageInfo pkgInfo = pm.getPackageInfo(this.mPackageName, 0);
                if (pkgInfo != null) {
                    drawable = pkgInfo.applicationInfo.loadIcon(pm);
                    this.mName = VpnConfig.getVpnLabel(userContext, this.mPackageName).toString();
                }
            } catch (NameNotFoundException e) {
            }
            if (drawable == null) {
                drawable = pm.getDefaultActivityIcon();
            }
        } catch (NameNotFoundException e2) {
        }
        setTitle(this.mName);
        setIcon(drawable);
        notifyHierarchyChanged();
    }

    private Context getUserContext() throws NameNotFoundException {
        return getContext().createPackageContextAsUser(getContext().getPackageName(), 0, new UserHandle(UserHandle.getUserId(this.mUid)));
    }

    public int compareTo(Preference preference) {
        if (preference instanceof AppPreference) {
            AppPreference another = (AppPreference) preference;
            int result = another.mState - this.mState;
            if (result == 0) {
                result = this.mName.compareToIgnoreCase(another.mName);
                if (result == 0) {
                    result = this.mPackageName.compareTo(another.mPackageName);
                    if (result == 0) {
                        result = this.mUid - another.mUid;
                    }
                }
            }
            return result;
        } else if (preference instanceof ConfigPreference) {
            return -((ConfigPreference) preference).compareTo(this);
        } else {
            return super.compareTo(preference);
        }
    }
}
