package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.PreferenceActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;

public class ConfirmNckCode extends PreferenceActivity {

    public static class ConfirmNckCodeFragment extends Fragment implements OnClickListener, OnEditorActionListener, TextWatcher {
        private static Context mContext;
        private Button mContinueButton;
        private CountDownTimer mCountdownTimer;
        private Handler mHandler = new Handler();
        private TextView mHeaderText;
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private PasswordEntryKeyboardView mKeyboardView;
        private LockPatternUtils mLockPatternUtils;
        private int mNumWrongConfirmAttempts;
        private TextView mPasswordEntry;
        private TextView mRemainedAttempts;
        private final Runnable mResetErrorRunnable = new C00771();

        class C00771 implements Runnable {
            C00771() {
            }

            public void run() {
                ConfirmNckCodeFragment.this.mHeaderText.setText(ConfirmNckCodeFragment.this.getDefaultHeader());
            }
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            mContext = getActivity();
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_nck_code, null);
            view.findViewById(R.id.cancel_button).setOnClickListener(this);
            this.mContinueButton = (Button) view.findViewById(R.id.next_button);
            this.mContinueButton.setOnClickListener(this);
            this.mContinueButton.setEnabled(false);
            this.mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.addTextChangedListener(this);
            this.mKeyboardView = (PasswordEntryKeyboardView) view.findViewById(R.id.keyboard);
            this.mHeaderText = (TextView) view.findViewById(R.id.headerText);
            this.mHeaderText.setText(getDefaultHeader());
            this.mRemainedAttempts = (TextView) view.findViewById(R.id.remained_attempts);
            this.mNumWrongConfirmAttempts = SystemProperties.getInt("persist.sys.nck_attempts", 5);
            this.mRemainedAttempts.setText(getResources().getString(R.string.remained_attempts, new Object[]{this.mNumWrongConfirmAttempts + ""}));
            Activity activity = getActivity();
            this.mKeyboardHelper = new PasswordEntryKeyboardHelper(activity, this.mKeyboardView, this.mPasswordEntry);
            this.mKeyboardHelper.setKeyboardMode(1);
            this.mKeyboardView.requestFocus();
            this.mPasswordEntry.setInputType(18);
            if (activity instanceof PreferenceActivity) {
                PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
                CharSequence title = getText(getDefaultHeader());
                preferenceActivity.showBreadCrumbs(title, title);
            }
            return view;
        }

        private int getDefaultHeader() {
            return R.string.nck_code_confirm_header;
        }

        public void onPause() {
            super.onPause();
            this.mKeyboardView.requestFocus();
            if (this.mCountdownTimer != null) {
                this.mCountdownTimer.cancel();
                this.mCountdownTimer = null;
            }
        }

        public void onResume() {
            super.onResume();
            this.mKeyboardView.requestFocus();
        }

        private void handleNext() {
            if (checkNckCode(this.mPasswordEntry.getText().toString())) {
                SystemProperties.set("persist.sys.nck_attempts", "5");
                getActivity().sendBroadcast(new Intent("com.mediatek.simmelock.simlock"));
                getActivity().finish();
                return;
            }
            this.mNumWrongConfirmAttempts--;
            if (this.mNumWrongConfirmAttempts < 0) {
                this.mNumWrongConfirmAttempts = 0;
            }
            SystemProperties.set("persist.sys.nck_attempts", this.mNumWrongConfirmAttempts + "");
            if (this.mNumWrongConfirmAttempts <= 0) {
                this.mRemainedAttempts.setText(getResources().getString(R.string.remained_attempts, new Object[]{this.mNumWrongConfirmAttempts + ""}));
                this.mContinueButton.setEnabled(false);
                return;
            }
            this.mRemainedAttempts.setText(getResources().getString(R.string.remained_attempts, new Object[]{this.mNumWrongConfirmAttempts + ""}));
            showError(R.string.lockpattern_need_to_unlock_wrong);
        }

        private boolean checkNckCode(String input) {
            if (input.equals(getNckCode())) {
                return true;
            }
            return false;
        }

        private static String getNckCode() {
            String imei = ((TelephonyManager) mContext.getSystemService("phone")).getDeviceId(0);
            if (imei == null) {
                return imei;
            }
            Log.d("tanglei", "imei = " + imei);
            String nckcode = "";
            for (int i = 0; i < imei.length() - 1; i = (i + 1) + 1) {
                nckcode = nckcode + ((Integer.parseInt(imei.substring(i, i + 1)) + Integer.parseInt(imei.substring(i, i + 2))) % 10);
            }
            nckcode = nckcode + imei.substring(0, 1);
            Log.d("tanglei", "nckcode = " + nckcode);
            return nckcode;
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cancel_button:
                    getActivity().finish();
                    return;
                case R.id.next_button:
                    handleNext();
                    return;
                default:
                    return;
            }
        }

        private void showError(int msg) {
            showError(msg, 3000);
        }

        private void showError(int msg, long timeout) {
            this.mHeaderText.setText(msg);
            this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            this.mPasswordEntry.setText(null);
            this.mHandler.removeCallbacks(this.mResetErrorRunnable);
            if (timeout != 0) {
                this.mHandler.postDelayed(this.mResetErrorRunnable, timeout);
            }
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId != 0 && actionId != 6 && actionId != 5) {
                return false;
            }
            handleNext();
            return true;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            boolean z = false;
            if (this.mNumWrongConfirmAttempts > 0) {
                Button button = this.mContinueButton;
                if (this.mPasswordEntry.getText().length() > 0) {
                    z = true;
                }
                button.setEnabled(z);
            }
        }
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":android:show_fragment", ConfirmNckCodeFragment.class.getName());
        modIntent.putExtra(":android:no_headers", true);
        return modIntent;
    }

    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmNckCodeFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.nck_code_confirm_header);
        showBreadCrumbs(msg, msg);
    }
}
