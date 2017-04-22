package com.mediatek.widget;

import android.content.Context;
import android.content.res.Resources;
import android.net.NetworkStats;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.widget.ChartAxis;
import com.android.settings.widget.InvertedChartAxis;
import com.mediatek.widget.ChartSweepView.OnSweepListener;
import java.util.Objects;

public class ChartBandwidthUsageView extends ChartView {
    private ChartBandwidthGridView mGrid;
    private Handler mHandler;
    private BandwidthChartListener mListener;
    private ChartBandwidthNetworkSeriesView mSeries;
    private ChartSweepView mSweepLimit;
    private OnSweepListener mVertListener;
    private long mVertMax;

    class C07481 implements OnSweepListener {
        C07481() {
        }

        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            if (sweepDone) {
                ChartBandwidthUsageView.this.clearUpdateAxisDelayed(sweep);
                if (sweep == ChartBandwidthUsageView.this.mSweepLimit && ChartBandwidthUsageView.this.mListener != null) {
                    ChartBandwidthUsageView.this.mListener.onLimitChanged();
                    return;
                }
                return;
            }
            ChartBandwidthUsageView.this.sendUpdateAxisDelayed(sweep, false);
            ChartBandwidthUsageView.this.mListener.onLimitChanging();
        }

        public void requestEdit(ChartSweepView sweep) {
            if (sweep == ChartBandwidthUsageView.this.mSweepLimit && ChartBandwidthUsageView.this.mListener != null) {
                ChartBandwidthUsageView.this.mListener.requestLimitEdit();
            }
        }
    }

    class C07492 extends Handler {
        C07492() {
        }

        public void handleMessage(Message msg) {
            ChartSweepView sweep = msg.obj;
            ChartBandwidthUsageView.this.updateVertAxisBounds(sweep);
            ChartBandwidthUsageView.this.sendUpdateAxisDelayed(sweep, true);
        }
    }

    public static class BandwidthAxis implements ChartAxis {
        private static final Object SPAN_SIZE = new Object();
        private static final Object SPAN_UNIT = new Object();
        private long mMax;
        private long mMin;
        private float mSize;

        public int hashCode() {
            return Objects.hash(new Object[]{Long.valueOf(this.mMin), Long.valueOf(this.mMax), Float.valueOf(this.mSize)});
        }

        public boolean setBounds(long min, long max) {
            if (this.mMin == min && this.mMax == max) {
                return false;
            }
            this.mMin = min;
            this.mMax = max;
            return true;
        }

        public boolean setSize(float size) {
            if (this.mSize == size) {
                return false;
            }
            this.mSize = size;
            return true;
        }

        public float convertToPoint(long value) {
            return (float) (((double) this.mSize) * Math.pow(10.0d, (Math.log10((((double) value) - ((double) this.mMin)) / ((double) (this.mMax - this.mMin))) * 0.3688434310617512d) - 103.35847522266619d));
        }

        public long convertToValue(float point) {
            return (long) (((double) this.mMin) + (((double) (this.mMax - this.mMin)) * (1.3102228476089057d * Math.pow((double) (point / this.mSize), 2.711177469316463d))));
        }

        public long buildLabel(Resources res, SpannableStringBuilder builder, long value) {
            CharSequence unit;
            long unitFactor;
            CharSequence size;
            double resultRounded;
            if (value < 1048576) {
                unit = res.getText(R.string.wifi_ap_bandwidth_KbyteShort);
                unitFactor = 1024;
            } else {
                unit = res.getText(R.string.wifi_ap_bandwidth_megabyteShort);
                unitFactor = 1048576;
            }
            double result = ((double) value) / ((double) unitFactor);
            if (value <= 1048576 || result >= 10.0d) {
                size = String.format("%.0f", new Object[]{Double.valueOf(result)});
                resultRounded = (double) (Math.round(result) * unitFactor);
            } else {
                size = String.format("%.1f", new Object[]{Double.valueOf(result)});
                resultRounded = (double) ((Math.round(10.0d * result) * unitFactor) / 10);
            }
            int[] sizeBounds = findOrCreateSpan(builder, SPAN_SIZE, "^1");
            builder.replace(sizeBounds[0], sizeBounds[1], size);
            int[] unitBounds = findOrCreateSpan(builder, SPAN_UNIT, "^2");
            builder.replace(unitBounds[0], unitBounds[1], unit);
            return (long) resultRounded;
        }

        public float[] getTickPoints() {
            long tickJump;
            long range = this.mMax - this.mMin;
            if (range < 3145728) {
                tickJump = 65536;
            } else if (range < 6291456) {
                tickJump = 131072;
            } else {
                tickJump = 262144;
            }
            float[] tickPoints = new float[((int) (range / tickJump))];
            long value = this.mMin;
            for (int i = 0; i < tickPoints.length; i++) {
                tickPoints[i] = convertToPoint(value);
                value += tickJump;
            }
            return tickPoints;
        }

        public int shouldAdjustAxis(long value) {
            float point = convertToPoint(value);
            if (((double) point) < ((double) this.mSize) * 0.5d) {
                return -1;
            }
            if (((double) point) > ((double) this.mSize) * 0.85d) {
                return 1;
            }
            return 0;
        }

        private int[] findOrCreateSpan(SpannableStringBuilder builder, Object key, CharSequence bootstrap) {
            int start = builder.getSpanStart(key);
            int end = builder.getSpanEnd(key);
            if (start == -1) {
                start = TextUtils.indexOf(builder, bootstrap);
                end = start + bootstrap.length();
                builder.setSpan(key, start, end, 18);
            }
            return new int[]{start, end};
        }
    }

    public interface BandwidthChartListener {
        void onLimitChanged();

        void onLimitChanging();

        void requestLimitEdit();
    }

    public class TimeAxis implements ChartAxis {
        private long mMax;
        private long mMin;
        private float mSize;

        public TimeAxis() {
            setBounds(0, 100);
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Long.valueOf(this.mMin), Long.valueOf(this.mMax), Float.valueOf(this.mSize)});
        }

        public boolean setBounds(long min, long max) {
            if (this.mMin == min && this.mMax == max) {
                return false;
            }
            this.mMin = min;
            this.mMax = max;
            return true;
        }

        public boolean setSize(float size) {
            if (this.mSize == size) {
                return false;
            }
            this.mSize = size;
            return true;
        }

        public float convertToPoint(long value) {
            return (this.mSize * ((float) (value - this.mMin))) / ((float) (this.mMax - this.mMin));
        }

        public long convertToValue(float point) {
            return (long) (((float) this.mMin) + ((((float) (this.mMax - this.mMin)) * point) / this.mSize));
        }

        public long buildLabel(Resources res, SpannableStringBuilder builder, long value) {
            builder.replace(0, builder.length(), Long.toString(value));
            return value;
        }

        public float[] getTickPoints() {
            int tickCount = (int) ((this.mMax - this.mMin) / 5);
            float[] tickPoints = new float[(tickCount + 1)];
            for (int i = 0; i <= tickCount; i++) {
                tickPoints[i] = convertToPoint(this.mMax - (((long) i) * 5));
            }
            return tickPoints;
        }

        public int shouldAdjustAxis(long value) {
            return 0;
        }
    }

    public ChartBandwidthUsageView(Context context) {
        this(context, null, 0);
    }

    public ChartBandwidthUsageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartBandwidthUsageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mVertListener = new C07481();
        init(new TimeAxis(), new InvertedChartAxis(new BandwidthAxis()));
        this.mHandler = new C07492();
    }

    protected void onFinishInflate() {
        Log.d("@M_ChartBandwidthUsageView", "onFinishInflate");
        super.onFinishInflate();
        this.mGrid = (ChartBandwidthGridView) findViewById(R.id.grid);
        this.mSeries = (ChartBandwidthNetworkSeriesView) findViewById(R.id.series);
        this.mSweepLimit = (ChartSweepView) findViewById(R.id.sweep_limit);
        this.mSweepLimit.setMaxValue(10485760);
        this.mSweepLimit.addOnSweepListener(this.mVertListener);
        this.mSweepLimit.setDragInterval(2048);
        this.mGrid.init(this.mHoriz, this.mVert);
        this.mSeries.init(this.mHoriz, this.mVert);
        this.mSweepLimit.init(this.mVert);
        this.mSweepLimit.setEnabled(true);
        updateVertAxisBounds(this.mSweepLimit);
    }

    public void focusSweepLimit() {
        this.mSweepLimit.setEnabled(true);
    }

    public void setListener(BandwidthChartListener listener) {
        this.mListener = listener;
    }

    private void sendUpdateAxisDelayed(ChartSweepView sweep, boolean force) {
        if (force || !this.mHandler.hasMessages(100, sweep)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100, sweep), 250);
        }
    }

    private void clearUpdateAxisDelayed(ChartSweepView sweep) {
        this.mHandler.removeMessages(100, sweep);
    }

    public long getLimitBytes() {
        return this.mSweepLimit.getLabelValue();
    }

    public void setLimitBytes(long value) {
        this.mSweepLimit.setValue(value);
    }

    public void setLimitState(boolean state) {
        this.mSweepLimit.setVisibility(state ? 0 : 4);
    }

    public long getTotalUsedData() {
        return this.mSeries.getTotalUsedData();
    }

    public void updateVertAxisBounds(ChartSweepView activeSweep) {
        long max = this.mVertMax;
        long newMax = 0;
        if (activeSweep != null) {
            int adjustAxis = activeSweep.shouldAdjustAxis();
            if (adjustAxis > 0) {
                newMax = (11 * max) / 10;
            } else if (adjustAxis < 0) {
                newMax = (9 * max) / 10;
            } else {
                newMax = max;
            }
        }
        newMax = Math.max(Math.max(Math.max((this.mSeries.getMaxBytes() * 12) / 10, 524288), (this.mSweepLimit.getValue() * 11) / 10), newMax);
        if (newMax > 10485760) {
            newMax = 10485760;
        }
        if (newMax != this.mVertMax) {
            this.mVertMax = newMax;
            boolean changed = this.mVert.setBounds(0, newMax);
            this.mSweepLimit.setValidRange(0, newMax);
            if (changed) {
                this.mSeries.invalidatePath();
            }
            this.mGrid.invalidate();
            if (activeSweep != null) {
                activeSweep.updateValueFromPosition();
            }
            if (this.mSweepLimit != activeSweep) {
                layoutSweep(this.mSweepLimit);
            }
        }
    }

    public void setNetworkStates(NetworkStats networkStats) {
        this.mSeries.setNetworkStates(networkStats);
    }
}
