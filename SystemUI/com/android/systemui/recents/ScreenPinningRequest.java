package com.android.systemui.recents;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.ArrayList;

public class ScreenPinningRequest implements OnClickListener {
    private final AccessibilityManager mAccessibilityService = ((AccessibilityManager) this.mContext.getSystemService("accessibility"));
    private final Context mContext;
    private RequestWindowView mRequestWindow;
    private final WindowManager mWindowManager = ((WindowManager) this.mContext.getSystemService("window"));
    private IWindowManager mWindowManagerService = WindowManagerGlobal.getWindowManagerService();

    private class RequestWindowView extends FrameLayout {
        private final ColorDrawable mColor = new ColorDrawable(0);
        private ValueAnimator mColorAnim;
        private ViewGroup mLayout;
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    RequestWindowView.this.post(RequestWindowView.this.mUpdateLayoutRunnable);
                } else if (intent.getAction().equals("android.intent.action.USER_SWITCHED") || intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                    ScreenPinningRequest.this.clearPrompt();
                }
            }
        };
        private boolean mShowCancel;
        private final Runnable mUpdateLayoutRunnable = new Runnable() {
            public void run() {
                if (RequestWindowView.this.mLayout != null && RequestWindowView.this.mLayout.getParent() != null) {
                    RequestWindowView.this.mLayout.setLayoutParams(ScreenPinningRequest.this.getRequestLayoutParams(RequestWindowView.this.isLandscapePhone(RequestWindowView.this.mContext)));
                }
            }
        };

        public RequestWindowView(Context context, boolean showCancel) {
            super(context);
            setClickable(true);
            setOnClickListener(ScreenPinningRequest.this);
            setBackground(this.mColor);
            this.mShowCancel = showCancel;
        }

        public void onAttachedToWindow() {
            DisplayMetrics metrics = new DisplayMetrics();
            ScreenPinningRequest.this.mWindowManager.getDefaultDisplay().getMetrics(metrics);
            float density = metrics.density;
            boolean isLandscape = isLandscapePhone(this.mContext);
            inflateView(isLandscape);
            int bgColor = this.mContext.getColor(R.color.screen_pinning_request_window_bg);
            if (ActivityManager.isHighEndGfx()) {
                this.mLayout.setAlpha(0.0f);
                if (isLandscape) {
                    this.mLayout.setTranslationX(96.0f * density);
                } else {
                    this.mLayout.setTranslationY(96.0f * density);
                }
                this.mLayout.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                this.mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{Integer.valueOf(0), Integer.valueOf(bgColor)});
                this.mColorAnim.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        RequestWindowView.this.mColor.setColor(((Integer) animation.getAnimatedValue()).intValue());
                    }
                });
                this.mColorAnim.setDuration(1000);
                this.mColorAnim.start();
            } else {
                this.mColor.setColor(bgColor);
            }
            IntentFilter filter = new IntentFilter("android.intent.action.CONFIGURATION_CHANGED");
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.intent.action.SCREEN_OFF");
            this.mContext.registerReceiver(this.mReceiver, filter);
        }

        private boolean isLandscapePhone(Context context) {
            Configuration config = this.mContext.getResources().getConfiguration();
            if (config.orientation != 2 || config.smallestScreenWidthDp >= 600) {
                return false;
            }
            return true;
        }

        private void inflateView(boolean isLandscape) {
            int description;
            this.mLayout = (ViewGroup) View.inflate(getContext(), isLandscape ? R.layout.screen_pinning_request_land_phone : R.layout.screen_pinning_request, null);
            this.mLayout.setClickable(true);
            this.mLayout.setLayoutDirection(0);
            View buttons = this.mLayout.findViewById(R.id.screen_pinning_buttons);
            buttons.setLayoutDirection(3);
            this.mLayout.findViewById(R.id.screen_pinning_text_area).setLayoutDirection(3);
            swapChildrenIfRtlAndVertical(buttons);
            ((Button) this.mLayout.findViewById(R.id.screen_pinning_ok_button)).setOnClickListener(ScreenPinningRequest.this);
            if (this.mShowCancel) {
                ((Button) this.mLayout.findViewById(R.id.screen_pinning_cancel_button)).setOnClickListener(ScreenPinningRequest.this);
            } else {
                ((Button) this.mLayout.findViewById(R.id.screen_pinning_cancel_button)).setVisibility(4);
            }
            if (ScreenPinningRequest.this.mAccessibilityService.isEnabled()) {
                description = R.string.screen_pinning_description_accessible;
            } else {
                description = R.string.screen_pinning_description;
            }
            try {
                if (!ScreenPinningRequest.this.mWindowManagerService.hasNavigationBar()) {
                    Log.d("ScreenPinningRequest", "No navigationbar, show unpin description as physical key");
                    description = R.string.screen_pinning_description_physical_key;
                }
            } catch (RemoteException e) {
                Log.e("ScreenPinningRequest", "RemoteException occured");
            }
            ((TextView) this.mLayout.findViewById(R.id.screen_pinning_description)).setText(description);
            int backBgVisibility = ScreenPinningRequest.this.mAccessibilityService.isEnabled() ? 4 : 0;
            this.mLayout.findViewById(R.id.screen_pinning_back_bg).setVisibility(backBgVisibility);
            this.mLayout.findViewById(R.id.screen_pinning_back_bg_light).setVisibility(backBgVisibility);
            addView(this.mLayout, ScreenPinningRequest.this.getRequestLayoutParams(isLandscape));
        }

        private void swapChildrenIfRtlAndVertical(View group) {
            if (this.mContext.getResources().getConfiguration().getLayoutDirection() == 1) {
                LinearLayout linearLayout = (LinearLayout) group;
                if (linearLayout.getOrientation() == 1) {
                    int i;
                    int childCount = linearLayout.getChildCount();
                    ArrayList<View> childList = new ArrayList(childCount);
                    for (i = 0; i < childCount; i++) {
                        childList.add(linearLayout.getChildAt(i));
                    }
                    linearLayout.removeAllViews();
                    for (i = childCount - 1; i >= 0; i--) {
                        linearLayout.addView((View) childList.get(i));
                    }
                }
            }
        }

        public void onDetachedFromWindow() {
            this.mContext.unregisterReceiver(this.mReceiver);
        }

        protected void onConfigurationChanged() {
            removeAllViews();
            inflateView(isLandscapePhone(this.mContext));
        }
    }

    public ScreenPinningRequest(Context context) {
        this.mContext = context;
    }

    public void clearPrompt() {
        if (this.mRequestWindow != null) {
            this.mWindowManager.removeView(this.mRequestWindow);
            this.mRequestWindow = null;
        }
    }

    public void showPrompt(boolean allowCancel) {
        clearPrompt();
        this.mRequestWindow = new RequestWindowView(this.mContext, allowCancel);
        this.mRequestWindow.setSystemUiVisibility(256);
        this.mWindowManager.addView(this.mRequestWindow, getWindowLayoutParams());
    }

    public void onConfigurationChanged() {
        if (this.mRequestWindow != null) {
            this.mRequestWindow.onConfigurationChanged();
        }
    }

    private LayoutParams getWindowLayoutParams() {
        LayoutParams lp = new LayoutParams(-1, -1, 2024, 16777480, -3);
        lp.privateFlags |= 16;
        lp.setTitle("ScreenPinningConfirmation");
        lp.gravity = 119;
        return lp;
    }

    public void onClick(View v) {
        if (v.getId() == R.id.screen_pinning_ok_button || this.mRequestWindow == v) {
            try {
                ActivityManagerNative.getDefault().startLockTaskModeOnCurrent();
            } catch (RemoteException e) {
            }
        }
        clearPrompt();
    }

    public FrameLayout.LayoutParams getRequestLayoutParams(boolean isLandscape) {
        int i;
        if (isLandscape) {
            i = 21;
        } else {
            i = 81;
        }
        return new FrameLayout.LayoutParams(-2, -2, i);
    }
}
