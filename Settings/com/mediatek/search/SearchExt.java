package com.mediatek.search;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.common.mom.MobileManagerUtils;
import com.mediatek.settings.UtilsExt;
import java.util.ArrayList;
import java.util.List;

public class SearchExt implements Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C07181();

    static class C07181 extends BaseSearchIndexProvider {
        C07181() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList();
            List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(new Intent("com.android.settings.SCHEDULE_POWER_ON_OFF_SETTING"), 0);
            if (!(apps == null || apps.size() == 0)) {
                Log.d("SearchExt", "schedule power on exist");
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.schedule_power_on_off_settings_title);
                indexable.intentAction = "com.android.settings.SCHEDULE_POWER_ON_OFF_SETTING";
                indexables.add(indexable);
            }
            return indexables;
        }

        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList();
            if (!UtilsExt.isGmsBuild(context) && MobileManagerUtils.isSupported()) {
                result.add("manage_perms");
            }
            return result;
        }
    }
}
