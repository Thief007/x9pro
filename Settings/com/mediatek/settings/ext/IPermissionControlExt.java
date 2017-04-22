package com.mediatek.settings.ext;

import android.preference.PreferenceGroup;
import android.provider.SearchIndexableData;
import java.util.List;

public interface IPermissionControlExt {
    void addAutoBootPrf(PreferenceGroup preferenceGroup);

    void addPermSwitchPrf(PreferenceGroup preferenceGroup);

    void enablerPause();

    void enablerResume();

    List<SearchIndexableData> getRawDataToIndex(boolean z);
}
