package com.android.settings.applock;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings.System;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;

public class AppLockPasswordActivity extends Activity {
    private static /* synthetic */ int[] f9xad57c04d;
    private DataBaseDao dataBaseDao;
    private int mAlreadyInputLength = 0;
    private AuthenticationCallback mAuthenticationCallback = new C02861();
    private int mErrorInputTimes = 0;
    private FingerprintManager mFinger;
    private CancellationSignal mFingerprintCancelSignal;
    private int mFingerprintRunningState = 0;
    private ImageView[] mImageviews = new ImageView[4];
    private TextView mInputErrerInfoView = null;
    private InputState mInputState = InputState.INPUT_NO_INPUT;
    private boolean mIsUnlockApp = false;
    private int mLockAppID = -1;
    private int[] mTempPassword = new int[4];
    private String packageName;

    class C02861 extends AuthenticationCallback {
        C02861() {
        }

        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
        }

        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
        }

        public void onAuthenticationSucceeded(AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            AppLockPasswordActivity.this.dataBaseDao.update(AppLockPasswordActivity.this.packageName);
            AppLockPasswordActivity.this.finish();
        }

        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
        }
    }

    public enum InputState {
        INPUT_NO_INPUT,
        INPUT_NORMAL_INPUT,
        INPUT_OLD_PASSWORD,
        INPUT_NEW_PASSWORD_PRE,
        INPUT_NEW_PASSWORD_AG
    }

    private class OnKeyClickListener implements OnClickListener {
        private int mKey = 0;

        public OnKeyClickListener(int key) {
            this.mKey = key;
        }

        public void onClick(View v) {
            AppLockPasswordActivity appLockPasswordActivity;
            if (AppLockPasswordActivity.this.mAlreadyInputLength >= 4 || this.mKey >= 10) {
                if (this.mKey == 10 && AppLockPasswordActivity.this.mAlreadyInputLength != 0) {
                    appLockPasswordActivity = AppLockPasswordActivity.this;
                    appLockPasswordActivity.mAlreadyInputLength = appLockPasswordActivity.mAlreadyInputLength - 1;
                    AppLockPasswordActivity.this.updateScreen();
                }
            } else if (AppLockPasswordActivity.this.mTempPassword != null) {
                AppLockPasswordActivity.this.mTempPassword[AppLockPasswordActivity.this.mAlreadyInputLength] = this.mKey;
                appLockPasswordActivity = AppLockPasswordActivity.this;
                appLockPasswordActivity.mAlreadyInputLength = appLockPasswordActivity.mAlreadyInputLength + 1;
                AppLockPasswordActivity.this.updateScreen();
                if (AppLockPasswordActivity.this.mAlreadyInputLength == 4) {
                    AppLockPasswordActivity.this.mAlreadyInputLength = 0;
                    if (AppLockPasswordActivity.this.checkPassword(AppLockPasswordActivity.this.mTempPassword)) {
                        AppLockPasswordActivity.this.dataBaseDao.update(AppLockPasswordActivity.this.packageName);
                        AppLockPasswordActivity.this.finish();
                        return;
                    }
                    AppLockPasswordActivity.this.showPasswordError();
                }
            }
        }
    }

    private static /* synthetic */ int[] m5x3218c1f1() {
        if (f9xad57c04d != null) {
            return f9xad57c04d;
        }
        int[] iArr = new int[InputState.values().length];
        try {
            iArr[InputState.INPUT_NEW_PASSWORD_AG.ordinal()] = 3;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[InputState.INPUT_NEW_PASSWORD_PRE.ordinal()] = 4;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[InputState.INPUT_NORMAL_INPUT.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[InputState.INPUT_NO_INPUT.ordinal()] = 5;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[InputState.INPUT_OLD_PASSWORD.ordinal()] = 2;
        } catch (NoSuchFieldError e5) {
        }
        f9xad57c04d = iArr;
        return iArr;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applock_activity_password);
        this.mLockAppID = getIntent().getIntExtra("app_lock_id", -1);
        this.mIsUnlockApp = getIntent().getBooleanExtra("app_lock_unlock", false);
        this.packageName = getIntent().getStringExtra("packageName");
        this.dataBaseDao = new AppListDaoImpl(this);
        loadView();
        ((Button) findViewById(R.id.key_clear)).setText("<-");
        ((Button) findViewById(R.id.key_clear)).setTextSize(30.0f);
        this.mInputState = InputState.INPUT_NORMAL_INPUT;
        this.mFinger = (FingerprintManager) getSystemService("fingerprint");
        updateScreen();
    }

    private void startListeningForFingerprint() {
        if (this.mFingerprintRunningState == 2) {
            setFingerprintRunningState(3);
            return;
        }
        if (isHasFingerprintEnroll()) {
            if (this.mFingerprintCancelSignal != null) {
                this.mFingerprintCancelSignal.cancel();
            }
            this.mFingerprintCancelSignal = new CancellationSignal();
            this.mFinger.authenticate(null, this.mFingerprintCancelSignal, 0, this.mAuthenticationCallback, null);
            setFingerprintRunningState(1);
        }
    }

    public boolean isHasFingerprintEnroll() {
        if (this.mFinger == null || !this.mFinger.isHardwareDetected()) {
            return false;
        }
        return this.mFinger.hasEnrolledFingerprints();
    }

    private void stopListeningForFingerprint() {
        if (this.mFingerprintRunningState == 1) {
            this.mFingerprintCancelSignal.cancel();
            this.mFingerprintCancelSignal = null;
            setFingerprintRunningState(2);
        }
        if (this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(2);
        }
    }

    private void setFingerprintRunningState(int fingerprintRunningState) {
        boolean wasRunning = this.mFingerprintRunningState == 1;
        boolean isRunning = fingerprintRunningState == 1;
        this.mFingerprintRunningState = fingerprintRunningState;
        if (wasRunning != isRunning) {
            notifyFingerprintRunningStateChanged();
        }
    }

    private void notifyFingerprintRunningStateChanged() {
    }

    protected void onDestroy() {
        stopListeningForFingerprint();
        super.onDestroy();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void loadView() {
        this.mImageviews[0] = (ImageView) findViewById(R.id.password_one);
        this.mImageviews[1] = (ImageView) findViewById(R.id.password_two);
        this.mImageviews[2] = (ImageView) findViewById(R.id.password_three);
        this.mImageviews[3] = (ImageView) findViewById(R.id.password_four);
        setButtonListener(R.id.key_one, new OnKeyClickListener(1));
        setButtonListener(R.id.key_two, new OnKeyClickListener(2));
        setButtonListener(R.id.key_three, new OnKeyClickListener(3));
        setButtonListener(R.id.key_four, new OnKeyClickListener(4));
        setButtonListener(R.id.key_five, new OnKeyClickListener(5));
        setButtonListener(R.id.key_six, new OnKeyClickListener(6));
        setButtonListener(R.id.key_seven, new OnKeyClickListener(7));
        setButtonListener(R.id.key_eight, new OnKeyClickListener(8));
        setButtonListener(R.id.key_nine, new OnKeyClickListener(9));
        setButtonListener(R.id.key_zero, new OnKeyClickListener(0));
        setButtonListener(R.id.key_clear, new OnKeyClickListener(10));
        this.mInputErrerInfoView = (TextView) findViewById(R.id.text_input_wrong);
    }

    private void setButtonListener(int id, OnKeyClickListener listener) {
        Button btn = (Button) findViewById(id);
        if (btn != null && listener != null) {
            btn.setOnClickListener(listener);
        }
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        startListeningForFingerprint();
        super.onResume();
    }

    private void updateScreen() {
        for (int i = 0; i < 4; i++) {
            if (i < this.mAlreadyInputLength) {
                this.mImageviews[i].setImageResource(R.drawable.ic_already_input);
            } else {
                this.mImageviews[i].setImageResource(R.drawable.ic_password_input);
            }
        }
        switch (m5x3218c1f1()[this.mInputState.ordinal()]) {
            case 1:
            case 2:
                if (this.mErrorInputTimes == 0) {
                    this.mInputErrerInfoView.setVisibility(4);
                    return;
                }
                this.mInputErrerInfoView.setVisibility(0);
                this.mInputErrerInfoView.setText(this.mErrorInputTimes + getResources().getString(R.string.wrong_password));
                return;
            default:
                return;
        }
    }

    private boolean checkPassword(int[] password) {
        if (length <= 0) {
            return false;
        }
        String pwd = "";
        for (int i : password) {
            pwd = pwd + i;
        }
        String oldPWD = System.getString(getContentResolver(), "vanzo_lock_password");
        if (oldPWD == null) {
            oldPWD = "";
        }
        return oldPWD.equals(pwd);
    }

    private void showPasswordError() {
        Toast toast = Toast.makeText(this, getResources().getString(R.string.wrong_password_apploack), 0);
        toast.setGravity(49, 0, 410);
        toast.show();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            Intent intent = new Intent();
            intent.addCategory("android.intent.category.HOME");
            intent.setAction("android.intent.action.MAIN");
            intent.addFlags(270532608);
            startActivity(intent);
            finish();
            return false;
        }
        if (keyCode == 3) {
            finish();
        }
        return super.onKeyUp(keyCode, event);
    }
}
