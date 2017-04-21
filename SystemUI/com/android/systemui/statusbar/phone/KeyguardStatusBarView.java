package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.keyguard.CarrierText;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import java.text.NumberFormat;

public class KeyguardStatusBarView extends RelativeLayout implements BatteryStateChangeCallback {
    private boolean mBatteryCharging;
    private BatteryController mBatteryController;
    private TextView mBatteryLevel;
    private boolean mBatteryListening;
    private CarrierText mCarrierLabel;
    private Context mContext;
    private Interpolator mFastOutSlowInInterpolator;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mKeyguardUserSwitcherShowing;
    private ImageView mMultiUserAvatar;
    private MultiUserSwitch mMultiUserSwitch;
    private View mSystemIconsSuperContainer;
    private int mSystemIconsSwitcherHiddenExpandedMargin;

    public KeyguardStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        this.mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        this.mCarrierLabel = (CarrierText) findViewById(R.id.keyguard_carrier_text);
        loadDimens();
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(), 17563661);
        updateUserSwitcher();
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mCarrierLabel.setTextSize(0, (float) getResources().getDimensionPixelSize(17105137));
        this.mBatteryLevel.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
    }

    private void loadDimens() {
        this.mSystemIconsSwitcherHiddenExpandedMargin = getResources().getDimensionPixelSize(R.dimen.system_icons_switcher_hidden_expanded_margin);
    }

    private void updateVisibilities() {
        int i = 0;
        if (this.mMultiUserSwitch.getParent() != this && !this.mKeyguardUserSwitcherShowing) {
            if (this.mMultiUserSwitch.getParent() != null) {
                getOverlay().remove(this.mMultiUserSwitch);
            }
            addView(this.mMultiUserSwitch, 0);
        } else if (this.mMultiUserSwitch.getParent() == this && this.mKeyguardUserSwitcherShowing) {
            removeView(this.mMultiUserSwitch);
        }
        TextView textView = this.mBatteryLevel;
        if (!this.mBatteryCharging && System.getInt(this.mContext.getContentResolver(), "status_bar_show_battery_percent", 0) == 0) {
            i = 8;
        }
        textView.setVisibility(i);
    }

    private void updateSystemIconsLayoutParams() {
        LayoutParams lp = (LayoutParams) this.mSystemIconsSuperContainer.getLayoutParams();
        int marginEnd = this.mKeyguardUserSwitcherShowing ? this.mSystemIconsSwitcherHiddenExpandedMargin : 0;
        if (marginEnd != lp.getMarginEnd()) {
            lp.setMarginEnd(marginEnd);
            this.mSystemIconsSuperContainer.setLayoutParams(lp);
        }
    }

    public void setListening(boolean listening) {
        if (listening != this.mBatteryListening) {
            this.mBatteryListening = listening;
            if (this.mBatteryListening) {
                this.mBatteryController.addStateChangedCallback(this);
            } else {
                this.mBatteryController.removeStateChangedCallback(this);
            }
        }
    }

    private void updateUserSwitcher() {
        boolean keyguardSwitcherAvailable = this.mKeyguardUserSwitcher != null;
        this.mMultiUserSwitch.setClickable(keyguardSwitcherAvailable);
        this.mMultiUserSwitch.setFocusable(keyguardSwitcherAvailable);
        this.mMultiUserSwitch.setKeyguardMode(keyguardSwitcherAvailable);
    }

    public void setBatteryController(BatteryController batteryController) {
        this.mBatteryController = batteryController;
        ((BatteryMeterView) findViewById(R.id.battery)).setBatteryController(batteryController);
    }

    public void setUserSwitcherController(UserSwitcherController controller) {
        this.mMultiUserSwitch.setUserSwitcherController(controller);
    }

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(new OnUserInfoChangedListener() {
            public void onUserInfoChanged(String name, Drawable picture) {
                Log.d("KeyguardStatusBarView", "onUserInfoChanged and set new profile icon");
                KeyguardStatusBarView.this.mMultiUserAvatar.setImageDrawable(picture);
            }
        });
    }

    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        this.mBatteryLevel.setText(NumberFormat.getPercentInstance().format(((double) level) / 100.0d));
        if (this.mBatteryCharging != charging) {
        }
        this.mBatteryCharging = charging;
        updateVisibilities();
    }

    public void onPowerSaveChanged() {
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        this.mMultiUserSwitch.setKeyguardUserSwitcher(keyguardUserSwitcher);
        updateUserSwitcher();
    }

    public void setKeyguardUserSwitcherShowing(boolean showing, boolean animate) {
        this.mKeyguardUserSwitcherShowing = showing;
        if (animate) {
            animateNextLayoutChange();
        }
        updateVisibilities();
        updateSystemIconsLayoutParams();
    }

    private void animateNextLayoutChange() {
        final int systemIconsCurrentX = this.mSystemIconsSuperContainer.getLeft();
        final boolean userSwitcherVisible = this.mMultiUserSwitch.getParent() == this;
        getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
            public boolean onPreDraw() {
                KeyguardStatusBarView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                boolean userSwitcherHiding = userSwitcherVisible ? KeyguardStatusBarView.this.mMultiUserSwitch.getParent() != KeyguardStatusBarView.this : false;
                KeyguardStatusBarView.this.mSystemIconsSuperContainer.setX((float) systemIconsCurrentX);
                KeyguardStatusBarView.this.mSystemIconsSuperContainer.animate().translationX(0.0f).setDuration(400).setStartDelay((long) (userSwitcherHiding ? 300 : 0)).setInterpolator(KeyguardStatusBarView.this.mFastOutSlowInInterpolator).start();
                if (userSwitcherHiding) {
                    KeyguardStatusBarView.this.getOverlay().add(KeyguardStatusBarView.this.mMultiUserSwitch);
                    KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(0.0f).setDuration(300).setStartDelay(0).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(new Runnable() {
                        public void run() {
                            KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(1.0f);
                            KeyguardStatusBarView.this.getOverlay().remove(KeyguardStatusBarView.this.mMultiUserSwitch);
                        }
                    }).start();
                } else {
                    KeyguardStatusBarView.this.mMultiUserSwitch.setAlpha(0.0f);
                    KeyguardStatusBarView.this.mMultiUserSwitch.animate().alpha(1.0f).setDuration(300).setStartDelay(200).setInterpolator(PhoneStatusBar.ALPHA_IN);
                }
                return true;
            }
        });
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != 0) {
            this.mSystemIconsSuperContainer.animate().cancel();
            this.mMultiUserSwitch.animate().cancel();
            this.mMultiUserSwitch.setAlpha(1.0f);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
