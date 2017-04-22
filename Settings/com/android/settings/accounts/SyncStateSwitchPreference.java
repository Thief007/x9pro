package com.android.settings.accounts;

import android.accounts.Account;
import android.app.ActivityManager;
import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.widget.AnimatedImageView;

public class SyncStateSwitchPreference extends SwitchPreference {
    private Account mAccount;
    private String mAuthority;
    private boolean mFailed;
    private boolean mIsActive;
    private boolean mIsPending;
    private boolean mOneTimeSyncMode;

    public SyncStateSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs, 0, R.style.SyncSwitchPreference);
        this.mIsActive = false;
        this.mIsPending = false;
        this.mFailed = false;
        this.mOneTimeSyncMode = false;
        this.mAccount = null;
        this.mAuthority = null;
    }

    public SyncStateSwitchPreference(Context context, Account account, String authority) {
        super(context, null, 0, R.style.SyncSwitchPreference);
        this.mIsActive = false;
        this.mIsPending = false;
        this.mFailed = false;
        this.mOneTimeSyncMode = false;
        this.mAccount = account;
        this.mAuthority = authority;
    }

    public void onBindView(View view) {
        int i;
        super.onBindView(view);
        AnimatedImageView syncActiveView = (AnimatedImageView) view.findViewById(R.id.sync_active);
        View syncFailedView = view.findViewById(R.id.sync_failed);
        boolean z = !this.mIsActive ? this.mIsPending : true;
        if (z) {
            i = 0;
        } else {
            i = 8;
        }
        syncActiveView.setVisibility(i);
        syncActiveView.setAnimating(this.mIsActive);
        boolean failedVisible = this.mFailed && !z;
        if (failedVisible) {
            i = 0;
        } else {
            i = 8;
        }
        syncFailedView.setVisibility(i);
        View switchView = view.findViewById(16909206);
        if (this.mOneTimeSyncMode) {
            switchView.setVisibility(8);
            ((TextView) view.findViewById(16908304)).setText(getContext().getString(R.string.sync_one_time_sync, new Object[]{getSummary()}));
            return;
        }
        switchView.setVisibility(0);
    }

    public void setActive(boolean isActive) {
        this.mIsActive = isActive;
        notifyChanged();
    }

    public void setPending(boolean isPending) {
        this.mIsPending = isPending;
        notifyChanged();
    }

    public void setFailed(boolean failed) {
        this.mFailed = failed;
        notifyChanged();
    }

    public void setOneTimeSyncMode(boolean oneTimeSyncMode) {
        this.mOneTimeSyncMode = oneTimeSyncMode;
        notifyChanged();
    }

    public boolean isOneTimeSyncMode() {
        return this.mOneTimeSyncMode;
    }

    protected void onClick() {
        if (!this.mOneTimeSyncMode) {
            if (ActivityManager.isUserAMonkey()) {
                Log.d("SyncState", "ignoring monkey's attempt to flip sync state");
            } else {
                super.onClick();
            }
        }
    }

    public Account getAccount() {
        return this.mAccount;
    }

    public String getAuthority() {
        return this.mAuthority;
    }
}
