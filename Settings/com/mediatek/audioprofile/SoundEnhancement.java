package com.mediatek.audioprofile;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class SoundEnhancement extends SettingsPreferenceFragment implements Indexable {
    public static final String CLOSE_LOSSLESS_NOTIFICATION = "android.intent.action.LOSSLESS_NOTIFICATION_CLOSE";
    private static final String GET_BESLOUDNESS_STATUS = "GetBesLoudnessStatus";
    private static final String GET_BESLOUDNESS_STATUS_ENABLED = "GetBesLoudnessStatus=1";
    public static final String GET_LOSSLESSBT_STATUS = "LosslessBT_Status";
    public static final String GET_LOSSLESSBT_STATUS_ENABLED = "LosslessBT_Status=1";
    private static final String GET_MUSIC_PLUS_STATUS = "GetMusicPlusStatus";
    private static final String GET_MUSIC_PLUS_STATUS_ENABLED = "GetMusicPlusStatus=1";
    private static final String KEY_BESLOUDNESS = "bes_loudness";
    private static final String KEY_BESSURROUND = "bes_surround";
    private static final String KEY_LOSSLESSBT = "bes_lossless";
    private static final String KEY_MUSIC_PLUS = "music_plus";
    private static final String KEY_SOUND_ENAHCNE = "sound_enhance";
    public static final String LOSSLESS_ADD = "android.intent.action.LOSSLESS_ADD";
    public static final String LOSSLESS_CLOSE = "android.intent.action.LOSSLESS_CLOSE";
    public static final int LOSSLESS_ICON_ID = 2130837537;
    public static final String LOSSLESS_NOT_SUPPORT = "android.intent.action.LOSSLESS_NOT_SUPPORT";
    public static final String LOSSLESS_PLAYING = "android.intent.action.LOSSLESS_PLAYING";
    public static final String LOSSLESS_STOP = "android.intent.action.LOSSLESS_STOP";
    private static final boolean LOSSLESS_SUPPORT = FeatureOption.MTK_LOSSLESS_SUPPORT;
    private static final String MTK_AUDENH_SUPPORT_State = "MTK_AUDENH_SUPPORT";
    private static final String MTK_AUDENH_SUPPORT_off = "MTK_AUDENH_SUPPORT=false";
    private static final String MTK_AUDENH_SUPPORT_on = "MTK_AUDENH_SUPPORT=true";
    private static final String NOTIFICATION_TAG = "Lossless_notification";
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C06921();
    private static final String SET_BESLOUDNESS_DISABLED = "SetBesLoudnessStatus=0";
    private static final String SET_BESLOUDNESS_ENABLED = "SetBesLoudnessStatus=1";
    public static final String SET_LOSSLESSBT_DISABLED = "LosslessBT_Status=0";
    public static final String SET_LOSSLESSBT_ENABLED = "LosslessBT_Status=1";
    public static final String SET_LOSSLESSBT_USERID = "LosslessBT_UserId=";
    private static final String SET_MUSIC_PLUS_DISABLED = "SetMusicPlusStatus=0";
    private static final String SET_MUSIC_PLUS_ENABLED = "SetMusicPlusStatus=1";
    private static final int SOUND_PREFERENCE_NULL_COUNT = 0;
    private static final String TAG = "SoundEnhancement";
    private String mAudenhState = null;
    private AudioManager mAudioManager = null;
    private SwitchPreference mBesLoudnessPref;
    private Preference mBesSurroundPref;
    private Context mContext;
    private SwitchPreference mLosslessBTPref;
    private SwitchPreference mMusicPlusPrf;
    private NotificationManager mNotificationManager;
    private AudioProfileManager mProfileManager;

    static class C06921 extends BaseSearchIndexProvider {
        C06921() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.sound_enhancement_title);
            data.screenTitle = res.getString(R.string.sound_enhancement_title);
            data.keywords = res.getString(R.string.sound_enhancement_title);
            result.add(data);
            return result;
        }
    }

    class C06932 extends BroadcastReceiver {
        C06932() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.d("@M_SoundEnhancement", "get close notification reciver.");
            SoundEnhancement.this.mLosslessBTPref.setChecked(SoundEnhancement.LOSSLESS_SUPPORT);
        }
    }

    public void onCreate(Bundle icicle) {
        Log.d("@M_SoundEnhancement", "onCreate");
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mProfileManager = (AudioProfileManager) getSystemService("audioprofile");
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
        this.mAudioManager = (AudioManager) getSystemService("audio");
        this.mAudenhState = this.mAudioManager.getParameters(MTK_AUDENH_SUPPORT_State);
        Log.d("@M_SoundEnhancement", "AudENH state: " + this.mAudenhState);
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.audioprofile_sound_enhancement);
        this.mMusicPlusPrf = (SwitchPreference) findPreference(KEY_MUSIC_PLUS);
        this.mBesLoudnessPref = (SwitchPreference) findPreference(KEY_BESLOUDNESS);
        this.mBesSurroundPref = findPreference(KEY_BESSURROUND);
        this.mLosslessBTPref = (SwitchPreference) findPreference(KEY_LOSSLESSBT);
        if (!this.mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on)) {
            Log.d("@M_SoundEnhancement", "remove audio enhance preference " + this.mMusicPlusPrf);
            getPreferenceScreen().removePreference(this.mMusicPlusPrf);
        }
        if (!FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            Log.d("@M_SoundEnhancement", "feature option is off, remove BesLoudness preference");
            getPreferenceScreen().removePreference(this.mBesLoudnessPref);
        }
        if (!FeatureOption.MTK_BESSURROUND_SUPPORT) {
            Log.d("@M_SoundEnhancement", "remove BesSurround preference " + this.mBesSurroundPref);
            getPreferenceScreen().removePreference(this.mBesSurroundPref);
        }
        if (!LOSSLESS_SUPPORT) {
            Log.d("@M_SoundEnhancement", "feature option is off, remove BesLosslessPref preference");
            getPreferenceScreen().removePreference(this.mLosslessBTPref);
        }
        getActivity().registerReceiver(new C06932(), new IntentFilter(CLOSE_LOSSLESS_NOTIFICATION));
        setHasOptionsMenu(LOSSLESS_SUPPORT);
    }

    private void updatePreferenceHierarchy() {
        String state;
        if (this.mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on)) {
            state = this.mAudioManager.getParameters(GET_MUSIC_PLUS_STATUS);
            Log.d("@M_SoundEnhancement", "get the state: " + state);
            boolean isChecked = LOSSLESS_SUPPORT;
            if (state != null) {
                if (state.equals(GET_MUSIC_PLUS_STATUS_ENABLED)) {
                    isChecked = true;
                } else {
                    isChecked = LOSSLESS_SUPPORT;
                }
            }
            this.mMusicPlusPrf.setChecked(isChecked);
        }
        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT) {
            state = this.mAudioManager.getParameters(GET_BESLOUDNESS_STATUS);
            Log.d("@M_SoundEnhancement", "get besloudness state: " + state);
            this.mBesLoudnessPref.setChecked(GET_BESLOUDNESS_STATUS_ENABLED.equals(state));
        }
        if (LOSSLESS_SUPPORT) {
            state = this.mAudioManager.getParameters(GET_LOSSLESSBT_STATUS);
            Log.d("@M_SoundEnhancement", "get losslessBT state: " + state);
            boolean checkedStatus = "LosslessBT_Status=1".equals(state);
            Log.d("@M_SoundEnhancement", "update the losslessBT state: " + checkedStatus);
            this.mLosslessBTPref.setChecked(checkedStatus);
        }
    }

    public void onResume() {
        Log.d("@M_SoundEnhancement", "onResume");
        super.onResume();
        updatePreferenceHierarchy();
    }

    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (this.mAudenhState.equalsIgnoreCase(MTK_AUDENH_SUPPORT_on) && this.mMusicPlusPrf == preference) {
            String cmdStr = ((SwitchPreference) preference).isChecked() ? SET_MUSIC_PLUS_ENABLED : SET_MUSIC_PLUS_DISABLED;
            Log.d("@M_SoundEnhancement", " set command about music plus: " + cmdStr);
            this.mAudioManager.setParameters(cmdStr);
        }
        if (FeatureOption.MTK_BESLOUDNESS_SUPPORT && this.mBesLoudnessPref == preference) {
            cmdStr = ((SwitchPreference) preference).isChecked() ? SET_BESLOUDNESS_ENABLED : SET_BESLOUDNESS_DISABLED;
            Log.d("@M_SoundEnhancement", " set command about besloudness: " + cmdStr);
            this.mAudioManager.setParameters(cmdStr);
        }
        if (this.mBesSurroundPref == null) {
            Log.d("@M_SoundEnhancement", " mBesSurroundPref = null");
        } else if (this.mBesSurroundPref.getKey() == null) {
            Log.d("@M_SoundEnhancement", " mBesSurroundPref.getKey() == null)");
        }
        if (this.mBesSurroundPref == preference) {
            Log.d("@M_SoundEnhancement", " mBesSurroundPref onPreferenceTreeClick");
            ((SettingsActivity) getActivity()).startPreferencePanel(BesSurroundSettings.class.getName(), null, -1, this.mContext.getText(R.string.audio_profile_bes_surround_title), null, 0);
        }
        if (LOSSLESS_SUPPORT && this.mLosslessBTPref == preference) {
            if (((SwitchPreference) preference).isChecked()) {
                Toast.makeText(getActivity(), R.string.lossless_toastmessage, 0).show();
                setLosslessStatus("LosslessBT_Status=1");
                this.mLosslessBTPref.setChecked(true);
            } else {
                this.mLosslessBTPref.setChecked(LOSSLESS_SUPPORT);
                cancelNotification(R.drawable.bt_audio);
                setLosslessStatus(SET_LOSSLESSBT_DISABLED);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void setLosslessStatus(String keys) {
        Log.d("@M_SoundEnhancement", " set command about losslessBT: " + keys);
        this.mAudioManager.setParameters(keys);
        if (keys.equals("LosslessBT_Status=1")) {
            Log.d("@M_SoundEnhancement", " LosslessBT userid cmd: " + (SET_LOSSLESSBT_USERID + UserHandle.myUserId()));
            this.mAudioManager.setParameters(SET_LOSSLESSBT_USERID);
        }
    }

    private void addToNotification() {
        Log.d("@M_SoundEnhancement", "Enable the lossless BT.");
        getActivity().sendBroadcastAsUser(new Intent(LOSSLESS_ADD), UserHandle.CURRENT);
    }

    private void cancelNotification(int id) {
        this.mNotificationManager.cancelAsUser(NOTIFICATION_TAG, id, UserHandle.CURRENT);
    }

    protected int getMetricsCategory() {
        return 100004;
    }
}
