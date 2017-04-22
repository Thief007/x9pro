package com.android.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class MasterClear extends InstrumentedFragment {
    private View mContentView;
    private CheckBox mExternalStorage;
    private View mExternalStorageContainer;
    private Button mInitiateButton;
    private final OnClickListener mInitiateListener = new C01501();

    class C01501 implements OnClickListener {
        C01501() {
        }

        public void onClick(View v) {
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = MasterClear.this.getActivity().getPackageManager().getApplicationInfo("net.argusmobile.argus", 0);
            } catch (Throwable th) {
                applicationInfo = null;
            }
            if (applicationInfo != null) {
                MasterClear.this.startArgusAntiTheftForPassword(61);
                return;
            }
            if (!MasterClear.this.runKeyguardConfirmation(55)) {
                MasterClear.this.showFinalConfirmation();
            }
        }
    }

    class C01512 implements OnClickListener {
        C01512() {
        }

        public void onClick(View v) {
            MasterClear.this.mExternalStorage.toggle();
        }
    }

    private void startArgusAntiTheftForPassword(int requestCode) {
        Intent intent = new Intent();
        intent.setClassName("net.argusmobile.argus", "net.argusmobile.argus.activities.MainActivity");
        intent.setAction("android.intent.action.VIEW");
        intent.putExtra("IsToCheckPasswordByOtherApp", true);
        startActivityForResult(intent, requestCode);
    }

    private boolean runKeyguardConfirmation(int request) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(request, getActivity().getResources().getText(R.string.master_clear_title));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 61 && -1 == resultCode && !runKeyguardConfirmation(55)) {
            showFinalConfirmation();
        } else if (requestCode == 55) {
            if (resultCode == -1) {
                showFinalConfirmation();
            } else {
                establishInitialState();
            }
        }
    }

    private void showFinalConfirmation() {
        Bundle args = new Bundle();
        args.putBoolean("erase_sd", this.mExternalStorage.isChecked());
        ((SettingsActivity) getActivity()).startPreferencePanel(MasterClearConfirm.class.getName(), args, R.string.master_clear_confirm_title, null, null, 0);
    }

    private void establishInitialState() {
        this.mInitiateButton = (Button) this.mContentView.findViewById(R.id.initiate_master_clear);
        this.mInitiateButton.setOnClickListener(this.mInitiateListener);
        this.mExternalStorageContainer = this.mContentView.findViewById(R.id.erase_external_container);
        this.mExternalStorage = (CheckBox) this.mContentView.findViewById(R.id.erase_external);
        boolean isExtStorageEmulated = Environment.isExternalStorageEmulated();
        if (isExtStorageEmulated || (!Environment.isExternalStorageRemovable() && isExtStorageEncrypted())) {
            boolean z;
            this.mExternalStorageContainer.setVisibility(8);
            this.mContentView.findViewById(R.id.erase_external_option_text).setVisibility(8);
            this.mContentView.findViewById(R.id.also_erases_external).setVisibility(0);
            CheckBox checkBox = this.mExternalStorage;
            if (isExtStorageEmulated) {
                z = false;
            } else {
                z = true;
            }
            checkBox.setChecked(z);
        } else {
            this.mExternalStorageContainer.setOnClickListener(new C01512());
        }
        loadAccountList((UserManager) getActivity().getSystemService("user"));
        StringBuffer contentDescription = new StringBuffer();
        View masterClearContainer = this.mContentView.findViewById(R.id.master_clear_container);
        getContentDescription(masterClearContainer, contentDescription);
        masterClearContainer.setContentDescription(contentDescription);
    }

    private void getContentDescription(View v, StringBuffer description) {
        if (v instanceof ViewGroup) {
            ViewGroup vGroup = (ViewGroup) v;
            for (int i = 0; i < vGroup.getChildCount(); i++) {
                getContentDescription(vGroup.getChildAt(i), description);
            }
        } else if (v instanceof TextView) {
            description.append(((TextView) v).getText());
            description.append(",");
        }
    }

    private boolean isExtStorageEncrypted() {
        return !"".equals(SystemProperties.get("vold.decrypt"));
    }

    private void loadAccountList(UserManager um) {
        int i;
        View accountsLabel = this.mContentView.findViewById(R.id.accounts_label);
        LinearLayout contents = (LinearLayout) this.mContentView.findViewById(R.id.accounts);
        contents.removeAllViews();
        Context context = getActivity();
        List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
        int profilesSize = profiles.size();
        AccountManager mgr = AccountManager.get(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        int accountsCount = 0;
        for (int profileIndex = 0; profileIndex < profilesSize; profileIndex++) {
            UserInfo userInfo = (UserInfo) profiles.get(profileIndex);
            int profileId = userInfo.id;
            UserHandle userHandle = new UserHandle(profileId);
            if (N != 0) {
                accountsCount += N;
                AuthenticatorDescription[] descs = AccountManager.get(context).getAuthenticatorTypesAsUser(profileId);
                int M = descs.length;
                View titleView = Utils.inflateCategoryHeader(inflater, contents);
                TextView titleText = (TextView) titleView.findViewById(16908310);
                if (userInfo.isManagedProfile()) {
                    i = R.string.category_work;
                } else {
                    i = R.string.category_personal;
                }
                titleText.setText(i);
                contents.addView(titleView);
                for (Account account : mgr.getAccountsAsUser(profileId)) {
                    AuthenticatorDescription desc = null;
                    for (int j = 0; j < M; j++) {
                        if (account.type.equals(descs[j].type)) {
                            desc = descs[j];
                            break;
                        }
                    }
                    if (desc == null) {
                        Log.w("MasterClear", "No descriptor for account name=" + account.name + " type=" + account.type);
                    } else {
                        Drawable icon = null;
                        try {
                            if (desc.iconId != 0) {
                                icon = context.getPackageManager().getUserBadgedIcon(context.createPackageContextAsUser(desc.packageName, 0, userHandle).getDrawable(desc.iconId), userHandle);
                            }
                        } catch (NameNotFoundException e) {
                            Log.w("MasterClear", "Bad package name for account type " + desc.type);
                        } catch (Throwable e2) {
                            Log.w("MasterClear", "Invalid icon id for account type " + desc.type, e2);
                        }
                        if (icon == null) {
                            icon = context.getPackageManager().getDefaultActivityIcon();
                        }
                        TextView child = (TextView) inflater.inflate(R.layout.master_clear_account, contents, false);
                        child.setText(account.name);
                        child.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                        contents.addView(child);
                    }
                }
            }
        }
        if (accountsCount > 0) {
            accountsLabel.setVisibility(0);
            contents.setVisibility(0);
        }
        View otherUsers = this.mContentView.findViewById(R.id.other_users_present);
        if (um.getUserCount() - profilesSize > 0) {
            i = 0;
        } else {
            i = 8;
        }
        otherUsers.setVisibility(i);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!Process.myUserHandle().isOwner() || UserManager.get(getActivity()).hasUserRestriction("no_factory_reset")) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, null);
        }
        this.mContentView = inflater.inflate(R.layout.master_clear, null);
        establishInitialState();
        return this.mContentView;
    }

    protected int getMetricsCategory() {
        return 66;
    }
}
