package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecureCameraLaunchManager {
    private CameraAvailabilityCallback mCameraAvailabilityCallback;
    private Map<String, Boolean> mCameraAvailabilityMap;
    private CameraManager mCameraManager;
    private Context mContext;
    private Handler mHandler = new Handler();
    private KeyguardBottomAreaView mKeyguardBottomArea;
    private Runnable mLaunchCameraRunnable;
    private LockPatternUtils mLockPatternUtils;
    private boolean mWaitingToLaunchSecureCamera;

    private class CameraAvailabilityCallback extends AvailabilityCallback {
        private CameraAvailabilityCallback() {
        }

        public void onCameraUnavailable(String cameraId) {
            SecureCameraLaunchManager.this.mCameraAvailabilityMap.put(cameraId, Boolean.valueOf(false));
        }

        public void onCameraAvailable(String cameraId) {
            SecureCameraLaunchManager.this.mCameraAvailabilityMap.put(cameraId, Boolean.valueOf(true));
            if (SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera && SecureCameraLaunchManager.this.areAllCamerasAvailable()) {
                SecureCameraLaunchManager.this.mKeyguardBottomArea.launchCamera();
                SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera = false;
                SecureCameraLaunchManager.this.mHandler.removeCallbacks(SecureCameraLaunchManager.this.mLaunchCameraRunnable);
            }
        }
    }

    public SecureCameraLaunchManager(Context context, KeyguardBottomAreaView keyguardBottomArea) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mKeyguardBottomArea = keyguardBottomArea;
        this.mCameraManager = (CameraManager) context.getSystemService("camera");
        this.mCameraAvailabilityCallback = new CameraAvailabilityCallback();
        this.mCameraAvailabilityMap = new HashMap();
        this.mWaitingToLaunchSecureCamera = false;
        this.mLaunchCameraRunnable = new Runnable() {
            public void run() {
                if (SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera) {
                    Log.w("SecureCameraLaunchManager", "Timeout waiting for camera availability");
                    SecureCameraLaunchManager.this.mKeyguardBottomArea.launchCamera();
                    SecureCameraLaunchManager.this.mWaitingToLaunchSecureCamera = false;
                }
            }
        };
    }

    public void create() {
        this.mCameraManager.registerAvailabilityCallback(this.mCameraAvailabilityCallback, this.mHandler);
    }

    public void destroy() {
        this.mCameraManager.unregisterAvailabilityCallback(this.mCameraAvailabilityCallback);
    }

    public void onSwipingStarted() {
        AsyncTask.execute(new Runnable() {
            public void run() {
                Intent intent = new Intent();
                intent.setAction("com.android.systemui.statusbar.phone.CLOSE_CAMERA");
                intent.addFlags(67108864);
                SecureCameraLaunchManager.this.mContext.sendBroadcast(intent);
            }
        });
    }

    public void startSecureCameraLaunch() {
        if (areAllCamerasAvailable() || targetWillWaitForCameraAvailable()) {
            this.mKeyguardBottomArea.launchCamera();
            return;
        }
        this.mWaitingToLaunchSecureCamera = true;
        this.mHandler.postDelayed(this.mLaunchCameraRunnable, 1000);
    }

    private boolean areAllCamerasAvailable() {
        for (Boolean booleanValue : this.mCameraAvailabilityMap.values()) {
            if (!booleanValue.booleanValue()) {
                return false;
            }
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean targetWillWaitForCameraAvailable() {
        Intent intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, KeyguardUpdateMonitor.getCurrentUser());
        if (appList.size() == 0) {
            return false;
        }
        ResolveInfo resolved = packageManager.resolveActivityAsUser(intent, 65664, KeyguardUpdateMonitor.getCurrentUser());
        if (resolved == null || resolved.activityInfo == null || wouldLaunchResolverActivity(resolved, appList) || resolved.activityInfo.metaData == null || resolved.activityInfo.metaData.isEmpty()) {
            return false;
        }
        return resolved.activityInfo.metaData.getBoolean("com.android.systemui.statusbar.phone.will_wait_for_camera_available");
    }

    private boolean wouldLaunchResolverActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
        for (int i = 0; i < appList.size(); i++) {
            ResolveInfo tmp = (ResolveInfo) appList.get(i);
            if (tmp.activityInfo.name.equals(resolved.activityInfo.name) && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                return false;
            }
        }
        return true;
    }
}
