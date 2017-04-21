package com.mediatek.keyguard.ext;

import android.content.Context;

public interface ICarrierTextExt {
    CharSequence customizeCarrierText(CharSequence charSequence, CharSequence charSequence2, int i);

    CharSequence customizeCarrierTextCapital(CharSequence charSequence);

    String customizeCarrierTextDivider(String str);

    CharSequence customizeCarrierTextWhenCardTypeLocked(CharSequence charSequence, Context context, int i, boolean z);

    CharSequence customizeCarrierTextWhenSimMissing(CharSequence charSequence);

    boolean showCarrierTextWhenSimMissing(boolean z, int i);
}
