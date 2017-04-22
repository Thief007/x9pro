package com.android.settings.deviceinfo;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.deviceinfo.StorageSettingsExts;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageSettings extends SettingsPreferenceFragment implements Indexable {
    static final int[] COLOR_PRIVATE = new int[]{Color.parseColor("#ff26a69a"), Color.parseColor("#ffab47bc"), Color.parseColor("#fff2a600"), Color.parseColor("#ffec407a"), Color.parseColor("#ffc0ca33")};
    static final int COLOR_PUBLIC = Color.parseColor("#ff9e9e9e");
    static final int COLOR_WARNING = Color.parseColor("#fff4511e");
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C03432();
    private static boolean sHasOpened;
    private StorageSettingsExts mCustomizationCategory;
    private PreferenceCategory mExternalCategory;
    private PreferenceCategory mInternalCategory;
    private StorageSummaryPreference mInternalSummary;
    private final StorageEventListener mStorageListener = new C03421();
    private StorageManager mStorageManager;

    class C03421 extends StorageEventListener {
        C03421() {
        }

        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (StorageSettings.isInteresting(vol)) {
                StorageSettings.this.refresh();
            }
        }

        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            StorageSettings.this.refresh();
        }

        public void onDiskDestroyed(DiskInfo disk) {
            StorageSettings.this.refresh();
        }
    }

    static class C03432 extends BaseSearchIndexProvider {
        C03432() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.storage_settings);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.internal_storage);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
            for (VolumeInfo vol : storage.getVolumes()) {
                if (StorageSettings.isInteresting(vol)) {
                    data.title = storage.getBestVolumeDescription(vol);
                    data.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data);
                }
            }
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_size);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_available);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_apps_usage);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_dcim_usage);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_music_usage);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_downloads_usage);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_media_cache_usage);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            data = new SearchIndexableRaw(context);
            data.title = context.getString(R.string.memory_media_misc_usage);
            data.screenTitle = context.getString(R.string.storage_settings);
            result.add(data);
            return result;
        }
    }

    public static class DiskInitFragment extends DialogFragment {
        public static void show(Fragment parent, int resId, String diskId) {
            Bundle args = new Bundle();
            args.putInt("android.intent.extra.TEXT", resId);
            args.putString("android.os.storage.extra.DISK_ID", diskId);
            DiskInitFragment dialog = new DiskInitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "disk_init");
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            StorageManager sm = (StorageManager) context.getSystemService(StorageManager.class);
            int resId = getArguments().getInt("android.intent.extra.TEXT");
            final String diskId = getArguments().getString("android.os.storage.extra.DISK_ID");
            DiskInfo disk = sm.findDiskById(diskId);
            Builder builder = new Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(resId), new CharSequence[]{disk.getDescription()}));
            builder.setPositiveButton(R.string.storage_menu_set_up, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(context, StorageWizardInit.class);
                    intent.putExtra("android.os.storage.extra.DISK_ID", diskId);
                    DiskInitFragment.this.startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            return builder.create();
        }
    }

    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final String mDescription;
        private final StorageManager mStorageManager = ((StorageManager) this.mContext.getSystemService(StorageManager.class));
        private final String mVolumeId;

        public MountTask(Context context, VolumeInfo volume) {
            this.mContext = context.getApplicationContext();
            this.mVolumeId = volume.getId();
            this.mDescription = this.mStorageManager.getBestVolumeDescription(volume);
        }

        protected Exception doInBackground(Void... params) {
            try {
                this.mStorageManager.mount(this.mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_mount_success, new Object[]{this.mDescription}), 0).show();
                return;
            }
            Log.e("StorageSettings", "Failed to mount " + this.mVolumeId, e);
            Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_mount_failure, new Object[]{this.mDescription}), 0).show();
        }
    }

    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final String mDescription;
        private final StorageManager mStorageManager = ((StorageManager) this.mContext.getSystemService(StorageManager.class));
        private final String mVolumeId;

        public UnmountTask(Context context, VolumeInfo volume) {
            this.mContext = context.getApplicationContext();
            this.mVolumeId = volume.getId();
            this.mDescription = this.mStorageManager.getBestVolumeDescription(volume);
        }

        protected Exception doInBackground(Void... params) {
            try {
                this.mStorageManager.unmount(this.mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_unmount_success, new Object[]{this.mDescription}), 0).show();
                return;
            }
            Log.e("StorageSettings", "Failed to unmount " + this.mVolumeId, e);
            Toast.makeText(this.mContext, this.mContext.getString(R.string.storage_unmount_failure, new Object[]{this.mDescription}), 0).show();
        }
    }

    public static class VolumeUnmountedFragment extends DialogFragment {
        public static void show(Fragment parent, String volumeId) {
            Bundle args = new Bundle();
            args.putString("android.os.storage.extra.VOLUME_ID", volumeId);
            VolumeUnmountedFragment dialog = new VolumeUnmountedFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "volume_unmounted");
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final VolumeInfo vol = ((StorageManager) context.getSystemService(StorageManager.class)).findVolumeById(getArguments().getString("android.os.storage.extra.VOLUME_ID"));
            Builder builder = new Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_dialog_unmounted), new CharSequence[]{vol.getDisk().getDescription()}));
            builder.setPositiveButton(R.string.storage_menu_mount, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    new MountTask(context, vol).execute(new Void[0]);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            return builder.create();
        }
    }

    protected int getMetricsCategory() {
        return 42;
    }

    protected int getHelpResource() {
        return R.string.help_uri_storage;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        addPreferencesFromResource(R.xml.device_info_storage);
        this.mInternalCategory = (PreferenceCategory) findPreference("storage_internal");
        this.mExternalCategory = (PreferenceCategory) findPreference("storage_external");
        this.mInternalSummary = new StorageSummaryPreference(context);
        setHasOptionsMenu(true);
        this.mCustomizationCategory = new StorageSettingsExts(getActivity(), getPreferenceScreen(), this.mStorageManager);
        this.mCustomizationCategory.initCustomizationCategory();
    }

    private static boolean isInteresting(VolumeInfo vol) {
        switch (vol.getType()) {
            case 0:
            case 1:
                return true;
            default:
                return false;
        }
    }

    private void refresh() {
        Context context = getActivity();
        getPreferenceScreen().removeAll();
        this.mInternalCategory.removeAll();
        this.mExternalCategory.removeAll();
        this.mCustomizationCategory.updateCustomizationCategory();
        this.mInternalCategory.addPreference(this.mInternalSummary);
        int privateCount = 0;
        long privateUsedBytes = 0;
        long privateTotalBytes = 0;
        List<VolumeInfo> volumes = this.mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == 1) {
                int privateCount2 = privateCount + 1;
                this.mInternalCategory.addPreference(new StorageVolumePreference(context, vol, COLOR_PRIVATE[privateCount % COLOR_PRIVATE.length]));
                if (vol.isMountedReadable()) {
                    File path = vol.getPath();
                    privateUsedBytes += path.getTotalSpace() - path.getFreeSpace();
                    privateTotalBytes += path.getTotalSpace();
                    privateCount = privateCount2;
                } else {
                    privateCount = privateCount2;
                }
            } else if (vol.getType() == 0) {
                this.mExternalCategory.addPreference(new StorageVolumePreference(context, vol, COLOR_PUBLIC));
            }
        }
        for (VolumeRecord rec : this.mStorageManager.getVolumeRecords()) {
            if (rec.getType() == 1 && this.mStorageManager.findVolumeByUuid(rec.getFsUuid()) == null) {
                Drawable icon = context.getDrawable(R.drawable.ic_sim_sd);
                icon.mutate();
                icon.setTint(COLOR_PUBLIC);
                Preference pref = new Preference(context);
                pref.setKey(rec.getFsUuid());
                pref.setTitle(rec.getNickname());
                pref.setSummary(17040398);
                pref.setIcon(icon);
                this.mInternalCategory.addPreference(pref);
            }
        }
        for (DiskInfo disk : this.mStorageManager.getDisks()) {
            if (disk.volumeCount == 0 && disk.size > 0) {
                pref = new Preference(context);
                pref.setKey(disk.getId());
                pref.setTitle(disk.getDescription());
                pref.setSummary(17040395);
                pref.setIcon(R.drawable.ic_sim_sd);
                this.mExternalCategory.addPreference(pref);
            }
        }
        BytesResult result = Formatter.formatBytes(getResources(), privateUsedBytes, 0);
        this.mInternalSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), new CharSequence[]{result.value, result.units}));
        this.mInternalSummary.setSummary(getString(R.string.storage_volume_used_total, new Object[]{Formatter.formatFileSize(context, privateTotalBytes)}));
        if (this.mInternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(this.mInternalCategory);
        }
        if (this.mExternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(this.mExternalCategory);
        }
        if (this.mInternalCategory.getPreferenceCount() == 2 && this.mExternalCategory.getPreferenceCount() == 0 && !sHasOpened) {
            Bundle args = new Bundle();
            args.putString("android.os.storage.extra.VOLUME_ID", "private");
            startFragment(this, PrivateVolumeSettings.class.getCanonicalName(), -1, 0, args);
            sHasOpened = true;
            finish();
        }
    }

    public void onResume() {
        super.onResume();
        this.mStorageManager.registerListener(this.mStorageListener);
        sHasOpened = false;
        refresh();
    }

    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
        String key = pref.getKey();
        Bundle args;
        if (pref instanceof StorageVolumePreference) {
            VolumeInfo vol = this.mStorageManager.findVolumeById(key);
            if (vol.getState() == 0) {
                VolumeUnmountedFragment.show(this, vol.getId());
                return true;
            } else if (vol.getState() == 6) {
                DiskInitFragment.show(this, R.string.storage_dialog_unmountable, vol.getDiskId());
                return true;
            } else if (vol.getType() == 1) {
                args = new Bundle();
                args.putString("android.os.storage.extra.VOLUME_ID", vol.getId());
                startFragment(this, PrivateVolumeSettings.class.getCanonicalName(), -1, 0, args);
                return true;
            } else if (vol.getType() != 0) {
                return false;
            } else {
                if (vol.isMountedReadable()) {
                    startActivity(vol.buildBrowseIntent());
                    return true;
                }
                args = new Bundle();
                args.putString("android.os.storage.extra.VOLUME_ID", vol.getId());
                startFragment(this, PublicVolumeSettings.class.getCanonicalName(), -1, 0, args);
                return true;
            }
        } else if (key.startsWith("disk:")) {
            DiskInitFragment.show(this, R.string.storage_dialog_unsupported, key);
            return true;
        } else if (key.startsWith("/storage/")) {
            return false;
        } else {
            args = new Bundle();
            args.putString("android.os.storage.extra.FS_UUID", key);
            startFragment(this, PrivateVolumeForget.class.getCanonicalName(), R.string.storage_menu_forget, 0, args);
            return true;
        }
    }
}
