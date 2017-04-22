package com.mediatek.settings.wfd;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.settings.R;

public final class WfdChangeResolutionFragment extends DialogFragment implements OnClickListener {
    private int mCurrentResolution = 0;
    private int mWhichIndex = 0;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.mCurrentResolution = Global.getInt(getActivity().getContentResolver(), "wifi_display_max_resolution", 0);
        Log.d("@M_WfdChangeResolutionFragment", "create dialog, current resolution is " + this.mCurrentResolution);
        int resolutionIndex = WfdSettingsExt.DEVICE_RESOLUTION_LIST.indexOf(Integer.valueOf(this.mCurrentResolution));
        this.mWhichIndex = resolutionIndex;
        return new Builder(getActivity()).setTitle(R.string.wfd_change_resolution_menu_title).setSingleChoiceItems(R.array.wfd_resolution_entry, resolutionIndex, this).setPositiveButton(17039370, this).create();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            int userChoice = ((Integer) WfdSettingsExt.DEVICE_RESOLUTION_LIST.get(this.mWhichIndex)).intValue();
            Log.d("@M_WfdChangeResolutionFragment", "User click ok button, set resolution as " + userChoice);
            Global.putInt(getActivity().getContentResolver(), "wifi_display_max_resolution", userChoice);
            return;
        }
        this.mWhichIndex = which;
        Log.d("@M_WfdChangeResolutionFragment", "User select the item " + this.mWhichIndex);
    }

    public void onResume() {
        super.onResume();
        if (!WfdSettingsExt.DEVICE_RESOLUTION_LIST.contains(Integer.valueOf(this.mCurrentResolution))) {
            dismiss();
        }
    }
}
