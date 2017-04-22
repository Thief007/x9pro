package com.android.settings.notification;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

public class RedactionInterstitial extends SettingsActivity {

    public static class RedactionInterstitialFragment extends SettingsPreferenceFragment implements OnCheckedChangeListener {
        private RadioGroup mRadioGroup;
        private RadioButton mRedactSensitiveButton;
        private RadioButton mShowAllButton;

        protected int getMetricsCategory() {
            return 74;
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.redaction_interstitial, container, false);
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            this.mRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
            this.mShowAllButton = (RadioButton) view.findViewById(R.id.show_all);
            this.mRedactSensitiveButton = (RadioButton) view.findViewById(R.id.redact_sensitive);
            this.mRadioGroup.setOnCheckedChangeListener(this);
            if (RedactionInterstitial.isSecureNotificationsDisabled(getActivity())) {
                this.mShowAllButton.setEnabled(false);
                this.mRedactSensitiveButton.setEnabled(false);
            } else if (RedactionInterstitial.isUnredactedNotificationsDisabled(getActivity())) {
                this.mShowAllButton.setEnabled(false);
            }
        }

        public void onResume() {
            super.onResume();
            loadFromSettings();
        }

        private void loadFromSettings() {
            boolean enabled = Secure.getInt(getContentResolver(), "lock_screen_show_notifications", 0) != 0;
            boolean show = Secure.getInt(getContentResolver(), "lock_screen_allow_private_notifications", 1) != 0;
            int checkedButtonId = R.id.hide_all;
            if (enabled) {
                if (show && this.mShowAllButton.isEnabled()) {
                    checkedButtonId = R.id.show_all;
                } else if (this.mRedactSensitiveButton.isEnabled()) {
                    checkedButtonId = R.id.redact_sensitive;
                }
            }
            this.mRadioGroup.check(checkedButtonId);
        }

        public void onCheckedChanged(RadioGroup group, int checkedId) {
            int i;
            int i2 = 1;
            boolean show = checkedId == R.id.show_all;
            boolean enabled = checkedId != R.id.hide_all;
            ContentResolver contentResolver = getContentResolver();
            String str = "lock_screen_allow_private_notifications";
            if (show) {
                i = 1;
            } else {
                i = 0;
            }
            Secure.putInt(contentResolver, str, i);
            ContentResolver contentResolver2 = getContentResolver();
            String str2 = "lock_screen_show_notifications";
            if (!enabled) {
                i2 = 0;
            }
            Secure.putInt(contentResolver2, str2, i2);
        }
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", RedactionInterstitialFragment.class.getName());
        return modIntent;
    }

    protected boolean isValidFragment(String fragmentName) {
        return RedactionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static Intent createStartIntent(Context ctx) {
        if (isSecureNotificationsDisabled(ctx)) {
            return null;
        }
        return new Intent(ctx, RedactionInterstitial.class).putExtra("extra_prefs_show_button_bar", true).putExtra("extra_prefs_set_back_text", (String) null).putExtra("extra_prefs_set_next_text", ctx.getString(R.string.app_notifications_dialog_done)).putExtra(":settings:show_fragment_title_resid", R.string.lock_screen_notifications_interstitial_title);
    }

    private static boolean isSecureNotificationsDisabled(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null || (dpm.getKeyguardDisabledFeatures(null) & 4) == 0) {
            return false;
        }
        return true;
    }

    private static boolean isUnredactedNotificationsDisabled(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null || (dpm.getKeyguardDisabledFeatures(null) & 8) == 0) {
            return false;
        }
        return true;
    }
}
