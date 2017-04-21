package com.android.systemui.settings;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.R$styleable;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;

public class ToggleSlider extends RelativeLayout {
    private final OnCheckedChangeListener mCheckListener;
    private TextView mLabel;
    private Listener mListener;
    private ToggleSlider mMirror;
    private BrightnessMirrorController mMirrorController;
    private final OnSeekBarChangeListener mSeekListener;
    private ToggleSeekBar mSlider;
    private CompoundButton mToggle;
    private boolean mTracking;

    public interface Listener {
        void onChanged(ToggleSlider toggleSlider, boolean z, boolean z2, int i, boolean z3);

        void onInit(ToggleSlider toggleSlider);
    }

    public ToggleSlider(Context context) {
        this(context, null);
    }

    public ToggleSlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToggleSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCheckListener = new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton toggle, boolean checked) {
                ToggleSlider.this.mSlider.setEnabled(!checked);
                if (ToggleSlider.this.mListener != null) {
                    ToggleSlider.this.mListener.onChanged(ToggleSlider.this, ToggleSlider.this.mTracking, checked, ToggleSlider.this.mSlider.getProgress(), false);
                }
                if (ToggleSlider.this.mMirror != null) {
                    ToggleSlider.this.mMirror.mToggle.setChecked(checked);
                }
            }
        };
        this.mSeekListener = new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (ToggleSlider.this.mListener != null) {
                    ToggleSlider.this.mListener.onChanged(ToggleSlider.this, ToggleSlider.this.mTracking, ToggleSlider.this.mToggle.isChecked(), progress, false);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                ToggleSlider.this.mTracking = true;
                if (ToggleSlider.this.mListener != null) {
                    ToggleSlider.this.mListener.onChanged(ToggleSlider.this, ToggleSlider.this.mTracking, ToggleSlider.this.mToggle.isChecked(), ToggleSlider.this.mSlider.getProgress(), false);
                }
                ToggleSlider.this.mToggle.setChecked(false);
                if (ToggleSlider.this.mMirrorController != null) {
                    ToggleSlider.this.mMirrorController.showMirror();
                    ToggleSlider.this.mMirrorController.setLocation((View) ToggleSlider.this.getParent());
                }
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                ToggleSlider.this.mTracking = false;
                if (ToggleSlider.this.mListener != null) {
                    ToggleSlider.this.mListener.onChanged(ToggleSlider.this, ToggleSlider.this.mTracking, ToggleSlider.this.mToggle.isChecked(), ToggleSlider.this.mSlider.getProgress(), true);
                }
                if (ToggleSlider.this.mMirrorController != null) {
                    ToggleSlider.this.mMirrorController.hideMirror();
                }
            }
        };
        View.inflate(context, R.layout.status_bar_toggle_slider, this);
        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.ToggleSlider, defStyle, 0);
        this.mToggle = (CompoundButton) findViewById(R.id.toggle);
        this.mToggle.setOnCheckedChangeListener(this.mCheckListener);
        this.mSlider = (ToggleSeekBar) findViewById(R.id.slider);
        this.mSlider.setOnSeekBarChangeListener(this.mSeekListener);
        this.mLabel = (TextView) findViewById(R.id.label);
        this.mLabel.setText(a.getString(0));
        this.mSlider.setAccessibilityLabel(getContentDescription().toString());
        a.recycle();
    }

    public void setMirror(ToggleSlider toggleSlider) {
        this.mMirror = toggleSlider;
        if (this.mMirror != null) {
            this.mMirror.setChecked(this.mToggle.isChecked());
            this.mMirror.setMax(this.mSlider.getMax());
            this.mMirror.setValue(this.mSlider.getProgress());
        }
    }

    public void setMirrorController(BrightnessMirrorController c) {
        this.mMirrorController = c;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mListener != null) {
            this.mListener.onInit(this);
        }
    }

    public void setOnChangedListener(Listener l) {
        this.mListener = l;
    }

    public void setChecked(boolean checked) {
        this.mToggle.setChecked(checked);
    }

    public void setMax(int max) {
        this.mSlider.setMax(max);
        if (this.mMirror != null) {
            this.mMirror.setMax(max);
        }
    }

    public void setValue(int value) {
        this.mSlider.setProgress(value);
        if (this.mMirror != null) {
            this.mMirror.setValue(value);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mMirror != null) {
            MotionEvent copy = ev.copy();
            this.mMirror.dispatchTouchEvent(copy);
            copy.recycle();
        }
        return super.dispatchTouchEvent(ev);
    }
}
