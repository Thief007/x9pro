package com.android.settings.vpn2;

import android.content.Context;
import android.preference.Preference;
import android.view.View.OnClickListener;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;

public class ConfigPreference extends ManageablePreference {
    private VpnProfile mProfile;
    private int mState = -1;

    ConfigPreference(Context context, OnClickListener onManage, VpnProfile profile) {
        super(context, null, onManage);
        setProfile(profile);
    }

    public VpnProfile getProfile() {
        return this.mProfile;
    }

    public void setProfile(VpnProfile profile) {
        this.mProfile = profile;
        update();
    }

    public void setState(int state) {
        this.mState = state;
        update();
    }

    private void update() {
        if (this.mState < 0) {
            setSummary("");
        } else {
            setSummary(getContext().getResources().getStringArray(R.array.vpn_states)[this.mState]);
        }
        setIcon(R.mipmap.ic_launcher_settings);
        setTitle(this.mProfile.name);
        notifyHierarchyChanged();
    }

    public int compareTo(Preference preference) {
        if (preference instanceof ConfigPreference) {
            ConfigPreference another = (ConfigPreference) preference;
            int result = another.mState - this.mState;
            if (result == 0) {
                result = this.mProfile.name.compareTo(another.mProfile.name);
                if (result == 0) {
                    result = this.mProfile.type - another.mProfile.type;
                    if (result == 0) {
                        result = this.mProfile.key.compareTo(another.mProfile.key);
                    }
                }
            }
            return result;
        } else if (!(preference instanceof AppPreference)) {
            return super.compareTo(preference);
        } else {
            AppPreference another2 = (AppPreference) preference;
            if (this.mState == 3 || another2.getState() != 3) {
                return -1;
            }
            return 1;
        }
    }
}
