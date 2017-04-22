package com.android.settings.fingerprint;

import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.CancellationSignal;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

public class FingerprintUiHelper extends AuthenticationCallback {
    private Callback mCallback;
    private CancellationSignal mCancellationSignal;
    private TextView mErrorTextView;
    private FingerprintManager mFingerprintManager;
    private ImageView mIcon;
    private Runnable mResetErrorTextRunnable = new C03891();

    public interface Callback {
        void onAuthenticated();

        void onFingerprintIconVisibilityChanged(boolean z);
    }

    class C03891 implements Runnable {
        C03891() {
        }

        public void run() {
            FingerprintUiHelper.this.mErrorTextView.setText("");
            FingerprintUiHelper.this.mIcon.setImageResource(R.drawable.ic_fingerprint);
        }
    }

    public FingerprintUiHelper(ImageView icon, TextView errorTextView, Callback callback) {
        this.mFingerprintManager = (FingerprintManager) icon.getContext().getSystemService(FingerprintManager.class);
        this.mIcon = icon;
        this.mErrorTextView = errorTextView;
        this.mCallback = callback;
    }

    public void startListening() {
        if (this.mFingerprintManager.getEnrolledFingerprints().size() > 0) {
            this.mCancellationSignal = new CancellationSignal();
            this.mFingerprintManager.authenticate(null, this.mCancellationSignal, 0, this, null);
            setFingerprintIconVisibility(true);
            this.mIcon.setImageResource(R.drawable.ic_fingerprint);
        }
    }

    public void stopListening() {
        if (this.mCancellationSignal != null) {
            this.mCancellationSignal.cancel();
            this.mCancellationSignal = null;
        }
    }

    private boolean isListening() {
        return (this.mCancellationSignal == null || this.mCancellationSignal.isCanceled()) ? false : true;
    }

    private void setFingerprintIconVisibility(boolean visible) {
        this.mIcon.setVisibility(visible ? 0 : 8);
        this.mCallback.onFingerprintIconVisibilityChanged(visible);
    }

    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        showError(errString);
        setFingerprintIconVisibility(false);
    }

    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
    }

    public void onAuthenticationFailed() {
        showError(this.mIcon.getResources().getString(R.string.fingerprint_not_recognized));
    }

    public void onAuthenticationSucceeded(AuthenticationResult result) {
        this.mIcon.setImageResource(R.drawable.ic_fingerprint_success);
        this.mCallback.onAuthenticated();
    }

    private void showError(CharSequence error) {
        if (isListening()) {
            this.mIcon.setImageResource(R.drawable.ic_fingerprint_error);
            this.mErrorTextView.setText(error);
            this.mErrorTextView.removeCallbacks(this.mResetErrorTextRunnable);
            this.mErrorTextView.postDelayed(this.mResetErrorTextRunnable, 1300);
        }
    }
}
