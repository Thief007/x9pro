package com.android.settings;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternChecker.OnCheckCallback;
import com.android.internal.widget.LockPatternChecker.OnVerifyCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.CellState;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.AppearAnimationUtils.RowTranslationScaler;
import com.android.settingslib.animation.DisappearAnimationUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfirmLockPattern extends ConfirmDeviceCredentialBaseActivity {

    public static class ConfirmLockPatternFragment extends ConfirmDeviceCredentialBaseFragment implements AppearAnimationCreator<Object> {
        private static /* synthetic */ int[] -com_android_settings_ConfirmLockPattern$StageSwitchesValues;
        private AppearAnimationUtils mAppearAnimationUtils;
        private Runnable mClearPatternRunnable = new C00701();
        private OnPatternListener mConfirmExistingLockPatternListener = new C00732();
        private CountDownTimer mCountdownTimer;
        private CharSequence mDetailsText;
        private TextView mDetailsTextView;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private int mEffectiveUserId;
        private TextView mErrorTextView;
        private CharSequence mHeaderText;
        private TextView mHeaderTextView;
        private View mLeftSpacerLandscape;
        private LockPatternUtils mLockPatternUtils;
        private LockPatternView mLockPatternView;
        private int mNumWrongConfirmAttempts;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private View mRightSpacerLandscape;

        class C00701 implements Runnable {
            C00701() {
            }

            public void run() {
                ConfirmLockPatternFragment.this.mLockPatternView.clearPattern();
            }
        }

        class C00732 implements OnPatternListener {
            C00732() {
            }

            public void onPatternStart() {
                ConfirmLockPatternFragment.this.mLockPatternView.removeCallbacks(ConfirmLockPatternFragment.this.mClearPatternRunnable);
            }

            public void onPatternCleared() {
                ConfirmLockPatternFragment.this.mLockPatternView.removeCallbacks(ConfirmLockPatternFragment.this.mClearPatternRunnable);
            }

            public void onPatternCellAdded(List<Cell> list) {
            }

            public void onPatternDetected(List<Cell> pattern) {
                ConfirmLockPatternFragment.this.mLockPatternView.setEnabled(false);
                if (ConfirmLockPatternFragment.this.mPendingLockCheck != null) {
                    ConfirmLockPatternFragment.this.mPendingLockCheck.cancel(false);
                }
                boolean verifyChallenge = ConfirmLockPatternFragment.this.getActivity().getIntent().getBooleanExtra("has_challenge", false);
                Intent intent = new Intent();
                if (!verifyChallenge) {
                    startCheckPattern(pattern, intent);
                } else if (isInternalActivity()) {
                    startVerifyPattern(pattern, intent);
                } else {
                    onPatternChecked(pattern, false, intent, 0, ConfirmLockPatternFragment.this.mEffectiveUserId);
                }
            }

            private boolean isInternalActivity() {
                return ConfirmLockPatternFragment.this.getActivity() instanceof InternalActivity;
            }

            private void startVerifyPattern(final List<Cell> pattern, final Intent intent) {
                final int localEffectiveUserId = ConfirmLockPatternFragment.this.mEffectiveUserId;
                List<Cell> list = pattern;
                ConfirmLockPatternFragment.this.mPendingLockCheck = LockPatternChecker.verifyPattern(ConfirmLockPatternFragment.this.mLockPatternUtils, list, ConfirmLockPatternFragment.this.getActivity().getIntent().getLongExtra("challenge", 0), localEffectiveUserId, new OnVerifyCallback() {
                    public void onVerified(byte[] token, int timeoutMs) {
                        ConfirmLockPatternFragment.this.mPendingLockCheck = null;
                        boolean matched = false;
                        if (token != null) {
                            matched = true;
                            intent.putExtra("hw_auth_token", token);
                        }
                        C00732.this.onPatternChecked(pattern, matched, intent, timeoutMs, localEffectiveUserId);
                    }
                });
            }

            private void startCheckPattern(final List<Cell> pattern, final Intent intent) {
                if (pattern.size() < 4) {
                    onPatternChecked(pattern, false, intent, 0, ConfirmLockPatternFragment.this.mEffectiveUserId);
                    return;
                }
                final int localEffectiveUserId = ConfirmLockPatternFragment.this.mEffectiveUserId;
                ConfirmLockPatternFragment.this.mPendingLockCheck = LockPatternChecker.checkPattern(ConfirmLockPatternFragment.this.mLockPatternUtils, pattern, localEffectiveUserId, new OnCheckCallback() {
                    public void onChecked(boolean matched, int timeoutMs) {
                        ConfirmLockPatternFragment.this.mPendingLockCheck = null;
                        if (matched && C00732.this.isInternalActivity()) {
                            intent.putExtra("type", 2);
                            intent.putExtra("password", LockPatternUtils.patternToString(pattern));
                        }
                        C00732.this.onPatternChecked(pattern, matched, intent, timeoutMs, localEffectiveUserId);
                    }
                });
            }

            private void onPatternChecked(List<Cell> list, boolean matched, Intent intent, int timeoutMs, int effectiveUserId) {
                ConfirmLockPatternFragment.this.mLockPatternView.setEnabled(true);
                if (matched) {
                    ConfirmLockPatternFragment.this.startDisappearAnimation(intent);
                } else if (timeoutMs > 0) {
                    ConfirmLockPatternFragment.this.handleAttemptLockout(ConfirmLockPatternFragment.this.mLockPatternUtils.setLockoutAttemptDeadline(effectiveUserId, timeoutMs));
                } else {
                    ConfirmLockPatternFragment.this.updateStage(Stage.NeedToUnlockWrong);
                    ConfirmLockPatternFragment.this.postClearPatternRunnable();
                }
            }
        }

        class C00743 implements RowTranslationScaler {
            C00743() {
            }

            public float getRowTranslationScale(int row, int numRows) {
                return ((float) (numRows - row)) / ((float) numRows);
            }
        }

        private static /* synthetic */ int[] -getcom_android_settings_ConfirmLockPattern$StageSwitchesValues() {
            if (-com_android_settings_ConfirmLockPattern$StageSwitchesValues != null) {
                return -com_android_settings_ConfirmLockPattern$StageSwitchesValues;
            }
            int[] iArr = new int[Stage.values().length];
            try {
                iArr[Stage.LockedOut.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Stage.NeedToUnlock.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Stage.NeedToUnlockWrong.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            -com_android_settings_ConfirmLockPattern$StageSwitchesValues = iArr;
            return iArr;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            this.mEffectiveUserId = Utils.getEffectiveUserId(getActivity());
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_lock_pattern, null);
            this.mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            this.mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            this.mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            this.mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            this.mLeftSpacerLandscape = view.findViewById(R.id.leftSpacer);
            this.mRightSpacerLandscape = view.findViewById(R.id.rightSpacer);
            ((LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout)).setDefaultTouchRecepient(this.mLockPatternView);
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                this.mHeaderText = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.header");
                this.mDetailsText = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.details");
            }
            this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
            this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(this.mEffectiveUserId));
            this.mLockPatternView.setOnPatternListener(this.mConfirmExistingLockPatternListener);
            updateStage(Stage.NeedToUnlock);
            if (savedInstanceState != null) {
                this.mNumWrongConfirmAttempts = savedInstanceState.getInt("num_wrong_attempts");
            } else if (!this.mLockPatternUtils.isLockPatternEnabled(this.mEffectiveUserId)) {
                getActivity().setResult(-1, new Intent());
                getActivity().finish();
            }
            this.mAppearAnimationUtils = new AppearAnimationUtils(getContext(), 220, 2.0f, 1.3f, AnimationUtils.loadInterpolator(getContext(), 17563662));
            this.mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(), 125, 4.0f, 0.3f, AnimationUtils.loadInterpolator(getContext(), 17563663), new C00743());
            setAccessibilityTitle(this.mHeaderTextView.getText());
            return view;
        }

        public void onSaveInstanceState(Bundle outState) {
            outState.putInt("num_wrong_attempts", this.mNumWrongConfirmAttempts);
        }

        public void onPause() {
            super.onPause();
            if (this.mCountdownTimer != null) {
                this.mCountdownTimer.cancel();
            }
            if (this.mPendingLockCheck != null) {
                this.mPendingLockCheck.cancel(false);
                this.mPendingLockCheck = null;
            }
        }

        protected int getMetricsCategory() {
            return 31;
        }

        public void onResume() {
            super.onResume();
            long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(this.mEffectiveUserId);
            if (deadline != 0) {
                handleAttemptLockout(deadline);
            } else if (!this.mLockPatternView.isEnabled()) {
                this.mNumWrongConfirmAttempts = 0;
                updateStage(Stage.NeedToUnlock);
            }
        }

        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            this.mHeaderTextView.setAlpha(0.0f);
            this.mCancelButton.setAlpha(0.0f);
            this.mLockPatternView.setAlpha(0.0f);
            this.mDetailsTextView.setAlpha(0.0f);
            this.mFingerprintIcon.setAlpha(0.0f);
        }

        private Object[][] getActiveViews() {
            int i;
            int j;
            ArrayList<ArrayList<Object>> result = new ArrayList();
            result.add(new ArrayList(Collections.singletonList(this.mHeaderTextView)));
            result.add(new ArrayList(Collections.singletonList(this.mDetailsTextView)));
            if (this.mCancelButton.getVisibility() == 0) {
                result.add(new ArrayList(Collections.singletonList(this.mCancelButton)));
            }
            CellState[][] cellStates = this.mLockPatternView.getCellStates();
            for (i = 0; i < cellStates.length; i++) {
                ArrayList<Object> row = new ArrayList();
                for (Object add : cellStates[i]) {
                    row.add(add);
                }
                result.add(row);
            }
            if (this.mFingerprintIcon.getVisibility() == 0) {
                result.add(new ArrayList(Collections.singletonList(this.mFingerprintIcon)));
            }
            Object[][] resultArr = (Object[][]) Array.newInstance(Object.class, new int[]{result.size(), cellStates[0].length});
            for (i = 0; i < result.size(); i++) {
                row = (ArrayList) result.get(i);
                for (j = 0; j < row.size(); j++) {
                    resultArr[i][j] = row.get(j);
                }
            }
            return resultArr;
        }

        public void startEnterAnimation() {
            super.startEnterAnimation();
            this.mLockPatternView.setAlpha(1.0f);
            this.mAppearAnimationUtils.startAnimation2d(getActiveViews(), null, this);
        }

        private void updateStage(Stage stage) {
            switch (-getcom_android_settings_ConfirmLockPattern$StageSwitchesValues()[stage.ordinal()]) {
                case 1:
                    this.mLockPatternView.clearPattern();
                    this.mLockPatternView.setEnabled(false);
                    break;
                case 2:
                    if (this.mHeaderText != null) {
                        this.mHeaderTextView.setText(this.mHeaderText);
                    } else {
                        this.mHeaderTextView.setText(R.string.lockpassword_confirm_your_pattern_header);
                    }
                    if (this.mDetailsText != null) {
                        this.mDetailsTextView.setText(this.mDetailsText);
                    } else {
                        this.mDetailsTextView.setText(R.string.lockpassword_confirm_your_pattern_generic);
                    }
                    this.mErrorTextView.setText("");
                    this.mLockPatternView.setEnabled(true);
                    this.mLockPatternView.enableInput();
                    this.mLockPatternView.clearPattern();
                    break;
                case 3:
                    this.mErrorTextView.setText(R.string.lockpattern_need_to_unlock_wrong);
                    this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    this.mLockPatternView.setEnabled(true);
                    this.mLockPatternView.enableInput();
                    break;
            }
            this.mHeaderTextView.announceForAccessibility(this.mHeaderTextView.getText());
        }

        private void postClearPatternRunnable() {
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 2000);
        }

        protected void authenticationSucceeded() {
            startDisappearAnimation(new Intent());
        }

        private void startDisappearAnimation(final Intent intent) {
            if (getActivity().getThemeResId() == R.style.Theme.ConfirmDeviceCredentialsDark) {
                this.mLockPatternView.clearPattern();
                this.mDisappearAnimationUtils.startAnimation2d(getActiveViews(), new Runnable() {
                    public void run() {
                        ConfirmLockPatternFragment.this.getActivity().setResult(-1, intent);
                        ConfirmLockPatternFragment.this.getActivity().finish();
                        ConfirmLockPatternFragment.this.getActivity().overridePendingTransition(R.anim.confirm_credential_close_enter, R.anim.confirm_credential_close_exit);
                    }
                }, this);
                return;
            }
            getActivity().setResult(-1, intent);
            getActivity().finish();
        }

        public void onFingerprintIconVisibilityChanged(boolean visible) {
            int i = 8;
            if (this.mLeftSpacerLandscape != null && this.mRightSpacerLandscape != null) {
                int i2;
                View view = this.mLeftSpacerLandscape;
                if (visible) {
                    i2 = 8;
                } else {
                    i2 = 0;
                }
                view.setVisibility(i2);
                View view2 = this.mRightSpacerLandscape;
                if (!visible) {
                    i = 0;
                }
                view2.setVisibility(i);
            }
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            this.mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - SystemClock.elapsedRealtime(), 1000) {
                public void onTick(long millisUntilFinished) {
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    ConfirmLockPatternFragment.this.mErrorTextView.setText(ConfirmLockPatternFragment.this.getString(R.string.lockpattern_too_many_failed_confirmation_attempts, new Object[]{Integer.valueOf(secondsCountdown)}));
                }

                public void onFinish() {
                    ConfirmLockPatternFragment.this.mNumWrongConfirmAttempts = 0;
                    ConfirmLockPatternFragment.this.updateStage(Stage.NeedToUnlock);
                }
            }.start();
        }

        public void createAnimation(Object obj, long delay, long duration, float translationY, boolean appearing, Interpolator interpolator, Runnable finishListener) {
            if (obj instanceof CellState) {
                float f;
                CellState animatedCell = (CellState) obj;
                LockPatternView lockPatternView = this.mLockPatternView;
                float f2 = appearing ? 1.0f : 0.0f;
                float f3 = appearing ? translationY : 0.0f;
                if (appearing) {
                    f = 0.0f;
                } else {
                    f = translationY;
                }
                lockPatternView.startCellStateAnimation(animatedCell, 1.0f, f2, f3, f, appearing ? 0.0f : 1.0f, 1.0f, delay, duration, interpolator, finishListener);
                return;
            }
            this.mAppearAnimationUtils.createAnimation((View) obj, delay, duration, translationY, appearing, interpolator, finishListener);
        }
    }

    public static class InternalActivity extends ConfirmLockPattern {
    }

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", ConfirmLockPatternFragment.class.getName());
        return modIntent;
    }

    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPatternFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }
}
