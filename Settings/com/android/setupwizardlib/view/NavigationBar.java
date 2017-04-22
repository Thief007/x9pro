package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.setupwizardlib.R$attr;
import com.android.setupwizardlib.R$dimen;
import com.android.setupwizardlib.R$id;
import com.android.setupwizardlib.R$layout;
import com.android.setupwizardlib.R$style;

public class NavigationBar extends LinearLayout implements OnClickListener {
    private Button mBackButton;
    private NavigationBarListener mListener;
    private Button mMoreButton;
    private Button mNextButton;

    public interface NavigationBarListener {
        void onNavigateBack();

        void onNavigateNext();
    }

    public static class NavButton extends Button {
        public NavButton(Context context) {
            super(context);
        }

        public NavButton(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setEnabled(boolean enabled) {
            int i = 0;
            super.setEnabled(enabled);
            int alpha = enabled ? 255 : 59;
            setTextColor(getTextColors().withAlpha(alpha));
            if (VERSION.SDK_INT >= 17) {
                for (Drawable d : getCompoundDrawablesRelative()) {
                    Drawable d2;
                    if (d2 != null) {
                        d2.mutate().setAlpha(alpha);
                    }
                }
            }
            Drawable[] compoundDrawables = getCompoundDrawables();
            int length = compoundDrawables.length;
            while (i < length) {
                d2 = compoundDrawables[i];
                if (d2 != null) {
                    d2.mutate().setAlpha(alpha);
                }
                i++;
            }
        }

        protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
            super.onTextChanged(text, start, lengthBefore, lengthAfter);
            setCompoundDrawablePadding(TextUtils.isEmpty(text) ? 0 : getResources().getDimensionPixelSize(R$dimen.suw_navbar_button_drawable_padding));
        }
    }

    private static int getNavbarTheme(Context context) {
        TypedArray attributes = context.obtainStyledAttributes(new int[]{R$attr.suwNavBarTheme, 16842800, 16842801});
        int theme = attributes.getResourceId(0, 0);
        if (theme == 0) {
            float[] foregroundHsv = new float[3];
            float[] backgroundHsv = new float[3];
            Color.colorToHSV(attributes.getColor(1, 0), foregroundHsv);
            Color.colorToHSV(attributes.getColor(2, 0), backgroundHsv);
            theme = (foregroundHsv[2] > backgroundHsv[2] ? 1 : (foregroundHsv[2] == backgroundHsv[2] ? 0 : -1)) > 0 ? R$style.SuwNavBarThemeDark : R$style.SuwNavBarThemeLight;
        }
        attributes.recycle();
        return theme;
    }

    private static Context getThemedContext(Context context) {
        return new ContextThemeWrapper(context, getNavbarTheme(context));
    }

    public NavigationBar(Context context) {
        super(getThemedContext(context));
        init();
    }

    public NavigationBar(Context context, AttributeSet attrs) {
        super(getThemedContext(context), attrs);
        init();
    }

    @TargetApi(11)
    public NavigationBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getThemedContext(context), attrs, defStyleAttr);
        init();
    }

    private void init() {
        View.inflate(getContext(), R$layout.suw_navbar_view, this);
        this.mNextButton = (Button) findViewById(R$id.suw_navbar_next);
        this.mBackButton = (Button) findViewById(R$id.suw_navbar_back);
        this.mMoreButton = (Button) findViewById(R$id.suw_navbar_more);
    }

    public Button getBackButton() {
        return this.mBackButton;
    }

    public Button getNextButton() {
        return this.mNextButton;
    }

    public void setNavigationBarListener(NavigationBarListener listener) {
        this.mListener = listener;
        if (this.mListener != null) {
            getBackButton().setOnClickListener(this);
            getNextButton().setOnClickListener(this);
        }
    }

    public void onClick(View view) {
        if (this.mListener == null) {
            return;
        }
        if (view == getBackButton()) {
            this.mListener.onNavigateBack();
        } else if (view == getNextButton()) {
            this.mListener.onNavigateNext();
        }
    }
}
