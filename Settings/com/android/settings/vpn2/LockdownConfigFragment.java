package com.android.settings.vpn2;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.security.KeyStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

public class LockdownConfigFragment extends DialogFragment {
    private int mCurrentIndex;
    private List<VpnProfile> mProfiles;
    private List<CharSequence> mTitles;

    private static class TitleAdapter extends ArrayAdapter<CharSequence> {
        public TitleAdapter(Context context, List<CharSequence> objects) {
            super(context, 17367258, 16908308, objects);
        }
    }

    public static void show(VpnSettings parent) {
        if (parent.isAdded()) {
            new LockdownConfigFragment().show(parent.getFragmentManager(), "lockdown");
        }
    }

    protected static String getStringOrNull(KeyStore keyStore, String key) {
        String str = null;
        if (!keyStore.isUnlocked()) {
            return null;
        }
        byte[] value = keyStore.get(key);
        if (value != null) {
            str = new String(value);
        }
        return str;
    }

    private void initProfiles(KeyStore keyStore, Resources res) {
        String lockdownKey = getStringOrNull(keyStore, "LOCKDOWN_VPN");
        this.mProfiles = VpnSettings.loadVpnProfiles(keyStore, new int[0]);
        this.mTitles = new ArrayList(this.mProfiles.size() + 1);
        this.mTitles.add(res.getText(R.string.vpn_lockdown_none));
        this.mCurrentIndex = 0;
        for (VpnProfile profile : this.mProfiles) {
            if (TextUtils.equals(profile.key, lockdownKey)) {
                this.mCurrentIndex = this.mTitles.size();
            }
            this.mTitles.add(profile.name);
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        final KeyStore keyStore = KeyStore.getInstance();
        initProfiles(keyStore, context.getResources());
        Builder builder = new Builder(context);
        LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
        builder.setTitle(R.string.vpn_menu_lockdown);
        View view = dialogInflater.inflate(R.layout.vpn_lockdown_editor, null, false);
        final ListView listView = (ListView) view.findViewById(16908298);
        listView.setChoiceMode(1);
        listView.setAdapter(new TitleAdapter(context, this.mTitles));
        listView.setItemChecked(this.mCurrentIndex, true);
        builder.setView(view);
        builder.setPositiveButton(17039370, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int newIndex = listView.getCheckedItemPosition();
                if (LockdownConfigFragment.this.mCurrentIndex != newIndex) {
                    if (newIndex == 0) {
                        keyStore.delete("LOCKDOWN_VPN");
                    } else {
                        VpnProfile profile = (VpnProfile) LockdownConfigFragment.this.mProfiles.get(newIndex - 1);
                        if (profile.isValidLockdownProfile()) {
                            keyStore.put("LOCKDOWN_VPN", profile.key.getBytes(), -1, 1);
                        } else {
                            Toast.makeText(context, R.string.vpn_lockdown_config_error, 1).show();
                            return;
                        }
                    }
                    ConnectivityManager.from(LockdownConfigFragment.this.getActivity()).updateLockdownVpn();
                }
            }
        });
        return builder.create();
    }
}
