package com.android.settings.users;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SimpleAdapter;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.OwnerInfoSettings;
import com.android.settings.R;
import com.android.settings.SelectableEditTextPreference;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.drawable.CircleFramedDrawable;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.users.EditUserInfoController.OnContentChangedCallback;
import com.android.setupwizardlib.R$styleable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class UserSettings extends SettingsPreferenceFragment implements OnPreferenceClickListener, OnClickListener, OnDismissListener, OnPreferenceChangeListener, OnContentChangedCallback, Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C05623();
    private Preference mAddUser;
    private int mAddedUserId = 0;
    private boolean mAddingUser;
    private Drawable mDefaultIconDrawable;
    private ProgressDialog mDeletingUserDialog;
    private EditUserInfoController mEditUserInfoController = new EditUserInfoController();
    private Handler mHandler = new C05601();
    private UserPreference mMePreference;
    private SelectableEditTextPreference mNicknamePreference;
    private int mRemovingUserId = -1;
    private boolean mUpdateUserListOperate = false;
    private UserCapabilities mUserCaps;
    private BroadcastReceiver mUserChangeReceiver = new C05612();
    private SparseArray<Bitmap> mUserIcons = new SparseArray();
    private PreferenceGroup mUserListCategory;
    private final Object mUserLock = new Object();
    private UserManager mUserManager;

    class C05601 extends Handler {
        C05601() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    UserSettings.this.updateUserList();
                    return;
                case 2:
                    UserSettings.this.onUserCreated(msg.arg1);
                    return;
                case 3:
                    UserSettings.this.onManageUserClicked(msg.arg1, true);
                    return;
                default:
                    return;
            }
        }
    }

    class C05612 extends BroadcastReceiver {
        C05612() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_REMOVED")) {
                UserSettings.this.dismissDeleteUserDialog();
                UserSettings.this.mRemovingUserId = -1;
            } else if (intent.getAction().equals("android.intent.action.USER_INFO_CHANGED")) {
                int userHandle = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userHandle != -1) {
                    UserSettings.this.mUserIcons.remove(userHandle);
                }
            }
            UserSettings.this.mHandler.sendEmptyMessage(1);
        }
    }

    static class C05623 extends BaseSearchIndexProvider {
        C05623() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            UserCapabilities userCaps = UserCapabilities.create(context);
            if (!userCaps.mEnabled) {
                return result;
            }
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.user_settings_title);
            data.screenTitle = res.getString(R.string.user_settings_title);
            result.add(data);
            if (userCaps.mCanAddUser) {
                int i;
                data = new SearchIndexableRaw(context);
                if (userCaps.mCanAddRestrictedProfile) {
                    i = R.string.user_add_user_or_profile_menu;
                } else {
                    i = R.string.user_add_user_menu;
                }
                data.title = res.getString(i);
                data.screenTitle = res.getString(R.string.user_settings_title);
                result.add(data);
            }
            return result;
        }
    }

    class C05634 extends AsyncTask<Void, Void, String> {
        C05634() {
        }

        protected void onPostExecute(String result) {
            UserSettings.this.finishLoadProfile(result);
        }

        protected String doInBackground(Void... values) {
            UserInfo user = UserSettings.this.mUserManager.getUserInfo(UserHandle.myUserId());
            if (user.iconPath == null || user.iconPath.equals("")) {
                UserSettings.this.assignProfilePhoto(user);
            }
            return user.name;
        }
    }

    class C05645 implements DialogInterface.OnClickListener {
        C05645() {
        }

        public void onClick(DialogInterface dialog, int which) {
            UserSettings.this.removeUserNow();
        }
    }

    class C05667 implements DialogInterface.OnClickListener {
        C05667() {
        }

        public void onClick(DialogInterface dialog, int which) {
            UserSettings.this.switchUserNow(UserSettings.this.mAddedUserId);
        }
    }

    class C05678 implements DialogInterface.OnClickListener {
        C05678() {
        }

        public void onClick(DialogInterface dialog, int which) {
            UserSettings.this.switchUserNow(UserSettings.this.mAddedUserId);
        }
    }

    class C05689 implements DialogInterface.OnClickListener {
        C05689() {
        }

        public void onClick(DialogInterface dialog, int which) {
            int i;
            UserSettings userSettings = UserSettings.this;
            if (which == 0) {
                i = 1;
            } else {
                i = 2;
            }
            userSettings.onAddUserClicked(i);
        }
    }

    private static class UserCapabilities {
        boolean mCanAddGuest;
        boolean mCanAddRestrictedProfile = true;
        boolean mCanAddUser = true;
        boolean mEnabled = true;
        boolean mIsGuest;
        boolean mIsOwner;

        private UserCapabilities() {
            boolean z = true;
            if (UserHandle.myUserId() != 0) {
                z = false;
            }
            this.mIsOwner = z;
        }

        public static UserCapabilities create(Context context) {
            UserManager userManager = (UserManager) context.getSystemService("user");
            UserCapabilities caps = new UserCapabilities();
            if (!UserManager.supportsMultipleUsers() || Utils.isMonkeyRunning()) {
                caps.mEnabled = false;
                return caps;
            }
            boolean canAddUsersWhenLocked;
            boolean disallowAddUser = userManager.hasUserRestriction("no_add_user");
            if (caps.mIsOwner && UserManager.getMaxSupportedUsers() >= 2 && UserManager.supportsMultipleUsers()) {
                if (disallowAddUser) {
                }
                if (((DevicePolicyManager) context.getSystemService("device_policy")).getDeviceOwner() != null || Utils.isVoiceCapable(context)) {
                    caps.mCanAddRestrictedProfile = false;
                }
                caps.mIsGuest = userManager.getUserInfo(UserHandle.myUserId()).isGuest();
                canAddUsersWhenLocked = caps.mIsOwner || Global.getInt(context.getContentResolver(), "add_users_when_locked", 0) == 1;
                if (caps.mIsGuest || disallowAddUser) {
                    canAddUsersWhenLocked = false;
                }
                caps.mCanAddGuest = canAddUsersWhenLocked;
                return caps;
            }
            caps.mCanAddUser = false;
            caps.mCanAddRestrictedProfile = false;
            caps.mIsGuest = userManager.getUserInfo(UserHandle.myUserId()).isGuest();
            if (!caps.mIsOwner) {
            }
            canAddUsersWhenLocked = false;
            caps.mCanAddGuest = canAddUsersWhenLocked;
            return caps;
        }

        public String toString() {
            return "UserCapabilities{mEnabled=" + this.mEnabled + ", mCanAddUser=" + this.mCanAddUser + ", mCanAddRestrictedProfile=" + this.mCanAddRestrictedProfile + ", mIsOwner=" + this.mIsOwner + ", mIsGuest=" + this.mIsGuest + '}';
        }
    }

    protected int getMetricsCategory() {
        return 96;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            if (icicle.containsKey("adding_user")) {
                this.mAddedUserId = icicle.getInt("adding_user");
            }
            if (icicle.containsKey("removing_user")) {
                this.mRemovingUserId = icicle.getInt("removing_user");
            }
            this.mEditUserInfoController.onRestoreInstanceState(icicle);
        }
        Context context = getActivity();
        this.mUserCaps = UserCapabilities.create(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
        if (this.mUserCaps.mEnabled) {
            int myUserId = UserHandle.myUserId();
            addPreferencesFromResource(R.xml.user_settings);
            this.mUserListCategory = (PreferenceGroup) findPreference("user_list");
            this.mMePreference = new UserPreference(context, null, myUserId, null, null);
            this.mMePreference.setKey("user_me");
            this.mMePreference.setOnPreferenceClickListener(this);
            if (this.mUserCaps.mIsOwner) {
                this.mMePreference.setSummary(R.string.user_owner);
            }
            this.mAddUser = findPreference("user_add");
            if (this.mUserCaps.mCanAddUser) {
                this.mAddUser.setOnPreferenceClickListener(this);
                if (!this.mUserCaps.mCanAddRestrictedProfile) {
                    this.mAddUser.setTitle(R.string.user_add_user_menu);
                }
            }
            loadProfile();
            setHasOptionsMenu(true);
            IntentFilter filter = new IntentFilter("android.intent.action.USER_REMOVED");
            filter.addAction("android.intent.action.USER_INFO_CHANGED");
            context.registerReceiverAsUser(this.mUserChangeReceiver, UserHandle.ALL, filter, null, this.mHandler);
            if (Global.getInt(getContext().getContentResolver(), "device_provisioned", 0) == 0) {
                getActivity().finish();
            }
        }
    }

    public void onResume() {
        super.onResume();
        if (this.mUserCaps.mEnabled) {
            loadProfile();
            updateUserList();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mUserCaps.mEnabled) {
            getActivity().unregisterReceiver(this.mUserChangeReceiver);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mEditUserInfoController.onSaveInstanceState(outState);
        outState.putInt("adding_user", this.mAddedUserId);
        outState.putInt("removing_user", this.mRemovingUserId);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        this.mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, requestCode);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean z = true;
        int pos = 0;
        UserManager um = (UserManager) getActivity().getSystemService("user");
        if (!(this.mUserCaps.mIsOwner || um.hasUserRestriction("no_remove_user"))) {
            String nickname = this.mUserManager.getUserName();
            pos = 1;
            menu.add(0, 1, 0, getResources().getString(R.string.user_remove_user_menu, new Object[]{nickname})).setShowAsAction(0);
        }
        if (this.mUserCaps.mIsOwner && !um.hasUserRestriction("no_add_user")) {
            int pos2 = pos + 1;
            MenuItem allowAddOnLockscreen = menu.add(0, 2, pos, R.string.user_add_on_lockscreen_menu);
            allowAddOnLockscreen.setCheckable(true);
            if (Global.getInt(getContentResolver(), "add_users_when_locked", 0) != 1) {
                z = false;
            }
            allowAddOnLockscreen.setChecked(z);
            pos = pos2;
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        boolean z = false;
        int itemId = item.getItemId();
        if (itemId == 1) {
            onRemoveUserClicked(UserHandle.myUserId());
            return true;
        } else if (itemId != 2) {
            return super.onOptionsItemSelected(item);
        } else {
            int i;
            boolean isChecked = item.isChecked();
            ContentResolver contentResolver = getContentResolver();
            String str = "add_users_when_locked";
            if (isChecked) {
                i = 0;
            } else {
                i = 1;
            }
            Global.putInt(contentResolver, str, i);
            if (!isChecked) {
                z = true;
            }
            item.setChecked(z);
            return true;
        }
    }

    private void loadProfile() {
        if (this.mUserCaps.mIsGuest) {
            this.mMePreference.setIcon(getEncircledDefaultIcon());
            this.mMePreference.setTitle(R.string.user_exit_guest_title);
            return;
        }
        new C05634().execute(new Void[0]);
    }

    private void finishLoadProfile(String profileName) {
        if (getActivity() != null) {
            this.mMePreference.setTitle(getString(R.string.user_you, new Object[]{profileName}));
            int myUserId = UserHandle.myUserId();
            Bitmap b = this.mUserManager.getUserIcon(myUserId);
            if (b != null) {
                this.mMePreference.setIcon(encircle(b));
                this.mUserIcons.put(myUserId, b);
            }
        }
    }

    private boolean hasLockscreenSecurity() {
        return new LockPatternUtils(getActivity()).isSecure(UserHandle.myUserId());
    }

    private void launchChooseLockscreen() {
        Intent chooseLockIntent = new Intent("android.app.action.SET_NEW_PASSWORD");
        chooseLockIntent.putExtra("minimum_quality", 65536);
        startActivityForResult(chooseLockIntent, 10);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 10) {
            this.mEditUserInfoController.onActivityResult(requestCode, resultCode, data);
        } else if (resultCode != 0 && hasLockscreenSecurity()) {
            addUserNow(2);
        }
    }

    private void onAddUserClicked(int userType) {
        synchronized (this.mUserLock) {
            if (this.mRemovingUserId == -1 && !this.mAddingUser) {
                switch (userType) {
                    case 1:
                        showDialog(2);
                        break;
                    case 2:
                        if (!hasLockscreenSecurity()) {
                            showDialog(7);
                            break;
                        } else {
                            addUserNow(2);
                            break;
                        }
                    default:
                        break;
                }
            }
        }
    }

    private void onRemoveUserClicked(int userId) {
        synchronized (this.mUserLock) {
            if (this.mRemovingUserId == -1 && !this.mAddingUser) {
                this.mRemovingUserId = userId;
                showDialog(1);
            }
        }
    }

    private UserInfo createLimitedUser() {
        int i = 0;
        UserInfo newUserInfo = this.mUserManager.createSecondaryUser(getResources().getString(R.string.user_new_profile_name), 8);
        int userId = newUserInfo.id;
        UserHandle user = new UserHandle(userId);
        this.mUserManager.setUserRestriction("no_modify_accounts", true, user);
        Secure.putIntForUser(getContentResolver(), "location_mode", 0, userId);
        this.mUserManager.setUserRestriction("no_share_location", true, user);
        assignDefaultPhoto(newUserInfo);
        AccountManager am = AccountManager.get(getActivity());
        Account[] accounts = am.getAccounts();
        if (accounts != null) {
            int length = accounts.length;
            while (i < length) {
                am.addSharedAccount(accounts[i], user);
                i++;
            }
        }
        return newUserInfo;
    }

    private UserInfo createTrustedUser() {
        UserInfo newUserInfo = this.mUserManager.createSecondaryUser(getResources().getString(R.string.user_new_user_name), 0);
        if (newUserInfo != null) {
            assignDefaultPhoto(newUserInfo);
        }
        return newUserInfo;
    }

    private void onManageUserClicked(int userId, boolean newUser) {
        if (userId == -11) {
            Bundle extras = new Bundle();
            extras.putBoolean("guest_user", true);
            ((SettingsActivity) getActivity()).startPreferencePanel(UserDetailsSettings.class.getName(), extras, R.string.user_guest, null, null, 0);
            return;
        }
        UserInfo info = this.mUserManager.getUserInfo(userId);
        if (info.isRestricted() && this.mUserCaps.mIsOwner) {
            extras = new Bundle();
            extras.putInt("user_id", userId);
            extras.putBoolean("new_user", newUser);
            ((SettingsActivity) getActivity()).startPreferencePanel(RestrictedProfileSettings.class.getName(), extras, R.string.user_restrictions_title, null, null, 0);
        } else if (info.id == UserHandle.myUserId()) {
            OwnerInfoSettings.show(this);
        } else if (this.mUserCaps.mIsOwner) {
            extras = new Bundle();
            extras.putInt("user_id", userId);
            ((SettingsActivity) getActivity()).startPreferencePanel(UserDetailsSettings.class.getName(), extras, -1, info.name, null, 0);
        }
    }

    private void onUserCreated(int userId) {
        this.mAddedUserId = userId;
        if (this.mUserManager.getUserInfo(userId).isRestricted()) {
            showDialog(4);
        } else {
            showDialog(3);
        }
    }

    public void onDialogShowing() {
        super.onDialogShowing();
        setOnDismissListener(this);
    }

    public Dialog onCreateDialog(int dialogId) {
        Context context = getActivity();
        if (context == null) {
            return null;
        }
        switch (dialogId) {
            case 1:
                return UserDialogs.createRemoveDialog(getActivity(), this.mRemovingUserId, new C05645());
            case 2:
                int messageResId;
                SharedPreferences preferences = getActivity().getPreferences(0);
                boolean longMessageDisplayed = preferences.getBoolean("key_add_user_long_message_displayed", false);
                if (longMessageDisplayed) {
                    messageResId = R.string.user_add_user_message_short;
                } else {
                    messageResId = R.string.user_add_user_message_long;
                }
                final int i = dialogId == 2 ? 1 : 2;
                final boolean z = longMessageDisplayed;
                final SharedPreferences sharedPreferences = preferences;
                return new Builder(context).setTitle(R.string.user_add_user_title).setMessage(messageResId).setPositiveButton(17039370, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        UserSettings.this.addUserNow(i);
                        if (!z) {
                            sharedPreferences.edit().putBoolean("key_add_user_long_message_displayed", true).apply();
                        }
                    }
                }).setNegativeButton(17039360, null).create();
            case 3:
                return new Builder(context).setTitle(R.string.user_setup_dialog_title).setMessage(R.string.user_setup_dialog_message).setPositiveButton(R.string.user_setup_button_setup_now, new C05667()).setNegativeButton(R.string.user_setup_button_setup_later, null).create();
            case 4:
                return new Builder(context).setMessage(R.string.user_setup_profile_dialog_message).setPositiveButton(17039370, new C05678()).setNegativeButton(17039360, null).create();
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                return new Builder(context).setMessage(R.string.user_cannot_manage_message).setPositiveButton(17039370, null).create();
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                List<HashMap<String, String>> data = new ArrayList();
                HashMap<String, String> addUserItem = new HashMap();
                addUserItem.put("title", getString(R.string.user_add_user_item_title));
                addUserItem.put("summary", getString(R.string.user_add_user_item_summary));
                HashMap<String, String> addProfileItem = new HashMap();
                addProfileItem.put("title", getString(R.string.user_add_profile_item_title));
                addProfileItem.put("summary", getString(R.string.user_add_profile_item_summary));
                data.add(addUserItem);
                data.add(addProfileItem);
                Builder builder = new Builder(context);
                SimpleAdapter adapter = new SimpleAdapter(builder.getContext(), data, R.layout.two_line_list_item, new String[]{"title", "summary"}, new int[]{R.id.title, R.id.summary});
                builder.setTitle(R.string.user_add_user_type_title);
                builder.setAdapter(adapter, new C05689());
                return builder.create();
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                return new Builder(context).setMessage(R.string.user_need_lock_message).setPositiveButton(R.string.user_set_lock_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        UserSettings.this.launchChooseLockscreen();
                    }
                }).setNegativeButton(17039360, null).create();
            case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                return new Builder(context).setTitle(R.string.user_exit_guest_confirm_title).setMessage(R.string.user_exit_guest_confirm_message).setPositiveButton(R.string.user_exit_guest_dialog_remove, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        UserSettings.this.exitGuest();
                    }
                }).setNegativeButton(17039360, null).create();
            case 9:
                return this.mEditUserInfoController.createDialog(this, this.mMePreference.getIcon(), this.mMePreference.getTitle(), R.string.profile_info_settings_title, this, Process.myUserHandle());
            default:
                return null;
        }
    }

    private void removeUserNow() {
        if (this.mRemovingUserId == UserHandle.myUserId()) {
            removeThisUser();
            return;
        }
        showDeleteUserDialog();
        new Thread() {
            public void run() {
                synchronized (UserSettings.this.mUserLock) {
                    UserSettings.this.mUserManager.removeUser(UserSettings.this.mRemovingUserId);
                    UserSettings.this.mHandler.sendEmptyMessage(1);
                }
            }
        }.start();
    }

    private void removeThisUser() {
        try {
            ActivityManagerNative.getDefault().switchUser(0);
            ((UserManager) getActivity().getSystemService("user")).removeUser(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.e("UserSettings", "Unable to remove self user");
        }
    }

    private void addUserNow(final int userType) {
        synchronized (this.mUserLock) {
            this.mAddingUser = true;
            new Thread() {
                public void run() {
                    UserInfo user;
                    if (userType == 1) {
                        user = UserSettings.this.createTrustedUser();
                    } else {
                        user = UserSettings.this.createLimitedUser();
                    }
                    synchronized (UserSettings.this.mUserLock) {
                        UserSettings.this.mAddingUser = false;
                        if (userType == 1) {
                            UserSettings.this.mHandler.sendEmptyMessage(1);
                            UserSettings.this.mHandler.sendMessage(UserSettings.this.mHandler.obtainMessage(2, user.id, user.serialNumber));
                        } else {
                            UserSettings.this.mHandler.sendMessage(UserSettings.this.mHandler.obtainMessage(3, user.id, user.serialNumber));
                        }
                    }
                }
            }.start();
        }
    }

    private void switchUserNow(int userId) {
        try {
            ActivityManagerNative.getDefault().switchUser(userId);
        } catch (RemoteException e) {
        }
    }

    private void exitGuest() {
        if (this.mUserCaps.mIsGuest) {
            removeThisUser();
        }
    }

    private void updateUserList() {
        this.mUpdateUserListOperate = true;
        if (getActivity() != null) {
            UserPreference userPreference;
            PreferenceGroup groupToAddUsers;
            List<UserInfo> users = this.mUserManager.getUsers(true);
            Context context = getActivity();
            boolean voiceCapable = Utils.isVoiceCapable(context);
            ArrayList<Integer> missingIcons = new ArrayList();
            ArrayList<UserPreference> userPreferences = new ArrayList();
            userPreferences.add(this.mMePreference);
            for (UserInfo user : users) {
                if (!user.isManagedProfile()) {
                    UserPreference pref;
                    if (user.id == UserHandle.myUserId()) {
                        pref = this.mMePreference;
                    } else if (!user.isGuest()) {
                        boolean isRestricted = this.mUserCaps.mIsOwner ? !voiceCapable ? user.isRestricted() : true : false;
                        boolean showDelete = this.mUserCaps.mIsOwner ? (voiceCapable || user.isRestricted() || user.isGuest()) ? false : true : false;
                        pref = new UserPreference(context, null, user.id, isRestricted ? this : null, showDelete ? this : null);
                        pref.setOnPreferenceClickListener(this);
                        pref.setKey("id=" + user.id);
                        userPreferences.add(pref);
                        if (user.id == 0) {
                            pref.setSummary(R.string.user_owner);
                        }
                        pref.setTitle(user.name);
                    }
                    if (isInitialized(user)) {
                        if (user.isRestricted()) {
                            pref.setSummary(R.string.user_summary_restricted_profile);
                        }
                    } else if (user.isRestricted()) {
                        pref.setSummary(R.string.user_summary_restricted_not_set_up);
                    } else {
                        pref.setSummary(R.string.user_summary_not_set_up);
                    }
                    if (user.iconPath == null) {
                        pref.setIcon(getEncircledDefaultIcon());
                    } else if (this.mUserIcons.get(user.id) == null) {
                        missingIcons.add(Integer.valueOf(user.id));
                        pref.setIcon(getEncircledDefaultIcon());
                    } else {
                        setPhotoId(pref, user);
                    }
                }
            }
            if (this.mAddingUser) {
                userPreference = new UserPreference(getActivity(), null, -10, null, null);
                userPreference.setEnabled(false);
                userPreference.setTitle(R.string.user_new_user_name);
                userPreference.setIcon(getEncircledDefaultIcon());
                userPreferences.add(userPreference);
            }
            if (!this.mUserCaps.mIsGuest && (this.mUserCaps.mCanAddGuest || findGuest() != null)) {
                Context activity = getActivity();
                OnClickListener onClickListener = (this.mUserCaps.mIsOwner && voiceCapable) ? this : null;
                userPreference = new UserPreference(activity, null, -11, onClickListener, null);
                userPreference.setTitle(R.string.user_guest);
                userPreference.setIcon(getEncircledDefaultIcon());
                userPreference.setOnPreferenceClickListener(this);
                userPreferences.add(userPreference);
            }
            Collections.sort(userPreferences, UserPreference.SERIAL_NUMBER_COMPARATOR);
            getActivity().invalidateOptionsMenu();
            if (missingIcons.size() > 0) {
                loadIconsAsync(missingIcons);
            }
            PreferenceGroup preferenceScreen = getPreferenceScreen();
            preferenceScreen.removeAll();
            if (this.mUserCaps.mCanAddRestrictedProfile) {
                this.mUserListCategory.removeAll();
                this.mUserListCategory.setOrder(Integer.MAX_VALUE);
                preferenceScreen.addPreference(this.mUserListCategory);
                groupToAddUsers = this.mUserListCategory;
            } else {
                groupToAddUsers = preferenceScreen;
            }
            for (Preference userPreference2 : userPreferences) {
                userPreference2.setOrder(Integer.MAX_VALUE);
                groupToAddUsers.addPreference(userPreference2);
            }
            if (this.mUserCaps.mCanAddUser) {
                boolean moreUsers = this.mUserManager.canAddMoreUsers();
                this.mAddUser.setOrder(Integer.MAX_VALUE);
                preferenceScreen.addPreference(this.mAddUser);
                this.mAddUser.setEnabled(moreUsers);
                if (moreUsers) {
                    this.mAddUser.setSummary(null);
                } else {
                    this.mAddUser.setSummary(getString(R.string.user_add_max_count, new Object[]{Integer.valueOf(getMaxRealUsers())}));
                }
            }
            this.mUpdateUserListOperate = false;
        }
    }

    private int getMaxRealUsers() {
        int maxUsersAndGuest = UserManager.getMaxSupportedUsers() + 1;
        int managedProfiles = 0;
        for (UserInfo user : this.mUserManager.getUsers()) {
            if (user.isManagedProfile()) {
                managedProfiles++;
            }
        }
        return maxUsersAndGuest - managedProfiles;
    }

    private void loadIconsAsync(List<Integer> missingIcons) {
        new AsyncTask<List<Integer>, Void, Void>() {
            protected void onPostExecute(Void result) {
                UserSettings.this.updateUserList();
            }

            protected Void doInBackground(List<Integer>... values) {
                for (Integer intValue : values[0]) {
                    int userId = intValue.intValue();
                    Bitmap bitmap = UserSettings.this.mUserManager.getUserIcon(userId);
                    if (bitmap == null) {
                        bitmap = Utils.getDefaultUserIconAsBitmap(userId);
                    }
                    UserSettings.this.mUserIcons.append(userId, bitmap);
                }
                return null;
            }
        }.execute(new List[]{missingIcons});
    }

    private void assignProfilePhoto(UserInfo user) {
        if (!Utils.copyMeProfilePhoto(getActivity(), user)) {
            assignDefaultPhoto(user);
        }
    }

    private void assignDefaultPhoto(UserInfo user) {
        this.mUserManager.setUserIcon(user.id, Utils.getDefaultUserIconAsBitmap(user.id));
    }

    private Drawable getEncircledDefaultIcon() {
        if (this.mDefaultIconDrawable == null) {
            this.mDefaultIconDrawable = encircle(Utils.getDefaultUserIconAsBitmap(-10000));
        }
        return this.mDefaultIconDrawable;
    }

    private void setPhotoId(Preference pref, UserInfo user) {
        Bitmap bitmap = (Bitmap) this.mUserIcons.get(user.id);
        if (bitmap != null) {
            pref.setIcon(encircle(bitmap));
        }
    }

    private void setUserName(String name) {
        this.mUserManager.setUserName(UserHandle.myUserId(), name);
        this.mNicknamePreference.setSummary(name);
        getActivity().invalidateOptionsMenu();
    }

    public boolean onPreferenceClick(Preference pref) {
        if (pref == this.mMePreference) {
            if (this.mUserCaps.mIsGuest) {
                showDialog(8);
                return true;
            } else if (this.mUserManager.isLinkedUser()) {
                onManageUserClicked(UserHandle.myUserId(), false);
            } else {
                showDialog(9);
            }
        } else if (pref instanceof UserPreference) {
            int userId = ((UserPreference) pref).getUserId();
            if (userId == -11) {
                createAndSwitchToGuestUser();
            } else {
                UserInfo user = this.mUserManager.getUserInfo(userId);
                if (isInitialized(user)) {
                    switchUserNow(userId);
                } else {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(2, user.id, user.serialNumber));
                }
            }
        } else if (pref == this.mAddUser) {
            if (this.mUserCaps.mCanAddRestrictedProfile) {
                showDialog(6);
            } else {
                onAddUserClicked(1);
            }
        }
        return false;
    }

    private void createAndSwitchToGuestUser() {
        UserInfo guest = findGuest();
        if (guest != null) {
            switchUserNow(guest.id);
            return;
        }
        UserInfo guestUser = this.mUserManager.createGuest(getActivity(), getResources().getString(R.string.user_guest));
        if (guestUser != null) {
            switchUserNow(guestUser.id);
        }
    }

    private UserInfo findGuest() {
        for (UserInfo user : this.mUserManager.getUsers()) {
            if (user.isGuest()) {
                return user;
            }
        }
        return null;
    }

    private boolean isInitialized(UserInfo user) {
        return (user.flags & 16) != 0;
    }

    private Drawable encircle(Bitmap icon) {
        return CircleFramedDrawable.getInstance(getActivity(), icon);
    }

    public void onClick(View v) {
        if (v.getTag() instanceof UserPreference) {
            int userId = ((UserPreference) v.getTag()).getUserId();
            switch (v.getId()) {
                case R.id.manage_user:
                    onManageUserClicked(userId, false);
                    break;
                case R.id.trash_user:
                    if (!this.mUpdateUserListOperate) {
                        onRemoveUserClicked(userId);
                        break;
                    }
                    return;
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
        synchronized (this.mUserLock) {
            this.mAddingUser = false;
            this.mRemovingUserId = -1;
            updateUserList();
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference != this.mNicknamePreference) {
            return false;
        }
        String value = (String) newValue;
        if (preference == this.mNicknamePreference && value != null && value.length() > 0) {
            setUserName(value);
        }
        return true;
    }

    public int getHelpResource() {
        return R.string.help_url_users;
    }

    public void onPhotoChanged(Drawable photo) {
        this.mMePreference.setIcon(photo);
    }

    public void onLabelChanged(CharSequence label) {
        this.mMePreference.setTitle(label);
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
}
