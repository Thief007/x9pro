package com.mediatek.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.internal.util.Preconditions;
import com.android.settings.R$styleable;
import com.android.settings.widget.ChartAxis;

public class ChartBandwidthNetworkSeriesView extends View {
    private long[] mCurrentBytes;
    private int mCurrentLen;
    private long mEndTime;
    private ChartAxis mHoriz;
    private long mMax;
    private Paint mPaintStroke;
    private Path mPathStroke;
    private boolean mPathValid;
    private NetworkStats mStats;
    private long mTotalUsed;
    private ChartAxis mVert;

    public ChartBandwidthNetworkSeriesView(Context context) {
        this(context, null, 0);
    }

    public ChartBandwidthNetworkSeriesView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartBandwidthNetworkSeriesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mEndTime = Long.MIN_VALUE;
        this.mPathValid = false;
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.ChartNetworkSeriesView, defStyle, 0);
        setSeriesColor(a.getColor(1, -65536));
        setWillNotDraw(false);
        a.recycle();
        this.mPathStroke = new Path();
        this.mCurrentBytes = new long[90];
        this.mCurrentLen = 0;
    }

    public void setSeriesColor(int stroke) {
        this.mPaintStroke = new Paint();
        this.mPaintStroke.setStrokeWidth(3.0f);
        this.mPaintStroke.setColor(stroke);
        this.mPaintStroke.setStyle(Style.STROKE);
        this.mPaintStroke.setAntiAlias(true);
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        this.mHoriz = (ChartAxis) Preconditions.checkNotNull(horiz, "missing horiz");
        this.mVert = (ChartAxis) Preconditions.checkNotNull(vert, "missing vert");
    }

    public long getMaxBytes() {
        return this.mMax > 1048576 ? this.mMax : 1048576;
    }

    public long getTotalUsedData() {
        return this.mTotalUsed;
    }

    public void setNetworkStates(NetworkStats networkStats) {
        this.mStats = networkStats;
        generatePath();
    }

    public void invalidatePath() {
        this.mPathValid = false;
        this.mMax = 0;
        invalidate();
    }

    public void generatePath() {
        long range = getMaxBytes();
        this.mMax = 0;
        this.mPathStroke.reset();
        if (this.mStats != null && this.mStats.size() >= 1) {
            int i;
            this.mPathValid = true;
            long totalData = 0;
            for (i = 0; i < this.mStats.size(); i++) {
                Entry entry = this.mStats.getValues(i, null);
                Log.d("@M_ChartBandwidthNetworkSeriesView", "index = " + i + ", rxBytes = " + entry.rxBytes + ", txBytes = " + entry.txBytes);
                totalData += entry.rxBytes + entry.txBytes;
            }
            Log.d("@M_ChartBandwidthNetworkSeriesView", "totalData = " + totalData + ", mTotalUsed = " + this.mTotalUsed);
            long currentData = this.mTotalUsed == 0 ? 0 : totalData - this.mTotalUsed;
            this.mTotalUsed = totalData;
            Log.d("@M_ChartBandwidthNetworkSeriesView", "currentData = " + currentData);
            if (this.mCurrentLen < 90) {
                this.mCurrentBytes[this.mCurrentLen] = currentData;
                this.mCurrentLen++;
            } else {
                System.arraycopy(this.mCurrentBytes, 1, this.mCurrentBytes, 0, 89);
                this.mCurrentBytes[89] = currentData;
            }
            this.mPathStroke.moveTo(this.mHoriz.convertToPoint((long) ((100 - this.mCurrentLen) + 1)), this.mVert.convertToPoint((this.mCurrentBytes[0] / range) * 100));
            i = 0;
            while (i < this.mCurrentLen) {
                this.mPathStroke.lineTo(this.mHoriz.convertToPoint((long) (((100 - this.mCurrentLen) + 1) + i)), this.mVert.convertToPoint(this.mCurrentBytes[i]));
                this.mMax = this.mMax < this.mCurrentBytes[i] ? this.mCurrentBytes[i] : this.mMax;
                i++;
            }
            invalidate();
        }
    }

    protected void onDraw(Canvas canvas) {
        if (!this.mPathValid) {
            generatePath();
        }
        float primaryLeftPoint = this.mHoriz.convertToPoint(0);
        float primaryRightPoint = this.mHoriz.convertToPoint(100);
        int save = canvas.save();
        canvas.clipRect(primaryLeftPoint, 0.0f, primaryRightPoint, (float) getHeight());
        canvas.drawPath(this.mPathStroke, this.mPaintStroke);
        canvas.restoreToCount(save);
    }
}
