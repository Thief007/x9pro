package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;
import com.android.settingslib.wifi.AccessPoint;
import com.mediatek.systemui.statusbar.defaultaccount.DefaultAccountStatus;
import java.util.List;

public interface NetworkController {

    public interface SignalCallback {
        void setDefaultAccountStatus(DefaultAccountStatus defaultAccountStatus);

        void setEthernetIndicators(IconState iconState);

        void setIsAirplaneMode(IconState iconState);

        void setMobileDataEnabled(boolean z);

        void setMobileDataIndicators(IconState iconState, IconState iconState2, int i, int i2, int i3, boolean z, boolean z2, int i4, int i5, String str, String str2, boolean z3, int i6);

        void setNoSims(boolean z);

        void setSubs(List<SubscriptionInfo> list);

        void setVolteStatusIcon(int i);

        void setWifiIndicators(boolean z, IconState iconState, IconState iconState2, boolean z2, boolean z3, String str);
    }

    public interface AccessPointController {

        public interface AccessPointCallback {
            void onAccessPointsChanged(List<AccessPoint> list);

            void onSettingsActivityTriggered(Intent intent);
        }

        void addAccessPointCallback(AccessPointCallback accessPointCallback);

        boolean canConfigWifi();

        boolean connect(AccessPoint accessPoint);

        int getIcon(AccessPoint accessPoint);

        void removeAccessPointCallback(AccessPointCallback accessPointCallback);

        void scanForAccessPoints();
    }

    public interface MobileDataController {

        public static class DataUsageInfo {
            public String carrier;
            public long limitLevel;
            public String period;
            public long usageLevel;
            public long warningLevel;
        }

        DataUsageInfo getDataUsageInfo();

        boolean isDefaultDataSimExist();

        boolean isMobileDataEnabled();

        boolean isMobileDataSupported();

        void setMobileDataEnabled(boolean z);
    }

    public static class IconState {
        public final String contentDescription;
        public final int icon;
        public final boolean visible;

        public IconState(boolean visible, int icon, String contentDescription) {
            this.visible = visible;
            this.icon = icon;
            this.contentDescription = contentDescription;
        }

        public IconState(boolean visible, int icon, int contentDescription, Context context) {
            this(visible, icon, context.getString(contentDescription));
        }
    }

    void addSignalCallback(SignalCallback signalCallback);

    AccessPointController getAccessPointController();

    MobileDataController getMobileDataController();

    boolean hasMobileDataFeature();

    void removeSignalCallback(SignalCallback signalCallback);

    void setWifiEnabled(boolean z);
}
