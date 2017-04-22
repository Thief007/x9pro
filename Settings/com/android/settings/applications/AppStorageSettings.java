package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.AppGlobals;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver.Stub;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.StorageWizardMoveConfirm;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AppStorageSettings extends AppInfoWithHeader implements OnClickListener, Callbacks, DialogInterface.OnClickListener {
    private static final String TAG = AppStorageSettings.class.getSimpleName();
    private Preference mAppSize;
    private Preference mCacheSize;
    private boolean mCanClearData = true;
    private VolumeInfo[] mCandidates;
    private Button mChangeStorageButton;
    private Button mClearCacheButton;
    private ClearCacheObserver mClearCacheObserver;
    private Button mClearDataButton;
    private ClearUserDataObserver mClearDataObserver;
    private CharSequence mComputingStr;
    private Preference mDataSize;
    private Builder mDialogBuilder;
    private Preference mExternalCodeSize;
    private Preference mExternalDataSize;
    private final Handler mHandler = new C02491();
    private boolean mHaveSizes = false;
    private CharSequence mInvalidSizeStr;
    private long mLastCacheSize = -1;
    private long mLastCodeSize = -1;
    private long mLastDataSize = -1;
    private long mLastExternalCodeSize = -1;
    private long mLastExternalDataSize = -1;
    private long mLastTotalSize = -1;
    private Preference mStorageUsed;
    private Preference mTotalSize;

    class C02491 extends Handler {
        C02491() {
        }

        public void handleMessage(Message msg) {
            if (AppStorageSettings.this.getView() != null) {
                switch (msg.what) {
                    case 1:
                        AppStorageSettings.this.processClearMsg(msg);
                        break;
                    case 3:
                        AppStorageSettings.this.mState.requestSize(AppStorageSettings.this.mPackageName, AppStorageSettings.this.mUserId);
                        break;
                }
            }
        }
    }

    class C02502 implements DialogInterface.OnClickListener {
        C02502() {
        }

        public void onClick(DialogInterface dialog, int which) {
            AppStorageSettings.this.initiateClearUserData();
        }
    }

    class C02513 implements DialogInterface.OnClickListener {
        C02513() {
        }

        public void onClick(DialogInterface dialog, int which) {
            AppStorageSettings.this.mClearDataButton.setEnabled(false);
            AppStorageSettings.this.setIntentAndFinish(false, false);
        }
    }

    class ClearCacheObserver extends Stub {
        ClearCacheObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            Message msg = AppStorageSettings.this.mHandler.obtainMessage(3);
            msg.arg1 = succeeded ? 1 : 2;
            AppStorageSettings.this.mHandler.sendMessage(msg);
        }
    }

    class ClearUserDataObserver extends Stub {
        ClearUserDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            int i = 1;
            Message msg = AppStorageSettings.this.mHandler.obtainMessage(1);
            if (!succeeded) {
                i = 2;
            }
            msg.arg1 = i;
            AppStorageSettings.this.mHandler.sendMessage(msg);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_storage_settings);
        setupViews();
    }

    public void onResume() {
        super.onResume();
        this.mState.requestSize(this.mPackageName, this.mUserId);
    }

    private void setupViews() {
        this.mComputingStr = getActivity().getText(R.string.computing_size);
        this.mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
        this.mTotalSize = findPreference("total_size");
        this.mAppSize = findPreference("app_size");
        this.mDataSize = findPreference("data_size");
        this.mExternalCodeSize = findPreference("external_code_size");
        this.mExternalDataSize = findPreference("external_data_size");
        if (Environment.isExternalStorageEmulated()) {
            PreferenceCategory category = (PreferenceCategory) findPreference("storage_category");
            category.removePreference(this.mExternalCodeSize);
            category.removePreference(this.mExternalDataSize);
        }
        this.mClearDataButton = (Button) ((LayoutPreference) findPreference("clear_data_button")).findViewById(R.id.button);
        this.mStorageUsed = findPreference("storage_used");
        this.mChangeStorageButton = (Button) ((LayoutPreference) findPreference("change_storage_button")).findViewById(R.id.button);
        this.mChangeStorageButton.setText(R.string.change);
        this.mChangeStorageButton.setOnClickListener(this);
        this.mCacheSize = findPreference("cache_size");
        this.mClearCacheButton = (Button) ((LayoutPreference) findPreference("clear_cache_button")).findViewById(R.id.button);
        this.mClearCacheButton.setText(R.string.clear_cache_btn_text);
    }

    public void onClick(View v) {
        if (v == this.mClearCacheButton) {
            if (this.mClearCacheObserver == null) {
                this.mClearCacheObserver = new ClearCacheObserver();
            }
            this.mPm.deleteApplicationCacheFiles(this.mPackageName, this.mClearCacheObserver);
        } else if (v == this.mClearDataButton) {
            if (this.mAppEntry.info.manageSpaceActivityName == null) {
                showDialogInner(1, 0);
            } else if (!Utils.isMonkeyRunning()) {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setClassName(this.mAppEntry.info.packageName, this.mAppEntry.info.manageSpaceActivityName);
                startActivityForResult(intent, 2);
            }
        } else if (v == this.mChangeStorageButton && this.mDialogBuilder != null && !isMoveInProgress()) {
            this.mDialogBuilder.show();
        }
    }

    private boolean isMoveInProgress() {
        try {
            return AppGlobals.getPackageManager().isPackageFrozen(this.mPackageName);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        Context context = getActivity();
        VolumeInfo targetVol = this.mCandidates[which];
        if (!Objects.equals(targetVol, context.getPackageManager().getPackageCurrentVolume(this.mAppEntry.info))) {
            Intent intent = new Intent(context, StorageWizardMoveConfirm.class);
            intent.putExtra("android.os.storage.extra.VOLUME_ID", targetVol.getId());
            intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAppEntry.info.packageName);
            startActivity(intent);
        }
        dialog.dismiss();
    }

    private String getSizeStr(long size) {
        if (size == -1) {
            return this.mInvalidSizeStr.toString();
        }
        return Formatter.formatFileSize(getActivity(), size);
    }

    private void refreshSizeInfo() {
        if (this.mAppEntry.size == -2 || this.mAppEntry.size == -1) {
            this.mLastTotalSize = -1;
            this.mLastCacheSize = -1;
            this.mLastDataSize = -1;
            this.mLastCodeSize = -1;
            if (!this.mHaveSizes) {
                this.mAppSize.setSummary(this.mComputingStr);
                this.mDataSize.setSummary(this.mComputingStr);
                this.mCacheSize.setSummary(this.mComputingStr);
                this.mTotalSize.setSummary(this.mComputingStr);
            }
            this.mClearDataButton.setEnabled(false);
            this.mClearCacheButton.setEnabled(false);
        } else {
            this.mHaveSizes = true;
            long codeSize = this.mAppEntry.codeSize;
            long dataSize = this.mAppEntry.dataSize;
            if (Environment.isExternalStorageEmulated()) {
                codeSize += this.mAppEntry.externalCodeSize;
                dataSize += this.mAppEntry.externalDataSize;
            } else {
                if (this.mLastExternalCodeSize != this.mAppEntry.externalCodeSize) {
                    this.mLastExternalCodeSize = this.mAppEntry.externalCodeSize;
                    this.mExternalCodeSize.setSummary(getSizeStr(this.mAppEntry.externalCodeSize));
                }
                if (this.mLastExternalDataSize != this.mAppEntry.externalDataSize) {
                    this.mLastExternalDataSize = this.mAppEntry.externalDataSize;
                    this.mExternalDataSize.setSummary(getSizeStr(this.mAppEntry.externalDataSize));
                }
            }
            if (this.mLastCodeSize != codeSize) {
                this.mLastCodeSize = codeSize;
                this.mAppSize.setSummary(getSizeStr(codeSize));
            }
            if (this.mLastDataSize != dataSize) {
                this.mLastDataSize = dataSize;
                this.mDataSize.setSummary(getSizeStr(dataSize));
            }
            long cacheSize = this.mAppEntry.cacheSize + this.mAppEntry.externalCacheSize;
            if (this.mLastCacheSize != cacheSize) {
                this.mLastCacheSize = cacheSize;
                this.mCacheSize.setSummary(getSizeStr(cacheSize));
            }
            if (this.mLastTotalSize != this.mAppEntry.size) {
                this.mLastTotalSize = this.mAppEntry.size;
                this.mTotalSize.setSummary(getSizeStr(this.mAppEntry.size));
            }
            if (this.mAppEntry.dataSize + this.mAppEntry.externalDataSize <= 0 || !this.mCanClearData) {
                this.mClearDataButton.setEnabled(false);
            } else {
                this.mClearDataButton.setEnabled(true);
                this.mClearDataButton.setOnClickListener(this);
            }
            if (cacheSize <= 0) {
                this.mClearCacheButton.setEnabled(false);
            } else {
                this.mClearCacheButton.setEnabled(true);
                this.mClearCacheButton.setOnClickListener(this);
            }
        }
        if (this.mAppControlRestricted) {
            this.mClearCacheButton.setEnabled(false);
            this.mClearDataButton.setEnabled(false);
        }
    }

    protected boolean refreshUi() {
        retrieveAppEntry();
        if (this.mAppEntry == null) {
            return false;
        }
        refreshSizeInfo();
        StorageManager storage = (StorageManager) getContext().getSystemService(StorageManager.class);
        this.mStorageUsed.setSummary(storage.getBestVolumeDescription(getActivity().getPackageManager().getPackageCurrentVolume(this.mAppEntry.info)));
        refreshButtons();
        return true;
    }

    private void refreshButtons() {
        initMoveDialog();
        initDataButtons();
    }

    private void initDataButtons() {
        if (this.mAppEntry.info.manageSpaceActivityName == null && ((this.mAppEntry.info.flags & 65) == 1 || this.mDpm.packageHasActiveAdmins(this.mPackageName))) {
            this.mClearDataButton.setText(R.string.clear_user_data_text);
            this.mClearDataButton.setEnabled(false);
            this.mCanClearData = false;
        } else {
            if (this.mAppEntry.info.manageSpaceActivityName != null) {
                this.mClearDataButton.setText(R.string.manage_space_text);
                if (((StorageManager) getActivity().getApplicationContext().getSystemService("storage")).getVolumeState(Environment.getLegacyExternalStorageDirectory().getPath()).equals("mounted")) {
                    Log.d(TAG, "/mnt/sdcard is mounted.");
                    this.mClearDataButton.setEnabled(true);
                    this.mCanClearData = true;
                } else {
                    Log.d(TAG, "/mnt/sdcard is not mounted.");
                    if ((this.mAppEntry.info.flags & 262144) != 0) {
                        Log.d(TAG, "ApplicationInfo.FLAG_EXTERNAL_STORAGE");
                        this.mClearDataButton.setEnabled(false);
                        this.mCanClearData = false;
                    }
                }
            } else {
                this.mClearDataButton.setText(R.string.clear_user_data_text);
            }
            this.mClearDataButton.setOnClickListener(this);
        }
        if (this.mAppControlRestricted) {
            this.mClearDataButton.setEnabled(false);
        }
    }

    private void initMoveDialog() {
        Context context = getActivity();
        StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
        List<VolumeInfo> candidates = context.getPackageManager().getPackageCandidateVolumes(this.mAppEntry.info);
        if (candidates.size() > 1) {
            Collections.sort(candidates, VolumeInfo.getDescriptionComparator());
            CharSequence[] labels = new CharSequence[candidates.size()];
            int current = -1;
            for (int i = 0; i < candidates.size(); i++) {
                String volDescrip = storage.getBestVolumeDescription((VolumeInfo) candidates.get(i));
                if (Objects.equals(volDescrip, this.mStorageUsed.getSummary())) {
                    current = i;
                }
                labels[i] = volDescrip;
            }
            this.mCandidates = (VolumeInfo[]) candidates.toArray(new VolumeInfo[candidates.size()]);
            this.mDialogBuilder = new Builder(getContext()).setTitle(R.string.change_storage).setSingleChoiceItems(labels, current, this).setNegativeButton(R.string.cancel, null);
            return;
        }
        removePreference("storage_used");
        removePreference("change_storage_button");
        removePreference("storage_space");
    }

    private void initiateClearUserData() {
        this.mClearDataButton.setEnabled(false);
        String packageName = this.mAppEntry.info.packageName;
        Log.i(TAG, "Clearing user data for package : " + packageName);
        if (this.mClearDataObserver == null) {
            this.mClearDataObserver = new ClearUserDataObserver();
        }
        if (((ActivityManager) getActivity().getSystemService("activity")).clearApplicationUserData(packageName, this.mClearDataObserver)) {
            this.mClearDataButton.setText(R.string.recompute_size);
            return;
        }
        Log.i(TAG, "Couldnt clear application user data for package:" + packageName);
        showDialogInner(2, 0);
    }

    private void processClearMsg(Message msg) {
        int result = msg.arg1;
        String packageName = this.mAppEntry.info.packageName;
        this.mClearDataButton.setText(R.string.clear_user_data_text);
        if (result == 1) {
            Log.i(TAG, "Cleared user data for package : " + packageName);
            this.mState.requestSize(this.mPackageName, this.mUserId);
            Intent packageDataCleared = new Intent("com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED");
            packageDataCleared.putExtra("packageName", packageName);
            getActivity().sendBroadcast(packageDataCleared);
            return;
        }
        this.mClearDataButton.setEnabled(true);
    }

    protected AlertDialog createDialog(int id, int errorCode) {
        switch (id) {
            case 1:
                return new Builder(getActivity()).setTitle(getActivity().getText(R.string.clear_data_dlg_title)).setMessage(getActivity().getText(R.string.clear_data_dlg_text)).setPositiveButton(R.string.dlg_ok, new C02502()).setNegativeButton(R.string.dlg_cancel, null).create();
            case 2:
                return new Builder(getActivity()).setTitle(getActivity().getText(R.string.clear_failed_dlg_title)).setMessage(getActivity().getText(R.string.clear_failed_dlg_text)).setNeutralButton(R.string.dlg_ok, new C02513()).create();
            default:
                return null;
        }
    }

    public void onPackageSizeChanged(String packageName) {
        if (this.mAppEntry != null && packageName.equals(this.mAppEntry.info.packageName)) {
            refreshSizeInfo();
        }
    }

    public static CharSequence getSummary(AppEntry appEntry, Context context) {
        if (appEntry.size == -2 || appEntry.size == -1) {
            return context.getText(R.string.computing_size);
        }
        int i;
        if ((appEntry.info.flags & 262144) != 0) {
            i = R.string.storage_type_external;
        } else {
            i = R.string.storage_type_internal;
        }
        CharSequence storageType = context.getString(i);
        return context.getString(R.string.storage_summary_format, new Object[]{getSize(appEntry, context), storageType});
    }

    private static CharSequence getSize(AppEntry appEntry, Context context) {
        long size = appEntry.size;
        if (size == -1) {
            return context.getText(R.string.invalid_size_value);
        }
        return Formatter.formatFileSize(context, size);
    }

    protected int getMetricsCategory() {
        return 19;
    }
}
