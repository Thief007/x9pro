package com.mediatek.audioprofile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SeekBarVolumizer implements OnSeekBarChangeListener, android.os.Handler.Callback {
    private final AudioManager mAudioManager;
    private final Callback mCallback;
    private final Context mContext;
    private final Uri mDefaultUri;
    private final Handler mHandler;
    private String mKey;
    private int mLastProgress = -1;
    private final int mMaxStreamVolume;
    private int mOriginalStreamVolume;
    private boolean mProfileIsActive = false;
    private final AudioProfileManager mProfileManager;
    private final Receiver mReceiver = new Receiver();
    private Ringtone mRingtone;
    private SeekBar mSeekBar;
    private final int mStreamType;
    private int mSystemVolume = -1;
    private final C0691H mUiHandler = new C0691H();
    private int mVolumeBeforeMute = -1;
    private final Observer mVolumeObserver;

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer seekBarVolumizer);
    }

    private final class C0691H extends Handler {
        private C0691H() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1 && SeekBarVolumizer.this.mSeekBar != null) {
                SeekBarVolumizer.this.mSeekBar.setProgress(msg.arg1);
                SeekBarVolumizer.this.mLastProgress = SeekBarVolumizer.this.mSeekBar.getProgress();
            }
        }

        public void postUpdateSlider(int volume) {
            obtainMessage(1, volume, 0).sendToTarget();
        }
    }

    private final class Observer extends ContentObserver {
        public Observer(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (SeekBarVolumizer.this.mSeekBar != null && SeekBarVolumizer.this.mAudioManager != null) {
                SeekBarVolumizer.this.mUiHandler.postUpdateSlider(SeekBarVolumizer.this.mAudioManager.getStreamVolume(SeekBarVolumizer.this.mStreamType));
            }
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mListening;

        private Receiver() {
        }

        public void setListening(boolean listening) {
            if (this.mListening != listening) {
                this.mListening = listening;
                if (listening) {
                    SeekBarVolumizer.this.mContext.registerReceiver(this, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
                } else {
                    SeekBarVolumizer.this.mContext.unregisterReceiver(this);
                }
            }
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                int streamValue = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                if (!(SeekBarVolumizer.this.mSeekBar == null || streamType != SeekBarVolumizer.this.mStreamType || streamValue == -1)) {
                    SeekBarVolumizer.this.mUiHandler.postUpdateSlider(streamValue);
                }
            }
        }
    }

    public SeekBarVolumizer(Context context, int streamType, Uri defaultUri, Callback callback, String profileKey) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mProfileManager = (AudioProfileManager) context.getSystemService("audioprofile");
        this.mStreamType = streamType;
        this.mKey = profileKey;
        this.mMaxStreamVolume = this.mProfileManager.getStreamMaxVolume(this.mStreamType);
        this.mSystemVolume = this.mAudioManager.getStreamVolume(this.mStreamType);
        Log.d("@M_AudioProfile_SeekBarVolumizer", "" + this.mStreamType + " get Original SYSTEM Volume: " + this.mSystemVolume);
        this.mOriginalStreamVolume = this.mProfileManager.getStreamVolume(this.mKey, this.mStreamType);
        Log.d("@M_AudioProfile_SeekBarVolumizer", "Profile keys: " + this.mKey + " " + this.mStreamType + " get Original Volume: " + this.mOriginalStreamVolume);
        this.mProfileIsActive = this.mProfileManager.isActiveProfile(this.mKey);
        if (this.mProfileIsActive && this.mSystemVolume != this.mOriginalStreamVolume) {
            Log.d("@M_AudioProfile_SeekBarVolumizer", " sync " + this.mStreamType + " original Volume to" + this.mSystemVolume);
            this.mOriginalStreamVolume = this.mSystemVolume;
        }
        HandlerThread thread = new HandlerThread("AudioProfile_SeekBarVolumizer.CallbackHandler");
        thread.start();
        this.mHandler = new Handler(thread.getLooper(), this);
        this.mCallback = callback;
        this.mVolumeObserver = new Observer(this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor(System.VOLUME_SETTINGS[this.mStreamType]), false, this.mVolumeObserver);
        this.mReceiver.setListening(true);
        if (defaultUri == null) {
            if (this.mStreamType == 2) {
                defaultUri = this.mProfileManager.getRingtoneUri(this.mKey, 1);
            } else if (this.mStreamType == 5) {
                defaultUri = this.mProfileManager.getRingtoneUri(this.mKey, 2);
            } else {
                defaultUri = System.DEFAULT_ALARM_ALERT_URI;
            }
        }
        this.mDefaultUri = defaultUri;
        this.mHandler.sendEmptyMessage(3);
    }

    public void setSeekBar(SeekBar seekBar) {
        if (this.mSeekBar != null) {
            this.mSeekBar.setOnSeekBarChangeListener(null);
        }
        this.mSeekBar = seekBar;
        this.mSeekBar.setOnSeekBarChangeListener(null);
        this.mSeekBar.setMax(this.mMaxStreamVolume);
        this.mSeekBar.setProgress(this.mLastProgress > -1 ? this.mLastProgress : this.mOriginalStreamVolume);
        this.mSeekBar.setOnSeekBarChangeListener(this);
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                saveVolume();
                break;
            case 1:
                onStartSample();
                break;
            case 2:
                onStopSample();
                break;
            case 3:
                onInitSample();
                break;
            default:
                Log.e("AudioProfile_SeekBarVolumizer", "invalid SeekBarVolumizer message: " + msg.what);
                break;
        }
        return true;
    }

    private void onInitSample() {
        this.mRingtone = RingtoneManager.getRingtone(this.mContext, this.mDefaultUri);
        if (this.mRingtone != null) {
            this.mRingtone.setStreamType(this.mStreamType);
        }
    }

    private void postStartSample() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), (long) (isSamplePlaying() ? 1000 : 0));
    }

    private void onStartSample() {
        if (!isSamplePlaying()) {
            if (this.mCallback != null) {
                Log.v("AudioProfile_SeekBarVolumizer", "Start sample.");
                this.mCallback.onSampleStarting(this);
            }
            if (this.mRingtone != null) {
                try {
                    this.mRingtone.play();
                } catch (Throwable e) {
                    Log.w("AudioProfile_SeekBarVolumizer", "Error playing ringtone, stream " + this.mStreamType, e);
                }
            }
        }
    }

    void postStopSample() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
    }

    private void onStopSample() {
        if (this.mRingtone != null) {
            this.mRingtone.stop();
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if (fromTouch) {
            postSetVolume(progress);
        }
    }

    void postSetVolume(int progress) {
        this.mLastProgress = progress;
        this.mHandler.removeMessages(0);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0));
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        postStartSample();
    }

    public boolean isSamplePlaying() {
        return this.mRingtone != null ? this.mRingtone.isPlaying() : false;
    }

    public void stopSample() {
        postStopSample();
    }

    private boolean isSilentProfileActive() {
        return this.mAudioManager.getRingerMode() != 2;
    }

    private void setVolume(int streamType, int volume, boolean flag) {
        if (streamType == 2) {
            if (flag) {
                this.mAudioManager.setAudioProfileStreamVolume(this.mStreamType, volume, 0);
                this.mAudioManager.setAudioProfileStreamVolume(5, volume, 0);
                return;
            }
            this.mAudioManager.setStreamVolume(2, volume, 0);
            this.mAudioManager.setStreamVolume(5, volume, 0);
        } else if (flag) {
            this.mAudioManager.setAudioProfileStreamVolume(streamType, volume, 0);
        } else {
            this.mAudioManager.setStreamVolume(streamType, volume, 0);
        }
    }

    public void saveVolume() {
        Log.d("@M_AudioProfile_SeekBarVolumizer", "" + this.mStreamType + " Save Last Volume " + this.mLastProgress);
        this.mProfileManager.setStreamVolume(this.mKey, this.mStreamType, this.mLastProgress);
        if (this.mStreamType == 2) {
            this.mProfileManager.setStreamVolume(this.mKey, 5, this.mLastProgress);
        }
        if (this.mProfileManager.isActiveProfile(this.mKey)) {
            Log.d("@M_AudioProfile_SeekBarVolumizer", "" + this.mStreamType + " Active, save system Volume " + this.mLastProgress);
            setVolume(this.mStreamType, this.mLastProgress, false);
        } else if (!isSilentProfileActive()) {
            Log.d("@M_AudioProfile_SeekBarVolumizer", "saveVolume: " + this.mStreamType + " not Active, Revert system Volume " + this.mSystemVolume);
        }
    }

    public void ringtoneChanged() {
        Log.d("@M_AudioProfile_SeekBarVolumizer", "Ringtone changed.");
        Uri newRingtoneUri = null;
        if (this.mStreamType == 2) {
            newRingtoneUri = this.mProfileManager.getRingtoneUri(this.mKey, 1);
        } else if (this.mStreamType == 5) {
            newRingtoneUri = this.mProfileManager.getRingtoneUri(this.mKey, 2);
        }
        if (newRingtoneUri != null) {
            this.mRingtone = RingtoneManager.getRingtone(this.mContext, newRingtoneUri);
            if (this.mRingtone != null) {
                this.mRingtone.setStreamType(this.mStreamType);
            }
        }
    }
}
