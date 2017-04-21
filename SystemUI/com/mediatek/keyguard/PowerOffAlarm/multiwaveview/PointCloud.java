package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.util.ArrayList;

public class PointCloud {
    GlowManager glowManager = new GlowManager();
    private float mCenterX;
    private float mCenterY;
    private Drawable mDrawable;
    private float mOuterRadius;
    private Paint mPaint = new Paint();
    private ArrayList<Point> mPointCloud = new ArrayList();
    private float mScale = 1.0f;
    WaveManager waveManager = new WaveManager();

    public class GlowManager {
        private float alpha = 0.0f;
        private float radius = 0.0f;
        private float x;
        private float y;

        public void setX(float x1) {
            this.x = x1;
        }

        public void setY(float y1) {
            this.y = y1;
        }

        public void setRadius(float r) {
            this.radius = r;
        }
    }

    class Point {
        float radius;
        float x;
        float y;

        public Point(float x2, float y2, float r) {
            this.x = x2;
            this.y = y2;
            this.radius = r;
        }
    }

    public class WaveManager {
        private float alpha = 0.0f;
        private float radius = 50.0f;
        private float width = 200.0f;

        public void setRadius(float r) {
            this.radius = r;
        }

        public void setAlpha(float a) {
            this.alpha = a;
        }
    }

    public PointCloud(Drawable drawable) {
        this.mPaint.setFilterBitmap(true);
        this.mPaint.setColor(Color.rgb(255, 255, 255));
        this.mPaint.setAntiAlias(true);
        this.mPaint.setDither(true);
        this.mDrawable = drawable;
        if (this.mDrawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
    }

    public void setCenter(float x, float y) {
        this.mCenterX = x;
        this.mCenterY = y;
    }

    public void makePointCloud(float innerRadius, float outerRadius) {
        if (innerRadius == 0.0f) {
            Log.w("PointCloud", "Must specify an inner radius");
            return;
        }
        this.mOuterRadius = outerRadius;
        this.mPointCloud.clear();
        float pointAreaRadius = outerRadius - innerRadius;
        float ds = (6.2831855f * innerRadius) / 8.0f;
        int bands = Math.round(pointAreaRadius / ds);
        float dr = pointAreaRadius / ((float) bands);
        float r = innerRadius;
        int b = 0;
        while (b <= bands) {
            int pointsInBand = (int) ((6.2831855f * r) / ds);
            float eta = 1.5707964f;
            float dEta = 6.2831855f / ((float) pointsInBand);
            for (int i = 0; i < pointsInBand; i++) {
                float x = r * ((float) Math.cos((double) eta));
                float y = r * ((float) Math.sin((double) eta));
                eta += dEta;
                this.mPointCloud.add(new Point(x, y, r));
            }
            b++;
            r += dr;
        }
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    private static float hypot(float x, float y) {
        return (float) Math.hypot((double) x, (double) y);
    }

    private static float max(float a, float b) {
        return a > b ? a : b;
    }

    public int getAlphaForPoint(Point point) {
        float glowDistance = hypot(this.glowManager.x - point.x, this.glowManager.y - point.y);
        float glowAlpha = 0.0f;
        if (glowDistance < this.glowManager.radius) {
            glowAlpha = this.glowManager.alpha * max(0.0f, (float) Math.pow(Math.cos((((double) glowDistance) * 0.7853981633974483d) / ((double) this.glowManager.radius)), 10.0d));
        }
        float distanceToWaveRing = hypot(point.x, point.y) - this.waveManager.radius;
        float waveAlpha = 0.0f;
        if (distanceToWaveRing < this.waveManager.width * 0.5f && distanceToWaveRing < 0.0f) {
            waveAlpha = this.waveManager.alpha * max(0.0f, (float) Math.pow(Math.cos((((double) distanceToWaveRing) * 0.7853981633974483d) / ((double) this.waveManager.width)), 20.0d));
        }
        return (int) (max(glowAlpha, waveAlpha) * 255.0f);
    }

    private float interp(float min, float max, float f) {
        return ((max - min) * f) + min;
    }

    public void draw(Canvas canvas) {
        ArrayList<Point> points = this.mPointCloud;
        canvas.save(1);
        canvas.scale(this.mScale, this.mScale, this.mCenterX, this.mCenterY);
        for (int i = 0; i < points.size(); i++) {
            Point point = (Point) points.get(i);
            float pointSize = interp(4.0f, 2.0f, point.radius / this.mOuterRadius);
            float px = point.x + this.mCenterX;
            float py = point.y + this.mCenterY;
            int alpha = getAlphaForPoint(point);
            if (alpha != 0) {
                if (this.mDrawable != null) {
                    canvas.save(1);
                    float cx = ((float) this.mDrawable.getIntrinsicWidth()) * 0.5f;
                    float cy = ((float) this.mDrawable.getIntrinsicHeight()) * 0.5f;
                    float s = pointSize / 4.0f;
                    canvas.scale(s, s, px, py);
                    canvas.translate(px - cx, py - cy);
                    this.mDrawable.setAlpha(alpha);
                    this.mDrawable.draw(canvas);
                    canvas.restore();
                } else {
                    this.mPaint.setAlpha(alpha);
                    canvas.drawCircle(px, py, pointSize, this.mPaint);
                }
            }
        }
        canvas.restore();
    }
}
