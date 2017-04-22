package com.mediatek.nfc;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import com.android.settings.R;

/* compiled from: CardEmulationSettings */
class CardEmulationProgressCategory extends PreferenceCategory {
    private boolean mProgress = false;

    public CardEmulationProgressCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_progress_category);
    }

    public void onBindView(View view) {
        super.onBindView(view);
        view.findViewById(R.id.scanning_progress).setVisibility(this.mProgress ? 0 : 8);
    }

    public void setProgress(boolean progressOn) {
        this.mProgress = progressOn;
        notifyChanged();
    }
}
