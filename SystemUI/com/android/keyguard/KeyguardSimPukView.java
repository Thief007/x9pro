package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.ITelephony.Stub;
import com.android.internal.telephony.IccCardConstants.State;

public class KeyguardSimPukView extends KeyguardPinBasedInputView {
    private CheckSimPuk mCheckSimPukThread;
    KeyguardUtils mKeyguardUtils;
    private int mPhoneId;
    private String mPinText;
    private String mPukText;
    private AlertDialog mRemainingAttemptsDialog;
    private ProgressDialog mSimUnlockProgressDialog;
    private StateMachine mStateMachine;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    private abstract class CheckSimPuk extends Thread {
        private final String mPin;
        private final String mPuk;

        abstract void onSimLockChangedResponse(int i, int i2);

        protected CheckSimPuk(String puk, String pin) {
            this.mPuk = puk;
            this.mPin = pin;
        }

        public void run() {
            try {
                Log.v("KeyguardSimPukView", "call supplyPukReportResultForSubscriber() mPhoneId = " + KeyguardSimPukView.this.mPhoneId);
                final int[] result = Stub.asInterface(ServiceManager.checkService("phone")).supplyPukReportResultForSubscriber(KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPukView.this.mPhoneId), this.mPuk, this.mPin);
                Log.v("KeyguardSimPukView", "supplyPukReportResultForSubscriber returned: " + result[0] + " " + result[1]);
                KeyguardSimPukView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(result[0], result[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e("KeyguardSimPukView", "RemoteException for supplyPukReportResult:", e);
                KeyguardSimPukView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(2, -1);
                    }
                });
            }
        }
    }

    private class StateMachine {
        final int CONFIRM_PIN;
        final int DONE;
        final int ENTER_PIN;
        final int ENTER_PUK;
        private int state;

        private StateMachine() {
            this.ENTER_PUK = 0;
            this.ENTER_PIN = 1;
            this.CONFIRM_PIN = 2;
            this.DONE = 3;
            this.state = 0;
        }

        public void next() {
            int msg = 0;
            if (this.state == 0) {
                if (KeyguardSimPukView.this.checkPuk()) {
                    this.state = 1;
                    msg = R$string.kg_puk_enter_pin_hint;
                } else {
                    msg = R$string.kg_invalid_sim_puk_hint;
                }
            } else if (this.state == 1) {
                if (KeyguardSimPukView.this.checkPin()) {
                    this.state = 2;
                    msg = R$string.kg_enter_confirm_pin_hint;
                } else {
                    msg = R$string.kg_invalid_sim_pin_hint;
                }
            } else if (this.state == 2) {
                if (KeyguardSimPukView.this.confirmPin()) {
                    this.state = 3;
                    msg = R$string.keyguard_sim_unlock_progress_dialog_message;
                    KeyguardSimPukView.this.updateSim();
                } else {
                    this.state = 1;
                    msg = R$string.kg_invalid_confirm_pin_hint;
                }
            }
            KeyguardSimPukView.this.resetPasswordText(true);
            if (msg != 0) {
                KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage(msg, true);
            }
        }

        void reset() {
            KeyguardSimPukView.this.mPinText = "";
            KeyguardSimPukView.this.mPukText = "";
            this.state = 0;
            KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage(R$string.kg_puk_enter_puk_hint, true);
            KeyguardSimPukView.this.mPasswordEntry.requestFocus();
        }
    }

    protected int getPromtReasonStringRes(int reason) {
        return 0;
    }

    private String getPukPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;
        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R$string.kg_password_wrong_puk_code_dead);
        } else if (attemptsRemaining > 0) {
            displayMessage = getContext().getResources().getQuantityString(R$plurals.kg_password_wrong_puk_code, attemptsRemaining, new Object[]{Integer.valueOf(attemptsRemaining)});
        } else {
            displayMessage = getContext().getString(R$string.kg_password_puk_failed);
        }
        Log.d("KeyguardSimPukView", "getPukPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    public KeyguardSimPukView(Context context) {
        this(context, null);
    }

    public KeyguardSimPukView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
        this.mStateMachine = new StateMachine();
        this.mPhoneId = 0;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            private static /* synthetic */ int[] -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;

            private static /* synthetic */ int[] -getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues() {
                if (-com_android_internal_telephony_IccCardConstants$StateSwitchesValues != null) {
                    return -com_android_internal_telephony_IccCardConstants$StateSwitchesValues;
                }
                int[] iArr = new int[State.values().length];
                try {
                    iArr[State.ABSENT.ordinal()] = 1;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[State.CARD_IO_ERROR.ordinal()] = 3;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[State.NETWORK_LOCKED.ordinal()] = 4;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[State.NOT_READY.ordinal()] = 2;
                } catch (NoSuchFieldError e4) {
                }
                try {
                    iArr[State.PERM_DISABLED.ordinal()] = 5;
                } catch (NoSuchFieldError e5) {
                }
                try {
                    iArr[State.PIN_REQUIRED.ordinal()] = 6;
                } catch (NoSuchFieldError e6) {
                }
                try {
                    iArr[State.PUK_REQUIRED.ordinal()] = 7;
                } catch (NoSuchFieldError e7) {
                }
                try {
                    iArr[State.READY.ordinal()] = 8;
                } catch (NoSuchFieldError e8) {
                }
                try {
                    iArr[State.UNKNOWN.ordinal()] = 9;
                } catch (NoSuchFieldError e9) {
                }
                -com_android_internal_telephony_IccCardConstants$StateSwitchesValues = iArr;
                return iArr;
            }

            public void onSimStateChangedUsingPhoneId(int phoneId, State simState) {
                Log.d("KeyguardSimPukView", "onSimStateChangedUsingPhoneId: " + simState + ", phoneId=" + phoneId);
                switch (AnonymousClass1.-getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues()[simState.ordinal()]) {
                    case 1:
                    case 2:
                        if (phoneId == KeyguardSimPukView.this.mPhoneId) {
                            KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.getContext()).reportSimUnlocked(KeyguardSimPukView.this.mPhoneId);
                            KeyguardSimPukView.this.mCallback.dismiss(true);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mKeyguardUtils = new KeyguardUtils(context);
    }

    public void resetState() {
        super.resetState();
        this.mStateMachine.reset();
    }

    protected boolean shouldLockout(long deadline) {
        return false;
    }

    protected int getPasswordTextViewId() {
        return R$id.pukEntry;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhoneId = KeyguardUpdateMonitor.getInstance(getContext()).getSimPukLockPhoneId();
        if (KeyguardUtils.getNumOfPhone() > 1) {
            View simIcon = findViewById(R$id.keyguard_sim);
            if (simIcon != null) {
                simIcon.setVisibility(8);
            }
            View simInfoMsg = findViewById(R$id.sim_info_message);
            if (simInfoMsg != null) {
                simInfoMsg.setVisibility(0);
            }
            dealwithSIMInfoChanged();
        }
        this.mSecurityMessageDisplay.setTimeout(0);
        if (this.mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) this.mEcaView).setCarrierTextVisible(true);
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallback);
    }

    public void onPause() {
        if (this.mSimUnlockProgressDialog != null) {
            this.mSimUnlockProgressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (this.mSimUnlockProgressDialog == null) {
            this.mSimUnlockProgressDialog = new ProgressDialog(this.mContext);
            this.mSimUnlockProgressDialog.setMessage(this.mContext.getString(R$string.kg_sim_unlock_progress_dialog_message));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            if (!(this.mContext instanceof Activity)) {
                this.mSimUnlockProgressDialog.getWindow().setType(2009);
            }
        }
        return this.mSimUnlockProgressDialog;
    }

    private Dialog getPukRemainingAttemptsDialog(int remaining) {
        String msg = getPukPasswordErrorMessage(remaining);
        if (this.mRemainingAttemptsDialog == null) {
            Builder builder = new Builder(this.mContext);
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setNeutralButton(R$string.ok, null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            this.mRemainingAttemptsDialog.setMessage(msg);
        }
        return this.mRemainingAttemptsDialog;
    }

    private boolean checkPuk() {
        if (this.mPasswordEntry.getText().length() != 8) {
            return false;
        }
        this.mPukText = this.mPasswordEntry.getText();
        return true;
    }

    private boolean checkPin() {
        int length = this.mPasswordEntry.getText().length();
        if (length < 4 || length > 8) {
            return false;
        }
        this.mPinText = this.mPasswordEntry.getText();
        return true;
    }

    public boolean confirmPin() {
        return this.mPinText.equals(this.mPasswordEntry.getText());
    }

    private void updateSim() {
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPukThread == null) {
            this.mCheckSimPukThread = new CheckSimPuk(this, this.mPukText, this.mPinText) {
                void onSimLockChangedResponse(final int result, final int attemptsRemaining) {
                    this.post(new Runnable() {
                        public void run() {
                            if (this.mSimUnlockProgressDialog != null) {
                                this.mSimUnlockProgressDialog.hide();
                            }
                            if (result == 0) {
                                KeyguardUpdateMonitor.getInstance(this.getContext()).reportSimUnlocked(this.mPhoneId);
                                this.mCallback.dismiss(true);
                            } else {
                                if (result != 1) {
                                    this.mSecurityMessageDisplay.setMessage(this.getContext().getString(R$string.kg_password_puk_failed), true);
                                } else if (attemptsRemaining <= 2) {
                                    this.getPukRemainingAttemptsDialog(attemptsRemaining).show();
                                } else {
                                    this.mSecurityMessageDisplay.setMessage(this.getPukPasswordErrorMessage(attemptsRemaining), true);
                                }
                                Log.d("KeyguardSimPukView", "verifyPasswordAndUnlock  UpdateSim.onSimCheckResponse:  attemptsRemaining=" + attemptsRemaining);
                                this.mStateMachine.reset();
                            }
                            this.mCheckSimPukThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPukThread.start();
        }
    }

    protected void verifyPasswordAndUnlock() {
        this.mStateMachine.next();
    }

    public void startAppearAnimation() {
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    private void dealwithSIMInfoChanged() {
        CharSequence operName = null;
        try {
            operName = this.mKeyguardUtils.getOptrNameUsingPhoneId(this.mPhoneId, this.mContext);
        } catch (IndexOutOfBoundsException e) {
            Log.w("KeyguardSimPukView", "getOptrNameBySlot exception, mPhoneId=" + this.mPhoneId);
        }
        Log.i("KeyguardSimPukView", "dealwithSIMInfoChanged, mPhoneId=" + this.mPhoneId + ", operName=" + operName);
        TextView forText = (TextView) findViewById(R$id.for_text);
        ImageView subIcon = (ImageView) findViewById(R$id.sub_icon);
        TextView simCardName = (TextView) findViewById(R$id.sim_card_name);
        if (operName == null) {
            Log.d("KeyguardSimPukView", "mPhoneId " + this.mPhoneId + " is new subInfo record");
            setForTextNewCard(this.mPhoneId, forText);
            subIcon.setVisibility(8);
            simCardName.setVisibility(8);
            return;
        }
        Log.d("KeyguardSimPukView", "dealwithSIMInfoChanged, show operName for mPhoneId=" + this.mPhoneId);
        forText.setText(this.mContext.getString(R$string.kg_slot_id, new Object[]{Integer.valueOf(this.mPhoneId + 1)}) + " ");
        if (operName == null) {
            operName = this.mContext.getString(R$string.kg_detecting_simcard);
        }
        simCardName.setText(operName);
        subIcon.setImageBitmap(this.mKeyguardUtils.getOptrBitmapUsingPhoneId(this.mPhoneId, this.mContext));
        subIcon.setVisibility(0);
        simCardName.setVisibility(0);
    }

    private void setForTextNewCard(int phoneId, TextView forText) {
        StringBuffer forSb = new StringBuffer();
        forSb.append(this.mContext.getString(R$string.kg_slot_id, new Object[]{Integer.valueOf(phoneId + 1)}));
        forSb.append(" ");
        forSb.append(this.mContext.getText(R$string.kg_new_simcard));
        forText.setText(forSb.toString());
    }
}
