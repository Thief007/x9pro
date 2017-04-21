package com.mediatek.keyguard.ext;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

public interface IKeyguardUtilExt {
    void customizeCarrierTextGravity(TextView textView);

    void customizePinPukLockView(int i, ImageView imageView, TextView textView);

    boolean lockImmediatelyWhenScreenTimeout();

    void showToastWhenUnlockPinPuk(Context context, int i);
}
