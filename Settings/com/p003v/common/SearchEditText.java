package com.p003v.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import com.android.settings.R;

public class SearchEditText extends EditText implements OnFocusChangeListener, TextWatcher {
    private Drawable mClearDrawable;

    public SearchEditText(Context context) {
        this(context, null);
    }

    public SearchEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 16842862);
    }

    public SearchEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mClearDrawable = null;
        init();
    }

    private void init() {
        this.mClearDrawable = getCompoundDrawables()[2];
        if (this.mClearDrawable == null) {
            this.mClearDrawable = getResources().getDrawable(R.drawable.v_search_clear_btn);
        }
        this.mClearDrawable.setBounds(0, 0, this.mClearDrawable.getIntrinsicWidth(), this.mClearDrawable.getIntrinsicHeight());
        setClearIconVisible(false);
        setOnFocusChangeListener(this);
        addTextChangedListener(this);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (getCompoundDrawables()[2] != null && event.getAction() == 1) {
            boolean touchable = event.getX() > ((float) ((getWidth() - getPaddingRight()) - this.mClearDrawable.getIntrinsicWidth())) ? event.getX() < ((float) (getWidth() - getPaddingRight())) : false;
            if (touchable) {
                setText("");
            }
        }
        return super.onTouchEvent(event);
    }

    public void onFocusChange(View v, boolean hasFocus) {
        boolean z = false;
        if (hasFocus) {
            if (getText().length() > 0) {
                z = true;
            }
            setClearIconVisible(z);
            return;
        }
        setClearIconVisible(false);
    }

    protected void setClearIconVisible(boolean visible) {
        setCompoundDrawables(getCompoundDrawables()[0], getCompoundDrawables()[1], visible ? this.mClearDrawable : null, getCompoundDrawables()[3]);
    }

    public void onTextChanged(CharSequence s, int start, int count, int after) {
        boolean z = false;
        if (s.length() > 0) {
            z = true;
        }
        setClearIconVisible(z);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable s) {
    }
}
