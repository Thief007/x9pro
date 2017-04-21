package com.mediatek.keyguard.ext;

import android.content.Context;

public interface IOperatorSIMString {

    public enum SIMChangedTag {
        SIMTOUIM,
        UIMSIM,
        DELSIM
    }

    String getOperatorSIMString(String str, int i, SIMChangedTag sIMChangedTag, Context context);
}
