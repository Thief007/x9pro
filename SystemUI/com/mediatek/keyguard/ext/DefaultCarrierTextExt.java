package com.mediatek.keyguard.ext;

import android.content.Context;
import android.os.SystemProperties;
import com.mediatek.common.PluginImpl;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;

@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.ICarrierTextExt")
public class DefaultCarrierTextExt implements ICarrierTextExt {
    public CharSequence customizeCarrierTextCapital(CharSequence carrierText) {
        if (SystemProperties.get(FeatureOptionUtils.MTK_CT6M_SUPPORT).equals(FeatureOptionUtils.SUPPORT_YES)) {
            return carrierText;
        }
        if (carrierText != null) {
            return carrierText.toString().toUpperCase();
        }
        return null;
    }

    public CharSequence customizeCarrierText(CharSequence carrierText, CharSequence simMessage, int simId) {
        return carrierText;
    }

    public boolean showCarrierTextWhenSimMissing(boolean isSimMissing, int simId) {
        return isSimMissing;
    }

    public CharSequence customizeCarrierTextWhenCardTypeLocked(CharSequence carrierText, Context context, int phoneId, boolean isCardLocked) {
        return carrierText;
    }

    public CharSequence customizeCarrierTextWhenSimMissing(CharSequence carrierText) {
        return carrierText;
    }

    public String customizeCarrierTextDivider(String divider) {
        return divider;
    }
}
