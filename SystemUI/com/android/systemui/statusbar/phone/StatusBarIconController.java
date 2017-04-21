package com.android.systemui.statusbar.phone;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class StatusBarIconController implements Tunable {
    private BatteryMeterView mBatteryMeterView;
    private TextView mClock;
    private Context mContext;
    private float mDarkIntensity;
    private int mDarkModeIconColorSingleTone;
    private DemoStatusIcons mDemoStatusIcons;
    private Interpolator mFastOutSlowIn;
    private final Handler mHandler;
    private final ArraySet<String> mIconBlacklist = new ArraySet();
    private int mIconHPadding;
    private int mIconSize;
    private int mIconTint = -1;
    private int mLightModeIconColorSingleTone;
    private Interpolator mLinearOutSlowIn;
    private ImageView mMoreIcon;
    private NotificationColorUtil mNotificationColorUtil;
    private View mNotificationIconArea;
    private IconMerger mNotificationIcons;
    private float mPendingDarkIntensity;
    private PhoneStatusBar mPhoneStatusBar;
    private SignalClusterView mSignalCluster;
    private LinearLayout mStatusIcons;
    private LinearLayout mStatusIconsKeyguard;
    private LinearLayout mSystemIconArea;
    private ValueAnimator mTintAnimator;
    private boolean mTintChangePending;
    private boolean mTransitionDeferring;
    private final Runnable mTransitionDeferringDoneRunnable = new Runnable() {
        public void run() {
            StatusBarIconController.this.mTransitionDeferring = false;
        }
    };
    private long mTransitionDeferringDuration;
    private long mTransitionDeferringStartTime;
    private boolean mTransitionPending;

    public StatusBarIconController(Context context, View statusBar, View keyguardStatusBar, PhoneStatusBar phoneStatusBar) {
        this.mContext = context;
        this.mPhoneStatusBar = phoneStatusBar;
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(context);
        this.mSystemIconArea = (LinearLayout) statusBar.findViewById(R.id.system_icon_area);
        this.mStatusIcons = (LinearLayout) statusBar.findViewById(R.id.statusIcons);
        this.mSignalCluster = (SignalClusterView) statusBar.findViewById(R.id.signal_cluster);
        this.mNotificationIconArea = statusBar.findViewById(R.id.notification_icon_area_inner);
        this.mNotificationIcons = (IconMerger) statusBar.findViewById(R.id.notificationIcons);
        this.mMoreIcon = (ImageView) statusBar.findViewById(R.id.moreIcon);
        this.mNotificationIcons.setOverflowIndicator(this.mMoreIcon);
        this.mStatusIconsKeyguard = (LinearLayout) keyguardStatusBar.findViewById(R.id.statusIcons);
        this.mBatteryMeterView = (BatteryMeterView) statusBar.findViewById(R.id.battery);
        this.mClock = (TextView) statusBar.findViewById(R.id.clock);
        this.mLinearOutSlowIn = AnimationUtils.loadInterpolator(this.mContext, 17563662);
        this.mFastOutSlowIn = AnimationUtils.loadInterpolator(this.mContext, 17563661);
        this.mDarkModeIconColorSingleTone = context.getColor(R.color.dark_mode_icon_color_single_tone);
        this.mLightModeIconColorSingleTone = context.getColor(R.color.light_mode_icon_color_single_tone);
        this.mHandler = new Handler();
        updateResources();
        TunerService.get(this.mContext).addTunable((Tunable) this, "icon_blacklist");
    }

    public void onTuningChanged(String key, String newValue) {
        if ("icon_blacklist".equals(key)) {
            int i;
            this.mIconBlacklist.clear();
            this.mIconBlacklist.addAll(getIconBlacklist(newValue));
            ArrayList<StatusBarIconView> views = new ArrayList();
            for (i = 0; i < this.mStatusIcons.getChildCount(); i++) {
                views.add((StatusBarIconView) this.mStatusIcons.getChildAt(i));
            }
            for (i = views.size() - 1; i >= 0; i--) {
                removeSystemIcon(((StatusBarIconView) views.get(i)).getSlot(), i, i);
            }
            for (i = 0; i < views.size(); i++) {
                addSystemIcon(((StatusBarIconView) views.get(i)).getSlot(), i, i, ((StatusBarIconView) views.get(i)).getStatusBarIcon());
            }
        }
    }

    public void updateResources() {
        this.mIconSize = this.mContext.getResources().getDimensionPixelSize(17104923);
        this.mIconHPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_padding);
        FontSizeUtils.updateFontSize(this.mClock, R.dimen.status_bar_clock_size);
    }

    public void addSystemIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
        boolean blocked = this.mIconBlacklist.contains(slot);
        StatusBarIconView view = new StatusBarIconView(this.mContext, slot, null, blocked);
        view.set(icon);
        this.mStatusIcons.addView(view, viewIndex, new LayoutParams(-2, this.mIconSize));
        view = new StatusBarIconView(this.mContext, slot, null, blocked);
        view.set(icon);
        this.mStatusIconsKeyguard.addView(view, viewIndex, new LayoutParams(-2, this.mIconSize));
        applyIconTint();
    }

    public void updateSystemIcon(String slot, int index, int viewIndex, StatusBarIcon old, StatusBarIcon icon) {
        ((StatusBarIconView) this.mStatusIcons.getChildAt(viewIndex)).set(icon);
        ((StatusBarIconView) this.mStatusIconsKeyguard.getChildAt(viewIndex)).set(icon);
        applyIconTint();
    }

    public void removeSystemIcon(String slot, int index, int viewIndex) {
        this.mStatusIcons.removeViewAt(viewIndex);
        this.mStatusIconsKeyguard.removeViewAt(viewIndex);
    }

    public void updateNotificationIcons(NotificationData notificationData) {
        int i;
        LayoutParams params = new LayoutParams(this.mIconSize + (this.mIconHPadding * 2), this.mPhoneStatusBar.getStatusBarHeight());
        ArrayList<Entry> activeNotifications = notificationData.getActiveNotifications();
        int N = activeNotifications.size();
        ArrayList<StatusBarIconView> toShow = new ArrayList(N);
        HashMap<String, Integer> uniqueIcon = new HashMap();
        for (i = 0; i < N; i++) {
            Entry ent = (Entry) activeNotifications.get(i);
            if ((!notificationData.isAmbient(ent.key) || NotificationData.showNotificationEvenIfUnprovisioned(ent.notification)) && PhoneStatusBar.isTopLevelChild(ent)) {
                String key = ent.notification.getPackageName() + String.valueOf(ent.notification.getNotification().icon);
                if (uniqueIcon.containsKey(key) && ((Integer) uniqueIcon.get(key)).intValue() == ent.notification.getNotification().iconLevel) {
                    Log.d("StatusBarIconController", "IconMerger feature, skip pkg / icon / iconlevel =" + ent.notification.getPackageName() + "/" + ent.notification.getNotification().icon + "/" + ent.notification.getNotification().iconLevel);
                } else {
                    uniqueIcon.put(key, Integer.valueOf(ent.notification.getNotification().iconLevel));
                    toShow.add(ent.icon);
                }
            }
        }
        ArrayList<View> toRemove = new ArrayList();
        for (i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            View child = this.mNotificationIcons.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }
        int toRemoveCount = toRemove.size();
        for (i = 0; i < toRemoveCount; i++) {
            this.mNotificationIcons.removeView((View) toRemove.get(i));
        }
        for (i = 0; i < toShow.size(); i++) {
            View v = (View) toShow.get(i);
            if (v.getParent() == null) {
                this.mNotificationIcons.addView(v, i, params);
            }
        }
        int childCount = this.mNotificationIcons.getChildCount();
        for (i = 0; i < childCount; i++) {
            View expected = (StatusBarIconView) toShow.get(i);
            if (this.mNotificationIcons.getChildAt(i) != expected) {
                this.mNotificationIcons.removeView(expected);
                this.mNotificationIcons.addView(expected, i);
            }
        }
        applyNotificationIconsTint();
    }

    public void hideSystemIconArea(boolean animate) {
        animateHide(this.mSystemIconArea, animate);
    }

    public void showSystemIconArea(boolean animate) {
        animateShow(this.mSystemIconArea, animate);
    }

    public void hideNotificationIconArea(boolean animate) {
        animateHide(this.mNotificationIconArea, animate);
    }

    public void showNotificationIconArea(boolean animate) {
        animateShow(this.mNotificationIconArea, animate);
    }

    public void setClockVisibility(boolean visible) {
        this.mClock.setVisibility(visible ? 0 : 8);
    }

    public void dump(PrintWriter pw) {
        int N = this.mStatusIcons.getChildCount();
        pw.println("  system icons: " + N);
        for (int i = 0; i < N; i++) {
            pw.println("    [" + i + "] icon=" + ((StatusBarIconView) this.mStatusIcons.getChildAt(i)));
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (this.mDemoStatusIcons == null) {
            this.mDemoStatusIcons = new DemoStatusIcons(this.mStatusIcons, this.mIconSize);
        }
        this.mDemoStatusIcons.dispatchDemoCommand(command, args);
    }

    private void animateHide(final View v, boolean animate) {
        v.animate().cancel();
        if (animate) {
            v.animate().alpha(0.0f).setDuration(160).setStartDelay(0).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(new Runnable() {
                public void run() {
                    v.setVisibility(4);
                }
            });
            return;
        }
        v.setAlpha(0.0f);
        v.setVisibility(4);
    }

    private void animateShow(View v, boolean animate) {
        v.animate().cancel();
        v.setVisibility(0);
        if (animate) {
            v.animate().alpha(1.0f).setDuration(320).setInterpolator(PhoneStatusBar.ALPHA_IN).setStartDelay(50).withEndAction(null);
            if (this.mPhoneStatusBar.isKeyguardFadingAway()) {
                v.animate().setDuration(this.mPhoneStatusBar.getKeyguardFadingAwayDuration()).setInterpolator(this.mLinearOutSlowIn).setStartDelay(this.mPhoneStatusBar.getKeyguardFadingAwayDelay()).start();
            }
            return;
        }
        v.setAlpha(1.0f);
    }

    public void setIconsDark(boolean dark) {
        float f = 1.0f;
        float f2 = 0.0f;
        if (this.mTransitionPending) {
            if (!dark) {
                f = 0.0f;
            }
            deferIconTintChange(f);
        } else if (this.mTransitionDeferring) {
            if (dark) {
                f2 = 1.0f;
            }
            animateIconTint(f2, Math.max(0, this.mTransitionDeferringStartTime - SystemClock.uptimeMillis()), this.mTransitionDeferringDuration);
        } else {
            if (dark) {
                f2 = 1.0f;
            }
            animateIconTint(f2, 0, 120);
        }
    }

    private void animateIconTint(float targetDarkIntensity, long delay, long duration) {
        if (this.mTintAnimator != null) {
            this.mTintAnimator.cancel();
        }
        if (this.mDarkIntensity != targetDarkIntensity) {
            this.mTintAnimator = ValueAnimator.ofFloat(new float[]{this.mDarkIntensity, targetDarkIntensity});
            this.mTintAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    StatusBarIconController.this.setIconTintInternal(((Float) animation.getAnimatedValue()).floatValue());
                }
            });
            this.mTintAnimator.setDuration(duration);
            this.mTintAnimator.setStartDelay(delay);
            this.mTintAnimator.setInterpolator(this.mFastOutSlowIn);
            this.mTintAnimator.start();
        }
    }

    private void setIconTintInternal(float darkIntensity) {
        this.mDarkIntensity = darkIntensity;
        this.mIconTint = ((Integer) ArgbEvaluator.getInstance().evaluate(darkIntensity, Integer.valueOf(this.mLightModeIconColorSingleTone), Integer.valueOf(this.mDarkModeIconColorSingleTone))).intValue();
        applyIconTint();
    }

    private void deferIconTintChange(float darkIntensity) {
        if (!this.mTintChangePending || darkIntensity != this.mPendingDarkIntensity) {
            this.mTintChangePending = true;
            this.mPendingDarkIntensity = darkIntensity;
        }
    }

    private void applyIconTint() {
        for (int i = 0; i < this.mStatusIcons.getChildCount(); i++) {
            ((StatusBarIconView) this.mStatusIcons.getChildAt(i)).setImageTintList(ColorStateList.valueOf(this.mIconTint));
        }
        this.mSignalCluster.setIconTint(this.mIconTint, this.mDarkIntensity);
        this.mMoreIcon.setImageTintList(ColorStateList.valueOf(this.mIconTint));
        this.mBatteryMeterView.setDarkIntensity(this.mDarkIntensity);
        this.mClock.setTextColor(this.mIconTint);
        applyNotificationIconsTint();
    }

    private void applyNotificationIconsTint() {
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) this.mNotificationIcons.getChildAt(i);
            if (Boolean.TRUE.equals(v.getTag(R.id.icon_is_pre_L)) ? isGrayscale(v) : true) {
                v.setImageTintList(ColorStateList.valueOf(this.mIconTint));
            }
        }
    }

    private boolean isGrayscale(StatusBarIconView v) {
        Object isGrayscale = v.getTag(R.id.icon_is_grayscale);
        if (isGrayscale != null) {
            return Boolean.TRUE.equals(isGrayscale);
        }
        boolean grayscale = this.mNotificationColorUtil.isGrayscaleIcon(v.getDrawable());
        v.setTag(R.id.icon_is_grayscale, Boolean.valueOf(grayscale));
        return grayscale;
    }

    public void appTransitionPending() {
        this.mTransitionPending = true;
    }

    public void appTransitionCancelled() {
        if (this.mTransitionPending && this.mTintChangePending) {
            this.mTintChangePending = false;
            animateIconTint(this.mPendingDarkIntensity, 0, 120);
        }
        this.mTransitionPending = false;
    }

    public void appTransitionStarting(long startTime, long duration) {
        if (this.mTransitionPending && this.mTintChangePending) {
            this.mTintChangePending = false;
            animateIconTint(this.mPendingDarkIntensity, Math.max(0, startTime - SystemClock.uptimeMillis()), duration);
        } else if (this.mTransitionPending) {
            this.mTransitionDeferring = true;
            this.mTransitionDeferringStartTime = startTime;
            this.mTransitionDeferringDuration = duration;
            this.mHandler.removeCallbacks(this.mTransitionDeferringDoneRunnable);
            this.mHandler.postAtTime(this.mTransitionDeferringDoneRunnable, startTime);
        }
        this.mTransitionPending = false;
    }

    public static ArraySet<String> getIconBlacklist(String blackListStr) {
        ArraySet<String> ret = new ArraySet();
        if (blackListStr != null) {
            for (String slot : blackListStr.split(",")) {
                if (!TextUtils.isEmpty(slot)) {
                    ret.add(slot);
                }
            }
        }
        return ret;
    }
}
