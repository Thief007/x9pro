package com.android.setupwizard.navigationbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class SetupWizardNavBar$NavButton extends Button {
    public SetupWizardNavBar$NavButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SetupWizardNavBar$NavButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SetupWizardNavBar$NavButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SetupWizardNavBar$NavButton(Context context) {
        super(context);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1.0f : 0.23f);
    }
}
