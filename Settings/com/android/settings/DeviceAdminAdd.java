package com.android.settings;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog.Builder;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminInfo.PolicyInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils.TruncateAt;
import android.util.EventLog;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class DeviceAdminAdd extends Activity {
    Button mActionButton;
    TextView mAddMsg;
    boolean mAddMsgEllipsized = true;
    ImageView mAddMsgExpander;
    CharSequence mAddMsgText;
    boolean mAdding;
    boolean mAddingProfileOwner;
    TextView mAdminDescription;
    ImageView mAdminIcon;
    TextView mAdminName;
    ViewGroup mAdminPolicies;
    boolean mAdminPoliciesInitialized;
    TextView mAdminWarning;
    AppOpsManager mAppOps;
    Button mCancelButton;
    int mCurSysAppOpMode;
    int mCurToastAppOpMode;
    DevicePolicyManager mDPM;
    DeviceAdminInfo mDeviceAdmin;
    Handler mHandler;
    String mProfileOwnerName;
    TextView mProfileOwnerWarning;
    boolean mRefreshing;
    boolean mWaitingForRemoveMsg;

    class C01141 implements OnClickListener {
        C01141() {
        }

        public void onClick(View v) {
            DeviceAdminAdd.this.toggleMessageEllipsis(DeviceAdminAdd.this.mAddMsg);
        }
    }

    class C01152 implements OnGlobalLayoutListener {
        C01152() {
        }

        public void onGlobalLayout() {
            boolean hideMsgExpander = DeviceAdminAdd.this.mAddMsg.getLineCount() <= DeviceAdminAdd.this.getEllipsizedLines();
            DeviceAdminAdd.this.mAddMsgExpander.setVisibility(hideMsgExpander ? 8 : 0);
            if (hideMsgExpander) {
                DeviceAdminAdd.this.mAddMsg.setOnClickListener(null);
                ((View) DeviceAdminAdd.this.mAddMsgExpander.getParent()).invalidate();
            }
            DeviceAdminAdd.this.mAddMsg.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    }

    class C01163 implements OnClickListener {
        C01163() {
        }

        public void onClick(View v) {
            EventLog.writeEvent(90202, DeviceAdminAdd.this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
            DeviceAdminAdd.this.finish();
        }
    }

    class C01194 implements OnClickListener {

        class C01182 implements Runnable {
            C01182() {
            }

            public void run() {
                DeviceAdminAdd.this.continueRemoveAction(null);
            }
        }

        C01194() {
        }

        public void onClick(View v) {
            if (DeviceAdminAdd.this.mAdding) {
                DeviceAdminAdd.this.addAndFinish();
            } else if (!DeviceAdminAdd.this.mWaitingForRemoveMsg) {
                try {
                    ActivityManagerNative.getDefault().stopAppSwitches();
                } catch (RemoteException e) {
                }
                DeviceAdminAdd.this.mWaitingForRemoveMsg = true;
                DeviceAdminAdd.this.mDPM.getRemoveWarning(DeviceAdminAdd.this.mDeviceAdmin.getComponent(), new RemoteCallback(DeviceAdminAdd.this.mHandler) {
                    protected void onResult(Bundle bundle) {
                        CharSequence charSequence;
                        if (bundle != null) {
                            charSequence = bundle.getCharSequence("android.app.extra.DISABLE_WARNING");
                        } else {
                            charSequence = null;
                        }
                        DeviceAdminAdd.this.continueRemoveAction(charSequence);
                    }
                });
                DeviceAdminAdd.this.getWindow().getDecorView().getHandler().postDelayed(new C01182(), 2000);
            }
        }
    }

    class C01205 implements DialogInterface.OnClickListener {
        C01205() {
        }

        public void onClick(DialogInterface dialog, int which) {
            try {
                ActivityManagerNative.getDefault().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            DeviceAdminAdd.this.mDPM.removeActiveAdmin(DeviceAdminAdd.this.mDeviceAdmin.getComponent());
            DeviceAdminAdd.this.finish();
        }
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mHandler = new Handler(getMainLooper());
        this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
        this.mAppOps = (AppOpsManager) getSystemService("appops");
        PackageManager packageManager = getPackageManager();
        if ((getIntent().getFlags() & 268435456) != 0) {
            Log.w("DeviceAdminAdd", "Cannot start ADD_DEVICE_ADMIN as a new task");
            finish();
            return;
        }
        String action = getIntent().getAction();
        ComponentName who = (ComponentName) getIntent().getParcelableExtra("android.app.extra.DEVICE_ADMIN");
        if (who == null) {
            Log.w("DeviceAdminAdd", "No component specified in " + action);
            finish();
            return;
        }
        if (action != null && action.equals("android.app.action.SET_PROFILE_OWNER")) {
            setResult(0);
            setFinishOnTouchOutside(true);
            this.mAddingProfileOwner = true;
            this.mProfileOwnerName = getIntent().getStringExtra("android.app.extra.PROFILE_OWNER_NAME");
            String callingPackage = getCallingPackage();
            if (callingPackage == null || !callingPackage.equals(who.getPackageName())) {
                Log.e("DeviceAdminAdd", "Unknown or incorrect caller");
                finish();
                return;
            }
            try {
                if ((packageManager.getPackageInfo(callingPackage, 0).applicationInfo.flags & 1) == 0) {
                    Log.e("DeviceAdminAdd", "Cannot set a non-system app as a profile owner");
                    finish();
                    return;
                }
            } catch (NameNotFoundException e) {
                Log.e("DeviceAdminAdd", "Cannot find the package " + callingPackage);
                finish();
                return;
            }
        }
        try {
            int i;
            ResolveInfo ri;
            ActivityInfo ai = packageManager.getReceiverInfo(who, 128);
            if (!this.mDPM.isAdminActive(who)) {
                List<ResolveInfo> avail = packageManager.queryBroadcastReceivers(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"), 32768);
                int count = avail == null ? 0 : avail.size();
                boolean found = false;
                i = 0;
                while (i < count) {
                    ri = (ResolveInfo) avail.get(i);
                    if (ai.packageName.equals(ri.activityInfo.packageName) && ai.name.equals(ri.activityInfo.name)) {
                        try {
                            ri.activityInfo = ai;
                            DeviceAdminInfo dpi = new DeviceAdminInfo(this, ri);
                            found = true;
                            break;
                        } catch (XmlPullParserException e2) {
                            Log.w("DeviceAdminAdd", "Bad " + ri.activityInfo, e2);
                        } catch (IOException e3) {
                            Log.w("DeviceAdminAdd", "Bad " + ri.activityInfo, e3);
                        }
                    } else {
                        i++;
                    }
                }
                if (!found) {
                    Log.w("DeviceAdminAdd", "Request to add invalid device admin: " + who);
                    finish();
                    return;
                }
            }
            ri = new ResolveInfo();
            ri.activityInfo = ai;
            try {
                this.mDeviceAdmin = new DeviceAdminInfo(this, ri);
                if ("android.app.action.ADD_DEVICE_ADMIN".equals(getIntent().getAction())) {
                    this.mRefreshing = false;
                    if (this.mDPM.isAdminActive(who)) {
                        ArrayList<PolicyInfo> newPolicies = this.mDeviceAdmin.getUsedPolicies();
                        for (i = 0; i < newPolicies.size(); i++) {
                            if (!this.mDPM.hasGrantedPolicy(who, ((PolicyInfo) newPolicies.get(i)).ident)) {
                                this.mRefreshing = true;
                                break;
                            }
                        }
                        if (!this.mRefreshing) {
                            setResult(-1);
                            finish();
                            return;
                        }
                    }
                }
                if (!this.mAddingProfileOwner || this.mDPM.hasUserSetupCompleted()) {
                    this.mAddMsgText = getIntent().getCharSequenceExtra("android.app.extra.ADD_EXPLANATION");
                    setContentView(R.layout.device_admin_add);
                    this.mAdminIcon = (ImageView) findViewById(R.id.admin_icon);
                    this.mAdminName = (TextView) findViewById(R.id.admin_name);
                    this.mAdminDescription = (TextView) findViewById(R.id.admin_description);
                    this.mProfileOwnerWarning = (TextView) findViewById(R.id.profile_owner_warning);
                    this.mAddMsg = (TextView) findViewById(R.id.add_msg);
                    this.mAddMsgExpander = (ImageView) findViewById(R.id.add_msg_expander);
                    OnClickListener c01141 = new C01141();
                    this.mAddMsgExpander.setOnClickListener(c01141);
                    this.mAddMsg.setOnClickListener(c01141);
                    this.mAddMsg.getViewTreeObserver().addOnGlobalLayoutListener(new C01152());
                    toggleMessageEllipsis(this.mAddMsg);
                    this.mAdminWarning = (TextView) findViewById(R.id.admin_warning);
                    this.mAdminPolicies = (ViewGroup) findViewById(R.id.admin_policies);
                    this.mCancelButton = (Button) findViewById(R.id.cancel_button);
                    this.mCancelButton.setFilterTouchesWhenObscured(true);
                    this.mCancelButton.setOnClickListener(new C01163());
                    this.mActionButton = (Button) findViewById(R.id.action_button);
                    this.mActionButton.setFilterTouchesWhenObscured(true);
                    this.mActionButton.setOnClickListener(new C01194());
                    return;
                }
                addAndFinish();
            } catch (XmlPullParserException e22) {
                Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + who, e22);
                finish();
            } catch (IOException e32) {
                Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + who, e32);
                finish();
            }
        } catch (NameNotFoundException e4) {
            Log.w("DeviceAdminAdd", "Unable to retrieve device policy " + who, e4);
            finish();
        }
    }

    void addAndFinish() {
        try {
            this.mDPM.setActiveAdmin(this.mDeviceAdmin.getComponent(), this.mRefreshing);
            EventLog.writeEvent(90201, this.mDeviceAdmin.getActivityInfo().applicationInfo.uid);
            setResult(-1);
        } catch (RuntimeException e) {
            Log.w("DeviceAdminAdd", "Exception trying to activate admin " + this.mDeviceAdmin.getComponent(), e);
            if (this.mDPM.isAdminActive(this.mDeviceAdmin.getComponent())) {
                setResult(-1);
            }
        }
        if (this.mAddingProfileOwner) {
            try {
                this.mDPM.setProfileOwner(this.mDeviceAdmin.getComponent(), this.mProfileOwnerName, UserHandle.myUserId());
            } catch (RuntimeException e2) {
                setResult(0);
            }
        }
        finish();
    }

    void continueRemoveAction(CharSequence msg) {
        if (this.mWaitingForRemoveMsg) {
            this.mWaitingForRemoveMsg = false;
            if (msg == null) {
                try {
                    ActivityManagerNative.getDefault().resumeAppSwitches();
                } catch (RemoteException e) {
                }
                this.mDPM.removeActiveAdmin(this.mDeviceAdmin.getComponent());
                finish();
            } else {
                try {
                    ActivityManagerNative.getDefault().stopAppSwitches();
                } catch (RemoteException e2) {
                }
                Bundle args = new Bundle();
                args.putCharSequence("android.app.extra.DISABLE_WARNING", msg);
                showDialog(1, args);
            }
        }
    }

    protected void onResume() {
        super.onResume();
        updateInterface();
        int uid = this.mDeviceAdmin.getActivityInfo().applicationInfo.uid;
        String pkg = this.mDeviceAdmin.getActivityInfo().applicationInfo.packageName;
        this.mCurSysAppOpMode = this.mAppOps.checkOp(24, uid, pkg);
        this.mCurToastAppOpMode = this.mAppOps.checkOp(45, uid, pkg);
        this.mAppOps.setMode(24, uid, pkg, 1);
        this.mAppOps.setMode(45, uid, pkg, 1);
    }

    protected void onPause() {
        super.onPause();
        int uid = this.mDeviceAdmin.getActivityInfo().applicationInfo.uid;
        String pkg = this.mDeviceAdmin.getActivityInfo().applicationInfo.packageName;
        this.mAppOps.setMode(24, uid, pkg, this.mCurSysAppOpMode);
        this.mAppOps.setMode(45, uid, pkg, this.mCurToastAppOpMode);
        try {
            ActivityManagerNative.getDefault().resumeAppSwitches();
        } catch (RemoteException e) {
        }
    }

    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case 1:
                CharSequence msg = args.getCharSequence("android.app.extra.DISABLE_WARNING");
                Builder builder = new Builder(this);
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.dlg_ok, new C01205());
                builder.setNegativeButton(R.string.dlg_cancel, null);
                return builder.create();
            default:
                return super.onCreateDialog(id, args);
        }
    }

    void updateInterface() {
        this.mAdminIcon.setImageDrawable(this.mDeviceAdmin.loadIcon(getPackageManager()));
        this.mAdminName.setText(this.mDeviceAdmin.loadLabel(getPackageManager()));
        try {
            this.mAdminDescription.setText(this.mDeviceAdmin.loadDescription(getPackageManager()));
            this.mAdminDescription.setVisibility(0);
        } catch (NotFoundException e) {
            this.mAdminDescription.setVisibility(8);
        }
        if (this.mAddingProfileOwner) {
            this.mProfileOwnerWarning.setVisibility(0);
        }
        if (this.mAddMsgText != null) {
            this.mAddMsg.setText(this.mAddMsgText);
            this.mAddMsg.setVisibility(0);
        } else {
            this.mAddMsg.setVisibility(8);
            this.mAddMsgExpander.setVisibility(8);
        }
        if (this.mRefreshing || this.mAddingProfileOwner || !this.mDPM.isAdminActive(this.mDeviceAdmin.getComponent())) {
            addDeviceAdminPolicies(true);
            this.mAdminWarning.setText(getString(R.string.device_admin_warning, new Object[]{this.mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())}));
            if (this.mAddingProfileOwner) {
                setTitle(getText(R.string.profile_owner_add_title));
            } else {
                setTitle(getText(R.string.add_device_admin_msg));
            }
            this.mActionButton.setText(getText(R.string.add_device_admin));
            this.mAdding = true;
            return;
        }
        addDeviceAdminPolicies(false);
        this.mAdminWarning.setText(getString(R.string.device_admin_status, new Object[]{this.mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())}));
        setTitle(getText(R.string.active_device_admin_msg));
        this.mActionButton.setText(getText(R.string.remove_device_admin));
        this.mAdding = false;
    }

    private void addDeviceAdminPolicies(boolean showDescription) {
        if (!this.mAdminPoliciesInitialized) {
            boolean isOwner = UserHandle.getCallingUserHandle().isOwner();
            for (PolicyInfo pi : this.mDeviceAdmin.getUsedPolicies()) {
                this.mAdminPolicies.addView(AppSecurityPermissions.getPermissionItemView(this, getText(isOwner ? pi.label : pi.labelForSecondaryUsers), showDescription ? getText(isOwner ? pi.description : pi.descriptionForSecondaryUsers) : "", true));
            }
            this.mAdminPoliciesInitialized = true;
        }
    }

    void toggleMessageEllipsis(View v) {
        int i;
        TextView tv = (TextView) v;
        this.mAddMsgEllipsized = !this.mAddMsgEllipsized;
        tv.setEllipsize(this.mAddMsgEllipsized ? TruncateAt.END : null);
        tv.setMaxLines(this.mAddMsgEllipsized ? getEllipsizedLines() : 15);
        ImageView imageView = this.mAddMsgExpander;
        if (this.mAddMsgEllipsized) {
            i = 17302212;
        } else {
            i = 17302211;
        }
        imageView.setImageResource(i);
    }

    int getEllipsizedLines() {
        Display d = ((WindowManager) getSystemService("window")).getDefaultDisplay();
        return d.getHeight() > d.getWidth() ? 5 : 2;
    }
}
