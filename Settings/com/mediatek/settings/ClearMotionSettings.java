package com.mediatek.settings;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;
import com.mediatek.galleryfeature.clearmotion.ClearMotionQualityJni;
import java.io.IOException;

public class ClearMotionSettings extends Activity implements OnCheckedChangeListener, OnCompletionListener, OnPreparedListener, Callback {
    private Switch mActionBarSwitch;
    private OnErrorListener mErrorListener = new C07202();
    private View mLineView;
    private MediaPlayer mMediaPlayer;
    private TextView mOffText;
    private TextView mOnText;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private BroadcastReceiver mUpdateClearMotionStatusReceiver = new C07191();

    class C07191 extends BroadcastReceiver {
        C07191() {
        }

        public void onReceive(Context context1, Intent intent) {
            Log.d("@M_ClearMotionSettingsLog", "mUpdateClearMotionStatusReceiver");
            ClearMotionSettings.this.updateClearMotionStatus();
            ClearMotionSettings.this.updateClearMotionDemo(ClearMotionSettings.this.mActionBarSwitch.isChecked());
            ClearMotionSettings.this.prepareVideo();
        }
    }

    class C07202 implements OnErrorListener {
        C07202() {
        }

        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d("@M_ClearMotionSettingsLog", "play error: " + what);
            Log.d("@M_ClearMotionSettingsLog", "play error: " + extra);
            ClearMotionSettings.this.releaseMediaPlayer();
            return false;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_view);
        initViews();
    }

    private void initViews() {
        this.mLineView = findViewById(R.id.line_view);
        this.mOnText = (TextView) findViewById(R.id.text_on);
        this.mOffText = (TextView) findViewById(R.id.text_off);
        this.mOnText.setBackgroundColor(Color.argb(155, 0, 0, 0));
        this.mOffText.setBackgroundColor(Color.argb(155, 0, 0, 0));
        this.mActionBarSwitch = new Switch(getLayoutInflater().getContext());
        this.mActionBarSwitch.setPaddingRelative(0, 0, getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding), 0);
        getActionBar().setDisplayOptions(20, 20);
        getActionBar().setCustomView(this.mActionBarSwitch, new LayoutParams(-2, -2, 8388629));
        getActionBar().setTitle(R.string.clear_motion_title);
        this.mActionBarSwitch.setOnCheckedChangeListener(this);
        updateClearMotionStatus();
        this.mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        this.mSurfaceHolder = this.mSurfaceView.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(3);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        return true;
    }

    public void onDestroy() {
        getActionBar().setCustomView(null);
        super.onDestroy();
    }

    public void onResume() {
        super.onResume();
        updateClearMotionDemo(this.mActionBarSwitch.isChecked());
        registerReceiver(this.mUpdateClearMotionStatusReceiver, new IntentFilter("com.mediatek.clearmotion.DIMMED_UPDATE"));
    }

    private void updateClearMotionStatus() {
        if (this.mActionBarSwitch != null) {
            Log.d("@M_ClearMotionSettingsLog", "updateClearMotionStatus");
            this.mActionBarSwitch.setChecked(SystemProperties.get("persist.sys.display.clearMotion", "0").equals("1"));
            this.mActionBarSwitch.setEnabled(SystemProperties.get("sys.display.clearMotion.dimmed", "0").equals("0"));
        }
    }

    private void updateClearMotionDemo(boolean status) {
        int i;
        int i2 = 8;
        int i3 = 0;
        Log.d("@M_ClearMotionSettingsLog", "updateClearMotionDemo status: " + status);
        View view = this.mLineView;
        if (status) {
            i = 0;
        } else {
            i = 8;
        }
        view.setVisibility(i);
        TextView textView = this.mOnText;
        if (status) {
            i = 0;
        } else {
            i = 8;
        }
        textView.setVisibility(i);
        TextView textView2 = this.mOffText;
        if (status) {
            i2 = 0;
        }
        textView2.setVisibility(i2);
        if (status) {
            i3 = 3;
        }
        ClearMotionQualityJni.nativeSetDemoMode(i3);
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(this.mUpdateClearMotionStatusReceiver);
        ClearMotionQualityJni.nativeSetDemoMode(0);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d("@M_ClearMotionSettingsLog", "onCheckedChanged " + isChecked);
        SystemProperties.set("persist.sys.display.clearMotion", isChecked ? "1" : "0");
        updateClearMotionDemo(isChecked);
        prepareVideo();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("@M_ClearMotionSettingsLog", "surfaceCreated " + holder);
        this.mSurfaceHolder = holder;
        this.mMediaPlayer = new MediaPlayer();
        this.mMediaPlayer.setOnCompletionListener(this);
        this.mMediaPlayer.setOnPreparedListener(this);
        this.mMediaPlayer.setOnErrorListener(this.mErrorListener);
        this.mMediaPlayer.setDisplay(this.mSurfaceHolder);
        prepareVideo();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("@M_ClearMotionSettingsLog", "surfaceChanged " + holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("@M_ClearMotionSettingsLog", "surfaceDestroyed " + holder);
        releaseMediaPlayer();
    }

    public void onPrepared(MediaPlayer mp) {
        Log.d("@M_ClearMotionSettingsLog", "onPrepared ");
        this.mMediaPlayer.start();
    }

    public void onCompletion(MediaPlayer mp) {
        Log.d("@M_ClearMotionSettingsLog", "onCompletion ");
        mp.seekTo(0);
        mp.start();
    }

    private void prepareVideo() {
        try {
            if (this.mMediaPlayer != null) {
                this.mMediaPlayer.reset();
                AssetFileDescriptor afd = getAssets().openFd("clear_motion_video.mp4");
                Log.d("@M_ClearMotionSettingsLog", "video path = " + afd.getFileDescriptor());
                this.mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                this.mMediaPlayer.prepare();
                Log.d("@M_ClearMotionSettingsLog", "mMediaPlayer prepare()");
            }
        } catch (IOException e) {
            Log.e("@M_ClearMotionSettingsLog", "unable to open file; error: " + e.getMessage(), e);
            releaseMediaPlayer();
        } catch (IllegalStateException e2) {
            Log.e("@M_ClearMotionSettingsLog", "media player is in illegal state; error: " + e2.getMessage(), e2);
            releaseMediaPlayer();
        }
    }

    private void releaseMediaPlayer() {
        Log.d("@M_ClearMotionSettingsLog", "releaseMediaPlayer");
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
    }
}
