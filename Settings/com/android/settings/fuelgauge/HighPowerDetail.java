package com.android.settings.fuelgauge;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class HighPowerDetail extends DialogFragment implements OnClickListener, View.OnClickListener {
    private final PowerWhitelistBackend mBackend = PowerWhitelistBackend.getInstance();
    private boolean mDefaultOn;
    private boolean mIsEnabled;
    private CharSequence mLabel;
    private Checkable mOptionOff;
    private Checkable mOptionOn;
    private String mPackageName;

    public void onCreate(Bundle savedInstanceState) {
        boolean z;
        super.onCreate(savedInstanceState);
        this.mPackageName = getArguments().getString("package");
        PackageManager pm = getContext().getPackageManager();
        try {
            this.mLabel = pm.getApplicationInfo(this.mPackageName, 0).loadLabel(pm);
        } catch (NameNotFoundException e) {
            this.mLabel = this.mPackageName;
        }
        this.mDefaultOn = getArguments().getBoolean("default_on");
        if (this.mDefaultOn) {
            z = true;
        } else {
            z = this.mBackend.isWhitelisted(this.mPackageName);
        }
        this.mIsEnabled = z;
    }

    public Checkable setup(View view, boolean on) {
        ((TextView) view.findViewById(16908310)).setText(on ? R.string.ignore_optimizations_on : R.string.ignore_optimizations_off);
        ((TextView) view.findViewById(16908304)).setText(on ? R.string.ignore_optimizations_on_desc : R.string.ignore_optimizations_off_desc);
        view.setClickable(true);
        view.setOnClickListener(this);
        if (!on && this.mBackend.isSysWhitelisted(this.mPackageName)) {
            view.setEnabled(false);
        }
        return (Checkable) view;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder b = new Builder(getContext()).setTitle(this.mLabel).setNegativeButton(R.string.cancel, null).setView(R.layout.ignore_optimizations_content);
        if (!this.mBackend.isSysWhitelisted(this.mPackageName)) {
            b.setPositiveButton(R.string.done, this);
        }
        return b.create();
    }

    public void onStart() {
        super.onStart();
        this.mOptionOn = setup(getDialog().findViewById(R.id.ignore_on), true);
        this.mOptionOff = setup(getDialog().findViewById(R.id.ignore_off), false);
        updateViews();
    }

    private void updateViews() {
        this.mOptionOn.setChecked(this.mIsEnabled);
        this.mOptionOff.setChecked(!this.mIsEnabled);
    }

    public void onClick(View v) {
        if (v == this.mOptionOn) {
            this.mIsEnabled = true;
            updateViews();
        } else if (v == this.mOptionOff) {
            this.mIsEnabled = false;
            updateViews();
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            boolean newValue = this.mIsEnabled;
            if (newValue == this.mBackend.isWhitelisted(this.mPackageName)) {
                return;
            }
            if (newValue) {
                this.mBackend.addApp(this.mPackageName);
            } else {
                this.mBackend.removeApp(this.mPackageName);
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Fragment target = getTargetFragment();
        if (target != null) {
            target.onActivityResult(getTargetRequestCode(), 0, null);
        }
    }

    public static CharSequence getSummary(Context context, AppEntry entry) {
        return getSummary(context, entry.info.packageName);
    }

    public static CharSequence getSummary(Context context, String pkg) {
        int i;
        PowerWhitelistBackend powerWhitelist = PowerWhitelistBackend.getInstance();
        if (powerWhitelist.isSysWhitelisted(pkg)) {
            i = R.string.high_power_system;
        } else if (powerWhitelist.isWhitelisted(pkg)) {
            i = R.string.high_power_filter_on;
        } else {
            i = R.string.high_power_off;
        }
        return context.getString(i);
    }

    public static void show(Fragment caller, String packageName, int requestCode, boolean defaultToOn) {
        HighPowerDetail fragment = new HighPowerDetail();
        Bundle args = new Bundle();
        args.putString("package", packageName);
        args.putBoolean("default_on", defaultToOn);
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, requestCode);
        fragment.show(caller.getFragmentManager(), HighPowerDetail.class.getSimpleName());
    }
}
