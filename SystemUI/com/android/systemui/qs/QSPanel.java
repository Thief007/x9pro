package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.qs.QSTile.State;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import java.util.ArrayList;
import java.util.Collection;

public class QSPanel extends ViewGroup {
    private BrightnessController mBrightnessController;
    private int mBrightnessPaddingTop;
    protected final View mBrightnessView;
    private Callback mCallback;
    private int mCellHeight;
    private int mCellWidth;
    private final QSDetailClipper mClipper;
    private boolean mClosingDetail;
    private int mColumns;
    private final Context mContext;
    private final View mDetail;
    private final ViewGroup mDetailContent;
    private final TextView mDetailDoneButton;
    private Record mDetailRecord;
    private final TextView mDetailSettingsButton;
    private int mDualTileUnderlap;
    private boolean mExpanded;
    private QSFooter mFooter;
    private boolean mGridContentVisible;
    private int mGridHeight;
    private final H mHandler;
    private final AnimatorListenerAdapter mHideGridContentWhenDone;
    private QSTileHost mHost;
    private int mLargeCellHeight;
    private int mLargeCellWidth;
    private boolean mListening;
    private int mPanelPaddingBottom;
    protected final ArrayList<TileRecord> mRecords;
    private final AnimatorListenerAdapter mTeardownDetailWhenDone;

    public interface Callback {
        void onScanStateChanged(boolean z);

        void onShowingDetail(DetailAdapter detailAdapter);

        void onToggleStateChanged(boolean z);
    }

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            if (msg.what == 1) {
                QSPanel qSPanel = QSPanel.this;
                Record record = (Record) msg.obj;
                if (msg.arg1 == 0) {
                    z = false;
                }
                qSPanel.handleShowDetail(record, z);
            } else if (msg.what == 2) {
                QSPanel.this.handleSetTileVisibility((View) msg.obj, msg.arg1);
            }
        }
    }

    private static class Record {
        DetailAdapter detailAdapter;
        View detailView;
        int x;
        int y;

        private Record() {
        }
    }

    protected static final class TileRecord extends Record {
        public int col;
        public boolean openingDetail;
        public int row;
        public boolean scanState;
        public QSTile<?> tile;
        public QSTileView tileView;

        protected TileRecord() {
            super();
        }
    }

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRecords = new ArrayList();
        this.mHandler = new H();
        this.mGridContentVisible = true;
        this.mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                QSPanel.this.mDetailContent.removeAllViews();
                QSPanel.this.setDetailRecord(null);
                QSPanel.this.mClosingDetail = false;
            }
        };
        this.mHideGridContentWhenDone = new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animation) {
                animation.removeListener(this);
                redrawTile();
            }

            public void onAnimationEnd(Animator animation) {
                if (QSPanel.this.mDetailRecord != null) {
                    QSPanel.this.setGridContentVisibility(false);
                    redrawTile();
                }
            }

            private void redrawTile() {
                if (QSPanel.this.mDetailRecord instanceof TileRecord) {
                    TileRecord tileRecord = (TileRecord) QSPanel.this.mDetailRecord;
                    tileRecord.openingDetail = false;
                    QSPanel.this.drawTile(tileRecord, tileRecord.tile.getState());
                }
            }
        };
        this.mContext = context;
        this.mDetail = LayoutInflater.from(context).inflate(R.layout.qs_detail, this, false);
        this.mDetailContent = (ViewGroup) this.mDetail.findViewById(16908290);
        this.mDetailSettingsButton = (TextView) this.mDetail.findViewById(16908314);
        this.mDetailDoneButton = (TextView) this.mDetail.findViewById(16908313);
        updateDetailText();
        this.mDetail.setVisibility(8);
        this.mDetail.setClickable(true);
        this.mBrightnessView = LayoutInflater.from(context).inflate(R.layout.quick_settings_brightness_dialog, this, false);
        this.mFooter = new QSFooter(this, context);
        addView(this.mDetail);
        addView(this.mBrightnessView);
        addView(this.mFooter.getView());
        this.mClipper = new QSDetailClipper(this.mDetail);
        updateResources();
        this.mBrightnessController = new BrightnessController(getContext(), (ImageView) findViewById(R.id.brightness_icon), (ToggleSlider) findViewById(R.id.brightness_slider));
        this.mDetailDoneButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                QSPanel.this.announceForAccessibility(QSPanel.this.mContext.getString(R.string.accessibility_desc_quick_settings));
                QSPanel.this.closeDetail();
            }
        });
    }

    private void updateDetailText() {
        this.mDetailDoneButton.setText(R.string.quick_settings_done);
        this.mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
    }

    public void setBrightnessMirror(BrightnessMirrorController c) {
        super.onFinishInflate();
        ToggleSlider brightnessSlider = (ToggleSlider) findViewById(R.id.brightness_slider);
        brightnessSlider.setMirror((ToggleSlider) c.getMirror().findViewById(R.id.brightness_slider));
        brightnessSlider.setMirrorController(c);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
        this.mFooter.setHost(host);
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public void updateResources() {
        Resources res = this.mContext.getResources();
        int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        this.mCellHeight = res.getDimensionPixelSize(R.dimen.qs_tile_height);
        this.mCellWidth = (int) (((float) this.mCellHeight) * 1.2f);
        this.mLargeCellHeight = res.getDimensionPixelSize(R.dimen.qs_dual_tile_height);
        this.mLargeCellWidth = (int) (((float) this.mLargeCellHeight) * 1.2f);
        this.mPanelPaddingBottom = res.getDimensionPixelSize(R.dimen.qs_panel_padding_bottom);
        this.mDualTileUnderlap = res.getDimensionPixelSize(R.dimen.qs_dual_tile_padding_vertical);
        this.mBrightnessPaddingTop = res.getDimensionPixelSize(R.dimen.qs_brightness_padding_top);
        if (this.mColumns != columns) {
            this.mColumns = columns;
            postInvalidate();
        }
        for (TileRecord r : this.mRecords) {
            r.tile.clearState();
        }
        if (this.mListening) {
            refreshAllTiles();
        }
        updateDetailText();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this.mDetailDoneButton, R.dimen.qs_detail_button_text_size);
        FontSizeUtils.updateFontSize(this.mDetailSettingsButton, R.dimen.qs_detail_button_text_size);
        int count = this.mRecords.size();
        for (int i = 0; i < count; i++) {
            View detailView = ((TileRecord) this.mRecords.get(i)).detailView;
            if (detailView != null) {
                detailView.dispatchConfigurationChanged(newConfig);
            }
        }
        this.mFooter.onConfigurationChanged();
    }

    public void setExpanded(boolean expanded) {
        if (this.mExpanded != expanded) {
            this.mExpanded = expanded;
            MetricsLogger.visibility(this.mContext, 111, this.mExpanded);
            if (this.mExpanded) {
                logTiles();
            } else {
                closeDetail();
            }
        }
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            for (TileRecord r : this.mRecords) {
                r.tile.setListening(this.mListening);
            }
            this.mFooter.setListening(this.mListening);
            if (this.mListening) {
                refreshAllTiles();
            }
            if (listening) {
                this.mBrightnessController.registerCallbacks();
            } else {
                this.mBrightnessController.unregisterCallbacks();
            }
        }
    }

    public void refreshAllTiles() {
        for (TileRecord r : this.mRecords) {
            r.tile.refreshState();
        }
        this.mFooter.refreshState();
    }

    public void showDetailAdapter(boolean show, DetailAdapter adapter, int[] locationInWindow) {
        int xInWindow = locationInWindow[0];
        int yInWindow = locationInWindow[1];
        this.mDetail.getLocationInWindow(locationInWindow);
        Record r = new Record();
        r.detailAdapter = adapter;
        r.x = xInWindow - locationInWindow[0];
        r.y = yInWindow - locationInWindow[1];
        locationInWindow[0] = xInWindow;
        locationInWindow[1] = yInWindow;
        showDetail(show, r);
    }

    private void showDetail(boolean show, Record r) {
        int i;
        H h = this.mHandler;
        if (show) {
            i = 1;
        } else {
            i = 0;
        }
        h.obtainMessage(1, i, 0, r).sendToTarget();
    }

    private void setTileVisibility(View v, int visibility) {
        this.mHandler.obtainMessage(2, visibility, 0, v).sendToTarget();
    }

    private void handleSetTileVisibility(View v, int visibility) {
        if (visibility == 0 && !this.mGridContentVisible) {
            visibility = 4;
        }
        if (visibility != v.getVisibility()) {
            v.setVisibility(visibility);
        }
    }

    public void setTiles(Collection<QSTile<?>> tiles) {
        for (TileRecord record : this.mRecords) {
            removeView(record.tileView);
        }
        this.mRecords.clear();
        for (QSTile<?> tile : tiles) {
            addTile(tile);
        }
        if (isShowingDetail()) {
            this.mDetail.bringToFront();
        }
    }

    private void drawTile(TileRecord r, State state) {
        setTileVisibility(r.tileView, state.visible ? 0 : 8);
        r.tileView.onStateChanged(state);
    }

    private void addTile(QSTile<?> tile) {
        final TileRecord r = new TileRecord();
        r.tile = tile;
        r.tileView = tile.createTileView(this.mContext);
        r.tileView.setVisibility(8);
        com.android.systemui.qs.QSTile.Callback callback = new com.android.systemui.qs.QSTile.Callback() {
            public void onStateChanged(State state) {
                if (!r.openingDetail) {
                    QSPanel.this.drawTile(r, state);
                }
            }

            public void onShowDetail(boolean show) {
                QSPanel.this.showDetail(show, r);
            }

            public void onToggleStateChanged(boolean state) {
                if (QSPanel.this.mDetailRecord == r) {
                    QSPanel.this.fireToggleStateChanged(state);
                }
            }

            public void onScanStateChanged(boolean state) {
                r.scanState = state;
                if (QSPanel.this.mDetailRecord == r) {
                    QSPanel.this.fireScanStateChanged(r.scanState);
                }
            }

            public void onAnnouncementRequested(CharSequence announcement) {
                QSPanel.this.announceForAccessibility(announcement);
            }
        };
        r.tile.setCallback(callback);
        r.tileView.init(new OnClickListener() {
            public void onClick(View v) {
                r.tile.click();
            }
        }, new OnClickListener() {
            public void onClick(View v) {
                r.tile.secondaryClick();
            }
        }, new OnLongClickListener() {
            public boolean onLongClick(View v) {
                r.tile.longClick();
                return true;
            }
        });
        r.tile.setListening(this.mListening);
        callback.onStateChanged(r.tile.getState());
        r.tile.refreshState();
        this.mRecords.add(r);
        addView(r.tileView);
    }

    public boolean isShowingDetail() {
        return this.mDetailRecord != null;
    }

    public void closeDetail() {
        showDetail(false, this.mDetailRecord);
    }

    public boolean isClosingDetail() {
        return this.mClosingDetail;
    }

    public int getGridHeight() {
        return this.mGridHeight;
    }

    private void handleShowDetail(Record r, boolean show) {
        if (r instanceof TileRecord) {
            handleShowDetailTile((TileRecord) r, show);
            return;
        }
        int x = 0;
        int y = 0;
        if (r != null) {
            x = r.x;
            y = r.y;
        }
        handleShowDetailImpl(r, show, x, y);
    }

    private void handleShowDetailTile(TileRecord r, boolean show) {
        if ((this.mDetailRecord != null) != show || this.mDetailRecord != r) {
            if (show) {
                r.detailAdapter = r.tile.getDetailAdapter();
                if (r.detailAdapter == null) {
                    return;
                }
            }
            r.tile.setDetailListening(show);
            handleShowDetailImpl(r, show, r.tileView.getLeft() + (r.tileView.getWidth() / 2), r.tileView.getTop() + (r.tileView.getHeight() / 2));
        }
    }

    private void handleShowDetailImpl(Record r, boolean show, int x, int y) {
        boolean z;
        if (this.mDetailRecord != null) {
            z = true;
        } else {
            z = false;
        }
        boolean visibleDiff = z != show;
        if (visibleDiff || this.mDetailRecord != r) {
            AnimatorListener listener;
            DetailAdapter detailAdapter = null;
            if (show) {
                detailAdapter = r.detailAdapter;
                r.detailView = detailAdapter.createDetailView(this.mContext, r.detailView, this.mDetailContent);
                if (r.detailView == null) {
                    throw new IllegalStateException("Must return detail view");
                }
                final Intent settingsIntent = detailAdapter.getSettingsIntent();
                this.mDetailSettingsButton.setVisibility(settingsIntent != null ? 0 : 8);
                this.mDetailSettingsButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        QSPanel.this.mHost.startActivityDismissingKeyguard(settingsIntent);
                    }
                });
                this.mDetailContent.removeAllViews();
                this.mDetail.bringToFront();
                this.mDetailContent.addView(r.detailView);
                MetricsLogger.visible(this.mContext, detailAdapter.getMetricsCategory());
                announceForAccessibility(this.mContext.getString(R.string.accessibility_quick_settings_detail, new Object[]{this.mContext.getString(detailAdapter.getTitle())}));
                setDetailRecord(r);
                listener = this.mHideGridContentWhenDone;
                if ((r instanceof TileRecord) && visibleDiff) {
                    ((TileRecord) r).openingDetail = true;
                }
            } else {
                if (this.mDetailRecord != null) {
                    MetricsLogger.hidden(this.mContext, this.mDetailRecord.detailAdapter.getMetricsCategory());
                }
                this.mClosingDetail = true;
                setGridContentVisibility(true);
                listener = this.mTeardownDetailWhenDone;
                fireScanStateChanged(false);
            }
            sendAccessibilityEvent(32);
            if (!show) {
                detailAdapter = null;
            }
            fireShowingDetail(detailAdapter);
            if (visibleDiff) {
                this.mClipper.animateCircularClip(x, y, show, listener);
            }
        }
    }

    private void setGridContentVisibility(boolean visible) {
        int newVis = visible ? 0 : 4;
        for (int i = 0; i < this.mRecords.size(); i++) {
            TileRecord tileRecord = (TileRecord) this.mRecords.get(i);
            if (tileRecord.tileView.getVisibility() != 8) {
                tileRecord.tileView.setVisibility(newVis);
            }
        }
        this.mBrightnessView.setVisibility(newVis);
        if (this.mGridContentVisible != visible) {
            MetricsLogger.visibility(this.mContext, 111, newVis);
        }
        this.mGridContentVisible = visible;
    }

    private void logTiles() {
        for (int i = 0; i < this.mRecords.size(); i++) {
            TileRecord tileRecord = (TileRecord) this.mRecords.get(i);
            if (tileRecord.tile.getState().visible) {
                MetricsLogger.visible(this.mContext, tileRecord.tile.getMetricsCategory());
            }
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        this.mBrightnessView.measure(exactly(width), 0);
        int brightnessHeight = this.mBrightnessView.getMeasuredHeight() + this.mBrightnessPaddingTop;
        this.mFooter.getView().measure(exactly(width), 0);
        int r = -1;
        int c = -1;
        int rows = 0;
        boolean rowIsDual = false;
        for (TileRecord record : this.mRecords) {
            if (record.tileView.getVisibility() != 8) {
                if (r == -1 || c == this.mColumns - 1 || r9 != record.tile.supportsDualTargets()) {
                    r++;
                    c = 0;
                    rowIsDual = record.tile.supportsDualTargets();
                } else {
                    c++;
                }
                record.row = r;
                record.col = c;
                rows = r + 1;
            }
        }
        View previousView = this.mBrightnessView;
        for (TileRecord record2 : this.mRecords) {
            if (record2.tileView.setDual(record2.tile.supportsDualTargets())) {
                record2.tileView.handleStateChanged(record2.tile.getState());
            }
            if (record2.tileView.getVisibility() != 8) {
                record2.tileView.measure(exactly(record2.row == 0 ? this.mLargeCellWidth : this.mCellWidth), exactly(record2.row == 0 ? this.mLargeCellHeight : this.mCellHeight));
                previousView = record2.tileView.updateAccessibilityOrder(previousView);
            }
        }
        int h = rows == 0 ? brightnessHeight : getRowTop(rows) + this.mPanelPaddingBottom;
        if (this.mFooter.hasFooter()) {
            h += this.mFooter.getView().getMeasuredHeight();
        }
        this.mDetail.measure(exactly(width), 0);
        if (this.mDetail.getMeasuredHeight() < h) {
            this.mDetail.measure(exactly(width), exactly(h));
        }
        this.mGridHeight = h;
        setMeasuredDimension(width, Math.max(h, this.mDetail.getMeasuredHeight()));
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getWidth();
        this.mBrightnessView.layout(0, this.mBrightnessPaddingTop, this.mBrightnessView.getMeasuredWidth(), this.mBrightnessPaddingTop + this.mBrightnessView.getMeasuredHeight());
        boolean isRtl = getLayoutDirection() == 1;
        for (TileRecord record : this.mRecords) {
            if (record.tileView.getVisibility() != 8) {
                int right;
                int cols = getColumnCount(record.row);
                int cw = record.row == 0 ? this.mLargeCellWidth : this.mCellWidth;
                int left = (record.col * cw) + ((record.col + 1) * ((w - (cw * cols)) / (cols + 1)));
                int top = getRowTop(record.row);
                int tileWith = record.tileView.getMeasuredWidth();
                if (isRtl) {
                    right = w - left;
                    left = right - tileWith;
                } else {
                    right = left + tileWith;
                }
                record.tileView.layout(left, top, right, record.tileView.getMeasuredHeight() + top);
            }
        }
        this.mDetail.layout(0, 0, this.mDetail.getMeasuredWidth(), Math.max(this.mDetail.getMeasuredHeight(), getMeasuredHeight()));
        if (this.mFooter.hasFooter()) {
            View footer = this.mFooter.getView();
            footer.layout(0, getMeasuredHeight() - footer.getMeasuredHeight(), footer.getMeasuredWidth(), getMeasuredHeight());
        }
    }

    private int getRowTop(int row) {
        if (row <= 0) {
            return this.mBrightnessView.getMeasuredHeight() + this.mBrightnessPaddingTop;
        }
        return (((this.mBrightnessView.getMeasuredHeight() + this.mBrightnessPaddingTop) + this.mLargeCellHeight) - this.mDualTileUnderlap) + ((row - 1) * this.mCellHeight);
    }

    private int getColumnCount(int row) {
        int cols = 0;
        for (TileRecord record : this.mRecords) {
            if (record.tileView.getVisibility() != 8 && record.row == row) {
                cols++;
            }
        }
        return cols;
    }

    private void fireShowingDetail(DetailAdapter detail) {
        if (this.mCallback != null) {
            this.mCallback.onShowingDetail(detail);
        }
    }

    private void fireToggleStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onToggleStateChanged(state);
        }
    }

    private void fireScanStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onScanStateChanged(state);
        }
    }

    private void setDetailRecord(Record r) {
        if (r != this.mDetailRecord) {
            boolean z;
            this.mDetailRecord = r;
            if (this.mDetailRecord instanceof TileRecord) {
                z = ((TileRecord) this.mDetailRecord).scanState;
            } else {
                z = false;
            }
            fireScanStateChanged(z);
        }
    }
}
