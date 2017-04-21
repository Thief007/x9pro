package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.systemui.R;

public class DismissView extends StackScrollerDecorView {
    private boolean mDismissAllInProgress;
    private DismissViewButton mDismissButton;

    public DismissView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected View findContentView() {
        return findViewById(R.id.dismiss_text);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDismissButton = (DismissViewButton) findContentView();
    }

    public void setOnButtonClickListener(OnClickListener listener) {
        this.mContent.setOnClickListener(listener);
    }

    public boolean isOnEmptySpace(float touchX, float touchY) {
        if (touchX < this.mContent.getX() || touchX > this.mContent.getX() + ((float) this.mContent.getWidth()) || touchY < this.mContent.getY() || touchY > this.mContent.getY() + ((float) this.mContent.getHeight())) {
            return true;
        }
        return false;
    }

    public void showClearButton() {
        this.mDismissButton.showButton();
    }

    public void setDismissAllInProgress(boolean dismissAllInProgress) {
        if (dismissAllInProgress) {
            setClipBounds(null);
        }
        this.mDismissAllInProgress = dismissAllInProgress;
    }

    public void setClipBounds(Rect clipBounds) {
        if (!this.mDismissAllInProgress) {
            super.setClipBounds(clipBounds);
        }
    }

    public boolean isButtonVisible() {
        return this.mDismissButton.isButtonStatic();
    }
}
