package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AnimationUtils;
import android.widget.OverScroller;
import com.android.systemui.ExpandHelper;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.SwipeHelper.Callback;
import com.android.systemui.SwipeHelper.LongPressListener;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.ExpandableView.OnHeightChangedListener;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.NotificationOverflowContainer;
import com.android.systemui.statusbar.SpeedBumpView;
import com.android.systemui.statusbar.StackScrollerDecorView;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager.NotificationGroup;
import com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.ScrollAdapter;
import java.util.ArrayList;
import java.util.HashSet;

public class NotificationStackScrollLayout extends ViewGroup implements Callback, ExpandHelper.Callback, ScrollAdapter, OnHeightChangedListener, OnGroupChangeListener {
    private boolean mActivateNeedsAnimation;
    private int mActivePointerId;
    private ArrayList<View> mAddedHeadsUpChildren;
    private AmbientState mAmbientState;
    private ArrayList<AnimationEvent> mAnimationEvents;
    private HashSet<Runnable> mAnimationFinishedRunnables;
    private boolean mAnimationsEnabled;
    private int mBottomStackPeekSize;
    private int mBottomStackSlowDownHeight;
    private boolean mChangePositionInProgress;
    private ArrayList<View> mChildrenChangingPositions;
    private ArrayList<View> mChildrenToAddAnimated;
    private ArrayList<View> mChildrenToRemoveAnimated;
    private boolean mChildrenUpdateRequested;
    private OnPreDrawListener mChildrenUpdater;
    private HashSet<View> mClearOverlayViewsWhenFinished;
    private int mCollapseSecondCardPadding;
    private int mCollapsedSize;
    private int mContentHeight;
    private int mCurrentStackHeight;
    private StackScrollState mCurrentStackScrollState;
    private int mDarkAnimationOriginIndex;
    private boolean mDarkNeedsAnimation;
    private boolean mDelegateToScrollView;
    private boolean mDimmedNeedsAnimation;
    private boolean mDisallowScrollingInThisMotion;
    private boolean mDismissAllInProgress;
    private DismissView mDismissView;
    private boolean mDontReportNextOverScroll;
    private int mDownX;
    private ArrayList<View> mDragAnimPendingChildren;
    private EmptyShadeView mEmptyShadeView;
    private boolean mEverythingNeedsAnimation;
    private ExpandHelper mExpandHelper;
    private View mExpandedGroupView;
    private boolean mExpandedInThisMotion;
    private boolean mExpandingNotification;
    private boolean mForceNoOverlappingRendering;
    private HashSet<View> mFromMoreCardAdditions;
    private boolean mGenerateChildOrderChangedEvent;
    private long mGoToFullShadeDelay;
    private boolean mGoToFullShadeNeedsAnimation;
    private NotificationGroupManager mGroupManager;
    private HashSet<Pair<ExpandableNotificationRow, Boolean>> mHeadsUpChangeAnimations;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHideSensitiveNeedsAnimation;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mInterceptDelegateEnabled;
    private int mIntrinsicPadding;
    private boolean mIsBeingDragged;
    private boolean mIsExpanded;
    private boolean mIsExpansionChanging;
    private int mLastMotionY;
    private float mLastSetStackHeight;
    private OnChildLocationsChangedListener mListener;
    private LongPressListener mLongPressListener;
    private int mMaxLayoutHeight;
    private float mMaxOverScroll;
    private int mMaxScrollAfterExpand;
    private int mMaximumVelocity;
    private float mMinTopOverScrollToEscape;
    private int mMinimumVelocity;
    private boolean mNeedViewResizeAnimation;
    private boolean mNeedsAnimation;
    private int mNotificationTopPadding;
    private OnEmptySpaceClickListener mOnEmptySpaceClickListener;
    private OnHeightChangedListener mOnHeightChangedListener;
    private boolean mOnlyScrollingInThisMotion;
    private float mOverScrolledBottomPixels;
    private float mOverScrolledTopPixels;
    private int mOverflingDistance;
    private NotificationOverflowContainer mOverflowContainer;
    private OnOverscrollTopChangedListener mOverscrollTopChangedListener;
    private int mOwnScrollY;
    private int mPaddingBetweenElements;
    private int mPaddingBetweenElementsDimmed;
    private int mPaddingBetweenElementsNormal;
    private boolean mPanelTracking;
    private PhoneStatusBar mPhoneStatusBar;
    private boolean mRequestViewResizeAnimationOnLayout;
    private ScrimController mScrimController;
    private ViewGroup mScrollView;
    private boolean mScrolledToTopOnFirstDown;
    private OverScroller mScroller;
    private boolean mScrollingEnabled;
    private int mSidePaddings;
    private ArrayList<View> mSnappedBackChildren;
    private SpeedBumpView mSpeedBumpView;
    private StackScrollAlgorithm mStackScrollAlgorithm;
    private float mStackTranslation;
    private final StackStateAnimator mStateAnimator;
    private SwipeHelper mSwipeHelper;
    private ArrayList<View> mSwipedOutViews;
    private boolean mSwipingInProgress;
    private int[] mTempInt2;
    private final ArrayList<Pair<ExpandableNotificationRow, Boolean>> mTmpList;
    private int mTopPadding;
    private boolean mTopPaddingNeedsAnimation;
    private float mTopPaddingOverflow;
    private boolean mTouchIsClick;
    private int mTouchSlop;
    private boolean mTrackingHeadsUp;
    private VelocityTracker mVelocityTracker;

    public interface OnOverscrollTopChangedListener {
        void flingTopOverscroll(float f, boolean z);

        void onOverscrollTopChanged(float f, boolean z);
    }

    public interface OnEmptySpaceClickListener {
        void onEmptySpaceClicked(float f, float f2);
    }

    public interface OnChildLocationsChangedListener {
        void onChildLocationsChanged(NotificationStackScrollLayout notificationStackScrollLayout);
    }

    static class AnimationEvent {
        static AnimationFilter[] FILTERS = new AnimationFilter[]{new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateScale().animateZ(), new AnimationFilter().animateAlpha(), new AnimationFilter().animateAlpha().animateHeight(), new AnimationFilter().animateScale().animateAlpha(), new AnimationFilter().animateY().animateScale().animateDimmed(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateDark().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateScale().animateZ().hasDelays(), new AnimationFilter().animateHideSensitive(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateDark().animateScale().animateDimmed().animateHideSensitive().animateHeight().animateTopInset().animateY().animateZ()};
        static int[] LENGTHS = new int[]{464, 464, 360, 360, 360, 360, 220, 220, 360, 360, 448, 360, 360, 360, 650, 230, 360, 360};
        final int animationType;
        final View changingView;
        int darkAnimationOriginIndex;
        final long eventStartTime;
        final AnimationFilter filter;
        boolean headsUpFromBottom;
        final long length;
        View viewAfterChangingView;

        AnimationEvent(View view, int type) {
            this(view, type, (long) LENGTHS[type]);
        }

        AnimationEvent(View view, int type, long length) {
            this.eventStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.changingView = view;
            this.animationType = type;
            this.filter = FILTERS[type];
            this.length = length;
        }

        static long combineLength(ArrayList<AnimationEvent> events) {
            long length = 0;
            int size = events.size();
            for (int i = 0; i < size; i++) {
                AnimationEvent event = (AnimationEvent) events.get(i);
                length = Math.max(length, event.length);
                if (event.animationType == 10) {
                    return event.length;
                }
            }
            return length;
        }
    }

    public NotificationStackScrollLayout(Context context) {
        this(context, null);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCurrentStackHeight = Integer.MAX_VALUE;
        this.mCurrentStackScrollState = new StackScrollState(this);
        this.mAmbientState = new AmbientState();
        this.mChildrenToAddAnimated = new ArrayList();
        this.mAddedHeadsUpChildren = new ArrayList();
        this.mChildrenToRemoveAnimated = new ArrayList();
        this.mSnappedBackChildren = new ArrayList();
        this.mDragAnimPendingChildren = new ArrayList();
        this.mChildrenChangingPositions = new ArrayList();
        this.mFromMoreCardAdditions = new HashSet();
        this.mAnimationEvents = new ArrayList();
        this.mSwipedOutViews = new ArrayList();
        this.mStateAnimator = new StackStateAnimator(this);
        this.mIsExpanded = true;
        this.mChildrenUpdater = new OnPreDrawListener() {
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.updateChildren();
                NotificationStackScrollLayout.this.mChildrenUpdateRequested = false;
                NotificationStackScrollLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        };
        this.mTempInt2 = new int[2];
        this.mAnimationFinishedRunnables = new HashSet();
        this.mClearOverlayViewsWhenFinished = new HashSet();
        this.mHeadsUpChangeAnimations = new HashSet();
        this.mTmpList = new ArrayList();
        this.mExpandHelper = new ExpandHelper(getContext(), this, getResources().getDimensionPixelSize(R.dimen.notification_min_height), getResources().getDimensionPixelSize(R.dimen.notification_max_height));
        this.mExpandHelper.setEventSource(this);
        this.mExpandHelper.setScrollAdapter(this);
        this.mSwipeHelper = new SwipeHelper(0, this, getContext());
        this.mSwipeHelper.setLongPressListener(this.mLongPressListener);
        initView(context);
    }

    protected void onDraw(Canvas canvas) {
    }

    private void initView(Context context) {
        this.mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(262144);
        setClipChildren(false);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mOverflingDistance = configuration.getScaledOverflingDistance();
        this.mSidePaddings = context.getResources().getDimensionPixelSize(R.dimen.notification_side_padding);
        this.mCollapsedSize = context.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mBottomStackPeekSize = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_peek_amount);
        this.mStackScrollAlgorithm = new StackScrollAlgorithm(context);
        this.mStackScrollAlgorithm.setDimmed(this.mAmbientState.isDimmed());
        this.mPaddingBetweenElementsDimmed = context.getResources().getDimensionPixelSize(R.dimen.notification_padding_dimmed);
        this.mPaddingBetweenElementsNormal = context.getResources().getDimensionPixelSize(R.dimen.notification_padding);
        updatePadding(this.mAmbientState.isDimmed());
        this.mMinTopOverScrollToEscape = (float) getResources().getDimensionPixelSize(R.dimen.min_top_overscroll_to_qs);
        this.mNotificationTopPadding = getResources().getDimensionPixelSize(R.dimen.notifications_top_padding);
        this.mCollapseSecondCardPadding = getResources().getDimensionPixelSize(R.dimen.notification_collapse_second_card_padding);
    }

    private void updatePadding(boolean dimmed) {
        int i;
        if (dimmed && this.mStackScrollAlgorithm.shouldScaleDimmed()) {
            i = this.mPaddingBetweenElementsDimmed;
        } else {
            i = this.mPaddingBetweenElementsNormal;
        }
        this.mPaddingBetweenElements = i;
        this.mBottomStackSlowDownHeight = this.mStackScrollAlgorithm.getBottomStackSlowDownLength();
        updateContentHeight();
        notifyHeightChangeListener(null);
    }

    private void notifyHeightChangeListener(ExpandableView view) {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(view, false);
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec) - (this.mSidePaddings * 2), MeasureSpec.getMode(widthMeasureSpec)), heightMeasureSpec);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float centerX = ((float) getWidth()) / 2.0f;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                float width = (float) child.getMeasuredWidth();
                child.layout((int) (centerX - (width / 2.0f)), 0, (int) ((width / 2.0f) + centerX), (int) ((float) child.getMeasuredHeight()));
            }
        }
        setMaxLayoutHeight(getHeight());
        updateContentHeight();
        clampScrollPosition();
        if (this.mRequestViewResizeAnimationOnLayout) {
            requestAnimationOnViewResize();
            this.mRequestViewResizeAnimationOnLayout = false;
        }
        requestChildrenUpdate();
    }

    private void requestAnimationOnViewResize() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mNeedViewResizeAnimation = true;
            this.mNeedsAnimation = true;
        }
    }

    public void updateSpeedBumpIndex(int newIndex) {
        int currentIndex = indexOfChild(this.mSpeedBumpView);
        boolean validIndex = newIndex > 0;
        if (newIndex > getChildCount() - 1) {
            validIndex = false;
            newIndex = -1;
        }
        if (validIndex && currentIndex != newIndex) {
            changeViewPosition(this.mSpeedBumpView, newIndex);
        }
        updateSpeedBump(validIndex);
        this.mAmbientState.setSpeedBumpIndex(newIndex);
    }

    public void setChildLocationsChangedListener(OnChildLocationsChangedListener listener) {
        this.mListener = listener;
    }

    public int getChildLocation(View child) {
        StackViewState childViewState = this.mCurrentStackScrollState.getViewStateForView(child);
        if (childViewState == null) {
            return 0;
        }
        if (childViewState.gone) {
            return 64;
        }
        return childViewState.location;
    }

    private void setMaxLayoutHeight(int maxLayoutHeight) {
        this.mMaxLayoutHeight = maxLayoutHeight;
        updateAlgorithmHeightAndPadding();
    }

    private void updateAlgorithmHeightAndPadding() {
        this.mAmbientState.setLayoutHeight(getLayoutHeight());
        this.mAmbientState.setTopPadding(this.mTopPadding);
    }

    private boolean needsHeightAdaption() {
        return getNotGoneChildCount() > 1;
    }

    private void updateChildren() {
        this.mAmbientState.setScrollY(this.mOwnScrollY);
        this.mStackScrollAlgorithm.getStackScrollState(this.mAmbientState, this.mCurrentStackScrollState);
        if (isCurrentlyAnimating() || this.mNeedsAnimation) {
            startAnimationToState();
        } else {
            applyCurrentState();
        }
    }

    private void requestChildrenUpdate() {
        if (!this.mChildrenUpdateRequested) {
            getViewTreeObserver().addOnPreDrawListener(this.mChildrenUpdater);
            this.mChildrenUpdateRequested = true;
            invalidate();
        }
    }

    private boolean isCurrentlyAnimating() {
        return this.mStateAnimator.isRunning();
    }

    private void clampScrollPosition() {
        int scrollRange = getScrollRange();
        if (scrollRange < this.mOwnScrollY) {
            this.mOwnScrollY = scrollRange;
        }
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    private void setTopPadding(int topPadding, boolean animate) {
        if (this.mTopPadding != topPadding) {
            this.mTopPadding = topPadding;
            updateAlgorithmHeightAndPadding();
            updateContentHeight();
            if (animate && this.mAnimationsEnabled && this.mIsExpanded) {
                this.mTopPaddingNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
            notifyHeightChangeListener(null);
        }
    }

    public void setStackHeight(float height) {
        int normalUnfoldPositionStart;
        float paddingOffset;
        int stackHeight;
        boolean z = false;
        this.mLastSetStackHeight = height;
        if (height > 0.0f) {
            z = true;
        }
        setIsExpanded(z);
        int newStackHeight = (int) height;
        int minStackHeight = getMinStackHeight();
        boolean hasPinnedHeadsUp = !this.mTrackingHeadsUp ? this.mHeadsUpManager.hasPinnedHeadsUp() : true;
        if (hasPinnedHeadsUp) {
            normalUnfoldPositionStart = this.mHeadsUpManager.getTopHeadsUpHeight();
        } else {
            normalUnfoldPositionStart = minStackHeight;
        }
        if (((float) (newStackHeight - this.mTopPadding)) - this.mTopPaddingOverflow >= ((float) normalUnfoldPositionStart) || getNotGoneChildCount() == 0) {
            paddingOffset = this.mTopPaddingOverflow;
            stackHeight = newStackHeight;
        } else {
            int translationY = newStackHeight - minStackHeight;
            float partiallyThere = Math.max(0.0f, (((float) (newStackHeight - this.mTopPadding)) - this.mTopPaddingOverflow) / ((float) minStackHeight));
            if (hasPinnedHeadsUp) {
                translationY = (int) (height - ((float) this.mHeadsUpManager.getTopHeadsUpHeight()));
            } else {
                translationY = (int) (((float) translationY) + ((1.0f - partiallyThere) * ((float) (this.mBottomStackPeekSize + this.mCollapseSecondCardPadding))));
            }
            paddingOffset = (float) (translationY - this.mTopPadding);
            stackHeight = (int) (height - ((float) (translationY - this.mTopPadding)));
        }
        if (stackHeight != this.mCurrentStackHeight) {
            this.mCurrentStackHeight = stackHeight;
            updateAlgorithmHeightAndPadding();
            requestChildrenUpdate();
        }
        setStackTranslation(paddingOffset);
    }

    public float getStackTranslation() {
        return this.mStackTranslation;
    }

    private void setStackTranslation(float stackTranslation) {
        if (stackTranslation != this.mStackTranslation) {
            this.mStackTranslation = stackTranslation;
            this.mAmbientState.setStackTranslation(stackTranslation);
            requestChildrenUpdate();
        }
    }

    private int getLayoutHeight() {
        return Math.min(this.mMaxLayoutHeight, this.mCurrentStackHeight);
    }

    public int getItemHeight() {
        return this.mCollapsedSize;
    }

    public int getBottomStackPeekSize() {
        return this.mBottomStackPeekSize;
    }

    public int getCollapseSecondCardPadding() {
        return this.mCollapseSecondCardPadding;
    }

    public void setLongPressListener(LongPressListener listener) {
        this.mSwipeHelper.setLongPressListener(listener);
        this.mLongPressListener = listener;
    }

    public void setScrollView(ViewGroup scrollView) {
        this.mScrollView = scrollView;
    }

    public void setInterceptDelegateEnabled(boolean interceptDelegateEnabled) {
        this.mInterceptDelegateEnabled = interceptDelegateEnabled;
    }

    public void onChildDismissed(View v) {
        if (!this.mDismissAllInProgress) {
            setSwipingInProgress(false);
            if (this.mDragAnimPendingChildren.contains(v)) {
                this.mDragAnimPendingChildren.remove(v);
            }
            this.mSwipedOutViews.add(v);
            this.mAmbientState.onDragFinished(v);
            if (v instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                if (row.isHeadsUp()) {
                    this.mHeadsUpManager.addSwipedOutNotification(row.getStatusBarNotification().getKey());
                }
            }
            View veto = v.findViewById(R.id.veto);
            if (!(veto == null || veto.getVisibility() == 8)) {
                veto.performClick();
            }
        }
    }

    public void onChildSnappedBack(View animView) {
        this.mAmbientState.onDragFinished(animView);
        if (this.mDragAnimPendingChildren.contains(animView)) {
            this.mDragAnimPendingChildren.remove(animView);
            return;
        }
        if (this.mAnimationsEnabled) {
            this.mSnappedBackChildren.add(animView);
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress) {
        if (!this.mIsExpanded && isPinnedHeadsUp(animView) && canChildBeDismissed(animView)) {
            this.mScrimController.setTopHeadsUpDragAmount(animView, Math.min(Math.abs(swipeProgress - 1.0f), 1.0f));
        }
        return false;
    }

    public void onBeginDrag(View v) {
        setSwipingInProgress(true);
        this.mAmbientState.onBeginDrag(v);
        if (this.mAnimationsEnabled && (this.mIsExpanded || !isPinnedHeadsUp(v))) {
            this.mDragAnimPendingChildren.add(v);
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public static boolean isPinnedHeadsUp(View v) {
        boolean z = false;
        if (!(v instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow row = (ExpandableNotificationRow) v;
        if (row.isHeadsUp()) {
            z = row.isPinned();
        }
        return z;
    }

    private boolean isHeadsUp(View v) {
        if (v instanceof ExpandableNotificationRow) {
            return ((ExpandableNotificationRow) v).isHeadsUp();
        }
        return false;
    }

    public void onDragCancelled(View v) {
        setSwipingInProgress(false);
    }

    public float getFalsingThresholdFactor() {
        return this.mPhoneStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    public View getChildAtPosition(MotionEvent ev) {
        return getChildAtPosition(ev.getX(), ev.getY());
    }

    public ExpandableView getClosestChildAtRawPosition(float touchX, float touchY) {
        getLocationOnScreen(this.mTempInt2);
        float localTouchY = touchY - ((float) this.mTempInt2[1]);
        ExpandableView closestChild = null;
        float minDist = Float.MAX_VALUE;
        int count = getChildCount();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView slidingChild = (ExpandableView) getChildAt(childIdx);
            if (!(slidingChild.getVisibility() == 8 || (slidingChild instanceof StackScrollerDecorView) || slidingChild == this.mSpeedBumpView)) {
                float childTop = slidingChild.getTranslationY();
                float dist = Math.min(Math.abs((childTop + ((float) slidingChild.getClipTopAmount())) - localTouchY), Math.abs((childTop + ((float) slidingChild.getActualHeight())) - localTouchY));
                if (dist < minDist) {
                    closestChild = slidingChild;
                    minDist = dist;
                }
            }
        }
        return closestChild;
    }

    public ExpandableView getChildAtRawPosition(float touchX, float touchY) {
        getLocationOnScreen(this.mTempInt2);
        return getChildAtPosition(touchX - ((float) this.mTempInt2[0]), touchY - ((float) this.mTempInt2[1]));
    }

    public ExpandableView getChildAtPosition(float touchX, float touchY) {
        int count = getChildCount();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView slidingChild = (ExpandableView) getChildAt(childIdx);
            if (!(slidingChild.getVisibility() == 8 || (slidingChild instanceof StackScrollerDecorView) || slidingChild == this.mSpeedBumpView)) {
                float childTop = slidingChild.getTranslationY();
                float top = childTop + ((float) slidingChild.getClipTopAmount());
                float bottom = childTop + ((float) slidingChild.getActualHeight());
                int right = getWidth();
                if (touchY >= top && touchY <= bottom && touchX >= 0.0f && touchX <= ((float) right)) {
                    if (!(slidingChild instanceof ExpandableNotificationRow)) {
                        return slidingChild;
                    }
                    ExpandableNotificationRow row = (ExpandableNotificationRow) slidingChild;
                    if (this.mIsExpanded || !row.isHeadsUp() || !row.isPinned() || this.mHeadsUpManager.getTopEntry().entry.row == row) {
                        return row.getViewAtPosition(touchY - childTop);
                    }
                }
            }
        }
        return null;
    }

    public boolean canChildBeExpanded(View v) {
        if ((v instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) v).isExpandable()) {
            return !((ExpandableNotificationRow) v).isHeadsUp();
        } else {
            return false;
        }
    }

    public void setUserExpandedChild(View v, boolean userExpanded) {
        if (v instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) v).setUserExpanded(userExpanded);
        }
    }

    public void setUserLockedChild(View v, boolean userLocked) {
        if (v instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) v).setUserLocked(userLocked);
        }
        removeLongPressCallback();
        requestDisallowInterceptTouchEvent(true);
    }

    public void expansionStateChanged(boolean isExpanding) {
        this.mExpandingNotification = isExpanding;
        if (!this.mExpandedInThisMotion) {
            this.mMaxScrollAfterExpand = this.mOwnScrollY;
            this.mExpandedInThisMotion = true;
        }
    }

    public void setScrollingEnabled(boolean enable) {
        this.mScrollingEnabled = enable;
    }

    public void setExpandingEnabled(boolean enable) {
        this.mExpandHelper.setEnabled(enable);
    }

    private boolean isScrollingEnabled() {
        return this.mScrollingEnabled;
    }

    public View getChildContentView(View v) {
        return v;
    }

    public boolean canChildBeDismissed(View v) {
        return StackScrollAlgorithm.canChildBeDismissed(v);
    }

    public boolean isAntiFalsingNeeded() {
        return this.mPhoneStatusBar.getBarState() == 1;
    }

    private void setSwipingInProgress(boolean isSwiped) {
        this.mSwipingInProgress = isSwiped;
        if (isSwiped) {
            requestDisallowInterceptTouchEvent(true);
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mSwipeHelper.setDensityScale(getResources().getDisplayMetrics().density);
        this.mSwipeHelper.setPagingTouchSlop((float) ViewConfiguration.get(getContext()).getScaledPagingTouchSlop());
        initView(getContext());
    }

    public void dismissViewAnimated(View child, Runnable endRunnable, int delay, long duration) {
        this.mSwipeHelper.dismissChild(child, 0.0f, endRunnable, (long) delay, true, duration);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean z = true;
        boolean isCancelOrUp = ev.getActionMasked() != 3 ? ev.getActionMasked() == 1 : true;
        if (this.mDelegateToScrollView) {
            if (isCancelOrUp) {
                this.mDelegateToScrollView = false;
            }
            transformTouchEvent(ev, this, this.mScrollView);
            return this.mScrollView.onTouchEvent(ev);
        }
        handleEmptySpaceClick(ev);
        boolean expandWantsIt = false;
        if (!(!this.mIsExpanded || this.mSwipingInProgress || this.mOnlyScrollingInThisMotion)) {
            if (isCancelOrUp) {
                this.mExpandHelper.onlyObserveMovements(false);
            }
            boolean wasExpandingBefore = this.mExpandingNotification;
            expandWantsIt = this.mExpandHelper.onTouchEvent(ev);
            if (this.mExpandedInThisMotion && !this.mExpandingNotification && wasExpandingBefore && !this.mDisallowScrollingInThisMotion) {
                dispatchDownEventToScroller(ev);
            }
        }
        boolean scrollerWantsIt = false;
        if (!(!this.mIsExpanded || this.mSwipingInProgress || this.mExpandingNotification || this.mDisallowScrollingInThisMotion)) {
            scrollerWantsIt = onScrollTouch(ev);
        }
        boolean horizontalSwipeWantsIt = false;
        if (!(this.mIsBeingDragged || this.mExpandingNotification || this.mExpandedInThisMotion || this.mOnlyScrollingInThisMotion)) {
            horizontalSwipeWantsIt = this.mSwipeHelper.onTouchEvent(ev);
        }
        if (!(horizontalSwipeWantsIt || r3 || r0)) {
            z = super.onTouchEvent(ev);
        }
        return z;
    }

    private void dispatchDownEventToScroller(MotionEvent ev) {
        MotionEvent downEvent = MotionEvent.obtain(ev);
        downEvent.setAction(0);
        onScrollTouch(downEvent);
        downEvent.recycle();
    }

    private boolean onScrollTouch(MotionEvent ev) {
        if (!isScrollingEnabled()) {
            return false;
        }
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(ev);
        switch (ev.getAction() & 255) {
            case 0:
                if (getChildCount() != 0 && isInContentBounds(ev)) {
                    setIsBeingDragged(!this.mScroller.isFinished());
                    if (!this.mScroller.isFinished()) {
                        this.mScroller.forceFinished(true);
                    }
                    this.mLastMotionY = (int) ev.getY();
                    this.mDownX = (int) ev.getX();
                    this.mActivePointerId = ev.getPointerId(0);
                    break;
                }
                return false;
            case 1:
                if (this.mIsBeingDragged) {
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE, (float) this.mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity(this.mActivePointerId);
                    if (shouldOverScrollFling(initialVelocity)) {
                        onOverScrollFling(true, initialVelocity);
                    } else if (getChildCount() > 0) {
                        if (Math.abs(initialVelocity) > this.mMinimumVelocity) {
                            if (getCurrentOverScrollAmount(true) == 0.0f || initialVelocity > 0) {
                                fling(-initialVelocity);
                            } else {
                                onOverScrollFling(false, initialVelocity);
                            }
                        } else if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                            postInvalidateOnAnimation();
                        }
                    }
                    this.mActivePointerId = -1;
                    endDrag();
                    break;
                }
                break;
            case 2:
                int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                if (activePointerIndex != -1) {
                    int y = (int) ev.getY(activePointerIndex);
                    int deltaY = this.mLastMotionY - y;
                    int xDiff = Math.abs(((int) ev.getX(activePointerIndex)) - this.mDownX);
                    int yDiff = Math.abs(deltaY);
                    if (!this.mIsBeingDragged && yDiff > this.mTouchSlop && yDiff > xDiff) {
                        setIsBeingDragged(true);
                        deltaY = deltaY > 0 ? deltaY - this.mTouchSlop : deltaY + this.mTouchSlop;
                    }
                    if (this.mIsBeingDragged) {
                        float scrollAmount;
                        this.mLastMotionY = y;
                        int range = getScrollRange();
                        if (this.mExpandedInThisMotion) {
                            range = Math.min(range, this.mMaxScrollAfterExpand);
                        }
                        if (deltaY < 0) {
                            scrollAmount = overScrollDown(deltaY);
                        } else {
                            scrollAmount = overScrollUp(deltaY, range);
                        }
                        if (scrollAmount != 0.0f) {
                            overScrollBy(0, (int) scrollAmount, 0, this.mOwnScrollY, 0, range, 0, getHeight() / 2, true);
                            break;
                        }
                    }
                }
                Log.e("NotificationStackScrollLayout", "Invalid pointerId=" + this.mActivePointerId + " in onTouchEvent");
                break;
                break;
            case 3:
                if (this.mIsBeingDragged && getChildCount() > 0) {
                    if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                        postInvalidateOnAnimation();
                    }
                    this.mActivePointerId = -1;
                    endDrag();
                    break;
                }
            case 5:
                int index = ev.getActionIndex();
                this.mLastMotionY = (int) ev.getY(index);
                this.mDownX = (int) ev.getX(index);
                this.mActivePointerId = ev.getPointerId(index);
                break;
            case 6:
                onSecondaryPointerUp(ev);
                this.mLastMotionY = (int) ev.getY(ev.findPointerIndex(this.mActivePointerId));
                this.mDownX = (int) ev.getX(ev.findPointerIndex(this.mActivePointerId));
                break;
        }
        return true;
    }

    private void onOverScrollFling(boolean open, int initialVelocity) {
        if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.flingTopOverscroll((float) initialVelocity, open);
        }
        this.mDontReportNextOverScroll = true;
        setOverScrollAmount(0.0f, true, false);
    }

    private float overScrollUp(int deltaY, int range) {
        deltaY = Math.max(deltaY, 0);
        float currentTopAmount = getCurrentOverScrollAmount(true);
        float newTopAmount = currentTopAmount - ((float) deltaY);
        if (currentTopAmount > 0.0f) {
            setOverScrollAmount(newTopAmount, true, false);
        }
        float scrollAmount = newTopAmount < 0.0f ? -newTopAmount : 0.0f;
        float newScrollY = ((float) this.mOwnScrollY) + scrollAmount;
        if (newScrollY <= ((float) range)) {
            return scrollAmount;
        }
        if (!this.mExpandedInThisMotion) {
            setOverScrolledPixels((getCurrentOverScrolledPixels(false) + newScrollY) - ((float) range), false, false);
        }
        this.mOwnScrollY = range;
        return 0.0f;
    }

    private float overScrollDown(int deltaY) {
        deltaY = Math.min(deltaY, 0);
        float currentBottomAmount = getCurrentOverScrollAmount(false);
        float newBottomAmount = currentBottomAmount + ((float) deltaY);
        if (currentBottomAmount > 0.0f) {
            setOverScrollAmount(newBottomAmount, false, false);
        }
        float scrollAmount = newBottomAmount < 0.0f ? newBottomAmount : 0.0f;
        float newScrollY = ((float) this.mOwnScrollY) + scrollAmount;
        if (newScrollY >= 0.0f) {
            return scrollAmount;
        }
        setOverScrolledPixels(getCurrentOverScrolledPixels(true) - newScrollY, true, false);
        this.mOwnScrollY = 0;
        return 0.0f;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = (ev.getAction() & 65280) >> 8;
        if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mLastMotionY = (int) ev.getY(newPointerIndex);
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int oldX = this.mScrollX;
            int oldY = this.mOwnScrollY;
            int x = this.mScroller.getCurrX();
            int y = this.mScroller.getCurrY();
            if (!(oldX == x && oldY == y)) {
                int range = getScrollRange();
                if (y >= 0 || oldY < 0) {
                    if (y > range && oldY <= range) {
                    }
                    overScrollBy(x - oldX, y - oldY, oldX, oldY, 0, range, 0, (int) this.mMaxOverScroll, false);
                    onScrollChanged(this.mScrollX, this.mOwnScrollY, oldX, oldY);
                }
                float currVelocity = this.mScroller.getCurrVelocity();
                if (currVelocity >= ((float) this.mMinimumVelocity)) {
                    this.mMaxOverScroll = (Math.abs(currVelocity) / 1000.0f) * ((float) this.mOverflingDistance);
                }
                overScrollBy(x - oldX, y - oldY, oldX, oldY, 0, range, 0, (int) this.mMaxOverScroll, false);
                onScrollChanged(this.mScrollX, this.mOwnScrollY, oldX, oldY);
            }
            postInvalidateOnAnimation();
        }
    }

    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        int newScrollY = scrollY + deltaY;
        int top = -maxOverScrollY;
        int bottom = maxOverScrollY + scrollRangeY;
        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }
        onOverScrolled(0, newScrollY, false, clampedY);
        return clampedY;
    }

    public void setOverScrolledPixels(float numPixels, boolean onTop, boolean animate) {
        setOverScrollAmount(getRubberBandFactor(onTop) * numPixels, onTop, animate, true);
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate) {
        setOverScrollAmount(amount, onTop, animate, true);
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate, boolean cancelAnimators) {
        setOverScrollAmount(amount, onTop, animate, cancelAnimators, isRubberbanded(onTop));
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate, boolean cancelAnimators, boolean isRubberbanded) {
        if (cancelAnimators) {
            this.mStateAnimator.cancelOverScrollAnimators(onTop);
        }
        setOverScrollAmountInternal(amount, onTop, animate, isRubberbanded);
    }

    private void setOverScrollAmountInternal(float amount, boolean onTop, boolean animate, boolean isRubberbanded) {
        amount = Math.max(0.0f, amount);
        if (animate) {
            this.mStateAnimator.animateOverScrollToAmount(amount, onTop, isRubberbanded);
            return;
        }
        setOverScrolledPixels(amount / getRubberBandFactor(onTop), onTop);
        this.mAmbientState.setOverScrollAmount(amount, onTop);
        if (onTop) {
            notifyOverscrollTopListener(amount, isRubberbanded);
        }
        requestChildrenUpdate();
    }

    private void notifyOverscrollTopListener(float amount, boolean isRubberbanded) {
        boolean z;
        ExpandHelper expandHelper = this.mExpandHelper;
        if (amount > 1.0f) {
            z = true;
        } else {
            z = false;
        }
        expandHelper.onlyObserveMovements(z);
        if (this.mDontReportNextOverScroll) {
            this.mDontReportNextOverScroll = false;
            return;
        }
        if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.onOverscrollTopChanged(amount, isRubberbanded);
        }
    }

    public void setOverscrollTopChangedListener(OnOverscrollTopChangedListener overscrollTopChangedListener) {
        this.mOverscrollTopChangedListener = overscrollTopChangedListener;
    }

    public float getCurrentOverScrollAmount(boolean top) {
        return this.mAmbientState.getOverScrollAmount(top);
    }

    public float getCurrentOverScrolledPixels(boolean top) {
        return top ? this.mOverScrolledTopPixels : this.mOverScrolledBottomPixels;
    }

    private void setOverScrolledPixels(float amount, boolean onTop) {
        if (onTop) {
            this.mOverScrolledTopPixels = amount;
        } else {
            this.mOverScrolledBottomPixels = amount;
        }
    }

    private void customScrollTo(int y) {
        this.mOwnScrollY = y;
        updateChildren();
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (this.mScroller.isFinished()) {
            customScrollTo(scrollY);
            scrollTo(scrollX, this.mScrollY);
            return;
        }
        int oldX = this.mScrollX;
        int oldY = this.mOwnScrollY;
        this.mScrollX = scrollX;
        this.mOwnScrollY = scrollY;
        if (clampedY) {
            springBack();
            return;
        }
        onScrollChanged(this.mScrollX, this.mOwnScrollY, oldX, oldY);
        invalidateParentIfNeeded();
        updateChildren();
        float overScrollTop = getCurrentOverScrollAmount(true);
        if (this.mOwnScrollY < 0) {
            notifyOverscrollTopListener((float) (-this.mOwnScrollY), isRubberbanded(true));
        } else {
            notifyOverscrollTopListener(overScrollTop, isRubberbanded(true));
        }
    }

    private void springBack() {
        int scrollRange = getScrollRange();
        boolean overScrolledTop = this.mOwnScrollY <= 0;
        boolean overScrolledBottom = this.mOwnScrollY >= scrollRange;
        if (overScrolledTop || overScrolledBottom) {
            boolean onTop;
            float newAmount;
            if (overScrolledTop) {
                onTop = true;
                newAmount = (float) (-this.mOwnScrollY);
                this.mOwnScrollY = 0;
                this.mDontReportNextOverScroll = true;
            } else {
                onTop = false;
                newAmount = (float) (this.mOwnScrollY - scrollRange);
                this.mOwnScrollY = scrollRange;
            }
            setOverScrollAmount(newAmount, onTop, false);
            setOverScrollAmount(0.0f, onTop, true);
            this.mScroller.forceFinished(true);
        }
    }

    private int getScrollRange() {
        ExpandableView firstChild = (ExpandableView) getFirstChildNotGone();
        if (firstChild == null) {
            return 0;
        }
        int contentHeight = getContentHeight();
        int firstChildMaxExpandHeight = getMaxExpandHeight(firstChild);
        int scrollRange = Math.max(0, ((contentHeight - this.mMaxLayoutHeight) + this.mBottomStackPeekSize) + this.mBottomStackSlowDownHeight);
        if (scrollRange <= 0) {
            return scrollRange;
        }
        View lastChild = getLastChildNotGone();
        return Math.max(scrollRange, firstChildMaxExpandHeight - this.mCollapsedSize);
    }

    private View getFirstChildNotGone() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }

    private View getFirstChildBelowTranlsationY(float translationY) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8 && child.getTranslationY() >= translationY) {
                return child;
            }
        }
        return null;
    }

    public View getLastChildNotGone() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }

    public int getNotGoneChildCount() {
        int childCount = getChildCount();
        int count = 0;
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            if (!(child.getVisibility() == 8 || child.willBeGone())) {
                count++;
            }
        }
        return count;
    }

    private int getMaxExpandHeight(View view) {
        if (view instanceof ExpandableNotificationRow) {
            return ((ExpandableNotificationRow) view).getIntrinsicHeight();
        }
        return view.getHeight();
    }

    public int getContentHeight() {
        return this.mContentHeight;
    }

    private void updateContentHeight() {
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                if (height != 0) {
                    height += this.mPaddingBetweenElements;
                }
                if (child instanceof ExpandableView) {
                    height += ((ExpandableView) child).getIntrinsicHeight();
                }
            }
        }
        this.mContentHeight = this.mTopPadding + height;
    }

    private void fling(int velocityY) {
        if (getChildCount() > 0) {
            int scrollRange = getScrollRange();
            float topAmount = getCurrentOverScrollAmount(true);
            float bottomAmount = getCurrentOverScrollAmount(false);
            if (velocityY < 0 && topAmount > 0.0f) {
                this.mOwnScrollY -= (int) topAmount;
                this.mDontReportNextOverScroll = true;
                setOverScrollAmount(0.0f, true, false);
                this.mMaxOverScroll = (((((float) Math.abs(velocityY)) / 1000.0f) * getRubberBandFactor(true)) * ((float) this.mOverflingDistance)) + topAmount;
            } else if (velocityY <= 0 || bottomAmount <= 0.0f) {
                this.mMaxOverScroll = 0.0f;
            } else {
                this.mOwnScrollY = (int) (((float) this.mOwnScrollY) + bottomAmount);
                setOverScrollAmount(0.0f, false, false);
                this.mMaxOverScroll = (((((float) Math.abs(velocityY)) / 1000.0f) * getRubberBandFactor(false)) * ((float) this.mOverflingDistance)) + bottomAmount;
            }
            this.mScroller.fling(this.mScrollX, this.mOwnScrollY, 1, velocityY, 0, 0, 0, Math.max(0, scrollRange), 0, 1073741823);
            postInvalidateOnAnimation();
        }
    }

    private boolean shouldOverScrollFling(int initialVelocity) {
        float topOverScroll = getCurrentOverScrollAmount(true);
        if (!this.mScrolledToTopOnFirstDown || this.mExpandedInThisMotion || topOverScroll <= this.mMinTopOverScrollToEscape) {
            return false;
        }
        if (initialVelocity <= 0) {
            return false;
        }
        return true;
    }

    public void updateTopPadding(float qsHeight, int scrollY, boolean animate, boolean ignoreIntrinsicPadding) {
        float start = (qsHeight - ((float) scrollY)) + ((float) this.mNotificationTopPadding);
        float stackHeight = ((float) getHeight()) - start;
        int minStackHeight = getMinStackHeight();
        if (stackHeight <= ((float) minStackHeight)) {
            start = ((float) getHeight()) - ((float) minStackHeight);
            this.mTopPaddingOverflow = ((float) minStackHeight) - stackHeight;
        } else {
            this.mTopPaddingOverflow = 0.0f;
        }
        setTopPadding(ignoreIntrinsicPadding ? (int) start : clampPadding((int) start), animate);
        setStackHeight(this.mLastSetStackHeight);
    }

    public int getNotificationTopPadding() {
        return this.mNotificationTopPadding;
    }

    public int getMinStackHeight() {
        return (this.mCollapsedSize + this.mBottomStackPeekSize) + this.mCollapseSecondCardPadding;
    }

    public float getTopPaddingOverflow() {
        return this.mTopPaddingOverflow;
    }

    public int getPeekHeight() {
        return ((this.mIntrinsicPadding + this.mCollapsedSize) + this.mBottomStackPeekSize) + this.mCollapseSecondCardPadding;
    }

    private int clampPadding(int desiredPadding) {
        return Math.max(desiredPadding, this.mIntrinsicPadding);
    }

    private float getRubberBandFactor(boolean onTop) {
        if (!onTop) {
            return 0.35f;
        }
        if (this.mExpandedInThisMotion) {
            return 0.15f;
        }
        if (this.mIsExpansionChanging || this.mPanelTracking) {
            return 0.21f;
        }
        if (this.mScrolledToTopOnFirstDown) {
            return 1.0f;
        }
        return 0.35f;
    }

    private boolean isRubberbanded(boolean onTop) {
        if (!onTop || this.mExpandedInThisMotion || this.mIsExpansionChanging || this.mPanelTracking || !this.mScrolledToTopOnFirstDown) {
            return true;
        }
        return false;
    }

    private void endDrag() {
        setIsBeingDragged(false);
        recycleVelocityTracker();
        if (getCurrentOverScrollAmount(true) > 0.0f) {
            setOverScrollAmount(0.0f, true, true);
        }
        if (getCurrentOverScrollAmount(false) > 0.0f) {
            setOverScrollAmount(0.0f, false, true);
        }
    }

    private void transformTouchEvent(MotionEvent ev, View sourceView, View targetView) {
        ev.offsetLocation(sourceView.getX(), sourceView.getY());
        ev.offsetLocation(-targetView.getX(), -targetView.getY());
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean z = true;
        if (this.mInterceptDelegateEnabled) {
            transformTouchEvent(ev, this, this.mScrollView);
            if (this.mScrollView.onInterceptTouchEvent(ev)) {
                this.mDelegateToScrollView = true;
                removeLongPressCallback();
                return true;
            }
            transformTouchEvent(ev, this.mScrollView, this);
        }
        initDownStates(ev);
        handleEmptySpaceClick(ev);
        boolean expandWantsIt = false;
        if (!(this.mSwipingInProgress || this.mOnlyScrollingInThisMotion)) {
            expandWantsIt = this.mExpandHelper.onInterceptTouchEvent(ev);
        }
        boolean scrollWantsIt = false;
        if (!(this.mSwipingInProgress || this.mExpandingNotification)) {
            scrollWantsIt = onInterceptTouchEventScroll(ev);
        }
        boolean swipeWantsIt = false;
        if (!(this.mIsBeingDragged || this.mExpandingNotification || this.mExpandedInThisMotion || this.mOnlyScrollingInThisMotion)) {
            swipeWantsIt = this.mSwipeHelper.onInterceptTouchEvent(ev);
        }
        if (!(swipeWantsIt || r1 || r0)) {
            z = super.onInterceptTouchEvent(ev);
        }
        return z;
    }

    private void handleEmptySpaceClick(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case 1:
                if (this.mPhoneStatusBar.getBarState() != 1 && this.mTouchIsClick && isBelowLastNotification(this.mInitialTouchX, this.mInitialTouchY)) {
                    this.mOnEmptySpaceClickListener.onEmptySpaceClicked(this.mInitialTouchX, this.mInitialTouchY);
                    return;
                }
                return;
            case 2:
                if (!this.mTouchIsClick) {
                    return;
                }
                if (Math.abs(ev.getY() - this.mInitialTouchY) > ((float) this.mTouchSlop) || Math.abs(ev.getX() - this.mInitialTouchX) > ((float) this.mTouchSlop)) {
                    this.mTouchIsClick = false;
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void initDownStates(MotionEvent ev) {
        if (ev.getAction() == 0) {
            boolean z;
            this.mExpandedInThisMotion = false;
            if (this.mScroller.isFinished()) {
                z = false;
            } else {
                z = true;
            }
            this.mOnlyScrollingInThisMotion = z;
            this.mDisallowScrollingInThisMotion = false;
            this.mTouchIsClick = true;
            this.mInitialTouchX = ev.getX();
            this.mInitialTouchY = ev.getY();
        }
    }

    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (!isChildInGroup(child)) {
            onViewRemovedInternal(child);
        }
    }

    private void onViewRemovedInternal(View child) {
        this.mStackScrollAlgorithm.notifyChildrenChanged(this);
        if (!this.mChangePositionInProgress) {
            ((ExpandableView) child).setOnHeightChangedListener(null);
            this.mCurrentStackScrollState.removeViewStateForView(child);
            updateScrollStateForRemovedChild(child);
            if (generateRemoveAnimation(child) && !this.mSwipedOutViews.contains(child)) {
                getOverlay().add(child);
            }
            updateAnimationState(false, child);
            ((ExpandableView) child).setClipTopOptimization(0);
        }
    }

    private boolean isChildInGroup(View child) {
        if (child instanceof ExpandableNotificationRow) {
            return this.mGroupManager.isChildInGroupWithSummary(((ExpandableNotificationRow) child).getStatusBarNotification());
        }
        return false;
    }

    private boolean generateRemoveAnimation(View child) {
        if (removeRemovedChildFromHeadsUpChangeAnimations(child)) {
            this.mAddedHeadsUpChildren.remove(child);
            return false;
        } else if (isClickedHeadsUp(child)) {
            this.mClearOverlayViewsWhenFinished.add(child);
            return true;
        } else if (!this.mIsExpanded || !this.mAnimationsEnabled || isChildInInvisibleGroup(child)) {
            return false;
        } else {
            if (this.mChildrenToAddAnimated.contains(child)) {
                this.mChildrenToAddAnimated.remove(child);
                this.mFromMoreCardAdditions.remove(child);
                return false;
            }
            this.mChildrenToRemoveAnimated.add(child);
            this.mNeedsAnimation = true;
            return true;
        }
    }

    private boolean isClickedHeadsUp(View child) {
        return HeadsUpManager.isClickedHeadsUpNotification(child);
    }

    private boolean removeRemovedChildFromHeadsUpChangeAnimations(View child) {
        boolean hasAddEvent = false;
        for (Pair<ExpandableNotificationRow, Boolean> eventPair : this.mHeadsUpChangeAnimations) {
            View row = eventPair.first;
            boolean isHeadsUp = ((Boolean) eventPair.second).booleanValue();
            if (child == row) {
                this.mTmpList.add(eventPair);
                hasAddEvent |= isHeadsUp;
            }
        }
        if (hasAddEvent) {
            this.mHeadsUpChangeAnimations.removeAll(this.mTmpList);
        }
        this.mTmpList.clear();
        return hasAddEvent;
    }

    private boolean isChildInInvisibleGroup(View child) {
        boolean z = false;
        if (child instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) child;
            ExpandableNotificationRow groupSummary = this.mGroupManager.getGroupSummary(row.getStatusBarNotification());
            if (!(groupSummary == null || groupSummary == row)) {
                if (!groupSummary.areChildrenExpanded()) {
                    z = true;
                }
                return z;
            }
        }
        return false;
    }

    private void updateScrollStateForRemovedChild(View removedChild) {
        int startingPosition = getPositionInLinearLayout(removedChild);
        int childHeight = getIntrinsicHeight(removedChild) + this.mPaddingBetweenElements;
        if (startingPosition + childHeight <= this.mOwnScrollY) {
            this.mOwnScrollY -= childHeight;
        } else if (startingPosition < this.mOwnScrollY) {
            this.mOwnScrollY = startingPosition;
        }
    }

    private int getIntrinsicHeight(View view) {
        if (view instanceof ExpandableView) {
            return ((ExpandableView) view).getIntrinsicHeight();
        }
        return view.getHeight();
    }

    private int getPositionInLinearLayout(View requestedChild) {
        int position = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == requestedChild) {
                return position;
            }
            if (child.getVisibility() != 8) {
                position += getIntrinsicHeight(child);
                if (i < getChildCount() - 1) {
                    position += this.mPaddingBetweenElements;
                }
            }
        }
        return 0;
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        onViewAddedInternal(child);
    }

    private void onViewAddedInternal(View child) {
        updateHideSensitiveForChild(child);
        this.mStackScrollAlgorithm.notifyChildrenChanged(this);
        ((ExpandableView) child).setOnHeightChangedListener(this);
        generateAddAnimation(child, false);
        updateAnimationState(child);
        if (canChildBeDismissed(child)) {
            this.mDismissView.showClearButton();
        }
    }

    private void updateHideSensitiveForChild(View child) {
        if (this.mAmbientState.isHideSensitive() && (child instanceof ExpandableView)) {
            ((ExpandableView) child).setHideSensitiveForIntrinsicHeight(true);
        }
    }

    public void notifyGroupChildRemoved(View row) {
        onViewRemovedInternal(row);
    }

    public void notifyGroupChildAdded(View row) {
        onViewAddedInternal(row);
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.mAnimationsEnabled = animationsEnabled;
        updateNotificationAnimationStates();
    }

    private void updateNotificationAnimationStates() {
        boolean running = this.mAnimationsEnabled;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            running &= !this.mIsExpanded ? isPinnedHeadsUp(child) : 1;
            updateAnimationState(running, child);
        }
    }

    private void updateAnimationState(View child) {
        boolean z = (this.mAnimationsEnabled || isPinnedHeadsUp(child)) ? this.mIsExpanded : false;
        updateAnimationState(z, child);
    }

    private void updateAnimationState(boolean running, View child) {
        if (child instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) child).setIconAnimationRunning(running);
        }
    }

    public boolean isAddOrRemoveAnimationPending() {
        if (!this.mNeedsAnimation) {
            return false;
        }
        if (this.mChildrenToAddAnimated.isEmpty() && this.mChildrenToRemoveAnimated.isEmpty()) {
            return false;
        }
        return true;
    }

    public void generateAddAnimation(View child, boolean fromMoreCard) {
        if (this.mIsExpanded && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mChildrenToAddAnimated.add(child);
            if (fromMoreCard) {
                this.mFromMoreCardAdditions.add(child);
            }
            this.mNeedsAnimation = true;
        }
        if (isHeadsUp(child)) {
            this.mAddedHeadsUpChildren.add(child);
            this.mChildrenToAddAnimated.remove(child);
        }
    }

    public void changeViewPosition(View child, int newIndex) {
        int currentIndex = indexOfChild(child);
        if (child != null && child.getParent() == this && currentIndex != newIndex) {
            this.mChangePositionInProgress = true;
            removeView(child);
            addView(child, newIndex);
            this.mChangePositionInProgress = false;
            if (this.mIsExpanded && this.mAnimationsEnabled && child.getVisibility() != 8) {
                this.mChildrenChangingPositions.add(child);
                this.mNeedsAnimation = true;
            }
        }
    }

    private void startAnimationToState() {
        if (this.mNeedsAnimation) {
            generateChildHierarchyEvents();
            this.mNeedsAnimation = false;
        }
        if (!this.mAnimationEvents.isEmpty() || isCurrentlyAnimating()) {
            this.mStateAnimator.startAnimationForEvents(this.mAnimationEvents, this.mCurrentStackScrollState, this.mGoToFullShadeDelay);
            this.mAnimationEvents.clear();
        } else {
            applyCurrentState();
        }
        this.mGoToFullShadeDelay = 0;
    }

    private void generateChildHierarchyEvents() {
        generateHeadsUpAnimationEvents();
        generateChildRemovalEvents();
        generateChildAdditionEvents();
        generatePositionChangeEvents();
        generateSnapBackEvents();
        generateDragEvents();
        generateTopPaddingEvent();
        generateActivateEvent();
        generateDimmedEvent();
        generateHideSensitiveEvent();
        generateDarkEvent();
        generateGoToFullShadeEvent();
        generateViewResizeEvent();
        generateGroupExpansionEvent();
        generateAnimateEverythingEvent();
        this.mNeedsAnimation = false;
    }

    private void generateHeadsUpAnimationEvents() {
        for (Pair<ExpandableNotificationRow, Boolean> eventPair : this.mHeadsUpChangeAnimations) {
            ExpandableNotificationRow row = eventPair.first;
            boolean isHeadsUp = ((Boolean) eventPair.second).booleanValue();
            int type = 16;
            boolean onBottom = false;
            boolean pinnedAndClosed = row.isPinned() && !this.mIsExpanded;
            if (this.mIsExpanded || isHeadsUp) {
                StackViewState viewState = this.mCurrentStackScrollState.getViewStateForView(row);
                if (viewState != null) {
                    if (isHeadsUp && (this.mAddedHeadsUpChildren.contains(row) || pinnedAndClosed)) {
                        if (pinnedAndClosed || shouldHunAppearFromBottom(viewState)) {
                            type = 14;
                        } else {
                            type = 0;
                        }
                        onBottom = !pinnedAndClosed;
                    }
                }
            } else {
                type = 15;
            }
            AnimationEvent event = new AnimationEvent(row, type);
            event.headsUpFromBottom = onBottom;
            this.mAnimationEvents.add(event);
        }
        this.mHeadsUpChangeAnimations.clear();
        this.mAddedHeadsUpChildren.clear();
    }

    private boolean shouldHunAppearFromBottom(StackViewState viewState) {
        if (viewState.yTranslation + ((float) viewState.height) < this.mAmbientState.getMaxHeadsUpTranslation()) {
            return false;
        }
        return true;
    }

    private void generateGroupExpansionEvent() {
        if (this.mExpandedGroupView != null) {
            this.mAnimationEvents.add(new AnimationEvent(this.mExpandedGroupView, 13));
            this.mExpandedGroupView = null;
        }
    }

    private void generateViewResizeEvent() {
        if (this.mNeedViewResizeAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 12));
        }
        this.mNeedViewResizeAnimation = false;
    }

    private void generateSnapBackEvents() {
        for (View child : this.mSnappedBackChildren) {
            this.mAnimationEvents.add(new AnimationEvent(child, 5));
        }
        this.mSnappedBackChildren.clear();
    }

    private void generateDragEvents() {
        for (View child : this.mDragAnimPendingChildren) {
            this.mAnimationEvents.add(new AnimationEvent(child, 4));
        }
        this.mDragAnimPendingChildren.clear();
    }

    private void generateChildRemovalEvents() {
        for (View child : this.mChildrenToRemoveAnimated) {
            int animationType;
            if (this.mSwipedOutViews.contains(child)) {
                animationType = 2;
            } else {
                animationType = 1;
            }
            AnimationEvent event = new AnimationEvent(child, animationType);
            event.viewAfterChangingView = getFirstChildBelowTranlsationY(child.getTranslationY());
            this.mAnimationEvents.add(event);
        }
        this.mSwipedOutViews.clear();
        this.mChildrenToRemoveAnimated.clear();
    }

    private void generatePositionChangeEvents() {
        for (View child : this.mChildrenChangingPositions) {
            this.mAnimationEvents.add(new AnimationEvent(child, 8));
        }
        this.mChildrenChangingPositions.clear();
        if (this.mGenerateChildOrderChangedEvent) {
            this.mAnimationEvents.add(new AnimationEvent(null, 8));
            this.mGenerateChildOrderChangedEvent = false;
        }
    }

    private void generateChildAdditionEvents() {
        for (View child : this.mChildrenToAddAnimated) {
            if (this.mFromMoreCardAdditions.contains(child)) {
                this.mAnimationEvents.add(new AnimationEvent(child, 0, 360));
            } else {
                this.mAnimationEvents.add(new AnimationEvent(child, 0));
            }
        }
        this.mChildrenToAddAnimated.clear();
        this.mFromMoreCardAdditions.clear();
    }

    private void generateTopPaddingEvent() {
        if (this.mTopPaddingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 3));
        }
        this.mTopPaddingNeedsAnimation = false;
    }

    private void generateActivateEvent() {
        if (this.mActivateNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 6));
        }
        this.mActivateNeedsAnimation = false;
    }

    private void generateAnimateEverythingEvent() {
        if (this.mEverythingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 17));
        }
        this.mEverythingNeedsAnimation = false;
    }

    private void generateDimmedEvent() {
        if (this.mDimmedNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 7));
        }
        this.mDimmedNeedsAnimation = false;
    }

    private void generateHideSensitiveEvent() {
        if (this.mHideSensitiveNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 11));
        }
        this.mHideSensitiveNeedsAnimation = false;
    }

    private void generateDarkEvent() {
        if (this.mDarkNeedsAnimation) {
            AnimationEvent ev = new AnimationEvent(null, 9);
            ev.darkAnimationOriginIndex = this.mDarkAnimationOriginIndex;
            this.mAnimationEvents.add(ev);
        }
        this.mDarkNeedsAnimation = false;
    }

    private void generateGoToFullShadeEvent() {
        if (this.mGoToFullShadeNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 10));
        }
        this.mGoToFullShadeNeedsAnimation = false;
    }

    private boolean onInterceptTouchEventScroll(MotionEvent ev) {
        if (!isScrollingEnabled()) {
            return false;
        }
        int action = ev.getAction();
        if (action == 2 && this.mIsBeingDragged) {
            return true;
        }
        int y;
        switch (action & 255) {
            case 0:
                y = (int) ev.getY();
                if (getChildAtPosition(ev.getX(), (float) y) != null) {
                    this.mLastMotionY = y;
                    this.mDownX = (int) ev.getX();
                    this.mActivePointerId = ev.getPointerId(0);
                    this.mScrolledToTopOnFirstDown = isScrolledToTop();
                    initOrResetVelocityTracker();
                    this.mVelocityTracker.addMovement(ev);
                    setIsBeingDragged(!this.mScroller.isFinished());
                    break;
                }
                setIsBeingDragged(false);
                recycleVelocityTracker();
                break;
            case 1:
            case 3:
                setIsBeingDragged(false);
                this.mActivePointerId = -1;
                recycleVelocityTracker();
                if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                    postInvalidateOnAnimation();
                    break;
                }
                break;
            case 2:
                int activePointerId = this.mActivePointerId;
                if (activePointerId != -1) {
                    int pointerIndex = ev.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        y = (int) ev.getY(pointerIndex);
                        int x = (int) ev.getX(pointerIndex);
                        int yDiff = Math.abs(y - this.mLastMotionY);
                        int xDiff = Math.abs(x - this.mDownX);
                        if (yDiff > this.mTouchSlop && yDiff > xDiff) {
                            setIsBeingDragged(true);
                            this.mLastMotionY = y;
                            this.mDownX = x;
                            initVelocityTrackerIfNotExists();
                            this.mVelocityTracker.addMovement(ev);
                            break;
                        }
                    }
                    Log.e("NotificationStackScrollLayout", "Invalid pointerId=" + activePointerId + " in onInterceptTouchEvent");
                    break;
                }
                break;
            case 6:
                onSecondaryPointerUp(ev);
                break;
        }
        return this.mIsBeingDragged;
    }

    private boolean isInContentBounds(MotionEvent event) {
        return isInContentBounds(event.getY());
    }

    public boolean isInContentBounds(float y) {
        return y < ((float) (getHeight() - getEmptyBottomMargin()));
    }

    private void setIsBeingDragged(boolean isDragged) {
        this.mIsBeingDragged = isDragged;
        if (isDragged) {
            requestDisallowInterceptTouchEvent(true);
            removeLongPressCallback();
        }
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            removeLongPressCallback();
        }
    }

    public void removeLongPressCallback() {
        this.mSwipeHelper.removeLongPressCallback();
    }

    public boolean isScrolledToTop() {
        return this.mOwnScrollY == 0;
    }

    public boolean isScrolledToBottom() {
        return this.mOwnScrollY >= getScrollRange();
    }

    public View getHostView() {
        return this;
    }

    public int getEmptyBottomMargin() {
        int emptyMargin = (this.mMaxLayoutHeight - this.mContentHeight) - this.mBottomStackPeekSize;
        if (needsHeightAdaption()) {
            emptyMargin -= this.mBottomStackSlowDownHeight;
        } else {
            emptyMargin -= this.mCollapseSecondCardPadding;
        }
        return Math.max(emptyMargin, 0);
    }

    public void onExpansionStarted() {
        this.mIsExpansionChanging = true;
        this.mStackScrollAlgorithm.onExpansionStarted(this.mCurrentStackScrollState);
    }

    public void onExpansionStopped() {
        this.mIsExpansionChanging = false;
        this.mStackScrollAlgorithm.onExpansionStopped();
        if (!this.mIsExpanded) {
            this.mOwnScrollY = 0;
            getOverlay().clear();
        }
    }

    public void onPanelTrackingStarted() {
        this.mPanelTracking = true;
    }

    public void onPanelTrackingStopped() {
        this.mPanelTracking = false;
    }

    public void resetScrollPosition() {
        this.mScroller.abortAnimation();
        this.mOwnScrollY = 0;
    }

    private void setIsExpanded(boolean isExpanded) {
        boolean changed = isExpanded != this.mIsExpanded;
        this.mIsExpanded = isExpanded;
        this.mStackScrollAlgorithm.setIsExpanded(isExpanded);
        if (changed) {
            updateNotificationAnimationStates();
        }
    }

    public void onHeightChanged(ExpandableView view, boolean needsAnimation) {
        updateContentHeight();
        updateScrollPositionOnExpandInBottom(view);
        clampScrollPosition();
        notifyHeightChangeListener(view);
        if (needsAnimation) {
            requestAnimationOnViewResize();
        }
        requestChildrenUpdate();
    }

    public void onReset(ExpandableView view) {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mRequestViewResizeAnimationOnLayout = true;
        }
        this.mStackScrollAlgorithm.onReset(view);
        updateAnimationState(view);
    }

    private void updateScrollPositionOnExpandInBottom(ExpandableView view) {
        if (view instanceof ExpandableNotificationRow) {
            View row = (ExpandableNotificationRow) view;
            if (row.isUserLocked() && row != getFirstChildNotGone()) {
                float endPosition = row.getTranslationY() + ((float) row.getActualHeight());
                int stackEnd = ((this.mMaxLayoutHeight - this.mBottomStackPeekSize) - this.mBottomStackSlowDownHeight) + ((int) this.mStackTranslation);
                if (endPosition > ((float) stackEnd)) {
                    this.mOwnScrollY = (int) (((float) this.mOwnScrollY) + (endPosition - ((float) stackEnd)));
                    this.mDisallowScrollingInThisMotion = true;
                }
            }
        }
    }

    public void setOnHeightChangedListener(OnHeightChangedListener mOnHeightChangedListener) {
        this.mOnHeightChangedListener = mOnHeightChangedListener;
    }

    public void setOnEmptySpaceClickListener(OnEmptySpaceClickListener listener) {
        this.mOnEmptySpaceClickListener = listener;
    }

    public void onChildAnimationFinished() {
        requestChildrenUpdate();
        runAnimationFinishedRunnables();
        clearViewOverlays();
    }

    private void clearViewOverlays() {
        for (View view : this.mClearOverlayViewsWhenFinished) {
            getOverlay().remove(view);
        }
    }

    private void runAnimationFinishedRunnables() {
        for (Runnable runnable : this.mAnimationFinishedRunnables) {
            runnable.run();
        }
        this.mAnimationFinishedRunnables.clear();
    }

    public void setDimmed(boolean dimmed, boolean animate) {
        this.mStackScrollAlgorithm.setDimmed(dimmed);
        this.mAmbientState.setDimmed(dimmed);
        updatePadding(dimmed);
        if (animate && this.mAnimationsEnabled) {
            this.mDimmedNeedsAnimation = true;
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public void setHideSensitive(boolean hideSensitive, boolean animate) {
        if (hideSensitive != this.mAmbientState.isHideSensitive()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((ExpandableView) getChildAt(i)).setHideSensitiveForIntrinsicHeight(hideSensitive);
            }
            this.mAmbientState.setHideSensitive(hideSensitive);
            if (animate && this.mAnimationsEnabled) {
                this.mHideSensitiveNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
        }
    }

    public void setActivatedChild(ActivatableNotificationView activatedChild) {
        this.mAmbientState.setActivatedChild(activatedChild);
        if (this.mAnimationsEnabled) {
            this.mActivateNeedsAnimation = true;
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mAmbientState.getActivatedChild();
    }

    private void applyCurrentState() {
        this.mCurrentStackScrollState.apply();
        if (this.mListener != null) {
            this.mListener.onChildLocationsChanged(this);
        }
        runAnimationFinishedRunnables();
    }

    public void setSpeedBumpView(SpeedBumpView speedBumpView) {
        this.mSpeedBumpView = speedBumpView;
        addView(speedBumpView);
    }

    private void updateSpeedBump(boolean visible) {
        if (visible != (this.mSpeedBumpView.getVisibility() != 8)) {
            this.mSpeedBumpView.setVisibility(visible ? 0 : 8);
            if (visible) {
                this.mSpeedBumpView.setInvisible();
            } else {
                generateRemoveAnimation(this.mSpeedBumpView);
            }
        }
    }

    public void goToFullShade(long delay) {
        updateSpeedBump(true);
        this.mDismissView.setInvisible();
        this.mEmptyShadeView.setInvisible();
        this.mGoToFullShadeNeedsAnimation = true;
        this.mGoToFullShadeDelay = delay;
        this.mNeedsAnimation = true;
        requestChildrenUpdate();
    }

    public void cancelExpandHelper() {
        this.mExpandHelper.cancel();
    }

    public void setIntrinsicPadding(int intrinsicPadding) {
        this.mIntrinsicPadding = intrinsicPadding;
    }

    public int getIntrinsicPadding() {
        return this.mIntrinsicPadding;
    }

    public float getNotificationsTopY() {
        return ((float) this.mTopPadding) + getStackTranslation();
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public void setDark(boolean dark, boolean animate, PointF touchWakeUpScreenLocation) {
        this.mAmbientState.setDark(dark);
        if (animate && this.mAnimationsEnabled) {
            this.mDarkNeedsAnimation = true;
            this.mDarkAnimationOriginIndex = findDarkAnimationOriginIndex(touchWakeUpScreenLocation);
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    private int findDarkAnimationOriginIndex(PointF screenLocation) {
        if (screenLocation == null || screenLocation.y < ((float) this.mTopPadding) + this.mTopPaddingOverflow) {
            return -1;
        }
        if (screenLocation.y > getBottomMostNotificationBottom()) {
            return -2;
        }
        View child = getClosestChildAtRawPosition(screenLocation.x, screenLocation.y);
        if (child != null) {
            return getNotGoneIndex(child);
        }
        return -1;
    }

    private int getNotGoneIndex(View child) {
        int count = getChildCount();
        int notGoneIndex = 0;
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (child == v) {
                return notGoneIndex;
            }
            if (v.getVisibility() != 8) {
                notGoneIndex++;
            }
        }
        return -1;
    }

    public void setDismissView(DismissView dismissView) {
        this.mDismissView = dismissView;
        addView(this.mDismissView);
    }

    public void setEmptyShadeView(EmptyShadeView emptyShadeView) {
        this.mEmptyShadeView = emptyShadeView;
        addView(this.mEmptyShadeView);
    }

    public void updateEmptyShadeView(boolean visible) {
        int oldVisibility = this.mEmptyShadeView.willBeGone() ? 8 : this.mEmptyShadeView.getVisibility();
        int newVisibility = visible ? 0 : 8;
        if (oldVisibility == newVisibility) {
            return;
        }
        if (newVisibility != 8) {
            if (this.mEmptyShadeView.willBeGone()) {
                this.mEmptyShadeView.cancelAnimation();
            } else {
                this.mEmptyShadeView.setInvisible();
            }
            this.mEmptyShadeView.setVisibility(newVisibility);
            this.mEmptyShadeView.setWillBeGone(false);
            updateContentHeight();
            notifyHeightChangeListener(this.mEmptyShadeView);
            return;
        }
        Runnable onFinishedRunnable = new Runnable() {
            public void run() {
                NotificationStackScrollLayout.this.mEmptyShadeView.setVisibility(8);
                NotificationStackScrollLayout.this.mEmptyShadeView.setWillBeGone(false);
                NotificationStackScrollLayout.this.updateContentHeight();
                NotificationStackScrollLayout.this.notifyHeightChangeListener(NotificationStackScrollLayout.this.mEmptyShadeView);
            }
        };
        if (this.mAnimationsEnabled) {
            this.mEmptyShadeView.setWillBeGone(true);
            this.mEmptyShadeView.performVisibilityAnimation(false, onFinishedRunnable);
            return;
        }
        this.mEmptyShadeView.setInvisible();
        onFinishedRunnable.run();
    }

    public void setOverflowContainer(NotificationOverflowContainer overFlowContainer) {
        this.mOverflowContainer = overFlowContainer;
        addView(this.mOverflowContainer);
    }

    public void updateOverflowContainerVisibility(boolean visible) {
        int oldVisibility;
        if (this.mOverflowContainer.willBeGone()) {
            oldVisibility = 8;
        } else {
            oldVisibility = this.mOverflowContainer.getVisibility();
        }
        final int newVisibility = visible ? 0 : 8;
        if (oldVisibility != newVisibility) {
            Runnable onFinishedRunnable = new Runnable() {
                public void run() {
                    NotificationStackScrollLayout.this.mOverflowContainer.setVisibility(newVisibility);
                    NotificationStackScrollLayout.this.mOverflowContainer.setWillBeGone(false);
                    NotificationStackScrollLayout.this.updateContentHeight();
                    NotificationStackScrollLayout.this.notifyHeightChangeListener(NotificationStackScrollLayout.this.mOverflowContainer);
                }
            };
            if (!this.mAnimationsEnabled || !this.mIsExpanded) {
                this.mOverflowContainer.cancelAppearDrawing();
                onFinishedRunnable.run();
            } else if (newVisibility != 8) {
                this.mOverflowContainer.performAddAnimation(0, 360);
                this.mOverflowContainer.setVisibility(newVisibility);
                this.mOverflowContainer.setWillBeGone(false);
                updateContentHeight();
                notifyHeightChangeListener(this.mOverflowContainer);
            } else {
                this.mOverflowContainer.performRemoveAnimation(360, 0.0f, onFinishedRunnable);
                this.mOverflowContainer.setWillBeGone(true);
            }
        }
    }

    public void updateDismissView(boolean visible) {
        int oldVisibility = this.mDismissView.willBeGone() ? 8 : this.mDismissView.getVisibility();
        int newVisibility = visible ? 0 : 8;
        if (oldVisibility == newVisibility) {
            return;
        }
        if (newVisibility != 8) {
            if (this.mDismissView.willBeGone()) {
                this.mDismissView.cancelAnimation();
            } else {
                this.mDismissView.setInvisible();
            }
            this.mDismissView.setVisibility(newVisibility);
            this.mDismissView.setWillBeGone(false);
            updateContentHeight();
            notifyHeightChangeListener(this.mDismissView);
            return;
        }
        Runnable dimissHideFinishRunnable = new Runnable() {
            public void run() {
                NotificationStackScrollLayout.this.mDismissView.setVisibility(8);
                NotificationStackScrollLayout.this.mDismissView.setWillBeGone(false);
                NotificationStackScrollLayout.this.updateContentHeight();
                NotificationStackScrollLayout.this.notifyHeightChangeListener(NotificationStackScrollLayout.this.mDismissView);
            }
        };
        if (this.mDismissView.isButtonVisible() && this.mIsExpanded && this.mAnimationsEnabled) {
            this.mDismissView.setWillBeGone(true);
            this.mDismissView.performVisibilityAnimation(false, dimissHideFinishRunnable);
            return;
        }
        dimissHideFinishRunnable.run();
        this.mDismissView.showClearButton();
    }

    public void setDismissAllInProgress(boolean dismissAllInProgress) {
        this.mDismissAllInProgress = dismissAllInProgress;
        this.mDismissView.setDismissAllInProgress(dismissAllInProgress);
        this.mAmbientState.setDismissAllInProgress(dismissAllInProgress);
        if (dismissAllInProgress) {
            disableClipOptimization();
        }
        handleDismissAllClipping();
    }

    private void handleDismissAllClipping() {
        int count = getChildCount();
        boolean previousChildWillBeDismissed = false;
        for (int i = 0; i < count; i++) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            if (child.getVisibility() != 8) {
                if (this.mDismissAllInProgress && r3) {
                    child.setMinClipTopAmount(child.getClipTopAmount());
                } else {
                    child.setMinClipTopAmount(0);
                }
                previousChildWillBeDismissed = canChildBeDismissed(child);
            }
        }
    }

    private void disableClipOptimization() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            if (child.getVisibility() != 8) {
                child.setClipTopOptimization(0);
            }
        }
    }

    public boolean isDismissViewNotGone() {
        return (this.mDismissView.getVisibility() == 8 || this.mDismissView.willBeGone()) ? false : true;
    }

    public boolean isDismissViewVisible() {
        return this.mDismissView.isVisible();
    }

    public int getDismissViewHeight() {
        int height = this.mDismissView.getHeight() + this.mPaddingBetweenElementsNormal;
        if (getNotGoneChildCount() == 2 && getLastChildNotGone() == this.mDismissView && (getFirstChildNotGone() instanceof ActivatableNotificationView)) {
            return height + this.mCollapseSecondCardPadding;
        }
        return height;
    }

    public int getEmptyShadeViewHeight() {
        return this.mEmptyShadeView.getHeight();
    }

    public float getBottomMostNotificationBottom() {
        int count = getChildCount();
        float max = 0.0f;
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView child = (ExpandableView) getChildAt(childIdx);
            if (child.getVisibility() != 8) {
                float bottom = child.getTranslationY() + ((float) child.getActualHeight());
                if (bottom > max) {
                    max = bottom;
                }
            }
        }
        return getStackTranslation() + max;
    }

    public void updateIsSmallScreen(int qsMinHeight) {
        this.mStackScrollAlgorithm.updateIsSmallScreen(this.mMaxLayoutHeight - qsMinHeight);
    }

    public void setPhoneStatusBar(PhoneStatusBar phoneStatusBar) {
        this.mPhoneStatusBar = phoneStatusBar;
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }

    public void onGoToKeyguard() {
        requestAnimateEverything();
    }

    private void requestAnimateEverything() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mEverythingNeedsAnimation = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public boolean isBelowLastNotification(float touchX, float touchY) {
        boolean z = true;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            if (child.getVisibility() != 8) {
                float childTop = child.getY();
                if (childTop > touchY) {
                    return false;
                }
                boolean belowChild = touchY > ((float) child.getActualHeight()) + childTop;
                if (child == this.mDismissView) {
                    if (!(belowChild || this.mDismissView.isOnEmptySpace(touchX - this.mDismissView.getX(), touchY - childTop))) {
                        return false;
                    }
                } else if (child == this.mEmptyShadeView) {
                    return true;
                } else {
                    if (!belowChild) {
                        return false;
                    }
                }
            }
        }
        if (touchY <= ((float) this.mTopPadding) + this.mStackTranslation) {
            z = false;
        }
        return z;
    }

    private void updateExpandButtons() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) child).updateExpandButton();
            }
        }
    }

    public void onGroupExpansionChanged(ExpandableNotificationRow changedRow, boolean expanded) {
        boolean z = this.mAnimationsEnabled ? this.mIsExpanded : false;
        if (z) {
            this.mExpandedGroupView = changedRow;
            this.mNeedsAnimation = true;
        }
        changedRow.setChildrenExpanded(expanded, z);
        onHeightChanged(changedRow, false);
    }

    public void onGroupsProhibitedChanged() {
        updateExpandButtons();
    }

    public void onGroupCreatedFromChildren(NotificationGroup group) {
        for (Entry entry : group.children) {
            ExpandableNotificationRow row = entry.row;
            if (indexOfChild(row) != -1) {
                removeView(row);
                group.summary.row.addChildNotification(row);
            }
        }
    }

    public void generateChildOrderChangedEvent() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mGenerateChildOrderChangedEvent = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public void runAfterAnimationFinished(Runnable runnable) {
        this.mAnimationFinishedRunnables.add(runnable);
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
        this.mAmbientState.setHeadsUpManager(headsUpManager);
        this.mStackScrollAlgorithm.setHeadsUpManager(headsUpManager);
    }

    public void generateHeadsUpAnimation(ExpandableNotificationRow row, boolean isHeadsUp) {
        if (this.mAnimationsEnabled) {
            this.mHeadsUpChangeAnimations.add(new Pair(row, Boolean.valueOf(isHeadsUp)));
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public void setShadeExpanded(boolean shadeExpanded) {
        this.mAmbientState.setShadeExpanded(shadeExpanded);
        this.mStateAnimator.setShadeExpanded(shadeExpanded);
    }

    public void setHeadsUpBoundaries(int height, int bottomBarHeight) {
        this.mAmbientState.setMaxHeadsUpTranslation((float) (height - bottomBarHeight));
        this.mStateAnimator.setHeadsUpAppearHeightBottom(height);
        requestChildrenUpdate();
    }

    public void setTrackingHeadsUp(boolean trackingHeadsUp) {
        this.mTrackingHeadsUp = trackingHeadsUp;
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    public void forceNoOverlappingRendering(boolean force) {
        this.mForceNoOverlappingRendering = force;
    }

    public boolean hasOverlappingRendering() {
        return !this.mForceNoOverlappingRendering ? super.hasOverlappingRendering() : false;
    }
}
