package com.android.settings.users;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;

public final class UserDialogs {
    public static Dialog createRemoveDialog(Context context, int removingUserId, OnClickListener onConfirmListener) {
        UserInfo userInfo = ((UserManager) context.getSystemService("user")).getUserInfo(removingUserId);
        Builder builder = new Builder(context).setPositiveButton(R.string.user_delete_button, onConfirmListener).setNegativeButton(17039360, null);
        if (UserHandle.myUserId() == removingUserId) {
            builder.setTitle(R.string.user_confirm_remove_self_title);
            builder.setMessage(R.string.user_confirm_remove_self_message);
        } else if (userInfo.isRestricted()) {
            builder.setTitle(R.string.user_profile_confirm_remove_title);
            builder.setMessage(R.string.user_profile_confirm_remove_message);
        } else if (userInfo.isManagedProfile()) {
            builder.setTitle(R.string.work_profile_confirm_remove_title);
            View view = createRemoveManagedUserDialogView(context, removingUserId);
            if (view != null) {
                builder.setView(view);
            } else {
                builder.setMessage(R.string.work_profile_confirm_remove_message);
            }
        } else {
            builder.setTitle(R.string.user_confirm_remove_title);
            builder.setMessage(R.string.user_confirm_remove_message);
        }
        return builder.create();
    }

    private static View createRemoveManagedUserDialogView(Context context, int userId) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo mdmApplicationInfo = Utils.getAdminApplicationInfo(context, userId);
        if (mdmApplicationInfo == null) {
            return null;
        }
        View view = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.delete_managed_profile_dialog, null);
        ((ImageView) view.findViewById(R.id.delete_managed_profile_mdm_icon_view)).setImageDrawable(packageManager.getUserBadgedIcon(packageManager.getApplicationIcon(mdmApplicationInfo), new UserHandle(userId)));
        CharSequence appLabel = packageManager.getApplicationLabel(mdmApplicationInfo);
        CharSequence badgedAppLabel = packageManager.getUserBadgedLabel(appLabel, new UserHandle(userId));
        TextView textView = (TextView) view.findViewById(R.id.delete_managed_profile_device_manager_name);
        textView.setText(appLabel);
        if (!appLabel.toString().contentEquals(badgedAppLabel)) {
            textView.setContentDescription(badgedAppLabel);
        }
        return view;
    }

    public static Dialog createEnablePhoneCallsAndSmsDialog(Context context, OnClickListener onConfirmListener) {
        return new Builder(context).setTitle(R.string.user_enable_calling_and_sms_confirm_title).setMessage(R.string.user_enable_calling_and_sms_confirm_message).setPositiveButton(R.string.okay, onConfirmListener).setNegativeButton(17039360, null).create();
    }

    public static Dialog createEnablePhoneCallsDialog(Context context, OnClickListener onConfirmListener) {
        return new Builder(context).setTitle(R.string.user_enable_calling_confirm_title).setMessage(R.string.user_enable_calling_confirm_message).setPositiveButton(R.string.okay, onConfirmListener).setNegativeButton(17039360, null).create();
    }
}
