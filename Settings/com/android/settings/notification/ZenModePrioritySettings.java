package com.android.settings.notification;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.R;
import com.android.settings.search.Indexable;

public class ZenModePrioritySettings extends ZenModeSettingsBase implements Indexable {
    private DropDownPreference mCalls;
    private boolean mDisableListeners;
    private SwitchPreference mEvents;
    private DropDownPreference mMessages;
    private SwitchPreference mReminders;
    private SwitchPreference mRepeatCallers;

    class C04731 implements OnPreferenceChangeListener {
        C04731() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (ZenModePrioritySettings.this.mDisableListeners) {
                return true;
            }
            boolean val = ((Boolean) newValue).booleanValue();
            MetricsLogger.action(ZenModePrioritySettings.this.mContext, 167, val);
            if (val == ZenModePrioritySettings.this.mConfig.allowReminders) {
                return true;
            }
            if (ZenModePrioritySettings.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange allowReminders=" + val);
            }
            ZenModeConfig newConfig = ZenModePrioritySettings.this.mConfig.copy();
            newConfig.allowReminders = val;
            return ZenModePrioritySettings.this.setZenModeConfig(newConfig);
        }
    }

    class C04742 implements OnPreferenceChangeListener {
        C04742() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (ZenModePrioritySettings.this.mDisableListeners) {
                return true;
            }
            boolean val = ((Boolean) newValue).booleanValue();
            MetricsLogger.action(ZenModePrioritySettings.this.mContext, 168, val);
            if (val == ZenModePrioritySettings.this.mConfig.allowEvents) {
                return true;
            }
            if (ZenModePrioritySettings.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange allowEvents=" + val);
            }
            ZenModeConfig newConfig = ZenModePrioritySettings.this.mConfig.copy();
            newConfig.allowEvents = val;
            return ZenModePrioritySettings.this.setZenModeConfig(newConfig);
        }
    }

    class C04753 implements Callback {
        C04753() {
        }

        public boolean onItemSelected(int pos, Object newValue) {
            if (ZenModePrioritySettings.this.mDisableListeners) {
                return true;
            }
            int val = ((Integer) newValue).intValue();
            MetricsLogger.action(ZenModePrioritySettings.this.mContext, 169, val);
            boolean allowMessages = val != -1;
            int allowMessagesFrom = val == -1 ? ZenModePrioritySettings.this.mConfig.allowMessagesFrom : val;
            if (allowMessages == ZenModePrioritySettings.this.mConfig.allowMessages && allowMessagesFrom == ZenModePrioritySettings.this.mConfig.allowMessagesFrom) {
                return true;
            }
            if (ZenModePrioritySettings.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange allowMessages=" + allowMessages + " allowMessagesFrom=" + ZenModeConfig.sourceToString(allowMessagesFrom));
            }
            ZenModeConfig newConfig = ZenModePrioritySettings.this.mConfig.copy();
            newConfig.allowMessages = allowMessages;
            newConfig.allowMessagesFrom = allowMessagesFrom;
            return ZenModePrioritySettings.this.setZenModeConfig(newConfig);
        }
    }

    class C04764 implements Callback {
        C04764() {
        }

        public boolean onItemSelected(int pos, Object newValue) {
            if (ZenModePrioritySettings.this.mDisableListeners) {
                return true;
            }
            int val = ((Integer) newValue).intValue();
            MetricsLogger.action(ZenModePrioritySettings.this.mContext, 170, val);
            boolean allowCalls = val != -1;
            int allowCallsFrom = val == -1 ? ZenModePrioritySettings.this.mConfig.allowCallsFrom : val;
            if (allowCalls == ZenModePrioritySettings.this.mConfig.allowCalls && allowCallsFrom == ZenModePrioritySettings.this.mConfig.allowCallsFrom) {
                return true;
            }
            if (ZenModePrioritySettings.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange allowCalls=" + allowCalls + " allowCallsFrom=" + ZenModeConfig.sourceToString(allowCallsFrom));
            }
            ZenModeConfig newConfig = ZenModePrioritySettings.this.mConfig.copy();
            newConfig.allowCalls = allowCalls;
            newConfig.allowCallsFrom = allowCallsFrom;
            return ZenModePrioritySettings.this.setZenModeConfig(newConfig);
        }
    }

    class C04775 implements OnPreferenceChangeListener {
        C04775() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (ZenModePrioritySettings.this.mDisableListeners) {
                return true;
            }
            boolean val = ((Boolean) newValue).booleanValue();
            MetricsLogger.action(ZenModePrioritySettings.this.mContext, 171, val);
            if (val == ZenModePrioritySettings.this.mConfig.allowRepeatCallers) {
                return true;
            }
            if (ZenModePrioritySettings.DEBUG) {
                Log.d("ZenModeSettings", "onPrefChange allowRepeatCallers=" + val);
            }
            ZenModeConfig newConfig = ZenModePrioritySettings.this.mConfig.copy();
            newConfig.allowRepeatCallers = val;
            return ZenModePrioritySettings.this.setZenModeConfig(newConfig);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_priority_settings);
        PreferenceScreen root = getPreferenceScreen();
        this.mReminders = (SwitchPreference) root.findPreference("reminders");
        this.mReminders.setOnPreferenceChangeListener(new C04731());
        this.mEvents = (SwitchPreference) root.findPreference("events");
        this.mEvents.setOnPreferenceChangeListener(new C04742());
        this.mMessages = (DropDownPreference) root.findPreference("messages");
        addSources(this.mMessages);
        this.mMessages.setCallback(new C04753());
        this.mCalls = (DropDownPreference) root.findPreference("calls");
        addSources(this.mCalls);
        this.mCalls.setCallback(new C04764());
        this.mRepeatCallers = (SwitchPreference) root.findPreference("repeat_callers");
        this.mRepeatCallers.setSummary(this.mContext.getString(R.string.zen_mode_repeat_callers_summary, new Object[]{Integer.valueOf(this.mContext.getResources().getInteger(17694864))}));
        this.mRepeatCallers.setOnPreferenceChangeListener(new C04775());
        updateControls();
    }

    protected void onZenModeChanged() {
    }

    protected void onZenModeConfigChanged() {
        updateControls();
    }

    private void updateControls() {
        boolean z;
        int i = -1;
        this.mDisableListeners = true;
        if (this.mCalls != null) {
            int i2;
            DropDownPreference dropDownPreference = this.mCalls;
            if (this.mConfig.allowCalls) {
                i2 = this.mConfig.allowCallsFrom;
            } else {
                i2 = -1;
            }
            dropDownPreference.setSelectedValue(Integer.valueOf(i2));
        }
        DropDownPreference dropDownPreference2 = this.mMessages;
        if (this.mConfig.allowMessages) {
            i = this.mConfig.allowMessagesFrom;
        }
        dropDownPreference2.setSelectedValue(Integer.valueOf(i));
        this.mReminders.setChecked(this.mConfig.allowReminders);
        this.mEvents.setChecked(this.mConfig.allowEvents);
        this.mRepeatCallers.setChecked(this.mConfig.allowRepeatCallers);
        SwitchPreference switchPreference = this.mRepeatCallers;
        if (!this.mConfig.allowCalls) {
            z = true;
        } else if (this.mConfig.allowCallsFrom != 0) {
            z = true;
        } else {
            z = false;
        }
        switchPreference.setEnabled(z);
        this.mDisableListeners = false;
    }

    protected int getMetricsCategory() {
        return 141;
    }

    private static void addSources(DropDownPreference pref) {
        pref.addItem((int) R.string.zen_mode_from_anyone, Integer.valueOf(0));
        pref.addItem((int) R.string.zen_mode_from_contacts, Integer.valueOf(1));
        pref.addItem((int) R.string.zen_mode_from_starred, Integer.valueOf(2));
        pref.addItem((int) R.string.zen_mode_from_none, Integer.valueOf(-1));
    }
}
