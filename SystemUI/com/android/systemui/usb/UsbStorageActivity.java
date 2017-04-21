package com.android.systemui.usb;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;

public class UsbStorageActivity extends Activity implements OnClickListener, OnCancelListener {
    private static boolean mSettingUMS = false;
    private int mAllowedShareNum = 0;
    private Handler mAsyncStorageHandler;
    private TextView mBanner;
    private boolean mDestroyed;
    private boolean mHasCheck = false;
    private ImageView mIcon;
    private boolean mIsShared = false;
    private TextView mMessage;
    private Button mMountButton;
    private ProgressBar mProgressBar;
    private int mSharedCount = 0;
    private StorageEventListener mStorageListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            Log.i("UsbStorageActivity", "VolumeInfo=" + vol + ",old=" + oldState + ",new=" + newState);
            boolean on = vol.getType() == 0 ? vol.getState() == 9 : false;
            if (UsbStorageActivity.this.sharableStorageNum() == 0) {
                UsbStorageActivity.this.finish();
            } else if (UsbStorageActivity.mSettingUMS) {
                boolean haveShared = UsbStorageActivity.this.isShared();
                Log.d("UsbStorageActivity", "onVolumeStateChanged - haveShared: " + haveShared);
                UsbStorageActivity.this.switchDisplay(haveShared);
            } else {
                UsbStorageActivity.this.switchDisplay(on);
            }
        }
    };
    private StorageManager mStorageManager = null;
    private Handler mUIHandler;
    private Button mUnmountButton;
    private BroadcastReceiver mUsbStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.hardware.usb.action.USB_STATE")) {
                UsbStorageActivity.this.handleUsbStateChanged(intent);
            }
        }
    };

    private int sharableStorageNum() {
        int num = 0;
        for (VolumeInfo info : this.mStorageManager.getVolumes()) {
            if (info != null && info.isAllowUsbMassStorage(ActivityManager.getCurrentUser()) && info.getType() == 0) {
                if (info.getState() == 6 && info.getState() == 7 && info.getState() == 8) {
                    if (info.getState() != 4) {
                    }
                }
                num++;
            }
        }
        return num;
    }

    private boolean isShared() {
        int num = 0;
        for (VolumeInfo info : this.mStorageManager.getVolumes()) {
            if (info != null && info.isAllowUsbMassStorage(ActivityManager.getCurrentUser()) && info.getType() == 0 && info.getState() == 9) {
                num++;
            }
        }
        if (num != 0) {
            return true;
        }
        return false;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mStorageManager == null) {
            this.mStorageManager = (StorageManager) getSystemService("storage");
            if (this.mStorageManager == null) {
                Log.w("UsbStorageActivity", "Failed to get StorageManager");
            }
        }
        this.mStorageManager.registerListener(this.mStorageListener);
        mSettingUMS |= this.mStorageManager.isUsbMassStorageEnabled();
        Log.i("UsbStorageActivity", "mSettingUMS=" + mSettingUMS);
        this.mUIHandler = new Handler();
        HandlerThread thr = new HandlerThread("SystemUI UsbStorageActivity");
        thr.start();
        this.mAsyncStorageHandler = new Handler(thr.getLooper());
        getWindow().addFlags(4194304);
        if (Environment.isExternalStorageRemovable()) {
            getWindow().addFlags(524288);
        }
        setContentView(17367288);
        this.mIcon = (ImageView) findViewById(16908294);
        this.mBanner = (TextView) findViewById(16909314);
        this.mMessage = (TextView) findViewById(16908299);
        this.mMountButton = (Button) findViewById(16909315);
        this.mMountButton.setOnClickListener(this);
        this.mUnmountButton = (Button) findViewById(16909316);
        this.mUnmountButton.setOnClickListener(this);
        this.mProgressBar = (ProgressBar) findViewById(16908301);
    }

    protected void onDestroy() {
        super.onDestroy();
        this.mDestroyed = true;
    }

    private void switchDisplay(final boolean usbStorageInUse) {
        this.mUIHandler.post(new Runnable() {
            public void run() {
                UsbStorageActivity.this.switchDisplayAsync(usbStorageInUse);
            }
        });
    }

    private void switchDisplayAsync(boolean usbStorageInUse) {
        if (usbStorageInUse) {
            this.mProgressBar.setVisibility(8);
            this.mUnmountButton.setVisibility(0);
            this.mMountButton.setVisibility(8);
            this.mIcon.setImageResource(17303362);
            this.mBanner.setText(17040337);
            this.mMessage.setText(17040338);
            return;
        }
        this.mProgressBar.setVisibility(8);
        this.mUnmountButton.setVisibility(8);
        this.mMountButton.setVisibility(0);
        this.mIcon.setImageResource(17303361);
        this.mBanner.setText(17040329);
        this.mMessage.setText(17040330);
    }

    protected void onResume() {
        super.onResume();
        this.mHasCheck = false;
        registerReceiver(this.mUsbStateReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        try {
            this.mAsyncStorageHandler.post(new Runnable() {
                public void run() {
                    UsbStorageActivity.this.switchDisplay(UsbStorageActivity.this.mStorageManager.isUsbMassStorageEnabled());
                }
            });
        } catch (Exception ex) {
            Log.e("UsbStorageActivity", "Failed to read UMS enable state", ex);
        }
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.mUsbStateReceiver);
    }

    private void handleUsbStateChanged(Intent intent) {
        boolean connected = intent.getExtras().getBoolean("connected");
        boolean isUMSmode = intent.getExtras().getBoolean("mass_storage");
        if (!connected || !isUMSmode) {
            if (mSettingUMS) {
                mSettingUMS = false;
                Log.i("UsbStorageActivity", "Unplug when UMS enabled " + connected);
            }
            finish();
        }
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return Stub.asInterface(service);
        }
        return null;
    }

    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case 1:
                return new Builder(this).setTitle(17040341).setPositiveButton(17040344, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        UsbStorageActivity.this.mHasCheck = false;
                        UsbStorageActivity.this.switchUsbMassStorage(true);
                    }
                }).setNegativeButton(17039360, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        UsbStorageActivity.this.mHasCheck = false;
                    }
                }).setMessage(17040342).setOnCancelListener(this).create();
            case 2:
                return new Builder(this).setTitle(17040343).setNeutralButton(17040344, null).setMessage(17040332).setOnCancelListener(this).create();
            default:
                return null;
        }
    }

    private void scheduleShowDialog(final int id) {
        this.mUIHandler.post(new Runnable() {
            public void run() {
                if (!UsbStorageActivity.this.mDestroyed) {
                    UsbStorageActivity.this.removeDialog(id);
                    UsbStorageActivity.this.showDialog(id);
                }
            }
        });
    }

    private void switchUsbMassStorage(final boolean on) {
        this.mUIHandler.post(new Runnable() {
            public void run() {
                UsbStorageActivity.this.mUnmountButton.setVisibility(8);
                UsbStorageActivity.this.mMountButton.setVisibility(8);
                UsbStorageActivity.this.mProgressBar.setVisibility(0);
            }
        });
        this.mAsyncStorageHandler.post(new Runnable() {
            public void run() {
                Log.i("UsbStorageActivity", "mStorageManager=" + on);
                if (on) {
                    UsbStorageActivity.mSettingUMS = true;
                    UsbStorageActivity.this.mStorageManager.enableUsbMassStorage();
                    return;
                }
                UsbStorageActivity.mSettingUMS = false;
                UsbStorageActivity.this.mStorageManager.disableUsbMassStorage();
            }
        });
    }

    private void checkStorageUsers() {
        this.mAsyncStorageHandler.post(new Runnable() {
            public void run() {
                UsbStorageActivity.this.checkStorageUsersAsync();
            }
        });
    }

    private void checkStorageUsersAsync() {
        IMountService ims = getMountService();
        if (ims == null) {
            scheduleShowDialog(2);
        }
        boolean showDialog = false;
        try {
            int[] stUsers = ims.getStorageUsers(Environment.getExternalStorageDirectory().toString());
            if (stUsers == null || stUsers.length <= 0) {
                List<ApplicationInfo> infoList = ((ActivityManager) getSystemService("activity")).getRunningExternalApplications();
                if (infoList != null && infoList.size() > 0) {
                    showDialog = true;
                }
                if (showDialog) {
                    switchUsbMassStorage(true);
                } else {
                    scheduleShowDialog(1);
                }
            }
            showDialog = true;
            if (showDialog) {
                switchUsbMassStorage(true);
            } else {
                scheduleShowDialog(1);
            }
        } catch (RemoteException e) {
            scheduleShowDialog(2);
        }
    }

    public void onClick(View v) {
        Log.i("UsbStorageActivity", "onClickaa" + this.mHasCheck);
        if (v == this.mMountButton) {
            if (!this.mHasCheck) {
                Log.i("UsbStorageActivity", "Enabling UMS");
                this.mHasCheck = true;
                checkStorageUsers();
            }
        } else if (v == this.mUnmountButton) {
            Log.i("UsbStorageActivity", "Disabling UMS");
            this.mHasCheck = false;
            switchUsbMassStorage(false);
        }
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
