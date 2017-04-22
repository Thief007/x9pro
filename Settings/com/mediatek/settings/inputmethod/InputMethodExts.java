package com.mediatek.settings.inputmethod;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.Log;
import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import java.util.List;

public class InputMethodExts {
    private Context mContext;
    private boolean mIsOnlyImeSettings;
    private PreferenceCategory mVoiceCategory;
    private Intent mVoiceControlIntent;
    private Preference mVoiceUiPref;

    public InputMethodExts(Context context, boolean isOnlyImeSettings, PreferenceCategory voiceCategory, PreferenceCategory pointCategory) {
        this.mContext = context;
        this.mIsOnlyImeSettings = isOnlyImeSettings;
        this.mVoiceCategory = voiceCategory;
    }

    public void initExtendsItems() {
        this.mVoiceUiPref = new Preference(this.mContext);
        this.mVoiceUiPref.setKey("voice_ui");
        this.mVoiceUiPref.setTitle(this.mContext.getString(R.string.voice_ui_title));
        if (this.mVoiceCategory != null) {
            this.mVoiceCategory.addPreference(this.mVoiceUiPref);
        }
        if (this.mIsOnlyImeSettings || !(FeatureOption.MTK_VOICE_UI_SUPPORT || isWakeupSupport(this.mContext))) {
            Log.d("@M_InputMethodAndLanguageSettings", "going to remove voice ui feature ");
            if (this.mVoiceUiPref != null && this.mVoiceCategory != null) {
                Log.d("@M_InputMethodAndLanguageSettings", "removed done");
                this.mVoiceCategory.removePreference(this.mVoiceUiPref);
            }
        }
    }

    public void resumeExtendsItems() {
        this.mVoiceControlIntent = new Intent("com.mediatek.voicecommand.VOICE_CONTROL_SETTINGS");
        this.mVoiceControlIntent.setFlags(268435456);
        List<ResolveInfo> apps = this.mContext.getPackageManager().queryIntentActivities(this.mVoiceControlIntent, 0);
        if (apps == null || apps.size() == 0) {
            Log.d("@M_InputMethodAndLanguageSettings", "going to remove voice ui feature ");
            if (this.mVoiceUiPref != null && this.mVoiceCategory != null) {
                Log.d("@M_InputMethodAndLanguageSettings", "removed done");
                this.mVoiceCategory.removePreference(this.mVoiceUiPref);
            }
        } else if (!this.mIsOnlyImeSettings && FeatureOption.MTK_VOICE_UI_SUPPORT) {
            Log.d("@M_InputMethodAndLanguageSettings", "going to add voice ui feature ");
            if (this.mVoiceUiPref != null && this.mVoiceCategory != null) {
                this.mVoiceCategory.addPreference(this.mVoiceUiPref);
            }
        }
    }

    public void onClickExtendsItems(String preferKey) {
        if ("voice_ui".equals(preferKey)) {
            this.mContext.startActivity(this.mVoiceControlIntent);
        }
    }

    public static boolean isWakeupSupport(Context context) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        if (am == null) {
            Log.e("@M_InputMethodAndLanguageSettings", "isWakeupSupport get audio service is null");
            return false;
        }
        String state = am.getParameters("MTK_VOW_SUPPORT");
        if (state != null) {
            return state.equalsIgnoreCase("MTK_VOW_SUPPORT=true");
        }
        return false;
    }
}
