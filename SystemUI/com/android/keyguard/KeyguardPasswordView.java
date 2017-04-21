package com.android.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.widget.TextViewInputDisabler;
import java.util.List;

public class KeyguardPasswordView extends KeyguardAbsKeyInputView implements KeyguardSecurityView, OnEditorActionListener, TextWatcher {
    private final int mDisappearYTranslation;
    private Interpolator mFastOutLinearInInterpolator;
    InputMethodManager mImm;
    private Interpolator mLinearOutSlowInInterpolator;
    private TextView mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryDisabler;
    private final boolean mShowImeAtScreenOn;

    public KeyguardPasswordView(Context context) {
        this(context, null);
    }

    public KeyguardPasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowImeAtScreenOn = context.getResources().getBoolean(R$bool.kg_show_ime_at_screen_on);
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
    }

    protected void resetState() {
        this.mSecurityMessageDisplay.setMessage(R$string.kg_password_instructions, true);
        boolean wasDisabled = this.mPasswordEntry.isEnabled();
        setPasswordEntryEnabled(true);
        setPasswordEntryInputEnabled(true);
        if (wasDisabled) {
            this.mImm.showSoftInput(this.mPasswordEntry, 1);
        }
    }

    protected int getPasswordTextViewId() {
        return R$id.passwordEntry;
    }

    public boolean needsInput() {
        Log.d("KeyguardPasswordView", "needsInput() - returns true.");
        return true;
    }

    public void onResume(final int reason) {
        super.onResume(reason);
        post(new Runnable() {
            public void run() {
                if (KeyguardPasswordView.this.isShown() && KeyguardPasswordView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPasswordView.this.mPasswordEntry.requestFocus();
                    Log.d("KeyguardPasswordView", "reason = " + reason + ", mShowImeAtScreenOn = " + KeyguardPasswordView.this.mShowImeAtScreenOn);
                    if (reason != 1 || KeyguardPasswordView.this.mShowImeAtScreenOn) {
                        Log.d("KeyguardPasswordView", "onResume() - call showSoftInput()");
                        KeyguardPasswordView.this.mImm.showSoftInput(KeyguardPasswordView.this.mPasswordEntry, 1);
                    }
                }
            }
        });
    }

    protected int getPromtReasonStringRes(int reason) {
        switch (reason) {
            case 1:
                return R$string.kg_prompt_reason_restart_password;
            case 2:
                return R$string.kg_prompt_reason_timeout_password;
            default:
                return 0;
        }
    }

    public void onPause() {
        super.onPause();
        this.mImm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public void reset() {
        super.reset();
        this.mPasswordEntry.requestFocus();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        boolean imeOrDeleteButtonVisible = false;
        this.mImm = (InputMethodManager) getContext().getSystemService("input_method");
        this.mPasswordEntry = (TextView) findViewById(getPasswordTextViewId());
        this.mPasswordEntryDisabler = new TextViewInputDisabler(this.mPasswordEntry);
        this.mPasswordEntry.setKeyListener(TextKeyListener.getInstance());
        this.mPasswordEntry.setInputType(129);
        this.mPasswordEntry.setOnEditorActionListener(this);
        this.mPasswordEntry.addTextChangedListener(this);
        this.mPasswordEntry.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                KeyguardPasswordView.this.mCallback.userActivity();
            }
        });
        this.mPasswordEntry.setSelected(true);
        this.mPasswordEntry.requestFocus();
        View switchImeButton = findViewById(R$id.switch_ime_button);
        if (switchImeButton != null && hasMultipleEnabledIMEsOrSubtypes(this.mImm, false)) {
            switchImeButton.setVisibility(0);
            imeOrDeleteButtonVisible = true;
            switchImeButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    KeyguardPasswordView.this.mCallback.userActivity();
                    KeyguardPasswordView.this.mImm.showInputMethodPicker(false);
                }
            });
        }
        if (!imeOrDeleteButtonVisible) {
            LayoutParams params = this.mPasswordEntry.getLayoutParams();
            if (params instanceof MarginLayoutParams) {
                ((MarginLayoutParams) params).setMarginStart(0);
                this.mPasswordEntry.setLayoutParams(params);
            }
        }
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return this.mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    protected void resetPasswordText(boolean animate) {
        this.mPasswordEntry.setText("");
    }

    protected String getPasswordText() {
        return this.mPasswordEntry.getText().toString();
    }

    protected void setPasswordEntryEnabled(boolean enabled) {
        this.mPasswordEntry.setEnabled(enabled);
    }

    protected void setPasswordEntryInputEnabled(boolean enabled) {
        this.mPasswordEntryDisabler.setInputEnabled(enabled);
    }

    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager imm, boolean shouldIncludeAuxiliarySubtypes) {
        boolean z = true;
        int filteredImisCount = 0;
        for (InputMethodInfo imi : imm.getEnabledInputMethodList()) {
            if (filteredImisCount > 1) {
                return true;
            }
            List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
            if (subtypes.isEmpty()) {
                filteredImisCount++;
            } else {
                int auxCount = 0;
                for (InputMethodSubtype subtype : subtypes) {
                    if (subtype.isAuxiliary()) {
                        auxCount++;
                    }
                }
                if (subtypes.size() - auxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                    filteredImisCount++;
                }
            }
        }
        if (filteredImisCount <= 1 && imm.getEnabledInputMethodSubtypeList(null, false).size() <= 1) {
            z = false;
        }
        return z;
    }

    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_password;
    }

    public void startAppearAnimation() {
        setAlpha(0.0f);
        setTranslationY(0.0f);
        animate().alpha(1.0f).withLayer().setDuration(300).setInterpolator(this.mLinearOutSlowInInterpolator);
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        animate().alpha(0.0f).translationY((float) this.mDisappearYTranslation).setInterpolator(this.mFastOutLinearInInterpolator).setDuration(100).withEndAction(finishRunnable);
        return true;
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (this.mCallback != null) {
            this.mCallback.userActivity();
        }
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
        if (!TextUtils.isEmpty(s)) {
            onUserInput();
        }
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean isSoftImeEvent = event == null ? (actionId == 0 || actionId == 6) ? true : actionId == 5 : false;
        boolean isKeyboardEnterKey = (event == null || !KeyEvent.isConfirmKey(event.getKeyCode())) ? false : event.getAction() == 0;
        if (!isSoftImeEvent && !isKeyboardEnterKey) {
            return false;
        }
        verifyPasswordAndUnlock();
        return true;
    }
}
