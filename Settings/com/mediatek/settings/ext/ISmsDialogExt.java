package com.mediatek.settings.ext;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController.AlertParams;

public interface ISmsDialogExt {
    void buildMessage(AlertParams alertParams, String str, Intent intent, String str2, String str3);

    boolean onClick(String str, AlertActivity alertActivity, Context context, int i);

    boolean onKeyDown(int i, KeyEvent keyEvent, AlertActivity alertActivity);
}
