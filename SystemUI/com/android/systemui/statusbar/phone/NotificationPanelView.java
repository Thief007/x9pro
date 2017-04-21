package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.DejankUtils;
import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.EVENT;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.ExpandableView.OnHeightChangedListener;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm.Result;
import com.android.systemui.statusbar.phone.ObservableScrollView.Listener;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.HeadsUpManager.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout.OnEmptySpaceClickListener;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout.OnOverscrollTopChangedListener;
import com.mediatek.keyguard.Clock.ClockView;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;

public class NotificationPanelView extends PanelView implements OnHeightChangedListener, Listener, OnClickListener, OnOverscrollTopChangedListener, Callback, OnEmptySpaceClickListener, OnHeadsUpChangedListener {
    private static boolean bA1Support = SystemProperties.get("ro.mtk_a1_feature").equals(FeatureOptionUtils.SUPPORT_YES);
    private static final Rect mDummyDirtyRect = new Rect(0, 0, 1, 1);
    private KeyguardAffordanceHelper mAfforanceHelper;
    private final AnimatorListener mAnimateHeaderSlidingInListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            NotificationPanelView.this.mHeaderAnimating = false;
            NotificationPanelView.this.mQsContainerAnimator = null;
            NotificationPanelView.this.mQsContainer.removeOnLayoutChangeListener(NotificationPanelView.this.mQsContainerAnimatorUpdater);
        }
    };
    private final Runnable mAnimateKeyguardBottomAreaInvisibleEndRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.mKeyguardBottomArea.setVisibility(8);
        }
    };
    private final Runnable mAnimateKeyguardStatusBarInvisibleEndRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.mKeyguardStatusBar.setVisibility(4);
            NotificationPanelView.this.mKeyguardStatusBar.setAlpha(1.0f);
            NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = 1.0f;
        }
    };
    private final Runnable mAnimateKeyguardStatusViewInvisibleEndRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
            NotificationPanelView.this.mKeyguardStatusView.setVisibility(8);
        }
    };
    private final Runnable mAnimateKeyguardStatusViewVisibleEndRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
        }
    };
    private boolean mAnimateNextTopPaddingChange;
    private boolean mBlockTouches;
    private int mClockAnimationTarget = -1;
    private ObjectAnimator mClockAnimator;
    private KeyguardClockPositionAlgorithm mClockPositionAlgorithm = new KeyguardClockPositionAlgorithm();
    private Result mClockPositionResult = new Result();
    private TextView mClockView;
    private boolean mClosingWithAlphaFadeOut;
    private boolean mCollapsedOnDown;
    private boolean mConflictingQsExpansionGesture;
    private Interpolator mDozeAnimationInterpolator;
    private boolean mDozing;
    private boolean mDozingOnDown;
    private float mEmptyDragAmount;
    private boolean mExpandingFromHeadsUp;
    private Interpolator mFastOutLinearInterpolator;
    private Interpolator mFastOutSlowInInterpolator;
    private FlingAnimationUtils mFlingAnimationUtils;
    private StatusBarHeaderView mHeader;
    private boolean mHeaderAnimating;
    private boolean mHeadsUpAnimatingAway;
    private Runnable mHeadsUpExistenceChangedRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.mHeadsUpAnimatingAway = false;
            NotificationPanelView.this.notifyBarPanelExpansionChanged();
        }
    };
    private HeadsUpTouchHelper mHeadsUpTouchHelper;
    private float mInitialHeightOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mIntercepting;
    private boolean mIsExpanding;
    private boolean mIsExpansionFromHeadsUp;
    private boolean mIsLaunchTransitionFinished;
    private boolean mIsLaunchTransitionRunning;
    private boolean mKeyguardShowing;
    private KeyguardStatusBarView mKeyguardStatusBar;
    private float mKeyguardStatusBarAnimateAlpha = 1.0f;
    private KeyguardStatusView mKeyguardStatusView;
    private boolean mKeyguardStatusViewAnimating;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mLastAnnouncementWasQuickSettings;
    private int mLastOrientation = -1;
    private float mLastOverscroll;
    private float mLastTouchX;
    private float mLastTouchY;
    private Runnable mLaunchAnimationEndRunnable;
    private boolean mListenForHeadsUp;
    private ClockView mMtkClockView;
    private int mNavigationBarBottomHeight;
    private NotificationsQuickSettingsContainer mNotificationContainerParent;
    private int mNotificationScrimWaitDistance;
    private NotificationStackScrollLayout mNotificationStackScroller;
    private int mNotificationTopPadding;
    private int mNotificationsHeaderCollideDistance;
    private int mOldLayoutDirection;
    private boolean mOnlyAffordanceInThisMotion;
    private boolean mPanelExpanded;
    private int mPositionMinSideMargin;
    private boolean mQsAnimatorExpand;
    private QSContainer mQsContainer;
    private ObjectAnimator mQsContainerAnimator;
    private final OnLayoutChangeListener mQsContainerAnimatorUpdater = new OnLayoutChangeListener() {
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int height = bottom - top;
            if (height != oldBottom - oldTop && NotificationPanelView.this.mQsContainerAnimator != null) {
                float newEndValue = (float) (((NotificationPanelView.this.mHeader.getCollapsedHeight() + NotificationPanelView.this.mQsPeekHeight) - height) - top);
                float newStartValue = (float) ((-height) - top);
                NotificationPanelView.this.mQsContainerAnimator.getValues()[0].setFloatValues(new float[]{newStartValue, newEndValue});
                NotificationPanelView.this.mQsContainerAnimator.setCurrentPlayTime(NotificationPanelView.this.mQsContainerAnimator.getCurrentPlayTime());
            }
        }
    };
    private boolean mQsExpandImmediate;
    private boolean mQsExpanded;
    private boolean mQsExpandedWhenExpandingStarted;
    private ValueAnimator mQsExpansionAnimator;
    private boolean mQsExpansionEnabled = true;
    private boolean mQsExpansionFromOverscroll;
    private float mQsExpansionHeight;
    private int mQsFalsingThreshold;
    private boolean mQsFullyExpanded;
    private int mQsMaxExpansionHeight;
    private int mQsMinExpansionHeight;
    private View mQsNavbarScrim;
    private QSPanel mQsPanel;
    private int mQsPeekHeight;
    private boolean mQsScrimEnabled = true;
    private ValueAnimator mQsSizeChangeAnimator;
    private boolean mQsTouchAboveFalsingThreshold;
    private boolean mQsTracking;
    private View mReserveNotificationSpace;
    private ObservableScrollView mScrollView;
    private int mScrollYOverride = -1;
    private SecureCameraLaunchManager mSecureCameraLaunchManager;
    private boolean mShadeEmpty;
    private boolean mStackScrollerOverscrolling;
    private final OnPreDrawListener mStartHeaderSlidingIn = new OnPreDrawListener() {
        public boolean onPreDraw() {
            long delay;
            NotificationPanelView.this.getViewTreeObserver().removeOnPreDrawListener(this);
            if (NotificationPanelView.this.mStatusBarState == 2) {
                delay = 0;
            } else {
                delay = NotificationPanelView.this.mStatusBar.calculateGoingToFullShadeDelay();
            }
            NotificationPanelView.this.mHeader.setTranslationY((float) ((-NotificationPanelView.this.mHeader.getCollapsedHeight()) - NotificationPanelView.this.mQsPeekHeight));
            NotificationPanelView.this.mHeader.animate().translationY(0.0f).setStartDelay(delay).setDuration(448).setInterpolator(NotificationPanelView.this.mFastOutSlowInInterpolator).start();
            NotificationPanelView.this.mQsContainer.setY((float) (-NotificationPanelView.this.mQsContainer.getHeight()));
            NotificationPanelView.this.mQsContainerAnimator = ObjectAnimator.ofFloat(NotificationPanelView.this.mQsContainer, View.TRANSLATION_Y, new float[]{NotificationPanelView.this.mQsContainer.getTranslationY(), (float) (((NotificationPanelView.this.mHeader.getCollapsedHeight() + NotificationPanelView.this.mQsPeekHeight) - NotificationPanelView.this.mQsContainer.getHeight()) - NotificationPanelView.this.mQsContainer.getTop())});
            NotificationPanelView.this.mQsContainerAnimator.setStartDelay(delay);
            NotificationPanelView.this.mQsContainerAnimator.setDuration(448);
            NotificationPanelView.this.mQsContainerAnimator.setInterpolator(NotificationPanelView.this.mFastOutSlowInInterpolator);
            NotificationPanelView.this.mQsContainerAnimator.addListener(NotificationPanelView.this.mAnimateHeaderSlidingInListener);
            NotificationPanelView.this.mQsContainerAnimator.start();
            NotificationPanelView.this.mQsContainer.addOnLayoutChangeListener(NotificationPanelView.this.mQsContainerAnimatorUpdater);
            return true;
        }
    };
    private final AnimatorUpdateListener mStatusBarAnimateAlphaListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = ((Float) animation.getAnimatedValue()).floatValue();
            NotificationPanelView.this.updateHeaderKeyguardAlpha();
        }
    };
    private int mStatusBarMinHeight;
    private int mStatusBarState;
    private int mTopPaddingAdjustment;
    private final Interpolator mTouchResponseInterpolator = new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);
    private int mTrackingPointer;
    private boolean mTwoFingerQsExpandPossible;
    private boolean mUnlockIconActive;
    private int mUnlockMoveDistance;
    private final Runnable mUpdateHeader = new Runnable() {
        public void run() {
            NotificationPanelView.this.mHeader.updateEverything();
        }
    };
    private VelocityTracker mVelocityTracker;

    public NotificationPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(true);
    }

    public void setStatusBar(PhoneStatusBar bar) {
        this.mStatusBar = bar;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHeader = (StatusBarHeaderView) findViewById(R.id.header);
        this.mHeader.setOnClickListener(this);
        this.mKeyguardStatusBar = (KeyguardStatusBarView) findViewById(R.id.keyguard_header);
        this.mKeyguardStatusView = (KeyguardStatusView) findViewById(R.id.keyguard_status_view);
        this.mQsContainer = (QSContainer) findViewById(R.id.quick_settings_container);
        this.mQsPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
        if (bA1Support) {
            this.mClockView = (TextView) findViewById(R.id.clock_view);
        } else {
            this.mMtkClockView = (ClockView) findViewById(R.id.clock_view);
        }
        this.mScrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        this.mScrollView.setListener(this);
        this.mScrollView.setFocusable(false);
        this.mReserveNotificationSpace = findViewById(R.id.reserve_notification_space);
        this.mNotificationContainerParent = (NotificationsQuickSettingsContainer) findViewById(R.id.notification_container_parent);
        this.mNotificationStackScroller = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationStackScroller.setOnHeightChangedListener(this);
        this.mNotificationStackScroller.setOverscrollTopChangedListener(this);
        this.mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        this.mNotificationStackScroller.setScrollView(this.mScrollView);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(), 17563661);
        this.mFastOutLinearInterpolator = AnimationUtils.loadInterpolator(getContext(), 17563663);
        this.mDozeAnimationInterpolator = AnimationUtils.loadInterpolator(getContext(), 17563662);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) findViewById(R.id.keyguard_bottom_area);
        this.mQsNavbarScrim = findViewById(R.id.qs_navbar_scrim);
        this.mAfforanceHelper = new KeyguardAffordanceHelper(this, getContext());
        this.mSecureCameraLaunchManager = new SecureCameraLaunchManager(getContext(), this.mKeyguardBottomArea);
        this.mLastOrientation = getResources().getConfiguration().orientation;
        this.mQsContainer.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom - top != oldBottom - oldTop) {
                    NotificationPanelView.this.onScrollChanged();
                }
            }
        });
    }

    protected void loadDimens() {
        super.loadDimens();
        this.mNotificationTopPadding = getResources().getDimensionPixelSize(R.dimen.notifications_top_padding);
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        this.mStatusBarMinHeight = getResources().getDimensionPixelSize(17104919);
        this.mQsPeekHeight = getResources().getDimensionPixelSize(R.dimen.qs_peek_height);
        this.mNotificationsHeaderCollideDistance = getResources().getDimensionPixelSize(R.dimen.header_notifications_collide_distance);
        this.mUnlockMoveDistance = getResources().getDimensionPixelOffset(R.dimen.unlock_move_distance);
        this.mClockPositionAlgorithm.loadDimens(getResources());
        this.mNotificationScrimWaitDistance = getResources().getDimensionPixelSize(R.dimen.notification_scrim_wait_distance);
        this.mQsFalsingThreshold = getResources().getDimensionPixelSize(R.dimen.qs_falsing_threshold);
        this.mPositionMinSideMargin = getResources().getDimensionPixelSize(R.dimen.notification_panel_min_side_margin);
    }

    public void updateResources() {
        int panelWidth = getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        int panelGravity = getResources().getInteger(R.integer.notification_panel_layout_gravity);
        LayoutParams lp = (LayoutParams) this.mHeader.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            this.mHeader.setLayoutParams(lp);
            this.mHeader.post(this.mUpdateHeader);
        }
        lp = (LayoutParams) this.mNotificationStackScroller.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            this.mNotificationStackScroller.setLayoutParams(lp);
        }
        lp = (LayoutParams) this.mScrollView.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            this.mScrollView.setLayoutParams(lp);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int i;
        super.onLayout(changed, left, top, right, bottom);
        this.mKeyguardStatusView.setPivotX((float) (getWidth() / 2));
        if (bA1Support) {
            this.mKeyguardStatusView.setPivotY(this.mClockView.getTextSize() * 0.34521484f);
        } else {
            this.mKeyguardStatusView.setPivotY(((float) this.mMtkClockView.getHeight()) * 0.34521484f);
        }
        int oldMaxHeight = this.mQsMaxExpansionHeight;
        if (this.mKeyguardShowing) {
            i = 0;
        } else {
            i = this.mHeader.getCollapsedHeight() + this.mQsPeekHeight;
        }
        this.mQsMinExpansionHeight = i;
        this.mQsMaxExpansionHeight = this.mHeader.getExpandedHeight() + this.mQsContainer.getDesiredHeight();
        positionClockAndNotifications();
        if (this.mQsExpanded && this.mQsFullyExpanded) {
            this.mQsExpansionHeight = (float) this.mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
            if (this.mQsMaxExpansionHeight != oldMaxHeight) {
                startQsSizeChangeAnimation(oldMaxHeight, this.mQsMaxExpansionHeight);
            }
        } else if (!this.mQsExpanded) {
            setQsExpansion(((float) this.mQsMinExpansionHeight) + this.mLastOverscroll);
        }
        updateStackHeight(getExpandedHeight());
        updateHeader();
        this.mNotificationStackScroller.updateIsSmallScreen(this.mHeader.getCollapsedHeight() + this.mQsPeekHeight);
        if (this.mQsSizeChangeAnimator == null) {
            this.mQsContainer.setHeightOverride(this.mQsContainer.getDesiredHeight());
        }
        updateMaxHeadsUpTranslation();
    }

    public void onAttachedToWindow() {
        this.mSecureCameraLaunchManager.create();
    }

    public void onDetachedFromWindow() {
        this.mSecureCameraLaunchManager.destroy();
    }

    private void startQsSizeChangeAnimation(int oldHeight, int newHeight) {
        if (this.mQsSizeChangeAnimator != null) {
            oldHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
            this.mQsSizeChangeAnimator.cancel();
        }
        this.mQsSizeChangeAnimator = ValueAnimator.ofInt(new int[]{oldHeight, newHeight});
        this.mQsSizeChangeAnimator.setDuration(300);
        this.mQsSizeChangeAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
        this.mQsSizeChangeAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.requestScrollerTopPaddingUpdate(false);
                NotificationPanelView.this.requestPanelHeightUpdate();
                NotificationPanelView.this.mQsContainer.setHeightOverride(((Integer) NotificationPanelView.this.mQsSizeChangeAnimator.getAnimatedValue()).intValue() - NotificationPanelView.this.mHeader.getExpandedHeight());
            }
        });
        this.mQsSizeChangeAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mQsSizeChangeAnimator = null;
            }
        });
        this.mQsSizeChangeAnimator.start();
    }

    private void positionClockAndNotifications() {
        int stackScrollerPadding;
        boolean animate = this.mNotificationStackScroller.isAddOrRemoveAnimationPending();
        if (this.mStatusBarState != 1) {
            int bottom = this.mHeader.getCollapsedHeight();
            if (this.mStatusBarState == 0) {
                stackScrollerPadding = (this.mQsPeekHeight + bottom) + this.mNotificationTopPadding;
            } else {
                stackScrollerPadding = this.mKeyguardStatusBar.getHeight() + this.mNotificationTopPadding;
            }
            this.mTopPaddingAdjustment = 0;
        } else {
            this.mClockPositionAlgorithm.setup(this.mStatusBar.getMaxKeyguardNotifications(), getMaxPanelHeight(), getExpandedHeight(), this.mNotificationStackScroller.getNotGoneChildCount(), getHeight(), this.mKeyguardStatusView.getHeight(), this.mEmptyDragAmount);
            this.mClockPositionAlgorithm.run(this.mClockPositionResult);
            if (animate || this.mClockAnimator != null) {
                startClockAnimation(this.mClockPositionResult.clockY);
            } else {
                this.mKeyguardStatusView.setY((float) this.mClockPositionResult.clockY);
            }
            updateClock(this.mClockPositionResult.clockAlpha, this.mClockPositionResult.clockScale);
            stackScrollerPadding = this.mClockPositionResult.stackScrollerPadding;
            this.mTopPaddingAdjustment = this.mClockPositionResult.stackScrollerPaddingAdjustment;
        }
        this.mNotificationStackScroller.setIntrinsicPadding(stackScrollerPadding);
        requestScrollerTopPaddingUpdate(animate);
    }

    private void startClockAnimation(int y) {
        if (this.mClockAnimationTarget != y) {
            this.mClockAnimationTarget = y;
            getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                public boolean onPreDraw() {
                    NotificationPanelView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (NotificationPanelView.this.mClockAnimator != null) {
                        NotificationPanelView.this.mClockAnimator.removeAllListeners();
                        NotificationPanelView.this.mClockAnimator.cancel();
                    }
                    NotificationPanelView.this.mClockAnimator = ObjectAnimator.ofFloat(NotificationPanelView.this.mKeyguardStatusView, View.Y, new float[]{(float) NotificationPanelView.this.mClockAnimationTarget});
                    NotificationPanelView.this.mClockAnimator.setInterpolator(NotificationPanelView.this.mFastOutSlowInInterpolator);
                    NotificationPanelView.this.mClockAnimator.setDuration(360);
                    NotificationPanelView.this.mClockAnimator.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            NotificationPanelView.this.mClockAnimator = null;
                            NotificationPanelView.this.mClockAnimationTarget = -1;
                        }
                    });
                    NotificationPanelView.this.mClockAnimator.start();
                    return true;
                }
            });
        }
    }

    private void updateClock(float alpha, float scale) {
        if (!this.mKeyguardStatusViewAnimating) {
            this.mKeyguardStatusView.setAlpha(alpha);
        }
        this.mKeyguardStatusView.setScaleX(scale);
        this.mKeyguardStatusView.setScaleY(scale);
    }

    public void animateToFullShade(long delay) {
        this.mAnimateNextTopPaddingChange = true;
        this.mNotificationStackScroller.goToFullShade(delay);
        requestLayout();
    }

    public void setQsExpansionEnabled(boolean qsExpansionEnabled) {
        this.mQsExpansionEnabled = qsExpansionEnabled;
        this.mHeader.setClickable(qsExpansionEnabled);
    }

    public void resetViews() {
        this.mIsLaunchTransitionFinished = false;
        this.mBlockTouches = false;
        this.mUnlockIconActive = false;
        this.mAfforanceHelper.reset(true);
        closeQs();
        this.mStatusBar.dismissPopups();
        this.mNotificationStackScroller.setOverScrollAmount(0.0f, true, false, true);
        this.mNotificationStackScroller.resetScrollPosition();
    }

    public void closeQs() {
        cancelQsAnimation();
        setQsExpansion((float) this.mQsMinExpansionHeight);
    }

    public void animateCloseQs() {
        if (this.mQsExpansionAnimator != null) {
            if (this.mQsAnimatorExpand) {
                float height = this.mQsExpansionHeight;
                this.mQsExpansionAnimator.cancel();
                setQsExpansion(height);
            } else {
                return;
            }
        }
        flingSettings(0.0f, false);
    }

    public void expandWithQs() {
        if (this.mQsExpansionEnabled) {
            this.mQsExpandImmediate = true;
        }
        expand();
    }

    public void fling(float vel, boolean expand) {
        GestureRecorder gr = ((PhoneStatusBarView) this.mBar).mBar.getGestureRecorder();
        if (gr != null) {
            gr.tag("fling " + (vel > 0.0f ? "open" : "closed"), "notifications,v=" + vel);
        }
        super.fling(vel, expand);
    }

    protected void flingToHeight(float vel, boolean expand, float target, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        boolean z;
        boolean z2 = false;
        HeadsUpTouchHelper headsUpTouchHelper = this.mHeadsUpTouchHelper;
        if (expand) {
            z = false;
        } else {
            z = true;
        }
        headsUpTouchHelper.notifyFling(z);
        if (!expand && getFadeoutAlpha() == 1.0f) {
            z2 = true;
        }
        setClosingWithAlphaFadeout(z2);
        super.flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (event.getEventType() != 32) {
            return super.dispatchPopulateAccessibilityEventInternal(event);
        }
        event.getText().add(getKeyguardOrLockScreenString());
        this.mLastAnnouncementWasQuickSettings = false;
        return true;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mBlockTouches) {
            return false;
        }
        initDownStates(event);
        if (this.mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open", 1);
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
            return true;
        } else if (isFullyCollapsed() || !onQsIntercept(event)) {
            return super.onInterceptTouchEvent(event);
        } else {
            return true;
        }
    }

    private boolean onQsIntercept(MotionEvent event) {
        boolean z = true;
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        switch (event.getActionMasked()) {
            case 0:
                this.mIntercepting = true;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                initVelocityTracker();
                trackMovement(event);
                if (shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, 0.0f)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (this.mQsExpansionAnimator != null) {
                    onQsExpansionStarted();
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    this.mQsTracking = true;
                    this.mIntercepting = false;
                    this.mNotificationStackScroller.removeLongPressCallback();
                    break;
                }
                break;
            case 1:
            case 3:
                trackMovement(event);
                if (this.mQsTracking) {
                    if (event.getActionMasked() != 3) {
                        z = false;
                    }
                    flingQsWithCurrentVelocity(y, z);
                    this.mQsTracking = false;
                }
                this.mIntercepting = false;
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                trackMovement(event);
                if (this.mQsTracking) {
                    setQsExpansion(this.mInitialHeightOnTouch + h);
                    trackMovement(event);
                    this.mIntercepting = false;
                    return true;
                } else if (Math.abs(h) > ((float) this.mTouchSlop) && Math.abs(h) > Math.abs(x - this.mInitialTouchX) && shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, h)) {
                    this.mQsTracking = true;
                    onQsExpansionStarted();
                    notifyExpandingFinished();
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mIntercepting = false;
                    this.mNotificationStackScroller.removeLongPressCallback();
                    return true;
                }
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialTouchX = event.getX(newIndex);
                    this.mInitialTouchY = event.getY(newIndex);
                    break;
                }
                break;
        }
        return false;
    }

    protected boolean isInContentBounds(float x, float y) {
        float stackScrollerX = this.mNotificationStackScroller.getX();
        if (this.mNotificationStackScroller.isBelowLastNotification(x - stackScrollerX, y) || stackScrollerX >= x || x >= ((float) this.mNotificationStackScroller.getWidth()) + stackScrollerX) {
            return false;
        }
        return true;
    }

    private void initDownStates(MotionEvent event) {
        boolean z = false;
        if (event.getActionMasked() == 0) {
            this.mOnlyAffordanceInThisMotion = false;
            this.mQsTouchAboveFalsingThreshold = this.mQsFullyExpanded;
            this.mDozingOnDown = isDozing();
            this.mCollapsedOnDown = isFullyCollapsed();
            if (this.mCollapsedOnDown) {
                z = this.mHeadsUpManager.hasPinnedHeadsUp();
            }
            this.mListenForHeadsUp = z;
        }
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (!this.mScrollView.isHandlingTouchEvent()) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private void flingQsWithCurrentVelocity(float y, boolean isCancelMotionEvent) {
        boolean z = false;
        float vel = getCurrentVelocity();
        boolean expandsQs = flingExpandsQs(vel);
        if (expandsQs) {
            logQsSwipeDown(y);
        }
        if (expandsQs && !isCancelMotionEvent) {
            z = true;
        }
        flingSettings(vel, z);
    }

    private void logQsSwipeDown(float y) {
        int gesture;
        float vel = getCurrentVelocity();
        if (this.mStatusBarState == 1) {
            gesture = 8;
        } else {
            gesture = 9;
        }
        EventLogTags.writeSysuiLockscreenGesture(gesture, (int) ((y - this.mInitialTouchY) / this.mStatusBar.getDisplayDensity()), (int) (vel / this.mStatusBar.getDisplayDensity()));
    }

    private boolean flingExpandsQs(float vel) {
        boolean z = true;
        if (isBelowFalsingThreshold()) {
            return false;
        }
        if (Math.abs(vel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            if (getQsExpansionFraction() <= 0.5f) {
                z = false;
            }
            return z;
        }
        if (vel <= 0.0f) {
            z = false;
        }
        return z;
    }

    private boolean isBelowFalsingThreshold() {
        return !this.mQsTouchAboveFalsingThreshold && this.mStatusBarState == 1;
    }

    private float getQsExpansionFraction() {
        return Math.min(1.0f, (this.mQsExpansionHeight - ((float) this.mQsMinExpansionHeight)) / ((float) (getTempQsMaxExpansion() - this.mQsMinExpansionHeight)));
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mBlockTouches) {
            return false;
        }
        initDownStates(event);
        if (this.mListenForHeadsUp && !this.mHeadsUpTouchHelper.isTrackingHeadsUp() && this.mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
        }
        if (!((this.mIsExpanding && !this.mHintAnimationRunning) || this.mQsExpanded || this.mStatusBar.getBarState() == 0)) {
            this.mAfforanceHelper.onTouchEvent(event);
        }
        if (this.mOnlyAffordanceInThisMotion) {
            return true;
        }
        this.mHeadsUpTouchHelper.onTouchEvent(event);
        if (!this.mHeadsUpTouchHelper.isTrackingHeadsUp() && handleQsTouch(event)) {
            return true;
        }
        if (event.getActionMasked() == 0 && isFullyCollapsed()) {
            MetricsLogger.count(this.mContext, "panel_open", 1);
            updateVerticalPanelPosition(event.getX());
        }
        super.onTouchEvent(event);
        return true;
    }

    private boolean handleQsTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0 && getExpandedFraction() == 1.0f && this.mStatusBar.getBarState() != 1 && !this.mQsExpanded && this.mQsExpansionEnabled) {
            this.mQsTracking = true;
            this.mConflictingQsExpansionGesture = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = event.getX();
            this.mInitialTouchX = event.getY();
        }
        if (!isFullyCollapsed()) {
            handleQsDown(event);
        }
        if (!this.mQsExpandImmediate && this.mQsTracking) {
            onQsTouch(event);
            if (!this.mConflictingQsExpansionGesture) {
                return true;
            }
        }
        if (action == 3 || action == 1) {
            this.mConflictingQsExpansionGesture = false;
        }
        if (action == 0 && isFullyCollapsed() && this.mQsExpansionEnabled) {
            this.mTwoFingerQsExpandPossible = true;
        }
        if (this.mTwoFingerQsExpandPossible && isOpenQsEvent(event) && event.getY(event.getActionIndex()) < ((float) this.mStatusBarMinHeight)) {
            MetricsLogger.count(this.mContext, "panel_open_qs", 1);
            this.mQsExpandImmediate = true;
            requestPanelHeightUpdate();
            setListening(true);
        }
        return false;
    }

    private boolean isInQsArea(float x, float y) {
        if (x < this.mScrollView.getX() || x > this.mScrollView.getX() + ((float) this.mScrollView.getWidth())) {
            return false;
        }
        return y <= this.mNotificationStackScroller.getBottomMostNotificationBottom() || y <= this.mQsContainer.getY() + ((float) this.mQsContainer.getHeight());
    }

    private boolean isOpenQsEvent(MotionEvent event) {
        boolean mouseButtonClickDrag;
        int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();
        boolean twoFingerDrag = action == 5 ? pointerCount == 2 : false;
        boolean stylusButtonClickDrag;
        if (action != 0) {
            stylusButtonClickDrag = false;
        } else if (event.isButtonPressed(32)) {
            stylusButtonClickDrag = true;
        } else {
            stylusButtonClickDrag = event.isButtonPressed(64);
        }
        if (action != 0) {
            mouseButtonClickDrag = false;
        } else if (event.isButtonPressed(2)) {
            mouseButtonClickDrag = true;
        } else {
            mouseButtonClickDrag = event.isButtonPressed(4);
        }
        if (twoFingerDrag || r3) {
            return true;
        }
        return mouseButtonClickDrag;
    }

    private void handleQsDown(MotionEvent event) {
        if (event.getActionMasked() == 0 && shouldQuickSettingsIntercept(event.getX(), event.getY(), -1.0f)) {
            this.mQsTracking = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = event.getX();
            this.mInitialTouchX = event.getY();
            notifyExpandingFinished();
        }
    }

    protected boolean flingExpands(float vel, float vectorVel, float x, float y) {
        boolean expands = super.flingExpands(vel, vectorVel, x, y);
        if (this.mQsExpansionAnimator != null) {
            return true;
        }
        return expands;
    }

    protected boolean hasConflictingGestures() {
        return this.mStatusBar.getBarState() != 0;
    }

    protected boolean shouldGestureIgnoreXTouchSlop(float x, float y) {
        return !this.mAfforanceHelper.isOnAffordanceIcon(x, y);
    }

    private void onQsTouch(MotionEvent event) {
        boolean z = true;
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float y = event.getY(pointerIndex);
        float x = event.getX(pointerIndex);
        float h = y - this.mInitialTouchY;
        switch (event.getActionMasked()) {
            case 0:
                this.mQsTracking = true;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                onQsExpansionStarted();
                this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                initVelocityTracker();
                trackMovement(event);
                return;
            case 1:
            case 3:
                this.mQsTracking = false;
                this.mTrackingPointer = -1;
                trackMovement(event);
                float fraction = getQsExpansionFraction();
                if ((fraction != 0.0f || y >= this.mInitialTouchY) && (fraction != 1.0f || y <= this.mInitialTouchY)) {
                    if (event.getActionMasked() != 3) {
                        z = false;
                    }
                    flingQsWithCurrentVelocity(y, z);
                } else {
                    logQsSwipeDown(y);
                    this.mScrollYOverride = -1;
                }
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                    return;
                }
                return;
            case 2:
                setQsExpansion(this.mInitialHeightOnTouch + h);
                if (h >= ((float) getFalsingThreshold())) {
                    this.mQsTouchAboveFalsingThreshold = true;
                }
                trackMovement(event);
                return;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    float newY = event.getY(newIndex);
                    float newX = event.getX(newIndex);
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    this.mInitialTouchY = newY;
                    this.mInitialTouchX = newX;
                    return;
                }
                return;
            default:
                return;
        }
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mQsFalsingThreshold) * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    public void onOverscrolled(float lastTouchX, float lastTouchY, int amount) {
        if (this.mIntercepting && shouldQuickSettingsIntercept(lastTouchX, lastTouchY, -1.0f)) {
            this.mQsTracking = true;
            onQsExpansionStarted(amount);
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = this.mLastTouchY;
            this.mInitialTouchX = this.mLastTouchX;
        }
    }

    public void onOverscrollTopChanged(float amount, boolean isRubberbanded) {
        boolean z = false;
        cancelQsAnimation();
        if (!this.mQsExpansionEnabled) {
            amount = 0.0f;
        }
        float rounded = amount >= 1.0f ? amount : 0.0f;
        if (rounded == 0.0f) {
            isRubberbanded = false;
        }
        this.mStackScrollerOverscrolling = isRubberbanded;
        if (rounded != 0.0f) {
            z = true;
        }
        this.mQsExpansionFromOverscroll = z;
        this.mLastOverscroll = rounded;
        updateQsState();
        setQsExpansion(((float) this.mQsMinExpansionHeight) + rounded);
    }

    public void flingTopOverscroll(float velocity, boolean open) {
        boolean z;
        this.mLastOverscroll = 0.0f;
        setQsExpansion(this.mQsExpansionHeight);
        if (!this.mQsExpansionEnabled && open) {
            velocity = 0.0f;
        }
        if (open) {
            z = this.mQsExpansionEnabled;
        } else {
            z = false;
        }
        flingSettings(velocity, z, new Runnable() {
            public void run() {
                NotificationPanelView.this.mStackScrollerOverscrolling = false;
                NotificationPanelView.this.mQsExpansionFromOverscroll = false;
                NotificationPanelView.this.updateQsState();
            }
        }, false);
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    private void onQsExpansionStarted(int overscrollAmount) {
        cancelQsAnimation();
        cancelHeightAnimator();
        float height = (this.mQsExpansionHeight - ((float) this.mScrollView.getScrollY())) - ((float) overscrollAmount);
        if (this.mScrollView.getScrollY() != 0) {
            this.mScrollYOverride = this.mScrollView.getScrollY();
        }
        this.mScrollView.scrollTo(0, 0);
        setQsExpansion(height);
        requestPanelHeightUpdate();
    }

    private void setQsExpanded(boolean expanded) {
        if (this.mQsExpanded != expanded) {
            this.mQsExpanded = expanded;
            updateQsState();
            requestPanelHeightUpdate();
            this.mNotificationStackScroller.setInterceptDelegateEnabled(expanded);
            this.mStatusBar.setQsExpanded(expanded);
            this.mQsPanel.setExpanded(expanded);
            this.mNotificationContainerParent.setQsExpanded(expanded);
        }
    }

    public void setBarState(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        int oldState = this.mStatusBarState;
        boolean keyguardShowing = statusBarState == 1;
        setKeyguardStatusViewVisibility(statusBarState, keyguardFadingAway, goingToFullShade);
        setKeyguardBottomAreaVisibility(statusBarState, goingToFullShade);
        this.mStatusBarState = statusBarState;
        this.mKeyguardShowing = keyguardShowing;
        if (goingToFullShade || (oldState == 1 && statusBarState == 2)) {
            animateKeyguardStatusBarOut();
            animateHeaderSlidingIn();
        } else if (oldState == 2 && statusBarState == 1) {
            animateKeyguardStatusBarIn(360);
            animateHeaderSlidingOut();
        } else {
            this.mKeyguardStatusBar.setAlpha(1.0f);
            this.mKeyguardStatusBar.setVisibility(keyguardShowing ? 0 : 4);
            if (keyguardShowing && oldState != this.mStatusBarState) {
                this.mKeyguardBottomArea.updateLeftAffordance();
                this.mAfforanceHelper.updatePreviews();
            }
        }
        if (keyguardShowing) {
            updateDozingVisibilities(false);
        }
        resetVerticalPanelPosition();
        updateQsState();
    }

    private void animateHeaderSlidingIn() {
        if (!this.mQsExpanded) {
            this.mHeaderAnimating = true;
            getViewTreeObserver().addOnPreDrawListener(this.mStartHeaderSlidingIn);
        }
    }

    private void animateHeaderSlidingOut() {
        this.mHeaderAnimating = true;
        this.mHeader.animate().y((float) (-this.mHeader.getHeight())).setStartDelay(0).setDuration(360).setInterpolator(this.mFastOutSlowInInterpolator).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mHeader.animate().setListener(null);
                NotificationPanelView.this.mHeaderAnimating = false;
                NotificationPanelView.this.updateQsState();
            }
        }).start();
        this.mQsContainer.animate().y((float) (-this.mQsContainer.getHeight())).setStartDelay(0).setDuration(360).setInterpolator(this.mFastOutSlowInInterpolator).start();
    }

    private void animateKeyguardStatusBarOut() {
        long keyguardFadingAwayDelay;
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{this.mKeyguardStatusBar.getAlpha(), 0.0f});
        anim.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            keyguardFadingAwayDelay = this.mStatusBar.getKeyguardFadingAwayDelay();
        } else {
            keyguardFadingAwayDelay = 0;
        }
        anim.setStartDelay(keyguardFadingAwayDelay);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            keyguardFadingAwayDelay = this.mStatusBar.getKeyguardFadingAwayDuration() / 2;
        } else {
            keyguardFadingAwayDelay = 360;
        }
        anim.setDuration(keyguardFadingAwayDelay);
        anim.setInterpolator(this.mDozeAnimationInterpolator);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
            }
        });
        anim.start();
    }

    private void animateKeyguardStatusBarIn(long duration) {
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardStatusBar.setAlpha(0.0f);
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        anim.setDuration(duration);
        anim.setInterpolator(this.mDozeAnimationInterpolator);
        anim.start();
    }

    private void setKeyguardBottomAreaVisibility(int statusBarState, boolean goingToFullShade) {
        if (goingToFullShade) {
            this.mKeyguardBottomArea.animate().cancel();
            this.mKeyguardBottomArea.animate().alpha(0.0f).setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(this.mAnimateKeyguardBottomAreaInvisibleEndRunnable).start();
        } else if (statusBarState == 1 || statusBarState == 2) {
            this.mKeyguardBottomArea.animate().cancel();
            if (!this.mDozing) {
                this.mKeyguardBottomArea.setVisibility(0);
            }
            this.mKeyguardBottomArea.setAlpha(1.0f);
        } else {
            this.mKeyguardBottomArea.animate().cancel();
            this.mKeyguardBottomArea.setVisibility(8);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        }
    }

    private void setKeyguardStatusViewVisibility(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        if ((!keyguardFadingAway && this.mStatusBarState == 1 && statusBarState != 1) || goingToFullShade) {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.animate().alpha(0.0f).setStartDelay(0).setDuration(160).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(this.mAnimateKeyguardStatusViewInvisibleEndRunnable);
            if (keyguardFadingAway) {
                this.mKeyguardStatusView.animate().setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).start();
            }
        } else if (this.mStatusBarState == 2 && statusBarState == 1) {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusView.setVisibility(0);
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.setAlpha(0.0f);
            this.mKeyguardStatusView.animate().alpha(1.0f).setStartDelay(0).setDuration(320).setInterpolator(PhoneStatusBar.ALPHA_IN).withEndAction(this.mAnimateKeyguardStatusViewVisibleEndRunnable);
        } else if (statusBarState == 1) {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusViewAnimating = false;
            this.mKeyguardStatusView.setVisibility(0);
            this.mKeyguardStatusView.setAlpha(1.0f);
        } else {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusViewAnimating = false;
            this.mKeyguardStatusView.setVisibility(8);
            this.mKeyguardStatusView.setAlpha(1.0f);
        }
    }

    private void updateQsState() {
        int i;
        boolean z;
        int i2 = 0;
        boolean z2 = (this.mQsExpanded || this.mStackScrollerOverscrolling) ? true : this.mHeaderAnimating;
        StatusBarHeaderView statusBarHeaderView = this.mHeader;
        if (this.mQsExpanded || !this.mKeyguardShowing || this.mHeaderAnimating) {
            i = 0;
        } else {
            i = 4;
        }
        statusBarHeaderView.setVisibility(i);
        statusBarHeaderView = this.mHeader;
        if (this.mKeyguardShowing && !this.mHeaderAnimating) {
            z = true;
        } else if (!this.mQsExpanded || this.mStackScrollerOverscrolling) {
            z = false;
        } else {
            z = true;
        }
        statusBarHeaderView.setExpanded(z);
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        if (this.mStatusBarState == 1) {
            z = false;
        } else if (this.mQsExpanded) {
            z = this.mQsExpansionFromOverscroll;
        } else {
            z = true;
        }
        notificationStackScrollLayout.setScrollingEnabled(z);
        QSPanel qSPanel = this.mQsPanel;
        if (z2) {
            i = 0;
        } else {
            i = 4;
        }
        qSPanel.setVisibility(i);
        QSContainer qSContainer = this.mQsContainer;
        if (!this.mKeyguardShowing || z2) {
            i = 0;
        } else {
            i = 4;
        }
        qSContainer.setVisibility(i);
        this.mScrollView.setTouchEnabled(this.mQsExpanded);
        updateEmptyShadeView();
        View view = this.mQsNavbarScrim;
        if (this.mStatusBarState != 0 || !this.mQsExpanded || this.mStackScrollerOverscrolling || !this.mQsScrimEnabled) {
            i2 = 4;
        }
        view.setVisibility(i2);
        if (this.mKeyguardUserSwitcher != null && this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            this.mKeyguardUserSwitcher.hideIfNotSimple(true);
        }
    }

    private void setQsExpansion(float height) {
        boolean z;
        height = Math.min(Math.max(height, (float) this.mQsMinExpansionHeight), (float) this.mQsMaxExpansionHeight);
        if (height == ((float) this.mQsMaxExpansionHeight)) {
            z = true;
        } else {
            z = false;
        }
        this.mQsFullyExpanded = z;
        if (height > ((float) this.mQsMinExpansionHeight) && !this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (height <= ((float) this.mQsMinExpansionHeight) && this.mQsExpanded) {
            setQsExpanded(false);
            if (!(!this.mLastAnnouncementWasQuickSettings || this.mTracking || isCollapsing())) {
                announceForAccessibility(getKeyguardOrLockScreenString());
                this.mLastAnnouncementWasQuickSettings = false;
            }
        }
        this.mQsExpansionHeight = height;
        this.mHeader.setExpansion(getHeaderExpansionFraction());
        setQsTranslation(height);
        requestScrollerTopPaddingUpdate(false);
        updateNotificationScrim(height);
        if (this.mKeyguardShowing) {
            updateHeaderKeyguard();
        }
        if (this.mStatusBarState == 2 || this.mStatusBarState == 1) {
            updateKeyguardBottomAreaAlpha();
        }
        if (this.mStatusBarState == 0 && this.mQsExpanded && !this.mStackScrollerOverscrolling && this.mQsScrimEnabled) {
            this.mQsNavbarScrim.setAlpha(getQsExpansionFraction());
        }
        if (height != 0.0f && this.mQsFullyExpanded && !this.mLastAnnouncementWasQuickSettings) {
            announceForAccessibility(getContext().getString(R.string.accessibility_desc_quick_settings));
            this.mLastAnnouncementWasQuickSettings = true;
        }
    }

    private String getKeyguardOrLockScreenString() {
        if (this.mStatusBarState == 1) {
            return getContext().getString(R.string.accessibility_desc_lock_screen);
        }
        return getContext().getString(R.string.accessibility_desc_notification_shade);
    }

    private void updateNotificationScrim(float height) {
        int startDistance = this.mQsMinExpansionHeight + this.mNotificationScrimWaitDistance;
        float progress = Math.max(0.0f, Math.min((height - ((float) startDistance)) / ((float) (this.mQsMaxExpansionHeight - startDistance)), 1.0f));
    }

    private float getHeaderExpansionFraction() {
        if (this.mKeyguardShowing) {
            return 1.0f;
        }
        return getQsExpansionFraction();
    }

    private void setQsTranslation(float height) {
        if (!this.mHeaderAnimating) {
            this.mQsContainer.setY((height - ((float) this.mQsContainer.getDesiredHeight())) + getHeaderTranslation());
        }
        if (this.mKeyguardShowing && !this.mHeaderAnimating) {
            this.mHeader.setY(interpolate(getQsExpansionFraction(), (float) (-this.mHeader.getHeight()), 0.0f));
        }
    }

    private float calculateQsTopPadding() {
        if (this.mKeyguardShowing && (this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted))) {
            int max;
            int maxNotifications = (this.mClockPositionResult.stackScrollerPadding - this.mClockPositionResult.stackScrollerPaddingAdjustment) - this.mNotificationTopPadding;
            int maxQs = getTempQsMaxExpansion();
            if (this.mStatusBarState == 1) {
                max = Math.max(maxNotifications, maxQs);
            } else {
                max = maxQs;
            }
            return (float) ((int) interpolate(getExpandedFraction(), (float) this.mQsMinExpansionHeight, (float) max));
        } else if (this.mQsSizeChangeAnimator != null) {
            return (float) ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        } else {
            if (this.mKeyguardShowing && this.mScrollYOverride == -1) {
                return interpolate(getQsExpansionFraction(), (float) (this.mNotificationStackScroller.getIntrinsicPadding() - this.mNotificationTopPadding), (float) this.mQsMaxExpansionHeight);
            }
            return this.mQsExpansionHeight;
        }
    }

    private void requestScrollerTopPaddingUpdate(boolean animate) {
        boolean z = true;
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        float calculateQsTopPadding = calculateQsTopPadding();
        int scrollY = this.mScrollView.getScrollY();
        if (this.mAnimateNextTopPaddingChange) {
            animate = true;
        }
        if (!this.mKeyguardShowing) {
            z = false;
        } else if (!this.mQsExpandImmediate) {
            z = this.mIsExpanding ? this.mQsExpandedWhenExpandingStarted : false;
        }
        notificationStackScrollLayout.updateTopPadding(calculateQsTopPadding, scrollY, animate, z);
        this.mAnimateNextTopPaddingChange = false;
    }

    private void trackMovement(MotionEvent event) {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
        this.mLastTouchX = event.getX();
        this.mLastTouchY = event.getY();
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(EVENT.DYNAMIC_PACK_EVENT_BASE);
        return this.mVelocityTracker.getYVelocity();
    }

    private void cancelQsAnimation() {
        if (this.mQsExpansionAnimator != null) {
            this.mQsExpansionAnimator.cancel();
        }
    }

    private void flingSettings(float vel, boolean expand) {
        flingSettings(vel, expand, null, false);
    }

    private void flingSettings(float vel, boolean expand, final Runnable onFinishRunnable, boolean isClick) {
        float target = (float) (expand ? this.mQsMaxExpansionHeight : this.mQsMinExpansionHeight);
        if (target == this.mQsExpansionHeight) {
            this.mScrollYOverride = -1;
            if (onFinishRunnable != null) {
                onFinishRunnable.run();
            }
            return;
        }
        boolean belowFalsingThreshold = isBelowFalsingThreshold();
        if (belowFalsingThreshold) {
            vel = 0.0f;
        }
        this.mScrollView.setBlockFlinging(true);
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mQsExpansionHeight, target});
        if (isClick) {
            animator.setInterpolator(this.mTouchResponseInterpolator);
            animator.setDuration(368);
        } else {
            this.mFlingAnimationUtils.apply(animator, this.mQsExpansionHeight, target, vel);
        }
        if (belowFalsingThreshold) {
            animator.setDuration(350);
        }
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.setQsExpansion(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mScrollView.setBlockFlinging(false);
                NotificationPanelView.this.mScrollYOverride = -1;
                NotificationPanelView.this.mQsExpansionAnimator = null;
                if (onFinishRunnable != null) {
                    onFinishRunnable.run();
                }
            }
        });
        animator.start();
        this.mQsExpansionAnimator = animator;
        this.mQsAnimatorExpand = expand;
    }

    private boolean shouldQuickSettingsIntercept(float x, float y, float yDiff) {
        boolean z = false;
        if (!this.mQsExpansionEnabled || this.mCollapsedOnDown) {
            return false;
        }
        View header = this.mKeyguardShowing ? this.mKeyguardStatusBar : this.mHeader;
        boolean onHeader = (x < header.getX() || x > header.getX() + ((float) header.getWidth()) || y < ((float) header.getTop())) ? false : y <= ((float) header.getBottom());
        if (!this.mQsExpanded) {
            return onHeader;
        }
        if (onHeader) {
            z = true;
        } else if (this.mScrollView.isScrolledToBottom() && yDiff < 0.0f) {
            z = isInQsArea(x, y);
        }
        return z;
    }

    protected boolean isScrolledToBottom() {
        boolean z = true;
        if (isInSettings()) {
            return this.mScrollView.isScrolledToBottom();
        }
        if (this.mStatusBar.getBarState() != 1) {
            z = this.mNotificationStackScroller.isScrolledToBottom();
        }
        return z;
    }

    protected int getMaxPanelHeight() {
        int maxHeight;
        int min = this.mStatusBarMinHeight;
        if (this.mStatusBar.getBarState() != 1 && this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            min = Math.max(min, (int) ((((float) this.mQsMinExpansionHeight) + getOverExpansionAmount()) * 2.05f));
        }
        if (this.mQsExpandImmediate || this.mQsExpanded || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) {
            maxHeight = calculatePanelHeightQsExpanded();
        } else {
            maxHeight = calculatePanelHeightShade();
        }
        return Math.max(maxHeight, min);
    }

    private boolean isInSettings() {
        return this.mQsExpanded;
    }

    protected void onHeightUpdated(float expandedHeight) {
        boolean z;
        if (!this.mQsExpanded || this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) {
            positionClockAndNotifications();
        }
        if (this.mQsExpandImmediate || (this.mQsExpanded && !this.mQsTracking && this.mQsExpansionAnimator == null && !this.mQsExpansionFromOverscroll)) {
            float t;
            if (this.mKeyguardShowing) {
                t = expandedHeight / ((float) getMaxPanelHeight());
            } else {
                float panelHeightQsCollapsed = (float) (this.mNotificationStackScroller.getIntrinsicPadding() + this.mNotificationStackScroller.getMinStackHeight());
                t = (expandedHeight - panelHeightQsCollapsed) / (((float) calculatePanelHeightQsExpanded()) - panelHeightQsCollapsed);
            }
            setQsExpansion(((float) this.mQsMinExpansionHeight) + (((float) (getTempQsMaxExpansion() - this.mQsMinExpansionHeight)) * t));
        }
        updateStackHeight(expandedHeight);
        updateHeader();
        updateUnlockIcon();
        updateNotificationTranslucency();
        updatePanelExpanded();
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        if (isFullyCollapsed()) {
            z = false;
        } else {
            z = true;
        }
        notificationStackScrollLayout.setShadeExpanded(z);
    }

    private void updatePanelExpanded() {
        boolean isExpanded = !isFullyCollapsed();
        if (this.mPanelExpanded != isExpanded) {
            this.mHeadsUpManager.setIsExpanded(isExpanded);
            this.mStatusBar.setPanelExpanded(isExpanded);
            this.mPanelExpanded = isExpanded;
        }
    }

    private int getTempQsMaxExpansion() {
        int qsTempMaxExpansion = this.mQsMaxExpansionHeight;
        if (this.mScrollYOverride != -1) {
            return qsTempMaxExpansion - this.mScrollYOverride;
        }
        return qsTempMaxExpansion;
    }

    private int calculatePanelHeightShade() {
        return (int) (((float) ((this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) - this.mTopPaddingAdjustment)) + this.mNotificationStackScroller.getTopPaddingOverflow());
    }

    private int calculatePanelHeightQsExpanded() {
        int i;
        float notificationHeight = (float) ((this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) - this.mNotificationStackScroller.getTopPadding());
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0 && this.mShadeEmpty) {
            notificationHeight = (float) ((this.mNotificationStackScroller.getEmptyShadeViewHeight() + this.mNotificationStackScroller.getBottomStackPeekSize()) + this.mNotificationStackScroller.getCollapseSecondCardPadding());
        }
        int maxQsHeight = this.mQsMaxExpansionHeight;
        if (this.mQsSizeChangeAnimator != null) {
            maxQsHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        int notificationTopPadding = maxQsHeight + this.mNotificationStackScroller.getNotificationTopPadding();
        if (this.mStatusBarState == 1) {
            i = this.mClockPositionResult.stackScrollerPadding - this.mTopPaddingAdjustment;
        } else {
            i = 0;
        }
        float totalHeight = ((float) Math.max(notificationTopPadding, i)) + notificationHeight;
        if (totalHeight > ((float) this.mNotificationStackScroller.getHeight())) {
            totalHeight = Math.max((float) (((this.mNotificationStackScroller.getMinStackHeight() + maxQsHeight) + this.mNotificationStackScroller.getNotificationTopPadding()) - getScrollViewScrollY()), (float) this.mNotificationStackScroller.getHeight());
        }
        return (int) totalHeight;
    }

    private int getScrollViewScrollY() {
        if (this.mScrollYOverride == -1 || this.mQsTracking) {
            return this.mScrollView.getScrollY();
        }
        return this.mScrollYOverride;
    }

    private void updateNotificationTranslucency() {
        float alpha = 1.0f;
        if (!(!this.mClosingWithAlphaFadeOut || this.mExpandingFromHeadsUp || this.mHeadsUpManager.hasPinnedHeadsUp())) {
            alpha = getFadeoutAlpha();
        }
        this.mNotificationStackScroller.setAlpha(alpha);
    }

    private float getFadeoutAlpha() {
        return (float) Math.pow((double) Math.max(0.0f, Math.min((getNotificationsTopY() + ((float) this.mNotificationStackScroller.getItemHeight())) / ((float) ((this.mQsMinExpansionHeight + this.mNotificationStackScroller.getBottomStackPeekSize()) - this.mNotificationStackScroller.getCollapseSecondCardPadding())), 1.0f)), 0.75d);
    }

    protected float getOverExpansionAmount() {
        return this.mNotificationStackScroller.getCurrentOverScrollAmount(true);
    }

    protected float getOverExpansionPixels() {
        return this.mNotificationStackScroller.getCurrentOverScrolledPixels(true);
    }

    private void updateUnlockIcon() {
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            boolean active = ((float) getMaxPanelHeight()) - getExpandedHeight() > ((float) this.mUnlockMoveDistance);
            KeyguardAffordanceView lockIcon = this.mKeyguardBottomArea.getLockIcon();
            if (active && !this.mUnlockIconActive && this.mTracking) {
                lockIcon.setImageAlpha(1.0f, true, 150, this.mFastOutLinearInterpolator, null);
                lockIcon.setImageScale(1.2f, true, 150, this.mFastOutLinearInterpolator);
            } else if (!active && this.mUnlockIconActive && this.mTracking) {
                lockIcon.setImageAlpha(lockIcon.getRestingAlpha(), true, 150, this.mFastOutLinearInterpolator, null);
                lockIcon.setImageScale(1.0f, true, 150, this.mFastOutLinearInterpolator);
            }
            this.mUnlockIconActive = active;
        }
    }

    private void updateHeader() {
        if (this.mStatusBar.getBarState() == 1) {
            updateHeaderKeyguard();
        } else {
            updateHeaderShade();
        }
    }

    private void updateHeaderShade() {
        if (!this.mHeaderAnimating) {
            this.mHeader.setTranslationY(getHeaderTranslation());
        }
        setQsTranslation(this.mQsExpansionHeight);
    }

    private float getHeaderTranslation() {
        if (this.mStatusBar.getBarState() == 1) {
            return 0.0f;
        }
        if (this.mNotificationStackScroller.getNotGoneChildCount() != 0) {
            float stackTranslation = this.mNotificationStackScroller.getStackTranslation();
            float translation = stackTranslation / 2.05f;
            if (this.mHeadsUpManager.hasPinnedHeadsUp() || this.mIsExpansionFromHeadsUp) {
                translation = ((((float) this.mNotificationStackScroller.getTopPadding()) + stackTranslation) - ((float) this.mNotificationTopPadding)) - ((float) this.mQsMinExpansionHeight);
            }
            return Math.min(0.0f, translation);
        } else if (this.mExpandedHeight / 2.05f >= ((float) this.mQsMinExpansionHeight)) {
            return 0.0f;
        } else {
            return (this.mExpandedHeight / 2.05f) - ((float) this.mQsMinExpansionHeight);
        }
    }

    private float getKeyguardContentsAlpha() {
        float alpha;
        if (this.mStatusBar.getBarState() == 1) {
            alpha = getNotificationsTopY() / ((float) (this.mKeyguardStatusBar.getHeight() + this.mNotificationsHeaderCollideDistance));
        } else {
            alpha = getNotificationsTopY() / ((float) this.mKeyguardStatusBar.getHeight());
        }
        return (float) Math.pow((double) MathUtils.constrain(alpha, 0.0f, 1.0f), 0.75d);
    }

    private void updateHeaderKeyguardAlpha() {
        this.mKeyguardStatusBar.setAlpha(Math.min(getKeyguardContentsAlpha(), 1.0f - Math.min(1.0f, getQsExpansionFraction() * 2.0f)) * this.mKeyguardStatusBarAnimateAlpha);
        KeyguardStatusBarView keyguardStatusBarView = this.mKeyguardStatusBar;
        int i = (this.mKeyguardStatusBar.getAlpha() == 0.0f || this.mDozing) ? 4 : 0;
        keyguardStatusBarView.setVisibility(i);
    }

    private void updateHeaderKeyguard() {
        updateHeaderKeyguardAlpha();
        setQsTranslation(this.mQsExpansionHeight);
    }

    private void updateKeyguardBottomAreaAlpha() {
        int i;
        float alpha = Math.min(getKeyguardContentsAlpha(), 1.0f - getQsExpansionFraction());
        this.mKeyguardBottomArea.setAlpha(alpha);
        KeyguardBottomAreaView keyguardBottomAreaView = this.mKeyguardBottomArea;
        if (alpha == 0.0f) {
            i = 4;
        } else {
            i = 0;
        }
        keyguardBottomAreaView.setImportantForAccessibility(i);
    }

    private float getNotificationsTopY() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return getExpandedHeight();
        }
        return this.mNotificationStackScroller.getNotificationsTopY();
    }

    protected void onExpandingStarted() {
        super.onExpandingStarted();
        this.mNotificationStackScroller.onExpansionStarted();
        this.mIsExpanding = true;
        this.mQsExpandedWhenExpandingStarted = this.mQsFullyExpanded;
        if (this.mQsExpanded) {
            onQsExpansionStarted();
        }
    }

    protected void onExpandingFinished() {
        super.onExpandingFinished();
        this.mNotificationStackScroller.onExpansionStopped();
        this.mHeadsUpManager.onExpandingFinished();
        this.mIsExpanding = false;
        this.mScrollYOverride = -1;
        if (isFullyCollapsed()) {
            DejankUtils.postAfterTraversal(new Runnable() {
                public void run() {
                    NotificationPanelView.this.setListening(false);
                }
            });
            postOnAnimation(new Runnable() {
                public void run() {
                    NotificationPanelView.this.getParent().invalidateChild(NotificationPanelView.this, NotificationPanelView.mDummyDirtyRect);
                }
            });
        } else {
            setListening(true);
        }
        this.mQsExpandImmediate = false;
        this.mTwoFingerQsExpandPossible = false;
        this.mIsExpansionFromHeadsUp = false;
        this.mNotificationStackScroller.setTrackingHeadsUp(false);
        this.mExpandingFromHeadsUp = false;
        setPanelScrimMinFraction(0.0f);
    }

    private void setListening(boolean listening) {
        this.mHeader.setListening(listening);
        this.mKeyguardStatusBar.setListening(listening);
        this.mQsPanel.setListening(listening);
    }

    public void instantExpand() {
        super.instantExpand();
        setListening(true);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void setOverExpansion(float overExpansion, boolean isPixels) {
        if (!(this.mConflictingQsExpansionGesture || this.mQsExpandImmediate || this.mStatusBar.getBarState() == 1)) {
            this.mNotificationStackScroller.setOnHeightChangedListener(null);
            if (isPixels) {
                this.mNotificationStackScroller.setOverScrolledPixels(overExpansion, true, false);
            } else {
                this.mNotificationStackScroller.setOverScrollAmount(overExpansion, true, false);
            }
            this.mNotificationStackScroller.setOnHeightChangedListener(this);
        }
    }

    protected void onTrackingStarted() {
        super.onTrackingStarted();
        if (this.mQsFullyExpanded) {
            this.mQsExpandImmediate = true;
        }
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            this.mAfforanceHelper.animateHideLeftRightIcon();
        }
        this.mNotificationStackScroller.onPanelTrackingStarted();
    }

    protected void onTrackingStopped(boolean expand) {
        super.onTrackingStopped(expand);
        if (expand) {
            this.mNotificationStackScroller.setOverScrolledPixels(0.0f, true, true);
        }
        this.mNotificationStackScroller.onPanelTrackingStopped();
        if (expand && ((this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) && !this.mHintAnimationRunning)) {
            this.mAfforanceHelper.reset(true);
        }
        if (!expand) {
            if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
                KeyguardAffordanceView lockIcon = this.mKeyguardBottomArea.getLockIcon();
                lockIcon.setImageAlpha(0.0f, true, 100, this.mFastOutLinearInterpolator, null);
                lockIcon.setImageScale(2.0f, true, 100, this.mFastOutLinearInterpolator);
            }
        }
    }

    public void onHeightChanged(ExpandableView view, boolean needsAnimation) {
        if (view != null || !this.mQsExpanded) {
            requestPanelHeightUpdate();
        }
    }

    public void onReset(ExpandableView view) {
    }

    public void onScrollChanged() {
        if (this.mQsExpanded) {
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mAfforanceHelper.onConfigurationChanged();
        if (newConfig.orientation != this.mLastOrientation) {
            resetVerticalPanelPosition();
        }
        this.mLastOrientation = newConfig.orientation;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mNavigationBarBottomHeight = insets.getSystemWindowInsetBottom();
        updateMaxHeadsUpTranslation();
        return insets;
    }

    private void updateMaxHeadsUpTranslation() {
        this.mNotificationStackScroller.setHeadsUpBoundaries(getHeight(), this.mNavigationBarBottomHeight);
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        if (layoutDirection != this.mOldLayoutDirection) {
            this.mAfforanceHelper.onRtlPropertiesChanged();
            this.mOldLayoutDirection = layoutDirection;
        }
    }

    public void onClick(View v) {
        if (v == this.mHeader) {
            onQsExpansionStarted();
            if (this.mQsExpanded) {
                flingSettings(0.0f, false, null, true);
            } else if (this.mQsExpansionEnabled) {
                EventLogTags.writeSysuiLockscreenGesture(10, 0, 0);
                flingSettings(0.0f, true, null, true);
            }
        }
    }

    public void onAnimationToSideStarted(boolean rightPage, float translation, float vel) {
        boolean z = getLayoutDirection() == 1 ? rightPage : !rightPage;
        this.mIsLaunchTransitionRunning = true;
        this.mLaunchAnimationEndRunnable = null;
        float displayDensity = this.mStatusBar.getDisplayDensity();
        int lengthDp = Math.abs((int) (translation / displayDensity));
        int velocityDp = Math.abs((int) (vel / displayDensity));
        if (z) {
            EventLogTags.writeSysuiLockscreenGesture(5, lengthDp, velocityDp);
            this.mKeyguardBottomArea.launchLeftAffordance();
        } else {
            EventLogTags.writeSysuiLockscreenGesture(4, lengthDp, velocityDp);
            this.mSecureCameraLaunchManager.startSecureCameraLaunch();
        }
        this.mStatusBar.startLaunchTransitionTimeout();
        this.mBlockTouches = true;
    }

    public void onAnimationToSideEnded() {
        this.mIsLaunchTransitionRunning = false;
        this.mIsLaunchTransitionFinished = true;
        if (this.mLaunchAnimationEndRunnable != null) {
            this.mLaunchAnimationEndRunnable.run();
            this.mLaunchAnimationEndRunnable = null;
        }
    }

    protected void startUnlockHintAnimation() {
        super.startUnlockHintAnimation();
        startHighlightIconAnimation(getCenterIcon());
    }

    private void startHighlightIconAnimation(final KeyguardAffordanceView icon) {
        icon.setImageAlpha(1.0f, true, 200, this.mFastOutSlowInInterpolator, new Runnable() {
            public void run() {
                icon.setImageAlpha(icon.getRestingAlpha(), true, 200, NotificationPanelView.this.mFastOutSlowInInterpolator, null);
            }
        });
    }

    public float getMaxTranslationDistance() {
        return (float) Math.hypot((double) getWidth(), (double) getHeight());
    }

    public void onSwipingStarted(boolean rightIcon) {
        boolean z = getLayoutDirection() == 1 ? !rightIcon : rightIcon;
        if (z) {
            this.mSecureCameraLaunchManager.onSwipingStarted();
            this.mKeyguardBottomArea.bindCameraPrewarmService();
        }
        requestDisallowInterceptTouchEvent(true);
        this.mOnlyAffordanceInThisMotion = true;
        this.mQsTracking = false;
    }

    public void onSwipingAborted() {
        this.mKeyguardBottomArea.unbindCameraPrewarmService(false);
    }

    public void onIconClicked(boolean rightIcon) {
        if (!this.mHintAnimationRunning) {
            this.mHintAnimationRunning = true;
            this.mAfforanceHelper.startHintAnimation(rightIcon, new Runnable() {
                public void run() {
                    NotificationPanelView.this.mHintAnimationRunning = false;
                    NotificationPanelView.this.mStatusBar.onHintFinished();
                }
            });
            if (getLayoutDirection() == 1) {
                rightIcon = !rightIcon;
            }
            if (rightIcon) {
                this.mStatusBar.onCameraHintStarted();
            } else if (this.mKeyguardBottomArea.isLeftVoiceAssist()) {
                this.mStatusBar.onVoiceAssistHintStarted();
            } else {
                this.mStatusBar.onPhoneHintStarted();
            }
        }
    }

    public KeyguardAffordanceView getLeftIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightView();
        }
        return this.mKeyguardBottomArea.getLeftView();
    }

    public KeyguardAffordanceView getCenterIcon() {
        return this.mKeyguardBottomArea.getLockIcon();
    }

    public KeyguardAffordanceView getRightIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftView();
        }
        return this.mKeyguardBottomArea.getRightView();
    }

    public View getLeftPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightPreview();
        }
        return this.mKeyguardBottomArea.getLeftPreview();
    }

    public View getRightPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftPreview();
        }
        return this.mKeyguardBottomArea.getRightPreview();
    }

    public float getAffordanceFalsingFactor() {
        return this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    protected float getPeekHeight() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() > 0) {
            return (float) this.mNotificationStackScroller.getPeekHeight();
        }
        return ((float) this.mQsMinExpansionHeight) * 2.05f;
    }

    protected float getCannedFlingDurationFactor() {
        if (this.mQsExpanded) {
            return 0.7f;
        }
        return 0.6f;
    }

    protected boolean fullyExpandedClearAllVisible() {
        if (this.mNotificationStackScroller.isDismissViewNotGone() && this.mNotificationStackScroller.isScrolledToBottom() && !this.mQsExpandImmediate) {
            return true;
        }
        return false;
    }

    protected boolean isClearAllVisible() {
        return this.mNotificationStackScroller.isDismissViewVisible();
    }

    protected int getClearAllHeight() {
        return this.mNotificationStackScroller.getDismissViewHeight();
    }

    protected boolean isTrackingBlocked() {
        return this.mConflictingQsExpansionGesture ? this.mQsExpanded : false;
    }

    public void notifyVisibleChildrenChanged() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() != 0) {
            this.mReserveNotificationSpace.setVisibility(0);
        } else {
            this.mReserveNotificationSpace.setVisibility(8);
        }
    }

    public boolean isQsExpanded() {
        return this.mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return this.mQsPanel.isShowingDetail();
    }

    public void closeQsDetail() {
        this.mQsPanel.closeDetail();
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public boolean isLaunchTransitionFinished() {
        return this.mIsLaunchTransitionFinished;
    }

    public boolean isLaunchTransitionRunning() {
        return this.mIsLaunchTransitionRunning;
    }

    public void setLaunchTransitionEndRunnable(Runnable r) {
        this.mLaunchAnimationEndRunnable = r;
    }

    public void setEmptyDragAmount(float amount) {
        float factor = 0.8f;
        if (this.mNotificationStackScroller.getNotGoneChildCount() > 0) {
            factor = 0.4f;
        } else if (!this.mStatusBar.hasActiveNotifications()) {
            factor = 0.4f;
        }
        this.mEmptyDragAmount = amount * factor;
        positionClockAndNotifications();
    }

    private static float interpolate(float t, float start, float end) {
        return ((1.0f - t) * start) + (t * end);
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (dozing != this.mDozing) {
            this.mDozing = dozing;
            if (this.mStatusBarState == 1) {
                updateDozingVisibilities(animate);
            }
        }
    }

    private void updateDozingVisibilities(boolean animate) {
        if (this.mDozing) {
            this.mKeyguardStatusBar.setVisibility(4);
            this.mKeyguardBottomArea.setVisibility(4);
            return;
        }
        this.mKeyguardBottomArea.setVisibility(0);
        this.mKeyguardStatusBar.setVisibility(0);
        if (animate) {
            animateKeyguardStatusBarIn(700);
            this.mKeyguardBottomArea.startFinishDozeAnimation();
        }
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public void setShadeEmpty(boolean shadeEmpty) {
        this.mShadeEmpty = shadeEmpty;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        boolean z = false;
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        if (this.mShadeEmpty && !this.mQsExpanded) {
            z = true;
        }
        notificationStackScrollLayout.updateEmptyShadeView(z);
    }

    public void setQsScrimEnabled(boolean qsScrimEnabled) {
        boolean changed = this.mQsScrimEnabled != qsScrimEnabled;
        this.mQsScrimEnabled = qsScrimEnabled;
        if (changed) {
            updateQsState();
        }
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void onScreenTurningOn() {
        this.mKeyguardStatusView.refreshTime();
    }

    public void onEmptySpaceClicked(float x, float y) {
        onEmptySpaceClick(x);
    }

    protected boolean onMiddleClicked() {
        switch (this.mStatusBar.getBarState()) {
            case 0:
                post(this.mPostCollapseRunnable);
                return false;
            case 1:
                if (!this.mDozingOnDown) {
                    EventLogTags.writeSysuiLockscreenGesture(3, 0, 0);
                    startUnlockHintAnimation();
                }
                return true;
            case 2:
                if (!this.mQsExpanded) {
                    this.mStatusBar.goToKeyguard();
                }
                return true;
            default:
                return true;
        }
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
        if (inPinnedMode) {
            this.mHeadsUpExistenceChangedRunnable.run();
            updateNotificationTranslucency();
            return;
        }
        this.mHeadsUpAnimatingAway = true;
        this.mNotificationStackScroller.runAfterAnimationFinished(this.mHeadsUpExistenceChangedRunnable);
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(headsUp, true);
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    public void onHeadsUpStateChanged(Entry entry, boolean isHeadsUp) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(entry.row, isHeadsUp);
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        super.setHeadsUpManager(headsUpManager);
        this.mHeadsUpTouchHelper = new HeadsUpTouchHelper(headsUpManager, this.mNotificationStackScroller, this);
    }

    public void setTrackingHeadsUp(boolean tracking) {
        if (tracking) {
            this.mNotificationStackScroller.setTrackingHeadsUp(true);
            this.mExpandingFromHeadsUp = true;
        }
    }

    protected void onClosingFinished() {
        super.onClosingFinished();
        resetVerticalPanelPosition();
        setClosingWithAlphaFadeout(false);
    }

    private void setClosingWithAlphaFadeout(boolean closing) {
        this.mClosingWithAlphaFadeOut = closing;
        this.mNotificationStackScroller.forceNoOverlappingRendering(closing);
    }

    private void updateVerticalPanelPosition(float x) {
        if (this.mNotificationStackScroller.getWidth() <= 0 || ((float) this.mNotificationStackScroller.getWidth()) * 1.75f > ((float) getWidth())) {
            resetVerticalPanelPosition();
            return;
        }
        float leftMost = (float) (this.mPositionMinSideMargin + (this.mNotificationStackScroller.getWidth() / 2));
        float rightMost = (float) ((getWidth() - this.mPositionMinSideMargin) - (this.mNotificationStackScroller.getWidth() / 2));
        if (Math.abs(x - ((float) (getWidth() / 2))) < ((float) (this.mNotificationStackScroller.getWidth() / 4))) {
            x = (float) (getWidth() / 2);
        }
        setVerticalPanelTranslation(Math.min(rightMost, Math.max(leftMost, x)) - ((float) (this.mNotificationStackScroller.getLeft() + (this.mNotificationStackScroller.getWidth() / 2))));
    }

    private void resetVerticalPanelPosition() {
        setVerticalPanelTranslation(0.0f);
    }

    private void setVerticalPanelTranslation(float translation) {
        this.mNotificationStackScroller.setTranslationX(translation);
        this.mScrollView.setTranslationX(translation);
        this.mHeader.setTranslationX(translation);
    }

    private void updateStackHeight(float stackHeight) {
        this.mNotificationStackScroller.setStackHeight(stackHeight);
        updateKeyguardBottomAreaAlpha();
    }

    public void setPanelScrimMinFraction(float minFraction) {
        this.mBar.panelScrimMinFractionChanged(minFraction);
    }

    public void clearNotificattonEffects() {
        this.mStatusBar.clearNotificationEffects();
    }

    protected boolean isPanelVisibleBecauseOfHeadsUp() {
        return !this.mHeadsUpManager.hasPinnedHeadsUp() ? this.mHeadsUpAnimatingAway : true;
    }
}
