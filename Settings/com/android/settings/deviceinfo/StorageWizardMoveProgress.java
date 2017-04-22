package com.android.settings.deviceinfo;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;

public class StorageWizardMoveProgress extends StorageWizardBase {
    private final MoveCallback mCallback = new C03551();
    private int mMoveId;

    class C03551 extends MoveCallback {
        C03551() {
        }

        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (StorageWizardMoveProgress.this.mMoveId == moveId) {
                if (PackageManager.isMoveStatusFinished(status)) {
                    Log.d("StorageSettings", "Finished with status " + status);
                    if (status != -100) {
                        Toast.makeText(StorageWizardMoveProgress.this, StorageWizardMoveProgress.this.moveStatusToMessage(status), 1).show();
                    }
                    StorageWizardMoveProgress.this.finishAffinity();
                } else {
                    StorageWizardMoveProgress.this.setCurrentProgress(status);
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
        String appName = getIntent().getStringExtra("android.intent.extra.TITLE");
        String volumeName = this.mStorage.getBestVolumeDescription(this.mVolume);
        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_move_progress_title, appName);
        setBodyText(R.string.storage_wizard_move_progress_body, volumeName, appName);
        getNextButton().setVisibility(8);
        getPackageManager().registerMoveCallback(this.mCallback, new Handler());
        this.mCallback.onStatusChanged(this.mMoveId, getPackageManager().getMoveStatus(this.mMoveId), -1);
    }

    protected void onDestroy() {
        super.onDestroy();
        getPackageManager().unregisterMoveCallback(this.mCallback);
    }

    private CharSequence moveStatusToMessage(int returnCode) {
        switch (returnCode) {
            case -5:
                return getString(R.string.invalid_location);
            case -4:
                return getString(R.string.app_forward_locked);
            case -3:
                return getString(R.string.system_package);
            case -2:
                return getString(R.string.does_not_exist);
            case -1:
                return getString(R.string.insufficient_storage);
            default:
                return getString(R.string.insufficient_storage);
        }
    }
}
