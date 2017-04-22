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
import android.widget.Button;
import com.android.settings.ChooseLockPattern.ChooseLockPatternFragment;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

public class SetupChooseLockPattern extends ChooseLockPattern {

    public static class SetupChooseLockPatternFragment extends ChooseLockPatternFragment implements NavigationBarListener {
        private NavigationBar mNavigationBar;
        private Button mRetryButton;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            SetupWizardLayout layout = (SetupWizardLayout) inflater.inflate(R.layout.setup_choose_lock_pattern, container, false);
            this.mNavigationBar = layout.getNavigationBar();
            this.mNavigationBar.setNavigationBarListener(this);
            layout.setHeaderText(getActivity().getTitle());
            return layout;
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            this.mRetryButton = (Button) view.findViewById(R.id.retryButton);
            this.mRetryButton.setOnClickListener(this);
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setImmersiveMode(getActivity());
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            Intent intent = SetupRedactionInterstitial.createStartIntent(context);
            if (intent != null) {
                SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            }
            return intent;
        }

        public void onClick(View v) {
            if (v == this.mRetryButton) {
                handleLeftButton();
            } else {
                super.onClick(v);
            }
        }

        protected void setRightButtonEnabled(boolean enabled) {
            this.mNavigationBar.getNextButton().setEnabled(enabled);
        }

        protected void setRightButtonText(int text) {
            this.mNavigationBar.getNextButton().setText(text);
        }

        protected void updateStage(Stage stage) {
            super.updateStage(stage);
            this.mRetryButton.setEnabled(stage == Stage.FirstChoiceValid);
        }

        public void onNavigateBack() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
            handleRightButton();
        }
    }

    public static Intent createIntent(Context context, boolean requirePassword, String pattern) {
        Intent intent = ChooseLockPattern.createIntent(context, requirePassword, pattern);
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, long challenge) {
        Intent intent = ChooseLockPattern.createIntent(context, requirePassword, challenge);
        intent.setClass(context, SetupChooseLockPattern.class);
        return intent;
    }

    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPatternFragment.class.getName().equals(fragmentName);
    }

    Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPatternFragment.class;
    }

    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), first);
    }
}
