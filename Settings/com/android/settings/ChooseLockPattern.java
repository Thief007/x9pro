package com.android.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnVerifyCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import com.android.settings.notification.RedactionInterstitial;
import com.android.setupwizardlib.R$styleable;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChooseLockPattern extends SettingsActivity {

    public static class ChooseLockPatternFragment extends InstrumentedFragment implements OnClickListener {
        private static /* synthetic */ int[] f6xfa0f5685;
        private final List<Cell> mAnimatePattern = Collections.unmodifiableList(Lists.newArrayList(new Cell[]{Cell.of(0, 0), Cell.of(0, 1), Cell.of(1, 1), Cell.of(2, 1)}));
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        protected OnPatternListener mChooseNewLockPatternListener = new C00591();
        protected List<Cell> mChosenPattern = null;
        private Runnable mClearPatternRunnable = new C00602();
        private String mCurrentPattern;
        private boolean mDone = false;
        private TextView mFooterLeftButton;
        private TextView mFooterRightButton;
        protected TextView mFooterText;
        private boolean mHasChallenge;
        protected TextView mHeaderText;
        protected LockPatternView mLockPatternView;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private Stage mUiStage = Stage.Introduction;

        class C00591 implements OnPatternListener {
            C00591() {
            }

            public void onPatternStart() {
                ChooseLockPatternFragment.this.mLockPatternView.removeCallbacks(ChooseLockPatternFragment.this.mClearPatternRunnable);
                patternInProgress();
            }

            public void onPatternCleared() {
                ChooseLockPatternFragment.this.mLockPatternView.removeCallbacks(ChooseLockPatternFragment.this.mClearPatternRunnable);
            }

            public void onPatternDetected(List<Cell> pattern) {
                if (ChooseLockPatternFragment.this.mUiStage == Stage.NeedToConfirm || ChooseLockPatternFragment.this.mUiStage == Stage.ConfirmWrong) {
                    if (ChooseLockPatternFragment.this.mChosenPattern == null) {
                        throw new IllegalStateException("null chosen pattern in stage 'need to confirm");
                    } else if (ChooseLockPatternFragment.this.mChosenPattern.equals(pattern)) {
                        ChooseLockPatternFragment.this.updateStage(Stage.ChoiceConfirmed);
                    } else {
                        ChooseLockPatternFragment.this.updateStage(Stage.ConfirmWrong);
                    }
                } else if (ChooseLockPatternFragment.this.mUiStage != Stage.Introduction && ChooseLockPatternFragment.this.mUiStage != Stage.ChoiceTooShort) {
                    throw new IllegalStateException("Unexpected stage " + ChooseLockPatternFragment.this.mUiStage + " when " + "entering the pattern.");
                } else if (pattern.size() < 4) {
                    ChooseLockPatternFragment.this.updateStage(Stage.ChoiceTooShort);
                } else {
                    ChooseLockPatternFragment.this.mChosenPattern = new ArrayList(pattern);
                    ChooseLockPatternFragment.this.updateStage(Stage.FirstChoiceValid);
                }
            }

            public void onPatternCellAdded(List<Cell> list) {
            }

            private void patternInProgress() {
                ChooseLockPatternFragment.this.mHeaderText.setText(R.string.lockpattern_recording_inprogress);
                ChooseLockPatternFragment.this.mFooterText.setText("");
                ChooseLockPatternFragment.this.mFooterLeftButton.setEnabled(false);
                ChooseLockPatternFragment.this.mFooterRightButton.setEnabled(false);
            }
        }

        class C00602 implements Runnable {
            C00602() {
            }

            public void run() {
                ChooseLockPatternFragment.this.mLockPatternView.clearPattern();
            }
        }

        enum LeftButtonMode {
            Cancel(R.string.cancel, true),
            CancelDisabled(R.string.cancel, false),
            Retry(R.string.lockpattern_retry_button_text, true),
            RetryDisabled(R.string.lockpattern_retry_button_text, false),
            Gone(-1, false);
            
            final boolean enabled;
            final int text;

            private LeftButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }
        }

        enum RightButtonMode {
            Continue(R.string.lockpattern_continue_button_text, true),
            ContinueDisabled(R.string.lockpattern_continue_button_text, false),
            Confirm(R.string.lockpattern_confirm_button_text, true),
            ConfirmDisabled(R.string.lockpattern_confirm_button_text, false),
            Ok(17039370, true);
            
            final boolean enabled;
            final int text;

            private RightButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }
        }

        protected enum Stage {
            Introduction(R.string.lockpattern_recording_intro_header, LeftButtonMode.Cancel, RightButtonMode.ContinueDisabled, -1, true),
            HelpScreen(R.string.lockpattern_settings_help_how_to_record, LeftButtonMode.Gone, RightButtonMode.Ok, -1, false),
            ChoiceTooShort(R.string.lockpattern_recording_incorrect_too_short, LeftButtonMode.Retry, RightButtonMode.ContinueDisabled, -1, true),
            FirstChoiceValid(R.string.lockpattern_pattern_entered_header, LeftButtonMode.Retry, RightButtonMode.Continue, -1, false),
            NeedToConfirm(R.string.lockpattern_need_to_confirm, LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled, -1, true),
            ConfirmWrong(R.string.lockpattern_need_to_unlock_wrong, LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled, -1, true),
            ChoiceConfirmed(R.string.lockpattern_pattern_confirmed_header, LeftButtonMode.Cancel, RightButtonMode.Confirm, -1, false);
            
            final int footerMessage;
            final int headerMessage;
            final LeftButtonMode leftMode;
            final boolean patternEnabled;
            final RightButtonMode rightMode;

            private Stage(int headerMessage, LeftButtonMode leftMode, RightButtonMode rightMode, int footerMessage, boolean patternEnabled) {
                this.headerMessage = headerMessage;
                this.leftMode = leftMode;
                this.rightMode = rightMode;
                this.footerMessage = footerMessage;
                this.patternEnabled = patternEnabled;
            }
        }

        private static /* synthetic */ int[] m2x379bf461() {
            if (f6xfa0f5685 != null) {
                return f6xfa0f5685;
            }
            int[] iArr = new int[Stage.values().length];
            try {
                iArr[Stage.ChoiceConfirmed.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Stage.ChoiceTooShort.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Stage.ConfirmWrong.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Stage.FirstChoiceValid.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[Stage.HelpScreen.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[Stage.Introduction.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[Stage.NeedToConfirm.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            f6xfa0f5685 = iArr;
            return iArr;
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case 55:
                    if (resultCode != -1) {
                        getActivity().setResult(1);
                        getActivity().finish();
                    } else {
                        this.mCurrentPattern = data.getStringExtra("password");
                    }
                    updateStage(Stage.Introduction);
                    return;
                default:
                    return;
            }
        }

        protected void setRightButtonEnabled(boolean enabled) {
            this.mFooterRightButton.setEnabled(enabled);
        }

        protected void setRightButtonText(int text) {
            this.mFooterRightButton.setText(text);
        }

        protected int getMetricsCategory() {
            return 29;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            if (!(getActivity() instanceof ChooseLockPattern)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_lock_pattern, container, false);
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            this.mHeaderText = (TextView) view.findViewById(R.id.headerText);
            this.mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            this.mLockPatternView.setOnPatternListener(this.mChooseNewLockPatternListener);
            this.mLockPatternView.setTactileFeedbackEnabled(this.mChooseLockSettingsHelper.utils().isTactileFeedbackEnabled());
            this.mFooterText = (TextView) view.findViewById(R.id.footerText);
            this.mFooterLeftButton = (TextView) view.findViewById(R.id.footerLeftButton);
            this.mFooterRightButton = (TextView) view.findViewById(R.id.footerRightButton);
            this.mFooterLeftButton.setOnClickListener(this);
            this.mFooterRightButton.setOnClickListener(this);
            ((LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout)).setDefaultTouchRecepient(this.mLockPatternView);
            boolean confirmCredentials = getActivity().getIntent().getBooleanExtra("confirm_credentials", true);
            Intent intent = getActivity().getIntent();
            this.mCurrentPattern = intent.getStringExtra("password");
            this.mHasChallenge = intent.getBooleanExtra("has_challenge", false);
            this.mChallenge = intent.getLongExtra("challenge", 0);
            if (savedInstanceState != null) {
                String patternString = savedInstanceState.getString("chosenPattern");
                if (patternString != null) {
                    this.mChosenPattern = LockPatternUtils.stringToPattern(patternString);
                }
                if (this.mCurrentPattern == null) {
                    this.mCurrentPattern = savedInstanceState.getString("currentPattern");
                }
                updateStage(Stage.values()[savedInstanceState.getInt("uiStage")]);
            } else if (confirmCredentials) {
                updateStage(Stage.NeedToConfirm);
                if (!this.mChooseLockSettingsHelper.launchConfirmationActivity(55, getString(R.string.unlock_set_unlock_launch_picker_title), true)) {
                    updateStage(Stage.Introduction);
                }
            } else {
                updateStage(Stage.Introduction);
            }
            this.mDone = false;
        }

        public void onResume() {
            super.onResume();
            this.mLockPatternView.enableInput();
        }

        public void onPause() {
            super.onPause();
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
                this.mPendingLockCheck = null;
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context);
        }

        public void handleLeftButton() {
            if (this.mUiStage.leftMode == LeftButtonMode.Retry) {
                this.mChosenPattern = null;
                this.mLockPatternView.clearPattern();
                updateStage(Stage.Introduction);
            } else if (this.mUiStage.leftMode == LeftButtonMode.Cancel) {
                getActivity().finish();
            } else {
                throw new IllegalStateException("left footer button pressed, but stage of " + this.mUiStage + " doesn't make sense");
            }
        }

        public void handleRightButton() {
            if (this.mUiStage.rightMode == RightButtonMode.Continue) {
                if (this.mUiStage != Stage.FirstChoiceValid) {
                    throw new IllegalStateException("expected ui stage " + Stage.FirstChoiceValid + " when button is " + RightButtonMode.Continue);
                }
                updateStage(Stage.NeedToConfirm);
            } else if (this.mUiStage.rightMode == RightButtonMode.Confirm) {
                if (this.mUiStage != Stage.ChoiceConfirmed) {
                    throw new IllegalStateException("expected ui stage " + Stage.ChoiceConfirmed + " when button is " + RightButtonMode.Confirm);
                }
                saveChosenPatternAndFinish();
            } else if (this.mUiStage.rightMode != RightButtonMode.Ok) {
            } else {
                if (this.mUiStage != Stage.HelpScreen) {
                    throw new IllegalStateException("Help screen is only mode with ok button, but stage is " + this.mUiStage);
                }
                this.mLockPatternView.clearPattern();
                this.mLockPatternView.setDisplayMode(DisplayMode.Correct);
                updateStage(Stage.Introduction);
            }
        }

        public void onClick(View v) {
            if (v == this.mFooterLeftButton) {
                handleLeftButton();
            } else if (v == this.mFooterRightButton) {
                handleRightButton();
            }
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("uiStage", this.mUiStage.ordinal());
            if (this.mChosenPattern != null) {
                outState.putString("chosenPattern", LockPatternUtils.patternToString(this.mChosenPattern));
            }
            if (this.mCurrentPattern != null) {
                outState.putString("currentPattern", this.mCurrentPattern);
            }
        }

        protected void updateStage(Stage stage) {
            Stage previousStage = this.mUiStage;
            this.mUiStage = stage;
            if (stage == Stage.ChoiceTooShort) {
                this.mHeaderText.setText(getResources().getString(stage.headerMessage, new Object[]{Integer.valueOf(4)}));
            } else {
                this.mHeaderText.setText(stage.headerMessage);
            }
            if (stage.footerMessage == -1) {
                this.mFooterText.setText("");
            } else {
                this.mFooterText.setText(stage.footerMessage);
            }
            if (stage.leftMode == LeftButtonMode.Gone) {
                this.mFooterLeftButton.setVisibility(8);
            } else {
                this.mFooterLeftButton.setVisibility(0);
                this.mFooterLeftButton.setText(stage.leftMode.text);
                this.mFooterLeftButton.setEnabled(stage.leftMode.enabled);
            }
            setRightButtonText(stage.rightMode.text);
            setRightButtonEnabled(stage.rightMode.enabled);
            if (stage.patternEnabled) {
                this.mLockPatternView.enableInput();
            } else {
                this.mLockPatternView.disableInput();
            }
            this.mLockPatternView.setDisplayMode(DisplayMode.Correct);
            boolean announceAlways = false;
            switch (m2x379bf461()[this.mUiStage.ordinal()]) {
                case 2:
                    this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    postClearPatternRunnable();
                    announceAlways = true;
                    break;
                case 3:
                    this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    postClearPatternRunnable();
                    announceAlways = true;
                    break;
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    this.mLockPatternView.setPattern(DisplayMode.Animate, this.mAnimatePattern);
                    break;
                case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                    this.mLockPatternView.clearPattern();
                    break;
                case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                    this.mLockPatternView.clearPattern();
                    break;
            }
            if (previousStage != stage || announceAlways) {
                this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            }
        }

        private void postClearPatternRunnable() {
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 2000);
        }

        private void saveChosenPatternAndFinish() {
            if (!this.mDone) {
                LockPatternUtils utils = this.mChooseLockSettingsHelper.utils();
                boolean lockVirgin = !utils.isPatternEverChosen(UserHandle.myUserId());
                boolean isFallback = getActivity().getIntent().getBooleanExtra("lockscreen.weak_fallback", false);
                String isFallbackFor = getActivity().getIntent().getStringExtra("lockscreen.weak_fallback_for");
                String commandKey = getActivity().getIntent().getStringExtra("settings_command_key");
                String commandValue = getActivity().getIntent().getStringExtra("settings_command_value");
                if (commandKey != null) {
                    System.putString(getActivity().getContentResolver(), commandKey, commandValue);
                }
                boolean wasSecureBefore = utils.isSecure(UserHandle.myUserId());
                utils.setCredentialRequiredToDecrypt(getActivity().getIntent().getBooleanExtra("extra_require_password", true));
                utils.saveLockPattern(this.mChosenPattern, this.mCurrentPattern, isFallback, isFallbackFor, UserHandle.myUserId());
                if (lockVirgin) {
                    utils.setVisiblePatternEnabled(true, UserHandle.myUserId());
                }
                if (this.mHasChallenge) {
                    startVerifyPattern(utils, wasSecureBefore);
                } else {
                    if (!wasSecureBefore) {
                        Intent intent = getRedactionInterstitialIntent(getActivity());
                        if (intent != null) {
                            startActivity(intent);
                        }
                    }
                    getActivity().setResult(1);
                    doFinish();
                }
            }
        }

        private void startVerifyPattern(LockPatternUtils utils, final boolean wasSecureBefore) {
            this.mLockPatternView.disableInput();
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
            }
            this.mPendingLockCheck = LockPatternChecker.verifyPattern(utils, this.mChosenPattern, this.mChallenge, UserHandle.myUserId(), new OnVerifyCallback() {
                public void onVerified(byte[] token, int timeoutMs) {
                    Intent intent;
                    if (token == null) {
                        Log.e("ChooseLockPattern", "critical: no token returned for known good pattern");
                    }
                    ChooseLockPatternFragment.this.mLockPatternView.enableInput();
                    ChooseLockPatternFragment.this.mPendingLockCheck = null;
                    if (!wasSecureBefore) {
                        intent = ChooseLockPatternFragment.this.getRedactionInterstitialIntent(ChooseLockPatternFragment.this.getActivity());
                        if (intent != null) {
                            ChooseLockPatternFragment.this.startActivity(intent);
                        }
                    }
                    intent = new Intent();
                    intent.putExtra("hw_auth_token", token);
                    ChooseLockPatternFragment.this.getActivity().setResult(1, intent);
                    ChooseLockPatternFragment.this.doFinish();
                }
            });
        }

        private void doFinish() {
            getActivity().finish();
            this.mDone = true;
        }
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, boolean confirmCredentials) {
        Intent intent = new Intent(context, ChooseLockPattern.class);
        intent.putExtra("key_lock_method", "pattern");
        intent.putExtra("confirm_credentials", confirmCredentials);
        intent.putExtra("extra_require_password", requirePassword);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, String pattern) {
        Intent intent = createIntent(context, requirePassword, false);
        intent.putExtra("password", pattern);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, long challenge) {
        Intent intent = createIntent(context, requirePassword, false);
        intent.putExtra("has_challenge", true);
        intent.putExtra("challenge", challenge);
        return intent;
    }

    public static Intent createIntent(Context context, boolean isFallback, boolean requirePassword, boolean confirmCredentials) {
        Intent intent = new Intent(context, ChooseLockPattern.class);
        intent.putExtra("key_lock_method", "pattern");
        intent.putExtra("confirm_credentials", confirmCredentials);
        intent.putExtra("lockscreen.weak_fallback", isFallback);
        intent.putExtra("extra_require_password", requirePassword);
        return intent;
    }

    protected boolean isValidFragment(String fragmentName) {
        if (ChooseLockPatternFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockPatternFragment.class;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.lockpassword_choose_your_pattern_header));
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }
}
