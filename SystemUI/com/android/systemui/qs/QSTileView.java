package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.MathUtils;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile.State;
import java.util.Objects;

public class QSTileView extends ViewGroup {
    private static final Typeface CONDENSED = Typeface.create("sans-serif-condensed", 0);
    private OnClickListener mClickPrimary;
    private OnClickListener mClickSecondary;
    protected final Context mContext;
    private final View mDivider;
    private boolean mDual;
    private QSDualTileLabel mDualLabel;
    private final int mDualTileVerticalPaddingPx;
    private final H mHandler = new H();
    private final View mIcon;
    private final int mIconSizePx;
    private TextView mLabel;
    private OnLongClickListener mLongClick;
    private RippleDrawable mRipple;
    private Drawable mTileBackground;
    private final int mTilePaddingBelowIconPx;
    private int mTilePaddingTopPx;
    private final int mTileSpacingPx;
    private final View mTopBackgroundView;

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                QSTileView.this.handleStateChanged((State) msg.obj);
            }
        }
    }

    public QSTileView(Context context) {
        super(context);
        this.mContext = context;
        Resources res = context.getResources();
        this.mIconSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        this.mTileSpacingPx = res.getDimensionPixelSize(R.dimen.qs_tile_spacing);
        this.mTilePaddingBelowIconPx = res.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        this.mDualTileVerticalPaddingPx = res.getDimensionPixelSize(R.dimen.qs_dual_tile_padding_vertical);
        this.mTileBackground = newTileBackground();
        recreateLabel();
        setClipChildren(false);
        this.mTopBackgroundView = new View(context);
        this.mTopBackgroundView.setId(View.generateViewId());
        addView(this.mTopBackgroundView);
        this.mIcon = createIcon();
        addView(this.mIcon);
        this.mDivider = new View(this.mContext);
        this.mDivider.setBackgroundColor(context.getColor(R.color.qs_tile_divider));
        this.mDivider.setLayoutParams(new LayoutParams(-1, res.getDimensionPixelSize(R.dimen.qs_tile_divider_height)));
        addView(this.mDivider);
        setClickable(true);
        updateTopPadding();
        setId(View.generateViewId());
    }

    private void updateTopPadding() {
        Resources res = getResources();
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale, 1.0f, 1.3f) - 1.0f) / 0.29999995f;
        this.mTilePaddingTopPx = Math.round(((1.0f - largeFactor) * ((float) res.getDimensionPixelSize(R.dimen.qs_tile_padding_top))) + (((float) res.getDimensionPixelSize(R.dimen.qs_tile_padding_top_large_text)) * largeFactor));
        requestLayout();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTopPadding();
        FontSizeUtils.updateFontSize(this.mLabel, R.dimen.qs_tile_text_size);
        if (this.mDualLabel != null) {
            this.mDualLabel.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.qs_tile_text_size));
        }
    }

    private void recreateLabel() {
        CharSequence charSequence = null;
        CharSequence charSequence2 = null;
        if (this.mLabel != null) {
            charSequence = this.mLabel.getText();
            removeView(this.mLabel);
            this.mLabel = null;
        }
        if (this.mDualLabel != null) {
            charSequence = this.mDualLabel.getText();
            charSequence2 = this.mLabel.getContentDescription();
            removeView(this.mDualLabel);
            this.mDualLabel = null;
        }
        Resources res = this.mContext.getResources();
        if (this.mDual) {
            this.mDualLabel = new QSDualTileLabel(this.mContext);
            this.mDualLabel.setId(View.generateViewId());
            this.mDualLabel.setBackgroundResource(R.drawable.btn_borderless_rect);
            this.mDualLabel.setFirstLineCaret(this.mContext.getDrawable(R.drawable.qs_dual_tile_caret));
            this.mDualLabel.setTextColor(this.mContext.getColor(R.color.qs_tile_text));
            this.mDualLabel.setPadding(0, this.mDualTileVerticalPaddingPx, 0, this.mDualTileVerticalPaddingPx);
            this.mDualLabel.setTypeface(CONDENSED);
            this.mDualLabel.setTextSize(0, (float) res.getDimensionPixelSize(R.dimen.qs_tile_text_size));
            this.mDualLabel.setClickable(true);
            this.mDualLabel.setOnClickListener(this.mClickSecondary);
            this.mDualLabel.setFocusable(true);
            if (charSequence != null) {
                this.mDualLabel.setText(charSequence);
            }
            if (charSequence2 != null) {
                this.mDualLabel.setContentDescription(charSequence2);
            }
            addView(this.mDualLabel);
            this.mDualLabel.setAccessibilityTraversalAfter(this.mTopBackgroundView.getId());
            return;
        }
        this.mLabel = new TextView(this.mContext);
        this.mLabel.setTextColor(this.mContext.getColor(R.color.qs_tile_text));
        this.mLabel.setGravity(1);
        this.mLabel.setMinLines(2);
        this.mLabel.setPadding(0, 0, 0, 0);
        this.mLabel.setTypeface(CONDENSED);
        this.mLabel.setTextSize(0, (float) res.getDimensionPixelSize(R.dimen.qs_tile_text_size));
        this.mLabel.setClickable(false);
        if (charSequence != null) {
            this.mLabel.setText(charSequence);
        }
        addView(this.mLabel);
    }

    public boolean setDual(boolean dual) {
        boolean z;
        int i = 0;
        boolean changed = dual != this.mDual;
        this.mDual = dual;
        if (changed) {
            recreateLabel();
        }
        if (this.mTileBackground instanceof RippleDrawable) {
            setRipple((RippleDrawable) this.mTileBackground);
        }
        if (dual) {
            this.mTopBackgroundView.setOnClickListener(this.mClickPrimary);
            setOnClickListener(null);
            setClickable(false);
            setImportantForAccessibility(2);
            this.mTopBackgroundView.setBackground(this.mTileBackground);
        } else {
            this.mTopBackgroundView.setOnClickListener(null);
            this.mTopBackgroundView.setClickable(false);
            setOnClickListener(this.mClickPrimary);
            setOnLongClickListener(this.mLongClick);
            setImportantForAccessibility(1);
            setBackground(this.mTileBackground);
        }
        this.mTopBackgroundView.setFocusable(dual);
        if (dual) {
            z = false;
        } else {
            z = true;
        }
        setFocusable(z);
        View view = this.mDivider;
        if (!dual) {
            i = 8;
        }
        view.setVisibility(i);
        postInvalidate();
        return changed;
    }

    private void setRipple(RippleDrawable tileBackground) {
        this.mRipple = tileBackground;
        if (getWidth() != 0) {
            updateRippleSize(getWidth(), getHeight());
        }
    }

    public void init(OnClickListener clickPrimary, OnClickListener clickSecondary, OnLongClickListener longClick) {
        this.mClickPrimary = clickPrimary;
        this.mClickSecondary = clickSecondary;
        this.mLongClick = longClick;
    }

    protected View createIcon() {
        ImageView icon = new ImageView(this.mContext);
        icon.setId(16908294);
        icon.setScaleType(ScaleType.CENTER_INSIDE);
        return icon;
    }

    private Drawable newTileBackground() {
        TypedArray ta = this.mContext.obtainStyledAttributes(new int[]{16843868});
        Drawable d = ta.getDrawable(0);
        ta.recycle();
        return d;
    }

    private View labelView() {
        return this.mDual ? this.mDualLabel : this.mLabel;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        this.mIcon.measure(MeasureSpec.makeMeasureSpec(w, Integer.MIN_VALUE), exactly(this.mIconSizePx));
        labelView().measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, Integer.MIN_VALUE));
        if (this.mDual) {
            this.mDivider.measure(widthMeasureSpec, exactly(this.mDivider.getLayoutParams().height));
        }
        this.mTopBackgroundView.measure(widthMeasureSpec, exactly((this.mIconSizePx + this.mTilePaddingBelowIconPx) + this.mTilePaddingTopPx));
        setMeasuredDimension(w, h);
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        layout(this.mTopBackgroundView, 0, this.mTileSpacingPx);
        int iconLeft = (w - this.mIcon.getMeasuredWidth()) / 2;
        layout(this.mIcon, iconLeft, (this.mTileSpacingPx + 0) + this.mTilePaddingTopPx);
        if (this.mRipple != null) {
            updateRippleSize(w, h);
        }
        int top = this.mIcon.getBottom() + this.mTilePaddingBelowIconPx;
        if (this.mDual) {
            layout(this.mDivider, 0, top);
            top = this.mDivider.getBottom();
        }
        layout(labelView(), 0, top);
    }

    protected void updateRippleSize(int width, int height) {
        int cx = width / 2;
        int cy = this.mDual ? this.mIcon.getTop() + (this.mIcon.getHeight() / 2) : height / 2;
        int rad = (int) (((float) this.mIcon.getHeight()) * 1.25f);
        this.mRipple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
    }

    private static void layout(View child, int left, int top) {
        child.layout(left, top, child.getMeasuredWidth() + left, child.getMeasuredHeight() + top);
    }

    protected void handleStateChanged(State state) {
        if (this.mIcon instanceof ImageView) {
            setIcon((ImageView) this.mIcon, state);
        }
        if (this.mDual) {
            this.mDualLabel.setText(state.label);
            this.mDualLabel.setContentDescription(state.dualLabelContentDescription);
            this.mTopBackgroundView.setContentDescription(state.contentDescription);
            return;
        }
        this.mLabel.setText(state.label);
        setContentDescription(state.contentDescription);
    }

    protected void setIcon(ImageView iv, State state) {
        if (!Objects.equals(state.icon, iv.getTag(R.id.qs_icon_tag))) {
            Drawable drawable = state.icon != null ? state.icon.getDrawable(this.mContext) : null;
            if (drawable != null && state.autoMirrorDrawable) {
                drawable.setAutoMirrored(true);
            }
            iv.setImageDrawable(drawable);
            iv.setTag(R.id.qs_icon_tag, state.icon);
            if (drawable instanceof Animatable) {
                Animatable a = (Animatable) drawable;
                if ((state.icon instanceof AnimationIcon) && !iv.isShown()) {
                    a.stop();
                }
            }
        }
    }

    public void onStateChanged(State state) {
        this.mHandler.obtainMessage(1, state).sendToTarget();
    }

    public View updateAccessibilityOrder(View previousView) {
        View lastView;
        View firstView;
        if (this.mDual) {
            lastView = this.mDualLabel;
            firstView = this.mTopBackgroundView;
        } else {
            firstView = this;
            lastView = this;
        }
        firstView.setAccessibilityTraversalAfter(previousView.getId());
        return lastView;
    }

    protected View getQSIcon() {
        return this.mIcon;
    }

    protected View getDivider() {
        return this.mDivider;
    }

    protected int getIconSizePx() {
        return this.mIconSizePx;
    }

    protected int getTilePaddingTopPx() {
        return this.mTilePaddingTopPx;
    }

    protected int getTilePaddingBelowIconPx() {
        return this.mTilePaddingBelowIconPx;
    }

    protected View getTopBackgroundView() {
        return this.mTopBackgroundView;
    }

    protected TextView getLabel() {
        return this.mLabel;
    }

    protected QSDualTileLabel getDualLabel() {
        return this.mDualLabel;
    }

    protected boolean getDual() {
        return this.mDual;
    }

    protected RippleDrawable getRipple() {
        return this.mRipple;
    }
}
