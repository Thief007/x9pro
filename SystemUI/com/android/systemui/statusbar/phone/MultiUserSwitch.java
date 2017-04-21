package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.QuickContact;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.UserSwitcherController.BaseUserAdapter;

public class MultiUserSwitch extends FrameLayout implements OnClickListener {
    private boolean mKeyguardMode;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private QSPanel mQsPanel;
    private final int[] mTmpInt2 = new int[2];
    private BaseUserAdapter mUserListener;
    final UserManager mUserManager = UserManager.get(getContext());
    private UserSwitcherController mUserSwitcherController;

    public MultiUserSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
        refreshContentDescription();
    }

    public void setQsPanel(QSPanel qsPanel) {
        this.mQsPanel = qsPanel;
        setUserSwitcherController(qsPanel.getHost().getUserSwitcherController());
    }

    public void setUserSwitcherController(UserSwitcherController userSwitcherController) {
        this.mUserSwitcherController = userSwitcherController;
        registerListener();
        refreshContentDescription();
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void setKeyguardMode(boolean keyguardShowing) {
        this.mKeyguardMode = keyguardShowing;
        registerListener();
    }

    private void registerListener() {
        if (UserSwitcherController.isUserSwitcherAvailable(this.mUserManager) && this.mUserListener == null) {
            UserSwitcherController controller = this.mUserSwitcherController;
            if (controller != null) {
                this.mUserListener = new BaseUserAdapter(controller) {
                    public void notifyDataSetChanged() {
                        MultiUserSwitch.this.refreshContentDescription();
                    }

                    public View getView(int position, View convertView, ViewGroup parent) {
                        return null;
                    }
                };
                refreshContentDescription();
            }
        }
    }

    public void onClick(View v) {
        if (!UserSwitcherController.isUserSwitcherAvailable(this.mUserManager)) {
            getContext().startActivityAsUser(QuickContact.composeQuickContactsIntent(getContext(), v, Profile.CONTENT_URI, 3, null), new UserHandle(-2));
        } else if (this.mKeyguardMode) {
            if (this.mKeyguardUserSwitcher != null) {
                this.mKeyguardUserSwitcher.show(true);
            }
        } else if (this.mQsPanel != null && this.mUserSwitcherController != null) {
            View center = getChildCount() > 0 ? getChildAt(0) : this;
            center.getLocationInWindow(this.mTmpInt2);
            int[] iArr = this.mTmpInt2;
            iArr[0] = iArr[0] + (center.getWidth() / 2);
            iArr = this.mTmpInt2;
            iArr[1] = iArr[1] + (center.getHeight() / 2);
            this.mQsPanel.showDetailAdapter(true, this.mUserSwitcherController.userDetailAdapter, this.mTmpInt2);
        }
    }

    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        refreshContentDescription();
    }

    private void refreshContentDescription() {
        CharSequence currentUser = null;
        if (UserSwitcherController.isUserSwitcherAvailable(this.mUserManager) && this.mUserSwitcherController != null) {
            currentUser = this.mUserSwitcherController.getCurrentUserName(this.mContext);
        }
        CharSequence text = null;
        if (isClickable()) {
            if (!UserSwitcherController.isUserSwitcherAvailable(this.mUserManager)) {
                text = this.mContext.getString(R.string.accessibility_multi_user_switch_quick_contact);
            } else if (TextUtils.isEmpty(currentUser)) {
                text = this.mContext.getString(R.string.accessibility_multi_user_switch_switcher);
            } else {
                text = this.mContext.getString(R.string.accessibility_multi_user_switch_switcher_with_current, new Object[]{currentUser});
            }
        } else if (!TextUtils.isEmpty(currentUser)) {
            text = this.mContext.getString(R.string.accessibility_multi_user_switch_inactive, new Object[]{currentUser});
        }
        if (!TextUtils.equals(getContentDescription(), text)) {
            setContentDescription(text);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
