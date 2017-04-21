package com.android.systemui.statusbar.stack;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.SpeedBumpView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackScrollState {
    private final int mClearAllTopPadding;
    private final ViewGroup mHostView;
    private Map<ExpandableView, StackViewState> mStateMap = new HashMap();

    public StackScrollState(ViewGroup hostView) {
        this.mHostView = hostView;
        this.mClearAllTopPadding = hostView.getContext().getResources().getDimensionPixelSize(R.dimen.clear_all_padding_top);
    }

    public ViewGroup getHostView() {
        return this.mHostView;
    }

    public void resetViewStates() {
        int numChildren = this.mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            ExpandableView child = (ExpandableView) this.mHostView.getChildAt(i);
            resetViewState(child);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                List<ExpandableNotificationRow> children = row.getNotificationChildren();
                if (row.areChildrenExpanded() && children != null) {
                    for (ExpandableNotificationRow childRow : children) {
                        resetViewState(childRow);
                    }
                }
            }
        }
    }

    private void resetViewState(ExpandableView view) {
        StackViewState viewState = (StackViewState) this.mStateMap.get(view);
        if (viewState == null) {
            viewState = new StackViewState();
            this.mStateMap.put(view, viewState);
        }
        viewState.height = view.getIntrinsicHeight();
        viewState.gone = view.getVisibility() == 8;
        viewState.alpha = 1.0f;
        viewState.notGoneIndex = -1;
    }

    public StackViewState getViewStateForView(View requestedView) {
        return (StackViewState) this.mStateMap.get(requestedView);
    }

    public void removeViewStateForView(View child) {
        this.mStateMap.remove(child);
    }

    public void apply() {
        int numChildren = this.mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            ExpandableView child = (ExpandableView) this.mHostView.getChildAt(i);
            StackViewState state = (StackViewState) this.mStateMap.get(child);
            if (applyState(child, state)) {
                if (child instanceof SpeedBumpView) {
                    performSpeedBumpAnimation(i, (SpeedBumpView) child, state, 0);
                } else if (child instanceof DismissView) {
                    DismissView dismissView = (DismissView) child;
                    if (!(state.topOverLap < this.mClearAllTopPadding) || dismissView.willBeGone()) {
                        r0 = false;
                    } else {
                        r0 = true;
                    }
                    dismissView.performVisibilityAnimation(r0);
                } else if (child instanceof EmptyShadeView) {
                    EmptyShadeView emptyShadeView = (EmptyShadeView) child;
                    if (!(state.topOverLap <= 0) || emptyShadeView.willBeGone()) {
                        r0 = false;
                    } else {
                        r0 = true;
                    }
                    emptyShadeView.performVisibilityAnimation(r0);
                }
            }
        }
    }

    public boolean applyState(ExpandableView view, StackViewState state) {
        if (state == null) {
            Log.wtf("StackScrollStateNoSuchChild", "No child state was found when applying this state to the hostView");
            return false;
        } else if (state.gone) {
            return false;
        } else {
            applyViewState(view, state);
            int height = view.getActualHeight();
            int newHeight = state.height;
            if (height != newHeight) {
                view.setActualHeight(newHeight, false);
            }
            view.setDimmed(state.dimmed, false);
            view.setHideSensitive(state.hideSensitive, false, 0, 0);
            view.setBelowSpeedBump(state.belowSpeedBump);
            view.setDark(state.dark, false, 0);
            if (((float) view.getClipTopAmount()) != ((float) state.clipTopAmount)) {
                view.setClipTopAmount(state.clipTopAmount);
            }
            if (((float) view.getClipTopOptimization()) != ((float) state.topOverLap)) {
                view.setClipTopOptimization(state.topOverLap);
            }
            if (view instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) view).applyChildrenState(this);
            }
            return true;
        }
    }

    public void applyViewState(View view, ViewState state) {
        float alpha = view.getAlpha();
        float yTranslation = view.getTranslationY();
        float xTranslation = view.getTranslationX();
        float zTranslation = view.getTranslationZ();
        float scale = view.getScaleX();
        float newAlpha = state.alpha;
        float newYTranslation = state.yTranslation;
        float newZTranslation = state.zTranslation;
        float newScale = state.scale;
        boolean becomesInvisible = newAlpha == 0.0f;
        if (alpha != newAlpha && xTranslation == 0.0f) {
            boolean z;
            int newLayerType;
            boolean becomesFullyVisible = newAlpha == 1.0f;
            if (becomesInvisible || becomesFullyVisible) {
                z = false;
            } else {
                z = view.hasOverlappingRendering();
            }
            int layerType = view.getLayerType();
            if (z) {
                newLayerType = 2;
            } else {
                newLayerType = 0;
            }
            if (layerType != newLayerType) {
                view.setLayerType(newLayerType, null);
            }
            view.setAlpha(newAlpha);
        }
        int oldVisibility = view.getVisibility();
        int newVisibility = becomesInvisible ? 4 : 0;
        if (!(newVisibility == oldVisibility || ((view instanceof ExpandableView) && ((ExpandableView) view).willBeGone()))) {
            view.setVisibility(newVisibility);
        }
        if (yTranslation != newYTranslation) {
            view.setTranslationY(newYTranslation);
        }
        if (zTranslation != newZTranslation) {
            view.setTranslationZ(newZTranslation);
        }
        if (scale != newScale) {
            view.setScaleX(newScale);
            view.setScaleY(newScale);
        }
    }

    public void performSpeedBumpAnimation(int i, SpeedBumpView speedBump, StackViewState state, long delay) {
        View nextChild = getNextChildNotGone(i);
        if (nextChild != null) {
            speedBump.animateDivider(getViewStateForView(nextChild).yTranslation > state.yTranslation + ((float) (state.height / 2)), delay, null);
        }
    }

    private View getNextChildNotGone(int childIndex) {
        int childCount = this.mHostView.getChildCount();
        for (int i = childIndex + 1; i < childCount; i++) {
            View child = this.mHostView.getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }
}
