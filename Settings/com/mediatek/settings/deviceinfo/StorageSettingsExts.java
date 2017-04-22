package com.mediatek.settings.deviceinfo;

import android.app.Activity;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.mediatek.settings.FeatureOption;
import com.mediatek.storage.StorageManagerEx;
import java.util.ArrayList;
import java.util.List;

public class StorageSettingsExts {
    private OnPreferenceChangeListener defaultWriteDiskListener = new C07331();
    private Activity mActivity;
    private RadioButtonPreference mDeafultWritePathPref;
    private String mDefaultWritePath;
    private PreferenceCategory mDiskCategory;
    private String mExternalSDPath;
    private boolean mIsCategoryAdded = true;
    private PreferenceScreen mRoot;
    private StorageManager mStorageManager;

    class C07331 implements OnPreferenceChangeListener {
        C07331() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference == null || !(preference instanceof RadioButtonPreference)) {
                return false;
            }
            if (StorageSettingsExts.this.mDeafultWritePathPref != null) {
                StorageSettingsExts.this.mDeafultWritePathPref.setChecked(false);
            }
            StorageManagerEx.setDefaultPath(preference.getKey());
            Log.d("StorageSettings", "Set default path : " + preference.getKey());
            StorageSettingsExts.this.mDeafultWritePathPref = (RadioButtonPreference) preference;
            return true;
        }
    }

    public StorageSettingsExts(Activity activity, PreferenceScreen preferenceScreen, StorageManager storageManager) {
        this.mActivity = activity;
        this.mRoot = preferenceScreen;
        this.mStorageManager = storageManager;
    }

    private void initDefaultWriteDiskCategory() {
        this.mDiskCategory = (PreferenceCategory) this.mRoot.findPreference("default_write_disk");
        if (FeatureOption.MTK_A1_FEATURE) {
            this.mRoot.removePreference(this.mDiskCategory);
        }
    }

    private void updateDefaultWriteDiskCategory() {
        if (!FeatureOption.MTK_A1_FEATURE) {
            this.mDiskCategory.removeAll();
            this.mExternalSDPath = StorageManagerEx.getExternalStoragePath();
            this.mDefaultWritePath = StorageManagerEx.getDefaultPath();
            Log.d("StorageSettings", "Get default Path : " + this.mDefaultWritePath);
            for (StorageVolume volume : getDefaultWriteDiskList()) {
                RadioButtonPreference preference = new RadioButtonPreference(this.mActivity);
                String path = volume.getPath();
                preference.setKey(path);
                preference.setTitle(volume.getDescription(this.mActivity));
                preference.setPath(path);
                preference.setOnPreferenceChangeListener(this.defaultWriteDiskListener);
                this.mDiskCategory.addPreference(preference);
                if (path.equals(this.mExternalSDPath)) {
                    preference.setOrder(-2);
                } else if (path.startsWith("/mnt/usbotg")) {
                    preference.setOrder(-1);
                } else {
                    preference.setOrder(-3);
                }
                if (this.mDefaultWritePath.equals(path)) {
                    preference.setChecked(true);
                    this.mDeafultWritePathPref = preference;
                } else {
                    preference.setChecked(false);
                }
            }
            if (this.mDiskCategory.getPreferenceCount() > 0) {
                this.mRoot.addPreference(this.mDiskCategory);
            }
        }
    }

    private StorageVolume[] getDefaultWriteDiskList() {
        List<StorageVolume> storageVolumes = new ArrayList();
        StorageManager storageManager = this.mStorageManager;
        for (StorageVolume volume : StorageManager.getVolumeList(UserHandle.myUserId(), 1)) {
            Log.d("StorageSettings", "Volume : " + volume.getDescription(this.mActivity) + " , path : " + volume.getPath() + " , state : " + this.mStorageManager.getVolumeState(volume.getPath()) + " , emulated : " + volume.isEmulated());
            if ("mounted".equals(this.mStorageManager.getVolumeState(volume.getPath()))) {
                storageVolumes.add(volume);
            }
        }
        return (StorageVolume[]) storageVolumes.toArray(new StorageVolume[storageVolumes.size()]);
    }

    public void initCustomizationCategory() {
        initDefaultWriteDiskCategory();
    }

    public void updateCustomizationCategory() {
        updateDefaultWriteDiskCategory();
    }
}
