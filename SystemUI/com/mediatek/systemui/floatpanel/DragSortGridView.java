package com.mediatek.systemui.floatpanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.MeasureSpec;
import android.view.View.OnDragListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.systemui.R;

public class DragSortGridView extends GridView {
    private static final SparseArray<String> DRAG_EVENT_ACTION = new SparseArray();
    private static View sDragView = null;
    private static View sOriginDragView = null;
    private FloatAppAdapter mAdapter;
    private int mAnimationCount;
    private int mDragDirection;
    private boolean mDragEnabled;
    private boolean mDragFinished;
    private OnDragListener mDragListener;
    private int mDragMode;
    private boolean mDragSucceed;
    private float mDragWeight;
    private boolean mEdgeScrollForceStop;
    private Animation mFadeOutAnimation;
    private int mFixedColumnNum;
    private Handler mHandler;
    private boolean mInDragState;
    private boolean mInStrechMode;
    private int mInsertedPos;
    private boolean mInternalDrag;
    private final OnItemLongClickListener mItemLongClickListener;
    private int mLastDragLocationY;
    private int mLastDraggingPosition;
    private boolean mMoveInView;
    private boolean mMovingChildViews;
    private OnReorderingListener mOnReorderingListener;
    private PositionInfo mReorderingPositions;
    private ScrollRunnable mScrollRunnable;
    private int mScrollerState;
    private int mSmoothScrollAmountAtEdge;
    private int mTouchSlot;
    private boolean mVerticalDragScrollEnable;
    private Rect mVisibleRect;
    private boolean mWaitingForInsertedItem;
    private boolean mWaitingForRemovedItem;

    private static class FloatDragShadowBuilder extends DragShadowBuilder {
        private static Drawable sShadow;

        public FloatDragShadowBuilder(View view) {
            super(view);
            sShadow = new ColorDrawable(-3355444);
        }

        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            View view = getView();
            if (view != null) {
                int width = view.getWidth();
                int height = view.getHeight();
                shadowSize.set((int) (((float) width) * 1.4f), (int) (((float) height) * 1.4f));
                shadowTouchPoint.set(shadowSize.x / 2, (shadowSize.y * 3) / 4);
                Log.d("DragGridView", "onProvideShadowMetrics: width = " + width + ",height = " + height + ",shadowSize = " + shadowSize + ",shadowTouchPoint = " + shadowTouchPoint + ", view = " + getView());
                return;
            }
            super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);
        }

        public void onDrawShadow(Canvas canvas) {
            View view = getView();
            if (view != null) {
                Drawable drawable = null;
                if (view instanceof TextView) {
                    drawable = ((TextView) view).getCompoundDrawables()[1];
                }
                Log.d("DragGridView", "onDrawShadow: vis = " + view.getVisibility() + ",drawable = " + drawable + ", view = " + view);
                view.draw(canvas);
                if (drawable != null) {
                    drawZoomedIcon(canvas, drawable, 1.4f);
                    return;
                }
                return;
            }
            Log.e("DragGridView", "Asked to draw drag shadow but no view");
        }

        private void drawZoomedIcon(Canvas canvas, Drawable drawable, float zoomRatio) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            Bitmap oldbmp = drawableToBitmap(drawable);
            Log.d("DragGridView", "drawZoomedIcon: width = " + width + ",height = " + height + ", oldbmp = " + oldbmp + ", zoomRatio = " + zoomRatio);
            Matrix matrix = new Matrix();
            matrix.postScale(zoomRatio, zoomRatio);
            Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height, matrix, true);
            Log.d("DragGridView", "drawZoomedIcon: newbmp width = " + newbmp.getWidth() + ",newbmp height = " + newbmp.getHeight());
            canvas.drawBitmap(newbmp, 0.0f, 0.0f, new Paint());
            if (!oldbmp.isRecycled()) {
                oldbmp.recycle();
            }
        }

        private Bitmap drawableToBitmap(Drawable drawable) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, drawable.getOpacity() != -1 ? Config.ARGB_8888 : Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        }
    }

    private class MoveViewAnimationListener implements AnimationListener {
        private int mNewX;
        private int mNewY;
        private View mTarget;

        public MoveViewAnimationListener(View view, int x, int y) {
            this.mTarget = view;
            this.mNewX = x;
            this.mNewY = y;
        }

        public void onAnimationEnd(Animation animation) {
            this.mTarget.layout(this.mNewX, this.mNewY, this.mNewX + this.mTarget.getWidth(), this.mNewY + this.mTarget.getHeight());
            this.mTarget.clearAnimation();
            DragSortGridView.this.mMovingChildViews = false;
            DragSortGridView dragSortGridView = DragSortGridView.this;
            dragSortGridView.mAnimationCount = dragSortGridView.mAnimationCount - 1;
            if (DragSortGridView.this.mAnimationCount == 0 && DragSortGridView.this.mDragFinished) {
                DragSortGridView.this.requestLayout();
            }
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationStart(Animation animation) {
            DragSortGridView dragSortGridView = DragSortGridView.this;
            dragSortGridView.mAnimationCount = dragSortGridView.mAnimationCount + 1;
        }
    }

    public interface OnReorderingListener {
        void onItemSwitched(int i);

        void onReordering(int i, int i2);
    }

    private class PositionInfo {
        private int[] mPositions;

        public PositionInfo(int size) {
            this.mPositions = new int[size];
            for (int i = 0; i < size; i++) {
                this.mPositions[i] = i;
            }
        }

        public int get(int position) {
            return this.mPositions[position];
        }

        public void reorder(int from, int to) {
            if (from != to) {
                int[] array = this.mPositions;
                int fromValue;
                int i;
                if (from < to) {
                    fromValue = array[from];
                    for (i = from; i < to; i++) {
                        array[i] = array[i + 1];
                    }
                    array[to] = fromValue;
                } else {
                    fromValue = array[from];
                    for (i = from; i > to; i--) {
                        array[i] = array[i - 1];
                    }
                    array[to] = fromValue;
                }
            }
        }

        public int getValueIndex(int value) {
            int[] array = this.mPositions;
            int size = array.length;
            for (int i = 0; i < size; i++) {
                if (value == array[i]) {
                    return i;
                }
            }
            return -1;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            for (int num : this.mPositions) {
                builder.append(Integer.toString(num)).append(",");
            }
            builder.append("]");
            return builder.toString();
        }
    }

    private class ScrollRunnable implements Runnable {
        private ScrollRunnable() {
        }

        public void run() {
            int flag = DragSortGridView.this.mDragDirection == 1 ? 1 : -1;
            Log.d("DragGridView", "ScrollRunnable run, mDragDirection=" + DragSortGridView.this.mDragDirection + ", falg=" + flag + ", mLastDragLocationY=" + DragSortGridView.this.mLastDragLocationY + ", mScrollerState=" + DragSortGridView.this.mScrollerState + ", mEdgeScrollForceStop=" + DragSortGridView.this.mEdgeScrollForceStop + ", mInDragState=" + DragSortGridView.this.mInDragState);
            int scrollDistance = (int) (((float) (DragSortGridView.this.mSmoothScrollAmountAtEdge * flag)) + (DragSortGridView.this.mDragWeight * DragSortGridView.this.mDragWeight));
            if (DragSortGridView.this.mScrollerState != 0) {
                DragSortGridView.this.mScrollerState = 0;
                if (!DragSortGridView.this.mEdgeScrollForceStop) {
                    Log.d("DragGridView", "ScrollRunnable run smooth scroll scrollDistance=" + scrollDistance);
                    DragSortGridView.this.smoothScrollBy(scrollDistance, 500);
                }
            } else {
                Log.d("DragGridView", "ScrollRunnable run idle state, mScrollerState=" + DragSortGridView.this.mScrollerState + ", mEdgeScrollForceStop=" + DragSortGridView.this.mEdgeScrollForceStop);
            }
            if (DragSortGridView.this.mInDragState && !DragSortGridView.this.mEdgeScrollForceStop) {
                DragSortGridView.this.scrollIfNeeded(DragSortGridView.this.mLastDragLocationY, DragSortGridView.this.mLastDragLocationY);
            }
        }
    }

    static {
        DRAG_EVENT_ACTION.put(1, "ACTION_DRAG_STARTED");
        DRAG_EVENT_ACTION.put(5, "ACTION_DRAG_ENTERED");
        DRAG_EVENT_ACTION.put(2, "ACTION_DRAG_LOCATION");
        DRAG_EVENT_ACTION.put(6, "ACTION_DRAG_EXITED");
        DRAG_EVENT_ACTION.put(3, "ACTION_DROP");
        DRAG_EVENT_ACTION.put(4, "ACTION_DRAG_ENDED");
    }

    public DragSortGridView(Context context) {
        this(context, null);
    }

    public DragSortGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragSortGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDragMode = 0;
        this.mMovingChildViews = false;
        this.mInternalDrag = false;
        this.mMoveInView = false;
        this.mDragFinished = false;
        this.mWaitingForInsertedItem = false;
        this.mWaitingForRemovedItem = false;
        this.mDragEnabled = true;
        this.mInsertedPos = -1;
        this.mAnimationCount = 0;
        this.mDragListener = new OnDragListener() {
            public boolean onDrag(View v, DragEvent event) {
                Object tag;
                int action = event.getAction();
                int x = Math.round(event.getX());
                int y = Math.round(event.getY());
                String str = "DragGridView";
                StringBuilder append = new StringBuilder().append("onDrag event action:").append((String) DragSortGridView.DRAG_EVENT_ACTION.get(action)).append(",x = ").append(x).append(",y = ").append(y).append(",mLastDraggingPosition = ").append(DragSortGridView.this.mLastDraggingPosition).append(", mWaitingForInsertedItem = ").append(DragSortGridView.this.mWaitingForInsertedItem).append(", mWaitingForRemovedItem = ").append(DragSortGridView.this.mWaitingForRemovedItem).append(", mInternalDrag = ").append(DragSortGridView.this.mInternalDrag).append(", mMoveInView = ").append(DragSortGridView.this.mMoveInView).append(",mMovingChildViews = ").append(DragSortGridView.this.mMovingChildViews).append(", sDragView = ");
                if (DragSortGridView.sDragView != null) {
                    tag = DragSortGridView.sDragView.getTag();
                } else {
                    tag = "null";
                }
                append = append.append(tag);
                if (DragSortGridView.sOriginDragView != null) {
                    tag = DragSortGridView.sOriginDragView.getTag();
                } else {
                    tag = "null";
                }
                append = append.append(tag).append(", drag view's vis = ");
                if (DragSortGridView.sDragView != null) {
                    tag = Integer.valueOf(DragSortGridView.sDragView.getVisibility());
                } else {
                    tag = "null";
                }
                append = append.append(tag).append(", origin drag view's vis = ");
                if (DragSortGridView.sOriginDragView != null) {
                    tag = Integer.valueOf(DragSortGridView.sOriginDragView.getVisibility());
                } else {
                    tag = "null";
                }
                Log.d(str, append.append(tag).append(", this = ").append(DragSortGridView.this).toString());
                int pos;
                int newPosition;
                switch (action) {
                    case 1:
                        if (DragSortGridView.this.mInternalDrag) {
                            DragSortGridView.this.mReorderingPositions = new PositionInfo(DragSortGridView.this.getAdapter().getCount());
                            DragSortGridView.this.mLastDraggingPosition = ((Integer) event.getLocalState()).intValue();
                            DragSortGridView.sDragView = DragSortGridView.this.getView(DragSortGridView.this.mLastDraggingPosition);
                            DragSortGridView.sOriginDragView = DragSortGridView.sDragView;
                            if (DragSortGridView.this.getView(DragSortGridView.this.mLastDraggingPosition) != null) {
                                DragSortGridView.this.getView(DragSortGridView.this.mLastDraggingPosition).startAnimation(DragSortGridView.this.mFadeOutAnimation);
                                break;
                            }
                        }
                        break;
                    case 2:
                        DragSortGridView.this.scrollIfNeeded(DragSortGridView.this.mLastDragLocationY, y);
                        DragSortGridView.this.mLastDragLocationY = y;
                        if (!DragSortGridView.this.mWaitingForInsertedItem && DragSortGridView.this.mMoveInView && !DragSortGridView.this.mMovingChildViews) {
                            pos = DragSortGridView.this.findInsertPosition(x, y);
                            newPosition = pos == -1 ? -1 : DragSortGridView.this.mReorderingPositions.getValueIndex(pos);
                            Log.d("DragGridView", "ACTION_DRAG_LOCATION inner, mLastDraggingPosition = " + DragSortGridView.this.mLastDraggingPosition + ", mInternalDrag = " + DragSortGridView.this.mInternalDrag + ", pos = " + pos + ",newPosition:" + newPosition + ", item = " + DragSortGridView.sDragView.getTag());
                            if (!(-1 == newPosition || DragSortGridView.this.mLastDraggingPosition == newPosition)) {
                                DragSortGridView.this.reorderViews(DragSortGridView.this.mLastDraggingPosition, newPosition);
                                DragSortGridView.this.mReorderingPositions.reorder(DragSortGridView.this.mLastDraggingPosition, newPosition);
                                DragSortGridView.this.mLastDraggingPosition = newPosition;
                                DragSortGridView.this.mMovingChildViews = true;
                                DragSortGridView.this.mDragSucceed = false;
                                Log.d("DragGridView", "reordering positions:" + DragSortGridView.this.mReorderingPositions);
                                break;
                            }
                        }
                        Log.d("DragGridView", "ACTION_DRAG_LOCATION canceld: mMovingChildViews = " + DragSortGridView.this.mMovingChildViews + ", mMoveInView = " + DragSortGridView.this.mMoveInView + ",mWaitingForInsertedItem = " + DragSortGridView.this.mWaitingForInsertedItem + ", this = " + this);
                        break;
                        break;
                    case 3:
                        DragSortGridView.this.dumpLayoutPosition();
                        DragSortGridView.this.mDragSucceed = true;
                        DragSortGridView.this.mHandler.removeCallbacks(DragSortGridView.this.mScrollRunnable);
                        DragSortGridView.this.mScrollerState = 0;
                        if (DragSortGridView.this.mMoveInView) {
                            DragSortGridView.this.mMoveInView = false;
                            int oldPosition = ((Integer) event.getLocalState()).intValue();
                            pos = DragSortGridView.this.findInsertPosition(x, y);
                            if (pos == -1) {
                                newPosition = DragSortGridView.this.mAdapter.getCount() - 1;
                            } else {
                                newPosition = DragSortGridView.this.mReorderingPositions.getValueIndex(pos);
                            }
                            View lastDragView = DragSortGridView.this.getView(DragSortGridView.this.mLastDraggingPosition);
                            if (lastDragView != null) {
                                lastDragView.clearAnimation();
                            }
                            Log.d("DragGridView", "ACTION_DROP pos:" + pos + ",newPosition:" + newPosition + ", oldPosition = " + oldPosition + ", lastDragView = " + lastDragView + ",mInsertedPos=" + DragSortGridView.this.mInsertedPos + ", this = " + this);
                            DragSortGridView.this.makeDragViewVisible();
                            if (DragSortGridView.this.mOnReorderingListener != null) {
                                if (DragSortGridView.this.mInternalDrag) {
                                    if (newPosition != -1 && newPosition != oldPosition) {
                                        DragSortGridView.this.mOnReorderingListener.onReordering(oldPosition, newPosition);
                                        break;
                                    }
                                    DragSortGridView.this.mAdapter.notifyDataSetChanged();
                                    break;
                                }
                                DragSortGridView.this.mOnReorderingListener.onItemSwitched(oldPosition);
                                if (newPosition == -1) {
                                    DragSortGridView.this.mAdapter.notifyDataSetChanged();
                                    break;
                                }
                                DragSortGridView.this.mOnReorderingListener.onReordering(DragSortGridView.this.mInsertedPos, newPosition);
                                break;
                            }
                        }
                        break;
                    case 4:
                        if (!DragSortGridView.this.mDragSucceed) {
                            DragSortGridView.this.mAdapter.notifyDataSetChanged();
                        }
                        DragSortGridView.this.makeDragViewVisible();
                        DragSortGridView.this.dumpLayoutPosition();
                        DragSortGridView.this.mDragMode = 0;
                        DragSortGridView.this.mWaitingForInsertedItem = false;
                        DragSortGridView.this.mWaitingForRemovedItem = false;
                        DragSortGridView.this.mInternalDrag = false;
                        DragSortGridView.this.mMoveInView = false;
                        DragSortGridView.this.mDragFinished = true;
                        DragSortGridView.sDragView = null;
                        DragSortGridView.sOriginDragView = null;
                        DragSortGridView.this.mInsertedPos = -1;
                        DragSortGridView.this.clearAllAnimations();
                        break;
                    case 5:
                        DragSortGridView.this.mLastDragLocationY = y;
                        DragSortGridView.this.mInDragState = true;
                        if (DragSortGridView.this.mInternalDrag) {
                            DragSortGridView.this.mDragMode = 1;
                            DragSortGridView.sDragView = DragSortGridView.sOriginDragView;
                            DragSortGridView.this.mDragFinished = false;
                            DragSortGridView.this.mAnimationCount = 0;
                        } else {
                            DragSortGridView.this.mDragMode = 2;
                            if (DragSortGridView.this.getChildCount() == 0) {
                                pos = 0;
                            } else if (DragSortGridView.this.mAdapter.getCount() > 24 || DragSortGridView.this.mAdapter.getCount() == 24) {
                                pos = DragSortGridView.this.findInsertPosition(x, y);
                                if (pos == -1) {
                                    int ChildBottom = 0;
                                    View lastChildView = DragSortGridView.this.getChildAt(DragSortGridView.this.getChildCount() - 1);
                                    if (lastChildView != null) {
                                        ChildBottom = lastChildView.getBottom();
                                    }
                                    Log.d("DragGridView", "ACTION_DRAG_ENTERED: ChildBottom = " + ChildBottom);
                                    int lastChildBottom = ChildBottom;
                                    int gridViewBottom = DragSortGridView.this.getBottom();
                                    int verticalSpace = DragSortGridView.this.getVerticalSpacing();
                                    Log.d("DragGridView", "ACTION_DRAG_ENTERED: pos = " + pos + ", lastChildBottom = " + lastChildBottom + ", gridViewBottom = " + gridViewBottom + ", verticalSpace = " + verticalSpace + ", lastChildView = " + lastChildView);
                                    if (lastChildBottom >= gridViewBottom || gridViewBottom - lastChildBottom > verticalSpace + 10) {
                                        pos = DragSortGridView.this.getFirstVisiblePosition() + DragSortGridView.this.getChildCount();
                                    } else {
                                        pos = (DragSortGridView.this.getFirstVisiblePosition() + DragSortGridView.this.getChildCount()) - 1;
                                    }
                                }
                            } else {
                                pos = DragSortGridView.this.mAdapter.getCount();
                            }
                            FloatAppItem floatItem = (FloatAppItem) DragSortGridView.sDragView.getTag();
                            floatItem.visible = false;
                            Log.d("DragGridView", "ACTION_DRAG_ENTERED lastPosition:" + DragSortGridView.this.mLastDraggingPosition + ", pos = " + pos + ",floatItem = " + floatItem + ",getTop() = " + DragSortGridView.this.getTop() + ", this = " + this);
                            DragSortGridView.this.mAdapter.addItem((FloatAppItem) DragSortGridView.sDragView.getTag(), pos);
                            DragSortGridView.this.mReorderingPositions = new PositionInfo(DragSortGridView.this.mAdapter.getCount());
                            DragSortGridView.this.mInsertedPos = pos;
                            DragSortGridView.this.mLastDraggingPosition = pos;
                            DragSortGridView.this.mWaitingForInsertedItem = true;
                        }
                        DragSortGridView.this.mMoveInView = true;
                        break;
                    case 6:
                        DragSortGridView.this.mInDragState = false;
                        DragSortGridView.this.mHandler.removeCallbacks(DragSortGridView.this.mScrollRunnable);
                        DragSortGridView.this.mScrollerState = 0;
                        DragSortGridView.this.mMoveInView = false;
                        if (!DragSortGridView.this.mInternalDrag) {
                            DragSortGridView.this.mDragMode = 0;
                            DragSortGridView.this.mWaitingForInsertedItem = false;
                            DragSortGridView.this.clearAllAnimations();
                            DragSortGridView.this.mAdapter.removeItem(DragSortGridView.this.mInsertedPos);
                            DragSortGridView.this.mWaitingForRemovedItem = true;
                            break;
                        }
                        DragSortGridView.this.mDragMode = 2;
                        break;
                }
                return DragSortGridView.this.mDragEnabled;
            }
        };
        this.mItemLongClickListener = new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.d("DragGridView", "onItemLongClick: position = " + position + ", mDragEnabled = " + DragSortGridView.this.mDragEnabled + ", tag = " + view.getTag() + ", view = " + view);
                if (DragSortGridView.this.mDragEnabled) {
                    view.startDrag(null, new FloatDragShadowBuilder(view), Integer.valueOf(position), 0);
                    DragSortGridView.this.mInternalDrag = true;
                }
                return DragSortGridView.this.mDragEnabled;
            }
        };
        if (this.mFadeOutAnimation == null) {
            this.mFadeOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.float_panel_item_fade_out);
        }
        setOnItemLongClickListener(this.mItemLongClickListener);
        setOnDragListener(this.mDragListener);
        this.mInStrechMode = false;
        this.mFixedColumnNum = context.getResources().getInteger(R.integer.float_panel_num_columns);
        DisplayMetrics metric = getResources().getDisplayMetrics();
        this.mSmoothScrollAmountAtEdge = (int) ((metric.density * 200.0f) + 0.5f);
        this.mTouchSlot = (int) ((metric.density * 75.0f) + 0.5f);
        this.mHandler = new Handler();
        this.mScrollerState = 0;
        this.mScrollRunnable = new ScrollRunnable();
        this.mInDragState = false;
        this.mVerticalDragScrollEnable = false;
        this.mVisibleRect = new Rect();
    }

    public void setEnableStrechMode(boolean enable) {
        this.mInStrechMode = true;
    }

    public void setEnableVerticalDragScroll(boolean enable) {
        this.mVerticalDragScrollEnable = enable;
    }

    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        this.mAdapter = (FloatAppAdapter) adapter;
        Log.d("DragGridView", "setAdapter: count = " + this.mAdapter.getCount() + ", mAdapter=" + this.mAdapter + ", this=" + this);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == 0 && this.mInStrechMode && this.mAdapter != null) {
            int itemCount = this.mAdapter.getCount();
            int minWidth = getResources().getDimensionPixelSize(R.dimen.float_bottom_min_width);
            int requestColumnWidth = getResources().getDimensionPixelSize(R.dimen.gridview_column_width);
            int spaceLeftOver = minWidth - ((this.mFixedColumnNum + 2) * requestColumnWidth);
            Log.d("DragGridView", "onMeasure 2: newHorizontalSpacing = " + getHorizontalSpacing() + ", spaceLeftOver = " + spaceLeftOver + ",minWidth = " + minWidth + ", requestColumnWidth = " + requestColumnWidth + ",columnWidth = " + getColumnWidth());
            setHorizontalSpacing(spaceLeftOver / (this.mFixedColumnNum - 1));
            int newWidth = ((((getPaddingLeft() + getPaddingRight()) + (itemCount * requestColumnWidth)) + (getRequestedHorizontalSpacing() * (itemCount - 1))) + (requestColumnWidth * 2)) + getVerticalScrollbarWidth();
            if (minWidth > newWidth) {
                newWidth = minWidth;
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(newWidth, 1073741824), heightMeasureSpec);
            Log.d("DragGridView", "onMeasure: measuredWidth = " + getMeasuredWidth() + ", measuredHeight = " + getMeasuredHeight() + ", itemCount = " + itemCount + ", newWidth = " + newWidth + ",minWidth = " + minWidth + ",numColumns = " + getNumColumns() + ",columnWidth = " + getColumnWidth() + ",newHorizontalSpacing = " + getHorizontalSpacing() + ", this = " + this);
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("DragGridView", "onMeasure else: measuredWidth = " + getMeasuredWidth() + ", measuredHeight = " + getMeasuredHeight() + ", itemCount = " + getCount() + ", newHorizontalSpacing = " + getHorizontalSpacing() + ",numColumns = " + getNumColumns() + ",columnWidth = " + getColumnWidth() + ", this = " + this);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!this.mMoveInView || this.mWaitingForInsertedItem || this.mWaitingForRemovedItem) {
            super.onLayout(changed, l, t, r, b);
        }
        if (this.mWaitingForInsertedItem) {
            sOriginDragView = sDragView;
            sDragView = getView(this.mLastDraggingPosition);
            if (sDragView != null) {
                Log.d("DragGridView", "onLayout: sDragView vis = " + sDragView.getVisibility());
            }
            this.mWaitingForInsertedItem = false;
        } else if (this.mWaitingForRemovedItem) {
            sDragView = sOriginDragView;
            Log.d("DragGridView", "onLayout: sDragView vis = " + sDragView.getVisibility() + ", tag = " + sDragView.getTag());
            this.mWaitingForRemovedItem = false;
        }
    }

    private void makeDragViewVisible() {
        FloatAppItem floatItem;
        if (sDragView != null) {
            floatItem = (FloatAppItem) sDragView.getTag();
            floatItem.visible = true;
            sDragView.clearAnimation();
            if (sDragView.getVisibility() == 4) {
                Log.d("DragGridView", "Reset drag view visible: floatItem = " + floatItem + ",sDragView = " + sDragView);
                sDragView.setVisibility(0);
            }
        }
        if (sOriginDragView != null) {
            floatItem = (FloatAppItem) sOriginDragView.getTag();
            floatItem.visible = true;
            sOriginDragView.clearAnimation();
            if (sOriginDragView.getVisibility() == 4) {
                Log.d("DragGridView", "Reset origin drag view visible: floatItem = " + floatItem + ", sOriginDragView = " + sOriginDragView);
                sOriginDragView.setVisibility(0);
            }
        }
    }

    private void clearAllAnimations() {
        int childrenCount = getChildCount();
        Log.d("DragGridView", "clearAllAnimations: childrenCount = " + childrenCount + ", this = " + this);
        for (int i = 0; i < childrenCount; i++) {
            View child = getChildAt(i);
            if (child != null) {
                child.clearAnimation();
            }
        }
    }

    private int findInsertPosition(int x, int y) {
        Rect frame = new Rect();
        Rect tempFrame = new Rect();
        Rect tempFrame1 = new Rect();
        int columnNum = getNumColumns();
        int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (!(child == null || child.getVisibility() == 8)) {
                child.getHitRect(frame);
                if (i % columnNum == 0 && frame.left < child.getWidth()) {
                    frame.left -= getPaddingLeft();
                }
                if (i < columnNum) {
                    frame.top -= getPaddingTop();
                }
                if ((i + 1) % columnNum == 0) {
                    if (getChildAt(i - 1) != null) {
                        getChildAt(i - 1).getHitRect(tempFrame);
                    }
                    if (tempFrame.left > frame.right) {
                        frame.right += getHorizontalSpacing();
                    } else {
                        frame.right = getRight();
                    }
                } else if (i + 1 < count) {
                    if (getChildAt(i + 1) != null) {
                        getChildAt(i + 1).getHitRect(tempFrame);
                    }
                    if (tempFrame.left - frame.right >= child.getWidth() || tempFrame.right < frame.left) {
                        frame.right += getHorizontalSpacing();
                    } else {
                        frame.right = tempFrame.left;
                    }
                } else {
                    frame.right += getHorizontalSpacing();
                }
                if (i + columnNum < count) {
                    if (getChildAt(i + columnNum) != null) {
                        getChildAt(i + columnNum).getHitRect(tempFrame1);
                    }
                    if (tempFrame1.top <= frame.bottom) {
                        frame.bottom += getVerticalSpacing();
                    } else {
                        frame.bottom = tempFrame1.top;
                    }
                } else {
                    frame.bottom += getVerticalSpacing();
                }
                Log.d("DragGridView", "findInsertPosition: i = " + i + ", x = " + x + ",y = " + y + ", frame = " + frame + ",columnNum = " + columnNum + ",tag = " + child.getTag() + ",tempFrame = " + tempFrame + ", tempFrame1 = " + tempFrame1);
                if (frame.contains(x, y)) {
                    Log.d("shan", "i = " + i);
                    Log.d("shan", "getFirstVisiblePosition() = " + getFirstVisiblePosition());
                    Log.d("shan", "getFirstVisiblePosition() + i = " + (getFirstVisiblePosition() + i));
                    return getFirstVisiblePosition() + i;
                }
            }
        }
        return -1;
    }

    private void reorderViews(int fromPosition, int toPosition) {
        if (fromPosition != toPosition) {
            View fromChild = getView(fromPosition);
            View toChild = getView(toPosition);
            Rect toLayout = new Rect();
            if (fromChild != null && toChild != null) {
                getLayoutRect(toChild, toLayout);
                int i;
                if (toPosition < fromPosition) {
                    for (i = toPosition; i < fromPosition; i++) {
                        moveView(i, i + 1);
                    }
                    fromChild.layout(toLayout.left, toLayout.top, toLayout.right, toLayout.bottom);
                } else {
                    for (i = toPosition; i > fromPosition; i--) {
                        moveView(i, i - 1);
                    }
                    fromChild.layout(toLayout.left, toLayout.top, toLayout.right, toLayout.bottom);
                }
            }
        }
    }

    private void moveView(int fromPosition, int toPosition) {
        View from = getView(fromPosition);
        Rect fromRect = new Rect();
        if (from != null) {
            getLayoutRect(from, fromRect);
        }
        View to = getView(toPosition);
        Rect toRect = new Rect();
        if (to != null) {
            getLayoutRect(to, toRect);
        }
        if (from != null && to != null) {
            Animation translate = new TranslateAnimation(0.0f, (float) (toRect.left - fromRect.left), 0.0f, (float) (toRect.top - fromRect.top));
            translate.setDuration(120);
            translate.setFillEnabled(true);
            translate.setFillBefore(true);
            translate.setFillAfter(true);
            translate.setAnimationListener(new MoveViewAnimationListener(from, to.getLeft(), to.getTop()));
            from.startAnimation(translate);
        }
    }

    private void getLayoutRect(View view, Rect outRect) {
        outRect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
    }

    public View getView(int reorderingPosition) {
        return getChildAt(this.mReorderingPositions.get(reorderingPosition) - getFirstVisiblePosition());
    }

    public void setOnReorderingListener(OnReorderingListener listener) {
        this.mOnReorderingListener = listener;
    }

    private void dumpLayoutPosition() {
        int childrenCount = getChildCount();
        Rect toLayout = new Rect();
        for (int i = 0; i < childrenCount; i++) {
            if (getChildAt(i) != null) {
                getLayoutRect(getChildAt(i), toLayout);
                Log.d("DragGridView", "Child[" + i + "] :" + toLayout + ",tag = " + getChildAt(i).getTag());
            }
        }
    }

    private void scrollIfNeeded(int oldY, int newY) {
        boolean z = true;
        boolean z2 = false;
        if (this.mVerticalDragScrollEnable) {
            Log.d("DragGridView", "scrollIfNeeded oldY=" + oldY + ", newY=" + newY + ", container.top=" + getTop() + ", container.bottom=" + getBottom() + ", mScrollerState=" + this.mScrollerState + ", mTouchSlot=" + this.mTouchSlot + ", canScrollHorizontally(1)=" + canScrollHorizontally(1) + ", canScrollHorizontally(0)=" + canScrollHorizontally(0));
            if (newY >= 0) {
                if (newY >= oldY && this.mTouchSlot + newY >= getBottom() && this.mScrollerState == 0) {
                    Log.d("DragGridView", "scrollIfNeeded scroll down");
                    this.mScrollerState = 1;
                    this.mDragDirection = 1;
                    this.mHandler.postDelayed(this.mScrollRunnable, 300);
                } else if (oldY >= newY && newY - this.mTouchSlot <= getTop() && this.mScrollerState == 0) {
                    Log.d("DragGridView", "scrollIfNeeded scroll up");
                    this.mScrollerState = 1;
                    this.mDragDirection = 0;
                    this.mHandler.postDelayed(this.mScrollRunnable, 300);
                }
                if (this.mDragDirection == 1) {
                    if (newY >= oldY) {
                        z = false;
                    } else if (newY + 10 >= oldY) {
                        z = false;
                    }
                    this.mEdgeScrollForceStop = z;
                } else if (this.mDragDirection == 0) {
                    if (newY > oldY && newY + 10 > oldY) {
                        z2 = true;
                    }
                    this.mEdgeScrollForceStop = z2;
                }
            }
        }
    }
}
