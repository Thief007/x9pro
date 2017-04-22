package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import java.util.ArrayList;
import java.util.List;

public class ScreenPinningSettings extends SettingsPreferenceFragment implements OnSwitchChangeListener, Indexable {
    private static final CharSequence KEY_USE_SCREEN_LOCK = "use_screen_lock";
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C01771();
    private LockPatternUtils mLockPatternUtils;
    private SwitchBar mSwitchBar;
    private SwitchPreference mUseScreenLock;

    static class C01771 extends BaseSearchIndexProvider {
        C01771() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.screen_pinning_title);
            data.screenTitle = res.getString(R.string.screen_pinning_title);
            result.add(data);
            if (ScreenPinningSettings.isLockToAppEnabled(context)) {
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.screen_pinning_unlock_none);
                data.screenTitle = res.getString(R.string.screen_pinning_title);
                result.add(data);
            } else {
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.screen_pinning_description);
                data.screenTitle = res.getString(R.string.screen_pinning_title);
                result.add(data);
            }
            return result;
        }
    }

    class C01782 implements OnPreferenceChangeListener {
        C01782() {
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return ScreenPinningSettings.this.setScreenLockUsed(((Boolean) newValue).booleanValue());
        }
    }

    protected int getMetricsCategory() {
        return 86;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mLockPatternUtils = new LockPatternUtils(activity);
        View emptyView = LayoutInflater.from(activity).inflate(R.layout.screen_pinning_instructions, null);
        ((ViewGroup) getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
        this.mSwitchBar.setChecked(isLockToAppEnabled(getActivity()));
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    private static boolean isLockToAppEnabled(Context context) {
        return System.getInt(context.getContentResolver(), "lock_to_app_enabled", 0) != 0;
    }

    private void setLockToAppEnabled(boolean isEnabled) {
        System.putInt(getContentResolver(), "lock_to_app_enabled", isEnabled ? 1 : 0);
        if (isEnabled) {
            setScreenLockUsedSetting(isScreenLockUsed());
        }
    }

    private boolean isScreenLockUsed() {
        if (Secure.getInt(getContentResolver(), "lock_to_app_exit_locked", getCurrentSecurityTitle() != R.string.screen_pinning_unlock_none ? 1 : 0) != 0) {
            return true;
        }
        return false;
    }

    private boolean setScreenLockUsed(boolean isEnabled) {
        if (isEnabled && new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality(UserHandle.myUserId()) == 0) {
            Intent chooseLockIntent = new Intent("android.app.action.SET_NEW_PASSWORD");
            chooseLockIntent.putExtra("minimum_quality", 65536);
            startActivityForResult(chooseLockIntent, 43);
            return false;
        }
        setScreenLockUsedSetting(isEnabled);
        return true;
    }

    private void setScreenLockUsedSetting(boolean isEnabled) {
        Secure.putInt(getContentResolver(), "lock_to_app_exit_locked", isEnabled ? 1 : 0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 43) {
            boolean validPassQuality = new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality(UserHandle.myUserId()) != 0;
            setScreenLockUsed(validPassQuality);
            this.mUseScreenLock.setChecked(validPassQuality);
        }
    }

    private int getCurrentSecurityTitle() {
        switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
            case 65536:
                if (this.mLockPatternUtils.isLockPatternEnabled(UserHandle.myUserId())) {
                    return R.string.screen_pinning_unlock_pattern;
                }
                break;
            case 131072:
            case 196608:
                return R.string.screen_pinning_unlock_pin;
            case 262144:
            case 327680:
            case 393216:
                return R.string.screen_pinning_unlock_password;
        }
        return R.string.screen_pinning_unlock_none;
    }

    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setLockToAppEnabled(isChecked);
        updateDisplay();
    }

    public void updateDisplay() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        if (isLockToAppEnabled(getActivity())) {
            addPreferencesFromResource(R.xml.screen_pinning_settings);
            this.mUseScreenLock = (SwitchPreference) getPreferenceScreen().findPreference(KEY_USE_SCREEN_LOCK);
            this.mUseScreenLock.setOnPreferenceChangeListener(new C01782());
            boolean isScreenLockUsed = isScreenLockUsed();
            this.mUseScreenLock.setChecked(isScreenLockUsed);
            setScreenLockUsed(isScreenLockUsed);
            this.mUseScreenLock.setTitle(getCurrentSecurityTitle());
        }
    }
}
