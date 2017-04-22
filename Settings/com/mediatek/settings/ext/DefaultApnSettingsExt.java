package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import java.util.ArrayList;

public class DefaultApnSettingsExt implements IApnSettingsExt {
    private static final String TAG = "DefaultApnSettingsExt";

    public void onDestroy() {
    }

    public void initTetherField(PreferenceFragment pref) {
    }

    public boolean isAllowEditPresetApn(String type, String apn, String numeric, int sourcetype) {
        Log.d(TAG, "isAllowEditPresetApn");
        return true;
    }

    public void customizeTetherApnSettings(PreferenceScreen root) {
    }

    public String getFillListQuery(String where, String mccmnc) {
        return where;
    }

    public void updateTetherState() {
    }

    public Uri getPreferCarrierUri(Uri defaultUri, int subId) {
        return defaultUri;
    }

    public void setApnTypePreferenceState(Preference preference, String apnType) {
    }

    public Uri getUriFromIntent(Uri defaultUri, Context context, Intent intent) {
        return defaultUri;
    }

    public String[] getApnTypeArray(String[] defaultApnArray, Context context, String apnType) {
        return defaultApnArray;
    }

    public boolean isSelectable(String type) {
        return true;
    }

    public boolean getScreenEnableState(int subId, Activity activity) {
        return true;
    }

    public void updateMenu(Menu menu, int newMenuId, int restoreMenuId, String numeric) {
    }

    public void addApnTypeExtra(Intent it) {
    }

    public void updateFieldsStatus(int subId, int sourceType, PreferenceScreen root) {
    }

    public void setPreferenceTextAndSummary(int subId, String text) {
    }

    public void customizePreference(int subId, PreferenceScreen root) {
    }

    public String[] customizeApnProjection(String[] projection) {
        return projection;
    }

    public void saveApnValues(ContentValues contentValues) {
    }

    public String updateApnName(String name, int sourcetype) {
        return name;
    }

    public long replaceApn(long defaultReplaceNum, Context context, Uri uri, String apn, String name, ContentValues values, String numeric) {
        return defaultReplaceNum;
    }

    public void customizeUnselectableApn(ArrayList<Preference> arrayList, ArrayList<Preference> arrayList2, int subId) {
    }

    public void setMvnoPreferenceState(Preference mvnoType, Preference mvnoMatchData) {
    }

    public String getApnSortOrder(String order) {
        return null;
    }
}
