package com.android.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnCheckCallback;
import com.android.internal.widget.LockPatternChecker.OnVerifyCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import java.util.ArrayList;

public class ConfirmLockPassword extends ConfirmDeviceCredentialBaseActivity {

    public static class ConfirmLockPasswordFragment extends ConfirmDeviceCredentialBaseFragment implements OnClickListener, OnEditorActionListener {
        private AppearAnimationUtils mAppearAnimationUtils;
        private boolean mBlockImm;
        private CountDownTimer mCountdownTimer;
        private TextView mDetailsTextView;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private int mEffectiveUserId;
        private TextView mErrorTextView;
        private Handler mHandler = new Handler();
        private TextView mHeaderTextView;
        private InputMethodManager mImm;
        private boolean mIsAlpha;
        private LockPatternUtils mLockPatternUtils;
        private int mNumWrongConfirmAttempts;
        private TextView mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private final Runnable mResetErrorRunnable = new C00631();
        private boolean mUsingFingerprint = false;

        class C00631 implements Runnable {
            C00631() {
            }

            public void run() {
                ConfirmLockPasswordFragment.this.mErrorTextView.setText("");
            }
        }

        class C00642 implements Runnable {
            C00642() {
            }

            public void run() {
                ConfirmLockPasswordFragment.this.mBlockImm = false;
                ConfirmLockPasswordFragment.this.resetState();
            }
        }

        class C00653 implements Runnable {
            C00653() {
            }

            public void run() {
                if (ConfirmLockPasswordFragment.this.shouldAutoShowSoftKeyboard()) {
                    ConfirmLockPasswordFragment.this.resetState();
                } else {
                    ConfirmLockPasswordFragment.this.mImm.hideSoftInputFromWindow(ConfirmLockPasswordFragment.this.mPasswordEntry.getWindowToken(), 1);
                }
            }
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            this.mEffectiveUserId = Utils.getEffectiveUserId(getActivity());
            if (savedInstanceState != null) {
                this.mNumWrongConfirmAttempts = savedInstanceState.getInt("confirm_lock_password_fragment.key_num_wrong_confirm_attempts", 0);
            }
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int storedQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mEffectiveUserId);
            View view = inflater.inflate(R.layout.confirm_lock_password, null);
            this.mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntryInputDisabler = new TextViewInputDisabler(this.mPasswordEntry);
            this.mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            this.mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            this.mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            boolean z = (262144 == storedQuality || 327680 == storedQuality) ? true : 393216 == storedQuality;
            this.mIsAlpha = z;
            this.mImm = (InputMethodManager) getActivity().getSystemService("input_method");
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence headerMessage = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.header");
                CharSequence detailsMessage = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.details");
                if (TextUtils.isEmpty(headerMessage)) {
                    headerMessage = getString(getDefaultHeader());
                }
                if (TextUtils.isEmpty(detailsMessage)) {
                    detailsMessage = getString(getDefaultDetails());
                }
                this.mHeaderTextView.setText(headerMessage);
                this.mDetailsTextView.setText(detailsMessage);
            }
            int currentType = this.mPasswordEntry.getInputType();
            TextView textView = this.mPasswordEntry;
            if (!this.mIsAlpha) {
                currentType = 18;
            }
            textView.setInputType(currentType);
            this.mAppearAnimationUtils = new AppearAnimationUtils(getContext(), 220, 2.0f, 1.0f, AnimationUtils.loadInterpolator(getContext(), 17563662));
            this.mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(), 110, 1.0f, 0.5f, AnimationUtils.loadInterpolator(getContext(), 17563663));
            setAccessibilityTitle(this.mHeaderTextView.getText());
            return view;
        }

        private int getDefaultHeader() {
            if (this.mIsAlpha) {
                return R.string.lockpassword_confirm_your_password_header;
            }
            return R.string.lockpassword_confirm_your_pin_header;
        }

        private int getDefaultDetails() {
            if (this.mIsAlpha) {
                return R.string.lockpassword_confirm_your_password_generic;
            }
            return R.string.lockpassword_confirm_your_pin_generic;
        }

        private int getErrorMessage() {
            if (this.mIsAlpha) {
                return R.string.lockpassword_invalid_password;
            }
            return R.string.lockpassword_invalid_pin;
        }

        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            this.mHeaderTextView.setAlpha(0.0f);
            this.mDetailsTextView.setAlpha(0.0f);
            this.mCancelButton.setAlpha(0.0f);
            this.mPasswordEntry.setAlpha(0.0f);
            this.mFingerprintIcon.setAlpha(0.0f);
            this.mBlockImm = true;
        }

        private View[] getActiveViews() {
            ArrayList<View> result = new ArrayList();
            result.add(this.mHeaderTextView);
            result.add(this.mDetailsTextView);
            if (this.mCancelButton.getVisibility() == 0) {
                result.add(this.mCancelButton);
            }
            result.add(this.mPasswordEntry);
            if (this.mFingerprintIcon.getVisibility() == 0) {
                result.add(this.mFingerprintIcon);
            }
            return (View[]) result.toArray(new View[0]);
        }

        public void startEnterAnimation() {
            super.startEnterAnimation();
            this.mAppearAnimationUtils.startAnimation(getActiveViews(), new C00642());
        }

        public void onPause() {
            super.onPause();
            if (this.mCountdownTimer != null) {
                this.mCountdownTimer.cancel();
                this.mCountdownTimer = null;
            }
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
                this.mPendingLockCheck = null;
            }
        }

        protected int getMetricsCategory() {
            return 30;
        }

        public void onResume() {
            super.onResume();
            long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(this.mEffectiveUserId);
            if (deadline != 0) {
                handleAttemptLockout(deadline);
            } else {
                resetState();
            }
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("confirm_lock_password_fragment.key_num_wrong_confirm_attempts", this.mNumWrongConfirmAttempts);
        }

        protected void authenticationSucceeded() {
            startDisappearAnimation(new Intent());
        }

        public void onFingerprintIconVisibilityChanged(boolean visible) {
            this.mUsingFingerprint = visible;
        }

        private void resetState() {
            if (!this.mBlockImm) {
                this.mPasswordEntry.setEnabled(true);
                this.mPasswordEntryInputDisabler.setInputEnabled(true);
                if (shouldAutoShowSoftKeyboard()) {
                    this.mImm.showSoftInput(this.mPasswordEntry, 1);
                }
            }
        }

        private boolean shouldAutoShowSoftKeyboard() {
            return this.mPasswordEntry.isEnabled() && !this.mUsingFingerprint;
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (hasFocus && !this.mBlockImm) {
                this.mPasswordEntry.post(new C00653());
            }
        }

        private void handleNext() {
            if (getActivity() == null) {
                Log.e("ConfirmLockPassword", "error,getActivity() is null");
                return;
            }
            this.mPasswordEntryInputDisabler.setInputEnabled(false);
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
            }
            String pin = this.mPasswordEntry.getText().toString();
            boolean verifyChallenge = getActivity().getIntent().getBooleanExtra("has_challenge", false);
            Intent intent = new Intent();
            if (!verifyChallenge) {
                startCheckPassword(pin, intent);
            } else if (isInternalActivity()) {
                startVerifyPassword(pin, intent);
            } else {
                onPasswordChecked(false, intent, 0, this.mEffectiveUserId);
            }
        }

        private boolean isInternalActivity() {
            return getActivity() instanceof InternalActivity;
        }

        private void startVerifyPassword(String pin, final Intent intent) {
            long challenge = getActivity().getIntent().getLongExtra("challenge", 0);
            final int localEffectiveUserId = this.mEffectiveUserId;
            this.mPendingLockCheck = LockPatternChecker.verifyPassword(this.mLockPatternUtils, pin, challenge, localEffectiveUserId, new OnVerifyCallback() {
                public void onVerified(byte[] token, int timeoutMs) {
                    ConfirmLockPasswordFragment.this.mPendingLockCheck = null;
                    boolean matched = false;
                    if (token != null) {
                        matched = true;
                        intent.putExtra("hw_auth_token", token);
                    }
                    ConfirmLockPasswordFragment.this.onPasswordChecked(matched, intent, timeoutMs, localEffectiveUserId);
                }
            });
        }

        private void startCheckPassword(final String pin, final Intent intent) {
            final int localEffectiveUserId = this.mEffectiveUserId;
            this.mPendingLockCheck = LockPatternChecker.checkPassword(this.mLockPatternUtils, pin, localEffectiveUserId, new OnCheckCallback() {
                public void onChecked(boolean matched, int timeoutMs) {
                    ConfirmLockPasswordFragment.this.mPendingLockCheck = null;
                    if (matched && ConfirmLockPasswordFragment.this.isInternalActivity()) {
                        int i;
                        Intent intent = intent;
                        String str = "type";
                        if (ConfirmLockPasswordFragment.this.mIsAlpha) {
                            i = 0;
                        } else {
                            i = 3;
                        }
                        intent.putExtra(str, i);
                        intent.putExtra("password", pin);
                    }
                    ConfirmLockPasswordFragment.this.onPasswordChecked(matched, intent, timeoutMs, localEffectiveUserId);
                }
            });
        }

        private void startDisappearAnimation(final Intent intent) {
            if (getActivity() == null) {
                Log.e("ConfirmLockPassword", "error,getActivity() is null");
                return;
            }
            if (getActivity().getThemeResId() == R.style.Theme.ConfirmDeviceCredentialsDark) {
                this.mDisappearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                    public void run() {
                        ConfirmLockPasswordFragment.this.getActivity().setResult(-1, intent);
                        ConfirmLockPasswordFragment.this.getActivity().finish();
                        ConfirmLockPasswordFragment.this.getActivity().overridePendingTransition(R.anim.confirm_credential_close_enter, R.anim.confirm_credential_close_exit);
                    }
                });
            } else {
                getActivity().setResult(-1, intent);
                getActivity().finish();
            }
        }

        private void onPasswordChecked(boolean matched, Intent intent, int timeoutMs, int effectiveUserId) {
            this.mPasswordEntryInputDisabler.setInputEnabled(true);
            if (matched) {
                startDisappearAnimation(intent);
            } else if (timeoutMs > 0) {
                handleAttemptLockout(this.mLockPatternUtils.setLockoutAttemptDeadline(effectiveUserId, timeoutMs));
            } else {
                showError(getErrorMessage());
            }
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mPasswordEntry.setEnabled(false);
            this.mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {
                public void onTick(long millisUntilFinished) {
                    if (ConfirmLockPasswordFragment.this.isAdded()) {
                        int secondsCountdown = (int) (millisUntilFinished / 1000);
                        ConfirmLockPasswordFragment.this.showError(ConfirmLockPasswordFragment.this.getString(R.string.lockpattern_too_many_failed_confirmation_attempts, new Object[]{Integer.valueOf(secondsCountdown)}), 0);
                    }
                }

                public void onFinish() {
                    ConfirmLockPasswordFragment.this.resetState();
                    ConfirmLockPasswordFragment.this.mErrorTextView.setText("");
                    ConfirmLockPasswordFragment.this.mNumWrongConfirmAttempts = 0;
                }
            }.start();
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cancel_button:
                    getActivity().setResult(0);
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

        private void showError(CharSequence msg, long timeout) {
            this.mErrorTextView.setText(msg);
            this.mPasswordEntry.setText(null);
            this.mHandler.removeCallbacks(this.mResetErrorRunnable);
            if (timeout != 0) {
                this.mHandler.postDelayed(this.mResetErrorRunnable, timeout);
            }
        }

        private void showError(int msg, long timeout) {
            showError(getText(msg), timeout);
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId != 0 && actionId != 6 && actionId != 5) {
                return false;
            }
            handleNext();
            return true;
        }
    }

    public static class InternalActivity extends ConfirmLockPassword {
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", ConfirmLockPasswordFragment.class.getName());
        return modIntent;
    }

    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPasswordFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragment != null && (fragment instanceof ConfirmLockPasswordFragment)) {
            ((ConfirmLockPasswordFragment) fragment).onWindowFocusChanged(hasFocus);
        }
    }
}
