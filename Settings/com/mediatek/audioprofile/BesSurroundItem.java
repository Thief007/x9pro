package com.mediatek.audioprofile;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;

public class BesSurroundItem extends CheckBoxPreference {
    private OnClickListener mListener;

    public interface OnClickListener {
        void onRadioButtonClicked(BesSurroundItem besSurroundItem);
    }

    public BesSurroundItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mListener = null;
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public BesSurroundItem(Context context, AttributeSet attrs) {
        this(context, attrs, 16842895);
    }

    public BesSurroundItem(Context context) {
        this(context, null);
    }

    void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    public void onClick() {
        if (this.mListener != null) {
            this.mListener.onRadioButtonClicked(this);
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
}
