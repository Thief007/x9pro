package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.deviceinfo.StorageSettings.MountTask;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;
import java.io.File;
import java.util.Objects;

public class PublicVolumeSettings extends SettingsPreferenceFragment {
    private DiskInfo mDisk;
    private Preference mFormatPrivate;
    private Preference mFormatPublic;
    private boolean mIsPermittedToAdopt;
    private Preference mMount;
    private final StorageEventListener mStorageListener = new C03331();
    private StorageManager mStorageManager;
    private StorageSummaryPreference mSummary;
    private Preference mUnmount;
    private VolumeInfo mVolume;
    private String mVolumeId;

    class C03331 extends StorageEventListener {
        C03331() {
        }

        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (Objects.equals(PublicVolumeSettings.this.mVolume.getId(), vol.getId())) {
                PublicVolumeSettings.this.mVolume = vol;
                PublicVolumeSettings.this.update();
            }
        }

        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (Objects.equals(PublicVolumeSettings.this.mVolume.getFsUuid(), rec.getFsUuid())) {
                PublicVolumeSettings.this.mVolume = PublicVolumeSettings.this.mStorageManager.findVolumeById(PublicVolumeSettings.this.mVolumeId);
                PublicVolumeSettings.this.update();
            }
        }
    }

    private boolean isVolumeValid() {
        if (this.mVolume == null || this.mVolume.getType() != 0) {
            return false;
        }
        return this.mVolume.isMountedReadable();
    }

    protected int getMetricsCategory() {
        return 42;
    }

    public void onCreate(Bundle icicle) {
        boolean z = false;
        super.onCreate(icicle);
        Context context = getActivity();
        if (UserManager.get(context).isAdminUser() && !ActivityManager.isUserAMonkey()) {
            z = true;
        }
        this.mIsPermittedToAdopt = z;
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        if ("android.provider.action.DOCUMENT_ROOT_SETTINGS".equals(getActivity().getIntent().getAction())) {
            this.mVolume = this.mStorageManager.findVolumeByUuid(DocumentsContract.getRootId(getActivity().getIntent().getData()));
        } else {
            this.mVolume = this.mStorageManager.findVolumeById(getArguments().getString("android.os.storage.extra.VOLUME_ID"));
        }
        if (isVolumeValid()) {
            this.mDisk = this.mStorageManager.findDiskById(this.mVolume.getDiskId());
            Preconditions.checkNotNull(this.mDisk);
            this.mVolumeId = this.mVolume.getId();
            addPreferencesFromResource(R.xml.device_info_storage_volume);
            getPreferenceScreen().setOrderingAsAdded(true);
            this.mSummary = new StorageSummaryPreference(context);
            this.mMount = buildAction(R.string.storage_menu_mount);
            this.mUnmount = buildAction(R.string.storage_menu_unmount);
            this.mFormatPublic = buildAction(R.string.storage_menu_format);
            if (this.mIsPermittedToAdopt) {
                this.mFormatPrivate = buildAction(R.string.storage_menu_format_private);
            }
            return;
        }
        getActivity().finish();
    }

    public void update() {
        if (isVolumeValid()) {
            getActivity().setTitle(this.mStorageManager.getBestVolumeDescription(this.mVolume));
            Context context = getActivity();
            getPreferenceScreen().removeAll();
            if (this.mVolume.isMountedReadable()) {
                addPreference(this.mSummary);
                File file = this.mVolume.getPath();
                long totalBytes = file.getTotalSpace();
                long usedBytes = totalBytes - file.getFreeSpace();
                BytesResult result = Formatter.formatBytes(getResources(), usedBytes, 0);
                this.mSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), new CharSequence[]{result.value, result.units}));
                this.mSummary.setSummary(getString(R.string.storage_volume_used, new Object[]{"16 GB"}));
                this.mSummary.setPercent((int) ((100 * usedBytes) / totalBytes));
            }
            if (this.mVolume.getState() == 0) {
                addPreference(this.mMount);
            }
            if (this.mVolume.isMountedReadable()) {
                addPreference(this.mUnmount);
            }
            addPreference(this.mFormatPublic);
            if (this.mDisk.isAdoptable() && this.mIsPermittedToAdopt) {
                addPreference(this.mFormatPrivate);
            }
            return;
        }
        getActivity().finish();
    }

    private void addPreference(Preference pref) {
        pref.setOrder(Integer.MAX_VALUE);
        getPreferenceScreen().addPreference(pref);
    }

    private Preference buildAction(int titleRes) {
        Preference pref = new Preference(getActivity());
        pref.setTitle(titleRes);
        return pref;
    }

    public void onResume() {
        super.onResume();
        this.mVolume = this.mStorageManager.findVolumeById(this.mVolumeId);
        if (isVolumeValid()) {
            this.mStorageManager.registerListener(this.mStorageListener);
            update();
            return;
        }
        getActivity().finish();
    }

    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
        Context context = getActivity();
        if (pref == this.mMount) {
            new MountTask(context, this.mVolume).execute(new Void[0]);
        } else if (pref == this.mUnmount) {
            new UnmountTask(context, this.mVolume).execute(new Void[0]);
        } else if (pref == this.mFormatPublic) {
            intent = new Intent(context, StorageWizardFormatConfirm.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            intent.putExtra("format_private", false);
            startActivity(intent);
        } else if (pref == this.mFormatPrivate) {
            intent = new Intent(context, StorageWizardFormatConfirm.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            intent.putExtra("format_private", true);
            startActivity(intent);
        }
        return super.onPreferenceTreeClick(preferenceScreen, pref);
    }
}
