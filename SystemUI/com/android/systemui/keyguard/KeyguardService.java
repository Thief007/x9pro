package com.android.systemui.keyguard;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService.Stub;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.assis.app.MAIN.EVENT;

public class KeyguardService extends Service {
    private final Stub mBinder = new Stub() {
        public void addStateMonitorCallback(IKeyguardStateCallback callback) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.addStateMonitorCallback(callback);
        }

        public void verifyUnlock(IKeyguardExitCallback callback) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.verifyUnlock(callback);
        }

        public void keyguardDone(boolean authenticated, boolean wakeup) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.keyguardDone(authenticated);
        }

        public void setOccluded(boolean isOccluded) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.setOccluded(isOccluded);
        }

        public void dismiss() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.dismiss();
        }

        public void onDreamingStarted() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onDreamingStarted();
        }

        public void onDreamingStopped() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onDreamingStopped();
        }

        public void onStartedGoingToSleep(int reason) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onStartedGoingToSleep(reason);
        }

        public void onFinishedGoingToSleep(int reason) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onFinishedGoingToSleep(reason);
        }

        public void onStartedWakingUp() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onStartedWakingUp();
        }

        public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onScreenTurningOn(callback);
        }

        public void onScreenTurnedOn() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onScreenTurnedOn();
        }

        public void onScreenTurnedOff() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onScreenTurnedOff();
        }

        public void setKeyguardEnabled(boolean enabled) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.setKeyguardEnabled(enabled);
        }

        public void onSystemReady() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onSystemReady();
        }

        public void doKeyguardTimeout(Bundle options) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.doKeyguardTimeout(options);
        }

        public void setCurrentUser(int userId) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.setCurrentUser(userId);
        }

        public void onBootCompleted() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onBootCompleted();
        }

        public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }

        public void onActivityDrawn() {
            KeyguardService.this.checkPermission();
            KeyguardService.this.mKeyguardViewMediator.onActivityDrawn();
        }
    };
    private KeyguardViewMediator mKeyguardViewMediator;

    public void onCreate() {
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        this.mKeyguardViewMediator = (KeyguardViewMediator) ((SystemUIApplication) getApplication()).getComponent(KeyguardViewMediator.class);
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    void checkPermission() {
        if (Binder.getCallingUid() != EVENT.DYNAMIC_PACK_EVENT_BASE && getBaseContext().checkCallingOrSelfPermission("android.permission.CONTROL_KEYGUARD") != 0) {
            Log.w("KeyguardService", "Caller needs permission 'android.permission.CONTROL_KEYGUARD' to call " + Debug.getCaller());
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid() + ", must have permission " + "android.permission.CONTROL_KEYGUARD");
        }
    }
}
