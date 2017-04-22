package com.android.settings;

import android.app.Fragment;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.view.MenuItem;

public abstract class ConfirmDeviceCredentialBaseActivity extends SettingsActivity {
    private boolean mDark;
    private boolean mEnterAnimationPending;
    private boolean mFirstTimeVisible = true;
    private boolean mRestoring;

    protected void onCreate(Bundle savedState) {
        boolean z;
        if (getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.darkTheme", false)) {
            setTheme(R.style.Theme.ConfirmDeviceCredentialsDark);
            this.mDark = true;
        }
        super.onCreate(savedState);
        if (((KeyguardManager) getSystemService(KeyguardManager.class)).isKeyguardLocked() && getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.showWhenLocked", false)) {
            getWindow().addFlags(524288);
        }
        setTitle(getIntent().getStringExtra("com.android.settings.ConfirmCredentials.title"));
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        if (savedState != null) {
            z = true;
        } else {
            z = false;
        }
        this.mRestoring = z;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        return true;
    }

    public void onResume() {
        super.onResume();
        if (!isChangingConfigurations() && !this.mRestoring && this.mDark && this.mFirstTimeVisible) {
            this.mFirstTimeVisible = false;
            prepareEnterAnimation();
            this.mEnterAnimationPending = true;
        }
    }

    private ConfirmDeviceCredentialBaseFragment getFragment() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragment == null || !(fragment instanceof ConfirmDeviceCredentialBaseFragment)) {
            return null;
        }
        return (ConfirmDeviceCredentialBaseFragment) fragment;
    }

    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (this.mEnterAnimationPending) {
            startEnterAnimation();
            this.mEnterAnimationPending = false;
        }
    }

    public void prepareEnterAnimation() {
        getFragment().prepareEnterAnimation();
    }

    public void startEnterAnimation() {
        getFragment().startEnterAnimation();
    }
}
