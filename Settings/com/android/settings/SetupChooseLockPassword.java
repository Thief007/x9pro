package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.ChooseLockPassword.ChooseLockPasswordFragment;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

public class SetupChooseLockPassword extends ChooseLockPassword {

    public static class SetupChooseLockPasswordFragment extends ChooseLockPasswordFragment implements NavigationBarListener {
        private SetupWizardLayout mLayout;
        private NavigationBar mNavigationBar;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            this.mLayout = (SetupWizardLayout) inflater.inflate(R.layout.setup_choose_lock_password, container, false);
            this.mNavigationBar = this.mLayout.getNavigationBar();
            this.mNavigationBar.setNavigationBarListener(this);
            return this.mLayout;
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SystemBarHelper.setImeInsetView(this.mLayout.findViewById(R.id.suw_bottom_scroll_view));
            SetupWizardUtils.setImmersiveMode(getActivity());
            this.mLayout.setHeaderText(getActivity().getTitle());
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            Intent intent = SetupRedactionInterstitial.createStartIntent(context);
            if (intent != null) {
                SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            }
            return intent;
        }

        protected void setNextEnabled(boolean enabled) {
            this.mNavigationBar.getNextButton().setEnabled(enabled);
        }

        protected void setNextText(int text) {
            this.mNavigationBar.getNextButton().setText(text);
        }

        public void onNavigateBack() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
            handleNext();
        }
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password) {
        Intent intent = ChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, password);
        intent.setClass(context, SetupChooseLockPassword.class);
        intent.putExtra("extra_prefs_show_button_bar", false);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge) {
        Intent intent = ChooseLockPassword.createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, challenge);
        intent.setClass(context, SetupChooseLockPassword.class);
        intent.putExtra("extra_prefs_show_button_bar", false);
        return intent;
    }

    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPasswordFragment.class.getName().equals(fragmentName);
    }

    Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPasswordFragment.class;
    }

    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), first);
    }
}
