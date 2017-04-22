package com.android.settings;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.security.KeyStore;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.ListView;
import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.settings.FeatureOption;
import java.util.List;

public class ChooseLockGeneric extends SettingsActivity {

    public static class ChooseLockGenericFragment extends SettingsPreferenceFragment {
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private DevicePolicyManager mDPM;
        private boolean mEncryptionRequestDisabled;
        private int mEncryptionRequestQuality;
        private FingerprintManager mFingerprintManager;
        private boolean mForFingerprint = false;
        private boolean mHasChallenge = false;
        private KeyStore mKeyStore;
        private LockPatternUtils mLockPatternUtils;
        private boolean mPasswordConfirmed = false;
        private RemovalCallback mRemovalCallback = new C00541();
        private boolean mRequirePassword;
        private String mUserPassword;
        private boolean mWaitingForConfirmation = false;

        class C00541 extends RemovalCallback {
            C00541() {
            }

            public void onRemovalSucceeded(Fingerprint fingerprint) {
                Log.v("ChooseLockGenericFragment", "Fingerprint removed: " + fingerprint.getFingerId());
                if (ChooseLockGenericFragment.this.mFingerprintManager.getEnrolledFingerprints().size() == 0) {
                    ChooseLockGenericFragment.this.finish();
                }
            }

            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                if (ChooseLockGenericFragment.this.getActivity() != null) {
                    Toast.makeText(ChooseLockGenericFragment.this.getActivity(), errString, 0);
                }
                ChooseLockGenericFragment.this.finish();
            }
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {

            class C00562 implements OnClickListener {
                C00562() {
                }

                public void onClick(DialogInterface dialog, int whichButton) {
                    FactoryResetProtectionWarningDialog.this.dismiss();
                }
            }

            public static FactoryResetProtectionWarningDialog newInstance(int messageRes, String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag = new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt("messageRes", messageRes);
                args.putString("unlockMethodToSet", unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) == null) {
                    super.show(manager, tag);
                }
            }

            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();
                return new Builder(getActivity()).setTitle(R.string.unlock_disable_frp_warning_title).setMessage(args.getInt("messageRes")).setPositiveButton(R.string.unlock_disable_frp_warning_ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ((ChooseLockGenericFragment) FactoryResetProtectionWarningDialog.this.getParentFragment()).setUnlockMethod(args.getString("unlockMethodToSet"));
                    }
                }).setNegativeButton(R.string.cancel, new C00562()).create();
            }
        }

        protected int getMetricsCategory() {
            return 27;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mFingerprintManager = (FingerprintManager) getActivity().getSystemService("fingerprint");
            this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
            this.mKeyStore = KeyStore.getInstance();
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            boolean confirmCredentials = getActivity().getIntent().getBooleanExtra("confirm_credentials", true);
            if (getActivity() instanceof InternalActivity) {
                this.mPasswordConfirmed = !confirmCredentials;
            }
            this.mHasChallenge = getActivity().getIntent().getBooleanExtra("has_challenge", false);
            this.mChallenge = getActivity().getIntent().getLongExtra("challenge", 0);
            this.mForFingerprint = getActivity().getIntent().getBooleanExtra("for_fingerprint", false);
            if (savedInstanceState != null) {
                this.mPasswordConfirmed = savedInstanceState.getBoolean("password_confirmed");
                this.mWaitingForConfirmation = savedInstanceState.getBoolean("waiting_for_confirmation");
                this.mEncryptionRequestQuality = savedInstanceState.getInt("encrypt_requested_quality");
                this.mEncryptionRequestDisabled = savedInstanceState.getBoolean("encrypt_requested_disabled");
            }
            if (this.mPasswordConfirmed) {
                updatePreferencesOrFinish();
            } else if (!this.mWaitingForConfirmation) {
                if (new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(100, getString(R.string.unlock_set_unlock_launch_picker_title), true)) {
                    this.mWaitingForConfirmation = true;
                    return;
                }
                this.mPasswordConfirmed = true;
                updatePreferencesOrFinish();
            }
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (this.mForFingerprint) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                ListView listView = getListView();
                listView.addHeaderView(inflater.inflate(R.layout.choose_lock_generic_fingerprint_header, listView, false), null, false);
            }
        }

        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (isUnlockMethodSecure(key) || !this.mLockPatternUtils.isSecure(UserHandle.myUserId())) {
                return setUnlockMethod(key);
            }
            showFactoryResetProtectionWarningDialog(key);
            return true;
        }

        private void maybeEnableEncryption(int quality, boolean disabled) {
            boolean z = false;
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
            if (Process.myUserHandle().isOwner() && LockPatternUtils.isDeviceEncryptionEnabled() && !dpm.getDoNotAskCredentialsOnBoot()) {
                this.mEncryptionRequestQuality = quality;
                this.mEncryptionRequestDisabled = disabled;
                Context context = getActivity();
                boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
                if (!accEn) {
                    z = true;
                }
                Intent intent = getEncryptionInterstitialIntent(context, quality, lockPatternUtils.isCredentialRequiredToDecrypt(z));
                intent.putExtra("for_fingerprint", this.mForFingerprint);
                startActivityForResult(intent, 101);
                return;
            }
            this.mRequirePassword = false;
            updateUnlockMethodAndFinish(quality, disabled);
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = super.onCreateView(inflater, container, savedInstanceState);
            if (getActivity().getIntent().getBooleanExtra("lockscreen.weak_fallback", false)) {
                View header = null;
                if (getActivity().getIntent().getStringExtra("lockscreen.weak_fallback_for").equals("voice_unlock")) {
                    header = View.inflate(getActivity(), R.layout.weak_voice_fallback_header, null);
                }
                ((ListView) v.findViewById(16908298)).addHeaderView(header, null, false);
            }
            return v;
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            this.mWaitingForConfirmation = false;
            if (requestCode == 100 && resultCode == -1) {
                this.mPasswordConfirmed = true;
                this.mUserPassword = data.getStringExtra("password");
                updatePreferencesOrFinish();
            } else if (requestCode == 101 && resultCode == -1) {
                this.mRequirePassword = data.getBooleanExtra("extra_require_password", true);
                updateUnlockMethodAndFinish(this.mEncryptionRequestQuality, this.mEncryptionRequestDisabled);
            } else if (requestCode == 102) {
                getActivity().setResult(resultCode, data);
                finish();
            } else if (requestCode == 101) {
                getActivity().setResult(resultCode);
                finish();
            } else {
                getActivity().setResult(0);
                finish();
            }
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean("password_confirmed", this.mPasswordConfirmed);
            outState.putBoolean("waiting_for_confirmation", this.mWaitingForConfirmation);
            outState.putInt("encrypt_requested_quality", this.mEncryptionRequestQuality);
            outState.putBoolean("encrypt_requested_disabled", this.mEncryptionRequestDisabled);
        }

        private void updatePreferencesOrFinish() {
            Intent intent = getActivity().getIntent();
            int quality = intent.getIntExtra("lockscreen.password_type", -1);
            if (quality == -1) {
                quality = upgradeQuality(intent.getIntExtra("minimum_quality", -1));
                boolean hideDisabledPrefs = intent.getBooleanExtra("hide_disabled_prefs", false);
                PreferenceScreen prefScreen = getPreferenceScreen();
                if (prefScreen != null) {
                    prefScreen.removeAll();
                }
                addPreferencesFromResource(R.xml.security_settings_picker);
                disableUnusablePreferences(quality, hideDisabledPrefs);
                updateCurrentPreference();
                updatePreferenceSummaryIfNeeded();
                return;
            }
            updateUnlockMethodAndFinish(quality, false);
        }

        private void updateCurrentPreference() {
            Preference preference = findPreference(getKeyForCurrent());
            if (preference != null) {
                preference.setSummary(R.string.current_screen_lock);
            }
        }

        private String getKeyForCurrent() {
            if (this.mLockPatternUtils.isLockScreenDisabled(UserHandle.myUserId())) {
                return "unlock_set_off";
            }
            if (this.mLockPatternUtils.usingVoiceWeak()) {
                return "unlock_set_voice_weak";
            }
            switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
                case 0:
                    return "unlock_set_none";
                case 65536:
                    return "unlock_set_pattern";
                case 131072:
                case 196608:
                    return "unlock_set_pin";
                case 262144:
                case 327680:
                    return "unlock_set_password";
                default:
                    return null;
            }
        }

        private int upgradeQuality(int quality) {
            return upgradeQualityForDPM(quality);
        }

        private int upgradeQualityForDPM(int quality) {
            int minQuality = this.mDPM.getPasswordQuality(null);
            if (quality < minQuality) {
                return minQuality;
            }
            return quality;
        }

        protected void disableUnusablePreferences(int quality, boolean hideDisabledPrefs) {
            disableUnusablePreferencesImpl(quality, hideDisabledPrefs);
        }

        protected void disableUnusablePreferencesImpl(int quality, boolean hideDisabled) {
            PreferenceScreen entries = getPreferenceScreen();
            for (int i = entries.getPreferenceCount() - 1; i >= 0; i--) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof PreferenceScreen) {
                    String key = pref.getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    if ("unlock_set_off".equals(key)) {
                        enabled = quality <= 0;
                    } else if ("unlock_set_none".equals(key)) {
                        enabled = quality <= 0;
                    } else if ("unlock_set_pattern".equals(key)) {
                        enabled = quality <= 65536;
                    } else if ("unlock_set_pin".equals(key)) {
                        enabled = quality <= 196608;
                    } else if ("unlock_set_password".equals(key)) {
                        enabled = quality <= 393216;
                    } else if ("unlock_set_voice_weak".equals(key)) {
                        enabled = quality <= 16384;
                        visible = FeatureOption.MTK_VOICE_UNLOCK_SUPPORT;
                    }
                    if (hideDisabled) {
                        visible = enabled;
                    }
                    boolean onlyShowFallback = getActivity().getIntent().getBooleanExtra("lockscreen.weak_fallback", false);
                    if (!visible || (onlyShowFallback && !allowedForFallback(key))) {
                        entries.removePreference(pref);
                    } else if (!enabled) {
                        pref.setSummary(R.string.unlock_set_unlock_disabled_summary);
                        pref.setEnabled(false);
                    }
                }
            }
        }

        private void updatePreferenceSummaryIfNeeded() {
            if (!LockPatternUtils.isDeviceEncrypted() && !AccessibilityManager.getInstance(getActivity()).getEnabledAccessibilityServiceList(-1).isEmpty()) {
                CharSequence summary = getString(R.string.secure_lock_encryption_warning);
                PreferenceScreen screen = getPreferenceScreen();
                int preferenceCount = screen.getPreferenceCount();
                for (int i = 0; i < preferenceCount; i++) {
                    Preference preference = screen.getPreference(i);
                    String key = preference.getKey();
                    if (!(key.equals("unlock_set_pattern") || key.equals("unlock_set_pin"))) {
                        if (!key.equals("unlock_set_password")) {
                        }
                    }
                    preference.setSummary(summary);
                }
            }
        }

        private boolean allowedForFallback(String key) {
            return !"unlock_set_pattern".equals(key) ? "unlock_set_pin".equals(key) : true;
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge) {
            return ChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, challenge);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password) {
            return ChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, password);
        }

        protected Intent getLockPatternIntent(Context context, boolean requirePassword, long challenge) {
            return ChooseLockPattern.createIntent(context, requirePassword, challenge);
        }

        protected Intent getLockPatternIntent(Context context, boolean requirePassword, String pattern) {
            return ChooseLockPattern.createIntent(context, requirePassword, pattern);
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality, boolean required) {
            return EncryptionInterstitial.createStartIntent(context, quality, required);
        }

        void updateUnlockMethodAndFinish(int quality, boolean disabled) {
            if (this.mPasswordConfirmed) {
                boolean isFallback = getActivity().getIntent().getBooleanExtra("lockscreen.weak_fallback", false);
                quality = upgradeQuality(quality);
                Context context = getActivity();
                Intent intent;
                String isFallbackFor;
                String commandKey;
                String commandValue;
                if (quality >= 131072) {
                    int minLength = this.mDPM.getPasswordMinimumLength(null);
                    if (minLength < 4) {
                        minLength = 4;
                    }
                    int maxLength = this.mDPM.getPasswordMaximumLength(quality);
                    if (isFallback) {
                        intent = ChooseLockPassword.createIntent(context, quality, isFallback, minLength, maxLength, this.mRequirePassword);
                        isFallbackFor = getActivity().getIntent().getStringExtra("lockscreen.weak_fallback_for");
                        commandKey = getActivity().getIntent().getStringExtra("settings_command_key");
                        commandValue = getActivity().getIntent().getStringExtra("settings_command_value");
                        intent.putExtra("settings_command_key", commandKey);
                        intent.putExtra("settings_command_value", commandValue);
                        intent.putExtra("lockscreen.weak_fallback_for", isFallbackFor);
                        startActivityForResult(intent, 101);
                        return;
                    }
                    if (this.mHasChallenge) {
                        intent = getLockPasswordIntent(context, quality, minLength, maxLength, this.mRequirePassword, this.mChallenge);
                    } else {
                        intent = getLockPasswordIntent(context, quality, minLength, maxLength, this.mRequirePassword, this.mUserPassword);
                    }
                    startActivityForResult(intent, 102);
                    return;
                } else if (quality == 65536) {
                    if (isFallback) {
                        intent = ChooseLockPattern.createIntent(context, isFallback, this.mRequirePassword, false);
                        isFallbackFor = getActivity().getIntent().getStringExtra("lockscreen.weak_fallback_for");
                        commandKey = getActivity().getIntent().getStringExtra("settings_command_key");
                        commandValue = getActivity().getIntent().getStringExtra("settings_command_value");
                        intent.putExtra("settings_command_key", commandKey);
                        intent.putExtra("settings_command_value", commandValue);
                        intent.putExtra("lockscreen.weak_fallback_for", isFallbackFor);
                        startActivityForResult(intent, 101);
                        return;
                    }
                    if (this.mHasChallenge) {
                        intent = getLockPatternIntent(context, this.mRequirePassword, this.mChallenge);
                    } else {
                        intent = getLockPatternIntent(context, this.mRequirePassword, this.mUserPassword);
                    }
                    startActivityForResult(intent, 102);
                    return;
                } else if (quality == 0) {
                    this.mChooseLockSettingsHelper.utils().clearLock(UserHandle.myUserId());
                    this.mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled, UserHandle.myUserId());
                    removeAllFingerprintTemplatesAndFinish();
                    getActivity().setResult(-1);
                    return;
                } else if (quality == 16384) {
                    intent = new Intent();
                    intent.setClassName("com.mediatek.voiceunlock", "com.mediatek.voiceunlock.VoiceUnlock");
                    startActivity(intent);
                    finish();
                    return;
                } else {
                    removeAllFingerprintTemplatesAndFinish();
                    return;
                }
            }
            throw new IllegalStateException("Tried to update password without confirming it");
        }

        private void removeAllFingerprintTemplatesAndFinish() {
            if (this.mFingerprintManager == null || !this.mFingerprintManager.isHardwareDetected() || this.mFingerprintManager.getEnrolledFingerprints().size() <= 0) {
                finish();
                return;
            }
            List<Fingerprint> items = this.mFingerprintManager.getEnrolledFingerprints();
            for (int i = 0; i < items.size(); i++) {
                this.mFingerprintManager.remove((Fingerprint) items.get(i), this.mRemovalCallback);
            }
        }

        public void onDestroy() {
            super.onDestroy();
        }

        protected int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningMessage() {
            boolean hasFingerprints = this.mFingerprintManager.hasEnrolledFingerprints();
            int i;
            switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
                case 65536:
                    if (hasFingerprints) {
                        i = R.string.unlock_disable_frp_warning_content_pattern_fingerprint;
                    } else {
                        i = R.string.unlock_disable_frp_warning_content_pattern;
                    }
                    return i;
                case 131072:
                case 196608:
                    if (hasFingerprints) {
                        i = R.string.unlock_disable_frp_warning_content_pin_fingerprint;
                    } else {
                        i = R.string.unlock_disable_frp_warning_content_pin;
                    }
                    return i;
                case 262144:
                case 327680:
                case 393216:
                    if (hasFingerprints) {
                        i = R.string.unlock_disable_frp_warning_content_password_fingerprint;
                    } else {
                        i = R.string.unlock_disable_frp_warning_content_password;
                    }
                    return i;
                default:
                    if (hasFingerprints) {
                        i = R.string.unlock_disable_frp_warning_content_unknown_fingerprint;
                    } else {
                        i = R.string.unlock_disable_frp_warning_content_unknown;
                    }
                    return i;
            }
        }

        private boolean isUnlockMethodSecure(String unlockMethod) {
            return ("unlock_set_off".equals(unlockMethod) || "unlock_set_none".equals(unlockMethod)) ? false : true;
        }

        private boolean setUnlockMethod(String unlockMethod) {
            EventLog.writeEvent(90200, unlockMethod);
            if ("unlock_set_off".equals(unlockMethod)) {
                updateUnlockMethodAndFinish(0, true);
            } else if ("unlock_set_none".equals(unlockMethod)) {
                updateUnlockMethodAndFinish(0, false);
            } else if ("unlock_set_pattern".equals(unlockMethod)) {
                maybeEnableEncryption(65536, false);
            } else if ("unlock_set_pin".equals(unlockMethod)) {
                maybeEnableEncryption(131072, false);
            } else if ("unlock_set_password".equals(unlockMethod)) {
                maybeEnableEncryption(262144, false);
            } else if ("unlock_set_voice_weak".equals(unlockMethod)) {
                updateUnlockMethodAndFinish(16384, false);
            } else {
                Log.e("ChooseLockGenericFragment", "Encountered unknown unlock method to set: " + unlockMethod);
                return false;
            }
            return true;
        }

        private void showFactoryResetProtectionWarningDialog(String unlockMethodToSet) {
            FactoryResetProtectionWarningDialog.newInstance(getResIdForFactoryResetProtectionWarningMessage(), unlockMethodToSet).show(getChildFragmentManager(), "frp_warning_dialog");
        }
    }

    public static class InternalActivity extends ChooseLockGeneric {
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        return modIntent;
    }

    protected boolean isValidFragment(String fragmentName) {
        if (ChooseLockGenericFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockGenericFragment.class;
    }
}
