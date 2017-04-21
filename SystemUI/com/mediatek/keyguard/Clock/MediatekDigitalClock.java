package com.mediatek.keyguard.Clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings.System;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.keyguard.R$id;
import com.mediatek.keyguard.PowerOffAlarm.Alarms;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

public class MediatekDigitalClock extends LinearLayout {
    private AmPm mAmPm;
    private boolean mAttached;
    private Calendar mCalendar;
    private ContentObserver mFormatChangeObserver;
    private final Handler mHandler;
    private String mHoursFormat;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mLive;
    private TextView mTimeDisplayHours;
    private TextView mTimeDisplayMinutes;
    private String mTimeZoneId;

    static class AmPm {
        private final TextView mAmPm;
        private final String mAmString;
        private final String mPmString;

        AmPm(View parent) {
            this.mAmPm = (TextView) parent.findViewById(R$id.am_pm);
            this.mAmPm.setPadding(0, 4, 0, 0);
            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            this.mAmString = ampm[0];
            this.mPmString = ampm[1];
        }

        void setShowAmPm(boolean show) {
            this.mAmPm.setVisibility(show ? 0 : 8);
        }

        void setIsMorning(boolean isMorning) {
            this.mAmPm.setText(isMorning ? this.mAmString : this.mPmString);
        }

        CharSequence getAmPmText() {
            return this.mAmPm.getText();
        }
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            MediatekDigitalClock.this.setDateFormat();
            MediatekDigitalClock.this.updateTime();
        }
    }

    public MediatekDigitalClock(Context context) {
        this(context, null);
    }

    public MediatekDigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLive = true;
        this.mHandler = new Handler();
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (MediatekDigitalClock.this.mLive && intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) {
                    MediatekDigitalClock.this.mCalendar = Calendar.getInstance();
                }
                MediatekDigitalClock.this.mHandler.post(new Runnable() {
                    public void run() {
                        MediatekDigitalClock.this.updateTime();
                    }
                });
            }
        };
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTimeDisplayHours = (TextView) findViewById(R$id.timeDisplayHours);
        this.mTimeDisplayMinutes = (TextView) findViewById(R$id.timeDisplayMinutes);
        this.mAmPm = new AmPm(this);
        this.mCalendar = Calendar.getInstance();
        setDateFormat();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.v("PowerOffAlarm", "onAttachedToWindow " + this);
        if (!this.mAttached) {
            this.mAttached = true;
            if (this.mLive) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_TICK");
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                getContext().registerReceiver(this.mIntentReceiver, filter);
            }
            this.mFormatChangeObserver = new FormatChangeObserver();
            getContext().getContentResolver().registerContentObserver(System.CONTENT_URI, true, this.mFormatChangeObserver);
            updateTime();
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            this.mAttached = false;
            if (this.mLive) {
                getContext().unregisterReceiver(this.mIntentReceiver);
            }
            getContext().getContentResolver().unregisterContentObserver(this.mFormatChangeObserver);
        }
    }

    private void updateTime() {
        if (this.mLive) {
            this.mCalendar.setTimeInMillis(System.currentTimeMillis());
        }
        if (this.mTimeZoneId != null) {
            this.mCalendar.setTimeZone(TimeZone.getTimeZone(this.mTimeZoneId));
        }
        StringBuilder fullTimeStr = new StringBuilder();
        CharSequence newTime = DateFormat.format(this.mHoursFormat, this.mCalendar);
        this.mTimeDisplayHours.setText(newTime);
        fullTimeStr.append(newTime);
        newTime = DateFormat.format(":mm", this.mCalendar);
        fullTimeStr.append(newTime);
        this.mTimeDisplayMinutes.setText(newTime);
        this.mAmPm.setIsMorning(this.mCalendar.get(9) == 0);
        if (!Alarms.get24HourMode(getContext())) {
            fullTimeStr.append(this.mAmPm.getAmPmText());
        }
        setContentDescription(fullTimeStr);
    }

    private void setDateFormat() {
        this.mHoursFormat = Alarms.get24HourMode(getContext()) ? "kk" : "h";
        this.mAmPm.setShowAmPm(!Alarms.get24HourMode(getContext()));
    }
}
