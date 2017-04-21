package com.android.systemui.volume;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.VolumePolicy;
import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.VolumeDialog.Callback;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VolumeDialogComponent implements VolumeComponent {
    private final Context mContext;
    private final VolumeDialogController mController;
    private final VolumeDialog mDialog;
    private final SystemUI mSysui;
    private final Callback mVolumeDialogCallback = new Callback() {
        public void onSettingsClicked() {
            VolumeDialogComponent.this.startSettings(new Intent("android.settings.NOTIFICATION_SETTINGS"));
        }
    };
    private final VolumePolicy mVolumePolicy = new VolumePolicy(true, true, true, 400);
    private final ZenModeController mZenModeController;

    public VolumeDialogComponent(SystemUI sysui, Context context, Handler handler, ZenModeController zen) {
        this.mSysui = sysui;
        this.mContext = context;
        this.mController = new VolumeDialogController(context, null) {
            protected void onUserActivityW() {
                VolumeDialogComponent.this.sendUserActivity();
            }
        };
        this.mZenModeController = zen;
        this.mDialog = new VolumeDialog(context, 2020, this.mController, zen, this.mVolumeDialogCallback);
        applyConfiguration();
    }

    private void sendUserActivity() {
        KeyguardViewMediator kvm = (KeyguardViewMediator) this.mSysui.getComponent(KeyguardViewMediator.class);
        if (kvm != null) {
            kvm.userActivity();
        }
    }

    private void applyConfiguration() {
        this.mDialog.setStreamImportant(4, true);
        this.mDialog.setStreamImportant(1, false);
        this.mDialog.setShowHeaders(false);
        this.mDialog.setAutomute(true);
        this.mDialog.setSilentMode(false);
        this.mController.setVolumePolicy(this.mVolumePolicy);
        this.mController.showDndTile(true);
    }

    public ZenModeController getZenController() {
        return this.mZenModeController;
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void dismissNow() {
        this.mController.dismiss();
    }

    public void dispatchDemoCommand(String command, Bundle args) {
    }

    public void register() {
        this.mController.register();
        DndTile.setCombinedIcon(this.mContext, true);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mController.dump(fd, pw, args);
        this.mDialog.dump(pw);
    }

    private void startSettings(Intent intent) {
        ((PhoneStatusBar) this.mSysui.getComponent(PhoneStatusBar.class)).startActivityDismissingKeyguard(intent, true, true);
    }
}
