package com.android.settings.notification;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceScreen;
import android.provider.CalendarContract.Calendars;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ZenModeEventRuleSettings extends ZenModeRuleSettingsBase {
    private static final Comparator<CalendarInfo> CALENDAR_NAME = new C04691();
    private DropDownPreference mCalendar;
    private List<CalendarInfo> mCalendars;
    private boolean mCreate;
    private EventInfo mEvent;
    private DropDownPreference mReply;

    static class C04691 implements Comparator<CalendarInfo> {
        C04691() {
        }

        public int compare(CalendarInfo lhs, CalendarInfo rhs) {
            return lhs.name.compareTo(rhs.name);
        }
    }

    class C04702 implements Callback {
        C04702() {
        }

        public boolean onItemSelected(int pos, Object value) {
            String calendarKey = (String) value;
            if (calendarKey.equals(ZenModeEventRuleSettings.key(ZenModeEventRuleSettings.this.mEvent))) {
                return true;
            }
            int i = calendarKey.indexOf(58);
            ZenModeEventRuleSettings.this.mEvent.userId = Integer.parseInt(calendarKey.substring(0, i));
            ZenModeEventRuleSettings.this.mEvent.calendar = calendarKey.substring(i + 1);
            if (ZenModeEventRuleSettings.this.mEvent.calendar.isEmpty()) {
                ZenModeEventRuleSettings.this.mEvent.calendar = null;
            }
            ZenModeEventRuleSettings.this.updateRule(ZenModeConfig.toEventConditionId(ZenModeEventRuleSettings.this.mEvent));
            return true;
        }
    }

    class C04713 implements Callback {
        C04713() {
        }

        public boolean onItemSelected(int pos, Object value) {
            int reply = ((Integer) value).intValue();
            if (reply == ZenModeEventRuleSettings.this.mEvent.reply) {
                return true;
            }
            ZenModeEventRuleSettings.this.mEvent.reply = reply;
            ZenModeEventRuleSettings.this.updateRule(ZenModeConfig.toEventConditionId(ZenModeEventRuleSettings.this.mEvent));
            return true;
        }
    }

    public static class CalendarInfo {
        public String name;
        public int userId;
    }

    protected boolean setRule(ZenRule rule) {
        EventInfo eventInfo = null;
        if (rule != null) {
            eventInfo = ZenModeConfig.tryParseEventConditionId(rule.conditionId);
        }
        this.mEvent = eventInfo;
        return this.mEvent != null;
    }

    protected String getZenModeDependency() {
        return null;
    }

    protected int getEnabledToastText() {
        return R.string.zen_event_rule_enabled_toast;
    }

    public void onResume() {
        super.onResume();
        if (!this.mCreate) {
            reloadCalendar();
        }
        this.mCreate = false;
    }

    private void reloadCalendar() {
        this.mCalendars = getCalendars(this.mContext);
        this.mCalendar.clearItems();
        this.mCalendar.addItem((int) R.string.zen_mode_event_rule_calendar_any, key(0, null));
        String str = this.mEvent != null ? this.mEvent.calendar : null;
        boolean found = false;
        for (CalendarInfo calendar : this.mCalendars) {
            this.mCalendar.addItem(calendar.name, key(calendar));
            if (str != null && str.equals(calendar.name)) {
                found = true;
            }
        }
        if (str != null && !found) {
            this.mCalendar.addItem(str, key(this.mEvent.userId, str));
        }
    }

    protected void onCreateInternal() {
        this.mCreate = true;
        addPreferencesFromResource(R.xml.zen_mode_event_rule_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mCalendar = (DropDownPreference) root.findPreference("calendar");
        this.mCalendar.setCallback(new C04702());
        this.mReply = (DropDownPreference) root.findPreference("reply");
        this.mReply.addItem((int) R.string.zen_mode_event_rule_reply_any_except_no, Integer.valueOf(0));
        this.mReply.addItem((int) R.string.zen_mode_event_rule_reply_yes_or_maybe, Integer.valueOf(1));
        this.mReply.addItem((int) R.string.zen_mode_event_rule_reply_yes, Integer.valueOf(2));
        this.mReply.setCallback(new C04713());
        reloadCalendar();
        updateControlsInternal();
    }

    protected void updateControlsInternal() {
        this.mCalendar.setSelectedValue(key(this.mEvent));
        this.mReply.setSelectedValue(Integer.valueOf(this.mEvent.reply));
    }

    protected int getMetricsCategory() {
        return 146;
    }

    private static List<CalendarInfo> getCalendars(Context context) {
        List<CalendarInfo> calendars = new ArrayList();
        for (UserHandle user : UserManager.get(context).getUserProfiles()) {
            Context userContext = getContextForUser(context, user);
            if (userContext != null) {
                addCalendars(userContext, calendars);
            }
        }
        Collections.sort(calendars, CALENDAR_NAME);
        return calendars;
    }

    private static Context getContextForUser(Context context, UserHandle user) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public static void addCalendars(Context context, List<CalendarInfo> outCalendars) {
        String primary = "\"primary\"";
        String selection = "\"primary\" = 1";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Calendars.CONTENT_URI, new String[]{"_id", "calendar_displayName", "(account_name=ownerAccount) AS \"primary\""}, "\"primary\" = 1", null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    CalendarInfo ci = new CalendarInfo();
                    ci.name = cursor.getString(1);
                    ci.userId = context.getUserId();
                    outCalendars.add(ci);
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static String key(CalendarInfo calendar) {
        return key(calendar.userId, calendar.name);
    }

    private static String key(EventInfo event) {
        return key(event.userId, event.calendar);
    }

    private static String key(int userId, String calendar) {
        StringBuilder append = new StringBuilder().append(EventInfo.resolveUserId(userId)).append(":");
        if (calendar == null) {
            calendar = "";
        }
        return append.append(calendar).toString();
    }
}
