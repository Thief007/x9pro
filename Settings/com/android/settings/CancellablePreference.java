package com.android.settings;

import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class CancellablePreference extends Preference implements OnClickListener {
    private boolean mCancellable;
    private OnCancelListener mListener;

    public interface OnCancelListener {
        void onCancel(CancellablePreference cancellablePreference);
    }

    public void setCancellable(boolean isCancellable) {
        this.mCancellable = isCancellable;
        notifyChanged();
    }

    public void setOnCancelListener(OnCancelListener listener) {
        this.mListener = listener;
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        ImageView cancel = (ImageView) view.findViewById(R.id.cancel);
        cancel.setVisibility(this.mCancellable ? 0 : 4);
        cancel.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (this.mListener != null) {
            this.mListener.onCancel(this);
        }
    }
}
