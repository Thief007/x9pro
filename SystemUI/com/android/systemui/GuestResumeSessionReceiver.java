package com.android.systemui;

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.System;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;

public class GuestResumeSessionReceiver extends BroadcastReceiver {
    private Dialog mNewSessionDialog;

    private static class ResetSessionDialog extends SystemUIDialog implements OnClickListener {
        private final int mUserId;

        public ResetSessionDialog(Context context, int userId) {
            super(context);
            setTitle(context.getString(R.string.guest_wipe_session_title));
            setMessage(context.getString(R.string.guest_wipe_session_message));
            setCanceledOnTouchOutside(false);
            setButton(-2, context.getString(R.string.guest_wipe_session_wipe), this);
            setButton(-1, context.getString(R.string.guest_wipe_session_dontwipe), this);
            this.mUserId = userId;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                GuestResumeSessionReceiver.wipeGuestSession(getContext(), this.mUserId);
                dismiss();
            } else if (which == -1) {
                cancel();
            }
        }
    }

    public void register(Context context) {
        context.registerReceiverAsUser(this, UserHandle.OWNER, new IntentFilter("android.intent.action.USER_SWITCHED"), null, null);
    }

    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
            cancelDialog();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (userId == -10000) {
                Log.e("GuestResumeSessionReceiver", intent + " sent to " + "GuestResumeSessionReceiver" + " without EXTRA_USER_HANDLE");
                return;
            }
            try {
                if (ActivityManagerNative.getDefault().getCurrentUser().isGuest()) {
                    ContentResolver cr = context.getContentResolver();
                    if (System.getIntForUser(cr, "systemui.guest_has_logged_in", 0, userId) != 0) {
                        this.mNewSessionDialog = new ResetSessionDialog(context, userId);
                        this.mNewSessionDialog.show();
                    } else {
                        System.putIntForUser(cr, "systemui.guest_has_logged_in", 1, userId);
                    }
                }
            } catch (RemoteException e) {
            }
        }
    }

    private static void wipeGuestSession(Context context, int userId) {
        UserManager userManager = (UserManager) context.getSystemService(FeatureOptionUtils.BUILD_TYPE_USER);
        try {
            UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            if (currentUser.id != userId) {
                Log.w("GuestResumeSessionReceiver", "User requesting to start a new session (" + userId + ")" + " is not current user (" + currentUser.id + ")");
            } else if (!currentUser.isGuest()) {
                Log.w("GuestResumeSessionReceiver", "User requesting to start a new session (" + userId + ")" + " is not a guest");
            } else if (userManager.markGuestForDeletion(currentUser.id)) {
                UserInfo newGuest = userManager.createGuest(context, currentUser.name);
                if (newGuest == null) {
                    try {
                        Log.e("GuestResumeSessionReceiver", "Could not create new guest, switching back to owner");
                        ActivityManagerNative.getDefault().switchUser(0);
                        userManager.removeUser(currentUser.id);
                        WindowManagerGlobal.getWindowManagerService().lockNow(null);
                        return;
                    } catch (RemoteException e) {
                        Log.e("GuestResumeSessionReceiver", "Couldn't wipe session because ActivityManager or WindowManager is dead");
                        return;
                    }
                }
                ActivityManagerNative.getDefault().switchUser(newGuest.id);
                userManager.removeUser(currentUser.id);
            } else {
                Log.w("GuestResumeSessionReceiver", "Couldn't mark the guest for deletion for user " + userId);
            }
        } catch (RemoteException e2) {
            Log.e("GuestResumeSessionReceiver", "Couldn't wipe session because ActivityManager is dead");
        }
    }

    private void cancelDialog() {
        if (this.mNewSessionDialog != null && this.mNewSessionDialog.isShowing()) {
            this.mNewSessionDialog.cancel();
            this.mNewSessionDialog = null;
        }
    }
}
