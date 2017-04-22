package com.android.settings.accounts;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.Utils;

public class AddAccountSettings extends Activity {
    private boolean mAddAccountCalled = false;
    private final AccountManagerCallback<Bundle> mCallback = new C02331();
    private PendingIntent mPendingIntent;
    private UserHandle mUserHandle;

    class C02331 implements AccountManagerCallback<Bundle> {
        public void run(android.accounts.AccountManagerFuture<android.os.Bundle> r11) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0076 in list [B:9:0x0071]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:42)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
            /*
            r10 = this;
            r2 = 1;
            r1 = r11.getResult();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r1 = (android.os.Bundle) r1;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = "intent";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r6 = r1.get(r7);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r6 = (android.content.Intent) r6;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            if (r6 == 0) goto L_0x0077;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x0012:
            r2 = 0;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r0 = new android.os.Bundle;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r0.<init>();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = "pendingIntent";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.mPendingIntent;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r0.putParcelable(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = "hasMultipleUsers";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = com.android.settings.Utils.hasMultipleUsers(r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r0.putBoolean(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = "android.intent.extra.USER";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.mUserHandle;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r0.putParcelable(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r6.putExtras(r0);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.mUserHandle;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r9 = 2;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7.startActivityForResultAsUser(r6, r9, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x004b:
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = 2;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = android.util.Log.isLoggable(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            if (r7 == 0) goto L_0x006f;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x0055:
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = new java.lang.StringBuilder;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8.<init>();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r9 = "account added: ";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.append(r9);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.append(r1);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.toString();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            android.util.Log.v(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x006f:
            if (r2 == 0) goto L_0x0076;
        L_0x0071:
            r7 = com.android.settings.accounts.AddAccountSettings.this;
            r7.finish();
        L_0x0076:
            return;
        L_0x0077:
            r7 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = -1;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7.setResult(r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = r7.mPendingIntent;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            if (r7 == 0) goto L_0x004b;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x0085:
            r7 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = r7.mPendingIntent;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7.cancel();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = com.android.settings.accounts.AddAccountSettings.this;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = 0;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7.mPendingIntent = r8;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            goto L_0x004b;
        L_0x0095:
            r4 = move-exception;
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = 2;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = android.util.Log.isLoggable(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            if (r7 == 0) goto L_0x00a9;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x00a0:
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = "addAccount was canceled";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            android.util.Log.v(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x00a9:
            if (r2 == 0) goto L_0x0076;
        L_0x00ab:
            r7 = com.android.settings.accounts.AddAccountSettings.this;
            r7.finish();
            goto L_0x0076;
        L_0x00b1:
            r3 = move-exception;
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = 2;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = android.util.Log.isLoggable(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            if (r7 == 0) goto L_0x00d6;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x00bc:
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = new java.lang.StringBuilder;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8.<init>();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r9 = "addAccount failed: ";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.append(r9);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.append(r3);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.toString();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            android.util.Log.v(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x00d6:
            if (r2 == 0) goto L_0x0076;
        L_0x00d8:
            r7 = com.android.settings.accounts.AddAccountSettings.this;
            r7.finish();
            goto L_0x0076;
        L_0x00de:
            r5 = move-exception;
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = 2;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r7 = android.util.Log.isLoggable(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            if (r7 == 0) goto L_0x0103;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x00e9:
            r7 = "AccountSettings";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = new java.lang.StringBuilder;	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8.<init>();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r9 = "addAccount failed: ";	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.append(r9);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.append(r5);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            r8 = r8.toString();	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
            android.util.Log.v(r7, r8);	 Catch:{ OperationCanceledException -> 0x0095, IOException -> 0x00de, AuthenticatorException -> 0x00b1, all -> 0x010c }
        L_0x0103:
            if (r2 == 0) goto L_0x0076;
        L_0x0105:
            r7 = com.android.settings.accounts.AddAccountSettings.this;
            r7.finish();
            goto L_0x0076;
        L_0x010c:
            r7 = move-exception;
            if (r2 == 0) goto L_0x0114;
        L_0x010f:
            r8 = com.android.settings.accounts.AddAccountSettings.this;
            r8.finish();
        L_0x0114:
            throw r7;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.settings.accounts.AddAccountSettings.1.run(android.accounts.AccountManagerFuture):void");
        }

        C02331() {
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mAddAccountCalled = savedInstanceState.getBoolean("AddAccountCalled");
            if (Log.isLoggable("AccountSettings", 2)) {
                Log.v("AccountSettings", "restored");
            }
        }
        UserManager um = (UserManager) getSystemService("user");
        this.mUserHandle = Utils.getSecureTargetUser(getActivityToken(), um, null, getIntent().getExtras());
        if (um.hasUserRestriction("no_modify_accounts", this.mUserHandle)) {
            Toast.makeText(this, R.string.user_cannot_add_accounts_message, 1).show();
            finish();
        } else if (this.mAddAccountCalled) {
            finish();
        } else {
            String[] authorities = getIntent().getStringArrayExtra("authorities");
            String[] accountTypes = getIntent().getStringArrayExtra("account_types");
            Intent intent = new Intent(this, ChooseAccountActivity.class);
            if (authorities != null) {
                intent.putExtra("authorities", authorities);
            }
            if (accountTypes != null) {
                intent.putExtra("account_types", accountTypes);
            }
            intent.putExtra("android.intent.extra.USER", this.mUserHandle);
            startActivityForResult(intent, 1);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode != 0) {
                    addAccount(data.getStringExtra("selected_account"));
                    break;
                }
                setResult(resultCode);
                finish();
                return;
            case 2:
                setResult(resultCode);
                if (this.mPendingIntent != null) {
                    this.mPendingIntent.cancel();
                    this.mPendingIntent = null;
                }
                finish();
                break;
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("AddAccountCalled", this.mAddAccountCalled);
        if (Log.isLoggable("AccountSettings", 2)) {
            Log.v("AccountSettings", "saved");
        }
    }

    private void addAccount(String accountType) {
        Bundle addAccountOptions = new Bundle();
        Intent identityIntent = new Intent();
        identityIntent.setComponent(new ComponentName("SHOULDN'T RESOLVE!", "SHOULDN'T RESOLVE!"));
        identityIntent.setAction("SHOULDN'T RESOLVE!");
        identityIntent.addCategory("SHOULDN'T RESOLVE!");
        this.mPendingIntent = PendingIntent.getBroadcast(this, 0, identityIntent, 0);
        addAccountOptions.putParcelable("pendingIntent", this.mPendingIntent);
        addAccountOptions.putBoolean("hasMultipleUsers", Utils.hasMultipleUsers(this));
        AccountManager.get(this).addAccountAsUser(accountType, null, null, addAccountOptions, null, this.mCallback, null, this.mUserHandle);
        this.mAddAccountCalled = true;
    }
}
