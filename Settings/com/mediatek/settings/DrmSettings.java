package com.mediatek.settings;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.drm.OmaDrmClient;

public class DrmSettings extends SettingsPreferenceFragment {
    private static OmaDrmClient sClient;
    private static Preference sPreferenceReset;
    private Context mContext;

    class C07221 implements OnClickListener {
        C07221() {
        }

        public void onClick(DialogInterface dialog, int whichButton) {
            if (DrmSettings.sClient != null) {
                if (DrmSettings.sClient.removeAllRights() == 0) {
                    Toast.makeText(DrmSettings.this.mContext, R.string.drm_reset_toast_msg, 0).show();
                    DrmSettings.sPreferenceReset.setEnabled(false);
                } else {
                    Log.d("DrmSettings", "removeAllRights fail!");
                }
                DrmSettings.sClient = null;
            }
        }
    }

    protected int getMetricsCategory() {
        return 81;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.drm_settings);
        sPreferenceReset = findPreference("drm_settings");
        this.mContext = getActivity();
        sClient = new OmaDrmClient(this.mContext);
    }

    public Dialog onCreateDialog(int id) {
        Builder builder = new Builder(this.mContext);
        switch (id) {
            case 1000:
                builder.setMessage(getResources().getString(R.string.drm_reset_dialog_msg));
                builder.setTitle(getResources().getString(R.string.drm_settings_title));
                builder.setIcon(17301543);
                builder.setPositiveButton(17039370, new C07221());
                builder.setNegativeButton(17039360, null);
                return builder.create();
            default:
                return null;
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == sPreferenceReset) {
            showDialog(1000);
        }
        return false;
    }

    public void onDestroy() {
        super.onDestroy();
        sClient = null;
    }
}
