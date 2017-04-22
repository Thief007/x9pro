package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class RebootDialog extends Activity {
    static int DIALOG_LOCK = 0;
    static int DIALOG_UNLOCK = 1;
    Handler handler = new Handler();
    Runnable runnable = new C01721();

    class C01721 implements Runnable {
        C01721() {
        }

        public void run() {
            Intent i = new Intent("android.intent.action.REBOOT");
            i.putExtra("nowait", 1);
            i.putExtra("interval", 1);
            i.putExtra("window", 0);
            RebootDialog.this.sendBroadcast(i);
        }
    }

    class C01732 implements OnClickListener {
        C01732() {
        }

        public void onClick(DialogInterface arg0, int arg1) {
            Intent i = new Intent("android.intent.action.REBOOT");
            i.putExtra("nowait", 1);
            i.putExtra("interval", 1);
            i.putExtra("window", 0);
            RebootDialog.this.sendBroadcast(i);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String reason = getIntent().getStringExtra("reason");
        if ("lock".equals(reason)) {
            showDialog(DIALOG_LOCK);
        } else if ("unlock".equals(reason)) {
            showDialog(DIALOG_UNLOCK);
        }
    }

    protected Dialog onCreateDialog(int id) {
        this.handler.postDelayed(this.runnable, 5000);
        return new Builder(this).setCancelable(false).setTitle(R.string.strAttention).setMessage(id == 0 ? R.string.nck_lock_reboot : R.string.nck_unlock_reboot).setPositiveButton(R.string.reboot_confirm, new C01732()).create();
    }
}
