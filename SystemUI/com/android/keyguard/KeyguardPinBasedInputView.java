package com.android.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import com.android.keyguard.PasswordTextView.UserActivityListener;

public abstract class KeyguardPinBasedInputView extends KeyguardAbsKeyInputView implements OnKeyListener {
    private View mButton0;
    private View mButton1;
    private View mButton2;
    private View mButton3;
    private View mButton4;
    private View mButton5;
    private View mButton6;
    private View mButton7;
    private View mButton8;
    private View mButton9;
    private View mDeleteButton;
    private View mOkButton;
    protected PasswordTextView mPasswordEntry;

    public KeyguardPinBasedInputView(Context context) {
        this(context, null);
    }

    public KeyguardPinBasedInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void reset() {
        this.mPasswordEntry.requestFocus();
        super.reset();
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return this.mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    protected void resetState() {
        this.mPasswordEntry.setEnabled(true);
    }

    protected void setPasswordEntryEnabled(boolean enabled) {
        this.mPasswordEntry.setEnabled(enabled);
    }

    protected void setPasswordEntryInputEnabled(boolean enabled) {
        this.mPasswordEntry.setEnabled(enabled);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.isConfirmKey(keyCode)) {
            performClick(this.mOkButton);
            return true;
        } else if (keyCode == 67) {
            performClick(this.mDeleteButton);
            return true;
        } else if (keyCode < 7 || keyCode > 16) {
            return super.onKeyDown(keyCode, event);
        } else {
            performNumberClick(keyCode - 7);
            return true;
        }
    }

    protected int getPromtReasonStringRes(int reason) {
        switch (reason) {
            case 1:
                return R$string.kg_prompt_reason_restart_pin;
            case 2:
                return R$string.kg_prompt_reason_timeout_pin;
            default:
                return 0;
        }
    }

    private void performClick(View view) {
        view.performClick();
    }

    private void performNumberClick(int number) {
        switch (number) {
            case 0:
                performClick(this.mButton0);
                return;
            case 1:
                performClick(this.mButton1);
                return;
            case 2:
                performClick(this.mButton2);
                return;
            case 3:
                performClick(this.mButton3);
                return;
            case 4:
                performClick(this.mButton4);
                return;
            case 5:
                performClick(this.mButton5);
                return;
            case 6:
                performClick(this.mButton6);
                return;
            case 7:
                performClick(this.mButton7);
                return;
            case 8:
                performClick(this.mButton8);
                return;
            case 9:
                performClick(this.mButton9);
                return;
            default:
                return;
        }
    }

    protected void resetPasswordText(boolean animate) {
        this.mPasswordEntry.reset(animate);
    }

    protected String getPasswordText() {
        return this.mPasswordEntry.getText();
    }

    protected void onFinishInflate() {
        this.mPasswordEntry = (PasswordTextView) findViewById(getPasswordTextViewId());
        this.mPasswordEntry.setOnKeyListener(this);
        this.mPasswordEntry.setSelected(true);
        this.mPasswordEntry.setUserActivityListener(new UserActivityListener() {
            public void onUserActivity() {
                KeyguardPinBasedInputView.this.onUserInput();
            }
        });
        this.mOkButton = findViewById(R$id.key_enter);
        if (this.mOkButton != null) {
            this.mOkButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    KeyguardPinBasedInputView.this.doHapticKeyClick();
                    if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                        Log.d("KeyguardPinBasedInputView", "mOkButton.onClick() is called, set PwEntry false.");
                        KeyguardPinBasedInputView.this.setPasswordEntryEnabled(false);
                        KeyguardPinBasedInputView.this.verifyPasswordAndUnlock();
                    }
                }
            });
            this.mOkButton.setOnHoverListener(new LiftToActivateListener(getContext()));
        }
        this.mDeleteButton = findViewById(R$id.delete_button);
        this.mDeleteButton.setVisibility(0);
        this.mDeleteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPinBasedInputView.this.mPasswordEntry.deleteLastChar();
                }
                KeyguardPinBasedInputView.this.doHapticKeyClick();
            }
        });
        this.mDeleteButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPinBasedInputView.this.resetPasswordText(true);
                }
                KeyguardPinBasedInputView.this.doHapticKeyClick();
                return true;
            }
        });
        this.mButton0 = findViewById(R$id.key0);
        this.mButton1 = findViewById(R$id.key1);
        this.mButton2 = findViewById(R$id.key2);
        this.mButton3 = findViewById(R$id.key3);
        this.mButton4 = findViewById(R$id.key4);
        this.mButton5 = findViewById(R$id.key5);
        this.mButton6 = findViewById(R$id.key6);
        this.mButton7 = findViewById(R$id.key7);
        this.mButton8 = findViewById(R$id.key8);
        this.mButton9 = findViewById(R$id.key9);
        this.mPasswordEntry.requestFocus();
        super.onFinishInflate();
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != 0) {
            return false;
        }
        onKeyDown(keyCode, event);
        return true;
    }
}
