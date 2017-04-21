package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.internal.util.UserIcons;
import com.android.systemui.BitmapHelper;
import com.android.systemui.GuestResumeSessionReceiver;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.qs.tiles.UserDetailView;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.KeyguardMonitor.Callback;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserSwitcherController {
    private final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList();
    private Dialog mAddUserDialog;
    private boolean mAddUsersWhenLocked;
    private final Callback mCallback = new Callback() {
        public void onKeyguardChanged() {
            UserSwitcherController.this.notifyAdapters();
        }
    };
    private final Context mContext;
    private Dialog mExitGuestDialog;
    private SparseBooleanArray mForcePictureLoadForUserId = new SparseBooleanArray(2);
    private final GuestResumeSessionReceiver mGuestResumeSessionReceiver = new GuestResumeSessionReceiver();
    private final Handler mHandler;
    private final KeyguardMonitor mKeyguardMonitor;
    private int mLastNonGuestUser = 0;
    private boolean mPauseRefreshUsers;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean unpauseRefreshUsers = false;
            int forcePictureLoadForId = -10000;
            UserInfo userInfo;
            if ("com.android.systemui.REMOVE_GUEST".equals(intent.getAction())) {
                int currentUser = ActivityManager.getCurrentUser();
                userInfo = UserSwitcherController.this.mUserManager.getUserInfo(currentUser);
                if (userInfo != null && userInfo.isGuest()) {
                    UserSwitcherController.this.showExitGuestDialog(currentUser);
                }
                return;
            }
            int currentId;
            if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                currentId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                userInfo = UserSwitcherController.this.mUserManager.getUserInfo(currentId);
                if (userInfo != null && userInfo.isGuest()) {
                    showGuestNotification(currentId);
                }
            } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (UserSwitcherController.this.mExitGuestDialog != null && UserSwitcherController.this.mExitGuestDialog.isShowing()) {
                    UserSwitcherController.this.mExitGuestDialog.cancel();
                    UserSwitcherController.this.mExitGuestDialog = null;
                }
                currentId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                int N = UserSwitcherController.this.mUsers.size();
                int i = 0;
                while (i < N) {
                    UserRecord record = (UserRecord) UserSwitcherController.this.mUsers.get(i);
                    if (record.info != null) {
                        boolean shouldBeCurrent = record.info.id == currentId;
                        if (record.isCurrent != shouldBeCurrent) {
                            UserSwitcherController.this.mUsers.set(i, record.copyWithIsCurrent(shouldBeCurrent));
                        }
                        if (shouldBeCurrent && !record.isGuest) {
                            UserSwitcherController.this.mLastNonGuestUser = record.info.id;
                        }
                        if (currentId != 0 && record.isRestricted) {
                            UserSwitcherController.this.mUsers.remove(i);
                            i--;
                        }
                    }
                    i++;
                }
                UserSwitcherController.this.notifyAdapters();
                unpauseRefreshUsers = true;
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(intent.getAction())) {
                forcePictureLoadForId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            }
            UserSwitcherController.this.refreshUsers(forcePictureLoadForId);
            if (unpauseRefreshUsers) {
                UserSwitcherController.this.mUnpauseRefreshUsers.run();
            }
        }

        private void showGuestNotification(int guestUserId) {
            NotificationManager.from(UserSwitcherController.this.mContext).notifyAsUser("remove_guest", 1010, new Builder(UserSwitcherController.this.mContext).setVisibility(-1).setPriority(-2).setSmallIcon(R.drawable.ic_person).setContentTitle(UserSwitcherController.this.mContext.getString(R.string.guest_notification_title)).setContentText(UserSwitcherController.this.mContext.getString(R.string.guest_notification_text)).setShowWhen(false).addAction(R.drawable.ic_delete, UserSwitcherController.this.mContext.getString(R.string.guest_notification_remove_action), PendingIntent.getBroadcastAsUser(UserSwitcherController.this.mContext, 0, new Intent("com.android.systemui.REMOVE_GUEST"), 0, UserHandle.OWNER)).build(), new UserHandle(guestUserId));
        }
    };
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            boolean z;
            boolean z2 = true;
            UserSwitcherController userSwitcherController = UserSwitcherController.this;
            if (Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "lockscreenSimpleUserSwitcher", 0) != 0) {
                z = true;
            } else {
                z = false;
            }
            userSwitcherController.mSimpleUserSwitcher = z;
            UserSwitcherController userSwitcherController2 = UserSwitcherController.this;
            if (Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "add_users_when_locked", 0) == 0) {
                z2 = false;
            }
            userSwitcherController2.mAddUsersWhenLocked = z2;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    private boolean mSimpleUserSwitcher;
    private final Runnable mUnpauseRefreshUsers = new Runnable() {
        public void run() {
            UserSwitcherController.this.mHandler.removeCallbacks(this);
            UserSwitcherController.this.mPauseRefreshUsers = false;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    private final UserManager mUserManager;
    private ArrayList<UserRecord> mUsers = new ArrayList();
    public final DetailAdapter userDetailAdapter = new DetailAdapter() {
        private final Intent USER_SETTINGS_INTENT = new Intent("android.settings.USER_SETTINGS");

        public int getTitle() {
            return R.string.quick_settings_user_title;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            UserDetailView v;
            if (convertView instanceof UserDetailView) {
                v = (UserDetailView) convertView;
            } else {
                v = UserDetailView.inflate(context, parent, false);
                v.createAndSetAdapter(UserSwitcherController.this);
            }
            v.refreshAdapter();
            return v;
        }

        public Intent getSettingsIntent() {
            return this.USER_SETTINGS_INTENT;
        }

        public Boolean getToggleState() {
            return null;
        }

        public void setToggleState(boolean state) {
        }

        public int getMetricsCategory() {
            return 125;
        }
    };

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final UserSwitcherController mController;

        protected BaseUserAdapter(UserSwitcherController controller) {
            this.mController = controller;
            controller.mAdapters.add(new WeakReference(this));
        }

        public int getCount() {
            boolean secureKeyguardShowing = (this.mController.mKeyguardMonitor.isShowing() && this.mController.mKeyguardMonitor.isSecure()) ? !this.mController.mKeyguardMonitor.canSkipBouncer() : false;
            if (!secureKeyguardShowing) {
                return this.mController.mUsers.size();
            }
            int N = this.mController.mUsers.size();
            int count = 0;
            int i = 0;
            while (i < N && !((UserRecord) this.mController.mUsers.get(i)).isRestricted) {
                count++;
                i++;
            }
            return count;
        }

        public UserRecord getItem(int position) {
            return (UserRecord) this.mController.mUsers.get(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public void switchTo(UserRecord record) {
            this.mController.switchTo(record);
        }

        public String getName(Context context, UserRecord item) {
            if (item.isGuest) {
                if (item.isCurrent) {
                    return context.getString(R.string.guest_exit_guest);
                }
                return context.getString(item.info == null ? R.string.guest_new_guest : R.string.guest_nickname);
            } else if (item.isAddUser) {
                return context.getString(R.string.user_add_user);
            } else {
                return item.info.name;
            }
        }

        public Drawable getDrawable(Context context, UserRecord item) {
            if (item.isAddUser) {
                return context.getDrawable(R.drawable.ic_add_circle_qs);
            }
            return UserIcons.getDefaultUserIcon(item.isGuest ? -10000 : item.info.id, true);
        }

        public void refresh() {
            this.mController.refreshUsers(-10000);
        }
    }

    private final class AddUserDialog extends SystemUIDialog implements OnClickListener {
        public AddUserDialog(Context context) {
            super(context);
            setTitle(R.string.user_add_user_title);
            setMessage(context.getString(R.string.user_add_user_message_short));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(17039370), this);
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                cancel();
            } else {
                dismiss();
                if (!ActivityManager.isUserAMonkey()) {
                    UserInfo user = UserSwitcherController.this.mUserManager.createSecondaryUser(UserSwitcherController.this.mContext.getString(R.string.user_new_user_name), 0);
                    if (user != null) {
                        int id = user.id;
                        UserSwitcherController.this.mUserManager.setUserIcon(id, UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(id, false)));
                        UserSwitcherController.this.switchToUserId(id);
                    }
                }
            }
        }
    }

    private final class ExitGuestDialog extends SystemUIDialog implements OnClickListener {
        private final int mGuestId;

        public ExitGuestDialog(Context context, int guestId) {
            super(context);
            setTitle(R.string.guest_exit_guest_dialog_title);
            setMessage(context.getString(R.string.guest_exit_guest_dialog_message));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(R.string.guest_exit_guest_dialog_remove), this);
            setCanceledOnTouchOutside(false);
            this.mGuestId = guestId;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                cancel();
                return;
            }
            dismiss();
            UserSwitcherController.this.exitGuest(this.mGuestId);
        }
    }

    public static final class UserRecord {
        public final UserInfo info;
        public final boolean isAddUser;
        public final boolean isCurrent;
        public final boolean isGuest;
        public final boolean isRestricted;
        public final Bitmap picture;

        public UserRecord(UserInfo info, Bitmap picture, boolean isGuest, boolean isCurrent, boolean isAddUser, boolean isRestricted) {
            this.info = info;
            this.picture = picture;
            this.isGuest = isGuest;
            this.isCurrent = isCurrent;
            this.isAddUser = isAddUser;
            this.isRestricted = isRestricted;
        }

        public UserRecord copyWithIsCurrent(boolean _isCurrent) {
            return new UserRecord(this.info, this.picture, this.isGuest, _isCurrent, this.isAddUser, this.isRestricted);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UserRecord(");
            if (this.info != null) {
                sb.append("name=\"").append(this.info.name).append("\" id=").append(this.info.id);
            } else if (this.isGuest) {
                sb.append("<add guest placeholder>");
            } else if (this.isAddUser) {
                sb.append("<add user placeholder>");
            }
            if (this.isGuest) {
                sb.append(" <isGuest>");
            }
            if (this.isAddUser) {
                sb.append(" <isAddUser>");
            }
            if (this.isCurrent) {
                sb.append(" <isCurrent>");
            }
            if (this.picture != null) {
                sb.append(" <hasPicture>");
            }
            if (this.isRestricted) {
                sb.append(" <isRestricted>");
            }
            sb.append(')');
            return sb.toString();
        }
    }

    public UserSwitcherController(Context context, KeyguardMonitor keyguardMonitor, Handler handler) {
        this.mContext = context;
        this.mGuestResumeSessionReceiver.register(context);
        this.mKeyguardMonitor = keyguardMonitor;
        this.mHandler = handler;
        this.mUserManager = UserManager.get(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_INFO_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_STOPPING");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, filter, null, null);
        filter = new IntentFilter();
        filter.addAction("com.android.systemui.REMOVE_GUEST");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, filter, "com.android.systemui.permission.SELF", null);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("lockscreenSimpleUserSwitcher"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("add_users_when_locked"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
        keyguardMonitor.addCallback(this.mCallback);
        refreshUsers(-10000);
    }

    private void refreshUsers(int forcePictureLoadForId) {
        if (forcePictureLoadForId != -10000) {
            this.mForcePictureLoadForUserId.put(forcePictureLoadForId, true);
        }
        if (!this.mPauseRefreshUsers) {
            SparseArray<Bitmap> bitmaps = new SparseArray(this.mUsers.size());
            int N = this.mUsers.size();
            for (int i = 0; i < N; i++) {
                UserRecord r = (UserRecord) this.mUsers.get(i);
                if (!(r == null || r.picture == null || r.info == null || this.mForcePictureLoadForUserId.get(r.info.id))) {
                    bitmaps.put(r.info.id, r.picture);
                }
            }
            this.mForcePictureLoadForUserId.clear();
            final boolean addUsersWhenLocked = this.mAddUsersWhenLocked;
            new AsyncTask<SparseArray<Bitmap>, Void, ArrayList<UserRecord>>() {
                protected ArrayList<UserRecord> doInBackground(SparseArray<Bitmap>... params) {
                    SparseArray<Bitmap> bitmaps = params[0];
                    List<UserInfo> infos = UserSwitcherController.this.mUserManager.getUsers(true);
                    if (infos == null) {
                        return null;
                    }
                    ArrayList<UserRecord> arrayList = new ArrayList(infos.size());
                    int currentId = ActivityManager.getCurrentUser();
                    UserRecord guestRecord = null;
                    int avatarSize = UserSwitcherController.this.mContext.getResources().getDimensionPixelSize(R.dimen.max_avatar_size);
                    for (UserInfo info : infos) {
                        boolean isCurrent = currentId == info.id;
                        if (info.isGuest()) {
                            guestRecord = new UserRecord(info, null, true, isCurrent, false, false);
                        } else if (info.supportsSwitchTo()) {
                            Bitmap picture = (Bitmap) bitmaps.get(info.id);
                            if (picture == null) {
                                picture = UserSwitcherController.this.mUserManager.getUserIcon(info.id);
                                if (picture != null) {
                                    picture = BitmapHelper.createCircularClip(picture, avatarSize, avatarSize);
                                }
                            }
                            arrayList.add(isCurrent ? 0 : arrayList.size(), new UserRecord(info, picture, false, isCurrent, false, false));
                        }
                    }
                    boolean ownerCanCreateUsers = !UserSwitcherController.this.mUserManager.hasUserRestriction("no_add_user", UserHandle.OWNER);
                    boolean z = currentId == 0 ? ownerCanCreateUsers : false;
                    boolean z2 = ownerCanCreateUsers ? addUsersWhenLocked : false;
                    boolean canCreateGuest = (z || z2) ? guestRecord == null : false;
                    boolean canAddMoreUsers;
                    if (z || z2) {
                        canAddMoreUsers = UserSwitcherController.this.mUserManager.canAddMoreUsers();
                    } else {
                        canAddMoreUsers = false;
                    }
                    boolean createIsRestricted = !addUsersWhenLocked;
                    if (!UserSwitcherController.this.mSimpleUserSwitcher) {
                        if (guestRecord != null) {
                            arrayList.add(guestRecord.isCurrent ? 0 : arrayList.size(), guestRecord);
                        } else if (canCreateGuest) {
                            arrayList.add(new UserRecord(null, null, true, false, false, createIsRestricted));
                        }
                    }
                    if (!UserSwitcherController.this.mSimpleUserSwitcher && r21) {
                        arrayList.add(new UserRecord(null, null, false, false, true, createIsRestricted));
                    }
                    return arrayList;
                }

                protected void onPostExecute(ArrayList<UserRecord> userRecords) {
                    if (userRecords != null) {
                        UserSwitcherController.this.mUsers = userRecords;
                        UserSwitcherController.this.notifyAdapters();
                    }
                }
            }.execute(new SparseArray[]{bitmaps});
        }
    }

    private void pauseRefreshUsers() {
        if (!this.mPauseRefreshUsers) {
            this.mHandler.postDelayed(this.mUnpauseRefreshUsers, 3000);
            this.mPauseRefreshUsers = true;
        }
    }

    private void notifyAdapters() {
        for (int i = this.mAdapters.size() - 1; i >= 0; i--) {
            BaseUserAdapter adapter = (BaseUserAdapter) ((WeakReference) this.mAdapters.get(i)).get();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            } else {
                this.mAdapters.remove(i);
            }
        }
    }

    public boolean isSimpleUserSwitcher() {
        return this.mSimpleUserSwitcher;
    }

    public void switchTo(UserRecord record) {
        int id;
        if (record.isGuest && record.info == null) {
            UserInfo guest = this.mUserManager.createGuest(this.mContext, this.mContext.getString(R.string.guest_nickname));
            if (guest != null) {
                id = guest.id;
            } else {
                return;
            }
        } else if (record.isAddUser) {
            showAddUserDialog();
            return;
        } else {
            id = record.info.id;
        }
        if (ActivityManager.getCurrentUser() == id) {
            if (record.isGuest) {
                showExitGuestDialog(id);
            }
            return;
        }
        switchToUserId(id);
    }

    private void switchToUserId(int id) {
        try {
            pauseRefreshUsers();
            ActivityManagerNative.getDefault().switchUser(id);
        } catch (RemoteException e) {
            Log.e("UserSwitcherController", "Couldn't switch user.", e);
        }
    }

    private void showExitGuestDialog(int id) {
        if (this.mExitGuestDialog != null && this.mExitGuestDialog.isShowing()) {
            this.mExitGuestDialog.cancel();
        }
        this.mExitGuestDialog = new ExitGuestDialog(this.mContext, id);
        this.mExitGuestDialog.show();
    }

    private void showAddUserDialog() {
        if (this.mAddUserDialog != null && this.mAddUserDialog.isShowing()) {
            this.mAddUserDialog.cancel();
        }
        this.mAddUserDialog = new AddUserDialog(this.mContext);
        this.mAddUserDialog.show();
    }

    private void exitGuest(int id) {
        int newId = 0;
        if (this.mLastNonGuestUser != 0) {
            UserInfo info = this.mUserManager.getUserInfo(this.mLastNonGuestUser);
            if (info != null && info.isEnabled() && info.supportsSwitchTo()) {
                newId = info.id;
            }
        }
        switchToUserId(newId);
        this.mUserManager.removeUser(id);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UserSwitcherController state:");
        pw.println("  mLastNonGuestUser=" + this.mLastNonGuestUser);
        pw.print("  mUsers.size=");
        pw.println(this.mUsers.size());
        for (int i = 0; i < this.mUsers.size(); i++) {
            UserRecord u = (UserRecord) this.mUsers.get(i);
            pw.print("    ");
            pw.println(u.toString());
        }
    }

    public String getCurrentUserName(Context context) {
        if (this.mUsers.isEmpty()) {
            return null;
        }
        UserRecord item = (UserRecord) this.mUsers.get(0);
        if (item == null || item.info == null) {
            return null;
        }
        if (item.isGuest) {
            return context.getString(R.string.guest_nickname);
        }
        return item.info.name;
    }

    public static boolean isUserSwitcherAvailable(UserManager um) {
        return UserManager.supportsMultipleUsers() ? um.isUserSwitcherEnabled() : false;
    }
}
