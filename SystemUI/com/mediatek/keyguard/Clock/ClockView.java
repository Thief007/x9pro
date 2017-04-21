package com.mediatek.keyguard.Clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.keyguard.R$id;
import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;
import libcore.icu.LocaleData;

public class ClockView extends RelativeLayout {
    private static Typeface mClockTypeface = null;
    private AmPm mAmPm;
    private int mAttached;
    private Calendar mCalendar;
    private String mFormat;
    private ContentObserver mFormatChangeObserver;
    private final Handler mHandler;
    private BroadcastReceiver mIntentReceiver;
    private TextView mTimeView;

    static class AmPm {
        private TextView mAmPmTextView;
        private String mAmString;
        private String mPmString;

        AmPm(View parent, Typeface tf) {
            this.mAmPmTextView = (TextView) parent.findViewById(R$id.am_pm);
            if (!(this.mAmPmTextView == null || tf == null)) {
                this.mAmPmTextView.setTypeface(tf);
            }
            updateAmPmText();
        }

        void setShowAmPm(boolean show) {
            if (this.mAmPmTextView != null) {
                this.mAmPmTextView.setVisibility(show ? 0 : 8);
            }
        }

        void setIsMorning(boolean isMorning) {
            if (this.mAmPmTextView != null) {
                this.mAmPmTextView.setText(isMorning ? this.mAmString : this.mPmString);
            }
        }

        void updateAmPmText() {
            Log.d("ClockView", "updateAmPmText() enters.");
            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            this.mAmString = ampm[0];
            this.mPmString = ampm[1];
            Log.d("ClockView", "mAmString = " + this.mAmString + " mPmString = " + this.mPmString);
        }
    }

    private static class FormatChangeObserver extends ContentObserver {
        private WeakReference<ClockView> mClock;
        private Context mContext;

        public FormatChangeObserver(ClockView clock) {
            super(new Handler());
            this.mClock = new WeakReference(clock);
            this.mContext = clock.getContext();
        }

        public void onChange(boolean selfChange) {
            ClockView digitalClock = (ClockView) this.mClock.get();
            if (digitalClock != null) {
                digitalClock.setDateFormat();
                digitalClock.updateTime();
                return;
            }
            try {
                this.mContext.getContentResolver().unregisterContentObserver(this);
            } catch (IllegalStateException e) {
                Log.e("ClockView", "onChange() - unregisterContentObserver() fails.");
            }
        }
    }

    private static class TimeChangedReceiver extends BroadcastReceiver {
        private WeakReference<ClockView> mClock;
        private Context mContext;

        public TimeChangedReceiver(ClockView clock) {
            this.mClock = new WeakReference(clock);
            this.mContext = clock.getContext();
        }

        public void onReceive(Context context, Intent intent) {
            final boolean timezoneChanged = intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED");
            final ClockView clock = (ClockView) this.mClock.get();
            if (clock != null) {
                clock.mHandler.post(new Runnable() {
                    public void run() {
                        if (timezoneChanged) {
                            clock.mCalendar = Calendar.getInstance();
                        }
                        clock.updateTime();
                    }
                });
                return;
            }
            try {
                this.mContext.unregisterReceiver(this);
            } catch (IllegalArgumentException e) {
                Log.e("ClockView", "onReceive() - unregisterReceiver(this) fails");
            }
        }
    }

    public ClockView(Context context) {
        this(context, null);
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAttached = 0;
        this.mHandler = new Handler();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTimeView = (TextView) findViewById(R$id.clock_text);
        this.mTimeView.setTypeface(getClockTypeface());
        this.mAmPm = new AmPm(this, null);
        this.mCalendar = Calendar.getInstance();
        setDateFormat();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttached++;
        if (this.mIntentReceiver == null) {
            this.mIntentReceiver = new TimeChangedReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.OWNER, filter, null, null);
        }
        if (this.mFormatChangeObserver == null) {
            this.mFormatChangeObserver = new FormatChangeObserver(this);
            this.mContext.getContentResolver().registerContentObserver(System.CONTENT_URI, true, this.mFormatChangeObserver);
        }
        this.mCalendar = Calendar.getInstance(TimeZone.getDefault());
        updateTime();
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAttached--;
        if (this.mIntentReceiver != null) {
            this.mContext.unregisterReceiver(this.mIntentReceiver);
        }
        if (this.mFormatChangeObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mFormatChangeObserver);
        }
        this.mFormatChangeObserver = null;
        this.mIntentReceiver = null;
    }

    public void updateTime() {
        boolean z = false;
        this.mCalendar.setTimeInMillis(System.currentTimeMillis());
        this.mTimeView.setText(DateFormat.format(this.mFormat, this.mCalendar));
        AmPm amPm = this.mAmPm;
        if (this.mCalendar.get(9) == 0) {
            z = true;
        }
        amPm.setIsMorning(z);
    }

    private void setDateFormat() {
        boolean z;
        LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
        boolean isTimeFormat24 = DateFormat.is24HourFormat(getContext());
        this.mFormat = isTimeFormat24 ? ld.timeFormat_Hm : ld.timeFormat_hm;
        if (this.mFormat == null) {
            this.mFormat = isTimeFormat24 ? "kk:mm" : "h:mm";
        } else {
            this.mFormat = this.mFormat.replace(" ", "");
            this.mFormat = this.mFormat.replace("a", "");
        }
        AmPm amPm = this.mAmPm;
        if (isTimeFormat24) {
            z = false;
        } else {
            z = true;
        }
        amPm.setShowAmPm(z);
    }

    private Typeface getClockTypeface() {
        if (mClockTypeface == null) {
            mClockTypeface = Typeface.createFromFile("/system/fonts/AndroidClock.ttf");
        }
        return mClockTypeface;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mAmPm.updateAmPmText();
        setDateFormat();
    }
}
