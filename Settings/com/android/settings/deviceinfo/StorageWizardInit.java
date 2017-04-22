package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import com.android.settings.R;

public class StorageWizardInit extends StorageWizardBase {
    private boolean mIsPermittedToAdopt;
    private RadioButton mRadioExternal;
    private RadioButton mRadioInternal;
    private final OnCheckedChangeListener mRadioListener = new C03501();

    class C03501 implements OnCheckedChangeListener {
        C03501() {
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (buttonView == StorageWizardInit.this.mRadioExternal) {
                    StorageWizardInit.this.mRadioInternal.setChecked(false);
                    StorageWizardInit.this.setIllustrationInternal(false);
                } else if (buttonView == StorageWizardInit.this.mRadioInternal) {
                    StorageWizardInit.this.mRadioExternal.setChecked(false);
                    StorageWizardInit.this.setIllustrationInternal(true);
                }
                StorageWizardInit.this.getNextButton().setEnabled(true);
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_init);
        boolean z = UserManager.get(this).isAdminUser() ? !ActivityManager.isUserAMonkey() : false;
        this.mIsPermittedToAdopt = z;
        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_init_title, this.mDisk.getDescription());
        this.mRadioExternal = (RadioButton) findViewById(R.id.storage_wizard_init_external_title);
        this.mRadioInternal = (RadioButton) findViewById(R.id.storage_wizard_init_internal_title);
        this.mRadioExternal.setOnCheckedChangeListener(this.mRadioListener);
        this.mRadioInternal.setOnCheckedChangeListener(this.mRadioListener);
        findViewById(R.id.storage_wizard_init_external_summary).setPadding(this.mRadioExternal.getCompoundPaddingLeft(), 0, this.mRadioExternal.getCompoundPaddingRight(), 0);
        findViewById(R.id.storage_wizard_init_internal_summary).setPadding(this.mRadioExternal.getCompoundPaddingLeft(), 0, this.mRadioExternal.getCompoundPaddingRight(), 0);
        getNextButton().setEnabled(false);
        if (!this.mDisk.isAdoptable()) {
            this.mRadioExternal.setChecked(true);
            onNavigateNext();
            finish();
        }
        if (!this.mIsPermittedToAdopt) {
            this.mRadioInternal.setEnabled(false);
        }
    }

    public void onNavigateNext() {
        Intent intent;
        if (this.mRadioExternal.isChecked()) {
            if (this.mVolume == null || this.mVolume.getType() != 0 || this.mVolume.getState() == 6) {
                intent = new Intent(this, StorageWizardFormatConfirm.class);
                intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
                intent.putExtra("format_private", false);
                startActivity(intent);
                return;
            }
            this.mStorage.setVolumeInited(this.mVolume.getFsUuid(), true);
            intent = new Intent(this, StorageWizardReady.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            startActivity(intent);
        } else if (this.mRadioInternal.isChecked()) {
            intent = new Intent(this, StorageWizardFormatConfirm.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            intent.putExtra("format_private", true);
            startActivity(intent);
        }
    }
}
