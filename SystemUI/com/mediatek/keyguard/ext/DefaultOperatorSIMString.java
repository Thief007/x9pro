package com.mediatek.keyguard.ext;

import android.content.Context;
import com.mediatek.common.PluginImpl;
import com.mediatek.keyguard.ext.IOperatorSIMString.SIMChangedTag;

@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.IOperatorSIMString")
public class DefaultOperatorSIMString implements IOperatorSIMString {
    public String getOperatorSIMString(String sourceStr, int slotId, SIMChangedTag simChangedTag, Context context) {
        return sourceStr;
    }
}
