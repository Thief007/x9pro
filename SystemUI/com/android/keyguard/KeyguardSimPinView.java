package com.android.keyguard;

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

public class KeyguardSimPinView extends KeyguardPinBasedInputView {
    private CheckSimPin mCheckSimPinThread;
    KeyguardUtils mKeyguardUtils;
    private int mPhoneId;
    private AlertDialog mRemainingAttemptsDialog;
    private ProgressDialog mSimUnlockProgressDialog;
    private int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    private abstract class CheckSimPin extends Thread {
        private final String mPin;

        abstract void onSimCheckResponse(int i, int i2);

        protected CheckSimPin(String pin) {
            this.mPin = pin;
        }

        public void run() {
            try {
                Log.v("KeyguardSimPinView", "call supplyPinReportResultForSubscriber(subid=" + KeyguardSimPinView.this.mSubId + ")");
                Log.d("KeyguardSimPinView", "call supplyPinReportResultForSubscriber() mPhoneId = " + KeyguardSimPinView.this.mPhoneId);
                int subId = KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinView.this.mPhoneId);
                final int[] result = Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(KeyguardSimPinView.this.mSubId, this.mPin);
                Log.v("KeyguardSimPinView", "supplyPinReportResult returned: " + result[0] + " " + result[1]);
                KeyguardSimPinView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(result[0], result[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e("KeyguardSimPinView", "RemoteException for supplyPinReportResult:", e);
                KeyguardSimPinView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(2, -1);
                    }
                });
            }
        }
    }

    public KeyguardSimPinView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
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
                Log.d("KeyguardSimPinView", "onSimStateChangedUsingSubId: " + simState + ", phoneId=" + phoneId);
                switch (AnonymousClass1.-getcom_android_internal_telephony_IccCardConstants$StateSwitchesValues()[simState.ordinal()]) {
                    case 1:
                    case 2:
                        if (phoneId == KeyguardSimPinView.this.mPhoneId) {
                            KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(KeyguardSimPinView.this.mPhoneId);
                            KeyguardSimPinView.this.mCallback.dismiss(true);
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
        Log.v("KeyguardSimPinView", "Resetting state");
        this.mSecurityMessageDisplay.setMessage(R$string.kg_sim_pin_instructions, true);
    }

    protected int getPromtReasonStringRes(int reason) {
        return 0;
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;
        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R$string.kg_password_wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = getContext().getResources().getQuantityString(R$plurals.kg_password_wrong_pin_code, attemptsRemaining, new Object[]{Integer.valueOf(attemptsRemaining)});
        } else {
            displayMessage = getContext().getString(R$string.kg_password_pin_failed);
        }
        Log.d("KeyguardSimPinView", "getPinPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    protected boolean shouldLockout(long deadline) {
        return false;
    }

    protected int getPasswordTextViewId() {
        return R$id.simPinEntry;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhoneId = KeyguardUpdateMonitor.getInstance(getContext()).getSimPinLockPhoneId();
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
            this.mSimUnlockProgressDialog.getWindow().setType(2009);
        }
        return this.mSimUnlockProgressDialog;
    }

    private Dialog getSimRemainingAttemptsDialog(int remaining) {
        String msg = getPinPasswordErrorMessage(remaining);
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

    protected void verifyPasswordAndUnlock() {
        if (this.mPasswordEntry.getText().length() < 4) {
            this.mSecurityMessageDisplay.setMessage(R$string.kg_invalid_sim_pin_hint, true);
            resetPasswordText(true);
            this.mCallback.userActivity();
            return;
        }
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPinThread == null) {
            this.mCheckSimPinThread = new CheckSimPin(this, this.mPasswordEntry.getText()) {
                void onSimCheckResponse(final int result, final int attemptsRemaining) {
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
                                    this.mSecurityMessageDisplay.setMessage(this.getContext().getString(R$string.kg_password_pin_failed), true);
                                } else if (attemptsRemaining <= 2) {
                                    this.getSimRemainingAttemptsDialog(attemptsRemaining).show();
                                } else {
                                    this.mSecurityMessageDisplay.setMessage(this.getPinPasswordErrorMessage(attemptsRemaining), true);
                                }
                                Log.d("KeyguardSimPinView", "verifyPasswordAndUnlock  CheckSimPin.onSimCheckResponse: " + result + " attemptsRemaining=" + attemptsRemaining);
                                this.resetPasswordText(true);
                            }
                            this.mCallback.userActivity();
                            this.mCheckSimPinThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPinThread.start();
        }
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
            Log.w("KeyguardSimPinView", "getOptrNameBySlot exception, mPhoneId=" + this.mPhoneId);
        }
        Log.i("KeyguardSimPinView", "dealwithSIMInfoChanged, mPhoneId=" + this.mPhoneId + ", operName=" + operName);
        TextView forText = (TextView) findViewById(R$id.for_text);
        ImageView subIcon = (ImageView) findViewById(R$id.sub_icon);
        TextView simCardName = (TextView) findViewById(R$id.sim_card_name);
        if (operName == null) {
            Log.d("KeyguardSimPinView", "mPhoneId " + this.mPhoneId + " is new subInfo record");
            setForTextNewCard(this.mPhoneId, forText);
            subIcon.setVisibility(8);
            simCardName.setVisibility(8);
            return;
        }
        Log.d("KeyguardSimPinView", "dealwithSIMInfoChanged, show operName for mPhoneId=" + this.mPhoneId);
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
