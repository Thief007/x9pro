package com.android.systemui.assist;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractionSessionShowCallback.Stub;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class AssistManager {
    private ComponentName mAssistComponent;
    private final AssistDisclosure mAssistDisclosure;
    private final ContentObserver mAssistSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            AssistManager.this.updateAssistInfo();
        }
    };
    private final AssistUtils mAssistUtils;
    private final BaseStatusBar mBar;
    private final Context mContext;
    private Runnable mHideRunnable = new Runnable() {
        public void run() {
            AssistManager.this.mView.removeCallbacks(this);
            AssistManager.this.mView.show(false, true);
        }
    };
    private IVoiceInteractionSessionShowCallback mShowCallback = new Stub() {
        public void onFailed() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }

        public void onShown() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }
    };
    private AssistOrbContainer mView;
    private final WindowManager mWindowManager;

    public AssistManager(BaseStatusBar bar, Context context) {
        this.mContext = context;
        this.mBar = bar;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAssistUtils = new AssistUtils(context);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("assistant"), false, this.mAssistSettingsObserver);
        this.mAssistSettingsObserver.onChange(false);
        this.mAssistDisclosure = new AssistDisclosure(context, new Handler());
    }

    public void onConfigurationChanged() {
        boolean z = false;
        if (this.mView != null) {
            z = this.mView.isShowing();
            this.mWindowManager.removeView(this.mView);
        }
        this.mView = (AssistOrbContainer) LayoutInflater.from(this.mContext).inflate(R.layout.assist_orb, null);
        this.mView.setVisibility(8);
        this.mView.setSystemUiVisibility(1792);
        this.mWindowManager.addView(this.mView, getLayoutParams());
        if (z) {
            this.mView.show(true, false);
        }
    }

    public void startAssist(Bundle args) {
        updateAssistInfo();
        if (this.mAssistComponent != null) {
            boolean isService = isAssistantService();
            if (!(isService && isVoiceSessionRunning())) {
                long j;
                showOrb();
                AssistOrbContainer assistOrbContainer = this.mView;
                Runnable runnable = this.mHideRunnable;
                if (isService) {
                    j = 2500;
                } else {
                    j = 1000;
                }
                assistOrbContainer.postDelayed(runnable, j);
            }
            startAssistInternal(args);
        }
    }

    public void hideAssist() {
        this.mAssistUtils.hideCurrentSession();
    }

    private LayoutParams getLayoutParams() {
        LayoutParams lp = new LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(R.dimen.assist_orb_scrim_height), 2033, 280, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.gravity = 8388691;
        lp.setTitle("AssistPreviewPanel");
        lp.softInputMode = 49;
        return lp;
    }

    private void showOrb() {
        maybeSwapSearchIcon();
        this.mView.show(true, true);
    }

    private void startAssistInternal(Bundle args) {
        if (this.mAssistComponent == null) {
            return;
        }
        if (isAssistantService()) {
            startVoiceInteractor(args);
        } else {
            startAssistActivity(args);
        }
    }

    private void startAssistActivity(Bundle args) {
        if (this.mBar.isDeviceProvisioned()) {
            this.mBar.animateCollapsePanels(3);
            boolean structureEnabled = Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, -2) != 0;
            final Intent intent = ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(structureEnabled);
            if (intent != null) {
                if (this.mAssistComponent != null) {
                    intent.setComponent(this.mAssistComponent);
                }
                intent.putExtras(args);
                if (structureEnabled) {
                    showDisclosure();
                }
                try {
                    final ActivityOptions opts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.search_launch_enter, R.anim.search_launch_exit);
                    intent.addFlags(268435456);
                    AsyncTask.execute(new Runnable() {
                        public void run() {
                            AssistManager.this.mContext.startActivityAsUser(intent, opts.toBundle(), new UserHandle(-2));
                        }
                    });
                } catch (ActivityNotFoundException e) {
                    Log.w("AssistManager", "Activity not found for " + intent.getAction());
                }
            }
        }
    }

    private void startVoiceInteractor(Bundle args) {
        this.mAssistUtils.showSessionForActiveService(args, 4, this.mShowCallback, null);
    }

    public void launchVoiceAssistFromKeyguard() {
        this.mAssistUtils.launchVoiceAssistFromKeyguard();
    }

    public boolean canVoiceAssistBeLaunchedFromKeyguard() {
        return this.mAssistUtils.activeServiceSupportsLaunchFromKeyguard();
    }

    public ComponentName getVoiceInteractorComponentName() {
        return this.mAssistUtils.getActiveServiceComponentName();
    }

    private boolean isVoiceSessionRunning() {
        return this.mAssistUtils.isSessionRunning();
    }

    public void destroy() {
        this.mWindowManager.removeViewImmediate(this.mView);
    }

    private void maybeSwapSearchIcon() {
        if (this.mAssistComponent != null) {
            replaceDrawable(this.mView.getOrb().getLogo(), this.mAssistComponent, "com.android.systemui.action_assist_icon", isAssistantService());
        } else {
            this.mView.getOrb().getLogo().setImageDrawable(null);
        }
    }

    public void replaceDrawable(ImageView v, ComponentName component, String name, boolean isService) {
        if (component != null) {
            try {
                Bundle metaData;
                PackageManager packageManager = this.mContext.getPackageManager();
                if (isService) {
                    metaData = packageManager.getServiceInfo(component, 128).metaData;
                } else {
                    metaData = packageManager.getActivityInfo(component, 128).metaData;
                }
                if (metaData != null) {
                    int iconResId = metaData.getInt(name);
                    if (iconResId != 0) {
                        v.setImageDrawable(packageManager.getResourcesForApplication(component.getPackageName()).getDrawable(iconResId));
                        return;
                    }
                }
            } catch (NameNotFoundException e) {
                Log.w("AssistManager", "Failed to swap drawable; " + component.flattenToShortString() + " not found", e);
            } catch (NotFoundException nfe) {
                Log.w("AssistManager", "Failed to swap drawable from " + component.flattenToShortString(), nfe);
            }
        }
        v.setImageDrawable(null);
    }

    private boolean isAssistantService() {
        return this.mAssistComponent == null ? false : this.mAssistComponent.equals(getVoiceInteractorComponentName());
    }

    private void updateAssistInfo() {
        this.mAssistComponent = this.mAssistUtils.getAssistComponentForUser(-2);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AssistManager state:");
        pw.print("  mAssistComponent=");
        pw.println(this.mAssistComponent);
    }

    public void showDisclosure() {
        this.mAssistDisclosure.postShow();
    }

    public void onUserSwitched(int newUserId) {
        updateAssistInfo();
    }

    public void onLockscreenShown() {
        this.mAssistUtils.onLockscreenShown();
    }
}
