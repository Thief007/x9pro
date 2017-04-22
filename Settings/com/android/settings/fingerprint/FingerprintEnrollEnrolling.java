package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable2.AnimationCallback;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.fingerprint.FingerprintEnrollSidecar.Listener;

public class FingerprintEnrollEnrolling extends FingerprintEnrollBase implements Listener {
    private boolean mAnimationCancelled;
    private final Runnable mDelayedFinishRunnable = new C03623();
    private TextView mErrorText;
    private String mFactoryMode = null;
    private Interpolator mFastOutLinearInInterpolator;
    private Interpolator mFastOutSlowInInterpolator;
    private ImageView mFingerprintAnimator;
    private BroadcastReceiver mFingerprintEnrollReceiver = new C03601();
    private final AnimationCallback mIconAnimationCallback = new C03644();
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private int mIconTouchCount;
    private int mIndicatorBackgroundActivatedColor;
    private int mIndicatorBackgroundRestingColor;
    private IntentFilter mIntentFilter;
    private Interpolator mLinearOutSlowInInterpolator;
    private PowerManager mPowerManager;
    private ObjectAnimator mProgressAnim;
    private final AnimatorListener mProgressAnimationListener = new C03612();
    private ProgressBar mProgressBar;
    private TextView mRepeatMessage;
    private boolean mRestoring;
    private final Runnable mShowDialogRunnable = new C03655();
    private FingerprintEnrollSidecar mSidecar;
    private TextView mStartMessage;
    private final Runnable mTouchAgainRunnable = new C03666();

    class C03601 extends BroadcastReceiver {
        C03601() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED") && intent.getIntExtra("plugged", 0) > 0) {
                FingerprintEnrollEnrolling.this.mIconAnimationDrawable.start();
            }
        }
    }

    class C03612 implements AnimatorListener {
        C03612() {
        }

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            if (FingerprintEnrollEnrolling.this.mProgressBar.getProgress() >= 10000) {
                FingerprintEnrollEnrolling.this.mProgressBar.postDelayed(FingerprintEnrollEnrolling.this.mDelayedFinishRunnable, 250);
            }
        }

        public void onAnimationCancel(Animator animation) {
        }
    }

    class C03623 implements Runnable {
        C03623() {
        }

        public void run() {
            FingerprintEnrollEnrolling.this.launchFinish(FingerprintEnrollEnrolling.this.mToken);
        }
    }

    class C03644 extends AnimationCallback {

        class C03631 implements Runnable {
            C03631() {
            }

            public void run() {
                FingerprintEnrollEnrolling.this.startIconAnimation();
            }
        }

        C03644() {
        }

        public void onAnimationEnd(Drawable d) {
            if (!FingerprintEnrollEnrolling.this.mAnimationCancelled) {
                FingerprintEnrollEnrolling.this.mFingerprintAnimator.post(new C03631());
            }
        }
    }

    class C03655 implements Runnable {
        C03655() {
        }

        public void run() {
            FingerprintEnrollEnrolling.this.showIconTouchDialog();
        }
    }

    class C03666 implements Runnable {
        C03666() {
        }

        public void run() {
            FingerprintEnrollEnrolling.this.showError(FingerprintEnrollEnrolling.this.getString(R.string.security_settings_fingerprint_enroll_lift_touch_again));
        }
    }

    class C03677 implements OnTouchListener {
        C03677() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            if (event.getActionMasked() == 0) {
                FingerprintEnrollEnrolling fingerprintEnrollEnrolling = FingerprintEnrollEnrolling.this;
                fingerprintEnrollEnrolling.mIconTouchCount = fingerprintEnrollEnrolling.mIconTouchCount + 1;
                if (FingerprintEnrollEnrolling.this.mIconTouchCount == 3) {
                    FingerprintEnrollEnrolling.this.showIconTouchDialog();
                } else {
                    FingerprintEnrollEnrolling.this.mFingerprintAnimator.postDelayed(FingerprintEnrollEnrolling.this.mShowDialogRunnable, 500);
                }
            } else if (event.getActionMasked() == 3 || event.getActionMasked() == 1) {
                FingerprintEnrollEnrolling.this.mFingerprintAnimator.removeCallbacks(FingerprintEnrollEnrolling.this.mShowDialogRunnable);
            }
            return true;
        }
    }

    class C03688 implements AnimatorUpdateListener {
        C03688() {
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            FingerprintEnrollEnrolling.this.mFingerprintAnimator.setBackgroundTintList(ColorStateList.valueOf(((Integer) animation.getAnimatedValue()).intValue()));
        }
    }

    public static class ErrorDialog extends DialogFragment {
        static ErrorDialog newInstance(CharSequence msg, int msgId) {
            ErrorDialog dlg = new ErrorDialog();
            Bundle args = new Bundle();
            args.putCharSequence("error_msg", msg);
            args.putInt("error_id", msgId);
            dlg.setArguments(args);
            return dlg;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new Builder(getActivity());
            CharSequence errorString = getArguments().getCharSequence("error_msg");
            final int errMsgId = getArguments().getInt("error_id");
            builder.setTitle(R.string.security_settings_fingerprint_enroll_error_dialog_title).setMessage(errorString).setCancelable(false).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    int i = 3;
                    dialog.dismiss();
                    boolean wasTimeout = errMsgId == 3;
                    Activity activity = ErrorDialog.this.getActivity();
                    if (!wasTimeout) {
                        i = 1;
                    }
                    activity.setResult(i);
                    activity.finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

    public static class IconTouchDialog extends DialogFragment {

        class C03711 implements OnClickListener {
            C03711() {
            }

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new Builder(getActivity());
            builder.setTitle(R.string.security_settings_fingerprint_enroll_touch_dialog_title).setMessage(R.string.security_settings_fingerprint_enroll_touch_dialog_message).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new C03711());
            return builder.create();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        boolean z;
        super.onCreate(savedInstanceState);
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mFactoryMode = getIntent().getStringExtra("flag");
        setContentView(R.layout.fingerprint_enroll_enrolling);
        setHeaderText(R.string.security_settings_fingerprint_enroll_start_title);
        this.mStartMessage = (TextView) findViewById(R.id.start_message);
        this.mRepeatMessage = (TextView) findViewById(R.id.repeat_message);
        this.mErrorText = (TextView) findViewById(R.id.error_text);
        this.mProgressBar = (ProgressBar) findViewById(R.id.fingerprint_progress_bar);
        this.mFingerprintAnimator = (ImageView) findViewById(R.id.fingerprint_animator);
        this.mIconAnimationDrawable = (AnimatedVectorDrawable) this.mFingerprintAnimator.getDrawable();
        this.mIconAnimationDrawable.registerAnimationCallback(this.mIconAnimationCallback);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this, 17563661);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(this, 17563662);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(this, 17563663);
        this.mFingerprintAnimator.setOnTouchListener(new C03677());
        this.mIndicatorBackgroundRestingColor = getColor(R.color.fingerprint_indicator_background_resting);
        this.mIndicatorBackgroundActivatedColor = getColor(R.color.fingerprint_indicator_background_activated);
        if (savedInstanceState != null) {
            z = true;
        } else {
            z = false;
        }
        this.mRestoring = z;
    }

    public void onResume() {
        super.onResume();
        registerReceiver(this.mFingerprintEnrollReceiver, this.mIntentFilter);
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(this.mFingerprintEnrollReceiver);
    }

    protected void onStart() {
        super.onStart();
        this.mSidecar = (FingerprintEnrollSidecar) getFragmentManager().findFragmentByTag("sidecar");
        if (this.mSidecar == null) {
            this.mSidecar = new FingerprintEnrollSidecar();
            getFragmentManager().beginTransaction().add(this.mSidecar, "sidecar").commit();
        }
        this.mSidecar.setListener(this);
        updateProgress(false);
        updateDescription();
        if (this.mRestoring) {
            startIconAnimation();
        }
    }

    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        this.mAnimationCancelled = false;
        startIconAnimation();
    }

    private void startIconAnimation() {
        this.mPowerManager = (PowerManager) getSystemService("power");
        if (this.mPowerManager.isPowerSaveMode()) {
            this.mIconAnimationDrawable.stop();
        } else {
            this.mIconAnimationDrawable.start();
        }
    }

    private void stopIconAnimation() {
        this.mAnimationCancelled = true;
        this.mIconAnimationDrawable.stop();
    }

    protected void onStop() {
        super.onStop();
        this.mSidecar.setListener(null);
        stopIconAnimation();
        if (!isChangingConfigurations()) {
            finish();
        }
    }

    private void animateProgress(int progress) {
        if (this.mProgressAnim != null) {
            this.mProgressAnim.cancel();
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(this.mProgressBar, "progress", new int[]{this.mProgressBar.getProgress(), progress});
        anim.addListener(this.mProgressAnimationListener);
        anim.setInterpolator(this.mFastOutSlowInInterpolator);
        anim.setDuration(250);
        anim.start();
        this.mProgressAnim = anim;
    }

    private void animateFlash() {
        ValueAnimator anim = ValueAnimator.ofArgb(new int[]{this.mIndicatorBackgroundRestingColor, this.mIndicatorBackgroundActivatedColor});
        final AnimatorUpdateListener listener = new C03688();
        anim.addUpdateListener(listener);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ValueAnimator anim = ValueAnimator.ofArgb(new int[]{FingerprintEnrollEnrolling.this.mIndicatorBackgroundActivatedColor, FingerprintEnrollEnrolling.this.mIndicatorBackgroundRestingColor});
                anim.addUpdateListener(listener);
                anim.setDuration(300);
                anim.setInterpolator(FingerprintEnrollEnrolling.this.mLinearOutSlowInInterpolator);
                anim.start();
            }
        });
        anim.setInterpolator(this.mFastOutSlowInInterpolator);
        anim.setDuration(300);
        anim.start();
    }

    private void launchFinish(byte[] token) {
        if ("factory".equals(this.mFactoryMode)) {
            Log.d("zhangwp", "mFactoryMode do not show FingerprintEnrollFinish");
            this.mFactoryMode = null;
            finish();
            return;
        }
        Intent intent = getFinishIntent();
        intent.addFlags(33554432);
        intent.putExtra("hw_auth_token", token);
        startActivity(intent);
        finish();
    }

    protected Intent getFinishIntent() {
        return new Intent(this, FingerprintEnrollFinish.class);
    }

    private void updateDescription() {
        if (this.mSidecar.getEnrollmentSteps() == -1) {
            setHeaderText(R.string.security_settings_fingerprint_enroll_start_title);
            this.mStartMessage.setVisibility(0);
            this.mRepeatMessage.setVisibility(4);
            return;
        }
        setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title, true);
        this.mStartMessage.setVisibility(4);
        this.mRepeatMessage.setVisibility(0);
    }

    public void onEnrollmentHelp(CharSequence helpString) {
        this.mErrorText.setText(helpString);
    }

    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        int msgId;
        switch (errMsgId) {
            case 3:
                msgId = R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message;
                break;
            default:
                msgId = R.string.security_settings_fingerprint_enroll_error_generic_dialog_message;
                break;
        }
        showErrorDialog(getText(msgId), errMsgId);
        stopIconAnimation();
        this.mErrorText.removeCallbacks(this.mTouchAgainRunnable);
    }

    public void onEnrollmentProgressChange(int steps, int remaining) {
        updateProgress(true);
        updateDescription();
        clearError();
        animateFlash();
        this.mErrorText.removeCallbacks(this.mTouchAgainRunnable);
        this.mErrorText.postDelayed(this.mTouchAgainRunnable, 2500);
    }

    private void updateProgress(boolean animate) {
        int progress = getProgress(this.mSidecar.getEnrollmentSteps(), this.mSidecar.getEnrollmentRemaining());
        if (animate) {
            animateProgress(progress);
        } else {
            this.mProgressBar.setProgress(progress);
        }
    }

    private int getProgress(int steps, int remaining) {
        if (steps == -1) {
            return 0;
        }
        return (Math.max(0, (steps + 1) - remaining) * 10000) / (steps + 1);
    }

    private void showErrorDialog(CharSequence msg, int msgId) {
        ErrorDialog.newInstance(msg, msgId).show(getFragmentManager(), ErrorDialog.class.getName());
    }

    private void showIconTouchDialog() {
        this.mIconTouchCount = 0;
        new IconTouchDialog().show(getFragmentManager(), null);
    }

    private void showError(CharSequence error) {
        this.mErrorText.setText(error);
        if (this.mErrorText.getVisibility() == 4) {
            this.mErrorText.setVisibility(0);
            this.mErrorText.setTranslationY((float) getResources().getDimensionPixelSize(R.dimen.fingerprint_error_text_appear_distance));
            this.mErrorText.setAlpha(0.0f);
            this.mErrorText.animate().alpha(1.0f).translationY(0.0f).setDuration(200).setInterpolator(this.mLinearOutSlowInInterpolator).start();
            return;
        }
        this.mErrorText.animate().cancel();
        this.mErrorText.setAlpha(1.0f);
        this.mErrorText.setTranslationY(0.0f);
    }

    private void clearError() {
        if (this.mErrorText.getVisibility() == 0) {
            this.mErrorText.animate().alpha(0.0f).translationY((float) getResources().getDimensionPixelSize(R.dimen.fingerprint_error_text_disappear_distance)).setDuration(100).setInterpolator(this.mFastOutLinearInInterpolator).withEndAction(new Runnable() {
                public void run() {
                    FingerprintEnrollEnrolling.this.mErrorText.setVisibility(4);
                }
            }).start();
        }
    }

    protected int getMetricsCategory() {
        return 240;
    }
}
