package com.mediatek.settings.deviceinfo;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;

public class RadioButtonPreference extends CheckBoxPreference {
    private OnClickListener mListener;
    private String mMountPath;

    public interface OnClickListener {
    }

    public RadioButtonPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mListener = null;
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public RadioButtonPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 16842895);
    }

    public RadioButtonPreference(Context context) {
        this(context, null);
    }

    public void onClick() {
        if (!isChecked()) {
            setChecked(true);
            callChangeListener(Boolean.valueOf(true));
        }
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        TextView title = (TextView) view.findViewById(16908310);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }

    public void setPath(String path) {
        this.mMountPath = path;
    }
}
