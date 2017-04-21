package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.keyguard.Clock.ClockView;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.util.Locale;

public class KeyguardStatusView extends GridLayout {
    private static boolean bA1Support = SystemProperties.get("ro.mtk_a1_feature").equals(FeatureOptionUtils.SUPPORT_YES);
    private final AlarmManager mAlarmManager;
    private TextView mAlarmStatusView;
    private TextClock mClockView;
    private TextClock mDateView;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private final LockPatternUtils mLockPatternUtils;
    private ClockView mMTKClockView;
    private TextView mOwnerInfo;

    private static final class Patterns {
        static String cacheKey;
        static String clockView12;
        static String clockView24;
        static String dateView;

        private Patterns() {
        }

        static void update(Context context, boolean hasAlarm) {
            int i;
            Locale locale = Locale.getDefault();
            Resources res = context.getResources();
            if (hasAlarm) {
                i = R$string.abbrev_wday_month_day_no_year_alarm;
            } else {
                i = R$string.abbrev_wday_month_day_no_year;
            }
            String dateViewSkel = res.getString(i);
            String clockView12Skel = res.getString(R$string.clock_12hr_format);
            String clockView24Skel = res.getString(R$string.clock_24hr_format);
            String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (!key.equals(cacheKey)) {
                dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);
                clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
                if (!clockView12Skel.contains("a")) {
                    clockView12 = clockView12.replaceAll("a", "").trim();
                }
                clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);
                clockView24 = clockView24.replace(':', '');
                clockView12 = clockView12.replace(':', '');
                cacheKey = key;
            }
        }
    }

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onTimeChanged() {
                KeyguardStatusView.this.refresh();
            }

            public void onKeyguardVisibilityChanged(boolean showing) {
                if (showing) {
                    Slog.v("KeyguardStatusView", "refresh statusview showing:" + showing);
                    KeyguardStatusView.this.refresh();
                    KeyguardStatusView.this.updateOwnerInfo();
                }
            }

            public void onStartedWakingUp() {
                KeyguardStatusView.this.setEnableMarquee(true);
            }

            public void onFinishedGoingToSleep(int why) {
                KeyguardStatusView.this.setEnableMarquee(false);
            }

            public void onUserSwitchComplete(int userId) {
                KeyguardStatusView.this.refresh();
                KeyguardStatusView.this.updateOwnerInfo();
            }
        };
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mLockPatternUtils = new LockPatternUtils(getContext());
    }

    private void setEnableMarquee(boolean enabled) {
        Log.v("KeyguardStatusView", (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (this.mAlarmStatusView != null) {
            this.mAlarmStatusView.setSelected(enabled);
        }
        if (this.mOwnerInfo != null) {
            this.mOwnerInfo.setSelected(enabled);
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        LinearLayout clockLayout = (LinearLayout) findViewById(R$id.keyguard_clock_container);
        LinearLayout mtkClockLayout = (LinearLayout) findViewById(R$id.mtk_keyguard_clock_container);
        if (bA1Support) {
            removeView(mtkClockLayout);
            this.mClockView = (TextClock) findViewById(R$id.clock_view);
        } else {
            removeView(clockLayout);
            this.mMTKClockView = (ClockView) findViewById(R$id.clock_view);
        }
        this.mAlarmStatusView = (TextView) findViewById(R$id.alarm_status);
        this.mDateView = (TextClock) findViewById(R$id.date_view);
        this.mOwnerInfo = (TextView) findViewById(R$id.owner_info);
        if (bA1Support) {
            this.mDateView.setShowCurrentUserTime(true);
            this.mClockView.setShowCurrentUserTime(true);
        }
        setEnableMarquee(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
        refresh();
        updateOwnerInfo();
        if (bA1Support) {
            this.mClockView.setElegantTextHeight(false);
        }
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (bA1Support) {
            this.mClockView.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.widget_big_font_size));
        }
        this.mDateView.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.widget_label_font_size));
        this.mOwnerInfo.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.widget_label_font_size));
    }

    public void refreshTime() {
        this.mDateView.setFormat24Hour(Patterns.dateView);
        this.mDateView.setFormat12Hour(Patterns.dateView);
        if (bA1Support) {
            this.mClockView.setFormat12Hour(Patterns.clockView12);
            this.mClockView.setFormat24Hour(Patterns.clockView24);
        } else if (this.mMTKClockView != null) {
            this.mMTKClockView.updateTime();
        }
    }

    private void refresh() {
        AlarmClockInfo nextAlarm = this.mAlarmManager.getNextAlarmClock(-2);
        Patterns.update(this.mContext, nextAlarm != null);
        refreshTime();
        refreshAlarmStatus(nextAlarm);
    }

    void refreshAlarmStatus(AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            this.mAlarmStatusView.setText(formatNextAlarm(this.mContext, nextAlarm));
            this.mAlarmStatusView.setContentDescription(getResources().getString(R$string.keyguard_accessibility_next_alarm, new Object[]{alarm}));
            this.mAlarmStatusView.setVisibility(0);
            return;
        }
        this.mAlarmStatusView.setVisibility(8);
    }

    public static String formatNextAlarm(Context context, AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton;
        if (DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())) {
            skeleton = "EHm";
        } else {
            skeleton = "Ehma";
        }
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton), info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (this.mOwnerInfo != null) {
            String ownerInfo = getOwnerInfo();
            if (TextUtils.isEmpty(ownerInfo)) {
                this.mOwnerInfo.setVisibility(8);
            } else {
                this.mOwnerInfo.setVisibility(0);
                this.mOwnerInfo.setText(ownerInfo);
            }
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
    }

    private String getOwnerInfo() {
        ContentResolver res = getContext().getContentResolver();
        if (this.mLockPatternUtils.isOwnerInfoEnabled(KeyguardUpdateMonitor.getCurrentUser())) {
            return this.mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
        }
        return null;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
