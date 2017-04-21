package com.mediatek.systemui.ext;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public interface IStatusBarPlmnPlugin {
    void addPlmn(LinearLayout linearLayout, Context context);

    View customizeCarrierLabel(ViewGroup viewGroup, View view);

    void setPlmnVisibility(int i);

    boolean supportCustomizeCarrierLabel();

    void updateCarrierLabel(int i, boolean z, boolean z2, String[] strArr);

    void updateCarrierLabelVisibility(boolean z, boolean z2);
}
