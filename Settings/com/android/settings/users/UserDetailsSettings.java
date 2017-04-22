package com.android.settings.users;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class UserDetailsSettings extends SettingsPreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener {
    private static final String TAG = UserDetailsSettings.class.getSimpleName();
    private Runnable mCheckDeleteComplete = new C05541();
    private Bundle mDefaultGuestRestrictions;
    private ProgressDialog mDeletingUserDialog;
    private boolean mGuestUser;
    private Handler mHandler = new Handler();
    private SwitchPreference mPhonePref;
    private Preference mRemoveUserPref;
    private BroadcastReceiver mUserChangeReceiver = new C05552();
    private UserInfo mUserInfo;
    private UserManager mUserManager;

    class C05541 implements Runnable {
        C05541() {
        }

        public void run() {
            if (UserDetailsSettings.this.isResumed()) {
                if (UserDetailsSettings.this.mUserInfo == null || UserDetailsSettings.this.mUserManager == null) {
                    UserDetailsSettings.this.dismissDialogAndFinish();
                } else {
                    UserInfo info = UserDetailsSettings.this.mUserManager.getUserInfo(UserDetailsSettings.this.mUserInfo.id);
                    if (info == null) {
                        UserDetailsSettings.this.dismissDialogAndFinish();
                    } else if (!info.isEnabled()) {
                        UserDetailsSettings.this.mHandler.postDelayed(this, 500);
                    }
                }
            }
        }
    }

    class C05552 extends BroadcastReceiver {
        C05552() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_REMOVED")) {
                UserDetailsSettings.this.dismissDialogAndFinish();
            }
        }
    }

    class C05563 implements OnClickListener {
        C05563() {
        }

        public void onClick(DialogInterface dialog, int which) {
            UserDetailsSettings.this.removeUser();
        }
    }

    class C05574 implements OnClickListener {
        C05574() {
        }

        public void onClick(DialogInterface dialog, int which) {
            UserDetailsSettings.this.enableCallsAndSms(true);
        }
    }

    class C05585 implements OnClickListener {
        C05585() {
        }

        public void onClick(DialogInterface dialog, int which) {
            UserDetailsSettings.this.enableCallsAndSms(true);
        }
    }

    protected int getMetricsCategory() {
        return 98;
    }

    public void onCreate(Bundle icicle) {
        boolean z = false;
        super.onCreate(icicle);
        this.mUserManager = (UserManager) getActivity().getSystemService("user");
        addPreferencesFromResource(R.xml.user_details_settings);
        this.mPhonePref = (SwitchPreference) findPreference("enable_calling");
        this.mRemoveUserPref = findPreference("remove_user");
        this.mGuestUser = getArguments().getBoolean("guest_user", false);
        if (this.mGuestUser) {
            removePreference("remove_user");
            this.mPhonePref.setTitle(R.string.user_enable_calling);
            this.mDefaultGuestRestrictions = this.mUserManager.getDefaultGuestRestrictions();
            SwitchPreference switchPreference = this.mPhonePref;
            if (!this.mDefaultGuestRestrictions.getBoolean("no_outgoing_calls")) {
                z = true;
            }
            switchPreference.setChecked(z);
        } else {
            int userId = getArguments().getInt("user_id", -1);
            if (userId == -1) {
                throw new RuntimeException("Arguments to this fragment must contain the user id");
            }
            boolean z2;
            this.mUserInfo = this.mUserManager.getUserInfo(userId);
            SwitchPreference switchPreference2 = this.mPhonePref;
            if (this.mUserManager.hasUserRestriction("no_outgoing_calls", new UserHandle(userId))) {
                z2 = false;
            } else {
                z2 = true;
            }
            switchPreference2.setChecked(z2);
            this.mRemoveUserPref.setOnPreferenceClickListener(this);
        }
        if (this.mUserManager.hasUserRestriction("no_remove_user")) {
            removePreference("remove_user");
        }
        this.mPhonePref.setOnPreferenceChangeListener(this);
    }

    public void onResume() {
        super.onResume();
        getContext().registerReceiverAsUser(this.mUserChangeReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_REMOVED"), null, null);
        if (!this.mGuestUser) {
            if (this.mUserInfo != null) {
                UserInfo info = this.mUserManager.getUserInfo(this.mUserInfo.id);
                if (info == null) {
                    dismissDialogAndFinish();
                    return;
                } else if (!info.isEnabled()) {
                    showDeleteUserDialog();
                    this.mHandler.postDelayed(this.mCheckDeleteComplete, 500);
                    return;
                } else {
                    return;
                }
            }
            dismissDialogAndFinish();
        }
    }

    public void onPause() {
        getActivity().unregisterReceiver(this.mUserChangeReceiver);
        super.onPause();
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference != this.mRemoveUserPref) {
            return false;
        }
        if (UserHandle.myUserId() != 0) {
            throw new RuntimeException("Only the owner can remove a user");
        }
        showDialog(1);
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.TRUE.equals(newValue)) {
            showDialog(this.mGuestUser ? 2 : 3);
            return false;
        }
        enableCallsAndSms(false);
        return true;
    }

    void enableCallsAndSms(boolean enabled) {
        boolean z = false;
        this.mPhonePref.setChecked(enabled);
        if (this.mGuestUser) {
            Bundle bundle = this.mDefaultGuestRestrictions;
            String str = "no_outgoing_calls";
            if (!enabled) {
                z = true;
            }
            bundle.putBoolean(str, z);
            this.mDefaultGuestRestrictions.putBoolean("no_sms", true);
            this.mUserManager.setDefaultGuestRestrictions(this.mDefaultGuestRestrictions);
            for (UserInfo user : this.mUserManager.getUsers(true)) {
                if (user.isGuest()) {
                    UserHandle userHandle = new UserHandle(user.id);
                    Bundle userRestrictions = this.mUserManager.getUserRestrictions(userHandle);
                    userRestrictions.putAll(this.mDefaultGuestRestrictions);
                    this.mUserManager.setUserRestrictions(userRestrictions, userHandle);
                }
            }
            return;
        }
        boolean z2;
        userHandle = new UserHandle(this.mUserInfo.id);
        UserManager userManager = this.mUserManager;
        String str2 = "no_outgoing_calls";
        if (enabled) {
            z2 = false;
        } else {
            z2 = true;
        }
        userManager.setUserRestriction(str2, z2, userHandle);
        UserManager userManager2 = this.mUserManager;
        str = "no_sms";
        if (!enabled) {
            z = true;
        }
        userManager2.setUserRestriction(str, z, userHandle);
    }

    public Dialog onCreateDialog(int dialogId) {
        if (getActivity() == null) {
            return null;
        }
        switch (dialogId) {
            case 1:
                return UserDialogs.createRemoveDialog(getActivity(), this.mUserInfo.id, new C05563());
            case 2:
                return UserDialogs.createEnablePhoneCallsDialog(getActivity(), new C05574());
            case 3:
                return UserDialogs.createEnablePhoneCallsAndSmsDialog(getActivity(), new C05585());
            default:
                throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
        }
    }

    void removeUser() {
        showDeleteUserDialog();
        this.mUserManager.removeUser(this.mUserInfo.id);
    }

    private void showDeleteUserDialog() {
        if (this.mDeletingUserDialog == null) {
            this.mDeletingUserDialog = new ProgressDialog(getActivity());
            this.mDeletingUserDialog.setMessage(getResources().getString(R.string.data_enabler_waiting_message));
            this.mDeletingUserDialog.setIndeterminate(true);
            this.mDeletingUserDialog.setCancelable(false);
        }
        if (!this.mDeletingUserDialog.isShowing()) {
            this.mDeletingUserDialog.show();
        }
    }

    private void dismissDeleteUserDialog() {
        if (this.mDeletingUserDialog != null && this.mDeletingUserDialog.isShowing()) {
            this.mDeletingUserDialog.dismiss();
        }
    }

    private void dismissDialogAndFinish() {
        dismissDeleteUserDialog();
        finishFragment();
    }
}
