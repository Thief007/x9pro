package com.mediatek.nfc;

import android.content.Context;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.settings.R;

/* compiled from: CardEmulationSettings */
class SecurityItemPreference extends Preference implements OnClickListener {
    private boolean mChecked = false;
    private RadioButton mPreferenceButton = null;
    private TextView mPreferenceTitle = null;
    private CharSequence mTitleValue = "";

    public SecurityItemPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.card_emulation_item);
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        this.mPreferenceTitle = (TextView) view.findViewById(R.id.preference_title);
        this.mPreferenceTitle.setText(this.mTitleValue);
        this.mPreferenceButton = (RadioButton) view.findViewById(R.id.preference_radiobutton);
        this.mPreferenceButton.setOnClickListener(this);
        this.mPreferenceButton.setChecked(this.mChecked);
    }

    public void setTitle(CharSequence title) {
        if (this.mPreferenceTitle == null) {
            this.mTitleValue = title;
        }
        if (!title.equals(this.mTitleValue)) {
            this.mTitleValue = title;
            this.mPreferenceTitle.setText(this.mTitleValue);
        }
    }

    public void onClick(View v) {
        boolean newValue = !isChecked();
        if (newValue) {
            if (setChecked(newValue)) {
                callChangeListener(Boolean.valueOf(newValue));
                Log.d("@M_SecurityItemPreference", "button.onClick");
            }
            return;
        }
        Log.d("@M_SecurityItemPreference", "button.onClick return");
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    public boolean setChecked(boolean checked) {
        if (this.mPreferenceButton == null) {
            Log.d("@M_SecurityItemPreference", "setChecked return");
            this.mChecked = checked;
            return false;
        } else if (this.mChecked == checked) {
            return false;
        } else {
            this.mPreferenceButton.setChecked(checked);
            this.mChecked = checked;
            return true;
        }
    }
}
