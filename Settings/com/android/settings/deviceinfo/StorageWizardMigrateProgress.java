package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;

public class StorageWizardMigrateProgress extends StorageWizardBase {
    private final MoveCallback mCallback = new C03541();
    private int mMoveId;

    class C03541 extends MoveCallback {
        C03541() {
        }

        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (StorageWizardMigrateProgress.this.mMoveId == moveId) {
                Context context = StorageWizardMigrateProgress.this;
                if (PackageManager.isMoveStatusFinished(status)) {
                    Log.d("StorageSettings", "Finished with status " + status);
                    if (status != -100) {
                        Toast.makeText(context, StorageWizardMigrateProgress.this.getString(R.string.insufficient_storage), 1).show();
                    } else if (StorageWizardMigrateProgress.this.mDisk != null) {
                        Intent finishIntent = new Intent("com.android.systemui.action.FINISH_WIZARD");
                        finishIntent.addFlags(1073741824);
                        StorageWizardMigrateProgress.this.sendBroadcast(finishIntent);
                        Intent intent = new Intent(context, StorageWizardReady.class);
                        intent.putExtra("android.os.storage.extra.DISK_ID", StorageWizardMigrateProgress.this.mDisk.getId());
                        StorageWizardMigrateProgress.this.startActivity(intent);
                    }
                    StorageWizardMigrateProgress.this.finishAffinity();
                } else {
                    StorageWizardMigrateProgress.this.setCurrentProgress(status);
                }
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mVolume == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);
        this.mMoveId = getIntent().getIntExtra("android.content.pm.extra.MOVE_ID", -1);
        String descrip = this.mStorage.getBestVolumeDescription(this.mVolume);
        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_migrate_progress_title, descrip);
        setBodyText(R.string.storage_wizard_migrate_details, descrip);
        getNextButton().setVisibility(8);
        getPackageManager().registerMoveCallback(this.mCallback, new Handler());
        this.mCallback.onStatusChanged(this.mMoveId, getPackageManager().getMoveStatus(this.mMoveId), -1);
    }
}
