package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import com.android.settings.R;

class AppDialog extends AlertDialog implements OnClickListener {
    private final String mLabel;
    private final Listener mListener;
    private final PackageInfo mPackageInfo;

    public interface Listener {
        void onForget(DialogInterface dialogInterface);
    }

    AppDialog(Context context, Listener listener, PackageInfo pkgInfo, String label) {
        super(context);
        this.mListener = listener;
        this.mPackageInfo = pkgInfo;
        this.mLabel = label;
    }

    protected void onCreate(Bundle savedState) {
        setTitle(this.mLabel);
        setMessage(getContext().getString(R.string.vpn_version, new Object[]{this.mPackageInfo.versionName}));
        createButtons();
        super.onCreate(savedState);
    }

    protected void createButtons() {
        Context context = getContext();
        setButton(-2, context.getString(R.string.vpn_forget), this);
        setButton(-1, context.getString(R.string.vpn_done), this);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -2) {
            this.mListener.onForget(dialog);
        }
        dismiss();
    }
}
