package com.android.settings.nfc;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.settings.R;

public class HowItWorks extends Activity {

    class C04231 implements OnClickListener {
        C04231() {
        }

        public void onClick(View v) {
            HowItWorks.this.finish();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_payment_how_it_works);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        ((Button) findViewById(R.id.nfc_how_it_works_button)).setOnClickListener(new C04231());
    }

    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
