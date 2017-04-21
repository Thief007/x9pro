package com.mediatek.keyguard.ext;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName = "com.mediatek.keyguard.ext.IKeyguardUtilExt")
public class DefaultKeyguardUtilExt implements IKeyguardUtilExt {
    private static final String TAG = "DefaultKeyguardUtilExt";

    public void showToastWhenUnlockPinPuk(Context context, int simLockType) {
        Log.d(TAG, "showToastWhenUnlockPinPuk");
    }

    public void customizePinPukLockView(int phoneId, ImageView imageView, TextView textView) {
        Log.d(TAG, "customizePinPukLockView");
    }

    public void customizeCarrierTextGravity(TextView view) {
        Log.d(TAG, "customizeCarrierTextGravity view = " + view);
    }

    public boolean lockImmediatelyWhenScreenTimeout() {
        Log.d(TAG, "lockImmediatelyWhenScreenTimeout, ret=false");
        return false;
    }
}
