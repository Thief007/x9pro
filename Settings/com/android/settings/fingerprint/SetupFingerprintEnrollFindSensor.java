package com.android.settings.fingerprint;

import android.content.Intent;
import android.content.res.Resources.Theme;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

public class SetupFingerprintEnrollFindSensor extends FingerprintEnrollFindSensor implements NavigationBarListener {
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, SetupFingerprintEnrollEnrolling.class);
        intent.putExtra("hw_auth_token", this.mToken);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), first);
    }

    protected void initViews() {
        SetupWizardUtils.setImmersiveMode(this);
        View nextButton = findViewById(R.id.next_button);
        if (nextButton != null) {
            nextButton.setVisibility(8);
        }
        getNavigationBar().setNavigationBarListener(this);
        getNavigationBar().getBackButton().setVisibility(8);
    }

    protected Button getNextButton() {
        return getNavigationBar().getNextButton();
    }

    public void onNavigateBack() {
        onBackPressed();
    }

    public void onNavigateNext() {
        onNextButtonClick();
    }

    protected int getMetricsCategory() {
        return 247;
    }
}
