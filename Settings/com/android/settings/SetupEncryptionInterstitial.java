package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.EncryptionInterstitial.EncryptionInterstitialFragment;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

public class SetupEncryptionInterstitial extends EncryptionInterstitial {

    public static class SetupEncryptionInterstitialFragment extends EncryptionInterstitialFragment implements NavigationBarListener {
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.setup_encryption_interstitial, container, false);
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ((SetupWizardLayout) view.findViewById(R.id.setup_wizard_layout)).getNavigationBar().setNavigationBarListener(this);
            Activity activity = getActivity();
            if (activity != null) {
                activity.setTitle(R.string.encryption_interstitial_header);
                SetupWizardUtils.setImmersiveMode(activity);
            }
        }

        public void onNavigateBack() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
            SetupEncryptionInterstitial activity = (SetupEncryptionInterstitial) getActivity();
            if (activity != null) {
                activity.setResult(-1, activity.getResultIntentData());
                finish();
            }
        }
    }

    public static Intent createStartIntent(Context ctx, int quality, boolean requirePasswordDefault) {
        Intent startIntent = EncryptionInterstitial.createStartIntent(ctx, quality, requirePasswordDefault);
        startIntent.setClass(ctx, SetupEncryptionInterstitial.class);
        startIntent.putExtra("extra_prefs_show_button_bar", false).putExtra(":settings:show_fragment_title_resid", -1);
        return startIntent;
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", SetupEncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    protected boolean isValidFragment(String fragmentName) {
        return SetupEncryptionInterstitialFragment.class.getName().equals(fragmentName);
    }

    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), first);
    }
}
