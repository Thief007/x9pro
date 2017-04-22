package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;
import java.util.Map;

public class DefaultDataUsageSummaryExt implements IDataUsageSummaryExt {
    public DefaultDataUsageSummaryExt(Context context) {
    }

    public String customizeBackgroundString(String defStr, String tag) {
        return defStr;
    }

    public boolean needToShowDialog() {
        return true;
    }

    public boolean setDataEnableClickListener(Activity activity, View dataEnabledView, Switch dataEnabled, OnClickListener dataEnabledDialogListerner) {
        return false;
    }

    public void setCurrentTab(int subId) {
    }

    public void create(Map<String, Boolean> map) {
    }

    public void destroy() {
    }

    public boolean isAllowDataEnable(View view, int subId) {
        return true;
    }
}
