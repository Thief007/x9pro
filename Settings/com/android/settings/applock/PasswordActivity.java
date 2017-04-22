package com.android.settings.applock;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings.System;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

public class PasswordActivity extends Activity {
    private static /* synthetic */ int[] f10x5d1b9cf1;
    private String START_TYPE;
    private boolean bEqualOldPassword = false;
    private boolean bEqualPrePassword = true;
    private int mAlreadyInputLength = 0;
    private int mErrorInputTimes = 0;
    private ImageView[] mImageviews = new ImageView[4];
    private TextView mInputErrerInfoView = null;
    private TextView mInputInfoView = null;
    private InputState mInputState = InputState.INPUT_NO_INPUT;
    private int[] mNewPassword = new int[4];
    private int[] mOldPassword = new int[4];
    private int[] mTempPassword = new int[4];

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
            PasswordActivity passwordActivity;
            int i;
            if (PasswordActivity.this.mAlreadyInputLength >= 4 || this.mKey >= 10) {
                if (this.mKey == 10 && PasswordActivity.this.mAlreadyInputLength != 0) {
                    passwordActivity = PasswordActivity.this;
                    passwordActivity.mAlreadyInputLength = passwordActivity.mAlreadyInputLength - 1;
                    for (i = 0; i < 4; i++) {
                        if (i < PasswordActivity.this.mAlreadyInputLength) {
                            PasswordActivity.this.mImageviews[i].setImageResource(R.drawable.ic_already_input);
                        } else {
                            PasswordActivity.this.mImageviews[i].setImageResource(R.drawable.ic_password_input);
                        }
                    }
                    PasswordActivity.this.updateScreen();
                }
            } else if (PasswordActivity.this.mTempPassword != null) {
                PasswordActivity.this.mTempPassword[PasswordActivity.this.mAlreadyInputLength] = this.mKey;
                passwordActivity = PasswordActivity.this;
                passwordActivity.mAlreadyInputLength = passwordActivity.mAlreadyInputLength + 1;
                for (i = 0; i < 4; i++) {
                    if (i < PasswordActivity.this.mAlreadyInputLength) {
                        PasswordActivity.this.mImageviews[i].setImageResource(R.drawable.ic_already_input);
                    } else {
                        PasswordActivity.this.mImageviews[i].setImageResource(R.drawable.ic_password_input);
                    }
                }
                if (PasswordActivity.this.mAlreadyInputLength == 4) {
                    PasswordActivity.this.mAlreadyInputLength = 0;
                    PasswordActivity.this.capturePassword();
                }
                PasswordActivity.this.updateScreen();
            }
        }
    }

    private static /* synthetic */ int[] m6x81596bcd() {
        if (f10x5d1b9cf1 != null) {
            return f10x5d1b9cf1;
        }
        int[] iArr = new int[InputState.values().length];
        try {
            iArr[InputState.INPUT_NEW_PASSWORD_AG.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[InputState.INPUT_NEW_PASSWORD_PRE.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[InputState.INPUT_NORMAL_INPUT.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[InputState.INPUT_NO_INPUT.ordinal()] = 5;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[InputState.INPUT_OLD_PASSWORD.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        f10x5d1b9cf1 = iArr;
        return iArr;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        loadView();
        ((Button) findViewById(R.id.key_clear)).setText("<-");
        ((Button) findViewById(R.id.key_clear)).setTextSize(30.0f);
        initActivity();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if ("change.password".equals(this.START_TYPE)) {
            actionBar.setTitle(getResources().getString(R.string.password_change_password));
        }
        updateScreen();
        for (int i = 0; i < 4; i++) {
            if (i < this.mAlreadyInputLength) {
                this.mImageviews[i].setImageResource(R.drawable.ic_already_input);
            } else {
                this.mImageviews[i].setImageResource(R.drawable.ic_password_input);
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private void initActivity() {
        this.START_TYPE = getIntent().getStringExtra("start_type");
        boolean isFirstStart = Integer.valueOf(Preferences.getPassword(this)).intValue() > 12344;
        if (this.START_TYPE == null) {
            if (isFirstStart) {
                this.mInputState = InputState.INPUT_NEW_PASSWORD_PRE;
                this.bEqualOldPassword = false;
                this.bEqualPrePassword = true;
                this.mErrorInputTimes = 0;
                this.START_TYPE = "first_start";
                return;
            }
            this.START_TYPE = "check.password";
            this.mInputState = InputState.INPUT_NORMAL_INPUT;
            this.bEqualOldPassword = false;
            this.bEqualPrePassword = true;
            this.mErrorInputTimes = 0;
        } else if ("change.password".equals(this.START_TYPE)) {
            this.mInputState = InputState.INPUT_OLD_PASSWORD;
        } else if ("check.password.result".equals(this.START_TYPE)) {
            this.mInputState = InputState.INPUT_NORMAL_INPUT;
            this.bEqualOldPassword = false;
            this.bEqualPrePassword = true;
            this.mErrorInputTimes = 0;
        } else {
            finish();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 16908332:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
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
        this.mInputInfoView = (TextView) findViewById(R.id.text_input_info);
        this.mInputErrerInfoView = (TextView) findViewById(R.id.text_input_wrong);
    }

    private void setButtonListener(int id, OnKeyClickListener listener) {
        Button btn = (Button) findViewById(id);
        if (btn != null && listener != null) {
            btn.setOnClickListener(listener);
        }
    }

    private void updateScreen() {
        switch (m6x81596bcd()[this.mInputState.ordinal()]) {
            case 1:
                this.mInputInfoView.setText(R.string.input_new_password_again);
                break;
            case 2:
                this.mInputInfoView.setText(R.string.input_new_password);
                break;
            case 3:
                this.mInputInfoView.setText(R.string.input_password);
                break;
            case 4:
                this.mInputInfoView.setText(R.string.input_old_password);
                break;
        }
        switch (m6x81596bcd()[this.mInputState.ordinal()]) {
            case 1:
                this.mInputErrerInfoView.setVisibility(4);
                break;
            case 2:
                if (this.bEqualOldPassword) {
                    this.mInputErrerInfoView.setVisibility(0);
                    this.mInputErrerInfoView.setText(R.string.input_other_password);
                    return;
                } else if (this.bEqualPrePassword) {
                    this.mInputErrerInfoView.setVisibility(4);
                    return;
                } else {
                    this.mInputErrerInfoView.setVisibility(0);
                    this.mInputErrerInfoView.setText(R.string.password_not_equal_pre);
                    return;
                }
            case 3:
            case 4:
                break;
            default:
                return;
        }
        if (this.mErrorInputTimes == 0) {
            this.mInputErrerInfoView.setVisibility(4);
            return;
        }
        this.mInputErrerInfoView.setVisibility(0);
        this.mInputErrerInfoView.setText(this.mErrorInputTimes + getResources().getString(R.string.wrong_password));
    }

    private boolean checkPassword(int[] password) {
        if (length <= 0) {
            return false;
        }
        String pwd = "";
        for (int i : password) {
            pwd = pwd + i;
        }
        return Preferences.getPassword(this).equals(pwd);
    }

    private boolean checkPasswordEqual(int[] pre, int[] ag) {
        for (int i = 0; i < 4; i++) {
            if (pre[i] != ag[i]) {
                return false;
            }
        }
        return true;
    }

    private void passwordAssign(int[] tag, int[] src) {
        for (int i = 0; i < 4; i++) {
            tag[i] = src[i];
        }
    }

    private void savePasswod(int[] password) throws RuntimeException {
        try {
            String old = "1234";
            String PWD = "" + password[0] + password[1] + password[2] + password[3];
            Preferences.setPassword(this, PWD);
            System.putString(getContentResolver(), "vanzo_lock_password", PWD);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private void capturePassword() {
        switch (m6x81596bcd()[this.mInputState.ordinal()]) {
            case 1:
                if (!checkPasswordEqual(this.mNewPassword, this.mTempPassword)) {
                    this.bEqualPrePassword = false;
                    this.bEqualOldPassword = false;
                    this.mInputState = InputState.INPUT_NEW_PASSWORD_PRE;
                    break;
                }
                this.bEqualOldPassword = false;
                this.bEqualPrePassword = true;
                try {
                    savePasswod(this.mTempPassword);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                this.mAlreadyInputLength = 4;
                if (this.START_TYPE == null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("check.return", true);
                    setResult(-1, resultIntent);
                    finish();
                } else if (this.START_TYPE == "first_start") {
                    this.mErrorInputTimes = 0;
                    startActivity(new Intent(this, ApplyAppLockActivity.class));
                }
                finish();
                break;
            case 2:
                if (!checkPasswordEqual(this.mOldPassword, this.mTempPassword)) {
                    this.bEqualOldPassword = false;
                    passwordAssign(this.mNewPassword, this.mTempPassword);
                    this.mInputState = InputState.INPUT_NEW_PASSWORD_AG;
                    this.bEqualPrePassword = true;
                    break;
                }
                this.bEqualOldPassword = true;
                this.bEqualPrePassword = true;
                this.mInputState = InputState.INPUT_NEW_PASSWORD_PRE;
                break;
            case 3:
                if (!checkPassword(this.mTempPassword)) {
                    this.mErrorInputTimes++;
                    if (this.mErrorInputTimes > 5) {
                        finish();
                        break;
                    }
                }
                if ("check.password.result".equals(this.START_TYPE)) {
                    Intent intent = new Intent(this, ApplyAppLockActivity.class);
                    intent.putExtra("check.return", true);
                    setResult(-1, intent);
                    finish();
                } else if ("check.password".equals(this.START_TYPE)) {
                    this.mErrorInputTimes = 0;
                    startActivity(new Intent(this, ApplyAppLockActivity.class));
                    finish();
                } else {
                    finish();
                }
                this.mAlreadyInputLength = 4;
                break;
                break;
            case 4:
                if (!checkPassword(this.mTempPassword)) {
                    this.mErrorInputTimes++;
                    break;
                }
                passwordAssign(this.mOldPassword, this.mTempPassword);
                this.mInputState = InputState.INPUT_NEW_PASSWORD_PRE;
                this.mErrorInputTimes = 0;
                break;
        }
        for (int i = 0; i < 4; i++) {
            if (i < this.mAlreadyInputLength) {
                this.mImageviews[i].setImageResource(R.drawable.ic_already_input);
            } else {
                this.mImageviews[i].setImageResource(R.drawable.ic_password_input);
            }
        }
    }

    protected void onPause() {
        finish();
        super.onPause();
    }
}
