package com.android.settings.deviceinfo;

import android.content.Intent;
import android.os.Bundle;
import com.android.settings.R;

public class StorageWizardFormatConfirm extends StorageWizardBase {
    private boolean mFormatPrivate;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);
        this.mFormatPrivate = getIntent().getBooleanExtra("format_private", false);
        setIllustrationInternal(this.mFormatPrivate);
        if (this.mFormatPrivate) {
            setHeaderText(R.string.storage_wizard_format_confirm_title, new String[0]);
            setBodyText(R.string.storage_wizard_format_confirm_body, this.mDisk.getDescription());
        } else {
            setHeaderText(R.string.storage_wizard_format_confirm_public_title, new String[0]);
            setBodyText(R.string.storage_wizard_format_confirm_public_body, this.mDisk.getDescription());
        }
        getNextButton().setText(R.string.storage_wizard_format_confirm_next);
        getNextButton().setBackgroundTintList(getColorStateList(R.color.storage_wizard_button_red));
    }

    public void onNavigateNext() {
        Intent intent = new Intent(this, StorageWizardFormatProgress.class);
        intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
        intent.putExtra("format_private", this.mFormatPrivate);
        intent.putExtra("forget_uuid", getIntent().getStringExtra("forget_uuid"));
        startActivity(intent);
        finishAffinity();
    }
}
