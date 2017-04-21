package com.mediatek.systemui.statusbar.networktype;

import android.telephony.ServiceState;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;
import java.util.HashMap;
import java.util.Map;

public class NetworkTypeUtils {
    public static final int[] VOLTEICON = new int[]{R.drawable.stat_sys_volte1, R.drawable.stat_sys_volte2};
    public static final int[] WFCICON = new int[]{R.drawable.stat_sys_wfc1, R.drawable.stat_sys_wfc2};
    static final Map<Integer, Integer> sNetworkTypeIcons = new HashMap<Integer, Integer>() {
        {
            put(Integer.valueOf(5), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(6), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(12), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(14), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(4), Integer.valueOf(R.drawable.stat_sys_network_type_1x));
            put(Integer.valueOf(7), Integer.valueOf(R.drawable.stat_sys_network_type_1x));
            put(Integer.valueOf(2), Integer.valueOf(R.drawable.stat_sys_network_type_e));
            put(Integer.valueOf(3), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(13), Integer.valueOf(R.drawable.stat_sys_network_type_4g));
            put(Integer.valueOf(8), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(9), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(10), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(15), Integer.valueOf(R.drawable.stat_sys_network_type_3g));
            put(Integer.valueOf(18), Integer.valueOf(0));
        }
    };

    public static int getNetworkTypeIcon(ServiceState serviceState, Config config, boolean hasService) {
        int i = 0;
        if (!hasService) {
            return 0;
        }
        int tempNetworkType = getNetworkType(serviceState);
        Integer iconId = (Integer) sNetworkTypeIcons.get(Integer.valueOf(tempNetworkType));
        if (iconId == null) {
            if (tempNetworkType != 0) {
                if (config.showAtLeast3G) {
                    i = R.drawable.stat_sys_network_type_3g;
                } else {
                    i = R.drawable.stat_sys_network_type_g;
                }
            }
            iconId = Integer.valueOf(i);
        }
        Log.d("NetworkTypeUtils", "getNetworkTypeIcon iconId = " + iconId);
        return iconId.intValue();
    }

    private static int getNetworkType(ServiceState serviceState) {
        int type = 0;
        if (serviceState != null) {
            type = serviceState.getDataNetworkType() != 0 ? serviceState.getDataNetworkType() : serviceState.getVoiceNetworkType();
        }
        Log.d("NetworkTypeUtils", "getNetworkType: type=" + type);
        return type;
    }

    public static int getDataNetTypeFromServiceState(int srcDataNetType, ServiceState sState) {
        int destDataNetType = srcDataNetType;
        if ((destDataNetType == 13 || destDataNetType == 139) && sState != null) {
            destDataNetType = sState.getProprietaryDataRadioTechnology() == 0 ? 13 : 139;
        }
        Log.d("NetworkTypeUtils", "getDataNetTypeFromServiceState:srcDataNetType = " + srcDataNetType + ", destDataNetType " + destDataNetType);
        return destDataNetType;
    }
}
