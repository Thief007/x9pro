package com.android.settings.vpn2;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.IConnectivityManager.Stub;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;

public class ConfigDialogFragment extends DialogFragment implements OnClickListener {
    private final IConnectivityManager mService = Stub.asInterface(ServiceManager.getService("connectivity"));
    private boolean mUnlocking = false;

    public static void show(VpnSettings parent, VpnProfile profile, boolean edit, boolean exists) {
        if (parent.isAdded()) {
            Bundle args = new Bundle();
            args.putParcelable("profile", profile);
            args.putBoolean("editing", edit);
            args.putBoolean("exists", exists);
            ConfigDialogFragment frag = new ConfigDialogFragment();
            frag.setArguments(args);
            frag.setTargetFragment(parent, 0);
            frag.show(parent.getFragmentManager(), "vpnconfigdialog");
        }
    }

    public void onResume() {
        boolean z = false;
        super.onResume();
        if (KeyStore.getInstance().isUnlocked()) {
            this.mUnlocking = false;
            return;
        }
        if (this.mUnlocking) {
            dismiss();
        } else {
            Credentials.getInstance().unlock(getActivity());
        }
        if (!this.mUnlocking) {
            z = true;
        }
        this.mUnlocking = z;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        return new ConfigDialog(getActivity(), this, (VpnProfile) args.getParcelable("profile"), args.getBoolean("editing"), args.getBoolean("exists"));
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        ConfigDialog dialog = (ConfigDialog) getDialog();
        VpnProfile profile = dialog.getProfile();
        KeyStore keyStore = KeyStore.getInstance();
        String lockdownKey = LockdownConfigFragment.getStringOrNull(keyStore, "LOCKDOWN_VPN");
        if (button == -1) {
            KeyStore.getInstance().put("VPN_" + profile.key, profile.encode(), -1, 1);
            if (lockdownKey == null) {
                disconnect(profile);
            }
            if (dialog.isEditing()) {
                if (TextUtils.equals(profile.key, lockdownKey) && !profile.isValidLockdownProfile()) {
                    keyStore.delete("LOCKDOWN_VPN");
                    ConnectivityManager.from(getActivity()).updateLockdownVpn();
                    Toast.makeText(getActivity(), R.string.vpn_lockdown_config_error, 1).show();
                }
            } else if (lockdownKey == null) {
                try {
                    connect(profile);
                } catch (RemoteException e) {
                    Log.e("ConfigDialogFragment", "Failed to connect", e);
                }
            } else {
                Toast.makeText(getActivity(), R.string.lockdown_vpn_already_connected, 1).show();
            }
        } else if (button == -3) {
            disconnect(profile);
            KeyStore.getInstance().delete("VPN_" + profile.key, -1);
            if (TextUtils.equals(profile.key, lockdownKey)) {
                keyStore.delete("LOCKDOWN_VPN");
                ConnectivityManager.from(getActivity()).updateLockdownVpn();
            }
        }
        dismiss();
    }

    public void onCancel(DialogInterface dialog) {
        dismiss();
        super.onCancel(dialog);
    }

    private void connect(VpnProfile profile) throws RemoteException {
        try {
            this.mService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, 1).show();
        }
    }

    private void disconnect(VpnProfile profile) {
        try {
            LegacyVpnInfo connected = this.mService.getLegacyVpnInfo(UserHandle.myUserId());
            if (connected != null && profile.key.equals(connected.key)) {
                this.mService.prepareVpn("[Legacy VPN]", "[Legacy VPN]", UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            Log.e("ConfigDialogFragment", "Failed to disconnect", e);
        }
    }
}
