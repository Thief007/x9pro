package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.mediatek.common.MPlugin;
import com.mediatek.multiwindow.MultiWindowProxy;
import com.mediatek.systemui.ext.DefaultNavigationBarPlugin;
import com.mediatek.systemui.ext.INavigationBarPlugin;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class NavigationBarView extends LinearLayout {
    private boolean currentFlag;
    private Drawable mBackAltIcon;
    private Drawable mBackAltLandIcon;
    private Drawable mBackIcon;
    private Drawable mBackLandIcon;
    private BaseStatusBar mBar;
    int mBarSize;
    private final NavigationBarTransitions mBarTransitions;
    View mCurrentView = null;
    private DeadZone mDeadZone;
    int mDisabledFlags = 0;
    final Display mDisplay;
    private BroadcastReceiver mFloatWindowBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int i = 0;
            String action = intent.getAction();
            Log.d("PhoneStatusBar/NavigationBarView", "action = " + action);
            boolean showFloatWindow = intent.getBooleanExtra("ShowFloatWindow", false);
            int passwordType = 1;
            try {
                passwordType = NavigationBarView.this.getMountService().getPasswordType();
            } catch (Exception e) {
                Log.e("PhoneStatusBar/NavigationBarView", "Error calling mount service " + e);
            }
            if (action.equals("android.intent.action.USER_SWITCHED")) {
                if (intent.getIntExtra("android.intent.extra.user_handle", -1) != 0) {
                    showFloatWindow = false;
                } else if (1 != passwordType) {
                    Log.d("PhoneStatusBar/NavigationBarView", "Have passwordType,Not to show FloatButton");
                    NavigationBarView.this.getFloatButton().setVisibility(4);
                    return;
                } else {
                    Log.d("PhoneStatusBar/NavigationBarView", "No passwordType");
                    showFloatWindow = true;
                }
            }
            NavigationBarView.this.mShowFloatWindow = showFloatWindow;
            Log.d("PhoneStatusBar/NavigationBarView", "mFloatWindowBroadcastReceiver showFloatWindow is " + showFloatWindow);
            ImageView floatButton = NavigationBarView.this.getFloatButton();
            if (!showFloatWindow) {
                i = 4;
            }
            floatButton.setVisibility(i);
        }
    };
    private H mHandler = new H();
    private final OnClickListener mImeSwitcherClickListener = new OnClickListener() {
        public void onClick(View view) {
            ((InputMethodManager) NavigationBarView.this.mContext.getSystemService("input_method")).showInputMethodPicker(true);
        }
    };
    private boolean mIsLayoutRtl;
    private boolean mIsSplitModeEnable = MultiWindowProxy.isSplitModeEnabled();
    private boolean mLayoutTransitionsEnabled;
    private INavigationBarPlugin mNavBarPlugin;
    int mNavigationIconHints = 0;
    private OnVerticalChangedListener mOnVerticalChangedListener;
    private Drawable mRecentIcon;
    private Drawable mRecentLandIcon;
    private View mRestoreButton;
    View[] mRotatedViews = new View[4];
    boolean mScreenOn;
    private boolean mShowFloatWindow = true;
    boolean mShowMenu;
    private boolean mShowRestoreButton;
    private NavigationBarViewTaskSwitchHelper mTaskSwitchHelper;
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();
    boolean mVertical;

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message m) {
            boolean z = true;
            int i = 0;
            switch (m.what) {
                case 1025:
                    boolean flag = m.getData().getBoolean("flag");
                    NavigationBarView.this.currentFlag = flag;
                    if (NavigationBarView.this.mRestoreButton != null) {
                        NavigationBarView navigationBarView = NavigationBarView.this;
                        if (!flag || !NavigationBarView.this.mShowFloatWindow) {
                            z = false;
                        } else if (NavigationBarView.this.mBar.isFloatPanelOpened()) {
                            z = false;
                        }
                        navigationBarView.mShowRestoreButton = z;
                        View -get2 = NavigationBarView.this.mRestoreButton;
                        if (!NavigationBarView.this.mShowRestoreButton) {
                            i = 4;
                        }
                        -get2.setVisibility(i);
                        return;
                    }
                    return;
                case 8686:
                    String how = "" + m.obj;
                    int w = NavigationBarView.this.getWidth();
                    int h = NavigationBarView.this.getHeight();
                    int vw = NavigationBarView.this.mCurrentView.getWidth();
                    if (h != NavigationBarView.this.mCurrentView.getHeight() || w != vw) {
                        Log.w("PhoneStatusBar/NavigationBarView", String.format("*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)", new Object[]{how, Integer.valueOf(w), Integer.valueOf(h), Integer.valueOf(vw), Integer.valueOf(vh)}));
                        NavigationBarView.this.requestLayout();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private long mDuration;
        private boolean mHomeAppearing;
        private TimeInterpolator mInterpolator;
        private long mStartDelay;

        private NavTransitionListener() {
        }

        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = true;
            } else if (view.getId() == R.id.home && transitionType == 2) {
                this.mHomeAppearing = true;
                this.mStartDelay = transition.getStartDelay(transitionType);
                this.mDuration = transition.getDuration(transitionType);
                this.mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == 2) {
                this.mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            if (!this.mBackTransitioning && NavigationBarView.this.getBackButton().getVisibility() == 0 && this.mHomeAppearing && NavigationBarView.this.getHomeButton().getAlpha() == 0.0f) {
                NavigationBarView.this.getBackButton().setAlpha(0.0f);
                ValueAnimator a = ObjectAnimator.ofFloat(NavigationBarView.this.getBackButton(), "alpha", new float[]{0.0f, 1.0f});
                a.setStartDelay(this.mStartDelay);
                a.setDuration(this.mDuration);
                a.setInterpolator(this.mInterpolator);
                a.start();
            }
        }
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean z);
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Resources res = getContext().getResources();
        this.mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        this.mVertical = false;
        this.mShowMenu = false;
        if (MultiWindowProxy.isSupported()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.systemui.FLOATWINDOW_SHOW");
            filter.addAction("android.intent.action.USER_SWITCHED");
            context.registerReceiver(this.mFloatWindowBroadcastReceiver, filter);
        }
        this.mTaskSwitchHelper = new NavigationBarViewTaskSwitchHelper(context);
        try {
            this.mNavBarPlugin = (INavigationBarPlugin) MPlugin.createInstance(INavigationBarPlugin.class.getName(), context);
        } catch (Exception e) {
            Log.e("PhoneStatusBar/NavigationBarView", "Catch INavigationBarPlugin exception: ", e);
        }
        if (this.mNavBarPlugin == null) {
            Log.d("PhoneStatusBar/NavigationBarView", "DefaultNavigationBarPlugin");
            this.mNavBarPlugin = new DefaultNavigationBarPlugin(context);
        }
        getIcons(res);
        this.mBarTransitions = new NavigationBarTransitions(this);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewRootImpl root = getViewRootImpl();
        if (root != null) {
            root.setDrawDuringWindowsAnimating(true);
        }
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(PhoneStatusBar phoneStatusBar) {
        this.mTaskSwitchHelper.setBar(phoneStatusBar);
        if (MultiWindowProxy.isSupported()) {
            this.mBar = phoneStatusBar;
        }
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        this.mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(this.mVertical);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mTaskSwitchHelper.onTouchEvent(event)) {
            return true;
        }
        if (this.mDeadZone != null && event.getAction() == 4) {
            this.mDeadZone.poke(event);
        }
        return super.onTouchEvent(event);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mTaskSwitchHelper.onInterceptTouchEvent(event);
    }

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public View getRecentsButton() {
        ImageView view = (ImageView) this.mCurrentView.findViewById(R.id.recent_apps);
        view.setImageDrawable(this.mNavBarPlugin.getRecentImage(view.getDrawable()));
        return view;
    }

    public View getMenuButton() {
        return this.mCurrentView.findViewById(R.id.menu);
    }

    public View getBackButton() {
        ImageView view = (ImageView) this.mCurrentView.findViewById(R.id.back);
        view.setImageDrawable(this.mNavBarPlugin.getBackImage(view.getDrawable()));
        return view;
    }

    public KeyButtonView getHomeButton() {
        KeyButtonView view = (KeyButtonView) this.mCurrentView.findViewById(R.id.home);
        view.setImageDrawable(this.mNavBarPlugin.getHomeImage(view.getDrawable()));
        return view;
    }

    public View getImeSwitchButton() {
        return this.mCurrentView.findViewById(R.id.ime_switcher);
    }

    private void getIcons(Resources res) {
        this.mBackIcon = this.mNavBarPlugin.getBackImage(res.getDrawable(R.drawable.ic_sysbar_back));
        this.mBackLandIcon = this.mNavBarPlugin.getBackLandImage(res.getDrawable(R.drawable.ic_sysbar_back_land));
        this.mBackAltIcon = this.mNavBarPlugin.getBackImeImage(res.getDrawable(R.drawable.ic_sysbar_back_ime));
        this.mBackAltLandIcon = this.mNavBarPlugin.getBackImeImage(res.getDrawable(R.drawable.ic_sysbar_back_ime_land));
        this.mRecentIcon = this.mNavBarPlugin.getRecentImage(res.getDrawable(R.drawable.ic_sysbar_recent));
        this.mRecentLandIcon = this.mNavBarPlugin.getRecentLandImage(res.getDrawable(R.drawable.ic_sysbar_recent_land));
    }

    public void setLayoutDirection(int layoutDirection) {
        getIcons(getContext().getResources());
        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        this.mScreenOn = screenOn;
        setDisabledFlags(this.mDisabledFlags, true);
    }

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    public void setNavigationIconHints(int hints, boolean force) {
        if (force || hints != this.mNavigationIconHints) {
            boolean backAlt = (hints & 1) != 0;
            if (!((this.mNavigationIconHints & 1) == 0 || backAlt)) {
                this.mTransitionListener.onBackAltCleared();
            }
            this.mNavigationIconHints = hints;
            ImageView imageView = (ImageView) getBackButton();
            Drawable drawable = backAlt ? this.mVertical ? this.mBackAltLandIcon : this.mBackAltIcon : this.mVertical ? this.mBackLandIcon : this.mBackIcon;
            imageView.setImageDrawable(drawable);
            ((ImageView) getRecentsButton()).setImageDrawable(this.mVertical ? this.mRecentLandIcon : this.mRecentIcon);
            getImeSwitchButton().setVisibility((hints & 2) != 0 ? 0 : 4);
            setMenuVisibility(this.mShowMenu, true);
            setDisabledFlags(this.mDisabledFlags, true);
        }
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (force || this.mDisabledFlags != disabledFlags) {
            this.mDisabledFlags = disabledFlags;
            boolean z = (2097152 & disabledFlags) != 0;
            boolean z2 = (16777216 & disabledFlags) != 0;
            boolean back = (4194304 & disabledFlags) != 0 ? (this.mNavigationIconHints & 1) == 0 : false;
            boolean search = (33554432 & disabledFlags) != 0;
            if (MultiWindowProxy.isSupported()) {
                z = !z ? this.mBar.isFloatPanelOpened() : true;
                z2 = !z2 ? this.mBar.isFloatPanelOpened() : true;
                back = !back ? this.mBar.isFloatPanelOpened() : true;
                search = !search ? this.mBar.isFloatPanelOpened() : true;
            }
            boolean disableHome = z;
            boolean disableRecent = z2;
            boolean disableBack = back;
            boolean disableSearch = search;
            if (!(disableHome && disableRecent && disableBack)) {
                disableSearch = false;
            }
            setSlippery(disableSearch);
            ViewGroup navButtons = (ViewGroup) this.mCurrentView.findViewById(R.id.nav_buttons);
            if (navButtons != null) {
                LayoutTransition lt = navButtons.getLayoutTransition();
                if (!(lt == null || lt.getTransitionListeners().contains(this.mTransitionListener))) {
                    lt.addTransitionListener(this.mTransitionListener);
                }
            }
            if (inLockTask() && disableRecent && !disableHome) {
                disableRecent = false;
            }
            getBackButton().setVisibility(disableBack ? 4 : 0);
            getHomeButton().setVisibility(disableHome ? 4 : 0);
            getRecentsButton().setVisibility(disableRecent ? 4 : 0);
            if (MultiWindowProxy.isSupported()) {
                setDisabledFlagsforMultiW(disableRecent);
            }
        }
    }

    private boolean inLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setWakeAndUnlocking(boolean wakeAndUnlocking) {
        setUseFadingAnimations(wakeAndUnlocking);
        setLayoutTransitionsEnabled(!wakeAndUnlocking);
    }

    private void setLayoutTransitionsEnabled(boolean enabled) {
        this.mLayoutTransitionsEnabled = enabled;
        LayoutTransition lt = ((ViewGroup) this.mCurrentView.findViewById(R.id.nav_buttons)).getLayoutTransition();
        if (lt == null) {
            return;
        }
        if (enabled) {
            lt.enableTransitionType(2);
            lt.enableTransitionType(3);
            lt.enableTransitionType(0);
            lt.enableTransitionType(1);
            return;
        }
        lt.disableTransitionType(2);
        lt.disableTransitionType(3);
        lt.disableTransitionType(0);
        lt.disableTransitionType(1);
    }

    private void setUseFadingAnimations(boolean useFadingAnimations) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean old = lp.windowAnimations != 0;
            if (!old && useFadingAnimations) {
                lp.windowAnimations = R.style.Animation.NavigationBarFadeIn;
            } else if (old && !useFadingAnimations) {
                lp.windowAnimations = 0;
            } else {
                return;
            }
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout(this, lp);
        }
    }

    public void setSlippery(boolean newSlippery) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & 536870912) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags |= 536870912;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= -536870913;
            } else {
                return;
            }
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout(this, lp);
        }
    }

    public void setMenuVisibility(boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(boolean show, boolean force) {
        int i = 0;
        if (force || this.mShowMenu != show) {
            this.mShowMenu = show;
            boolean shouldShow = this.mShowMenu ? (this.mNavigationIconHints & 2) == 0 : false;
            View menuButton = getMenuButton();
            if (!shouldShow) {
                i = 4;
            }
            menuButton.setVisibility(i);
        }
    }

    public void onFinishInflate() {
        View[] viewArr = this.mRotatedViews;
        View findViewById = findViewById(R.id.rot0);
        this.mRotatedViews[2] = findViewById;
        viewArr[0] = findViewById;
        this.mRotatedViews[1] = findViewById(R.id.rot90);
        this.mRotatedViews[3] = this.mRotatedViews[1];
        this.mCurrentView = this.mRotatedViews[0];
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        updateRTLOrder();
    }

    public void reorient() {
        int rot = this.mDisplay.getRotation();
        for (int i = 0; i < 4; i++) {
            this.mRotatedViews[i].setVisibility(8);
        }
        this.mCurrentView = this.mRotatedViews[rot];
        this.mCurrentView.setVisibility(0);
        setLayoutTransitionsEnabled(this.mLayoutTransitionsEnabled);
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        this.mDeadZone = (DeadZone) this.mCurrentView.findViewById(R.id.deadzone);
        this.mBarTransitions.init();
        setDisabledFlags(this.mDisabledFlags, true);
        setMenuVisibility(this.mShowMenu, true);
        updateTaskSwitchHelper();
        setNavigationIconHints(this.mNavigationIconHints, true);
    }

    private void updateTaskSwitchHelper() {
        this.mTaskSwitchHelper.setBarState(this.mVertical, getLayoutDirection() == 1);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        boolean newVertical = w > 0 && h > w;
        if (newVertical != this.mVertical) {
            this.mVertical = newVertical;
            reorient();
            notifyVerticalChangedListener(newVertical);
        }
        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (this.mOnVerticalChangedListener != null) {
            this.mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
        updateTaskSwitchHelper();
        getBackButton().setContentDescription(getResources().getString(R.string.accessibility_back));
        getHomeButton().setContentDescription(getResources().getString(R.string.accessibility_home));
        getRecentsButton().setContentDescription(getResources().getString(R.string.accessibility_recent));
        getMenuButton().setContentDescription(getResources().getString(R.string.accessibility_menu));
        getImeSwitchButton().setContentDescription(getResources().getString(R.string.accessibility_ime_switch_button));
    }

    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration().getLayoutDirection() == 1;
        if (this.mIsLayoutRtl != isLayoutRtl) {
            View rotation90 = this.mRotatedViews[1];
            swapChildrenOrderIfVertical(rotation90.findViewById(R.id.nav_buttons));
            adjustExtraKeyGravity(rotation90, isLayoutRtl);
            View rotation270 = this.mRotatedViews[3];
            if (rotation90 != rotation270) {
                swapChildrenOrderIfVertical(rotation270.findViewById(R.id.nav_buttons));
                adjustExtraKeyGravity(rotation270, isLayoutRtl);
            }
            this.mIsLayoutRtl = isLayoutRtl;
        }
    }

    private void adjustExtraKeyGravity(View navBar, boolean isLayoutRtl) {
        int i = 80;
        View menu = navBar.findViewById(R.id.menu);
        View imeSwitcher = navBar.findViewById(R.id.ime_switcher);
        if (menu != null) {
            int i2;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) menu.getLayoutParams();
            if (isLayoutRtl) {
                i2 = 80;
            } else {
                i2 = 48;
            }
            lp.gravity = i2;
            menu.setLayoutParams(lp);
        }
        if (imeSwitcher != null) {
            lp = (FrameLayout.LayoutParams) imeSwitcher.getLayoutParams();
            if (!isLayoutRtl) {
                i = 48;
            }
            lp.gravity = i;
            imeSwitcher.setLayoutParams(lp);
        }
    }

    private void swapChildrenOrderIfVertical(View group) {
        if (group instanceof LinearLayout) {
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

    private String getResourceName(int resId) {
        if (resId == 0) {
            return "(null)";
        }
        try {
            return getContext().getResources().getResourceName(resId);
        } catch (NotFoundException e) {
            return "(unknown)";
        }
    }

    private void postCheckForInvalidLayout(String how) {
        this.mHandler.obtainMessage(8686, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case 4:
                return "INVISIBLE";
            case 8:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        Rect r = new Rect();
        Point size = new Point();
        this.mDisplay.getRealSize(size);
        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this) + " " + visibilityToString(getVisibility()), new Object[0]));
        getWindowVisibleDisplayFrame(r);
        boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: " + r.toShortString() + " " + visibilityToString(getWindowVisibility()) + (offscreen ? " OFFSCREEN!" : ""));
        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s", new Object[]{getResourceName(this.mCurrentView.getId()), Integer.valueOf(this.mCurrentView.getWidth()), Integer.valueOf(this.mCurrentView.getHeight()), visibilityToString(this.mCurrentView.getVisibility())}));
        String str = "      disabled=0x%08x vertical=%s menu=%s";
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(this.mDisabledFlags);
        objArr[1] = this.mVertical ? "true" : "false";
        objArr[2] = this.mShowMenu ? "true" : "false";
        pw.println(String.format(str, objArr));
        dumpButton(pw, "back", getBackButton());
        dumpButton(pw, "home", getHomeButton());
        dumpButton(pw, "rcnt", getRecentsButton());
        dumpButton(pw, "menu", getMenuButton());
        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, View button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(PhoneStatusBar.viewInfo(button) + " " + visibilityToString(button.getVisibility()) + " alpha=" + button.getAlpha());
        }
        pw.println();
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return Stub.asInterface(service);
        }
        return null;
    }

    public void refreshRestoreButton() {
        showRestoreButton(this.currentFlag);
    }

    public void showRestoreButton(boolean flag) {
        Message msg = this.mHandler.obtainMessage(1025);
        Bundle b = new Bundle();
        b.putBoolean("flag", flag);
        msg.setData(b);
        msg.sendToTarget();
    }

    public ImageView getFloatButton() {
        return (ImageView) this.mCurrentView.findViewById(R.id.multi_float);
    }

    public Button getExtensionButton() {
        return (Button) this.mCurrentView.findViewById(R.id.more);
    }

    public ImageView getFloatModeButton() {
        return (ImageView) this.mCurrentView.findViewById(R.id.float_mode);
    }

    public ImageView getSplitModeButton() {
        return (ImageView) this.mCurrentView.findViewById(R.id.split_mode);
    }

    public View getLineView() {
        return this.mCurrentView.findViewById(R.id.button_line);
    }

    public void setDisabledFlagsforMultiW(boolean flag) {
        int i;
        int i2 = 4;
        boolean z = false;
        boolean disableFloat = flag ? !this.mBar.isFloatPanelOpened() : false;
        Button extensionButton = getExtensionButton();
        if (this.mBar.isFloatPanelOpened()) {
            i = 0;
        } else {
            i = 4;
        }
        extensionButton.setVisibility(i);
        ImageView floatButton = getFloatButton();
        if (disableFloat || !this.mShowFloatWindow) {
            i = 4;
        } else {
            i = 0;
        }
        floatButton.setVisibility(i);
        if (this.mIsSplitModeEnable) {
            floatButton = getFloatModeButton();
            if (this.mBar.isFloatPanelOpened()) {
                i = 0;
            } else {
                i = 4;
            }
            floatButton.setVisibility(i);
            floatButton = getSplitModeButton();
            if (this.mBar.isFloatPanelOpened()) {
                i = 0;
            } else {
                i = 4;
            }
            floatButton.setVisibility(i);
            View lineView = getLineView();
            if (this.mBar.isFloatPanelOpened()) {
                i = 0;
            } else {
                i = 4;
            }
            lineView.setVisibility(i);
        }
        if (this.mCurrentView != null) {
            this.mRestoreButton = this.mCurrentView.findViewById(R.id.restore);
            View view = this.mRestoreButton;
            if (this.mShowRestoreButton) {
                i2 = 0;
            }
            view.setVisibility(i2);
            this.mRestoreButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    MultiWindowProxy.getInstance().restoreWindow(null, false);
                    Log.d("PhoneStatusBar/NavigationBarView", "added for restore button in navi onCLick!");
                }
            });
        }
        String str = "PhoneStatusBar/NavigationBarView";
        StringBuilder append = new StringBuilder().append("setDisabledFlags showFloatWindow is ");
        if (!disableFloat) {
            z = true;
        }
        Log.d(str, append.append(z).toString());
    }
}
