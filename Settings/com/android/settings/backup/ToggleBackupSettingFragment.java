package com.android.settings.backup;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.app.backup.IBackupManager.Stub;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;

public class ToggleBackupSettingFragment extends SettingsPreferenceFragment implements OnClickListener, OnDismissListener {
    private IBackupManager mBackupManager;
    private Dialog mConfirmDialog;
    private Preference mSummaryPreference;
    protected SwitchBar mSwitchBar;
    protected ToggleSwitch mToggleSwitch;
    private boolean mWaitingForConfirmationDialog = false;

    class C02902 implements OnBeforeCheckedChangeListener {
        C02902() {
        }

        public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
            if (checked) {
                ToggleBackupSettingFragment.this.setBackupEnabled(true);
                ToggleBackupSettingFragment.this.mSwitchBar.setCheckedInternal(true);
                return true;
            }
            ToggleBackupSettingFragment.this.showEraseBackupDialog();
            return true;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mBackupManager = Stub.asInterface(ServiceManager.getService("backup"));
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(preferenceScreen);
        this.mSummaryPreference = new Preference(getActivity()) {
            protected void onBindView(View view) {
                super.onBindView(view);
                ((TextView) view.findViewById(16908304)).setText(getSummary());
            }
        };
        this.mSummaryPreference.setPersistent(false);
        this.mSummaryPreference.setLayoutResource(R.layout.text_description_preference);
        preferenceScreen.addPreference(this.mSummaryPreference);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        if (Secure.getInt(getContentResolver(), "user_full_data_backup_aware", 0) != 0) {
            this.mSummaryPreference.setSummary(R.string.fullbackup_data_summary);
        } else {
            this.mSummaryPreference.setSummary(R.string.backup_data_summary);
        }
        try {
            this.mSwitchBar.setCheckedInternal(this.mBackupManager == null ? false : this.mBackupManager.isBackupEnabled());
        } catch (RemoteException e) {
            this.mSwitchBar.setEnabled(false);
        }
        getActivity().setTitle(R.string.backup_data_title);
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        this.mSwitchBar.hide();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new C02902());
        this.mSwitchBar.show();
    }

    public void onStop() {
        if (this.mConfirmDialog != null && this.mConfirmDialog.isShowing()) {
            this.mConfirmDialog.dismiss();
        }
        this.mConfirmDialog = null;
        super.onStop();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            this.mWaitingForConfirmationDialog = false;
            setBackupEnabled(false);
            this.mSwitchBar.setCheckedInternal(false);
        } else if (which == -2) {
            this.mWaitingForConfirmationDialog = false;
            setBackupEnabled(true);
            this.mSwitchBar.setCheckedInternal(true);
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (this.mWaitingForConfirmationDialog) {
            setBackupEnabled(true);
            this.mSwitchBar.setCheckedInternal(true);
        }
    }

    private void showEraseBackupDialog() {
        CharSequence msg;
        if (Secure.getInt(getContentResolver(), "user_full_data_backup_aware", 0) != 0) {
            msg = getResources().getText(R.string.fullbackup_erase_dialog_message);
        } else {
            msg = getResources().getText(R.string.backup_erase_dialog_message);
        }
        this.mWaitingForConfirmationDialog = true;
        this.mConfirmDialog = new Builder(getActivity()).setMessage(msg).setTitle(R.string.backup_erase_dialog_title).setPositiveButton(17039370, this).setNegativeButton(17039360, this).setOnDismissListener(this).show();
    }

    protected int getMetricsCategory() {
        return 81;
    }

    private void setBackupEnabled(boolean enable) {
        if (this.mBackupManager != null) {
            try {
                this.mBackupManager.setBackupEnabled(enable);
            } catch (RemoteException e) {
                Log.e("ToggleBackupSettingFragment", "Error communicating with BackupManager", e);
            }
        }
    }
}
