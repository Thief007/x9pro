package com.android.systemui.volume;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioSystem;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.NET;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.VolumeDialogController.Callbacks;
import com.android.systemui.volume.VolumeDialogController.State;
import com.android.systemui.volume.VolumeDialogController.StreamState;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class VolumeDialog {
    private static final String TAG = Util.logTag(VolumeDialog.class);
    private final Accessibility mAccessibility = new Accessibility();
    private final ColorStateList mActiveSliderTint;
    private int mActiveStream;
    private boolean mAutomute = true;
    private Callback mCallback;
    private final OnClickListener mClickExpand = new OnClickListener() {
        public void onClick(View v) {
            if (!VolumeDialog.this.mExpandButtonAnimationRunning) {
                Events.writeEvent(VolumeDialog.this.mContext, 3, Boolean.valueOf(!VolumeDialog.this.mExpanded));
                VolumeDialog.this.setExpandedH(newExpand);
            }
        }
    };
    private final OnClickListener mClickSettings = new OnClickListener() {
        public void onClick(View v) {
            VolumeDialog.this.mSettingsButton.postDelayed(new Runnable() {
                public void run() {
                    Events.writeEvent(VolumeDialog.this.mContext, 8, new Object[0]);
                    if (VolumeDialog.this.mCallback != null) {
                        VolumeDialog.this.mCallback.onSettingsClicked();
                    }
                }
            }, 200);
        }
    };
    private long mCollapseTime;
    private final Context mContext;
    private final VolumeDialogController mController;
    private final Callbacks mControllerCallbackH = new Callbacks() {
        public void onShowRequested(int reason) {
            VolumeDialog.this.showH(reason);
        }

        public void onDismissRequested(int reason) {
            VolumeDialog.this.dismissH(reason);
        }

        public void onScreenOff() {
            VolumeDialog.this.dismissH(4);
        }

        public void onStateChanged(State state) {
            VolumeDialog.this.onStateChangedH(state);
        }

        public void onLayoutDirectionChanged(int layoutDirection) {
            VolumeDialog.this.mDialogView.setLayoutDirection(layoutDirection);
        }

        public void onConfigurationChanged() {
            VolumeDialog.this.updateWindowWidthH();
            VolumeDialog.this.mSpTexts.update();
            VolumeDialog.this.mZenFooter.onConfigurationChanged();
        }

        public void onShowVibrateHint() {
            if (VolumeDialog.this.mSilentMode) {
                VolumeDialog.this.mController.setRingerMode(0, false);
            }
        }

        public void onShowSilentHint() {
            if (VolumeDialog.this.mSilentMode) {
                VolumeDialog.this.mController.setRingerMode(2, false);
            }
        }

        public void onShowSafetyWarning(int flags) {
            VolumeDialog.this.showSafetyWarningH(flags);
        }
    };
    private final CustomDialog mDialog;
    private final ViewGroup mDialogContentView;
    private final ViewGroup mDialogView;
    private final SparseBooleanArray mDynamic = new SparseBooleanArray();
    private final ImageButton mExpandButton;
    private final int mExpandButtonAnimationDuration;
    private boolean mExpandButtonAnimationRunning;
    private int mExpandButtonRes;
    private boolean mExpanded;
    private final H mHandler = new H();
    private final ColorStateList mInactiveSliderTint;
    private final KeyguardManager mKeyguard;
    private final LayoutTransition mLayoutTransition;
    private final VolumeDialogMotion mMotion;
    private boolean mPendingRecheckAll;
    private boolean mPendingStateChanged;
    private final List<VolumeRow> mRows = new ArrayList();
    private SafetyWarningDialog mSafetyWarning;
    private final Object mSafetyWarningLock = new Object();
    private final View mSettingsButton;
    private boolean mShowHeaders = true;
    private boolean mShowing;
    private boolean mSilentMode = true;
    private final SpTexts mSpTexts;
    private State mState;
    private final ZenFooter mZenFooter;

    private final class Accessibility extends AccessibilityDelegate {
        private boolean mFeedbackEnabled;
        private AccessibilityManager mMgr;

        private Accessibility() {
        }

        public void init() {
            this.mMgr = (AccessibilityManager) VolumeDialog.this.mContext.getSystemService("accessibility");
            VolumeDialog.this.mDialogView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                public void onViewDetachedFromWindow(View v) {
                    if (D.BUG) {
                        Log.d(VolumeDialog.TAG, "onViewDetachedFromWindow");
                    }
                }

                public void onViewAttachedToWindow(View v) {
                    if (D.BUG) {
                        Log.d(VolumeDialog.TAG, "onViewAttachedToWindow");
                    }
                    Accessibility.this.updateFeedbackEnabled();
                }
            });
            VolumeDialog.this.mDialogView.setAccessibilityDelegate(this);
            this.mMgr.addAccessibilityStateChangeListener(new AccessibilityStateChangeListener() {
                public void onAccessibilityStateChanged(boolean enabled) {
                    Accessibility.this.updateFeedbackEnabled();
                }
            });
            updateFeedbackEnabled();
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
            VolumeDialog.this.rescheduleTimeoutH();
            return super.onRequestSendAccessibilityEvent(host, child, event);
        }

        private void updateFeedbackEnabled() {
            this.mFeedbackEnabled = computeFeedbackEnabled();
        }

        private boolean computeFeedbackEnabled() {
            for (AccessibilityServiceInfo asi : this.mMgr.getEnabledAccessibilityServiceList(-1)) {
                if (asi.feedbackType != 0 && asi.feedbackType != 16) {
                    return true;
                }
            }
            return false;
        }
    }

    public interface Callback {
        void onSettingsClicked();
    }

    private final class CustomDialog extends Dialog {
        public CustomDialog(Context context) {
            super(context);
        }

        public boolean dispatchTouchEvent(MotionEvent ev) {
            VolumeDialog.this.rescheduleTimeoutH();
            return super.dispatchTouchEvent(ev);
        }

        protected void onStop() {
            super.onStop();
            boolean animating = VolumeDialog.this.mMotion.isAnimating();
            if (D.BUG) {
                Log.d(VolumeDialog.TAG, "onStop animating=" + animating);
            }
            if (animating) {
                VolumeDialog.this.mPendingRecheckAll = true;
            } else {
                VolumeDialog.this.mHandler.sendEmptyMessage(4);
            }
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (!isShowing() || event.getAction() != 4) {
                return false;
            }
            VolumeDialog.this.dismissH(1);
            return true;
        }
    }

    private final class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 1:
                    VolumeDialog.this.showH(msg.arg1);
                    return;
                case 2:
                    VolumeDialog.this.dismissH(msg.arg1);
                    return;
                case 3:
                    VolumeDialog.this.recheckH((VolumeRow) msg.obj);
                    return;
                case 4:
                    VolumeDialog.this.recheckH(null);
                    return;
                case 5:
                    VolumeDialog volumeDialog = VolumeDialog.this;
                    int i = msg.arg1;
                    if (msg.arg2 != 0) {
                        z = true;
                    }
                    volumeDialog.setStreamImportantH(i, z);
                    return;
                case 6:
                    VolumeDialog.this.rescheduleTimeoutH();
                    return;
                case 7:
                    VolumeDialog.this.onStateChangedH(VolumeDialog.this.mState);
                    return;
                case 8:
                    VolumeDialog.this.updateDialogBottomMarginH();
                    return;
                default:
                    return;
            }
        }
    }

    private static class VolumeRow {
        private ObjectAnimator anim;
        private int animTargetProgress;
        private int cachedExpandButtonRes;
        private int cachedIconRes;
        private boolean cachedShowHeaders;
        private ColorStateList cachedSliderTint;
        private TextView header;
        private ImageButton icon;
        private int iconMuteRes;
        private int iconRes;
        private int iconState;
        private boolean important;
        private int lastAudibleLevel;
        private int requestedLevel;
        private ImageButton settingsButton;
        private SeekBar slider;
        private View space;
        private StreamState ss;
        private int stream;
        private boolean tracking;
        private long userAttempt;
        private View view;

        private VolumeRow() {
            this.requestedLevel = -1;
            this.cachedShowHeaders = true;
            this.lastAudibleLevel = 1;
        }
    }

    private final class VolumeSeekBarChangeListener implements OnSeekBarChangeListener {
        private final VolumeRow mRow;

        private VolumeSeekBarChangeListener(VolumeRow row) {
            this.mRow = row;
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (this.mRow.ss != null) {
                if (D.BUG) {
                    Log.d(VolumeDialog.TAG, AudioSystem.streamToString(this.mRow.stream) + " onProgressChanged " + progress + " fromUser=" + fromUser);
                }
                if (fromUser) {
                    if (this.mRow.ss.levelMin > 0) {
                        int minProgress = this.mRow.ss.levelMin * 100;
                        if (progress < minProgress) {
                            seekBar.setProgress(minProgress);
                        }
                    }
                    int userLevel = VolumeDialog.getImpliedLevel(seekBar, progress);
                    if (this.mRow.ss.level != userLevel || (this.mRow.ss.muted && userLevel > 0)) {
                        this.mRow.userAttempt = SystemClock.uptimeMillis();
                        if (this.mRow.requestedLevel != userLevel) {
                            VolumeDialog.this.mController.setStreamVolume(this.mRow.stream, userLevel);
                            this.mRow.requestedLevel = userLevel;
                            Events.writeEvent(VolumeDialog.this.mContext, 9, Integer.valueOf(this.mRow.stream), Integer.valueOf(userLevel));
                        }
                    }
                }
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            if (D.BUG) {
                Log.d(VolumeDialog.TAG, "onStartTrackingTouch " + this.mRow.stream);
            }
            VolumeDialog.this.mController.setActiveStream(this.mRow.stream);
            this.mRow.tracking = true;
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            if (D.BUG) {
                Log.d(VolumeDialog.TAG, "onStopTrackingTouch " + this.mRow.stream);
            }
            this.mRow.tracking = false;
            this.mRow.userAttempt = SystemClock.uptimeMillis();
            int userLevel = VolumeDialog.getImpliedLevel(seekBar, seekBar.getProgress());
            Events.writeEvent(VolumeDialog.this.mContext, 16, Integer.valueOf(this.mRow.stream), Integer.valueOf(userLevel));
            if (this.mRow.ss.level != userLevel) {
                VolumeDialog.this.mHandler.sendMessageDelayed(VolumeDialog.this.mHandler.obtainMessage(3, this.mRow), 1000);
            }
        }
    }

    public VolumeDialog(Context context, int windowType, VolumeDialogController controller, ZenModeController zenModeController, Callback callback) {
        this.mContext = context;
        this.mController = controller;
        this.mCallback = callback;
        this.mSpTexts = new SpTexts(this.mContext);
        this.mKeyguard = (KeyguardManager) context.getSystemService("keyguard");
        this.mDialog = new CustomDialog(this.mContext);
        Window window = this.mDialog.getWindow();
        window.requestFeature(1);
        window.setBackgroundDrawable(new ColorDrawable(0));
        window.clearFlags(2);
        window.addFlags(17563944);
        this.mDialog.setCanceledOnTouchOutside(true);
        Resources res = this.mContext.getResources();
        LayoutParams lp = window.getAttributes();
        lp.type = windowType;
        lp.format = -3;
        lp.setTitle(VolumeDialog.class.getSimpleName());
        lp.gravity = 49;
        lp.y = res.getDimensionPixelSize(R.dimen.volume_offset_top);
        lp.gravity = 48;
        lp.windowAnimations = -1;
        window.setAttributes(lp);
        window.setSoftInputMode(48);
        this.mActiveSliderTint = loadColorStateList(R.color.system_accent_color);
        this.mInactiveSliderTint = loadColorStateList(R.color.volume_slider_inactive);
        this.mDialog.setContentView(R.layout.volume_dialog);
        this.mDialogView = (ViewGroup) this.mDialog.findViewById(R.id.volume_dialog);
        this.mDialogContentView = (ViewGroup) this.mDialog.findViewById(R.id.volume_dialog_content);
        this.mExpandButton = (ImageButton) this.mDialogView.findViewById(R.id.volume_expand_button);
        this.mExpandButton.setOnClickListener(this.mClickExpand);
        updateWindowWidthH();
        updateExpandButtonH();
        this.mLayoutTransition = new LayoutTransition();
        this.mLayoutTransition.setDuration(new ValueAnimator().getDuration() / 2);
        this.mDialogContentView.setLayoutTransition(this.mLayoutTransition);
        this.mMotion = new VolumeDialogMotion(this.mDialog, this.mDialogView, this.mDialogContentView, this.mExpandButton, new com.android.systemui.volume.VolumeDialogMotion.Callback() {
            public void onAnimatingChanged(boolean animating) {
                if (!animating) {
                    if (VolumeDialog.this.mPendingStateChanged) {
                        VolumeDialog.this.mHandler.sendEmptyMessage(7);
                        VolumeDialog.this.mPendingStateChanged = false;
                    }
                    if (VolumeDialog.this.mPendingRecheckAll) {
                        VolumeDialog.this.mHandler.sendEmptyMessage(4);
                        VolumeDialog.this.mPendingRecheckAll = false;
                    }
                }
            }
        });
        addRow(2, R.drawable.ic_volume_ringer, R.drawable.ic_volume_ringer_mute, true);
        addRow(3, R.drawable.ic_volume_media, R.drawable.ic_volume_media_mute, true);
        addRow(4, R.drawable.ic_volume_alarm, R.drawable.ic_volume_alarm_mute, false);
        addRow(0, R.drawable.ic_volume_voice, R.drawable.ic_volume_voice, false);
        addRow(6, R.drawable.ic_volume_bt_sco, R.drawable.ic_volume_bt_sco, false);
        addRow(1, R.drawable.ic_volume_system, R.drawable.ic_volume_system_mute, false);
        this.mSettingsButton = this.mDialog.findViewById(R.id.volume_settings_button);
        this.mSettingsButton.setOnClickListener(this.mClickSettings);
        this.mExpandButtonAnimationDuration = res.getInteger(R.integer.volume_expand_animation_duration);
        this.mZenFooter = (ZenFooter) this.mDialog.findViewById(R.id.volume_zen_footer);
        this.mZenFooter.init(zenModeController);
        this.mAccessibility.init();
        controller.addCallback(this.mControllerCallbackH, this.mHandler);
        controller.getState();
    }

    private ColorStateList loadColorStateList(int colorResId) {
        return ColorStateList.valueOf(this.mContext.getColor(colorResId));
    }

    private void updateWindowWidthH() {
        ViewGroup.LayoutParams lp = this.mDialogView.getLayoutParams();
        DisplayMetrics dm = this.mContext.getResources().getDisplayMetrics();
        if (D.BUG) {
            Log.d(TAG, "updateWindowWidth dm.w=" + dm.widthPixels);
        }
        int w = dm.widthPixels;
        int max = this.mContext.getResources().getDimensionPixelSize(R.dimen.standard_notification_panel_width);
        if (w > max) {
            w = max;
        }
        lp.width = w - (this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_side_padding) * 2);
        this.mDialogView.setLayoutParams(lp);
    }

    public void setStreamImportant(int stream, boolean important) {
        this.mHandler.obtainMessage(5, stream, important ? 1 : 0).sendToTarget();
    }

    public void setShowHeaders(boolean showHeaders) {
        if (showHeaders != this.mShowHeaders) {
            this.mShowHeaders = showHeaders;
            this.mHandler.sendEmptyMessage(4);
        }
    }

    public void setAutomute(boolean automute) {
        if (this.mAutomute != automute) {
            this.mAutomute = automute;
            this.mHandler.sendEmptyMessage(4);
        }
    }

    public void setSilentMode(boolean silentMode) {
        if (this.mSilentMode != silentMode) {
            this.mSilentMode = silentMode;
            this.mHandler.sendEmptyMessage(4);
        }
    }

    private void addRow(int stream, int iconRes, int iconMuteRes, boolean important) {
        final VolumeRow row = initRow(stream, iconRes, iconMuteRes, important);
        if (!this.mRows.isEmpty()) {
            View v = new View(this.mContext);
            v.setId(16908288);
            this.mDialogContentView.addView(v, this.mDialogContentView.getChildCount() - 1, new LinearLayout.LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(R.dimen.volume_slider_interspacing)));
            row.space = v;
        }
        row.settingsButton.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                boolean moved = (oldLeft == left && oldTop == top) ? false : true;
                if (D.BUG) {
                    Log.d(VolumeDialog.TAG, "onLayoutChange moved=" + moved + " old=" + new Rect(oldLeft, oldTop, oldRight, oldBottom).toShortString() + " new=" + new Rect(left, top, right, bottom).toShortString());
                }
                if (moved) {
                    for (int i = 0; i < VolumeDialog.this.mDialogContentView.getChildCount(); i++) {
                        View c = VolumeDialog.this.mDialogContentView.getChildAt(i);
                        if (c.isShown()) {
                            if (c == row.view) {
                                VolumeDialog.this.repositionExpandAnim(row);
                            }
                            return;
                        }
                    }
                }
            }
        });
        this.mDialogContentView.addView(row.view, this.mDialogContentView.getChildCount() - 1);
        this.mRows.add(row);
    }

    private boolean isAttached() {
        return this.mDialogContentView != null ? this.mDialogContentView.isAttachedToWindow() : false;
    }

    private VolumeRow getActiveRow() {
        for (VolumeRow row : this.mRows) {
            if (row.stream == this.mActiveStream) {
                return row;
            }
        }
        return (VolumeRow) this.mRows.get(0);
    }

    private VolumeRow findRow(int stream) {
        for (VolumeRow row : this.mRows) {
            if (row.stream == stream) {
                return row;
            }
        }
        return null;
    }

    private void repositionExpandAnim(VolumeRow row) {
        int[] loc = new int[2];
        row.settingsButton.getLocationInWindow(loc);
        MarginLayoutParams mlp = (MarginLayoutParams) this.mDialogView.getLayoutParams();
        int x = loc[0] - mlp.leftMargin;
        int y = loc[1] - mlp.topMargin;
        if (D.BUG) {
            Log.d(TAG, "repositionExpandAnim x=" + x + " y=" + y);
        }
        this.mExpandButton.setTranslationX((float) x);
        this.mExpandButton.setTranslationY((float) y);
        this.mExpandButton.setTag(Integer.valueOf(y));
    }

    public void dump(PrintWriter writer) {
        writer.println(VolumeDialog.class.getSimpleName() + " state:");
        writer.print("  mShowing: ");
        writer.println(this.mShowing);
        writer.print("  mExpanded: ");
        writer.println(this.mExpanded);
        writer.print("  mExpandButtonAnimationRunning: ");
        writer.println(this.mExpandButtonAnimationRunning);
        writer.print("  mActiveStream: ");
        writer.println(this.mActiveStream);
        writer.print("  mDynamic: ");
        writer.println(this.mDynamic);
        writer.print("  mShowHeaders: ");
        writer.println(this.mShowHeaders);
        writer.print("  mAutomute: ");
        writer.println(this.mAutomute);
        writer.print("  mSilentMode: ");
        writer.println(this.mSilentMode);
        writer.print("  mCollapseTime: ");
        writer.println(this.mCollapseTime);
        writer.print("  mAccessibility.mFeedbackEnabled: ");
        writer.println(this.mAccessibility.mFeedbackEnabled);
    }

    private static int getImpliedLevel(SeekBar seekBar, int progress) {
        int m = seekBar.getMax();
        int n = (m / 100) - 1;
        if (progress == 0) {
            return 0;
        }
        return progress == m ? m / 100 : ((int) ((((float) progress) / ((float) m)) * ((float) n))) + 1;
    }

    @SuppressLint({"InflateParams"})
    private VolumeRow initRow(final int stream, int iconRes, int iconMuteRes, boolean important) {
        final VolumeRow row = new VolumeRow();
        row.stream = stream;
        row.iconRes = iconRes;
        row.iconMuteRes = iconMuteRes;
        row.important = important;
        row.view = this.mDialog.getLayoutInflater().inflate(R.layout.volume_dialog_row, null);
        row.view.setTag(row);
        row.header = (TextView) row.view.findViewById(R.id.volume_row_header);
        this.mSpTexts.add(row.header);
        row.slider = (SeekBar) row.view.findViewById(R.id.volume_row_slider);
        row.slider.setOnSeekBarChangeListener(new VolumeSeekBarChangeListener(row));
        row.view.setOnTouchListener(new OnTouchListener() {
            private boolean mDragging;
            private final Rect mSliderHitRect = new Rect();

            @SuppressLint({"ClickableViewAccessibility"})
            public boolean onTouch(View v, MotionEvent event) {
                row.slider.getHitRect(this.mSliderHitRect);
                if (!this.mDragging && event.getActionMasked() == 0 && event.getY() < ((float) this.mSliderHitRect.top)) {
                    this.mDragging = true;
                }
                if (!this.mDragging) {
                    return false;
                }
                event.offsetLocation((float) (-this.mSliderHitRect.left), (float) (-this.mSliderHitRect.top));
                row.slider.dispatchTouchEvent(event);
                if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
                    this.mDragging = false;
                }
                return true;
            }
        });
        row.icon = (ImageButton) row.view.findViewById(R.id.volume_row_icon);
        row.icon.setImageResource(iconRes);
        row.icon.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int i = 0;
                Events.writeEvent(VolumeDialog.this.mContext, 7, Integer.valueOf(row.stream), Integer.valueOf(row.iconState));
                VolumeDialog.this.mController.setActiveStream(row.stream);
                VolumeDialogController -get3;
                int i2;
                if (row.stream == 2) {
                    boolean hasVibrator = VolumeDialog.this.mController.hasVibrator();
                    if (VolumeDialog.this.mState.ringerModeInternal != 2) {
                        VolumeDialog.this.mController.setRingerMode(2, false);
                        if (row.ss.level == 0) {
                            VolumeDialog.this.mController.setStreamVolume(stream, 1);
                        }
                    } else if (hasVibrator) {
                        VolumeDialog.this.mController.setRingerMode(1, false);
                    } else {
                        boolean wasZero = row.ss.level == 0;
                        -get3 = VolumeDialog.this.mController;
                        i2 = stream;
                        if (wasZero) {
                            i = row.lastAudibleLevel;
                        }
                        -get3.setStreamVolume(i2, i);
                    }
                } else {
                    boolean vmute = row.ss.level == 0;
                    -get3 = VolumeDialog.this.mController;
                    i2 = stream;
                    if (vmute) {
                        i = row.lastAudibleLevel;
                    }
                    -get3.setStreamVolume(i2, i);
                }
                row.userAttempt = 0;
            }
        });
        row.settingsButton = (ImageButton) row.view.findViewById(R.id.volume_settings_button);
        row.settingsButton.setOnClickListener(this.mClickSettings);
        return row;
    }

    private void showH(int reason) {
        if (D.BUG) {
            Log.d(TAG, "showH r=" + Events.DISMISS_REASONS[reason]);
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        rescheduleTimeoutH();
        if (!this.mShowing) {
            this.mShowing = true;
            this.mMotion.startShow();
            Events.writeEvent(this.mContext, 0, Integer.valueOf(reason), Boolean.valueOf(this.mKeyguard.isKeyguardLocked()));
            this.mController.notifyVisible(true);
        }
    }

    protected void rescheduleTimeoutH() {
        this.mHandler.removeMessages(2);
        int timeout = computeTimeoutH();
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2, 3, 0), (long) timeout);
        if (D.BUG) {
            Log.d(TAG, "rescheduleTimeout " + timeout + " " + Debug.getCaller());
        }
        this.mController.userActivity();
    }

    private int computeTimeoutH() {
        if (this.mAccessibility.mFeedbackEnabled) {
            return NET.DEFAULT_TIMEOUT;
        }
        if (this.mSafetyWarning != null || this.mExpanded || this.mExpandButtonAnimationRunning) {
            return 5000;
        }
        if (this.mActiveStream == 3) {
            return 1500;
        }
        return 3000;
    }

    protected void dismissH(int reason) {
        if (!this.mMotion.isAnimating()) {
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(1);
            if (this.mShowing) {
                this.mShowing = false;
                this.mMotion.startDismiss(new Runnable() {
                    public void run() {
                        VolumeDialog.this.setExpandedH(false);
                    }
                });
                Events.writeEvent(this.mContext, 1, Integer.valueOf(reason));
                this.mController.notifyVisible(false);
                synchronized (this.mSafetyWarningLock) {
                    if (this.mSafetyWarning != null) {
                        if (D.BUG) {
                            Log.d(TAG, "SafetyWarning dismissed");
                        }
                        this.mSafetyWarning.dismiss();
                    }
                }
            }
        }
    }

    private void updateDialogBottomMarginH() {
        int bottomMargin;
        boolean collapsing = this.mCollapseTime != 0 && System.currentTimeMillis() - this.mCollapseTime < getConservativeCollapseDuration();
        MarginLayoutParams mlp = (MarginLayoutParams) this.mDialogView.getLayoutParams();
        if (collapsing) {
            bottomMargin = this.mDialogContentView.getHeight();
        } else {
            bottomMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.volume_dialog_margin_bottom);
        }
        if (bottomMargin != mlp.bottomMargin) {
            if (D.BUG) {
                Log.d(TAG, "bottomMargin " + mlp.bottomMargin + " -> " + bottomMargin);
            }
            mlp.bottomMargin = bottomMargin;
            this.mDialogView.setLayoutParams(mlp);
        }
    }

    private long getConservativeCollapseDuration() {
        return (long) (this.mExpandButtonAnimationDuration * 3);
    }

    private void prepareForCollapse() {
        this.mHandler.removeMessages(8);
        this.mCollapseTime = System.currentTimeMillis();
        updateDialogBottomMarginH();
        this.mHandler.sendEmptyMessageDelayed(8, getConservativeCollapseDuration());
    }

    private void setExpandedH(boolean expanded) {
        if (this.mExpanded != expanded) {
            this.mExpanded = expanded;
            this.mExpandButtonAnimationRunning = isAttached();
            if (D.BUG) {
                Log.d(TAG, "setExpandedH " + expanded);
            }
            if (!this.mExpanded && this.mExpandButtonAnimationRunning) {
                prepareForCollapse();
            }
            updateRowsH();
            if (this.mExpandButtonAnimationRunning) {
                Drawable d = this.mExpandButton.getDrawable();
                if (d instanceof AnimatedVectorDrawable) {
                    AnimatedVectorDrawable avd = (AnimatedVectorDrawable) d.getConstantState().newDrawable();
                    this.mExpandButton.setImageDrawable(avd);
                    avd.start();
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            VolumeDialog.this.mExpandButtonAnimationRunning = false;
                            VolumeDialog.this.updateExpandButtonH();
                            VolumeDialog.this.rescheduleTimeoutH();
                        }
                    }, (long) this.mExpandButtonAnimationDuration);
                }
            }
            rescheduleTimeoutH();
        }
    }

    private void updateExpandButtonH() {
        if (D.BUG) {
            Log.d(TAG, "updateExpandButtonH");
        }
        this.mExpandButton.setClickable(!this.mExpandButtonAnimationRunning);
        if (!this.mExpandButtonAnimationRunning || !isAttached()) {
            int res;
            if (this.mExpanded) {
                res = R.drawable.ic_volume_collapse_animation;
            } else {
                res = R.drawable.ic_volume_expand_animation;
            }
            if (res != this.mExpandButtonRes) {
                this.mExpandButtonRes = res;
                this.mExpandButton.setImageResource(res);
                this.mExpandButton.setContentDescription(this.mContext.getString(this.mExpanded ? R.string.accessibility_volume_collapse : R.string.accessibility_volume_expand));
            }
        }
    }

    private boolean isVisibleH(VolumeRow row, boolean isActive) {
        if ((this.mExpanded && row.view.getVisibility() == 0) || (this.mExpanded && (row.important || isActive))) {
            return true;
        }
        if (this.mExpanded) {
            return false;
        }
        return isActive;
    }

    private void updateRowsH() {
        if (D.BUG) {
            Log.d(TAG, "updateRowsH");
        }
        VolumeRow activeRow = getActiveRow();
        updateFooterH();
        updateExpandButtonH();
        if (!this.mShowing) {
            trimObsoleteH();
        }
        for (VolumeRow row : this.mRows) {
            boolean z;
            boolean isActive = row == activeRow;
            boolean visible = isVisibleH(row, isActive);
            Util.setVisOrGone(row.view, visible);
            View -get16 = row.space;
            if (visible) {
                z = this.mExpanded;
            } else {
                z = false;
            }
            Util.setVisOrGone(-get16, z);
            int expandButtonRes = this.mExpanded ? R.drawable.ic_volume_settings : 0;
            if (expandButtonRes != row.cachedExpandButtonRes) {
                row.cachedExpandButtonRes = expandButtonRes;
                if (expandButtonRes == 0) {
                    row.settingsButton.setImageDrawable(null);
                } else {
                    row.settingsButton.setImageResource(expandButtonRes);
                }
            }
            Util.setVisOrInvis(row.settingsButton, false);
            updateVolumeRowHeaderVisibleH(row);
            TextView -get6 = row.header;
            float f = (this.mExpanded && isActive) ? 1.0f : 0.5f;
            -get6.setAlpha(f);
            updateVolumeRowSliderTintH(row, isActive);
        }
    }

    private void trimObsoleteH() {
        if (D.BUG) {
            Log.d(TAG, "trimObsoleteH");
        }
        for (int i = this.mRows.size() - 1; i >= 0; i--) {
            VolumeRow row = (VolumeRow) this.mRows.get(i);
            if (!(row.ss == null || !row.ss.dynamic || this.mDynamic.get(row.stream))) {
                this.mRows.remove(i);
                this.mDialogContentView.removeView(row.view);
                this.mDialogContentView.removeView(row.space);
            }
        }
    }

    private void onStateChangedH(State state) {
        boolean animating = this.mMotion.isAnimating();
        if (D.BUG) {
            Log.d(TAG, "onStateChangedH animating=" + animating);
        }
        this.mState = state;
        if (animating) {
            this.mPendingStateChanged = true;
            return;
        }
        this.mDynamic.clear();
        for (int i = 0; i < state.states.size(); i++) {
            int stream = state.states.keyAt(i);
            if (((StreamState) state.states.valueAt(i)).dynamic) {
                this.mDynamic.put(stream, true);
                if (findRow(stream) == null) {
                    addRow(stream, R.drawable.ic_volume_remote, R.drawable.ic_volume_remote_mute, true);
                }
            }
        }
        if (this.mActiveStream != state.activeStream) {
            this.mActiveStream = state.activeStream;
            updateRowsH();
            rescheduleTimeoutH();
        }
        for (VolumeRow row : this.mRows) {
            updateVolumeRowH(row);
        }
        updateFooterH();
    }

    private void updateFooterH() {
        if (D.BUG) {
            Log.d(TAG, "updateFooterH");
        }
        boolean wasVisible = this.mZenFooter.getVisibility() == 0;
        boolean visible = this.mState.zenMode != 0;
        if (!(wasVisible == visible || visible)) {
            prepareForCollapse();
        }
        Util.setVisOrGone(this.mZenFooter, visible);
        this.mZenFooter.update();
    }

    private void updateVolumeRowH(VolumeRow row) {
        if (D.BUG) {
            Log.d(TAG, "updateVolumeRowH s=" + row.stream);
        }
        if (this.mState != null) {
            StreamState ss = (StreamState) this.mState.states.get(row.stream);
            if (ss != null) {
                int i;
                int vlevel;
                row.ss = ss;
                if (ss.level > 0) {
                    row.lastAudibleLevel = ss.level;
                }
                if (ss.level == row.requestedLevel) {
                    row.requestedLevel = -1;
                }
                boolean isRingStream = row.stream == 2;
                boolean isSystemStream = row.stream == 1;
                boolean isAlarmStream = row.stream == 4;
                boolean isMusicStream = row.stream == 3;
                boolean isRingVibrate = isRingStream ? this.mState.ringerModeInternal == 1 : false;
                boolean isRingSilent = isRingStream ? this.mState.ringerModeInternal == 0 : false;
                boolean isZenAlarms = this.mState.zenMode == 3;
                boolean isZenNone = this.mState.zenMode == 2;
                boolean isZenPriority = this.mState.zenMode == 1;
                boolean z = (isRingStream || isSystemStream) ? isZenNone : false;
                boolean z2 = isRingStream ? isZenPriority : false;
                boolean z3 = isZenAlarms ? !isRingStream ? isSystemStream : true : isZenNone ? (isRingStream || isSystemStream || isAlarmStream) ? true : isMusicStream : false;
                int max = ss.levelMax * 100;
                if (max != row.slider.getMax()) {
                    row.slider.setMax(max);
                }
                updateVolumeRowHeaderVisibleH(row);
                String text = ss.name;
                if (this.mShowHeaders) {
                    if (z) {
                        text = this.mContext.getString(R.string.volume_stream_muted_dnd, new Object[]{ss.name});
                    } else if (isRingVibrate && z2) {
                        text = this.mContext.getString(R.string.volume_stream_vibrate_dnd, new Object[]{ss.name});
                    } else if (isRingVibrate) {
                        text = this.mContext.getString(R.string.volume_stream_vibrate, new Object[]{ss.name});
                    } else if (ss.muted || (this.mAutomute && ss.level == 0)) {
                        text = this.mContext.getString(R.string.volume_stream_muted, new Object[]{ss.name});
                    } else if (z2) {
                        text = this.mContext.getString(R.string.volume_stream_limited_dnd, new Object[]{ss.name});
                    }
                }
                Util.setText(row.header, text);
                boolean iconEnabled = (this.mAutomute || ss.muteSupported) && !z3;
                row.icon.setEnabled(iconEnabled);
                row.icon.setAlpha(iconEnabled ? 1.0f : 0.5f);
                int iconRes = isRingVibrate ? R.drawable.ic_volume_ringer_vibrate : (isRingSilent || z3) ? row.cachedIconRes : ss.routedToBluetooth ? ss.muted ? R.drawable.ic_volume_media_bt_mute : R.drawable.ic_volume_media_bt : (this.mAutomute && ss.level == 0) ? row.iconMuteRes : ss.muted ? row.iconMuteRes : row.iconRes;
                if (iconRes != row.cachedIconRes) {
                    if (row.cachedIconRes != 0 && isRingVibrate) {
                        this.mController.vibrate();
                    }
                    row.cachedIconRes = iconRes;
                    row.icon.setImageResource(iconRes);
                }
                if (iconRes == R.drawable.ic_volume_ringer_vibrate) {
                    i = 3;
                } else if (iconRes == R.drawable.ic_volume_media_bt_mute || iconRes == row.iconMuteRes) {
                    i = 2;
                } else if (iconRes == R.drawable.ic_volume_media_bt || iconRes == row.iconRes) {
                    i = 1;
                } else {
                    i = 0;
                }
                row.iconState = i;
                row.icon.setContentDescription(ss.name);
                boolean enableSlider = !z3;
                if (!row.ss.muted || (!isRingVibrate && (isRingStream || z3))) {
                    vlevel = row.ss.level;
                } else {
                    vlevel = 0;
                }
                updateVolumeRowSliderH(row, enableSlider, vlevel);
            }
        }
    }

    private void updateVolumeRowHeaderVisibleH(VolumeRow row) {
        boolean z = !this.mShowHeaders ? this.mExpanded ? row.ss != null ? row.ss.dynamic : false : false : true;
        if (row.cachedShowHeaders != z) {
            row.cachedShowHeaders = z;
            Util.setVisOrGone(row.header, z);
        }
    }

    private void updateVolumeRowSliderTintH(VolumeRow row, boolean isActive) {
        ColorStateList tint;
        if (isActive && this.mExpanded) {
            row.slider.requestFocus();
        }
        if (isActive && row.slider.isEnabled()) {
            tint = this.mActiveSliderTint;
        } else {
            tint = this.mInactiveSliderTint;
        }
        if (tint != row.cachedSliderTint) {
            row.cachedSliderTint = tint;
            row.slider.setProgressTintList(tint);
            row.slider.setThumbTintList(tint);
        }
    }

    private void updateVolumeRowSliderH(VolumeRow row, boolean enable, int vlevel) {
        row.slider.setEnabled(enable);
        updateVolumeRowSliderTintH(row, row.stream == this.mActiveStream);
        if (!row.tracking) {
            int progress = row.slider.getProgress();
            int level = getImpliedLevel(row.slider, progress);
            boolean rowVisible = row.view.getVisibility() == 0;
            boolean inGracePeriod = SystemClock.uptimeMillis() - row.userAttempt < 1000;
            this.mHandler.removeMessages(3, row);
            if (this.mShowing && rowVisible && inGracePeriod) {
                if (D.BUG) {
                    Log.d(TAG, "inGracePeriod");
                }
                this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(3, row), row.userAttempt + 1000);
            } else if (vlevel != level || !this.mShowing || !rowVisible) {
                int newProgress = vlevel * 100;
                if (progress != newProgress) {
                    if (!this.mShowing || !rowVisible) {
                        if (row.anim != null) {
                            row.anim.cancel();
                        }
                        row.slider.setProgress(newProgress);
                    } else if (row.anim == null || !row.anim.isRunning() || row.animTargetProgress != newProgress) {
                        if (row.anim == null) {
                            row.anim = ObjectAnimator.ofInt(row.slider, "progress", new int[]{progress, newProgress});
                            row.anim.setInterpolator(new DecelerateInterpolator());
                        } else {
                            row.anim.cancel();
                            row.anim.setIntValues(new int[]{progress, newProgress});
                        }
                        row.animTargetProgress = newProgress;
                        row.anim.setDuration(80);
                        row.anim.start();
                    }
                }
            }
        }
    }

    private void recheckH(VolumeRow row) {
        if (row == null) {
            if (D.BUG) {
                Log.d(TAG, "recheckH ALL");
            }
            trimObsoleteH();
            for (VolumeRow r : this.mRows) {
                updateVolumeRowH(r);
            }
            return;
        }
        if (D.BUG) {
            Log.d(TAG, "recheckH " + row.stream);
        }
        updateVolumeRowH(row);
    }

    private void setStreamImportantH(int stream, boolean important) {
        for (VolumeRow row : this.mRows) {
            if (row.stream == stream) {
                row.important = important;
                return;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void showSafetyWarningH(int flags) {
        if ((flags & 1025) != 0 || this.mShowing) {
            synchronized (this.mSafetyWarningLock) {
                if (this.mSafetyWarning != null) {
                    return;
                } else {
                    this.mSafetyWarning = new SafetyWarningDialog(this.mContext, this.mController.getAudioManager()) {
                        protected void cleanUp() {
                            synchronized (VolumeDialog.this.mSafetyWarningLock) {
                                VolumeDialog.this.mSafetyWarning = null;
                            }
                            VolumeDialog.this.recheckH(null);
                        }
                    };
                    this.mSafetyWarning.show();
                }
            }
        }
        rescheduleTimeoutH();
    }
}
