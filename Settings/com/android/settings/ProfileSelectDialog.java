package com.android.settings;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.dashboard.DashboardTile;

public class ProfileSelectDialog extends DialogFragment implements OnClickListener {
    private DashboardTile mSelectedTile;

    public static void show(FragmentManager manager, DashboardTile tile) {
        ProfileSelectDialog dialog = new ProfileSelectDialog();
        Bundle args = new Bundle();
        args.putParcelable("selectedTile", tile);
        dialog.setArguments(args);
        dialog.show(manager, "select_profile");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSelectedTile = (DashboardTile) getArguments().getParcelable("selectedTile");
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        Builder builder = new Builder(context);
        builder.setTitle(R.string.choose_profile).setAdapter(Utils.createUserAdapter(UserManager.get(context), context, this.mSelectedTile.userHandle), this);
        return builder.create();
    }

    public void onClick(DialogInterface dialog, int which) {
        getActivity().startActivityAsUser(this.mSelectedTile.intent, (UserHandle) this.mSelectedTile.userHandle.get(which));
    }
}
