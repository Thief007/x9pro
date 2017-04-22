package com.android.settings;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.security.KeyStore.State;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.harmony.security.utils.AlgNameMapper;

public final class CredentialStorage extends Activity {
    private static /* synthetic */ int[] -android_security_KeyStore$StateSwitchesValues;
    private static final int[] SYSTEM_CREDENTIAL_UIDS = new int[]{1010, 1016, 0, 1000};
    private static AlertDialog sConfigureKeyGuardDialog = null;
    private static AlertDialog sResetDialog = null;
    private static AlertDialog sUnlockDialog = null;
    private Bundle mInstallBundle;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private int mRetriesRemaining = -1;

    private class ConfigureKeyGuardDialog implements OnClickListener, OnDismissListener {
        private boolean mConfigureConfirmed;

        private ConfigureKeyGuardDialog() {
            if (CredentialStorage.sConfigureKeyGuardDialog == null) {
                AlertDialog dialog = new Builder(CredentialStorage.this).setTitle(17039380).setMessage(R.string.credentials_configure_lock_screen_hint).setPositiveButton(17039370, this).setNegativeButton(17039360, this).create();
                CredentialStorage.sConfigureKeyGuardDialog = dialog;
                dialog.setOnDismissListener(this);
                dialog.show();
            }
        }

        public void onClick(DialogInterface dialog, int button) {
            this.mConfigureConfirmed = button == -1;
        }

        public void onDismiss(DialogInterface dialog) {
            CredentialStorage.sConfigureKeyGuardDialog = null;
            if (this.mConfigureConfirmed) {
                this.mConfigureConfirmed = false;
                Intent intent = new Intent("android.app.action.SET_NEW_PASSWORD");
                intent.putExtra("minimum_quality", 65536);
                CredentialStorage.this.startActivity(intent);
                return;
            }
            CredentialStorage.this.finish();
        }
    }

    private class ResetDialog implements OnClickListener, OnDismissListener {
        private boolean mResetConfirmed;

        private ResetDialog() {
            if (CredentialStorage.sResetDialog == null) {
                AlertDialog dialog = new Builder(CredentialStorage.this).setTitle(17039380).setMessage(R.string.credentials_reset_hint).setPositiveButton(17039370, this).setNegativeButton(17039360, this).create();
                CredentialStorage.sResetDialog = dialog;
                dialog.setOnDismissListener(this);
                dialog.show();
            }
        }

        public void onClick(DialogInterface dialog, int button) {
            this.mResetConfirmed = button == -1;
        }

        public void onDismiss(DialogInterface dialog) {
            CredentialStorage.sResetDialog = null;
            if (this.mResetConfirmed) {
                this.mResetConfirmed = false;
                new ResetKeyStoreAndKeyChain().execute(new Void[0]);
                return;
            }
            CredentialStorage.this.finish();
        }
    }

    private class ResetKeyStoreAndKeyChain extends AsyncTask<Void, Void, Boolean> {
        private ResetKeyStoreAndKeyChain() {
        }

        protected Boolean doInBackground(Void... unused) {
            for (UserInfo pi : ((UserManager) CredentialStorage.this.getSystemService("user")).getProfiles(UserHandle.getUserId(Process.myUid()))) {
                for (int uid : CredentialStorage.SYSTEM_CREDENTIAL_UIDS) {
                    CredentialStorage.this.mKeyStore.clearUid(UserHandle.getUid(pi.id, uid));
                }
            }
            try {
                KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this);
                Boolean valueOf;
                try {
                    valueOf = Boolean.valueOf(keyChainConnection.getService().reset());
                    return valueOf;
                } catch (RemoteException e) {
                    valueOf = Boolean.valueOf(false);
                    return valueOf;
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e2) {
                Thread.currentThread().interrupt();
                return Boolean.valueOf(false);
            }
        }

        protected void onPostExecute(Boolean success) {
            if (success.booleanValue()) {
                Toast.makeText(CredentialStorage.this, R.string.credentials_erased, 0).show();
            } else {
                Toast.makeText(CredentialStorage.this, R.string.credentials_not_erased, 0).show();
            }
            CredentialStorage.this.finish();
        }
    }

    private class UnlockDialog implements TextWatcher, OnClickListener, OnDismissListener {
        private final Button mButton;
        private final TextView mError;
        private final TextView mOldPassword;
        private boolean mUnlockConfirmed;

        private UnlockDialog() {
            CharSequence text;
            View view = View.inflate(CredentialStorage.this, R.layout.credentials_dialog, null);
            if (CredentialStorage.this.mRetriesRemaining == -1) {
                text = CredentialStorage.this.getResources().getText(R.string.credentials_unlock_hint);
            } else if (CredentialStorage.this.mRetriesRemaining > 3) {
                text = CredentialStorage.this.getResources().getText(R.string.credentials_wrong_password);
            } else if (CredentialStorage.this.mRetriesRemaining == 1) {
                text = CredentialStorage.this.getResources().getText(R.string.credentials_reset_warning);
            } else {
                text = CredentialStorage.this.getString(R.string.credentials_reset_warning_plural, new Object[]{Integer.valueOf(this$0.mRetriesRemaining)});
            }
            ((TextView) view.findViewById(R.id.hint)).setText(text);
            this.mOldPassword = (TextView) view.findViewById(R.id.old_password);
            this.mOldPassword.setVisibility(0);
            this.mOldPassword.addTextChangedListener(this);
            this.mError = (TextView) view.findViewById(R.id.error);
            if (CredentialStorage.sUnlockDialog == null) {
                AlertDialog dialog = new Builder(CredentialStorage.this).setView(view).setTitle(R.string.credentials_unlock).setPositiveButton(17039370, this).setNegativeButton(17039360, this).create();
                CredentialStorage.sUnlockDialog = dialog;
                dialog.setOnDismissListener(this);
                dialog.show();
            }
            this.mButton = CredentialStorage.sUnlockDialog.getButton(-1);
            this.mButton.setEnabled(false);
        }

        public void afterTextChanged(Editable editable) {
            boolean z = true;
            Button button = this.mButton;
            if (this.mOldPassword != null && this.mOldPassword.getText().length() <= 0) {
                z = false;
            }
            button.setEnabled(z);
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void onClick(DialogInterface dialog, int button) {
            this.mUnlockConfirmed = button == -1;
        }

        public void onDismiss(DialogInterface dialog) {
            CredentialStorage.sUnlockDialog = null;
            if (this.mUnlockConfirmed) {
                this.mUnlockConfirmed = false;
                this.mError.setVisibility(0);
                CredentialStorage.this.mKeyStore.unlock(this.mOldPassword.getText().toString());
                int error = CredentialStorage.this.mKeyStore.getLastError();
                if (error == 1) {
                    CredentialStorage.this.mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this, R.string.credentials_enabled, 0).show();
                    CredentialStorage.this.ensureKeyGuard();
                } else if (error == 3) {
                    CredentialStorage.this.mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this, R.string.credentials_erased, 0).show();
                    CredentialStorage.this.handleUnlockOrInstall();
                } else if (error >= 10) {
                    CredentialStorage.this.mRetriesRemaining = (error - 10) + 1;
                    CredentialStorage.this.handleUnlockOrInstall();
                }
                return;
            }
            CredentialStorage.this.finish();
        }
    }

    private static /* synthetic */ int[] -getandroid_security_KeyStore$StateSwitchesValues() {
        if (-android_security_KeyStore$StateSwitchesValues != null) {
            return -android_security_KeyStore$StateSwitchesValues;
        }
        int[] iArr = new int[State.values().length];
        try {
            iArr[State.LOCKED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[State.UNINITIALIZED.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[State.UNLOCKED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        -android_security_KeyStore$StateSwitchesValues = iArr;
        return iArr;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sConfigureKeyGuardDialog = null;
        sResetDialog = null;
        sUnlockDialog = null;
    }

    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
        if (((UserManager) getSystemService("user")).hasUserRestriction("no_config_credentials")) {
            if ("com.android.credentials.UNLOCK".equals(action) && this.mKeyStore.state() == State.UNINITIALIZED) {
                ensureKeyGuard();
            } else {
                finish();
            }
        } else if ("com.android.credentials.RESET".equals(action)) {
            ResetDialog resetDialog = new ResetDialog();
        } else {
            if ("com.android.credentials.INSTALL".equals(action) && checkCallerIsCertInstallerOrSelfInProfile()) {
                this.mInstallBundle = intent.getExtras();
            }
            handleUnlockOrInstall();
        }
    }

    protected void onDestroy() {
        if (sConfigureKeyGuardDialog != null) {
            sConfigureKeyGuardDialog = null;
        }
        if (sResetDialog != null) {
            sResetDialog = null;
        }
        if (sUnlockDialog != null) {
            sUnlockDialog = null;
        }
        super.onDestroy();
    }

    private void handleUnlockOrInstall() {
        if (!isFinishing()) {
            switch (-getandroid_security_KeyStore$StateSwitchesValues()[this.mKeyStore.state().ordinal()]) {
                case 1:
                    UnlockDialog unlockDialog = new UnlockDialog();
                    return;
                case 2:
                    ensureKeyGuard();
                    return;
                case 3:
                    if (checkKeyGuardQuality()) {
                        installIfAvailable();
                        finish();
                        return;
                    }
                    ConfigureKeyGuardDialog configureKeyGuardDialog = new ConfigureKeyGuardDialog();
                    return;
                default:
                    return;
            }
        }
    }

    private void ensureKeyGuard() {
        if (!checkKeyGuardQuality()) {
            ConfigureKeyGuardDialog configureKeyGuardDialog = new ConfigureKeyGuardDialog();
        } else if (!confirmKeyGuard()) {
            finish();
        }
    }

    private boolean checkKeyGuardQuality() {
        UserInfo parent = UserManager.get(this).getProfileParent(UserHandle.myUserId());
        return new LockPatternUtils(this).getActivePasswordQuality(parent != null ? parent.id : UserHandle.myUserId()) >= 65536;
    }

    private boolean isHardwareBackedKey(byte[] keyData) {
        try {
            return KeyChain.isBoundKeyAlgorithm(AlgNameMapper.map2AlgName(PrivateKeyInfo.getInstance(new ASN1InputStream(new ByteArrayInputStream(keyData)).readObject()).getAlgorithmId().getAlgorithm().getId()));
        } catch (IOException e) {
            Log.e("CredentialStorage", "Failed to parse key data");
            return false;
        }
    }

    private void installIfAvailable() {
        if (this.mInstallBundle != null && !this.mInstallBundle.isEmpty()) {
            Bundle bundle = this.mInstallBundle;
            this.mInstallBundle = null;
            int uid = bundle.getInt("install_as_uid", -1);
            if (uid == -1 || UserHandle.isSameUser(uid, Process.myUid())) {
                int flags;
                String caListName;
                byte[] caListData;
                if (bundle.containsKey("user_private_key_name")) {
                    String key = bundle.getString("user_private_key_name");
                    byte[] value = bundle.getByteArray("user_private_key_data");
                    flags = 1;
                    if (uid == 1010 && isHardwareBackedKey(value)) {
                        Log.d("CredentialStorage", "Saving private key with FLAG_NONE for WIFI_UID");
                        flags = 0;
                    }
                    if (!this.mKeyStore.importKey(key, value, uid, flags)) {
                        Log.e("CredentialStorage", "Failed to install " + key + " as uid " + uid);
                        return;
                    }
                }
                flags = uid == 1010 ? 0 : 1;
                if (bundle.containsKey("user_certificate_name")) {
                    String certName = bundle.getString("user_certificate_name");
                    if (!this.mKeyStore.put(certName, bundle.getByteArray("user_certificate_data"), uid, flags)) {
                        Log.e("CredentialStorage", "Failed to install " + certName + " as uid " + uid);
                        return;
                    }
                }
                if (bundle.containsKey("ca_certificates_name")) {
                    caListName = bundle.getString("ca_certificates_name");
                    if (!this.mKeyStore.put(caListName, bundle.getByteArray("ca_certificates_data"), uid, flags)) {
                        Log.e("CredentialStorage", "Failed to install " + caListName + " as uid " + uid);
                        return;
                    }
                }
                if (bundle.containsKey("wapi_user_certificate_name")) {
                    caListName = bundle.getString("wapi_user_certificate_name");
                    caListData = bundle.getByteArray("wapi_user_certificate_data");
                    if (!(caListName == null || this.mKeyStore.put(caListName, caListData, uid, 1))) {
                        Log.d("CredentialStorage", "Failed to install " + caListName + " as user " + uid);
                        return;
                    }
                }
                if (bundle.containsKey("wapi_server_certificate_name")) {
                    caListName = bundle.getString("wapi_server_certificate_name");
                    caListData = bundle.getByteArray("wapi_server_certificate_data");
                    if (!(caListName == null || this.mKeyStore.put(caListName, caListData, uid, 1))) {
                        Log.d("CredentialStorage", "Failed to install " + caListName + " as user " + uid);
                        return;
                    }
                }
                setResult(-1);
                return;
            }
            int dstUserId = UserHandle.getUserId(uid);
            int myUserId = UserHandle.myUserId();
            if (uid != 1010) {
                Log.e("CredentialStorage", "Failed to install credentials as uid " + uid + ": cross-user installs" + " may only target wifi uids");
            } else {
                startActivityAsUser(new Intent("com.android.credentials.INSTALL").setFlags(33554432).putExtras(bundle), new UserHandle(dstUserId));
            }
        }
    }

    private boolean checkCallerIsCertInstallerOrSelfInProfile() {
        boolean z = true;
        if (TextUtils.equals("com.android.certinstaller", getCallingPackage())) {
            if (getPackageManager().checkSignatures(getCallingPackage(), getPackageName()) != 0) {
                z = false;
            }
            return z;
        }
        try {
            int launchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
            if (launchedFromUid == -1) {
                Log.e("CredentialStorage", "com.android.credentials.INSTALL must be started with startActivityForResult");
                return false;
            } else if (!UserHandle.isSameApp(launchedFromUid, Process.myUid())) {
                return false;
            } else {
                UserInfo parentInfo = ((UserManager) getSystemService("user")).getProfileParent(UserHandle.getUserId(launchedFromUid));
                return parentInfo != null && parentInfo.id == UserHandle.myUserId();
            }
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean confirmKeyGuard() {
        return new ChooseLockSettingsHelper(this).launchConfirmationActivity(1, getResources().getText(R.string.credentials_title), true);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == -1) {
                String password = data.getStringExtra("password");
                if (!TextUtils.isEmpty(password)) {
                    this.mKeyStore.unlock(password);
                    return;
                }
            }
            finish();
        }
    }
}
