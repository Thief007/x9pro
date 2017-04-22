package com.mediatek.audioprofile;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.settings.R;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IAudioProfileExt;

public class AudioProfilePreference extends Preference implements OnCheckedChangeListener, OnClickListener {
    private static String sActiveKey = null;
    private static CompoundButton sCurrentChecked = null;
    private RadioButton mCheckboxButton;
    private Context mContext;
    private IAudioProfileExt mExt;
    private LayoutInflater mInflater;
    private String mKey;
    private OnClickListener mOnSettingsClickListener;
    private String mPreferenceSummary;
    private String mPreferenceTitle;
    private AudioProfileManager mProfileManager;
    private TextView mSummary;
    private TextView mTextView;

    class C06811 implements OnClickListener {
        C06811() {
        }

        public void onClick(View v) {
            Log.d("@M_Settings/AudioP", "AudioProfilePreference:onClick " + AudioProfilePreference.this.getKey());
            if (AudioProfilePreference.this.mCheckboxButton.equals(AudioProfilePreference.sCurrentChecked)) {
                Log.d("@M_Settings/AudioP", "AudioProfilePreference:Click the active profile, do nothing return");
            } else if (AudioProfilePreference.sCurrentChecked != null) {
                AudioProfilePreference.sCurrentChecked.setChecked(false);
                AudioProfilePreference.this.mCheckboxButton.setChecked(true);
                AudioProfilePreference.sCurrentChecked = AudioProfilePreference.this.mCheckboxButton;
                AudioProfilePreference.this.mProfileManager.setActiveProfile(AudioProfilePreference.this.mKey);
            }
        }
    }

    public AudioProfilePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPreferenceTitle = null;
        this.mPreferenceSummary = null;
        this.mTextView = null;
        this.mSummary = null;
        this.mCheckboxButton = null;
        this.mContext = context;
        this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        if (super.getTitle() != null) {
            this.mPreferenceTitle = super.getTitle().toString();
        }
        if (super.getSummary() != null) {
            this.mPreferenceSummary = super.getSummary().toString();
        }
        this.mProfileManager = (AudioProfileManager) context.getSystemService("audioprofile");
        this.mKey = getKey();
        this.mExt = UtilsExt.getAudioProfilePlugin(context);
    }

    public AudioProfilePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioProfilePreference(Context context) {
        this(context, null);
    }

    public void setProfileKey(String key) {
        setKey(key);
        this.mKey = key;
    }

    public View onCreateView(ViewGroup parent) {
        Log.d("@M_Settings/AudioP", "AudioProfilePreference:onCreateView " + getKey());
        this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        View view = this.mInflater.inflate(R.layout.audio_profile_item, null);
        this.mCheckboxButton = (RadioButton) view.findViewById(R.id.radiobutton);
        this.mTextView = (TextView) view.findViewById(R.id.profiles_text);
        this.mSummary = (TextView) view.findViewById(R.id.profiles_summary);
        if (this.mCheckboxButton != null) {
            this.mCheckboxButton.setOnClickListener(new C06811());
            this.mCheckboxButton.setChecked(isChecked());
            if (isChecked()) {
                setChecked();
            }
        }
        if (this.mPreferenceTitle == null || this.mTextView == null) {
            Log.d("@M_Settings/AudioP", "AudioProfilePreference:PreferenceTitle is null");
        } else {
            this.mTextView.setText(this.mPreferenceTitle);
        }
        dynamicShowSummary();
        ImageView detailsView = (ImageView) view.findViewById(R.id.deviceDetails);
        ImageView dividerImage = (ImageView) view.findViewById(R.id.divider);
        if (detailsView != null) {
            detailsView.setTag(this.mKey);
            Scenario scenario = AudioProfileManager.getScenario(this.mKey);
            if (Scenario.CUSTOM.equals(scenario) || Scenario.GENERAL.equals(scenario) || this.mExt.isOtherAudioProfileEditable()) {
                detailsView.setOnClickListener(this);
            } else {
                detailsView.setVisibility(8);
                dividerImage.setVisibility(8);
                detailsView.setOnClickListener(null);
            }
        }
        return view;
    }

    public void dynamicShowSummary() {
        Log.d("@M_Settings/AudioP", "AudioProfilePreference:" + this.mKey + " dynamicShowSummary");
        if (this.mSummary != null) {
            Scenario scenario = AudioProfileManager.getScenario(this.mKey);
            if (Scenario.GENERAL.equals(scenario) || Scenario.CUSTOM.equals(scenario)) {
                boolean vibrationEnabled = this.mProfileManager.isVibrationEnabled(this.mKey);
                Log.d("@M_Settings/AudioP", "AudioProfilePreference:vibrationEnabled" + vibrationEnabled);
                if (vibrationEnabled) {
                    this.mSummary.setText(this.mContext.getString(R.string.ring_vibrate_summary));
                } else {
                    this.mSummary.setText(this.mContext.getString(R.string.ring_summary));
                }
            } else if (this.mPreferenceSummary != null) {
                this.mSummary.setText(this.mPreferenceSummary);
            }
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d("@M_Settings/AudioP", "AudioProfilePreference:onCheckedChanged " + isChecked + getKey());
        if (isChecked) {
            if (sCurrentChecked != null) {
                sCurrentChecked.setChecked(false);
            }
            sCurrentChecked = buttonView;
            this.mProfileManager.setActiveProfile(this.mKey);
        }
    }

    public boolean isChecked() {
        if (sActiveKey != null) {
            return getKey().equals(sActiveKey);
        }
        return false;
    }

    public void setChecked() {
        sActiveKey = getKey();
        if (this.mCheckboxButton == null) {
            Log.d("@M_Settings/AudioP", "AudioProfilePreference:mCheckboxButton is null");
        } else if (!this.mCheckboxButton.equals(sCurrentChecked)) {
            if (sCurrentChecked != null) {
                sCurrentChecked.setChecked(false);
            }
            Log.d("@M_Settings/AudioP", "AudioProfilePreference:setChecked" + getKey());
            this.mCheckboxButton.setChecked(true);
            sCurrentChecked = this.mCheckboxButton;
        }
    }

    public void setTitle(String title, boolean setToProfile) {
        this.mPreferenceTitle = title;
        if (setToProfile) {
            this.mProfileManager.setProfileName(this.mKey, title);
        }
        if (this.mTextView != null) {
            this.mTextView.setText(title);
        }
    }

    public String getTitle() {
        return this.mPreferenceTitle;
    }

    public void setOnSettingsClickListener(OnClickListener listener) {
        this.mOnSettingsClickListener = listener;
    }

    public void onClick(View v) {
        if (this.mOnSettingsClickListener != null) {
            this.mOnSettingsClickListener.onClick(v);
        }
    }
}
