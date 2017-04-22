package com.android.settings.vpn2;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.net.IConnectivityManager;
import android.net.IConnectivityManager.Stub;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.net.VpnConfig;
import com.android.settings.R;
import com.android.settings.vpn2.AppDialog.Listener;

public class AppDialogFragment extends DialogFragment implements Listener {
    private PackageInfo mPackageInfo;
    private final IConnectivityManager mService = Stub.asInterface(ServiceManager.getService("connectivity"));

    class C05701 implements OnClickListener {
        C05701() {
        }

        public void onClick(DialogInterface dialog, int which) {
            AppDialogFragment.this.onDisconnect(dialog);
        }
    }

    public static void show(VpnSettings parent, PackageInfo packageInfo, String label, boolean managing, boolean connected) {
        if (parent.isAdded()) {
            Bundle args = new Bundle();
            args.putParcelable("package", packageInfo);
            args.putString("label", label);
            args.putBoolean("managing", managing);
            args.putBoolean("connected", connected);
            AppDialogFragment frag = new AppDialogFragment();
            frag.setArguments(args);
            frag.setTargetFragment(parent, 0);
            frag.show(parent.getFragmentManager(), "vpnappdialog");
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String label = args.getString("label");
        boolean managing = args.getBoolean("managing");
        boolean connected = args.getBoolean("connected");
        this.mPackageInfo = (PackageInfo) args.getParcelable("package");
        if (managing) {
            return new AppDialog(getActivity(), this, this.mPackageInfo, label);
        }
        Builder dlog = new Builder(getActivity()).setTitle(label).setMessage(getActivity().getString(R.string.vpn_disconnect_confirm)).setNegativeButton(getActivity().getString(R.string.vpn_cancel), null);
        if (connected) {
            dlog.setPositiveButton(getActivity().getString(R.string.vpn_disconnect), new C05701());
        }
        return dlog.create();
    }

    public void onCancel(DialogInterface dialog) {
        dismiss();
        super.onCancel(dialog);
    }

    public void onForget(DialogInterface dialog) {
        int userId = UserHandle.getUserId(this.mPackageInfo.applicationInfo.uid);
        try {
            this.mService.setVpnPackageAuthorization(this.mPackageInfo.packageName, userId, false);
            onDisconnect(dialog);
        } catch (RemoteException e) {
            Log.e("AppDialogFragment", "Failed to forget authorization of " + this.mPackageInfo.packageName + " for user " + userId, e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void onDisconnect(DialogInterface dialog) {
        int userId = UserHandle.getUserId(this.mPackageInfo.applicationInfo.uid);
        try {
            VpnConfig vpnConfig = this.mService.getVpnConfig(userId);
            if (!(vpnConfig == null || vpnConfig.legacy || !this.mPackageInfo.packageName.equals(vpnConfig.user))) {
                this.mService.prepareVpn(this.mPackageInfo.packageName, "[Legacy VPN]", userId);
            }
        } catch (RemoteException e) {
            Log.e("AppDialogFragment", "Failed to disconnect package " + this.mPackageInfo.packageName + " for user " + userId, e);
        }
    }
}
