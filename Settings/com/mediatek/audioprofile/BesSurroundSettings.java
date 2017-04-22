package com.mediatek.audioprofile;

import android.app.ActionBar.LayoutParams;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.audioprofile.BesSurroundItem.OnClickListener;

public class BesSurroundSettings extends SettingsPreferenceFragment implements OnCheckedChangeListener, OnClickListener {
    private BesSurroundItem mMovieMode;
    private BesSurroundItem mMusicMode;
    private AudioProfileManager mProfileManager;
    private Switch mSwitch;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.audioprofile_bessurrond_settings);
        this.mMovieMode = (BesSurroundItem) findPreference("movie_mode");
        this.mMusicMode = (BesSurroundItem) findPreference("music_mode");
        this.mMovieMode.setOnClickListener(this);
        this.mMusicMode.setOnClickListener(this);
        this.mProfileManager = (AudioProfileManager) getSystemService("audioprofile");
    }

    public void onResume() {
        super.onResume();
        createSwitch();
        initBesSurroundStatus();
    }

    public void createSwitch() {
        this.mSwitch = new Switch(getActivity());
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitch.setPaddingRelative(0, 0, activity.getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding), 0);
        activity.getActionBar().setDisplayOptions(16, 16);
        activity.getActionBar().setCustomView(this.mSwitch, new LayoutParams(-2, -2, 8388629));
        this.mSwitch.setOnCheckedChangeListener(this);
        activity.setTitle(R.string.audio_profile_bes_surround_title);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d("@M_Settings/AudioP", "BesSurroundSettings:onCheckedChanged: " + isChecked);
        this.mProfileManager.setBesSurroundState(isChecked);
        getPreferenceScreen().setEnabled(isChecked);
    }

    private void initBesSurroundStatus() {
        this.mSwitch.setChecked(this.mProfileManager.getBesSurroundState());
        getPreferenceScreen().setEnabled(this.mSwitch.isChecked());
        if (this.mProfileManager.getBesSurroundMode() == 0) {
            this.mMovieMode.setChecked(true);
            this.mMusicMode.setChecked(false);
            return;
        }
        this.mMovieMode.setChecked(false);
        this.mMusicMode.setChecked(true);
    }

    public void onRadioButtonClicked(BesSurroundItem emiter) {
        if (emiter == this.mMovieMode) {
            this.mProfileManager.setBesSurroundMode(0);
            this.mMusicMode.setChecked(false);
        } else if (emiter == this.mMusicMode) {
            this.mProfileManager.setBesSurroundMode(1);
            this.mMovieMode.setChecked(false);
        }
        emiter.setChecked(true);
    }

    protected int getMetricsCategory() {
        return 100004;
    }
}
