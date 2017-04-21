package com.android.systemui.qs;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.Objects;

public class QSDualTileLabel extends LinearLayout {
    private final Context mContext;
    private final TextView mFirstLine;
    private final ImageView mFirstLineCaret;
    private final int mHorizontalPaddingPx;
    private final TextView mSecondLine;
    private String mText;
    private final Runnable mUpdateText = new Runnable() {
        public void run() {
            QSDualTileLabel.this.updateText();
        }
    };

    public QSDualTileLabel(Context context) {
        super(context);
        this.mContext = context;
        setOrientation(1);
        this.mHorizontalPaddingPx = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_dual_tile_padding_horizontal);
        this.mFirstLine = initTextView();
        this.mFirstLine.setPadding(this.mHorizontalPaddingPx, 0, this.mHorizontalPaddingPx, 0);
        LinearLayout firstLineLayout = new LinearLayout(this.mContext);
        firstLineLayout.setPadding(0, 0, 0, 0);
        firstLineLayout.setOrientation(0);
        firstLineLayout.setClickable(false);
        firstLineLayout.setBackground(null);
        firstLineLayout.addView(this.mFirstLine);
        this.mFirstLineCaret = new ImageView(this.mContext);
        this.mFirstLineCaret.setScaleType(ScaleType.MATRIX);
        this.mFirstLineCaret.setClickable(false);
        firstLineLayout.addView(this.mFirstLineCaret);
        addView(firstLineLayout, newLinearLayoutParams());
        this.mSecondLine = initTextView();
        this.mSecondLine.setPadding(this.mHorizontalPaddingPx, 0, this.mHorizontalPaddingPx, 0);
        this.mSecondLine.setEllipsize(TruncateAt.END);
        this.mSecondLine.setVisibility(8);
        addView(this.mSecondLine, newLinearLayoutParams());
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (oldRight - oldLeft != right - left) {
                    QSDualTileLabel.this.rescheduleUpdateText();
                }
            }
        });
    }

    private static LayoutParams newLinearLayoutParams() {
        LayoutParams lp = new LayoutParams(-2, -2);
        lp.gravity = 1;
        return lp;
    }

    public void setFirstLineCaret(Drawable d) {
        this.mFirstLineCaret.setImageDrawable(d);
        if (d != null) {
            this.mFirstLine.setMinHeight(d.getIntrinsicHeight());
            this.mFirstLine.setPadding(this.mHorizontalPaddingPx, 0, 0, 0);
        }
    }

    private TextView initTextView() {
        TextView tv = new TextView(this.mContext);
        tv.setPadding(0, 0, 0, 0);
        tv.setGravity(16);
        tv.setSingleLine(true);
        tv.setClickable(false);
        tv.setBackground(null);
        return tv;
    }

    public void setText(CharSequence text) {
        String newText = text == null ? null : text.toString().trim();
        if (!Objects.equals(newText, this.mText)) {
            this.mText = newText;
            rescheduleUpdateText();
        }
    }

    public String getText() {
        return this.mText;
    }

    public void setTextSize(int unit, float size) {
        this.mFirstLine.setTextSize(unit, size);
        this.mSecondLine.setTextSize(unit, size);
        rescheduleUpdateText();
    }

    public void setTextColor(int color) {
        this.mFirstLine.setTextColor(color);
        this.mSecondLine.setTextColor(color);
        rescheduleUpdateText();
    }

    public void setTypeface(Typeface tf) {
        this.mFirstLine.setTypeface(tf);
        this.mSecondLine.setTypeface(tf);
        rescheduleUpdateText();
    }

    private void rescheduleUpdateText() {
        removeCallbacks(this.mUpdateText);
        post(this.mUpdateText);
    }

    private void updateText() {
        if (getWidth() != 0) {
            if (TextUtils.isEmpty(this.mText)) {
                this.mFirstLine.setText(null);
                this.mSecondLine.setText(null);
                this.mSecondLine.setVisibility(8);
                return;
            }
            float maxWidth = (float) ((((getWidth() - this.mFirstLineCaret.getWidth()) - this.mHorizontalPaddingPx) - getPaddingLeft()) - getPaddingRight());
            if (this.mFirstLine.getPaint().measureText(this.mText) <= maxWidth) {
                this.mFirstLine.setText(this.mText);
                this.mSecondLine.setText(null);
                this.mSecondLine.setVisibility(8);
                return;
            }
            int n = this.mText.length();
            int lastWordBoundary = -1;
            boolean inWhitespace = false;
            int i = 1;
            while (i < n) {
                boolean done = this.mFirstLine.getPaint().measureText(this.mText.substring(0, i)) > maxWidth;
                if (Character.isWhitespace(this.mText.charAt(i))) {
                    if (!(inWhitespace || done)) {
                        lastWordBoundary = i;
                    }
                    inWhitespace = true;
                } else {
                    inWhitespace = false;
                }
                if (done) {
                    break;
                }
                i++;
            }
            if (lastWordBoundary == -1) {
                lastWordBoundary = i - 1;
            }
            this.mFirstLine.setText(this.mText.substring(0, lastWordBoundary));
            this.mSecondLine.setText(this.mText.substring(lastWordBoundary).trim());
            this.mSecondLine.setVisibility(0);
        }
    }
}
