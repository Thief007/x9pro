package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;
import java.util.ArrayList;

public class DebugOverlayView extends FrameLayout implements OnSeekBarChangeListener {
    DebugOverlayViewCallbacks mCb;
    RecentsConfiguration mConfig;
    Paint mDebugOutline;
    boolean mEnabled;
    SeekBar mPrimarySeekBar;
    ArrayList<Pair<Rect, Integer>> mRects;
    SeekBar mSecondarySeekBar;
    String mText;
    Paint mTmpPaint;
    Rect mTmpRect;

    public interface DebugOverlayViewCallbacks {
        void onPrimarySeekBarChanged(float f);

        void onSecondarySeekBarChanged(float f);
    }

    public DebugOverlayView(Context context) {
        this(context, null);
    }

    public DebugOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DebugOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DebugOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mRects = new ArrayList();
        this.mDebugOutline = new Paint();
        this.mTmpPaint = new Paint();
        this.mTmpRect = new Rect();
        this.mEnabled = true;
        this.mConfig = RecentsConfiguration.getInstance();
        this.mDebugOutline.setColor(-65536);
        this.mDebugOutline.setStyle(Style.STROKE);
        this.mDebugOutline.setStrokeWidth(8.0f);
        setWillNotDraw(false);
    }

    public void setCallbacks(DebugOverlayViewCallbacks cb) {
        this.mCb = cb;
    }

    protected void onFinishInflate() {
        this.mPrimarySeekBar = (SeekBar) findViewById(R.id.debug_seek_bar_1);
        this.mPrimarySeekBar.setOnSeekBarChangeListener(this);
        this.mSecondarySeekBar = (SeekBar) findViewById(R.id.debug_seek_bar_2);
        this.mSecondarySeekBar.setOnSeekBarChangeListener(this);
    }

    public void enable() {
        this.mEnabled = true;
        setVisibility(0);
    }

    public void disable() {
        this.mEnabled = false;
        setVisibility(8);
    }

    public void clear() {
        this.mRects.clear();
    }

    void addRect(Rect r, int color) {
        this.mRects.add(new Pair(r, Integer.valueOf(color)));
        invalidate();
    }

    void setText(String message) {
        this.mText = message;
        invalidate();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        addRect(new Rect(0, 0, 50, 50), -65536);
        addRect(new Rect(getMeasuredWidth() - 50, getMeasuredHeight() - 50, getMeasuredWidth(), getMeasuredHeight()), -65536);
    }

    protected void onDraw(Canvas canvas) {
        if (this.mEnabled) {
            canvas.drawRect(0.0f, 0.0f, (float) getMeasuredWidth(), (float) getMeasuredHeight(), this.mDebugOutline);
            int numRects = this.mRects.size();
            for (int i = 0; i < numRects; i++) {
                Pair<Rect, Integer> r = (Pair) this.mRects.get(i);
                this.mTmpPaint.setColor(((Integer) r.second).intValue());
                canvas.drawRect((Rect) r.first, this.mTmpPaint);
            }
            if (this.mText != null && this.mText.length() > 0) {
                this.mTmpPaint.setColor(-65536);
                this.mTmpPaint.setTextSize(60.0f);
                this.mTmpPaint.getTextBounds(this.mText, 0, 1, this.mTmpRect);
                canvas.drawText(this.mText, 10.0f, (float) ((getMeasuredHeight() - this.mTmpRect.height()) - this.mConfig.systemInsets.bottom), this.mTmpPaint);
            }
        }
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == this.mPrimarySeekBar) {
            this.mCb.onPrimarySeekBarChanged(((float) progress) / ((float) this.mPrimarySeekBar.getMax()));
        } else if (seekBar == this.mSecondarySeekBar) {
            this.mCb.onSecondarySeekBarChanged(((float) progress) / ((float) this.mSecondarySeekBar.getMax()));
        }
    }
}
