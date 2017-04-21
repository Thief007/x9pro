package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.util.ArrayList;
import java.util.List;

public class StackScrollAlgorithm {
    private StackIndentationFunctor mBottomStackIndentationFunctor;
    private int mBottomStackPeekSize;
    private int mBottomStackSlowDownLength;
    private int mCollapseSecondCardPadding;
    private int mCollapsedSize;
    private boolean mExpandedOnStart;
    private int mFirstChildMaxHeight;
    private ExpandableView mFirstChildWhileExpanding;
    private HeadsUpManager mHeadsUpManager;
    private boolean mIsExpanded;
    private boolean mIsExpansionChanging;
    private boolean mIsSmallScreen;
    private int mMaxNotificationHeight;
    private int mNotificationsTopPadding;
    private int mPaddingBetweenElements;
    private int mPaddingBetweenElementsDimmed;
    private int mPaddingBetweenElementsNormal;
    private int mRoundedRectCornerRadius;
    private boolean mScaleDimmed;
    private StackScrollAlgorithmState mTempAlgorithmState = new StackScrollAlgorithmState();
    private StackIndentationFunctor mTopStackIndentationFunctor;
    private int mTopStackPeekSize;
    private int mTopStackSlowDownLength;
    private int mTopStackTotalSize;
    private int mZBasicHeight;
    private int mZDistanceBetweenElements;

    class StackScrollAlgorithmState {
        public float itemsInBottomStack;
        public float itemsInTopStack;
        public int lastTopStackIndex;
        public float partialInBottom;
        public float partialInTop;
        public int scrollY;
        public float scrolledPixelsTop;
        public final ArrayList<ExpandableView> visibleChildren = new ArrayList();

        StackScrollAlgorithmState() {
        }
    }

    public StackScrollAlgorithm(Context context) {
        initConstants(context);
        updatePadding(false);
    }

    private void updatePadding(boolean dimmed) {
        int i;
        if (dimmed && this.mScaleDimmed) {
            i = this.mPaddingBetweenElementsDimmed;
        } else {
            i = this.mPaddingBetweenElementsNormal;
        }
        this.mPaddingBetweenElements = i;
        this.mTopStackTotalSize = (this.mTopStackSlowDownLength + this.mPaddingBetweenElements) + this.mTopStackPeekSize;
        this.mTopStackIndentationFunctor = new PiecewiseLinearIndentationFunctor(3, this.mTopStackPeekSize, this.mTopStackTotalSize - this.mTopStackPeekSize, 0.5f);
        this.mBottomStackIndentationFunctor = new PiecewiseLinearIndentationFunctor(3, this.mBottomStackPeekSize, getBottomStackSlowDownLength(), 0.5f);
    }

    public int getBottomStackSlowDownLength() {
        return this.mBottomStackSlowDownLength + this.mPaddingBetweenElements;
    }

    private void initConstants(Context context) {
        boolean z;
        this.mPaddingBetweenElementsDimmed = context.getResources().getDimensionPixelSize(R.dimen.notification_padding_dimmed);
        this.mPaddingBetweenElementsNormal = context.getResources().getDimensionPixelSize(R.dimen.notification_padding);
        this.mNotificationsTopPadding = context.getResources().getDimensionPixelSize(R.dimen.notifications_top_padding);
        this.mCollapsedSize = context.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mMaxNotificationHeight = context.getResources().getDimensionPixelSize(R.dimen.notification_max_height);
        this.mTopStackPeekSize = context.getResources().getDimensionPixelSize(R.dimen.top_stack_peek_amount);
        this.mBottomStackPeekSize = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_peek_amount);
        this.mZDistanceBetweenElements = context.getResources().getDimensionPixelSize(R.dimen.z_distance_between_notifications);
        this.mZBasicHeight = this.mZDistanceBetweenElements * 4;
        this.mBottomStackSlowDownLength = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_slow_down_length);
        this.mTopStackSlowDownLength = context.getResources().getDimensionPixelSize(R.dimen.top_stack_slow_down_length);
        this.mRoundedRectCornerRadius = context.getResources().getDimensionPixelSize(R.dimen.notification_material_rounded_rect_radius);
        this.mCollapseSecondCardPadding = context.getResources().getDimensionPixelSize(R.dimen.notification_collapse_second_card_padding);
        if (context.getResources().getDisplayMetrics().densityDpi >= 480) {
            z = true;
        } else {
            z = false;
        }
        this.mScaleDimmed = z;
    }

    public boolean shouldScaleDimmed() {
        return this.mScaleDimmed;
    }

    public void getStackScrollState(AmbientState ambientState, StackScrollState resultState) {
        StackScrollAlgorithmState algorithmState = this.mTempAlgorithmState;
        resultState.resetViewStates();
        algorithmState.itemsInTopStack = 0.0f;
        algorithmState.partialInTop = 0.0f;
        algorithmState.lastTopStackIndex = 0;
        algorithmState.scrolledPixelsTop = 0.0f;
        algorithmState.itemsInBottomStack = 0.0f;
        algorithmState.partialInBottom = 0.0f;
        algorithmState.scrollY = (int) (((float) (this.mCollapsedSize + Math.max(0, ambientState.getScrollY()))) + ambientState.getOverScrollAmount(false));
        updateVisibleChildren(resultState, algorithmState);
        findNumberOfItemsInTopStackAndUpdateState(resultState, algorithmState, ambientState);
        updatePositionsForState(resultState, algorithmState, ambientState);
        updateZValuesForState(resultState, algorithmState);
        handleDraggedViews(ambientState, resultState, algorithmState);
        updateDimmedActivatedHideSensitive(ambientState, resultState, algorithmState);
        updateClipping(resultState, algorithmState, ambientState);
        updateSpeedBumpState(resultState, algorithmState, ambientState.getSpeedBumpIndex());
        getNotificationChildrenStates(resultState, algorithmState);
    }

    private void getNotificationChildrenStates(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableView v = (ExpandableView) algorithmState.visibleChildren.get(i);
            if (v instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) v).getChildrenStates(resultState);
            }
        }
    }

    private void updateSpeedBumpState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, int speedBumpIndex) {
        int childCount = algorithmState.visibleChildren.size();
        int i = 0;
        while (i < childCount) {
            StackViewState childViewState = resultState.getViewStateForView((View) algorithmState.visibleChildren.get(i));
            boolean z = speedBumpIndex != -1 && i >= speedBumpIndex;
            childViewState.belowSpeedBump = z;
            i++;
        }
    }

    private void updateClipping(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        boolean dismissAllInProgress = ambientState.isDismissAllInProgress();
        float previousNotificationEnd = 0.0f;
        float previousNotificationStart = 0.0f;
        boolean previousNotificationIsSwiped = false;
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            float clipHeight;
            ExpandableView child = (ExpandableView) algorithmState.visibleChildren.get(i);
            StackViewState state = resultState.getViewStateForView(child);
            float newYTranslation = state.yTranslation + ((((float) state.height) * (1.0f - state.scale)) / 2.0f);
            float newHeight = ((float) state.height) * state.scale;
            float newNotificationEnd = newYTranslation + newHeight;
            if (previousNotificationIsSwiped) {
                clipHeight = newHeight;
            } else {
                clipHeight = Math.max(0.0f, newNotificationEnd - previousNotificationEnd);
                if (clipHeight != 0.0f) {
                    float clippingCorrection;
                    if (state.dimmed) {
                        clippingCorrection = 0.0f;
                    } else {
                        clippingCorrection = ((float) this.mRoundedRectCornerRadius) * state.scale;
                    }
                    clipHeight += clippingCorrection;
                }
            }
            updateChildClippingAndBackground(state, newHeight, clipHeight, newHeight - (previousNotificationStart - newYTranslation));
            if (dismissAllInProgress) {
                state.clipTopAmount = Math.max(child.getMinClipTopAmount(), state.clipTopAmount);
            }
            if (!child.isTransparent()) {
                if (dismissAllInProgress && canChildBeDismissed(child)) {
                    previousNotificationIsSwiped = true;
                } else {
                    previousNotificationIsSwiped = ambientState.getDraggedViews().contains(child);
                    previousNotificationEnd = newNotificationEnd;
                    previousNotificationStart = newYTranslation + (((float) state.clipTopAmount) * state.scale);
                }
            }
        }
    }

    public static boolean canChildBeDismissed(View v) {
        View veto = v.findViewById(R.id.veto);
        if (veto == null || veto.getVisibility() == 8) {
            return false;
        }
        return true;
    }

    private void updateChildClippingAndBackground(StackViewState state, float realHeight, float clipHeight, float backgroundHeight) {
        if (realHeight > clipHeight) {
            state.topOverLap = (int) Math.floor((double) ((realHeight - clipHeight) / state.scale));
        } else {
            state.topOverLap = 0;
        }
        if (realHeight > backgroundHeight) {
            state.clipTopAmount = (int) Math.floor((double) ((realHeight - backgroundHeight) / state.scale));
        } else {
            state.clipTopAmount = 0;
        }
    }

    private void updateDimmedActivatedHideSensitive(AmbientState ambientState, StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        boolean dimmed = ambientState.isDimmed();
        boolean dark = ambientState.isDark();
        boolean hideSensitive = ambientState.isHideSensitive();
        View activatedChild = ambientState.getActivatedChild();
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            float f;
            View child = (View) algorithmState.visibleChildren.get(i);
            StackViewState childViewState = resultState.getViewStateForView(child);
            childViewState.dimmed = dimmed;
            childViewState.dark = dark;
            childViewState.hideSensitive = hideSensitive;
            boolean isActivatedChild = activatedChild == child;
            if (this.mScaleDimmed && dimmed && !isActivatedChild) {
                f = 0.95f;
            } else {
                f = 1.0f;
            }
            childViewState.scale = f;
            if (dimmed && isActivatedChild) {
                childViewState.zTranslation += ((float) this.mZDistanceBetweenElements) * 2.0f;
            }
        }
    }

    private void handleDraggedViews(AmbientState ambientState, StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        ArrayList<View> draggedViews = ambientState.getDraggedViews();
        for (View draggedView : draggedViews) {
            int childIndex = algorithmState.visibleChildren.indexOf(draggedView);
            if (childIndex >= 0 && childIndex < algorithmState.visibleChildren.size() - 1) {
                View nextChild = (View) algorithmState.visibleChildren.get(childIndex + 1);
                if (!draggedViews.contains(nextChild)) {
                    StackViewState viewState = resultState.getViewStateForView(nextChild);
                    if (ambientState.isShadeExpanded()) {
                        viewState.alpha = 1.0f;
                    }
                }
                resultState.getViewStateForView(draggedView).alpha = draggedView.getAlpha();
            }
        }
    }

    private void updateVisibleChildren(StackScrollState resultState, StackScrollAlgorithmState state) {
        ViewGroup hostView = resultState.getHostView();
        int childCount = hostView.getChildCount();
        state.visibleChildren.clear();
        state.visibleChildren.ensureCapacity(childCount);
        int notGoneIndex = 0;
        for (int i = 0; i < childCount; i++) {
            ExpandableView v = (ExpandableView) hostView.getChildAt(i);
            if (v.getVisibility() != 8) {
                notGoneIndex = updateNotGoneIndex(resultState, state, notGoneIndex, v);
                if (v instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                    List<ExpandableNotificationRow> children = row.getNotificationChildren();
                    if (row.areChildrenExpanded() && children != null) {
                        for (ExpandableNotificationRow childRow : children) {
                            if (childRow.getVisibility() != 8) {
                                resultState.getViewStateForView(childRow).notGoneIndex = notGoneIndex;
                                notGoneIndex++;
                            }
                        }
                    }
                }
            }
        }
    }

    private int updateNotGoneIndex(StackScrollState resultState, StackScrollAlgorithmState state, int notGoneIndex, ExpandableView v) {
        resultState.getViewStateForView(v).notGoneIndex = notGoneIndex;
        state.visibleChildren.add(v);
        return notGoneIndex + 1;
    }

    private void updatePositionsForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        int numberOfElementsCompletelyIn;
        float bottomPeekStart = (float) (ambientState.getInnerHeight() - this.mBottomStackPeekSize);
        float bottomStackStart = bottomPeekStart - ((float) this.mBottomStackSlowDownLength);
        float currentYPosition = 0.0f;
        float yPositionInScrollView = 0.0f;
        View topHeadsUpEntry = ambientState.getTopHeadsUpEntry();
        int childCount = algorithmState.visibleChildren.size();
        if (algorithmState.partialInTop == 1.0f) {
            numberOfElementsCompletelyIn = algorithmState.lastTopStackIndex;
        } else {
            numberOfElementsCompletelyIn = (int) algorithmState.itemsInTopStack;
        }
        int i = 0;
        while (i < childCount) {
            View child = (ExpandableView) algorithmState.visibleChildren.get(i);
            StackViewState childViewState = resultState.getViewStateForView(child);
            childViewState.location = 0;
            int childHeight = getMaxAllowedChildHeight(child, ambientState);
            float yPositionInScrollViewAfterElement = (((float) childHeight) + yPositionInScrollView) + ((float) this.mPaddingBetweenElements);
            float scrollOffset = (yPositionInScrollView - ((float) algorithmState.scrollY)) + ((float) this.mCollapsedSize);
            if (i == algorithmState.lastTopStackIndex + 1) {
                currentYPosition = Math.min(scrollOffset, bottomStackStart);
            }
            childViewState.yTranslation = currentYPosition;
            float nextYPosition = (((float) childHeight) + currentYPosition) + ((float) this.mPaddingBetweenElements);
            if (i <= algorithmState.lastTopStackIndex) {
                updateStateForTopStackChild(algorithmState, numberOfElementsCompletelyIn, i, childHeight, childViewState, scrollOffset);
                clampPositionToTopStackEnd(childViewState, childHeight);
                if ((childViewState.yTranslation + ((float) childHeight)) + ((float) this.mPaddingBetweenElements) >= bottomStackStart && !this.mIsExpansionChanging && i != 0 && this.mIsSmallScreen) {
                    childViewState.height = (int) Math.max((bottomStackStart - ((float) this.mPaddingBetweenElements)) - childViewState.yTranslation, (float) this.mCollapsedSize);
                    updateStateForChildTransitioningInBottom(algorithmState, bottomStackStart, bottomPeekStart, childViewState.yTranslation, childViewState, childHeight);
                }
                clampPositionToBottomStackStart(childViewState, childViewState.height, ambientState);
            } else if (nextYPosition < bottomStackStart) {
                childViewState.location = 8;
                clampYTranslation(childViewState, childHeight, ambientState);
            } else if (currentYPosition >= bottomStackStart) {
                updateStateForChildFullyInBottomStack(algorithmState, bottomStackStart, childViewState, childHeight, ambientState);
            } else {
                updateStateForChildTransitioningInBottom(algorithmState, bottomStackStart, bottomPeekStart, currentYPosition, childViewState, childHeight);
            }
            if (i == 0) {
                childViewState.alpha = 1.0f;
                childViewState.yTranslation = (float) Math.max(this.mCollapsedSize - algorithmState.scrollY, 0);
                if (childViewState.yTranslation + ((float) childViewState.height) > bottomPeekStart - ((float) this.mCollapseSecondCardPadding)) {
                    childViewState.height = (int) Math.max((bottomPeekStart - ((float) this.mCollapseSecondCardPadding)) - childViewState.yTranslation, (float) this.mCollapsedSize);
                }
                childViewState.location = 1;
            }
            if (childViewState.location == 0) {
                Log.wtf("StackScrollAlgorithm", "Failed to assign location for child " + i);
            }
            currentYPosition = (childViewState.yTranslation + ((float) childHeight)) + ((float) this.mPaddingBetweenElements);
            yPositionInScrollView = yPositionInScrollViewAfterElement;
            if (!(!ambientState.isShadeExpanded() || topHeadsUpEntry == null || child == topHeadsUpEntry)) {
                childViewState.yTranslation += (float) (topHeadsUpEntry.getHeadsUpHeight() - this.mCollapsedSize);
            }
            childViewState.yTranslation += ambientState.getTopPadding() + ambientState.getStackTranslation();
            i++;
        }
        updateHeadsUpStates(resultState, algorithmState, ambientState);
    }

    private void updateHeadsUpStates(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        int childCount = algorithmState.visibleChildren.size();
        ExpandableNotificationRow topHeadsUpEntry = null;
        int i = 0;
        while (i < childCount) {
            View child = (View) algorithmState.visibleChildren.get(i);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                if (row.isHeadsUp()) {
                    if (topHeadsUpEntry == null) {
                        topHeadsUpEntry = row;
                    }
                    StackViewState childState = resultState.getViewStateForView(row);
                    boolean isTopEntry = topHeadsUpEntry == row;
                    if (this.mIsExpanded) {
                        if (isTopEntry) {
                            childState.height += row.getHeadsUpHeight() - this.mCollapsedSize;
                        }
                        childState.height = Math.max(childState.height, row.getHeadsUpHeight());
                        childState.yTranslation = Math.min(childState.yTranslation, ambientState.getMaxHeadsUpTranslation() - ((float) childState.height));
                    }
                    if (row.isPinned()) {
                        childState.yTranslation = Math.max(childState.yTranslation, (float) this.mNotificationsTopPadding);
                        childState.height = row.getHeadsUpHeight();
                        if (!isTopEntry) {
                            StackViewState topState = resultState.getViewStateForView(topHeadsUpEntry);
                            childState.height = row.getHeadsUpHeight();
                            childState.yTranslation = (topState.yTranslation + ((float) topState.height)) - ((float) childState.height);
                        }
                    }
                    i++;
                } else {
                    return;
                }
            }
            return;
        }
    }

    private void clampYTranslation(StackViewState childViewState, int childHeight, AmbientState ambientState) {
        clampPositionToBottomStackStart(childViewState, childHeight, ambientState);
        clampPositionToTopStackEnd(childViewState, childHeight);
    }

    private void clampPositionToBottomStackStart(StackViewState childViewState, int childHeight, AmbientState ambientState) {
        childViewState.yTranslation = Math.min(childViewState.yTranslation, (float) (((ambientState.getInnerHeight() - this.mBottomStackPeekSize) - this.mCollapseSecondCardPadding) - childHeight));
    }

    private void clampPositionToTopStackEnd(StackViewState childViewState, int childHeight) {
        childViewState.yTranslation = Math.max(childViewState.yTranslation, (float) (this.mCollapsedSize - childHeight));
    }

    private int getMaxAllowedChildHeight(View child, AmbientState ambientState) {
        if (child instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) child;
            if ((ambientState != null || !row.isHeadsUp()) && (ambientState == null || ambientState.getTopHeadsUpEntry() != child)) {
                return row.getIntrinsicHeight();
            }
            return this.mCollapsedSize + (row.getIntrinsicHeight() - row.getHeadsUpHeight());
        } else if (child instanceof ExpandableView) {
            return ((ExpandableView) child).getIntrinsicHeight();
        } else {
            return child == null ? this.mCollapsedSize : child.getHeight();
        }
    }

    private void updateStateForChildTransitioningInBottom(StackScrollAlgorithmState algorithmState, float transitioningPositionStart, float bottomPeakStart, float currentYPosition, StackViewState childViewState, int childHeight) {
        algorithmState.partialInBottom = 1.0f - ((transitioningPositionStart - currentYPosition) / ((float) (this.mPaddingBetweenElements + childHeight)));
        float offset = this.mBottomStackIndentationFunctor.getValue(algorithmState.partialInBottom);
        algorithmState.itemsInBottomStack += algorithmState.partialInBottom;
        int newHeight = childHeight;
        if (childHeight > this.mCollapsedSize && this.mIsSmallScreen) {
            newHeight = (int) Math.max(Math.min(((transitioningPositionStart + offset) - ((float) this.mPaddingBetweenElements)) - currentYPosition, (float) childHeight), (float) this.mCollapsedSize);
            childViewState.height = newHeight;
        }
        childViewState.yTranslation = ((transitioningPositionStart + offset) - ((float) newHeight)) - ((float) this.mPaddingBetweenElements);
        clampPositionToTopStackEnd(childViewState, newHeight);
        childViewState.location = 8;
    }

    private void updateStateForChildFullyInBottomStack(StackScrollAlgorithmState algorithmState, float transitioningPositionStart, StackViewState childViewState, int childHeight, AmbientState ambientState) {
        float currentYPosition;
        algorithmState.itemsInBottomStack += 1.0f;
        if (algorithmState.itemsInBottomStack < 3.0f) {
            currentYPosition = (this.mBottomStackIndentationFunctor.getValue(algorithmState.itemsInBottomStack) + transitioningPositionStart) - ((float) this.mPaddingBetweenElements);
            childViewState.location = 16;
        } else {
            if (algorithmState.itemsInBottomStack > 5.0f) {
                childViewState.alpha = 0.0f;
            } else if (algorithmState.itemsInBottomStack > 4.0f) {
                childViewState.alpha = 1.0f - algorithmState.partialInBottom;
            }
            childViewState.location = 32;
            currentYPosition = (float) ambientState.getInnerHeight();
        }
        childViewState.yTranslation = currentYPosition - ((float) childHeight);
        clampPositionToTopStackEnd(childViewState, childHeight);
    }

    private void updateStateForTopStackChild(StackScrollAlgorithmState algorithmState, int numberOfElementsCompletelyIn, int i, int childHeight, StackViewState childViewState, float scrollOffset) {
        int paddedIndex = (i - 1) - Math.max(numberOfElementsCompletelyIn - 3, 0);
        if (paddedIndex >= 0) {
            float distanceToStack = ((float) (this.mPaddingBetweenElements + childHeight)) - algorithmState.scrolledPixelsTop;
            if (i != algorithmState.lastTopStackIndex || distanceToStack <= ((float) (this.mTopStackTotalSize + this.mPaddingBetweenElements))) {
                float numItemsBefore;
                if (i == algorithmState.lastTopStackIndex) {
                    numItemsBefore = 1.0f - (distanceToStack / ((float) (this.mTopStackTotalSize + this.mPaddingBetweenElements)));
                } else {
                    numItemsBefore = algorithmState.itemsInTopStack - ((float) i);
                }
                childViewState.yTranslation = (((float) (this.mCollapsedSize + this.mTopStackTotalSize)) - this.mTopStackIndentationFunctor.getValue(numItemsBefore)) - ((float) childHeight);
            } else {
                childViewState.yTranslation = scrollOffset;
            }
            childViewState.location = 4;
            return;
        }
        if (paddedIndex == -1) {
            childViewState.alpha = 1.0f - algorithmState.partialInTop;
        } else {
            childViewState.alpha = 0.0f;
        }
        childViewState.yTranslation = (float) (this.mCollapsedSize - childHeight);
        childViewState.location = 2;
    }

    private void findNumberOfItemsInTopStackAndUpdateState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        float yPositionInScrollView = 0.0f;
        int childCount = algorithmState.visibleChildren.size();
        int i = 0;
        while (i < childCount) {
            ExpandableView child = (ExpandableView) algorithmState.visibleChildren.get(i);
            StackViewState childViewState = resultState.getViewStateForView(child);
            int childHeight = getMaxAllowedChildHeight(child, ambientState);
            float yPositionInScrollViewAfterElement = (((float) childHeight) + yPositionInScrollView) + ((float) this.mPaddingBetweenElements);
            if (yPositionInScrollView < ((float) algorithmState.scrollY)) {
                if (i == 0 && algorithmState.scrollY <= this.mCollapsedSize) {
                    int bottomPeekStart = (ambientState.getInnerHeight() - this.mBottomStackPeekSize) - this.mCollapseSecondCardPadding;
                    if (this.mIsExpansionChanging && child == this.mFirstChildWhileExpanding) {
                        childHeight = this.mFirstChildMaxHeight;
                    }
                    childViewState.height = (int) Math.max(Math.min((float) bottomPeekStart, (float) childHeight), (float) this.mCollapsedSize);
                    algorithmState.itemsInTopStack = 1.0f;
                } else if (yPositionInScrollViewAfterElement < ((float) algorithmState.scrollY)) {
                    algorithmState.itemsInTopStack += 1.0f;
                    if (i == 0) {
                        childViewState.height = this.mCollapsedSize;
                    }
                } else {
                    algorithmState.scrolledPixelsTop = ((float) algorithmState.scrollY) - yPositionInScrollView;
                    algorithmState.partialInTop = algorithmState.scrolledPixelsTop / ((float) (this.mPaddingBetweenElements + childHeight));
                    algorithmState.partialInTop = Math.max(0.0f, algorithmState.partialInTop);
                    algorithmState.itemsInTopStack += algorithmState.partialInTop;
                    if (i == 0) {
                        float newSize = Math.max((float) this.mCollapsedSize, ((yPositionInScrollViewAfterElement - ((float) this.mPaddingBetweenElements)) - ((float) algorithmState.scrollY)) + ((float) this.mCollapsedSize));
                        algorithmState.itemsInTopStack = 1.0f;
                        childViewState.height = (int) newSize;
                    }
                    algorithmState.lastTopStackIndex = i;
                    return;
                }
                yPositionInScrollView = yPositionInScrollViewAfterElement;
                i++;
            } else {
                algorithmState.lastTopStackIndex = i - 1;
                return;
            }
        }
    }

    private void updateZValuesForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            StackViewState childViewState = resultState.getViewStateForView((View) algorithmState.visibleChildren.get(i));
            if (((float) i) < algorithmState.itemsInTopStack) {
                float f;
                float stackIndex = algorithmState.itemsInTopStack - ((float) i);
                if (i == 0) {
                    f = 2.5f;
                } else {
                    f = 2.0f;
                }
                stackIndex = Math.min(stackIndex, 3.0f + f);
                if (i == 0 && algorithmState.itemsInTopStack < 2.0f) {
                    stackIndex -= 1.0f;
                    if (algorithmState.scrollY > this.mCollapsedSize) {
                        stackIndex = 0.1f + (1.9f * stackIndex);
                    }
                }
                childViewState.zTranslation = ((float) this.mZBasicHeight) + (((float) this.mZDistanceBetweenElements) * stackIndex);
            } else if (((float) i) > ((float) (childCount - 1)) - algorithmState.itemsInBottomStack) {
                childViewState.zTranslation = ((float) this.mZBasicHeight) - (((float) this.mZDistanceBetweenElements) * (((float) i) - (((float) (childCount - 1)) - algorithmState.itemsInBottomStack)));
            } else {
                childViewState.zTranslation = (float) this.mZBasicHeight;
            }
        }
    }

    public void updateIsSmallScreen(int panelHeight) {
        this.mIsSmallScreen = panelHeight < ((this.mCollapsedSize + this.mBottomStackSlowDownLength) + this.mBottomStackPeekSize) + this.mMaxNotificationHeight;
    }

    public void onExpansionStarted(StackScrollState currentState) {
        this.mIsExpansionChanging = true;
        this.mExpandedOnStart = this.mIsExpanded;
        updateFirstChildHeightWhileExpanding(currentState.getHostView());
    }

    private void updateFirstChildHeightWhileExpanding(ViewGroup hostView) {
        this.mFirstChildWhileExpanding = (ExpandableView) findFirstVisibleChild(hostView);
        if (this.mFirstChildWhileExpanding == null) {
            this.mFirstChildMaxHeight = 0;
        } else if (this.mExpandedOnStart) {
            this.mFirstChildMaxHeight = StackStateAnimator.getFinalActualHeight(this.mFirstChildWhileExpanding);
            if (this.mFirstChildWhileExpanding instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = this.mFirstChildWhileExpanding;
                if (row.isHeadsUp()) {
                    this.mFirstChildMaxHeight += this.mCollapsedSize - row.getHeadsUpHeight();
                }
            }
        } else {
            updateFirstChildMaxSizeToMaxHeight();
        }
    }

    private void updateFirstChildMaxSizeToMaxHeight() {
        if (isMaxSizeInitialized(this.mFirstChildWhileExpanding)) {
            this.mFirstChildMaxHeight = getMaxAllowedChildHeight(this.mFirstChildWhileExpanding, null);
        } else {
            this.mFirstChildWhileExpanding.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (StackScrollAlgorithm.this.mFirstChildWhileExpanding != null) {
                        StackScrollAlgorithm.this.mFirstChildMaxHeight = StackScrollAlgorithm.this.getMaxAllowedChildHeight(StackScrollAlgorithm.this.mFirstChildWhileExpanding, null);
                    } else {
                        StackScrollAlgorithm.this.mFirstChildMaxHeight = 0;
                    }
                    v.removeOnLayoutChangeListener(this);
                }
            });
        }
    }

    private boolean isMaxSizeInitialized(ExpandableView child) {
        boolean z = true;
        if (child instanceof ExpandableNotificationRow) {
            return ((ExpandableNotificationRow) child).isMaxExpandHeightInitialized();
        }
        if (child != null && child.getWidth() == 0) {
            z = false;
        }
        return z;
    }

    private View findFirstVisibleChild(ViewGroup container) {
        int childCount = container.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }

    public void onExpansionStopped() {
        this.mIsExpansionChanging = false;
        this.mFirstChildWhileExpanding = null;
    }

    public void setIsExpanded(boolean isExpanded) {
        this.mIsExpanded = isExpanded;
    }

    public void notifyChildrenChanged(final ViewGroup hostView) {
        if (this.mIsExpansionChanging) {
            hostView.post(new Runnable() {
                public void run() {
                    StackScrollAlgorithm.this.updateFirstChildHeightWhileExpanding(hostView);
                }
            });
        }
    }

    public void setDimmed(boolean dimmed) {
        updatePadding(dimmed);
    }

    public void onReset(ExpandableView view) {
        if (view.equals(this.mFirstChildWhileExpanding)) {
            updateFirstChildMaxSizeToMaxHeight();
        }
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }
}
