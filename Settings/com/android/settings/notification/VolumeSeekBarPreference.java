package com.android.settings.notification;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager.OnActivityStopListener;
import android.preference.SeekBarPreference;
import android.preference.SeekBarVolumizer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.R;
import java.util.Objects;

public class VolumeSeekBarPreference extends SeekBarPreference implements OnActivityStopListener {
    private Callback mCallback;
    private int mIconResId;
    private ImageView mIconView;
    private int mMuteIconResId;
    private boolean mMuted;
    private SeekBar mSeekBar;
    private boolean mStopped;
    private int mStream;
    private String mSuppressionText;
    private TextView mSuppressionTextView;
    private SeekBarVolumizer mVolumizer;
    private boolean mZenMuted;

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer seekBarVolumizer);

        void onStreamValueChanged(int i, int i2);
    }

    class C04561 implements android.preference.SeekBarVolumizer.Callback {
        C04561() {
        }

        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (VolumeSeekBarPreference.this.mCallback != null) {
                VolumeSeekBarPreference.this.mCallback.onSampleStarting(sbv);
            }
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            if (VolumeSeekBarPreference.this.mCallback != null) {
                VolumeSeekBarPreference.this.mCallback.onStreamValueChanged(VolumeSeekBarPreference.this.mStream, progress);
            }
        }

        public void onMuted(boolean muted, boolean zenMuted) {
            if (VolumeSeekBarPreference.this.mMuted != muted || VolumeSeekBarPreference.this.mZenMuted != zenMuted) {
                VolumeSeekBarPreference.this.mMuted = muted;
                VolumeSeekBarPreference.this.mZenMuted = zenMuted;
                VolumeSeekBarPreference.this.updateIconView();
            }
        }
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_volume_slider);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumeSeekBarPreference(Context context) {
        this(context, null);
    }

    public void setStream(int stream) {
        this.mStream = stream;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void onActivityResume() {
        if (this.mStopped) {
            init();
        }
    }

    public void onActivityStop() {
        this.mStopped = true;
        if (this.mVolumizer != null) {
            this.mVolumizer.stop();
        }
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        if (this.mStream == 0) {
            Log.w("VolumeSeekBarPreference", "No stream found, not binding volumizer");
            return;
        }
        this.mSeekBar = (SeekBar) view.findViewById(16909197);
        this.mIconView = (ImageView) view.findViewById(16908294);
        this.mSuppressionTextView = (TextView) view.findViewById(R.id.suppression_text);
        init();
    }

    private void init() {
        if (this.mSeekBar != null) {
            getPreferenceManager().registerOnActivityStopListener(this);
            android.preference.SeekBarVolumizer.Callback sbvc = new C04561();
            Uri mediaVolumeUri = this.mStream == 3 ? getMediaVolumeUri() : null;
            if (this.mVolumizer == null) {
                this.mVolumizer = new SeekBarVolumizer(getContext(), this.mStream, mediaVolumeUri, sbvc);
            }
            this.mVolumizer.start();
            this.mVolumizer.setSeekBar(this.mSeekBar);
            updateIconView();
            this.mCallback.onStreamValueChanged(this.mStream, this.mSeekBar.getProgress());
            updateSuppressionText();
            if (!isEnabled()) {
                this.mSeekBar.setEnabled(false);
                this.mVolumizer.stop();
            }
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        this.mCallback.onStreamValueChanged(this.mStream, progress);
    }

    private void updateIconView() {
        if (this.mIconView != null) {
            if (this.mIconResId != 0) {
                this.mIconView.setImageResource(this.mIconResId);
            } else if (this.mMuteIconResId == 0 || !this.mMuted || this.mZenMuted) {
                this.mIconView.setImageDrawable(getIcon());
            } else {
                this.mIconView.setImageResource(this.mMuteIconResId);
            }
        }
    }

    public void showIcon(int resId) {
        if (this.mIconResId != resId) {
            this.mIconResId = resId;
            updateIconView();
        }
    }

    public void setMuteIcon(int resId) {
        if (this.mMuteIconResId != resId) {
            this.mMuteIconResId = resId;
            updateIconView();
        }
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.media_volume);
    }

    public void setSuppressionText(String text) {
        if (!Objects.equals(text, this.mSuppressionText)) {
            this.mSuppressionText = text;
            updateSuppressionText();
        }
    }

    private void updateSuppressionText() {
        int i = 4;
        if (this.mSuppressionTextView != null && this.mSeekBar != null) {
            int i2;
            this.mSuppressionTextView.setText(this.mSuppressionText);
            boolean showSuppression = !TextUtils.isEmpty(this.mSuppressionText);
            TextView textView = this.mSuppressionTextView;
            if (showSuppression) {
                i2 = 0;
            } else {
                i2 = 4;
            }
            textView.setVisibility(i2);
            SeekBar seekBar = this.mSeekBar;
            if (!showSuppression) {
                i = 0;
            }
            seekBar.setVisibility(i);
        }
    }
}
