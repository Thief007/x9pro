package com.android.systemui.statusbar.policy;

import android.app.Notification.Action;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.systemui.R;

public class RemoteInputView extends FrameLayout implements OnClickListener {
    private Action mAction;
    private RemoteEditText mEditText;
    private PendingIntent mPendingIntent;
    private ProgressBar mProgressBar;
    private RemoteInput mRemoteInput;

    public static class RemoteEditText extends EditText {
        private final Drawable mBackground = getBackground();

        public RemoteEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        private void defocusIfNeeded() {
            if (isFocusable() && isEnabled()) {
                setInnerFocusable(false);
            }
        }

        protected void onVisibilityChanged(View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            if (!isShown()) {
                defocusIfNeeded();
            }
        }

        protected void onFocusLost() {
            super.onFocusLost();
            defocusIfNeeded();
        }

        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == 4) {
                defocusIfNeeded();
            }
            return super.onKeyPreIme(keyCode, event);
        }

        void setInnerFocusable(boolean focusable) {
            setFocusableInTouchMode(focusable);
            setFocusable(focusable);
            setCursorVisible(focusable);
            if (focusable) {
                requestFocus();
                setBackground(this.mBackground);
                return;
            }
            setBackground(null);
        }
    }

    public RemoteInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mProgressBar = (ProgressBar) findViewById(R.id.remote_input_progress);
        this.mEditText = (RemoteEditText) getChildAt(0);
        this.mEditText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean isSoftImeEvent = event == null ? (actionId == 6 || actionId == 5) ? true : actionId == 4 : false;
                boolean isKeyboardEnterKey = (event == null || !KeyEvent.isConfirmKey(event.getKeyCode())) ? false : event.getAction() == 0;
                if (!isSoftImeEvent && !isKeyboardEnterKey) {
                    return false;
                }
                RemoteInputView.this.sendRemoteInput();
                return true;
            }
        });
        this.mEditText.setOnClickListener(this);
        this.mEditText.setInnerFocusable(false);
    }

    private void sendRemoteInput() {
        Bundle results = new Bundle();
        results.putString(this.mRemoteInput.getResultKey(), this.mEditText.getText().toString());
        Intent fillInIntent = new Intent();
        RemoteInput.addResultsToIntent(this.mAction.getRemoteInputs(), fillInIntent, results);
        this.mEditText.setEnabled(false);
        this.mProgressBar.setVisibility(0);
        try {
            this.mPendingIntent.send(this.mContext, 0, fillInIntent);
        } catch (CanceledException e) {
            Log.i("RemoteInput", "Unable to send remote input result", e);
        }
    }

    public void onClick(View v) {
        if (v == this.mEditText && !this.mEditText.isFocusable()) {
            this.mEditText.setInnerFocusable(true);
            InputMethodManager imm = InputMethodManager.getInstance();
            if (imm != null) {
                imm.viewClicked(this.mEditText);
                imm.showSoftInput(this.mEditText, 0);
            }
        }
    }
}
