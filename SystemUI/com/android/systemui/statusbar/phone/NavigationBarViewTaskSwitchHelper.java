package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

public class NavigationBarViewTaskSwitchHelper extends SimpleOnGestureListener {
    private BaseStatusBar mBar;
    private boolean mIsRTL;
    private boolean mIsVertical;
    private final int mMinFlingVelocity;
    private final int mScrollTouchSlop;
    private final GestureDetector mTaskSwitcherDetector;
    private int mTouchDownX;
    private int mTouchDownY;

    public NavigationBarViewTaskSwitchHelper(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mScrollTouchSlop = context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_min_swipe_distance);
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mTaskSwitcherDetector = new GestureDetector(context, this);
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        this.mBar = phoneStatusBar;
    }

    public void setBarState(boolean isVertical, boolean isRTL) {
        this.mIsVertical = isVertical;
        this.mIsRTL = isRTL;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        this.mTaskSwitcherDetector.onTouchEvent(event);
        switch (event.getAction() & 255) {
            case 0:
                this.mTouchDownX = (int) event.getX();
                this.mTouchDownY = (int) event.getY();
                break;
            case 2:
                int y = (int) event.getY();
                int xDiff = Math.abs(((int) event.getX()) - this.mTouchDownX);
                int yDiff = Math.abs(y - this.mTouchDownY);
                boolean exceededTouchSlop = !this.mIsVertical ? xDiff > this.mScrollTouchSlop && xDiff > yDiff : yDiff > this.mScrollTouchSlop && yDiff > xDiff;
                if (exceededTouchSlop) {
                    return true;
                }
                break;
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return this.mTaskSwitcherDetector.onTouchEvent(event);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean isValidFling;
        float absVelX = Math.abs(velocityX);
        float absVelY = Math.abs(velocityY);
        boolean showNext;
        if (absVelX <= ((float) this.mMinFlingVelocity) || !this.mIsVertical) {
            if (absVelX > absVelY) {
            }
            isValidFling = false;
            if (isValidFling) {
                if (this.mIsRTL) {
                    if (this.mIsVertical) {
                        if (velocityX < 0.0f) {
                        }
                        showNext = false;
                    }
                    showNext = true;
                } else {
                    if (this.mIsVertical) {
                        if (velocityX > 0.0f) {
                        }
                        showNext = false;
                    }
                    showNext = true;
                }
                if (showNext) {
                    this.mBar.showPreviousAffiliatedTask();
                } else {
                    this.mBar.showNextAffiliatedTask();
                }
            }
            return true;
        }
        isValidFling = true;
        if (isValidFling) {
            if (this.mIsRTL) {
                if (this.mIsVertical) {
                    if (velocityX > 0.0f) {
                    }
                    showNext = false;
                }
                showNext = true;
            } else {
                if (this.mIsVertical) {
                    if (velocityX < 0.0f) {
                    }
                    showNext = false;
                }
                showNext = true;
            }
            if (showNext) {
                this.mBar.showPreviousAffiliatedTask();
            } else {
                this.mBar.showNextAffiliatedTask();
            }
        }
        return true;
    }
}
