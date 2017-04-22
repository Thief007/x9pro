package com.android.settings.notification;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.util.SparseArray;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

public class ZenModeSettings extends ZenModeSettingsBase implements Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C04891();
    private Preference mPrioritySettings;

    static class C04891 extends BaseSearchIndexProvider {
        C04891() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            SparseArray<String> keyTitles = ZenModeSettings.allKeyTitles(context);
            int N = keyTitles.size();
            List<SearchIndexableRaw> result = new ArrayList(N);
            Resources res = context.getResources();
            for (int i = 0; i < N; i++) {
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.key = (String) keyTitles.valueAt(i);
                data.title = res.getString(keyTitles.keyAt(i));
                data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                result.add(data);
            }
            return result;
        }

        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> rt = new ArrayList();
            if (!ZenModeSettingsBase.isScheduleSupported(context)) {
                rt.add("automation_settings");
            }
            return rt;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_settings);
        this.mPrioritySettings = getPreferenceScreen().findPreference("priority_settings");
        if (!ZenModeSettingsBase.isScheduleSupported(this.mContext)) {
            removePreference("automation_settings");
        }
    }

    public void onResume() {
        super.onResume();
        updateControls();
    }

    protected int getMetricsCategory() {
        return 76;
    }

    protected void onZenModeChanged() {
        updateControls();
    }

    protected void onZenModeConfigChanged() {
        updateControls();
    }

    private void updateControls() {
        updatePrioritySettingsSummary();
    }

    private void updatePrioritySettingsSummary() {
        this.mPrioritySettings.setSummary(appendLowercase(appendLowercase(appendLowercase(appendLowercase(getResources().getString(R.string.zen_mode_alarms), this.mConfig.allowReminders, R.string.zen_mode_reminders), this.mConfig.allowEvents, R.string.zen_mode_events), !this.mConfig.allowCalls ? this.mConfig.allowRepeatCallers : true, R.string.zen_mode_selected_callers), this.mConfig.allowMessages, R.string.zen_mode_selected_messages));
    }

    private String appendLowercase(String s, boolean condition, int resId) {
        if (!condition) {
            return s;
        }
        return getResources().getString(R.string.join_many_items_middle, new Object[]{s, getResources().getString(resId).toLowerCase()});
    }

    private static SparseArray<String> allKeyTitles(Context context) {
        SparseArray<String> rt = new SparseArray();
        rt.put(R.string.zen_mode_priority_settings_title, "priority_settings");
        rt.put(R.string.zen_mode_automation_settings_title, "automation_settings");
        return rt;
    }

    protected int getHelpResource() {
        return R.string.help_uri_interruptions;
    }
}
