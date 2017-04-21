package com.android.systemui;

import android.animation.ArgbEvaluator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.Op;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.mediatek.systemui.statusbar.util.BatteryHelper;

public class BatteryMeterView extends View implements DemoMode, BatteryStateChangeCallback {
    public static final String TAG = BatteryMeterView.class.getSimpleName();
    private BatteryController mBatteryController;
    private int mBatteryLevel;
    private final Paint mBatteryPaint;
    private final RectF mBoltFrame;
    private final Paint mBoltPaint;
    private final Path mBoltPath;
    private final float[] mBoltPoints;
    private final RectF mButtonFrame;
    private float mButtonHeightFraction;
    private int mChargeColor;
    private final Path mClipPath;
    private final int[] mColors;
    private final int mCriticalLevel;
    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;
    private boolean mDemoMode;
    private BatteryTracker mDemoTracker;
    private final RectF mFrame;
    private final Paint mFramePaint;
    private int mHeight;
    private int mIconTint;
    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;
    private boolean mPowerSaveEnabled;
    private final SettingObserver mSettingObserver;
    private final Path mShapePath;
    private final Path mShapePathUnion;
    private int mShouldShowBatteryPercentage;
    private boolean mShowPercent;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private float mTextHeight;
    private final Paint mTextPaint;
    private final Path mTextPath;
    private BatteryTracker mTracker;
    private String mWarningString;
    private float mWarningTextHeight;
    private final Paint mWarningTextPaint;
    private int mWidth;

    private final class BatteryTracker extends BroadcastReceiver {
        int health;
        int level;
        int plugType;
        boolean plugged;
        int status;
        String technology;
        int temperature;
        boolean testmode;
        int voltage;

        private BatteryTracker() {
            this.level = -1;
            this.testmode = false;
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                if (!this.testmode || intent.getBooleanExtra("testmode", false)) {
                    boolean z;
                    this.level = (int) ((((float) intent.getIntExtra("level", 0)) * 100.0f) / ((float) intent.getIntExtra("scale", 100)));
                    this.plugType = intent.getIntExtra("plugged", 0);
                    if (this.plugType != 0) {
                        z = true;
                    } else {
                        z = false;
                    }
                    this.plugged = z;
                    this.health = intent.getIntExtra("health", 1);
                    this.status = intent.getIntExtra("status", 1);
                    this.technology = intent.getStringExtra("technology");
                    this.voltage = intent.getIntExtra("voltage", 0);
                    this.temperature = intent.getIntExtra("temperature", 0);
                    BatteryMeterView.this.setContentDescription(context.getString(R.string.accessibility_battery_level, new Object[]{Integer.valueOf(this.level)}));
                    BatteryMeterView.this.postInvalidate();
                }
            } else if (action.equals("com.android.systemui.BATTERY_LEVEL_TEST")) {
                this.testmode = true;
                BatteryMeterView.this.post(new Runnable() {
                    int curLevel = 0;
                    Intent dummy = new Intent("android.intent.action.BATTERY_CHANGED");
                    int incr = 1;
                    int saveLevel = BatteryTracker.this.level;
                    int savePlugged = BatteryTracker.this.plugType;

                    public void run() {
                        int i = 0;
                        if (this.curLevel < 0) {
                            BatteryTracker.this.testmode = false;
                            this.dummy.putExtra("level", this.saveLevel);
                            this.dummy.putExtra("plugged", this.savePlugged);
                            this.dummy.putExtra("testmode", false);
                        } else {
                            this.dummy.putExtra("level", this.curLevel);
                            Intent intent = this.dummy;
                            String str = "plugged";
                            if (this.incr > 0) {
                                i = 1;
                            }
                            intent.putExtra(str, i);
                            this.dummy.putExtra("testmode", true);
                        }
                        BatteryMeterView.this.getContext().sendBroadcast(this.dummy);
                        if (BatteryTracker.this.testmode) {
                            this.curLevel += this.incr;
                            if (this.curLevel == 100) {
                                this.incr *= -1;
                            }
                            BatteryMeterView.this.postDelayed(this, 200);
                        }
                    }
                });
            } else if (action.equals("com.intent.action.BATTERY_PERCENTAGE_SWITCH")) {
                BatteryMeterView.this.mShowPercent = false;
                Log.d("qiukai", "mShowPercent ==" + BatteryMeterView.this.mShowPercent);
                BatteryMeterView.this.postInvalidate();
            }
        }
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            BatteryMeterView.this.updateShowPercent();
            BatteryMeterView.this.postInvalidate();
        }
    }

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mShouldShowBatteryPercentage = 0;
        this.mIconTint = -1;
        this.mBoltPath = new Path();
        this.mFrame = new RectF();
        this.mButtonFrame = new RectF();
        this.mBoltFrame = new RectF();
        this.mShapePath = new Path();
        this.mShapePathUnion = new Path();
        this.mClipPath = new Path();
        this.mTextPath = new Path();
        this.mTracker = new BatteryTracker();
        this.mSettingObserver = new SettingObserver();
        this.mDemoTracker = new BatteryTracker();
        Resources res = context.getResources();
        TypedArray atts = context.obtainStyledAttributes(attrs, R$styleable.BatteryMeterView, defStyle, 0);
        int frameColor = atts.getColor(0, context.getColor(R.color.batterymeter_frame_color));
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);
        int N = levels.length();
        this.mColors = new int[(N * 2)];
        for (int i = 0; i < N; i++) {
            this.mColors[i * 2] = levels.getInt(i, 0);
            this.mColors[(i * 2) + 1] = colors.getColor(i, 0);
        }
        levels.recycle();
        colors.recycle();
        atts.recycle();
        updateShowPercent();
        this.mShowPercent = false;
        this.mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        this.mCriticalLevel = this.mContext.getResources().getInteger(17694797);
        this.mButtonHeightFraction = context.getResources().getFraction(R.fraction.battery_button_height_fraction, 1, 1);
        this.mSubpixelSmoothingLeft = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_left, 1, 1);
        this.mSubpixelSmoothingRight = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_right, 1, 1);
        this.mFramePaint = new Paint(1);
        this.mFramePaint.setColor(frameColor);
        this.mFramePaint.setDither(true);
        this.mFramePaint.setStrokeWidth(0.0f);
        this.mFramePaint.setStyle(Style.FILL_AND_STROKE);
        this.mBatteryPaint = new Paint(1);
        this.mBatteryPaint.setDither(true);
        this.mBatteryPaint.setStrokeWidth(0.0f);
        this.mBatteryPaint.setStyle(Style.FILL_AND_STROKE);
        this.mTextPaint = new Paint(1);
        this.mTextPaint.setTypeface(Typeface.create("sans-serif-condensed", 1));
        this.mTextPaint.setTextAlign(Align.CENTER);
        this.mWarningTextPaint = new Paint(1);
        this.mWarningTextPaint.setColor(this.mColors[1]);
        this.mWarningTextPaint.setTypeface(Typeface.create("sans-serif", 1));
        this.mWarningTextPaint.setTextAlign(Align.CENTER);
        this.mChargeColor = context.getColor(R.color.batterymeter_charge_color);
        this.mBoltPaint = new Paint(1);
        this.mBoltPaint.setColor(context.getColor(R.color.batterymeter_bolt_color));
        this.mBoltPoints = loadBoltPoints(res);
        this.mDarkModeBackgroundColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
        this.mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
        this.mLightModeBackgroundColor = context.getColor(R.color.light_mode_icon_color_dual_tone_background);
        this.mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("com.android.systemui.BATTERY_LEVEL_TEST");
        filter.addAction("com.intent.action.BATTERY_PERCENTAGE_SWITCH");
        Intent sticky = getContext().registerReceiver(this.mTracker, filter);
        if (sticky != null) {
            this.mTracker.onReceive(getContext(), sticky);
        }
        this.mBatteryController.addStateChangedCallback(this);
        getContext().getContentResolver().registerContentObserver(System.getUriFor("status_bar_show_battery_percent"), false, this.mSettingObserver);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(this.mTracker);
        this.mBatteryController.removeStateChangedCallback(this);
        getContext().getContentResolver().unregisterContentObserver(this.mSettingObserver);
    }

    public void setBatteryController(BatteryController batteryController) {
        this.mBatteryController = batteryController;
        this.mPowerSaveEnabled = this.mBatteryController.isPowerSave();
    }

    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
    }

    public void onPowerSaveChanged() {
        this.mPowerSaveEnabled = this.mBatteryController.isPowerSave();
        invalidate();
    }

    private static float[] loadBoltPoints(Resources res) {
        int i;
        int[] pts = res.getIntArray(R.array.batterymeter_bolt_points);
        int maxX = 0;
        int maxY = 0;
        for (i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        float[] ptsF = new float[pts.length];
        for (i = 0; i < pts.length; i += 2) {
            ptsF[i] = ((float) pts[i]) / ((float) maxX);
            ptsF[i + 1] = ((float) pts[i + 1]) / ((float) maxY);
        }
        return ptsF;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mHeight = h;
        this.mWidth = w;
        this.mWarningTextPaint.setTextSize(((float) h) * 0.75f);
        this.mWarningTextHeight = -this.mWarningTextPaint.getFontMetrics().ascent;
    }

    private void updateShowPercent() {
        boolean z = false;
        if (System.getInt(getContext().getContentResolver(), "status_bar_show_battery_percent", 0) != 0) {
            z = true;
        }
        this.mShowPercent = z;
    }

    private int getColorForLevel(int percent) {
        if (this.mPowerSaveEnabled) {
            return this.mColors[this.mColors.length - 1];
        }
        int color = 0;
        int i = 0;
        while (i < this.mColors.length) {
            int thresh = this.mColors[i];
            color = this.mColors[i + 1];
            if (percent > thresh) {
                i += 2;
            } else if (i == this.mColors.length - 2) {
                return this.mIconTint;
            } else {
                return color;
            }
        }
        return color;
    }

    public void setDarkIntensity(float darkIntensity) {
        int backgroundColor = getBackgroundColor(darkIntensity);
        int fillColor = getFillColor(darkIntensity);
        this.mIconTint = fillColor;
        this.mFramePaint.setColor(backgroundColor);
        this.mBoltPaint.setColor(fillColor);
        this.mChargeColor = fillColor;
        invalidate();
    }

    private int getBackgroundColor(float darkIntensity) {
        return getColorForDarkIntensity(darkIntensity, this.mLightModeBackgroundColor, this.mDarkModeBackgroundColor);
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(darkIntensity, this.mLightModeFillColor, this.mDarkModeFillColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return ((Integer) ArgbEvaluator.getInstance().evaluate(darkIntensity, Integer.valueOf(lightColor), Integer.valueOf(darkColor))).intValue();
    }

    public int getLevel() {
        return this.mBatteryLevel;
    }

    public void draw(Canvas c) {
        BatteryTracker tracker = this.mDemoMode ? this.mDemoTracker : this.mTracker;
        int level = tracker.level;
        this.mBatteryLevel = tracker.level;
        boolean isPlugForProtection = tracker.plugged ? BatteryHelper.isPlugForProtection(tracker.status, tracker.level) : false;
        if (level != -1) {
            float levelTop;
            float drawFrac = ((float) level) / 100.0f;
            int pt = getPaddingTop();
            int pl = getPaddingLeft();
            int pr = getPaddingRight();
            int pb = getPaddingBottom();
            int height = (this.mHeight - pt) - pb;
            int width = (this.mWidth - pl) - pr;
            if (isPlugForProtection && BatteryHelper.isWirelessCharging(tracker.plugType)) {
                height = (int) (((double) ((this.mHeight - pt) - pb)) * 0.95d);
            }
            int buttonHeight = (int) (((float) height) * this.mButtonHeightFraction);
            this.mFrame.set(0.0f, 0.0f, (float) width, (float) height);
            this.mFrame.offset((float) pl, (float) pt);
            this.mButtonFrame.set(this.mFrame.left + ((float) Math.round(((float) width) * 0.25f)), this.mFrame.top, this.mFrame.right - ((float) Math.round(((float) width) * 0.25f)), this.mFrame.top + ((float) buttonHeight));
            RectF rectF = this.mButtonFrame;
            rectF.top += this.mSubpixelSmoothingLeft;
            rectF = this.mButtonFrame;
            rectF.left += this.mSubpixelSmoothingLeft;
            rectF = this.mButtonFrame;
            rectF.right -= this.mSubpixelSmoothingRight;
            rectF = this.mFrame;
            rectF.top += (float) buttonHeight;
            rectF = this.mFrame;
            rectF.left += this.mSubpixelSmoothingLeft;
            rectF = this.mFrame;
            rectF.top += this.mSubpixelSmoothingLeft;
            rectF = this.mFrame;
            rectF.right -= this.mSubpixelSmoothingRight;
            rectF = this.mFrame;
            rectF.bottom -= this.mSubpixelSmoothingRight;
            this.mBatteryPaint.setColor(tracker.plugged ? this.mChargeColor : getColorForLevel(level));
            if (level >= 96) {
                drawFrac = 1.0f;
            } else if (level <= this.mCriticalLevel) {
                drawFrac = 0.0f;
            }
            if (drawFrac == 1.0f) {
                levelTop = this.mButtonFrame.top;
            } else {
                levelTop = this.mFrame.top + (this.mFrame.height() * (1.0f - drawFrac));
            }
            this.mShapePath.reset();
            this.mShapePath.moveTo(this.mButtonFrame.left, this.mButtonFrame.top);
            this.mShapePath.lineTo(this.mButtonFrame.right, this.mButtonFrame.top);
            this.mShapePath.lineTo(this.mButtonFrame.right, this.mFrame.top);
            this.mShapePath.lineTo(this.mFrame.right, this.mFrame.top);
            this.mShapePath.lineTo(this.mFrame.right, this.mFrame.bottom);
            this.mShapePath.lineTo(this.mFrame.left, this.mFrame.bottom);
            this.mShapePath.lineTo(this.mFrame.left, this.mFrame.top);
            this.mShapePath.lineTo(this.mButtonFrame.left, this.mFrame.top);
            this.mShapePath.lineTo(this.mButtonFrame.left, this.mButtonFrame.top);
            if (isPlugForProtection) {
                float bl = this.mFrame.left + (this.mFrame.width() / 4.5f);
                float bt = this.mFrame.top + (this.mFrame.height() / 6.0f);
                float br = this.mFrame.right - (this.mFrame.width() / 7.0f);
                float bb = this.mFrame.bottom - (this.mFrame.height() / 10.0f);
                if (!(this.mBoltFrame.left == bl && this.mBoltFrame.top == bt && this.mBoltFrame.right == br && this.mBoltFrame.bottom == bb)) {
                    this.mBoltFrame.set(bl, bt, br, bb);
                    this.mBoltPath.reset();
                    this.mBoltPath.moveTo(this.mBoltFrame.left + (this.mBoltPoints[0] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[1] * this.mBoltFrame.height()));
                    for (int i = 2; i < this.mBoltPoints.length; i += 2) {
                        this.mBoltPath.lineTo(this.mBoltFrame.left + (this.mBoltPoints[i] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[i + 1] * this.mBoltFrame.height()));
                    }
                    this.mBoltPath.lineTo(this.mBoltFrame.left + (this.mBoltPoints[0] * this.mBoltFrame.width()), this.mBoltFrame.top + (this.mBoltPoints[1] * this.mBoltFrame.height()));
                }
                if (Math.min(Math.max((this.mBoltFrame.bottom - levelTop) / (this.mBoltFrame.bottom - this.mBoltFrame.top), 0.0f), 1.0f) <= 0.3f) {
                    c.drawPath(this.mBoltPath, this.mBoltPaint);
                } else {
                    this.mShapePath.op(this.mBoltPath, Op.DIFFERENCE);
                }
                if (BatteryHelper.isWirelessCharging(tracker.plugType)) {
                    c.drawLine(this.mFrame.left, (float) this.mHeight, this.mFrame.right, (float) this.mHeight, this.mBatteryPaint);
                }
            }
            boolean pctOpaque = false;
            float pctX = 0.0f;
            float pctY = 0.0f;
            String str = null;
            if (!tracker.plugged && level > this.mCriticalLevel && this.mShowPercent) {
                this.mTextPaint.setColor(getColorForLevel(level));
                this.mTextPaint.setTextSize((tracker.level == 100 ? 0.38f : 0.5f) * ((float) height));
                this.mTextHeight = -this.mTextPaint.getFontMetrics().ascent;
                str = String.valueOf(level);
                pctX = ((float) this.mWidth) * 0.5f;
                pctY = (((float) this.mHeight) + this.mTextHeight) * 0.47f;
                pctOpaque = levelTop > pctY;
                if (!pctOpaque) {
                    this.mTextPath.reset();
                    this.mTextPaint.getTextPath(str, 0, str.length(), pctX, pctY, this.mTextPath);
                    this.mShapePath.op(this.mTextPath, Op.DIFFERENCE);
                }
            }
            c.drawPath(this.mShapePath, this.mFramePaint);
            this.mFrame.top = levelTop;
            this.mClipPath.reset();
            this.mClipPath.addRect(this.mFrame, Direction.CCW);
            this.mShapePathUnion.reset();
            this.mShapePathUnion.op(this.mShapePath, Op.UNION);
            this.mShapePathUnion.op(this.mClipPath, Op.INTERSECT);
            c.drawPath(this.mShapePathUnion, this.mBatteryPaint);
            if (!tracker.plugged) {
                if (level <= this.mCriticalLevel) {
                    c.drawText(this.mWarningString, ((float) this.mWidth) * 0.5f, (((float) this.mHeight) + this.mWarningTextHeight) * 0.48f, this.mWarningTextPaint);
                } else if (pctOpaque) {
                    c.drawText(str, pctX, pctY, this.mTextPaint);
                }
            }
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            this.mDemoTracker.level = this.mTracker.level;
            this.mDemoTracker.plugged = this.mTracker.plugged;
        } else if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            postInvalidate();
        } else if (this.mDemoMode && command.equals("battery")) {
            String level = args.getString("level");
            String plugged = args.getString("plugged");
            if (level != null) {
                this.mDemoTracker.level = Math.min(Math.max(Integer.parseInt(level), 0), 100);
            }
            if (plugged != null) {
                this.mDemoTracker.plugged = Boolean.parseBoolean(plugged);
            }
            postInvalidate();
        }
    }
}
