package com.p003v.hardkey;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import com.android.settings.R;
import java.io.File;
import java.io.FileWriter;

public class HardKeyActivity extends Activity implements OnCheckedChangeListener {
    ImageView mLeftImage;
    RadioButton mLeftRadioButton;
    ImageView mRightImage;
    RadioButton mRightRadioButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("HardKeySettings", "onCreate() :");
        setContentView(R.xml.v_hardkey_settings);
        this.mLeftRadioButton = (RadioButton) findViewById(R.id.left_radio);
        this.mRightRadioButton = (RadioButton) findViewById(R.id.right_radio);
        this.mLeftImage = (ImageView) findViewById(R.id.left_image);
        this.mRightImage = (ImageView) findViewById(R.id.right_image);
        this.mLeftRadioButton.setOnCheckedChangeListener(this);
        this.mRightRadioButton.setOnCheckedChangeListener(this);
    }

    public void onResume() {
        super.onResume();
        Log.d("HardKeySettings", "onResume() :");
        if (SystemProperties.getBoolean("persist.sys.hardkey_switch", false)) {
            this.mLeftRadioButton.setChecked(false);
            this.mRightRadioButton.setChecked(true);
            return;
        }
        this.mLeftRadioButton.setChecked(true);
        this.mRightRadioButton.setChecked(false);
    }

    public void onPause() {
        super.onPause();
        Log.d("HardKeySettings", "onPause() :");
    }

    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        Log.d("HardKeySettings", " onCheckedChanged: isChecked : " + isChecked);
        if (view.getId() == R.id.left_radio) {
            if (isChecked) {
                HardKeyActivity.setSwitchKeyValue(0);
                SystemProperties.set("persist.sys.hardkey_switch", "false");
                this.mLeftRadioButton.setChecked(true);
                this.mRightRadioButton.setChecked(false);
                this.mLeftImage.setImageResource(R.drawable.left_enable);
                this.mRightImage.setImageResource(R.drawable.right_disable);
            }
        } else if (view.getId() == R.id.right_radio && isChecked) {
            HardKeyActivity.setSwitchKeyValue(1);
            SystemProperties.set("persist.sys.hardkey_switch", "true");
            this.mLeftRadioButton.setChecked(false);
            this.mRightRadioButton.setChecked(true);
            this.mLeftImage.setImageResource(R.drawable.left_disable);
            this.mRightImage.setImageResource(R.drawable.right_enable);
        }
    }

    public static void setSwitchKeyValue(int value) {
        Exception e;
        Log.v("HardKeySettings", "setSwitchKeyValue value =====" + value);
        try {
            FileWriter f1 = new FileWriter(new File("sys/devices/bus/bus:touch@/tpd_switch_vkey"));
            try {
                f1.write(String.valueOf(value));
                f1.flush();
                f1.close();
                FileWriter fileWriter = f1;
            } catch (Exception e2) {
                e = e2;
                Log.e("HardKeySettings", " setSwitchKeyValue e " + e.toString());
            }
        } catch (Exception e3) {
            e = e3;
            Log.e("HardKeySettings", " setSwitchKeyValue e " + e.toString());
        }
    }
}
