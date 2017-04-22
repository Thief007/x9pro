package com.android.settings.deviceinfo;

import android.content.Context;
import android.preference.Preference;

public class StorageItemPreference extends Preference {
    public int userHandle;

    public StorageItemPreference(Context context) {
        super(context);
    }
}
