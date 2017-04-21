package com.mediatek.systemui.qs.tiles;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.mediatek.audioprofile.AudioProfileManager;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import com.mediatek.common.audioprofile.AudioProfileListener;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.util.ArrayList;
import java.util.List;

public class AudioProfileTile extends QSTile<BooleanState> {
    private static /* synthetic */ int[] -com_mediatek_audioprofile_AudioProfileManager$ScenarioSwitchesValues;
    private static final boolean ENABLE_AUDIO_PROFILE = SIMHelper.isMtkAudioProfilesSupport();
    private AudioProfileListener mAudioProfileListenr = new AudioProfileListener() {
        public void onProfileChanged(String profileKey) {
            if (AudioProfileTile.ENABLE_AUDIO_PROFILE && profileKey != null) {
                if (AudioProfileTile.this.mUpdating) {
                    Scenario senario = AudioProfileManager.getScenario(profileKey);
                    Log.d("AudioProfileTile", "onProfileChanged onReceive called, profile type is: " + senario);
                    if (senario != null) {
                        AudioProfileTile.this.updateProfileView(senario);
                    }
                } else {
                    Log.d("AudioProfileTile", "onProfileChanged !mUpdating");
                }
            }
        }
    };
    private int mAudioState = R.drawable.ic_qs_custom_on;
    private Scenario mCurrentScenario;
    private Runnable mDismissProfileSwitchDialogRunnable = new Runnable() {
        public void run() {
            Log.d("AudioProfileTile", "mDismissProfileSwitchDialogRunnable");
            if (AudioProfileTile.this.mProfileSwitchDialog != null && AudioProfileTile.this.mProfileSwitchDialog.isShowing()) {
                AudioProfileTile.this.mProfileSwitchDialog.dismiss();
            }
            AudioProfileTile.this.removeAllProfileSwitchDialogCallbacks();
        }
    };
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9000:
                    AudioProfileTile.this.showProfileSwitchDialog();
                    return;
                default:
                    return;
            }
        }
    };
    private ImageView mMettingProfileIcon;
    private ImageView mMuteProfileIcon;
    private ImageView mNormalProfileIcon;
    private ImageView mOutdoorSwitchIcon;
    private List<String> mProfileKeys;
    private AudioProfileManager mProfileManager;
    private Dialog mProfileSwitchDialog;
    private OnClickListener mProfileSwitchListener = new OnClickListener() {
        public void onClick(View v) {
            if (AudioProfileTile.ENABLE_AUDIO_PROFILE) {
                for (int i = 0; i < AudioProfileTile.this.mProfileKeys.size(); i++) {
                    if (v.getTag().equals(AudioProfileTile.this.mProfileKeys.get(i))) {
                        Log.d("AudioProfileTile", "onClick called, profile clicked is:" + ((String) AudioProfileTile.this.mProfileKeys.get(i)));
                        String key = (String) AudioProfileTile.this.mProfileKeys.get(i);
                        AudioProfileTile.this.updateAudioProfile(key);
                        AudioProfileTile.this.updateProfileView(AudioProfileManager.getScenario(key));
                        if (AudioProfileTile.this.mProfileSwitchDialog != null) {
                            AudioProfileTile.this.mProfileSwitchDialog.dismiss();
                        }
                    }
                }
            }
        }
    };
    private boolean mUpdating = false;

    private static /* synthetic */ int[] -getcom_mediatek_audioprofile_AudioProfileManager$ScenarioSwitchesValues() {
        if (-com_mediatek_audioprofile_AudioProfileManager$ScenarioSwitchesValues != null) {
            return -com_mediatek_audioprofile_AudioProfileManager$ScenarioSwitchesValues;
        }
        int[] iArr = new int[Scenario.values().length];
        try {
            iArr[Scenario.CUSTOM.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Scenario.GENERAL.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Scenario.MEETING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Scenario.OUTDOOR.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Scenario.SILENT.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        -com_mediatek_audioprofile_AudioProfileManager$ScenarioSwitchesValues = iArr;
        return iArr;
    }

    public AudioProfileTile(Host host) {
        super(host);
        createProfileSwitchDialog();
        setAudioProfileUpdates(true);
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
    }

    protected void handleClick() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9000));
    }

    protected void handleLongClick() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$AudioProfileSettingsActivity"));
        this.mHost.startActivityDismissingKeyguard(intent);
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = this.mContext.getString(R.string.audio_profile);
        state.visible = true;
        state.icon = ResourceIcon.get(this.mAudioState);
    }

    private void updateAudioProfile(String key) {
        if (key != null) {
            Log.i("AudioProfileTile", "updateAudioProfile called, selected profile is: " + key);
            if (ENABLE_AUDIO_PROFILE) {
                this.mProfileManager.setActiveProfile(key);
            }
            Log.d("AudioProfileTile", "updateAudioProfile called, setActiveProfile is: " + key);
        }
    }

    public int getMetricsCategory() {
        return 111;
    }

    private void showProfileSwitchDialog() {
        createProfileSwitchDialog();
        if (!this.mProfileSwitchDialog.isShowing()) {
            this.mProfileSwitchDialog.show();
            dismissProfileSwitchDialog(4000);
        }
    }

    private void createProfileSwitchDialog() {
        Log.i("AudioProfileTile", "createProfileSwitchDialog");
        this.mProfileSwitchDialog = null;
        this.mProfileSwitchDialog = new Dialog(this.mContext);
        this.mProfileSwitchDialog.requestWindowFeature(1);
        this.mProfileSwitchDialog.setContentView(R.layout.quick_settings_profile_switch_dialog);
        this.mProfileSwitchDialog.setCanceledOnTouchOutside(true);
        this.mProfileSwitchDialog.getWindow().setType(2014);
        LayoutParams attributes = this.mProfileSwitchDialog.getWindow().getAttributes();
        attributes.privateFlags |= 16;
        this.mProfileSwitchDialog.getWindow().clearFlags(2);
        this.mProfileSwitchDialog.getWindow().setFlags(131072, 131072);
        this.mProfileSwitchDialog.getWindow().setFlags(524288, 524288);
        this.mMettingProfileIcon = (ImageView) this.mProfileSwitchDialog.findViewById(R.id.meeting_profile_icon);
        this.mOutdoorSwitchIcon = (ImageView) this.mProfileSwitchDialog.findViewById(R.id.outdoor_profile_icon);
        this.mMuteProfileIcon = (ImageView) this.mProfileSwitchDialog.findViewById(R.id.mute_profile_icon);
        this.mNormalProfileIcon = (ImageView) this.mProfileSwitchDialog.findViewById(R.id.normal_profile_icon);
        View normalProfile = this.mProfileSwitchDialog.findViewById(R.id.normal_profile);
        TextView normalProfileText = (TextView) this.mProfileSwitchDialog.findViewById(R.id.normal_profile_text);
        normalProfileText.setText(this.mContext.getString(R.string.normal));
        FontSizeUtils.updateFontSize(normalProfileText, R.dimen.qs_tile_text_size);
        normalProfile.setOnClickListener(this.mProfileSwitchListener);
        normalProfile.setTag(AudioProfileManager.getProfileKey(Scenario.GENERAL));
        View muteProfile = this.mProfileSwitchDialog.findViewById(R.id.mute_profile);
        TextView muteProfileText = (TextView) this.mProfileSwitchDialog.findViewById(R.id.mute_profile_text);
        muteProfileText.setText(this.mContext.getString(R.string.mute));
        FontSizeUtils.updateFontSize(muteProfileText, R.dimen.qs_tile_text_size);
        muteProfile.setOnClickListener(this.mProfileSwitchListener);
        muteProfile.setTag(AudioProfileManager.getProfileKey(Scenario.SILENT));
        View meetingProfile = this.mProfileSwitchDialog.findViewById(R.id.meeting_profile);
        TextView meetingProfileText = (TextView) this.mProfileSwitchDialog.findViewById(R.id.meeting_profile_text);
        meetingProfileText.setText(this.mContext.getString(R.string.meeting));
        FontSizeUtils.updateFontSize(meetingProfileText, R.dimen.qs_tile_text_size);
        meetingProfile.setOnClickListener(this.mProfileSwitchListener);
        meetingProfile.setTag(AudioProfileManager.getProfileKey(Scenario.MEETING));
        View outdoorProfile = this.mProfileSwitchDialog.findViewById(R.id.outdoor_profile);
        TextView outdoorProfileText = (TextView) this.mProfileSwitchDialog.findViewById(R.id.outdoor_profile_text);
        outdoorProfileText.setText(this.mContext.getString(R.string.outdoor));
        FontSizeUtils.updateFontSize(outdoorProfileText, R.dimen.qs_tile_text_size);
        outdoorProfile.setOnClickListener(this.mProfileSwitchListener);
        outdoorProfile.setTag(AudioProfileManager.getProfileKey(Scenario.OUTDOOR));
        if (this.mCurrentScenario != null) {
            Log.i("AudioProfileTile", "mCurrentScenario != null");
            loadEnabledProfileResource(this.mCurrentScenario);
        }
    }

    private void updateProfileView(Scenario scenario) {
        Log.d("AudioProfileTile", "updateProfileView before");
        loadDisabledProfileResouceForAll();
        loadEnabledProfileResource(scenario);
    }

    private void loadDisabledProfileResouceForAll() {
        Log.d("AudioProfileTile", "loadDisabledProfileResouceForAll");
        this.mNormalProfileIcon.setImageResource(R.drawable.ic_qs_normal_off);
        this.mMettingProfileIcon.setImageResource(R.drawable.ic_qs_meeting_profile_off);
        this.mOutdoorSwitchIcon.setImageResource(R.drawable.ic_qs_outdoor_off);
        this.mMuteProfileIcon.setImageResource(R.drawable.ic_qs_mute_profile_off);
    }

    private void loadEnabledProfileResource(Scenario scenario) {
        Log.d("AudioProfileTile", "loadEnabledProfileResource called, profile is: " + scenario);
        this.mCurrentScenario = scenario;
        switch (-getcom_mediatek_audioprofile_AudioProfileManager$ScenarioSwitchesValues()[scenario.ordinal()]) {
            case 1:
                this.mAudioState = R.drawable.ic_qs_custom_on;
                break;
            case 2:
                this.mNormalProfileIcon.setImageResource(R.drawable.ic_qs_normal_profile_enable);
                this.mAudioState = R.drawable.ic_qs_general_on;
                break;
            case 3:
                this.mMettingProfileIcon.setImageResource(R.drawable.ic_qs_meeting_profile_enable);
                this.mAudioState = R.drawable.ic_qs_meeting_on;
                break;
            case 4:
                this.mOutdoorSwitchIcon.setImageResource(R.drawable.ic_qs_outdoor_profile_enable);
                this.mAudioState = R.drawable.ic_qs_outdoor_on;
                break;
            case 5:
                this.mMuteProfileIcon.setImageResource(R.drawable.ic_qs_mute_profile_enable);
                this.mAudioState = R.drawable.ic_qs_silent_on;
                break;
        }
        this.mAudioState = R.drawable.ic_qs_custom_on;
        refreshState();
    }

    private void dismissProfileSwitchDialog(int timeout) {
        removeAllProfileSwitchDialogCallbacks();
        if (this.mProfileSwitchDialog != null) {
            this.mHandler.postDelayed(this.mDismissProfileSwitchDialogRunnable, (long) timeout);
        }
    }

    private void removeAllProfileSwitchDialogCallbacks() {
        this.mHandler.removeCallbacks(this.mDismissProfileSwitchDialogRunnable);
    }

    public void setAudioProfileUpdates(boolean update) {
        if (update != this.mUpdating) {
            if (ENABLE_AUDIO_PROFILE) {
                this.mProfileManager = (AudioProfileManager) this.mContext.getSystemService("audioprofile");
                this.mProfileManager.listenAudioProfie(this.mAudioProfileListenr, 1);
            }
            this.mProfileKeys = new ArrayList();
            this.mProfileKeys = this.mProfileManager.getPredefinedProfileKeys();
            this.mUpdating = update;
        } else if (ENABLE_AUDIO_PROFILE) {
            this.mProfileManager.listenAudioProfie(this.mAudioProfileListenr, 0);
        }
    }
}
