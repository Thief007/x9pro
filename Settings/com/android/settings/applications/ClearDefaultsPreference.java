package com.android.settings.applications;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.IUsbManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.Preference;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

public class ClearDefaultsPreference extends Preference {
    protected static final String TAG = ClearDefaultsPreference.class.getSimpleName();
    private Button mActivitiesButton;
    protected AppEntry mAppEntry;
    private AppWidgetManager mAppWidgetManager;
    private String mPackageName;
    private PackageManager mPm;
    private View mRootView;
    private IUsbManager mUsbManager;

    class C02521 implements OnClickListener {
        C02521() {
        }

        public void onClick(View v) {
            if (ClearDefaultsPreference.this.mUsbManager != null) {
                ClearDefaultsPreference.this.mPm.clearPackagePreferredActivities(ClearDefaultsPreference.this.mPackageName);
                try {
                    ClearDefaultsPreference.this.mUsbManager.clearDefaults(ClearDefaultsPreference.this.mPackageName, UserHandle.myUserId());
                } catch (RemoteException e) {
                    Log.e(ClearDefaultsPreference.TAG, "mUsbManager.clearDefaults", e);
                }
                ClearDefaultsPreference.this.mAppWidgetManager.setBindAppWidgetPermission(ClearDefaultsPreference.this.mPackageName, false);
                ClearDefaultsPreference.this.resetLaunchDefaultsUi((TextView) ClearDefaultsPreference.this.mRootView.findViewById(R.id.auto_launch));
            }
        }
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.app_preferred_settings);
        this.mAppWidgetManager = AppWidgetManager.getInstance(context);
        this.mPm = context.getPackageManager();
        this.mUsbManager = Stub.asInterface(ServiceManager.getService("usb"));
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClearDefaultsPreference(Context context) {
        this(context, null);
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public void setAppEntry(AppEntry entry) {
        this.mAppEntry = entry;
    }

    protected View onCreateView(ViewGroup parent) {
        this.mRootView = super.onCreateView(parent);
        this.mActivitiesButton = (Button) this.mRootView.findViewById(R.id.clear_activities_button);
        this.mActivitiesButton.setOnClickListener(new C02521());
        return this.mRootView;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        updateUI();
    }

    public boolean updateUI() {
        boolean autoLaunchEnabled;
        boolean hasBindAppWidgetPermission = this.mAppWidgetManager.hasBindAppWidgetPermission(this.mAppEntry.info.packageName);
        TextView autoLaunchView = (TextView) this.mRootView.findViewById(R.id.auto_launch);
        if (Utils.hasPreferredActivities(this.mPm, this.mPackageName)) {
            autoLaunchEnabled = true;
        } else {
            autoLaunchEnabled = Utils.hasUsbDefaults(this.mUsbManager, this.mPackageName);
        }
        if (autoLaunchEnabled || hasBindAppWidgetPermission) {
            SpannableString s;
            boolean z = hasBindAppWidgetPermission ? autoLaunchEnabled : false;
            if (hasBindAppWidgetPermission) {
                autoLaunchView.setText(R.string.auto_launch_label_generic);
            } else {
                autoLaunchView.setText(R.string.auto_launch_label);
            }
            Context context = getContext();
            CharSequence text = null;
            int bulletIndent = context.getResources().getDimensionPixelSize(R.dimen.installed_app_details_bullet_offset);
            if (autoLaunchEnabled) {
                CharSequence autoLaunchEnableText = context.getText(R.string.auto_launch_enable_text);
                s = new SpannableString(autoLaunchEnableText);
                if (z) {
                    s.setSpan(new BulletSpan(bulletIndent), 0, autoLaunchEnableText.length(), 0);
                }
                text = TextUtils.concat(new CharSequence[]{s, "\n"});
            }
            if (hasBindAppWidgetPermission) {
                CharSequence alwaysAllowBindAppWidgetsText = context.getText(R.string.always_allow_bind_appwidgets_text);
                s = new SpannableString(alwaysAllowBindAppWidgetsText);
                if (z) {
                    s.setSpan(new BulletSpan(bulletIndent), 0, alwaysAllowBindAppWidgetsText.length(), 0);
                }
                text = text == null ? TextUtils.concat(new CharSequence[]{s, "\n"}) : TextUtils.concat(new CharSequence[]{text, "\n", s, "\n"});
            }
            autoLaunchView.setText(text);
            this.mActivitiesButton.setEnabled(true);
        } else {
            resetLaunchDefaultsUi(autoLaunchView);
        }
        return true;
    }

    private void resetLaunchDefaultsUi(TextView autoLaunchView) {
        autoLaunchView.setText(R.string.auto_launch_disable_text);
        this.mActivitiesButton.setEnabled(false);
    }
}
