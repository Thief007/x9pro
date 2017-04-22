package com.android.settings.applications;

import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppOpsState.AppOpEntry;
import com.android.settings.applications.AppOpsState.OpsTemplate;

public class AppOpsDetails extends InstrumentedFragment {
    private AppOpsManager mAppOps;
    private LayoutInflater mInflater;
    private LinearLayout mOperationsSection;
    private PackageInfo mPackageInfo;
    private PackageManager mPm;
    private View mRootView;
    private AppOpsState mState;

    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        CharSequence charSequence = null;
        View appSnippet = this.mRootView.findViewById(R.id.app_snippet);
        CharSequence label = this.mPm.getApplicationLabel(pkgInfo.applicationInfo);
        Drawable icon = this.mPm.getApplicationIcon(pkgInfo.applicationInfo);
        if (pkgInfo != null) {
            charSequence = pkgInfo.versionName;
        }
        InstalledAppDetails.setupAppSnippet(appSnippet, label, icon, charSequence);
    }

    private String retrieveAppEntry() {
        Bundle args = getArguments();
        String string = args != null ? args.getString("package") : null;
        if (string == null) {
            Intent intent = args == null ? getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                string = intent.getData().getSchemeSpecificPart();
            }
        }
        try {
            this.mPackageInfo = this.mPm.getPackageInfo(string, 8704);
        } catch (NameNotFoundException e) {
            Log.e("AppOpsDetails", "Exception when retrieving package:" + string, e);
            this.mPackageInfo = null;
        }
        return string;
    }

    private boolean refreshUi() {
        if (this.mPackageInfo == null) {
            return false;
        }
        setAppLabelAndIcon(this.mPackageInfo);
        Resources res = getActivity().getResources();
        this.mOperationsSection.removeAllViews();
        String lastPermGroup = "";
        for (OpsTemplate tpl : AppOpsState.ALL_TEMPLATES) {
            for (final AppOpEntry entry : this.mState.buildState(tpl, this.mPackageInfo.applicationInfo.uid, this.mPackageInfo.packageName)) {
                OpEntry firstOp = entry.getOpEntry(0);
                View view = this.mInflater.inflate(R.layout.app_ops_details_item, this.mOperationsSection, false);
                this.mOperationsSection.addView(view);
                String perm = AppOpsManager.opToPermission(firstOp.getOp());
                if (perm != null) {
                    try {
                        PermissionInfo pi = this.mPm.getPermissionInfo(perm, 0);
                        if (!(pi.group == null || lastPermGroup.equals(pi.group))) {
                            lastPermGroup = pi.group;
                            PermissionGroupInfo pgi = this.mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                                ((ImageView) view.findViewById(R.id.op_icon)).setImageDrawable(pgi.loadIcon(this.mPm));
                            }
                        }
                    } catch (NameNotFoundException e) {
                    }
                }
                ((TextView) view.findViewById(R.id.op_name)).setText(entry.getSwitchText(this.mState));
                ((TextView) view.findViewById(R.id.op_time)).setText(entry.getTimeText(res, true));
                Switch sw = (Switch) view.findViewById(R.id.switchWidget);
                final int switchOp = AppOpsManager.opToSwitch(firstOp.getOp());
                sw.setChecked(this.mAppOps.checkOp(switchOp, entry.getPackageOps().getUid(), entry.getPackageOps().getPackageName()) == 0);
                sw.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppOpsDetails.this.mAppOps.setMode(switchOp, entry.getPackageOps().getUid(), entry.getPackageOps().getPackageName(), isChecked ? 0 : 1);
                    }
                });
            }
        }
        return true;
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        Intent intent = new Intent();
        intent.putExtra("chg", appChanged);
        ((SettingsActivity) getActivity()).finishPreferencePanel(this, -1, intent);
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mState = new AppOpsState(getActivity());
        this.mPm = getActivity().getPackageManager();
        this.mInflater = (LayoutInflater) getActivity().getSystemService("layout_inflater");
        this.mAppOps = (AppOpsManager) getActivity().getSystemService("appops");
        retrieveAppEntry();
        setHasOptionsMenu(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_ops_details, container, false);
        Utils.prepareCustomPreferencesList(container, view, view, false);
        this.mRootView = view;
        this.mOperationsSection = (LinearLayout) view.findViewById(R.id.operations_section);
        return view;
    }

    protected int getMetricsCategory() {
        return 14;
    }

    public void onResume() {
        super.onResume();
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }
}
