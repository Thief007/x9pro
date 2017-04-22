package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ConfirmLockPattern.InternalActivity;

public final class ChooseLockSettingsHelper {
    private Activity mActivity;
    private Fragment mFragment;
    private LockPatternUtils mLockPatternUtils;

    public ChooseLockSettingsHelper(Activity activity) {
        this.mActivity = activity;
        this.mLockPatternUtils = new LockPatternUtils(activity);
    }

    public ChooseLockSettingsHelper(Activity activity, Fragment fragment) {
        this(activity);
        this.mFragment = fragment;
    }

    public LockPatternUtils utils() {
        return this.mLockPatternUtils;
    }

    boolean launchConfirmationActivity(int request, CharSequence title) {
        return launchConfirmationActivity(request, title, null, null, false, false);
    }

    boolean launchConfirmationActivity(int request, CharSequence title, boolean returnCredentials) {
        return launchConfirmationActivity(request, title, null, null, returnCredentials, false);
    }

    boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, boolean returnCredentials, boolean external) {
        return launchConfirmationActivity(request, title, header, description, returnCredentials, external, false, 0);
    }

    public boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, long challenge) {
        return launchConfirmationActivity(request, title, header, description, false, false, true, challenge);
    }

    private boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, boolean returnCredentials, boolean external, boolean hasChallenge, long challenge) {
        Class cls;
        switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(Utils.getEffectiveUserId(this.mActivity))) {
            case 65536:
                if (returnCredentials || hasChallenge) {
                    cls = InternalActivity.class;
                } else {
                    cls = ConfirmLockPattern.class;
                }
                return launchConfirmationActivity(request, title, header, description, cls, external, hasChallenge, challenge);
            case 131072:
            case 196608:
            case 262144:
            case 327680:
            case 393216:
                if (returnCredentials || hasChallenge) {
                    cls = ConfirmLockPassword.InternalActivity.class;
                } else {
                    cls = ConfirmLockPassword.class;
                }
                return launchConfirmationActivity(request, title, header, description, cls, external, hasChallenge, challenge);
            default:
                return false;
        }
    }

    private boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence message, Class<?> activityClass, boolean external, boolean hasChallenge, long challenge) {
        Intent intent = new Intent();
        intent.putExtra("com.android.settings.ConfirmCredentials.title", title);
        intent.putExtra("com.android.settings.ConfirmCredentials.header", header);
        intent.putExtra("com.android.settings.ConfirmCredentials.details", message);
        intent.putExtra("com.android.settings.ConfirmCredentials.allowFpAuthentication", external);
        intent.putExtra("com.android.settings.ConfirmCredentials.darkTheme", external);
        intent.putExtra("com.android.settings.ConfirmCredentials.showCancelButton", external);
        intent.putExtra("com.android.settings.ConfirmCredentials.showWhenLocked", external);
        intent.putExtra("has_challenge", hasChallenge);
        intent.putExtra("challenge", challenge);
        intent.setClassName("com.android.settings", activityClass.getName());
        if (this.mFragment != null) {
            this.mFragment.startActivityForResult(intent, request);
        } else {
            this.mActivity.startActivityForResult(intent, request);
        }
        return true;
    }
}
