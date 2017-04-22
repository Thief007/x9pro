package com.android.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.fingerprint.FingerprintUiHelper;
import com.android.settings.fingerprint.FingerprintUiHelper.Callback;

public abstract class ConfirmDeviceCredentialBaseFragment extends InstrumentedFragment implements Callback {
    private boolean mAllowFpAuthentication;
    protected Button mCancelButton;
    private FingerprintUiHelper mFingerprintHelper;
    protected ImageView mFingerprintIcon;

    class C00621 implements OnClickListener {
        C00621() {
        }

        public void onClick(View v) {
            ConfirmDeviceCredentialBaseFragment.this.getActivity().finish();
        }
    }

    protected abstract void authenticationSucceeded();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAllowFpAuthentication = getActivity().getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.allowFpAuthentication", false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mCancelButton = (Button) view.findViewById(R.id.cancelButton);
        this.mFingerprintIcon = (ImageView) view.findViewById(R.id.fingerprintIcon);
        this.mFingerprintHelper = new FingerprintUiHelper(this.mFingerprintIcon, (TextView) view.findViewById(R.id.errorText), this);
        this.mCancelButton.setVisibility(getActivity().getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.showCancelButton", false) ? 0 : 8);
        this.mCancelButton.setOnClickListener(new C00621());
    }

    public void onResume() {
        super.onResume();
        if (this.mAllowFpAuthentication) {
            this.mFingerprintHelper.startListening();
        }
    }

    protected void setAccessibilityTitle(CharSequence suplementalText) {
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            CharSequence titleText = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.title");
            if (titleText != null && suplementalText != null) {
                getActivity().setTitle(Utils.createAccessibleSequence(titleText, "," + suplementalText));
            }
        }
    }

    public void onPause() {
        super.onPause();
        if (this.mAllowFpAuthentication) {
            this.mFingerprintHelper.stopListening();
        }
    }

    public void onAuthenticated() {
        if (getActivity() != null && getActivity().isResumed()) {
            authenticationSucceeded();
        }
    }

    public void onFingerprintIconVisibilityChanged(boolean visible) {
    }

    public void prepareEnterAnimation() {
    }

    public void startEnterAnimation() {
    }
}
