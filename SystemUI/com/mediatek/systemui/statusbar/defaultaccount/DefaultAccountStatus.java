package com.mediatek.systemui.statusbar.defaultaccount;

import android.telephony.SubscriptionManager;
import com.android.systemui.R;

public class DefaultAccountStatus {
    private int mAccountStatusIconId;
    private int mDefSignalBackgroundIconId;
    private int mSubId;

    public DefaultAccountStatus(int subId) {
        this.mSubId = subId;
        this.mAccountStatusIconId = getIconId(subId);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            this.mDefSignalBackgroundIconId = R.drawable.stat_sys_default_sim_indicator;
        }
    }

    public int getSubId() {
        return this.mSubId;
    }

    public int getAccountStatusIconId() {
        return this.mAccountStatusIconId;
    }

    private int getIconId(int subId) {
        switch (subId) {
            case -100:
                return R.drawable.sim_indicator_always_ask;
            case -99:
                return R.drawable.sim_indicator_internet_call;
            case -98:
                return R.drawable.sim_indicator_auto;
            case -97:
                return R.drawable.sim_indicator_others;
            default:
                return 0;
        }
    }

    public int getDefSignalBackgroundIconId() {
        return this.mDefSignalBackgroundIconId;
    }
}
