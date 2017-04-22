package com.android.settings.deviceinfo;

import android.os.Bundle;
import android.os.storage.StorageEventListener;
import android.os.storage.VolumeInfo;
import android.util.Log;
import com.android.settings.R;
import java.util.Objects;

public class StorageWizardReady extends StorageWizardBase {
    private final StorageEventListener mStorageMountListener = new C03561();

    class C03561 extends StorageEventListener {
        C03561() {
        }

        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            Log.d("StorageWizardReady", "onVolumeStateChanged, disk : " + vol.getDiskId() + ", type : " + vol.getType() + ", state : " + vol.getState());
            if (Objects.equals(StorageWizardReady.this.mDisk.getId(), vol.getDiskId()) && vol.getType() == 0 && newState == 2) {
                StorageWizardReady.this.setIllustrationInternal(false);
                StorageWizardReady.this.setBodyText(R.string.storage_wizard_ready_external_body, StorageWizardReady.this.mDisk.getDescription());
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);
        setHeaderText(R.string.storage_wizard_ready_title, this.mDisk.getDescription());
        VolumeInfo publicVol = findFirstVolume(0);
        VolumeInfo privateVol = findFirstVolume(1);
        Log.d("StorageWizardReady", "onCreate(), publicVol : " + publicVol + " privateVol : " + privateVol);
        if (publicVol != null) {
            setIllustrationInternal(false);
            setBodyText(R.string.storage_wizard_ready_external_body, this.mDisk.getDescription());
        } else if (privateVol != null) {
            setIllustrationInternal(true);
            setBodyText(R.string.storage_wizard_ready_internal_body, this.mDisk.getDescription());
        }
        getNextButton().setText(R.string.done);
        this.mStorage.registerListener(this.mStorageMountListener);
    }

    public void onNavigateNext() {
        finishAffinity();
    }

    protected void onDestroy() {
        this.mStorage.unregisterListener(this.mStorageMountListener);
        super.onDestroy();
    }
}
