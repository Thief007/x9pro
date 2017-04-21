package com.android.systemui.statusbar.policy;

import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout.LayoutParams;
import com.android.systemui.R;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;

public class BrightnessMirrorController {
    public long TRANSITION_DURATION_IN = 200;
    public long TRANSITION_DURATION_OUT = 150;
    private final View mBrightnessMirror;
    private final int[] mInt2Cache = new int[2];
    private final View mPanelHolder;
    private final ScrimView mScrimBehind;

    public BrightnessMirrorController(StatusBarWindowView statusBarWindow) {
        this.mScrimBehind = (ScrimView) statusBarWindow.findViewById(R.id.scrim_behind);
        this.mBrightnessMirror = statusBarWindow.findViewById(R.id.brightness_mirror);
        this.mPanelHolder = statusBarWindow.findViewById(R.id.panel_holder);
    }

    public void showMirror() {
        this.mBrightnessMirror.setVisibility(0);
        this.mScrimBehind.animateViewAlpha(0.0f, this.TRANSITION_DURATION_OUT, PhoneStatusBar.ALPHA_OUT);
        outAnimation(this.mPanelHolder.animate()).withLayer();
    }

    public void hideMirror() {
        this.mScrimBehind.animateViewAlpha(1.0f, this.TRANSITION_DURATION_IN, PhoneStatusBar.ALPHA_IN);
        inAnimation(this.mPanelHolder.animate()).withLayer().withEndAction(new Runnable() {
            public void run() {
                BrightnessMirrorController.this.mBrightnessMirror.setVisibility(4);
            }
        });
    }

    private ViewPropertyAnimator outAnimation(ViewPropertyAnimator a) {
        return a.alpha(0.0f).setDuration(this.TRANSITION_DURATION_OUT).setInterpolator(PhoneStatusBar.ALPHA_OUT);
    }

    private ViewPropertyAnimator inAnimation(ViewPropertyAnimator a) {
        return a.alpha(1.0f).setDuration(this.TRANSITION_DURATION_IN).setInterpolator(PhoneStatusBar.ALPHA_IN);
    }

    public void setLocation(View original) {
        original.getLocationInWindow(this.mInt2Cache);
        int originalX = this.mInt2Cache[0] + (original.getWidth() / 2);
        int originalY = this.mInt2Cache[1];
        this.mBrightnessMirror.setTranslationX(0.0f);
        this.mBrightnessMirror.setTranslationY(0.0f);
        this.mBrightnessMirror.getLocationInWindow(this.mInt2Cache);
        int mirrorX = this.mInt2Cache[0] + (this.mBrightnessMirror.getWidth() / 2);
        int mirrorY = this.mInt2Cache[1];
        this.mBrightnessMirror.setTranslationX((float) (originalX - mirrorX));
        this.mBrightnessMirror.setTranslationY((float) (originalY - mirrorY));
    }

    public View getMirror() {
        return this.mBrightnessMirror;
    }

    public void updateResources() {
        LayoutParams lp = (LayoutParams) this.mBrightnessMirror.getLayoutParams();
        lp.width = this.mBrightnessMirror.getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        lp.gravity = this.mBrightnessMirror.getResources().getInteger(R.integer.notification_panel_layout_gravity);
        this.mBrightnessMirror.setLayoutParams(lp);
        int padding = this.mBrightnessMirror.getResources().getDimensionPixelSize(R.dimen.notification_side_padding);
        this.mBrightnessMirror.setPadding(padding, this.mBrightnessMirror.getPaddingTop(), padding, this.mBrightnessMirror.getPaddingBottom());
    }
}
