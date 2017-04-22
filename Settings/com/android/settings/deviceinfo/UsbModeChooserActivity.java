package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.setupwizardlib.R$styleable;

public class UsbModeChooserActivity extends Activity {
    public static final int[] DEFAULT_MODES = new int[]{0, 1, 2, 4, 6, 8, 10};
    private UsbBackend mBackend;
    private AlertDialog mDialog;
    private LayoutInflater mLayoutInflater;

    class C03571 implements OnDismissListener {
        C03571() {
        }

        public void onDismiss(DialogInterface dialog) {
            UsbModeChooserActivity.this.finish();
        }
    }

    class C03582 implements OnClickListener {
        C03582() {
        }

        public void onClick(DialogInterface dialog, int which) {
            UsbModeChooserActivity.this.finish();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mLayoutInflater = LayoutInflater.from(this);
        this.mDialog = new Builder(this).setTitle(R.string.usb_use).setView(R.layout.usb_dialog_container).setOnDismissListener(new C03571()).setNegativeButton(R.string.cancel, new C03582()).create();
        this.mDialog.show();
        LinearLayout container = (LinearLayout) this.mDialog.findViewById(R.id.container);
        this.mBackend = new UsbBackend(this);
        int current = this.mBackend.getCurrentMode();
        for (int i = 0; i < DEFAULT_MODES.length; i++) {
            if (this.mBackend.isModeSupported(DEFAULT_MODES[i])) {
                inflateOption(DEFAULT_MODES[i], current == DEFAULT_MODES[i], container);
            }
        }
    }

    private void inflateOption(final int mode, boolean selected, LinearLayout container) {
        View v = this.mLayoutInflater.inflate(R.layout.radio_with_summary, container, false);
        ((TextView) v.findViewById(16908310)).setText(getTitle(mode));
        ((TextView) v.findViewById(16908304)).setText(getSummary(mode));
        v.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!ActivityManager.isUserAMonkey()) {
                    UsbModeChooserActivity.this.mBackend.setMode(mode);
                }
                UsbModeChooserActivity.this.mDialog.dismiss();
                UsbModeChooserActivity.this.finish();
            }
        });
        ((Checkable) v).setChecked(selected);
        container.addView(v);
    }

    private static int getSummary(int mode) {
        switch (mode) {
            case 0:
                return R.string.usb_use_charging_only_desc;
            case 1:
                return R.string.usb_use_power_only_desc;
            case 2:
                return R.string.usb_use_file_transfers_desc;
            case 4:
                return R.string.usb_use_photo_transfers_desc;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                return R.string.usb_use_MIDI_desc;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                return R.string.usb_ums_summary;
            case 10:
                return R.string.usb_bicr_summary;
            default:
                return 0;
        }
    }

    private static int getTitle(int mode) {
        switch (mode) {
            case 0:
                return R.string.usb_use_charging_only;
            case 1:
                return R.string.usb_use_power_only;
            case 2:
                return R.string.usb_use_file_transfers;
            case 4:
                return R.string.usb_use_photo_transfers;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                return R.string.usb_use_MIDI;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                return R.string.usb_use_mass_storage;
            case 10:
                return R.string.usb_use_built_in_cd_rom;
            default:
                return 0;
        }
    }
}
