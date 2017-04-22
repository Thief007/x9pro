package com.android.settings.deviceinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.telecom.Log;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.app.IMediaContainerService.Stub;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class MigrateEstimateTask extends AsyncTask<Void, Void, Long> implements ServiceConnection {
    private static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName("com.android.defcontainer", "com.android.defcontainer.DefaultContainerService");
    private final CountDownLatch mConnected = new CountDownLatch(1);
    private final Context mContext;
    private IMediaContainerService mService;
    private long mSizeBytes = -1;
    private final StorageManager mStorage;
    private long mTimeMillis = -1;

    public abstract void onPostExecute(String str, String str2);

    public MigrateEstimateTask(Context context) {
        this.mContext = context;
        this.mStorage = (StorageManager) context.getSystemService(StorageManager.class);
    }

    public void copyFrom(Intent intent) {
        this.mSizeBytes = intent.getLongExtra("size_bytes", -1);
    }

    public void copyTo(Intent intent) {
        intent.putExtra("size_bytes", this.mSizeBytes);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Long doInBackground(Void... params) {
        if (this.mSizeBytes != -1) {
            return Long.valueOf(this.mSizeBytes);
        }
        VolumeInfo emulatedVol = this.mStorage.findEmulatedForPrivate(this.mContext.getPackageManager().getPrimaryStorageCurrentVolume());
        if (emulatedVol == null) {
            Log.w("StorageSettings", "Failed to find current primary storage", new Object[0]);
            return Long.valueOf(-1);
        }
        String path = emulatedVol.getPath().getAbsolutePath();
        Log.d("StorageSettings", "Estimating for current path " + path, new Object[0]);
        this.mContext.bindServiceAsUser(new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT), this, 1, UserHandle.OWNER);
        try {
            if (this.mConnected.await(15, TimeUnit.SECONDS)) {
                Long valueOf = Long.valueOf(this.mService.calculateDirectorySize(path));
                try {
                    this.mContext.unbindService(this);
                } catch (IllegalArgumentException e) {
                    Log.w("StorageSettings", "Already unbindService, just exit.", new Object[0]);
                }
                return valueOf;
            }
            try {
                this.mContext.unbindService(this);
            } catch (IllegalArgumentException e2) {
                Log.w("StorageSettings", "Already unbindService, just exit.", new Object[0]);
            }
            return Long.valueOf(-1);
        } catch (InterruptedException e3) {
            Log.w("StorageSettings", "Failed to measure " + path, new Object[0]);
        } catch (Throwable th) {
            try {
                this.mContext.unbindService(this);
            } catch (IllegalArgumentException e4) {
                Log.w("StorageSettings", "Already unbindService, just exit.", new Object[0]);
            }
        }
    }

    protected void onPostExecute(Long result) {
        this.mSizeBytes = result.longValue();
        this.mTimeMillis = (this.mSizeBytes * 1000) / 10485760;
        this.mTimeMillis = Math.max(this.mTimeMillis, 1000);
        onPostExecute(Formatter.formatFileSize(this.mContext, this.mSizeBytes), DateUtils.formatDuration(this.mTimeMillis).toString());
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        this.mService = Stub.asInterface(service);
        this.mConnected.countDown();
    }

    public void onServiceDisconnected(ComponentName name) {
    }
}
