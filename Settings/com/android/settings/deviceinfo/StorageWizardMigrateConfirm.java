package com.android.settings.deviceinfo;

import android.content.Intent;
import android.os.Bundle;
import android.os.storage.VolumeInfo;
import android.util.Log;
import com.android.settings.R;

public class StorageWizardMigrateConfirm extends StorageWizardBase {
    private MigrateEstimateTask mEstimate;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_generic);
        if (this.mVolume == null) {
            this.mVolume = findFirstVolume(1);
        }
        VolumeInfo sourceVol = getPackageManager().getPrimaryStorageCurrentVolume();
        if (sourceVol == null || this.mVolume == null) {
            Log.d("StorageSettings", "Missing either source or target volume");
            finish();
            return;
        }
        final String sourceDescrip = this.mStorage.getBestVolumeDescription(sourceVol);
        String targetDescrip = this.mStorage.getBestVolumeDescription(this.mVolume);
        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_migrate_confirm_title, targetDescrip);
        setBodyText(R.string.memory_calculating_size, new String[0]);
        setSecondaryBodyText(R.string.storage_wizard_migrate_details, targetDescrip);
        this.mEstimate = new MigrateEstimateTask(this) {
            public void onPostExecute(String size, String time) {
                StorageWizardMigrateConfirm.this.setBodyText(R.string.storage_wizard_migrate_confirm_body, time, size, sourceDescrip);
            }
        };
        this.mEstimate.copyFrom(getIntent());
        this.mEstimate.execute(new Void[0]);
        getNextButton().setText(R.string.storage_wizard_migrate_confirm_next);
    }

    public void onNavigateNext() {
        int moveId = getPackageManager().movePrimaryStorage(this.mVolume);
        Intent intent = new Intent(this, StorageWizardMigrateProgress.class);
        intent.putExtra("android.os.storage.extra.VOLUME_ID", this.mVolume.getId());
        intent.putExtra("android.content.pm.extra.MOVE_ID", moveId);
        startActivity(intent);
        finishAffinity();
    }
}
