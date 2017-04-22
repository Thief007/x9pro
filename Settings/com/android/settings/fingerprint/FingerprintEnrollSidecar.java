package com.android.settings.fingerprint;

import android.app.Activity;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import com.android.settings.InstrumentedFragment;

public class FingerprintEnrollSidecar extends InstrumentedFragment {
    private boolean mDone;
    private boolean mEnrolling;
    private EnrollmentCallback mEnrollmentCallback = new C03721();
    private CancellationSignal mEnrollmentCancel;
    private int mEnrollmentRemaining = 0;
    private int mEnrollmentSteps = -1;
    private Handler mHandler = new Handler();
    private Listener mListener;
    private final Runnable mTimeoutRunnable = new C03732();
    private byte[] mToken;

    public interface Listener {
        void onEnrollmentError(int i, CharSequence charSequence);

        void onEnrollmentHelp(CharSequence charSequence);

        void onEnrollmentProgressChange(int i, int i2);
    }

    class C03721 extends EnrollmentCallback {
        C03721() {
        }

        public void onEnrollmentProgress(int remaining) {
            boolean z = false;
            if (FingerprintEnrollSidecar.this.mEnrollmentSteps == -1) {
                FingerprintEnrollSidecar.this.mEnrollmentSteps = remaining;
            }
            FingerprintEnrollSidecar.this.mEnrollmentRemaining = remaining;
            FingerprintEnrollSidecar fingerprintEnrollSidecar = FingerprintEnrollSidecar.this;
            if (remaining == 0) {
                z = true;
            }
            fingerprintEnrollSidecar.mDone = z;
            if (FingerprintEnrollSidecar.this.mListener != null) {
                FingerprintEnrollSidecar.this.mListener.onEnrollmentProgressChange(FingerprintEnrollSidecar.this.mEnrollmentSteps, remaining);
            }
        }

        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            if (FingerprintEnrollSidecar.this.mListener != null) {
                FingerprintEnrollSidecar.this.mListener.onEnrollmentHelp(helpString);
            }
        }

        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            if (FingerprintEnrollSidecar.this.mListener != null) {
                FingerprintEnrollSidecar.this.mListener.onEnrollmentError(errMsgId, errString);
            }
        }
    }

    class C03732 implements Runnable {
        C03732() {
        }

        public void run() {
            FingerprintEnrollSidecar.this.cancelEnrollment();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mToken = activity.getIntent().getByteArrayExtra("hw_auth_token");
    }

    public void onStart() {
        super.onStart();
        if (!this.mEnrolling) {
            startEnrollment();
        }
    }

    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations()) {
            cancelEnrollment();
        }
    }

    private void startEnrollment() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        this.mEnrollmentSteps = -1;
        this.mEnrollmentCancel = new CancellationSignal();
        ((FingerprintManager) getActivity().getSystemService(FingerprintManager.class)).enroll(this.mToken, this.mEnrollmentCancel, 0, this.mEnrollmentCallback);
        this.mEnrolling = true;
    }

    private void cancelEnrollment() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        if (this.mEnrolling) {
            this.mEnrollmentCancel.cancel();
            this.mEnrolling = false;
            this.mEnrollmentSteps = -1;
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public int getEnrollmentSteps() {
        return this.mEnrollmentSteps;
    }

    public int getEnrollmentRemaining() {
        return this.mEnrollmentRemaining;
    }

    protected int getMetricsCategory() {
        return 245;
    }
}
