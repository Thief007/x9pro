package com.mediatek.settings.ext;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;
import java.util.Map;

public interface IDataUsageSummaryExt {
    public static final String TAG_BG_DATA_APP_DIALOG_MESSAGE = "bgDataDialogMessage";
    public static final String TAG_BG_DATA_APP_DIALOG_TITLE = "bgDataDialogTitle";
    public static final String TAG_BG_DATA_MENU_DIALOG_MESSAGE = "bgDataMenuDialogMessage";
    public static final String TAG_BG_DATA_RESTRICT_DENY_MESSAGE = "bgDataRestrictDenyMessage";
    public static final String TAG_BG_DATA_SUMMARY = "bgDataSummary";
    public static final String TAG_BG_DATA_SWITCH = "bgDataSwitch";

    void create(Map<String, Boolean> map);

    String customizeBackgroundString(String str, String str2);

    void destroy();

    boolean isAllowDataEnable(View view, int i);

    boolean needToShowDialog();

    void setCurrentTab(int i);

    boolean setDataEnableClickListener(Activity activity, View view, Switch switchR, OnClickListener onClickListener);
}
