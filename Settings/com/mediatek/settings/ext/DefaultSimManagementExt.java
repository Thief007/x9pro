package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.telecom.PhoneAccountHandle;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;

public class DefaultSimManagementExt implements ISimManagementExt {
    public void onResume(Context context) {
    }

    public void onPause() {
    }

    public void updateSimEditorPref(PreferenceFragment pref) {
    }

    public void updateDefaultSmsSummary(Preference pref) {
    }

    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
    }

    public void hideSimEditorView(View view, Context context) {
    }

    public void setSmsAutoItemIcon(ImageView view, int dialogId, int position) {
    }

    public int getDefaultSmsSubIdForAuto() {
        return 0;
    }

    public void initAutoItemForSms(ArrayList<String> arrayList, ArrayList<SubscriptionInfo> arrayList2) {
    }

    public void setDataState(int subId) {
    }

    public void setDataStateEnable(int subId) {
    }

    public boolean switchDefaultDataSub(Context context, int subId) {
        return false;
    }

    public void customizeListArray(List<String> list) {
    }

    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> list) {
    }

    public int customizeValue(int value) {
        return value;
    }

    public boolean isSimDialogNeeded() {
        return true;
    }

    public boolean useCtTestcard() {
        return false;
    }

    public void setRadioPowerState(int subId, boolean turnOn) {
    }

    public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo sir, String type) {
        return sir;
    }

    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccount) {
        return phoneAccount;
    }

    public void configSimPreferenceScreen(Preference simPref, String type, int size) {
    }
}
