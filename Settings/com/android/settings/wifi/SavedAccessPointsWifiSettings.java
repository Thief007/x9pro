package com.android.settings.wifi;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.wifi.AccessPointPreference.UserBadgeCache;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SavedAccessPointsWifiSettings extends SettingsPreferenceFragment implements OnClickListener, Indexable {
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C05991();
    private Bundle mAccessPointSavedState;
    private WifiDialog mDialog;
    private AccessPoint mDlgAccessPoint;
    private AccessPoint mSelectedAccessPoint;
    private UserBadgeCache mUserBadgeCache;
    private WifiManager mWifiManager;

    static class C05991 extends BaseSearchIndexProvider {
        C05991() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            String title = context.getResources().getString(R.string.wifi_saved_access_points_titlebar);
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = title;
            data.screenTitle = title;
            data.enabled = enabled;
            result.add(data);
            List<AccessPoint> accessPoints = WifiTracker.getCurrentAccessPoints(context, true, false, true);
            int accessPointsSize = accessPoints.size();
            for (int i = 0; i < accessPointsSize; i++) {
                data = new SearchIndexableRaw(context);
                data.title = ((AccessPoint) accessPoints.get(i)).getSsidStr();
                data.screenTitle = title;
                data.enabled = enabled;
                result.add(data);
            }
            return result;
        }
    }

    class C06002 implements Comparator<AccessPoint> {
        C06002() {
        }

        public int compare(AccessPoint ap1, AccessPoint ap2) {
            if (ap1.getConfigName() != null) {
                return ap1.getConfigName().compareTo(ap2.getConfigName());
            }
            return -1;
        }
    }

    protected int getMetricsCategory() {
        return 106;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
        this.mUserBadgeCache = new UserBadgeCache(getPackageManager());
    }

    public void onResume() {
        super.onResume();
        initPreferences();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        if (savedInstanceState != null && savedInstanceState.containsKey("wifi_ap_state")) {
            this.mAccessPointSavedState = savedInstanceState.getBundle("wifi_ap_state");
        }
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context context = getActivity();
        List<AccessPoint> accessPoints = WifiTracker.getCurrentAccessPoints(context, true, false, true);
        Collections.sort(accessPoints, new C06002());
        preferenceScreen.removeAll();
        int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; i++) {
            AccessPointPreference preference = new AccessPointPreference((AccessPoint) accessPoints.get(i), context, this.mUserBadgeCache, true);
            preference.setIcon(null);
            preferenceScreen.addPreference(preference);
        }
        if (getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w("SavedAccessPointsWifiSettings", "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    private void showDialog(AccessPointPreference accessPoint, boolean edit) {
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint.getAccessPoint();
        showDialog(1);
    }

    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case 1:
                if (this.mDlgAccessPoint == null) {
                    this.mDlgAccessPoint = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                    this.mAccessPointSavedState = null;
                }
                this.mSelectedAccessPoint = this.mDlgAccessPoint;
                this.mDialog = new WifiDialog(getActivity(), this, this.mDlgAccessPoint, false, false, true, WifiSettings.isEditabilityLockedDown(getActivity(), this.mDlgAccessPoint.getConfig()));
                return this.mDialog;
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mDialog != null && this.mDialog.isShowing() && this.mDlgAccessPoint != null) {
            this.mAccessPointSavedState = new Bundle();
            this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
            outState.putBundle("wifi_ap_state", this.mAccessPointSavedState);
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -3 && this.mSelectedAccessPoint != null) {
            this.mWifiManager.forget(this.mSelectedAccessPoint.getConfig().networkId, null);
            if (findSelectedAccessPointPreference() != null) {
                getPreferenceScreen().removePreference(findSelectedAccessPointPreference());
            }
            this.mSelectedAccessPoint = null;
        }
    }

    private AccessPointPreference findSelectedAccessPointPreference() {
        PreferenceScreen prefScreen = getPreferenceScreen();
        int size = prefScreen.getPreferenceCount();
        for (int i = 0; i < size; i++) {
            AccessPointPreference ap = (AccessPointPreference) prefScreen.getPreference(i);
            if (ap.getAccessPoint() != null && this.mSelectedAccessPoint != null && ap.getAccessPoint().getConfig().networkId == this.mSelectedAccessPoint.getConfig().networkId) {
                return ap;
            }
        }
        return null;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (!(preference instanceof AccessPointPreference)) {
            return super.onPreferenceTreeClick(screen, preference);
        }
        showDialog((AccessPointPreference) preference, false);
        return true;
    }
}
