package com.android.settings.wifi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.WifiConfiguration;
import android.os.Looper;
import android.os.UserHandle;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

public class AccessPointPreference extends Preference {
    private static final int[] STATE_NONE = new int[0];
    private static final int[] STATE_SECURED = new int[]{R.attr.state_encrypted};
    static final int[] WIFI_CONNECTION_STRENGTH = new int[]{R.string.accessibility_wifi_one_bar, R.string.accessibility_wifi_two_bars, R.string.accessibility_wifi_three_bars, R.string.accessibility_wifi_signal_full};
    private static int[] wifi_signal_attributes = new int[]{R.attr.wifi_signal};
    private AccessPoint mAccessPoint;
    private Drawable mBadge;
    private final UserBadgeCache mBadgeCache;
    private final int mBadgePadding;
    private CharSequence mContentDescription;
    private boolean mForSavedNetworks;
    private int mLevel;
    private final Runnable mNotifyChanged;
    private TextView mTitleView;
    private final StateListDrawable mWifiSld;

    class C05951 implements Runnable {
        C05951() {
        }

        public void run() {
            AccessPointPreference.this.notifyChanged();
        }
    }

    public static class UserBadgeCache {
        private final SparseArray<Drawable> mBadges = new SparseArray();
        private final PackageManager mPm;

        UserBadgeCache(PackageManager pm) {
            this.mPm = pm;
        }

        private Drawable getUserBadge(int userId) {
            int index = this.mBadges.indexOfKey(userId);
            if (index >= 0) {
                return (Drawable) this.mBadges.valueAt(index);
            }
            Drawable badge = this.mPm.getUserBadgeForDensity(new UserHandle(userId), 0);
            this.mBadges.put(userId, badge);
            return badge;
        }
    }

    public AccessPointPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mForSavedNetworks = false;
        this.mNotifyChanged = new C05951();
        this.mWifiSld = null;
        this.mBadgePadding = 0;
        this.mBadgeCache = null;
    }

    public AccessPointPreference(AccessPoint accessPoint, Context context, UserBadgeCache cache, boolean forSavedNetworks) {
        super(context);
        this.mForSavedNetworks = false;
        this.mNotifyChanged = new C05951();
        this.mBadgeCache = cache;
        this.mAccessPoint = accessPoint;
        this.mForSavedNetworks = forSavedNetworks;
        this.mAccessPoint.setTag(this);
        this.mLevel = -1;
        this.mWifiSld = (StateListDrawable) context.getTheme().obtainStyledAttributes(wifi_signal_attributes).getDrawable(0);
        this.mBadgePadding = context.getResources().getDimensionPixelSize(R.dimen.wifi_preference_badge_padding);
        refresh();
    }

    public AccessPoint getAccessPoint() {
        return this.mAccessPoint;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        if (this.mAccessPoint != null) {
            Drawable drawable = getIcon();
            if (drawable != null) {
                drawable.setLevel(this.mLevel);
            }
            this.mTitleView = (TextView) view.findViewById(16908310);
            if (this.mTitleView != null) {
                this.mTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, this.mBadge, null);
                this.mTitleView.setCompoundDrawablePadding(this.mBadgePadding);
            }
            view.setContentDescription(this.mContentDescription);
        }
    }

    protected void updateIcon(int level, Context context) {
        if (level == -1) {
            setIcon(null);
        } else if (getIcon() == null && this.mWifiSld != null) {
            int[] iArr;
            StateListDrawable stateListDrawable = this.mWifiSld;
            if (this.mAccessPoint.getSecurity() != 0) {
                iArr = STATE_SECURED;
            } else {
                iArr = STATE_NONE;
            }
            stateListDrawable.setState(iArr);
            Drawable drawable = this.mWifiSld.getCurrent();
            if (this.mForSavedNetworks) {
                setIcon(null);
            } else {
                setIcon(drawable);
            }
        }
    }

    protected void updateBadge(Context context) {
        WifiConfiguration config = this.mAccessPoint.getConfig();
        if (config != null) {
            this.mBadge = this.mBadgeCache.getUserBadge(config.creatorUid);
        }
    }

    public void refresh() {
        CharSequence savedNetworkSummary;
        if (this.mForSavedNetworks) {
            setTitle(this.mAccessPoint.getConfigName());
        } else {
            setTitle(this.mAccessPoint.getSsid());
        }
        Context context = getContext();
        int level = this.mAccessPoint.getLevel();
        if (level != this.mLevel) {
            this.mLevel = level;
            updateIcon(this.mLevel, context);
            notifyChanged();
        }
        updateBadge(context);
        if (this.mForSavedNetworks) {
            savedNetworkSummary = this.mAccessPoint.getSavedNetworkSummary();
        } else {
            savedNetworkSummary = this.mAccessPoint.getSettingsSummary();
        }
        setSummary(savedNetworkSummary);
        this.mContentDescription = getTitle();
        if (getSummary() != null) {
            this.mContentDescription = TextUtils.concat(new CharSequence[]{this.mContentDescription, ",", getSummary()});
        }
        if (level >= 0 && level < WIFI_CONNECTION_STRENGTH.length) {
            this.mContentDescription = TextUtils.concat(new CharSequence[]{this.mContentDescription, ",", getContext().getString(WIFI_CONNECTION_STRENGTH[level])});
        }
    }

    protected void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    public void onLevelChanged() {
        postNotifyChanged();
    }

    private void postNotifyChanged() {
        if (this.mTitleView != null) {
            this.mTitleView.post(this.mNotifyChanged);
        }
    }
}
