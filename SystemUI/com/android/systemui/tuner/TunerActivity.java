package com.android.systemui.tuner;

import android.app.Activity;
import android.os.Bundle;

public class TunerActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(16908290, new TunerFragment()).commit();
    }
}
