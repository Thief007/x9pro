package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.stack.NotificationChildrenContainer;
import com.android.systemui.statusbar.stack.StackScrollState;
import com.android.systemui.statusbar.stack.StackStateAnimator;
import java.util.List;

public class ExpandableNotificationRow extends ActivatableNotificationView {
    private ValueAnimator mChildExpandAnimator;
    private NotificationChildrenContainer mChildrenContainer;
    private ViewStub mChildrenContainerStub;
    private float mChildrenExpandProgress;
    private boolean mChildrenExpanded;
    private View mExpandButton;
    private View mExpandButtonContainer;
    private View mExpandButtonDivider;
    private float mExpandButtonStart;
    private ViewStub mExpandButtonStub;
    private OnClickListener mExpandClickListener = new OnClickListener() {
        public void onClick(View v) {
            ExpandableNotificationRow.this.mGroupManager.setGroupExpanded(ExpandableNotificationRow.this.mStatusBarNotification, !ExpandableNotificationRow.this.mChildrenExpanded);
        }
    };
    private boolean mExpandable;
    private boolean mExpansionDisabled;
    private NotificationGroupManager mGroupManager;
    private NotificationGuts mGuts;
    private ViewStub mGutsStub;
    private boolean mHasExpandAction;
    private boolean mHasUserChangedExpansion;
    private int mHeadsUpHeight;
    private boolean mHideSensitiveForIntrinsicHeight;
    private boolean mIsHeadsUp;
    private boolean mIsPinned;
    private boolean mIsSystemChildExpanded;
    private boolean mIsSystemExpanded;
    private final LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private ExpansionLogger mLogger;
    private String mLoggingKey;
    private int mMaxExpandHeight;
    private NotificationContentView mPrivateLayout;
    private NotificationContentView mPublicLayout;
    private int mRowMinHeight;
    private boolean mSensitive;
    private boolean mShowingPublic;
    private boolean mShowingPublicInitialized;
    private StatusBarNotification mStatusBarNotification;
    private boolean mUserExpanded;
    private boolean mUserLocked;
    private View mVetoButton;
    private boolean mWasReset;

    public interface ExpansionLogger {
        void logNotificationExpansion(String str, boolean z, boolean z2);
    }

    public NotificationContentView getPrivateLayout() {
        return this.mPrivateLayout;
    }

    public NotificationContentView getPublicLayout() {
        return this.mPublicLayout;
    }

    public void setIconAnimationRunning(boolean running) {
        setIconAnimationRunning(running, this.mPublicLayout);
        setIconAnimationRunning(running, this.mPrivateLayout);
    }

    private void setIconAnimationRunning(boolean running, NotificationContentView layout) {
        if (layout != null) {
            View contractedChild = layout.getContractedChild();
            View expandedChild = layout.getExpandedChild();
            View headsUpChild = layout.getHeadsUpChild();
            setIconAnimationRunningForChild(running, contractedChild);
            setIconAnimationRunningForChild(running, expandedChild);
            setIconAnimationRunningForChild(running, headsUpChild);
        }
    }

    private void setIconAnimationRunningForChild(boolean running, View child) {
        if (child != null) {
            setIconRunning((ImageView) child.findViewById(16908294), running);
            setIconRunning((ImageView) child.findViewById(16908352), running);
        }
    }

    private void setIconRunning(ImageView imageView, boolean running) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                if (running) {
                    animationDrawable.start();
                } else {
                    animationDrawable.stop();
                }
            } else if (drawable instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable animationDrawable2 = (AnimatedVectorDrawable) drawable;
                if (running) {
                    animationDrawable2.start();
                } else {
                    animationDrawable2.stop();
                }
            }
        }
    }

    public void setStatusBarNotification(StatusBarNotification statusBarNotification) {
        this.mStatusBarNotification = statusBarNotification;
        updateVetoButton();
        updateExpandButton();
    }

    public StatusBarNotification getStatusBarNotification() {
        return this.mStatusBarNotification;
    }

    public boolean isHeadsUp() {
        return this.mIsHeadsUp;
    }

    public void setHeadsUp(boolean isHeadsUp) {
        int intrinsicBefore = getIntrinsicHeight();
        this.mIsHeadsUp = isHeadsUp;
        this.mPrivateLayout.setHeadsUp(isHeadsUp);
        if (intrinsicBefore != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }

    public void addChildNotification(ExpandableNotificationRow row) {
        addChildNotification(row, -1);
    }

    public void addChildNotification(ExpandableNotificationRow row, int childIndex) {
        if (this.mChildrenContainer == null) {
            this.mChildrenContainerStub.inflate();
        }
        this.mChildrenContainer.addNotification(row, childIndex);
    }

    public void removeChildNotification(ExpandableNotificationRow row) {
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.removeNotification(row);
        }
    }

    public boolean areChildrenExpanded() {
        return this.mChildrenExpanded;
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        return this.mChildrenContainer == null ? null : this.mChildrenContainer.getNotificationChildren();
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> childOrder) {
        return this.mChildrenContainer != null ? this.mChildrenContainer.applyChildOrder(childOrder) : false;
    }

    public void getChildrenStates(StackScrollState resultState) {
        if (this.mChildrenExpanded) {
            this.mChildrenContainer.getState(resultState, resultState.getViewStateForView(this));
        }
    }

    public void applyChildrenState(StackScrollState state) {
        if (this.mChildrenExpanded) {
            this.mChildrenContainer.applyState(state);
        }
    }

    public void prepareExpansionChanged(StackScrollState state) {
        if (this.mChildrenExpanded) {
            this.mChildrenContainer.prepareExpansionChanged(state);
        }
    }

    public void startChildAnimation(StackScrollState finalState, StackStateAnimator stateAnimator, boolean withDelays, long delay, long duration) {
        if (this.mChildrenExpanded) {
            this.mChildrenContainer.startAnimationToState(finalState, stateAnimator, withDelays, delay, duration);
        }
    }

    public ExpandableNotificationRow getViewAtPosition(float y) {
        if (!this.mChildrenExpanded) {
            return this;
        }
        ExpandableNotificationRow view = this.mChildrenContainer.getViewAtPosition(y);
        if (view != null) {
            this = view;
        }
        return this;
    }

    public NotificationGuts getGuts() {
        return this.mGuts;
    }

    protected int calculateContentHeightFromActualHeight(int actualHeight) {
        int realActualHeight = actualHeight;
        if (hasBottomDecor()) {
            realActualHeight -= getBottomDecorHeight();
        }
        return Math.max(getMinHeight(), realActualHeight);
    }

    public void setPinned(boolean pinned) {
        this.mIsPinned = pinned;
    }

    public boolean isPinned() {
        return this.mIsPinned;
    }

    public int getHeadsUpHeight() {
        return this.mHeadsUpHeight;
    }

    public ExpandableNotificationRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void reset() {
        super.reset();
        this.mRowMinHeight = 0;
        boolean wasExpanded = isExpanded();
        this.mMaxViewHeight = 0;
        this.mExpandable = false;
        this.mHasUserChangedExpansion = false;
        this.mUserLocked = false;
        this.mShowingPublic = false;
        this.mSensitive = false;
        this.mShowingPublicInitialized = false;
        this.mIsSystemExpanded = false;
        this.mExpansionDisabled = false;
        this.mPublicLayout.reset(this.mIsHeadsUp);
        this.mPrivateLayout.reset(this.mIsHeadsUp);
        resetHeight();
        logExpansionEvent(false, wasExpanded);
    }

    public void resetHeight() {
        if (this.mIsHeadsUp) {
            resetActualHeight();
        }
        this.mMaxExpandHeight = 0;
        this.mHeadsUpHeight = 0;
        this.mWasReset = true;
        onHeightReset();
        requestLayout();
    }

    protected boolean filterMotionEvent(MotionEvent event) {
        return !this.mIsHeadsUp ? super.filterMotionEvent(event) : true;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPublicLayout = (NotificationContentView) findViewById(R.id.expandedPublic);
        this.mPrivateLayout = (NotificationContentView) findViewById(R.id.expanded);
        this.mGutsStub = (ViewStub) findViewById(R.id.notification_guts_stub);
        this.mGutsStub.setOnInflateListener(new OnInflateListener() {
            public void onInflate(ViewStub stub, View inflated) {
                ExpandableNotificationRow.this.mGuts = (NotificationGuts) inflated;
                ExpandableNotificationRow.this.mGuts.setClipTopAmount(ExpandableNotificationRow.this.getClipTopAmount());
                ExpandableNotificationRow.this.mGuts.setActualHeight(ExpandableNotificationRow.this.getActualHeight());
                ExpandableNotificationRow.this.mGutsStub = null;
            }
        });
        this.mExpandButtonStub = (ViewStub) findViewById(R.id.more_button_stub);
        this.mExpandButtonStub.setOnInflateListener(new OnInflateListener() {
            public void onInflate(ViewStub stub, View inflated) {
                ExpandableNotificationRow.this.mExpandButtonContainer = inflated;
                ExpandableNotificationRow.this.mExpandButton = inflated.findViewById(R.id.notification_expand_button);
                ExpandableNotificationRow.this.mExpandButtonDivider = inflated.findViewById(R.id.notification_expand_divider);
                ExpandableNotificationRow.this.mExpandButtonContainer.setOnClickListener(ExpandableNotificationRow.this.mExpandClickListener);
            }
        });
        this.mChildrenContainerStub = (ViewStub) findViewById(R.id.child_container_stub);
        this.mChildrenContainerStub.setOnInflateListener(new OnInflateListener() {
            public void onInflate(ViewStub stub, View inflated) {
                ExpandableNotificationRow.this.mChildrenContainer = (NotificationChildrenContainer) inflated;
                ExpandableNotificationRow.this.mChildrenContainer.setCollapseClickListener(ExpandableNotificationRow.this.mExpandClickListener);
                ExpandableNotificationRow.this.updateChildrenVisibility(false);
            }
        });
        this.mVetoButton = findViewById(R.id.veto);
    }

    public void inflateGuts() {
        if (this.mGuts == null) {
            this.mGutsStub.inflate();
        }
    }

    private void updateChildrenVisibility(boolean animated) {
        int i = 0;
        if (this.mChildrenContainer != null) {
            if (this.mChildExpandAnimator != null) {
                this.mChildExpandAnimator.cancel();
            }
            float targetProgress = this.mChildrenExpanded ? 1.0f : 0.0f;
            if (animated) {
                if (this.mChildrenExpanded) {
                    this.mChildrenContainer.setVisibility(0);
                }
                this.mExpandButtonStart = this.mExpandButtonContainer.getTranslationY();
                this.mChildExpandAnimator = ValueAnimator.ofFloat(new float[]{this.mChildrenExpandProgress, targetProgress});
                this.mChildExpandAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        ExpandableNotificationRow.this.setChildrenExpandProgress(((Float) animation.getAnimatedValue()).floatValue());
                    }
                });
                this.mChildExpandAnimator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        ExpandableNotificationRow.this.mChildExpandAnimator = null;
                        if (!ExpandableNotificationRow.this.mChildrenExpanded) {
                            ExpandableNotificationRow.this.mChildrenContainer.setVisibility(4);
                        }
                    }
                });
                this.mChildExpandAnimator.setInterpolator(this.mLinearInterpolator);
                this.mChildExpandAnimator.setDuration(360);
                this.mChildExpandAnimator.start();
            } else {
                setChildrenExpandProgress(targetProgress);
                NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
                if (!this.mChildrenExpanded) {
                    i = 4;
                }
                notificationChildrenContainer.setVisibility(i);
            }
        }
    }

    private void setChildrenExpandProgress(float progress) {
        this.mChildrenExpandProgress = progress;
        updateExpandButtonAppearance();
        getShowingLayout().setAlpha(PhoneStatusBar.ALPHA_OUT.getInterpolation(1.0f - this.mChildrenExpandProgress));
    }

    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEventInternal(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        NotificationContentView showing = getShowingLayout();
        if (showing != null) {
            showing.setDark(dark, fade, delay);
        }
    }

    public void setHeightRange(int rowMinHeight, int rowMaxHeight) {
        this.mRowMinHeight = rowMinHeight;
        this.mMaxViewHeight = rowMaxHeight;
    }

    public boolean isExpandable() {
        return this.mExpandable;
    }

    public void setExpandable(boolean expandable) {
        this.mExpandable = expandable;
    }

    public boolean hasUserChangedExpansion() {
        return this.mHasUserChangedExpansion;
    }

    public boolean isUserExpanded() {
        return this.mUserExpanded;
    }

    public void setUserExpanded(boolean userExpanded) {
        if (!userExpanded || this.mExpandable) {
            boolean wasExpanded = isExpanded();
            this.mHasUserChangedExpansion = true;
            this.mUserExpanded = userExpanded;
            logExpansionEvent(true, wasExpanded);
        }
    }

    public void resetUserExpansion() {
        this.mHasUserChangedExpansion = false;
        this.mUserExpanded = false;
    }

    public boolean isUserLocked() {
        return this.mUserLocked;
    }

    public void setUserLocked(boolean userLocked) {
        this.mUserLocked = userLocked;
    }

    public boolean isSystemExpanded() {
        return this.mIsSystemExpanded;
    }

    public void setSystemExpanded(boolean expand) {
        if (expand != this.mIsSystemExpanded) {
            boolean wasExpanded = isExpanded();
            this.mIsSystemExpanded = expand;
            notifyHeightChanged(false);
            logExpansionEvent(false, wasExpanded);
        }
    }

    public void setExpansionDisabled(boolean expansionDisabled) {
        if (expansionDisabled != this.mExpansionDisabled) {
            boolean wasExpanded = isExpanded();
            this.mExpansionDisabled = expansionDisabled;
            logExpansionEvent(false, wasExpanded);
            if (wasExpanded != isExpanded()) {
                notifyHeightChanged(false);
            }
        }
    }

    public boolean isClearable() {
        return this.mStatusBarNotification != null ? this.mStatusBarNotification.isClearable() : false;
    }

    public void applyExpansionToLayout() {
        if (isExpanded() && this.mExpandable) {
            setContentHeight(this.mMaxExpandHeight);
        } else {
            setContentHeight(this.mRowMinHeight);
        }
    }

    public int getIntrinsicHeight() {
        if (isUserLocked()) {
            return getActualHeight();
        }
        boolean inExpansionState = isExpanded();
        if (this.mSensitive && this.mHideSensitiveForIntrinsicHeight) {
            return this.mRowMinHeight;
        }
        int maxContentHeight;
        if (this.mIsHeadsUp) {
            if (inExpansionState) {
                maxContentHeight = Math.max(this.mMaxExpandHeight, this.mHeadsUpHeight);
            } else {
                maxContentHeight = Math.max(this.mRowMinHeight, this.mHeadsUpHeight);
            }
        } else if (!inExpansionState && !this.mChildrenExpanded) {
            maxContentHeight = this.mRowMinHeight;
        } else if (this.mChildrenExpanded) {
            maxContentHeight = this.mChildrenContainer.getIntrinsicHeight();
        } else {
            maxContentHeight = getMaxExpandHeight();
        }
        return getBottomDecorHeight() + maxContentHeight;
    }

    protected boolean hasBottomDecor() {
        if (!BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS || this.mIsHeadsUp) {
            return false;
        }
        return this.mGroupManager.hasGroupChildren(this.mStatusBarNotification);
    }

    protected boolean canHaveBottomDecor() {
        return BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS && !this.mIsHeadsUp;
    }

    private boolean isExpanded() {
        if (this.mExpansionDisabled) {
            return false;
        }
        if (hasUserChangedExpansion() || (!isSystemExpanded() && !isSystemChildExpanded())) {
            return isUserExpanded();
        }
        return true;
    }

    private boolean isSystemChildExpanded() {
        return this.mIsSystemChildExpanded;
    }

    public void setSystemChildExpanded(boolean expanded) {
        this.mIsSystemChildExpanded = expanded;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        boolean updateExpandHeight = this.mMaxExpandHeight == 0 && !this.mWasReset;
        updateMaxHeights();
        if (updateExpandHeight) {
            applyExpansionToLayout();
        }
        this.mWasReset = false;
    }

    protected boolean isChildInvisible(View child) {
        return !super.isChildInvisible(child) ? child == this.mChildrenContainer ? this.mIsHeadsUp : false : true;
    }

    private void updateMaxHeights() {
        int intrinsicBefore = getIntrinsicHeight();
        View expandedChild = this.mPrivateLayout.getExpandedChild();
        if (expandedChild == null) {
            expandedChild = this.mPrivateLayout.getContractedChild();
        }
        this.mMaxExpandHeight = expandedChild.getHeight();
        View headsUpChild = this.mPrivateLayout.getHeadsUpChild();
        if (headsUpChild == null) {
            headsUpChild = this.mPrivateLayout.getContractedChild();
        }
        this.mHeadsUpHeight = headsUpChild.getHeight();
        if (intrinsicBefore != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
    }

    public void setSensitive(boolean sensitive) {
        this.mSensitive = sensitive;
    }

    public void setHideSensitiveForIntrinsicHeight(boolean hideSensitive) {
        this.mHideSensitiveForIntrinsicHeight = hideSensitive;
    }

    public void setHideSensitive(boolean hideSensitive, boolean animated, long delay, long duration) {
        int i = 4;
        boolean oldShowingPublic = this.mShowingPublic;
        if (!this.mSensitive) {
            hideSensitive = false;
        }
        this.mShowingPublic = hideSensitive;
        if ((!this.mShowingPublicInitialized || this.mShowingPublic != oldShowingPublic) && this.mPublicLayout.getChildCount() != 0) {
            if (animated) {
                animateShowingPublic(delay, duration);
            } else {
                this.mPublicLayout.animate().cancel();
                this.mPrivateLayout.animate().cancel();
                this.mPublicLayout.setAlpha(1.0f);
                this.mPrivateLayout.setAlpha(1.0f);
                this.mPublicLayout.setVisibility(this.mShowingPublic ? 0 : 4);
                NotificationContentView notificationContentView = this.mPrivateLayout;
                if (!this.mShowingPublic) {
                    i = 0;
                }
                notificationContentView.setVisibility(i);
            }
            updateVetoButton();
            this.mShowingPublicInitialized = true;
        }
    }

    private void animateShowingPublic(long delay, long duration) {
        final View source = this.mShowingPublic ? this.mPrivateLayout : this.mPublicLayout;
        View target = this.mShowingPublic ? this.mPublicLayout : this.mPrivateLayout;
        source.setVisibility(0);
        target.setVisibility(0);
        target.setAlpha(0.0f);
        source.animate().cancel();
        target.animate().cancel();
        source.animate().alpha(0.0f).setStartDelay(delay).setDuration(duration).withEndAction(new Runnable() {
            public void run() {
                source.setVisibility(4);
            }
        });
        target.animate().alpha(1.0f).setStartDelay(delay).setDuration(duration);
    }

    private void updateVetoButton() {
        View view = this.mVetoButton;
        int i = (!isClearable() || this.mShowingPublic) ? 8 : 0;
        view.setVisibility(i);
    }

    public void setChildrenExpanded(boolean expanded, boolean animate) {
        this.mChildrenExpanded = expanded;
        updateChildrenVisibility(animate);
    }

    public void updateExpandButton() {
        boolean hasExpand = hasBottomDecor();
        if (hasExpand != this.mHasExpandAction) {
            if (hasExpand) {
                if (this.mExpandButtonContainer == null) {
                    this.mExpandButtonStub.inflate();
                }
                this.mExpandButtonContainer.setVisibility(0);
                updateExpandButtonAppearance();
                updateExpandButtonColor();
            } else if (this.mExpandButtonContainer != null) {
                this.mExpandButtonContainer.setVisibility(8);
            }
            notifyHeightChanged(true);
        }
        this.mHasExpandAction = hasExpand;
    }

    private void updateExpandButtonAppearance() {
        if (this.mExpandButtonContainer != null) {
            float expandButtonAlpha = 0.0f;
            float expandButtonTranslation = 0.0f;
            float containerTranslation = 0.0f;
            int minHeight = getMinHeight();
            if (!(this.mChildrenExpanded && this.mChildExpandAnimator == null)) {
                int expandActionHeight = getBottomDecorHeight();
                int translationY = getActualHeight() - expandActionHeight;
                if (translationY > minHeight) {
                    containerTranslation = (float) translationY;
                    expandButtonAlpha = 1.0f;
                    expandButtonTranslation = 0.0f;
                } else {
                    float progress;
                    containerTranslation = (float) minHeight;
                    if (expandActionHeight != 0) {
                        progress = ((float) (minHeight - translationY)) / ((float) expandActionHeight);
                    } else {
                        progress = 1.0f;
                    }
                    expandButtonTranslation = ((-progress) * ((float) expandActionHeight)) * 0.7f;
                    expandButtonAlpha = 1.0f - PhoneStatusBar.ALPHA_OUT.getInterpolation(Math.min(progress / 0.7f, 1.0f));
                }
            }
            if (this.mChildExpandAnimator != null || this.mChildrenExpanded) {
                float collapsedHeight;
                expandButtonAlpha *= 1.0f - this.mChildrenExpandProgress;
                expandButtonTranslation *= 1.0f - this.mChildrenExpandProgress;
                float newTranslation = (float) (-getBottomDecorHeight());
                if (this.mChildrenExpanded) {
                    collapsedHeight = this.mExpandButtonStart;
                } else {
                    collapsedHeight = (float) Math.max(StackStateAnimator.getFinalActualHeight(this) - getBottomDecorHeight(), minHeight);
                }
                float translationProgress = this.mFastOutSlowInInterpolator.getInterpolation(this.mChildrenExpandProgress);
                containerTranslation = ((1.0f - translationProgress) * collapsedHeight) + (translationProgress * newTranslation);
            }
            this.mExpandButton.setAlpha(expandButtonAlpha);
            this.mExpandButtonDivider.setAlpha(expandButtonAlpha);
            this.mExpandButton.setTranslationY(expandButtonTranslation);
            this.mExpandButtonContainer.setTranslationY(containerTranslation);
            NotificationContentView showingLayout = getShowingLayout();
            float layoutTranslation = Math.min(this.mExpandButtonContainer.getTranslationY() - ((float) showingLayout.getContentHeight()), 0.0f);
            if (!this.mChildrenExpanded && this.mChildExpandAnimator == null) {
                layoutTranslation = 0.0f;
            }
            showingLayout.setTranslationY(layoutTranslation);
            if (this.mChildrenContainer != null) {
                this.mChildrenContainer.setTranslationY(this.mExpandButtonContainer.getTranslationY() + ((float) getBottomDecorHeight()));
            }
        }
    }

    private void updateExpandButtonColor() {
        int color = getRippleColor();
        if (color == this.mNormalRippleColor) {
            color = 0;
        }
        if (this.mExpandButtonDivider != null) {
            applyTint(this.mExpandButtonDivider, color);
        }
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setTintColor(color);
        }
    }

    public static void applyTint(View v, int color) {
        int alpha;
        if (color != 0) {
            alpha = 123;
        } else {
            color = -16777216;
            alpha = 41;
        }
        if (v.getBackground() instanceof ColorDrawable) {
            ColorDrawable background = (ColorDrawable) v.getBackground();
            background.mutate();
            background.setColor(color);
            background.setAlpha(alpha);
        }
    }

    public int getMaxExpandHeight() {
        return this.mMaxExpandHeight;
    }

    public boolean isContentExpandable() {
        return getShowingLayout().isContentExpandable();
    }

    protected View getContentView() {
        return getShowingLayout();
    }

    public void setActualHeight(int height, boolean notifyListeners) {
        super.setActualHeight(height, notifyListeners);
        int contentHeight = calculateContentHeightFromActualHeight(height);
        this.mPrivateLayout.setContentHeight(contentHeight);
        this.mPublicLayout.setContentHeight(contentHeight);
        if (this.mGuts != null) {
            this.mGuts.setActualHeight(height);
        }
        invalidate();
        updateExpandButtonAppearance();
    }

    public int getMaxContentHeight() {
        return getShowingLayout().getMaxHeight();
    }

    public int getMinHeight() {
        return getShowingLayout().getMinHeight();
    }

    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        this.mPrivateLayout.setClipTopAmount(clipTopAmount);
        this.mPublicLayout.setClipTopAmount(clipTopAmount);
        if (this.mGuts != null) {
            this.mGuts.setClipTopAmount(clipTopAmount);
        }
    }

    public void notifyContentUpdated() {
        this.mPublicLayout.notifyContentUpdated();
        this.mPrivateLayout.notifyContentUpdated();
    }

    public boolean isMaxExpandHeightInitialized() {
        return this.mMaxExpandHeight != 0;
    }

    private NotificationContentView getShowingLayout() {
        return this.mShowingPublic ? this.mPublicLayout : this.mPrivateLayout;
    }

    public void setShowingLegacyBackground(boolean showing) {
        super.setShowingLegacyBackground(showing);
        this.mPrivateLayout.setShowingLegacyBackground(showing);
        this.mPublicLayout.setShowingLegacyBackground(showing);
    }

    public void setExpansionLogger(ExpansionLogger logger, String key) {
        this.mLogger = logger;
        this.mLoggingKey = key;
    }

    private void logExpansionEvent(boolean userAction, boolean wasExpanded) {
        boolean nowExpanded = isExpanded();
        if (wasExpanded != nowExpanded && this.mLogger != null) {
            this.mLogger.logNotificationExpansion(this.mLoggingKey, userAction, nowExpanded);
        }
    }
}
