package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.KeyboardView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnVerifyCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.notification.RedactionInterstitial;

public class ChooseLockPassword extends SettingsActivity {

    public static class ChooseLockPasswordFragment extends InstrumentedFragment implements OnClickListener, OnEditorActionListener, TextWatcher {
        private Button mCancelButton;
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private String mCurrentPassword;
        private boolean mDone = false;
        private String mFirstPin;
        private Handler mHandler = new C00571();
        private boolean mHasChallenge;
        private TextView mHeaderText;
        private boolean mIsAlphaMode;
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private KeyboardView mKeyboardView;
        private LockPatternUtils mLockPatternUtils;
        private Button mNextButton;
        private TextView mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private int mPasswordMaxLength = 16;
        private int mPasswordMinLength = 4;
        private int mPasswordMinLetters = 0;
        private int mPasswordMinLowerCase = 0;
        private int mPasswordMinNonLetter = 0;
        private int mPasswordMinNumeric = 0;
        private int mPasswordMinSymbols = 0;
        private int mPasswordMinUpperCase = 0;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private int mRequestedQuality = 131072;
        private Stage mUiStage = Stage.Introduction;

        class C00571 extends Handler {
            C00571() {
            }

            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    ChooseLockPasswordFragment.this.updateStage((Stage) msg.obj);
                }
            }
        }

        protected enum Stage {
            Introduction(R.string.lockpassword_choose_your_password_header, R.string.lockpassword_choose_your_pin_header, R.string.lockpassword_continue_label),
            NeedToConfirm(R.string.lockpassword_confirm_your_password_header, R.string.lockpassword_confirm_your_pin_header, R.string.lockpassword_ok_label),
            ConfirmWrong(R.string.lockpassword_confirm_passwords_dont_match, R.string.lockpassword_confirm_pins_dont_match, R.string.lockpassword_continue_label);
            
            public final int alphaHint;
            public final int buttonText;
            public final int numericHint;

            private Stage(int hintInAlpha, int hintInNumeric, int nextButtonText) {
                this.alphaHint = hintInAlpha;
                this.numericHint = hintInNumeric;
                this.buttonText = nextButtonText;
            }
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            Intent intent = getActivity().getIntent();
            if (getActivity() instanceof ChooseLockPassword) {
                this.mRequestedQuality = Math.max(intent.getIntExtra("lockscreen.password_type", this.mRequestedQuality), this.mLockPatternUtils.getRequestedPasswordQuality(UserHandle.myUserId()));
                this.mPasswordMinLength = Math.max(Math.max(4, intent.getIntExtra("lockscreen.password_min", this.mPasswordMinLength)), this.mLockPatternUtils.getRequestedMinimumPasswordLength(UserHandle.myUserId()));
                this.mPasswordMaxLength = intent.getIntExtra("lockscreen.password_max", this.mPasswordMaxLength);
                this.mPasswordMinLetters = Math.max(intent.getIntExtra("lockscreen.password_min_letters", this.mPasswordMinLetters), this.mLockPatternUtils.getRequestedPasswordMinimumLetters(UserHandle.myUserId()));
                this.mPasswordMinUpperCase = Math.max(intent.getIntExtra("lockscreen.password_min_uppercase", this.mPasswordMinUpperCase), this.mLockPatternUtils.getRequestedPasswordMinimumUpperCase(UserHandle.myUserId()));
                this.mPasswordMinLowerCase = Math.max(intent.getIntExtra("lockscreen.password_min_lowercase", this.mPasswordMinLowerCase), this.mLockPatternUtils.getRequestedPasswordMinimumLowerCase(UserHandle.myUserId()));
                this.mPasswordMinNumeric = Math.max(intent.getIntExtra("lockscreen.password_min_numeric", this.mPasswordMinNumeric), this.mLockPatternUtils.getRequestedPasswordMinimumNumeric(UserHandle.myUserId()));
                this.mPasswordMinSymbols = Math.max(intent.getIntExtra("lockscreen.password_min_symbols", this.mPasswordMinSymbols), this.mLockPatternUtils.getRequestedPasswordMinimumSymbols(UserHandle.myUserId()));
                this.mPasswordMinNonLetter = Math.max(intent.getIntExtra("lockscreen.password_min_nonletter", this.mPasswordMinNonLetter), this.mLockPatternUtils.getRequestedPasswordMinimumNonLetter(UserHandle.myUserId()));
                this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
                return;
            }
            throw new SecurityException("Fragment contained in wrong activity");
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_lock_password, container, false);
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            int i;
            super.onViewCreated(view, savedInstanceState);
            this.mCancelButton = (Button) view.findViewById(R.id.cancel_button);
            this.mCancelButton.setOnClickListener(this);
            this.mNextButton = (Button) view.findViewById(R.id.next_button);
            this.mNextButton.setOnClickListener(this);
            boolean z = (262144 == this.mRequestedQuality || 327680 == this.mRequestedQuality) ? true : 393216 == this.mRequestedQuality;
            this.mIsAlphaMode = z;
            this.mKeyboardView = (PasswordEntryKeyboardView) view.findViewById(R.id.keyboard);
            this.mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.addTextChangedListener(this);
            this.mPasswordEntryInputDisabler = new TextViewInputDisabler(this.mPasswordEntry);
            Activity activity = getActivity();
            this.mKeyboardHelper = new PasswordEntryKeyboardHelper(activity, this.mKeyboardView, this.mPasswordEntry);
            PasswordEntryKeyboardHelper passwordEntryKeyboardHelper = this.mKeyboardHelper;
            if (this.mIsAlphaMode) {
                i = 0;
            } else {
                i = 1;
            }
            passwordEntryKeyboardHelper.setKeyboardMode(i);
            this.mHeaderText = (TextView) view.findViewById(R.id.headerText);
            this.mKeyboardView.requestFocus();
            int currentType = this.mPasswordEntry.getInputType();
            TextView textView = this.mPasswordEntry;
            if (!this.mIsAlphaMode) {
                currentType = 18;
            }
            textView.setInputType(currentType);
            Intent intent = getActivity().getIntent();
            boolean confirmCredentials = intent.getBooleanExtra("confirm_credentials", true);
            this.mCurrentPassword = intent.getStringExtra("password");
            this.mHasChallenge = intent.getBooleanExtra("has_challenge", false);
            this.mChallenge = intent.getLongExtra("challenge", 0);
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
                if (confirmCredentials) {
                    this.mChooseLockSettingsHelper.launchConfirmationActivity(58, getString(R.string.unlock_set_unlock_launch_picker_title), true);
                }
            } else {
                this.mFirstPin = savedInstanceState.getString("first_pin");
                String state = savedInstanceState.getString("ui_stage");
                if (state != null) {
                    this.mUiStage = Stage.valueOf(state);
                    updateStage(this.mUiStage);
                }
                if (this.mCurrentPassword == null) {
                    this.mCurrentPassword = savedInstanceState.getString("current_password");
                }
            }
            this.mDone = false;
            if (activity instanceof SettingsActivity) {
                int id;
                SettingsActivity sa = (SettingsActivity) activity;
                if (this.mIsAlphaMode) {
                    id = R.string.lockpassword_choose_your_password_header;
                } else {
                    id = R.string.lockpassword_choose_your_pin_header;
                }
                sa.setTitle(getText(id));
            }
        }

        protected int getMetricsCategory() {
            return 28;
        }

        public void onResume() {
            super.onResume();
            updateStage(this.mUiStage);
            this.mPasswordEntryInputDisabler.setInputEnabled(true);
            this.mKeyboardView.requestFocus();
        }

        public void onPause() {
            this.mHandler.removeMessages(1);
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
                this.mPendingLockCheck = null;
            }
            super.onPause();
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("ui_stage", this.mUiStage.name());
            outState.putString("first_pin", this.mFirstPin);
            outState.putString("current_password", this.mCurrentPassword);
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case 58:
                    if (resultCode != -1) {
                        getActivity().setResult(1);
                        getActivity().finish();
                        return;
                    }
                    this.mCurrentPassword = data.getStringExtra("password");
                    return;
                default:
                    return;
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context);
        }

        protected void updateStage(Stage stage) {
            Stage previousStage = this.mUiStage;
            this.mUiStage = stage;
            updateUi();
            if (previousStage != stage) {
                this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            }
        }

        private String validatePassword(String password) {
            int i;
            if (password.length() < this.mPasswordMinLength) {
                if (this.mIsAlphaMode) {
                    i = R.string.lockpassword_password_too_short;
                } else {
                    i = R.string.lockpassword_pin_too_short;
                }
                return getString(i, new Object[]{Integer.valueOf(this.mPasswordMinLength)});
            } else if (password.length() > this.mPasswordMaxLength) {
                if (this.mIsAlphaMode) {
                    i = R.string.lockpassword_password_too_long;
                } else {
                    i = R.string.lockpassword_pin_too_long;
                }
                return getString(i, new Object[]{Integer.valueOf(this.mPasswordMaxLength + 1)});
            } else {
                int letters = 0;
                int numbers = 0;
                int lowercase = 0;
                int symbols = 0;
                int uppercase = 0;
                int nonletter = 0;
                for (int i2 = 0; i2 < password.length(); i2++) {
                    char c = password.charAt(i2);
                    if (c < ' ' || c > '') {
                        return getString(R.string.lockpassword_illegal_character);
                    }
                    if (c >= '0' && c <= '9') {
                        numbers++;
                        nonletter++;
                    } else if (c >= 'A' && c <= 'Z') {
                        letters++;
                        uppercase++;
                    } else if (c < 'a' || c > 'z') {
                        symbols++;
                        nonletter++;
                    } else {
                        letters++;
                        lowercase++;
                    }
                }
                if (131072 == this.mRequestedQuality || 196608 == this.mRequestedQuality) {
                    if (letters > 0 || symbols > 0) {
                        return getString(R.string.lockpassword_pin_contains_non_digits);
                    }
                    int sequence = LockPatternUtils.maxLengthSequence(password);
                    if (196608 == this.mRequestedQuality && sequence > 3) {
                        return getString(R.string.lockpassword_pin_no_sequential_digits);
                    }
                } else if (393216 != this.mRequestedQuality) {
                    boolean alphabetic = 262144 == this.mRequestedQuality;
                    boolean alphanumeric = 327680 == this.mRequestedQuality;
                    if ((alphabetic || alphanumeric) && letters == 0) {
                        return getString(R.string.lockpassword_password_requires_alpha);
                    }
                    if (alphanumeric && numbers == 0) {
                        return getString(R.string.lockpassword_password_requires_digit);
                    }
                } else if (letters < this.mPasswordMinLetters) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_letters, this.mPasswordMinLetters), new Object[]{Integer.valueOf(this.mPasswordMinLetters)});
                } else if (numbers < this.mPasswordMinNumeric) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_numeric, this.mPasswordMinNumeric), new Object[]{Integer.valueOf(this.mPasswordMinNumeric)});
                } else if (lowercase < this.mPasswordMinLowerCase) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_lowercase, this.mPasswordMinLowerCase), new Object[]{Integer.valueOf(this.mPasswordMinLowerCase)});
                } else if (uppercase < this.mPasswordMinUpperCase) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_uppercase, this.mPasswordMinUpperCase), new Object[]{Integer.valueOf(this.mPasswordMinUpperCase)});
                } else if (symbols < this.mPasswordMinSymbols) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_symbols, this.mPasswordMinSymbols), new Object[]{Integer.valueOf(this.mPasswordMinSymbols)});
                } else if (nonletter < this.mPasswordMinNonLetter) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_nonletter, this.mPasswordMinNonLetter), new Object[]{Integer.valueOf(this.mPasswordMinNonLetter)});
                }
                if (!this.mLockPatternUtils.checkPasswordHistory(password, UserHandle.myUserId())) {
                    return null;
                }
                if (this.mIsAlphaMode) {
                    i = R.string.lockpassword_password_recently_used;
                } else {
                    i = R.string.lockpassword_pin_recently_used;
                }
                return getString(i);
            }
        }

        public void handleNext() {
            if (!this.mDone) {
                String pin = this.mPasswordEntry.getText().toString();
                if (!TextUtils.isEmpty(pin)) {
                    String errorMsg = null;
                    if (this.mUiStage == Stage.Introduction) {
                        errorMsg = validatePassword(pin);
                        if (errorMsg == null) {
                            this.mFirstPin = pin;
                            this.mPasswordEntry.setText("");
                            updateStage(Stage.NeedToConfirm);
                        }
                    } else if (this.mUiStage == Stage.NeedToConfirm) {
                        if (this.mFirstPin.equals(pin)) {
                            boolean wasSecureBefore = this.mLockPatternUtils.isSecure(UserHandle.myUserId());
                            boolean isFallback = getActivity().getIntent().getBooleanExtra("lockscreen.weak_fallback", false);
                            String commandKey = getActivity().getIntent().getStringExtra("settings_command_key");
                            String commandValue = getActivity().getIntent().getStringExtra("settings_command_value");
                            String isFallbackFor = getActivity().getIntent().getStringExtra("lockscreen.weak_fallback_for");
                            if (commandKey != null) {
                                System.putString(getActivity().getContentResolver(), commandKey, commandValue);
                            }
                            this.mLockPatternUtils.setCredentialRequiredToDecrypt(getActivity().getIntent().getBooleanExtra("extra_require_password", true));
                            this.mLockPatternUtils.saveLockPassword(pin, this.mCurrentPassword, this.mRequestedQuality, isFallback, isFallbackFor, UserHandle.myUserId());
                            Log.d("ChooseLockPassword", "saveLockPassword : pin " + pin + " mRequestedQuality : " + this.mRequestedQuality);
                            if (this.mHasChallenge) {
                                startVerifyPassword(pin, wasSecureBefore);
                                return;
                            } else {
                                getActivity().setResult(1);
                                finishConfirmStage(wasSecureBefore);
                            }
                        } else {
                            CharSequence tmp = this.mPasswordEntry.getText();
                            if (tmp != null) {
                                Selection.setSelection((Spannable) tmp, 0, tmp.length());
                            }
                            updateStage(Stage.ConfirmWrong);
                        }
                    }
                    if (errorMsg != null) {
                        showError(errorMsg, this.mUiStage);
                    }
                }
            }
        }

        private void startVerifyPassword(String pin, final boolean wasSecureBefore) {
            this.mPasswordEntryInputDisabler.setInputEnabled(false);
            setNextEnabled(false);
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
            }
            this.mPendingLockCheck = LockPatternChecker.verifyPassword(this.mLockPatternUtils, pin, this.mChallenge, UserHandle.myUserId(), new OnVerifyCallback() {
                public void onVerified(byte[] token, int timeoutMs) {
                    if (token == null) {
                        Log.e("ChooseLockPassword", "critical: no token returned from known good password");
                    }
                    ChooseLockPasswordFragment.this.mPasswordEntryInputDisabler.setInputEnabled(true);
                    ChooseLockPasswordFragment.this.setNextEnabled(true);
                    ChooseLockPasswordFragment.this.mPendingLockCheck = null;
                    Intent intent = new Intent();
                    intent.putExtra("hw_auth_token", token);
                    ChooseLockPasswordFragment.this.getActivity().setResult(1, intent);
                    ChooseLockPasswordFragment.this.finishConfirmStage(wasSecureBefore);
                }
            });
        }

        private void finishConfirmStage(boolean wasSecureBefore) {
            getActivity().finish();
            this.mDone = true;
            if (!wasSecureBefore) {
                Intent intent = getRedactionInterstitialIntent(getActivity());
                if (intent != null) {
                    startActivity(intent);
                }
            }
        }

        protected void setNextEnabled(boolean enabled) {
            this.mNextButton.setEnabled(enabled);
        }

        protected void setNextText(int text) {
            this.mNextButton.setText(text);
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cancel_button:
                    if (!(this.mPasswordEntry == null || getActivity() == null)) {
                        ((InputMethodManager) getActivity().getSystemService("input_method")).hideSoftInputFromWindow(this.mPasswordEntry.getWindowToken(), 0);
                    }
                    getActivity().finish();
                    return;
                case R.id.next_button:
                    handleNext();
                    return;
                default:
                    return;
            }
        }

        private void showError(String msg, Stage next) {
            this.mHeaderText.setText(msg);
            this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            Message mesg = this.mHandler.obtainMessage(1, next);
            this.mHandler.removeMessages(1);
            this.mHandler.sendMessageDelayed(mesg, 3000);
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId != 0 && actionId != 6 && actionId != 5) {
                return false;
            }
            handleNext();
            return true;
        }

        private void updateUi() {
            String password = this.mPasswordEntry.getText().toString();
            int length = password.length();
            if (this.mUiStage != Stage.Introduction) {
                boolean z;
                this.mHeaderText.setText(this.mIsAlphaMode ? this.mUiStage.alphaHint : this.mUiStage.numericHint);
                if (length > 0) {
                    z = true;
                } else {
                    z = false;
                }
                setNextEnabled(z);
            } else if (length < this.mPasswordMinLength) {
                int i;
                if (this.mIsAlphaMode) {
                    i = R.string.lockpassword_password_too_short;
                } else {
                    i = R.string.lockpassword_pin_too_short;
                }
                this.mHeaderText.setText(getString(i, new Object[]{Integer.valueOf(this.mPasswordMinLength)}));
                setNextEnabled(false);
            } else {
                String error = validatePassword(password);
                if (error != null) {
                    this.mHeaderText.setText(error);
                    setNextEnabled(false);
                } else {
                    this.mHeaderText.setText(R.string.lockpassword_press_continue);
                    setNextEnabled(true);
                }
            }
            setNextText(this.mUiStage.buttonText);
        }

        public void afterTextChanged(Editable s) {
            if (this.mUiStage == Stage.ConfirmWrong) {
                this.mUiStage = Stage.NeedToConfirm;
            }
            updateUi();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, boolean confirmCredentials) {
        Intent intent = new Intent().setClass(context, ChooseLockPassword.class);
        intent.putExtra("lockscreen.password_type", quality);
        intent.putExtra("lockscreen.password_min", minLength);
        intent.putExtra("lockscreen.password_max", maxLength);
        intent.putExtra("confirm_credentials", confirmCredentials);
        intent.putExtra("extra_require_password", requirePasswordToDecrypt);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password) {
        Intent intent = createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, false);
        intent.putExtra("password", password);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge) {
        Intent intent = createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, false);
        intent.putExtra("has_challenge", true);
        intent.putExtra("challenge", challenge);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, boolean isFallback, int minLength, int maxLength, boolean requirePasswordToDecrypt) {
        Intent intent = new Intent().setClass(context, ChooseLockPassword.class);
        intent.putExtra("lockscreen.password_type", quality);
        intent.putExtra("lockscreen.password_min", minLength);
        intent.putExtra("lockscreen.password_max", maxLength);
        intent.putExtra("confirm_credentials", false);
        intent.putExtra("lockscreen.weak_fallback", isFallback);
        intent.putExtra("extra_require_password", requirePasswordToDecrypt);
        return intent;
    }

    protected boolean isValidFragment(String fragmentName) {
        if (ChooseLockPasswordFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockPasswordFragment.class;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.lockpassword_choose_your_password_header));
    }
}
