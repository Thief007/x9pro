package com.android.systemui.statusbar.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.view.WindowManager.LayoutParams;
import com.android.systemui.R;

public class SystemUIDialog extends AlertDialog {
    private final Context mContext;

    public SystemUIDialog(Context context) {
        super(context, R.style.Theme.SystemUI.Dialog);
        this.mContext = context;
        getWindow().setType(2014);
        getWindow().addFlags(655360);
        LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle(getClass().getSimpleName());
        getWindow().setAttributes(attrs);
    }

    public void setShowForAllUsers(boolean show) {
        if (show) {
            LayoutParams attributes = getWindow().getAttributes();
            attributes.privateFlags |= 16;
            return;
        }
        attributes = getWindow().getAttributes();
        attributes.privateFlags &= -17;
    }

    public void setMessage(int resId) {
        setMessage(this.mContext.getString(resId));
    }

    public void setPositiveButton(int resId, OnClickListener onClick) {
        setButton(-1, this.mContext.getString(resId), onClick);
    }

    public void setNegativeButton(int resId, OnClickListener onClick) {
        setButton(-2, this.mContext.getString(resId), onClick);
    }
}
