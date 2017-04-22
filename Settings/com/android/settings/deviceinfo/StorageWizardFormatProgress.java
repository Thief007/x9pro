package com.android.settings.deviceinfo;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.IPackageMoveObserver.Stub;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import java.util.Objects;

public class StorageWizardFormatProgress extends StorageWizardBase {
    private boolean mFormatPrivate;
    private PartitionTask mTask;

    public static class PartitionTask extends AsyncTask<Void, Integer, Exception> {
        public StorageWizardFormatProgress mActivity;
        private volatile long mInternalBench;
        private volatile long mPrivateBench;
        private volatile int mProgress = 20;

        protected Exception doInBackground(Void... params) {
            StorageWizardFormatProgress activity = this.mActivity;
            StorageManager storage = this.mActivity.mStorage;
            try {
                if (activity.mFormatPrivate) {
                    storage.partitionPrivate(activity.mDisk.getId());
                    publishProgress(new Integer[]{Integer.valueOf(40)});
                    this.mInternalBench = storage.benchmark(null);
                    publishProgress(new Integer[]{Integer.valueOf(60)});
                    VolumeInfo privateVol = activity.findFirstVolume(1);
                    this.mPrivateBench = storage.benchmark(privateVol.getId());
                    if (activity.mDisk.isDefaultPrimary() && Objects.equals(storage.getPrimaryStorageUuid(), "primary_physical")) {
                        Log.d("StorageSettings", "Just formatted primary physical; silently moving storage to new emulated volume");
                        storage.setPrimaryStorageUuid(privateVol.getFsUuid(), new SilentObserver());
                    }
                } else {
                    storage.partitionPublic(activity.mDisk.getId());
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        protected void onProgressUpdate(Integer... progress) {
            this.mProgress = progress[0].intValue();
            this.mActivity.setCurrentProgress(this.mProgress);
        }

        public void setActivity(StorageWizardFormatProgress activity) {
            this.mActivity = activity;
            this.mActivity.setCurrentProgress(this.mProgress);
        }

        protected void onPostExecute(Exception e) {
            StorageWizardFormatProgress activity = this.mActivity;
            if (e != null) {
                Log.e("StorageSettings", "Failed to partition", e);
                activity.finishAffinity();
                return;
            }
            if (activity.mFormatPrivate) {
                float pct = ((float) this.mInternalBench) / ((float) this.mPrivateBench);
                Log.d("StorageSettings", "New volume is " + pct + "x the speed of internal");
                if (activity.isDestroyed() || (!Float.isNaN(pct) && ((double) pct) >= 0.25d)) {
                    activity.onFormatFinished();
                } else {
                    new SlowWarningFragment().showAllowingStateLoss(activity.getFragmentManager(), "slow_warning");
                }
            } else {
                activity.onFormatFinished();
            }
        }
    }

    private static class SilentObserver extends Stub {
        private SilentObserver() {
        }

        public void onCreated(int moveId, Bundle extras) {
        }

        public void onStatusChanged(int moveId, int status, long estMillis) {
        }
    }

    public static class SlowWarningFragment extends DialogFragment {

        class C03491 implements OnClickListener {
            C03491() {
            }

            public void onClick(DialogInterface dialog, int which) {
                ((StorageWizardFormatProgress) SlowWarningFragment.this.getActivity()).onFormatFinished();
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new Builder(getActivity());
            StorageWizardFormatProgress target = (StorageWizardFormatProgress) getActivity();
            String descrip = target.getDiskDescription();
            String genericDescip = target.getGenericDiskDescription();
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_wizard_slow_body), new CharSequence[]{descrip, genericDescip}));
            builder.setPositiveButton(17039370, new C03491());
            return builder.create();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_progress);
        setKeepScreenOn(true);
        this.mFormatPrivate = getIntent().getBooleanExtra("format_private", false);
        setIllustrationInternal(this.mFormatPrivate);
        setHeaderText(R.string.storage_wizard_format_progress_title, this.mDisk.getDescription());
        setBodyText(R.string.storage_wizard_format_progress_body, this.mDisk.getDescription());
        getNextButton().setVisibility(8);
        this.mTask = (PartitionTask) getLastNonConfigurationInstance();
        if (this.mTask == null) {
            this.mTask = new PartitionTask();
            this.mTask.setActivity(this);
            this.mTask.execute(new Void[0]);
        } else {
            this.mTask.setActivity(this);
        }
    }

    public Object onRetainNonConfigurationInstance() {
        return this.mTask;
    }

    private String getDiskDescription() {
        return this.mDisk.getDescription();
    }

    private String getGenericDiskDescription() {
        if (this.mDisk.isSd()) {
            return getString(17040514);
        }
        if (this.mDisk.isUsb()) {
            return getString(17040516);
        }
        return null;
    }

    private void onFormatFinished() {
        boolean equals;
        String forgetUuid = getIntent().getStringExtra("forget_uuid");
        if (!TextUtils.isEmpty(forgetUuid)) {
            this.mStorage.forgetVolume(forgetUuid);
        }
        if (this.mFormatPrivate) {
            VolumeInfo privateVol = getPackageManager().getPrimaryStorageCurrentVolume();
            if (privateVol != null) {
                equals = "private".equals(privateVol.getId());
            } else {
                equals = false;
            }
        } else {
            equals = false;
        }
        Intent intent;
        if (equals) {
            intent = new Intent(this, StorageWizardMigrate.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            startActivity(intent);
        } else {
            intent = new Intent(this, StorageWizardReady.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            startActivity(intent);
        }
        finishAffinity();
    }
}
