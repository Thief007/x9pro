package com.android.settings.drawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import com.android.settings.R;

public class CircleFramedDrawable extends Drawable {
    private final Bitmap mBitmap = Bitmap.createBitmap(this.mSize, this.mSize, Config.ARGB_8888);
    private RectF mDstRect;
    private final Paint mPaint;
    private float mScale;
    private final int mSize;
    private Rect mSrcRect;

    public static CircleFramedDrawable getInstance(Context context, Bitmap icon) {
        return new CircleFramedDrawable(icon, (int) context.getResources().getDimension(R.dimen.circle_avatar_size));
    }

    public CircleFramedDrawable(Bitmap icon, int size) {
        this.mSize = size;
        Canvas canvas = new Canvas(this.mBitmap);
        int width = icon.getWidth();
        int height = icon.getHeight();
        int square = Math.min(width, height);
        Rect cropRect = new Rect((width - square) / 2, (height - square) / 2, square, square);
        RectF circleRect = new RectF(0.0f, 0.0f, (float) this.mSize, (float) this.mSize);
        Path fillPath = new Path();
        fillPath.addArc(circleRect, 0.0f, 360.0f);
        canvas.drawColor(0, Mode.CLEAR);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(-16777216);
        this.mPaint.setStyle(Style.FILL);
        canvas.drawPath(fillPath, this.mPaint);
        this.mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(icon, cropRect, circleRect, this.mPaint);
        this.mPaint.setXfermode(null);
        this.mScale = 1.0f;
        this.mSrcRect = new Rect(0, 0, this.mSize, this.mSize);
        this.mDstRect = new RectF(0.0f, 0.0f, (float) this.mSize, (float) this.mSize);
    }

    public void draw(Canvas canvas) {
        float pad = (((float) this.mSize) - (this.mScale * ((float) this.mSize))) / 2.0f;
        this.mDstRect.set(pad, pad, ((float) this.mSize) - pad, ((float) this.mSize) - pad);
        canvas.drawBitmap(this.mBitmap, this.mSrcRect, this.mDstRect, null);
    }

    public int getOpacity() {
        return -3;
    }

    public void setAlpha(int alpha) {
    }

    public void setColorFilter(ColorFilter cf) {
    }

    public int getIntrinsicWidth() {
        return this.mSize;
    }

    public int getIntrinsicHeight() {
        return this.mSize;
    }
}
