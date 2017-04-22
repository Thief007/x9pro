package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.Preference;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.android.settings.R;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;
import java.io.File;

public class StorageVolumePreference extends Preference {
    private int mColor;
    private final StorageManager mStorageManager;
    private final OnClickListener mUnmountListener = new C03461();
    private int mUsedPercent = -1;
    private final VolumeInfo mVolume;

    class C03461 implements OnClickListener {
        C03461() {
        }

        public void onClick(View v) {
            new UnmountTask(StorageVolumePreference.this.getContext(), StorageVolumePreference.this.mVolume).execute(new Void[0]);
        }
    }

    public StorageVolumePreference(Context context, VolumeInfo volume, int color) {
        Drawable icon;
        super(context);
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        this.mVolume = volume;
        this.mColor = color;
        setLayoutResource(R.layout.storage_volume);
        setKey(volume.getId());
        setTitle(this.mStorageManager.getBestVolumeDescription(volume));
        if ("private".equals(volume.getId())) {
            icon = context.getDrawable(R.drawable.ic_settings_storage);
        } else {
            icon = context.getDrawable(R.drawable.ic_sim_sd);
        }
        if (volume.isMountedReadable()) {
            File path = volume.getPath();
            long freeBytes = path.getFreeSpace();
            long totalBytes = path.getTotalSpace();
            long usedBytes = totalBytes - freeBytes;
            String used = Formatter.formatFileSize(context, usedBytes);
            String total = Formatter.formatFileSize(context, totalBytes);
            if ("private".equals(volume.getId())) {
                setSummary(context.getString(R.string.storage_volume_summary, new Object[]{used, "16 GB"}));
            } else {
                setSummary(context.getString(R.string.storage_volume_summary, new Object[]{used, total}));
            }
            if (totalBytes != 0) {
                this.mUsedPercent = (int) ((100 * usedBytes) / totalBytes);
            }
            if (freeBytes < this.mStorageManager.getStorageLowBytes(path)) {
                this.mColor = StorageSettings.COLOR_WARNING;
                icon = context.getDrawable(R.drawable.ic_warning_24dp);
            }
        } else {
            setSummary(volume.getStateDescription());
            this.mUsedPercent = -1;
        }
        icon.mutate();
        icon.setTint(this.mColor);
        setIcon(icon);
        if (volume.getType() == 0 && volume.isMountedReadable()) {
            setWidgetLayoutResource(R.layout.preference_storage_action);
        }
    }

    protected void onBindView(View view) {
        ImageView unmount = (ImageView) view.findViewById(R.id.unmount);
        if (unmount != null) {
            unmount.setImageTintList(ColorStateList.valueOf(Color.parseColor("#8a000000")));
            unmount.setOnClickListener(this.mUnmountListener);
        }
        ProgressBar progress = (ProgressBar) view.findViewById(16908301);
        if (this.mVolume.getType() != 1 || this.mUsedPercent == -1) {
            progress.setVisibility(8);
        } else {
            progress.setVisibility(0);
            progress.setProgress(this.mUsedPercent);
            progress.setProgressTintList(ColorStateList.valueOf(this.mColor));
        }
        super.onBindView(view);
    }
}
