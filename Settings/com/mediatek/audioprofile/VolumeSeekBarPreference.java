package com.mediatek.audioprofile;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager.OnActivityStopListener;
import android.preference.SeekBarPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import com.android.settings.R;

public class VolumeSeekBarPreference extends SeekBarPreference implements OnActivityStopListener {
    private Callback mCallback;
    private String mKey;
    private SeekBar mSeekBar;
    private int mStream;
    private SeekBarVolumizer mVolumizer;

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer seekBarVolumizer);
    }

    class C06941 implements com.mediatek.audioprofile.SeekBarVolumizer.Callback {
        C06941() {
        }

        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (VolumeSeekBarPreference.this.mCallback != null) {
                VolumeSeekBarPreference.this.mCallback.onSampleStarting(sbv);
            }
        }
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

    public void onActivityStop() {
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        if (this.mStream == 0) {
            Log.w("VolumeSeekBarPreference", "No stream found, not binding volumizer  ");
            return;
        }
        getPreferenceManager().registerOnActivityStopListener(this);
        SeekBar seekBar = (SeekBar) view.findViewById(16909197);
        if (seekBar != this.mSeekBar) {
            this.mSeekBar = seekBar;
            com.mediatek.audioprofile.SeekBarVolumizer.Callback sbvc = new C06941();
            Uri mediaVolumeUri = this.mStream == 3 ? getMediaVolumeUri() : null;
            if (this.mVolumizer == null) {
                this.mVolumizer = new SeekBarVolumizer(getContext(), this.mStream, mediaVolumeUri, sbvc, this.mKey);
            }
            this.mVolumizer.setSeekBar(this.mSeekBar);
        }
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName() + "/" + R.raw.media_volume);
    }

    public void setProfile(String key) {
        this.mKey = key;
    }

    public SeekBarVolumizer getSeekBar() {
        return this.mVolumizer;
    }
}
