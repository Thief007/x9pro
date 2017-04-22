package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.setupwizardlib.SetupWizardListLayout;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

public class SetupChooseLockGeneric extends ChooseLockGeneric {

    public static class SetupChooseLockGenericFragment extends ChooseLockGenericFragment implements NavigationBarListener {
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            SetupWizardListLayout layout = (SetupWizardListLayout) inflater.inflate(R.layout.setup_choose_lock_generic, container, false);
            layout.setHeaderText(getActivity().getTitle());
            NavigationBar navigationBar = layout.getNavigationBar();
            navigationBar.getNextButton().setEnabled(false);
            navigationBar.setNavigationBarListener(this);
            return layout;
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setImmersiveMode(getActivity());
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode != 0) {
                if (data == null) {
                    data = new Intent();
                }
                data.putExtra(":settings:password_quality", new LockPatternUtils(getActivity()).getKeyguardStoredPasswordQuality(UserHandle.myUserId()));
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        protected void disableUnusablePreferences(int quality, boolean hideDisabled) {
            super.disableUnusablePreferencesImpl(Math.max(quality, 65536), true);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge) {
            Intent intent = SetupChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password) {
            Intent intent = SetupChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, password);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        protected Intent getLockPatternIntent(Context context, boolean requirePassword, long challenge) {
            Intent intent = SetupChooseLockPattern.createIntent(context, requirePassword, challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        protected Intent getLockPatternIntent(Context context, boolean requirePassword, String pattern) {
            Intent intent = SetupChooseLockPattern.createIntent(context, requirePassword, pattern);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality, boolean required) {
            Intent intent = SetupEncryptionInterstitial.createStartIntent(context, quality, required);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        public void onNavigateBack() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockGenericFragment.class.getName().equals(fragmentName);
    }

    Class<? extends PreferenceFragment> getFragmentClass() {
        return SetupChooseLockGenericFragment.class;
    }

    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), first);
    }
}
