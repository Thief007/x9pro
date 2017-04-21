package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import java.util.ArrayList;
import java.util.List;

public class NotificationChildrenContainer extends ViewGroup {
    private final int mChildPadding;
    private final List<ExpandableNotificationRow> mChildren;
    private final View mCollapseButton;
    private final int mCollapseButtonHeight;
    private final View mCollapseDivider;
    private final int mDividerHeight;
    private final List<View> mDividers;
    private final int mMaxNotificationHeight;
    private final int mNotificationAppearDistance;

    public NotificationChildrenContainer(Context context) {
        this(context, null);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDividers = new ArrayList();
        this.mChildren = new ArrayList();
        this.mChildPadding = getResources().getDimensionPixelSize(R.dimen.notification_children_padding);
        this.mDividerHeight = getResources().getDimensionPixelSize(R.dimen.notification_children_divider_height);
        this.mMaxNotificationHeight = getResources().getDimensionPixelSize(R.dimen.notification_max_height);
        this.mNotificationAppearDistance = getResources().getDimensionPixelSize(R.dimen.notification_appear_distance);
        this.mCollapseButton = ((LayoutInflater) this.mContext.getSystemService(LayoutInflater.class)).inflate(R.layout.notification_collapse_button, this, false);
        this.mCollapseButtonHeight = getResources().getDimensionPixelSize(R.dimen.notification_bottom_decor_height);
        addView(this.mCollapseButton);
        this.mCollapseDivider = inflateDivider();
        addView(this.mCollapseDivider);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = this.mChildren.size();
        boolean firstChild = true;
        for (int i = 0; i < childCount; i++) {
            View child = (View) this.mChildren.get(i);
            boolean viewGone = child.getVisibility() == 8;
            if (i != 0) {
                View divider = (View) this.mDividers.get(i - 1);
                int dividerVisibility = divider.getVisibility();
                int newVisibility = viewGone ? 4 : 0;
                if (dividerVisibility != newVisibility) {
                    divider.setVisibility(newVisibility);
                }
            }
            if (!viewGone) {
                child.layout(0, 0, getWidth(), child.getMeasuredHeight());
                if (firstChild) {
                    firstChild = false;
                } else {
                    ((View) this.mDividers.get(i - 1)).layout(0, 0, getWidth(), this.mDividerHeight);
                }
            }
        }
        this.mCollapseButton.layout(0, 0, getWidth(), this.mCollapseButtonHeight);
        this.mCollapseDivider.layout(0, this.mCollapseButtonHeight - this.mDividerHeight, getWidth(), this.mCollapseButtonHeight);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ownMaxHeight = this.mMaxNotificationHeight;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean hasFixedHeight = heightMode == 1073741824;
        boolean isHeightLimited = heightMode == Integer.MIN_VALUE;
        if (hasFixedHeight || isHeightLimited) {
            ownMaxHeight = Math.min(ownMaxHeight, MeasureSpec.getSize(heightMeasureSpec));
        }
        int newHeightSpec = MeasureSpec.makeMeasureSpec(ownMaxHeight, Integer.MIN_VALUE);
        int dividerHeightSpec = MeasureSpec.makeMeasureSpec(this.mDividerHeight, 1073741824);
        this.mCollapseButton.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(this.mCollapseButtonHeight, 1073741824));
        this.mCollapseDivider.measure(widthMeasureSpec, dividerHeightSpec);
        int height = this.mCollapseButtonHeight;
        int childCount = this.mChildren.size();
        boolean firstChild = true;
        for (int i = 0; i < childCount; i++) {
            View child = (View) this.mChildren.get(i);
            if (child.getVisibility() != 8) {
                child.measure(widthMeasureSpec, newHeightSpec);
                height += child.getMeasuredHeight();
                if (firstChild) {
                    firstChild = false;
                } else {
                    ((View) this.mDividers.get(i - 1)).measure(widthMeasureSpec, dividerHeightSpec);
                    height += this.mChildPadding;
                }
            }
        }
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (hasFixedHeight) {
            height = ownMaxHeight;
        } else if (isHeightLimited) {
            height = Math.min(ownMaxHeight, height);
        }
        setMeasuredDimension(width, height);
    }

    public void addNotification(ExpandableNotificationRow row, int childIndex) {
        int newIndex = childIndex < 0 ? this.mChildren.size() : childIndex;
        this.mChildren.add(newIndex, row);
        addView(row);
        if (this.mChildren.size() != 1) {
            View divider = inflateDivider();
            addView(divider);
            this.mDividers.add(Math.max(newIndex - 1, 0), divider);
        }
    }

    public void removeNotification(ExpandableNotificationRow row) {
        int childIndex = this.mChildren.indexOf(row);
        this.mChildren.remove(row);
        removeView(row);
        if (!this.mDividers.isEmpty()) {
            removeView((View) this.mDividers.remove(Math.max(childIndex - 1, 0)));
        }
        row.setSystemChildExpanded(false);
    }

    private View inflateDivider() {
        return LayoutInflater.from(this.mContext).inflate(R.layout.notification_children_divider, this, false);
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        return this.mChildren;
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> childOrder) {
        if (childOrder == null) {
            return false;
        }
        boolean result = false;
        int i = 0;
        while (i < this.mChildren.size() && i < childOrder.size()) {
            ExpandableNotificationRow desiredChild = (ExpandableNotificationRow) childOrder.get(i);
            if (((ExpandableNotificationRow) this.mChildren.get(i)) != desiredChild) {
                this.mChildren.remove(desiredChild);
                this.mChildren.add(i, desiredChild);
                result = true;
            }
            i++;
        }
        boolean first = true;
        for (i = 0; i < childOrder.size(); i++) {
            ((ExpandableNotificationRow) childOrder.get(i)).setSystemChildExpanded(first);
            first = false;
        }
        return result;
    }

    public int getIntrinsicHeight() {
        int childCount = this.mChildren.size();
        int intrinsicHeight = 0;
        int visibleChildren = 0;
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = (ExpandableNotificationRow) this.mChildren.get(i);
            if (child.getVisibility() != 8) {
                intrinsicHeight += child.getIntrinsicHeight();
                visibleChildren++;
            }
        }
        if (visibleChildren > 0) {
            return intrinsicHeight + ((visibleChildren - 1) * this.mDividerHeight);
        }
        return intrinsicHeight;
    }

    public void getState(StackScrollState resultState, StackViewState parentState) {
        int childCount = this.mChildren.size();
        int yPosition = this.mCollapseButtonHeight;
        boolean firstChild = true;
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = (ExpandableNotificationRow) this.mChildren.get(i);
            if (child.getVisibility() != 8) {
                if (firstChild) {
                    firstChild = false;
                } else {
                    yPosition += this.mChildPadding;
                }
                StackViewState childState = resultState.getViewStateForView(child);
                int intrinsicHeight = child.getIntrinsicHeight();
                childState.yTranslation = (float) yPosition;
                childState.zTranslation = 0.0f;
                childState.height = intrinsicHeight;
                childState.dimmed = parentState.dimmed;
                childState.dark = parentState.dark;
                childState.hideSensitive = parentState.hideSensitive;
                childState.belowSpeedBump = parentState.belowSpeedBump;
                childState.scale = parentState.scale;
                childState.clipTopAmount = 0;
                childState.topOverLap = 0;
                childState.location = parentState.location;
                yPosition += intrinsicHeight;
            }
        }
    }

    public void applyState(StackScrollState state) {
        int childCount = this.mChildren.size();
        boolean firstChild = true;
        ViewState dividerState = new ViewState();
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = (ExpandableNotificationRow) this.mChildren.get(i);
            StackViewState viewState = state.getViewStateForView(child);
            if (child.getVisibility() != 8) {
                if (firstChild) {
                    firstChild = false;
                } else {
                    View divider = (View) this.mDividers.get(i - 1);
                    dividerState.initFrom(divider);
                    dividerState.yTranslation = (float) ((int) (viewState.yTranslation - (((float) (this.mChildPadding + this.mDividerHeight)) / 2.0f)));
                    dividerState.alpha = 1.0f;
                    state.applyViewState(divider, dividerState);
                }
                state.applyState(child, viewState);
            }
        }
    }

    public void setCollapseClickListener(OnClickListener collapseClickListener) {
        this.mCollapseButton.setOnClickListener(collapseClickListener);
    }

    public void prepareExpansionChanged(StackScrollState state) {
        int childCount = this.mChildren.size();
        boolean firstChild = true;
        StackViewState sourceState = new StackViewState();
        ViewState dividerState = new ViewState();
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = (ExpandableNotificationRow) this.mChildren.get(i);
            StackViewState viewState = state.getViewStateForView(child);
            if (child.getVisibility() != 8) {
                if (firstChild) {
                    firstChild = false;
                } else {
                    View divider = (View) this.mDividers.get(i - 1);
                    dividerState.initFrom(divider);
                    dividerState.yTranslation = (viewState.yTranslation - (((float) (this.mChildPadding + this.mDividerHeight)) / 2.0f)) + ((float) this.mNotificationAppearDistance);
                    dividerState.alpha = 0.0f;
                    state.applyViewState(divider, dividerState);
                }
                sourceState.copyFrom(viewState);
                sourceState.alpha = 0.0f;
                sourceState.yTranslation += (float) this.mNotificationAppearDistance;
                state.applyState(child, sourceState);
            }
        }
        this.mCollapseButton.setAlpha(0.0f);
        this.mCollapseDivider.setAlpha(0.0f);
        this.mCollapseDivider.setTranslationY((float) (this.mNotificationAppearDistance / 4));
    }

    public void startAnimationToState(StackScrollState state, StackStateAnimator stateAnimator, boolean withDelays, long baseDelay, long duration) {
        int childCount = this.mChildren.size();
        boolean firstChild = true;
        ViewState dividerState = new ViewState();
        int notGoneIndex = 0;
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = (ExpandableNotificationRow) this.mChildren.get(i);
            StackViewState viewState = state.getViewStateForView(child);
            if (child.getVisibility() != 8) {
                int i2;
                int difference = Math.min(3, notGoneIndex + 1);
                if (withDelays) {
                    i2 = difference * 54;
                } else {
                    i2 = 0;
                }
                long delay = ((long) i2) + baseDelay;
                if (firstChild) {
                    firstChild = false;
                } else {
                    View divider = (View) this.mDividers.get(i - 1);
                    dividerState.initFrom(divider);
                    dividerState.yTranslation = viewState.yTranslation - (((float) (this.mChildPadding + this.mDividerHeight)) / 2.0f);
                    dividerState.alpha = 1.0f;
                    stateAnimator.startViewAnimations(divider, dividerState, delay, duration);
                }
                stateAnimator.startStackAnimations(child, viewState, state, -1, delay);
                notGoneIndex++;
            }
        }
        dividerState.initFrom(this.mCollapseButton);
        dividerState.alpha = 1.0f;
        stateAnimator.startViewAnimations(this.mCollapseButton, dividerState, baseDelay, duration);
        dividerState.initFrom(this.mCollapseDivider);
        dividerState.alpha = 1.0f;
        dividerState.yTranslation = 0.0f;
        stateAnimator.startViewAnimations(this.mCollapseDivider, dividerState, baseDelay, duration);
    }

    public ExpandableNotificationRow getViewAtPosition(float y) {
        int count = this.mChildren.size();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableNotificationRow slidingChild = (ExpandableNotificationRow) this.mChildren.get(childIdx);
            float childTop = slidingChild.getTranslationY();
            float bottom = childTop + ((float) slidingChild.getActualHeight());
            if (y >= childTop + ((float) slidingChild.getClipTopAmount()) && y <= bottom) {
                return slidingChild;
            }
        }
        return null;
    }

    public void setTintColor(int color) {
        ExpandableNotificationRow.applyTint(this.mCollapseDivider, color);
    }
}
