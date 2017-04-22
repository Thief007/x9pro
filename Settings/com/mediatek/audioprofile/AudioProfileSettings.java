package com.mediatek.audioprofile;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import com.mediatek.common.audioprofile.AudioProfileListener;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IAudioProfileExt;
import java.util.ArrayList;
import java.util.List;

public class AudioProfileSettings extends SettingsPreferenceFragment implements OnClickListener, Indexable {
    private static final int DIALOG_DELETE = 3;
    private static final int DIALOG_ERROR = 1;
    private static final int DIALOG_NAME = 0;
    private static final int DIALOG_RESET = 2;
    private static final int ERROR_COUNT_OVERFLOW = 2;
    private static final int ERROR_NAME_EXIST = 0;
    private static final int ERROR_NAME_LENGTH = 1;
    private static final String GENERAL_PREF_KEY = "mtk_audioprofile_general";
    private static final int H_RESET_SUCCESS = 11;
    private static final String MEETING_PREF_KEY = "mtk_audioprofile_meeting";
    private static final int MENUID_ADD = 1;
    private static final int MENUID_DELETE = 4;
    private static final int MENUID_ENABLE = 2;
    private static final int MENUID_RENAME = 3;
    private static final int MENUID_RESET = 2;
    private static final String OUTDOOR_PREF_KEY = "mtk_audioprofile_outdoor";
    private static final String PREDEFINEDCATEGORY = "predefine";
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new C06854();
    private static final String SILENT_PREF_KEY = "mtk_audioprofile_silent";
    private static final String TAG = "AudioProfileSettings:";
    private static final String XLOGTAG = "Settings/AudioP";
    private static String mEditProfileKey;
    private static int mErrorType;
    private static int mMenuId;
    private static AudioProfilePreference mPref;
    private final BroadcastReceiver mBroadcastReceiver = new C06832();
    private Context mContext;
    public int mCurrentDialogId = -1;
    private boolean mCustomerExist = true;
    private List<AudioProfilePreference> mCustomerProfilePrefList;
    private String mDefaultKey;
    private EditText mEditText = null;
    private IAudioProfileExt mExt;
    private AudioProfilePreference mGeneralPref;
    private Handler mHandler = null;
    private boolean mIsSwitchUser = false;
    private final AudioProfileListener mListener = new C06821();
    private PreferenceCategory mPredefineParent;
    private AudioProfileManager mProfileManager;
    private final View.OnClickListener mProfileSettingListener = new C06843();
    private String[] mProfileTitle;
    private String mRenameDialogtext;

    class C06821 extends AudioProfileListener {
        C06821() {
        }

        public void onProfileChanged(String profileKey) {
            super.onProfileChanged(profileKey);
            Log.d("@M_Settings/AudioP", "AudioProfileSettings:onPerfileChanged:key " + profileKey);
            AudioProfileSettings audioProfileSettings = AudioProfileSettings.this;
            if (profileKey == null) {
                profileKey = AudioProfileSettings.this.mDefaultKey;
            }
            AudioProfilePreference activePreference = (AudioProfilePreference) audioProfileSettings.findPreference(profileKey);
            if (activePreference != null) {
                activePreference.setChecked();
            }
        }
    }

    class C06832 extends BroadcastReceiver {
        C06832() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                Log.d("@M_Settings/AudioP", "User switched");
                AudioProfileSettings.this.mIsSwitchUser = true;
            }
        }
    }

    class C06843 implements View.OnClickListener {
        C06843() {
        }

        public void onClick(View v) {
            String key = (String) v.getTag();
            Log.d("@M_Settings/AudioP", "on Click ImageView: " + key);
            Bundle args = new Bundle();
            args.putString("profileKey", key);
            ((SettingsActivity) AudioProfileSettings.this.getActivity()).startPreferencePanel(Editprofile.class.getName(), args, 0, null, null, 0);
        }
    }

    static class C06854 extends BaseSearchIndexProvider {
        C06854() {
        }

        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.notification_settings);
            data.screenTitle = res.getString(R.string.notification_settings);
            data.keywords = res.getString(R.string.notification_settings);
            result.add(data);
            return result;
        }
    }

    class C06865 extends Handler {
        C06865() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == AudioProfileSettings.H_RESET_SUCCESS) {
                AudioProfileSettings.this.mPredefineParent.setEnabled(true);
                AudioProfileSettings.this.mGeneralPref.dynamicShowSummary();
            }
        }
    }

    private class ResetTask extends AsyncTask<String, Void, Integer> {
        private static final int RESET_ONGOING = 1;
        private static final int RESET_SUCCESS = 0;

        private ResetTask() {
        }

        protected Integer doInBackground(String... arg) {
            AudioProfileSettings.this.mProfileManager.resetProfiles();
            SystemProperties.set("persist.sys.boot_ringtone", "0");
            return Integer.valueOf(0);
        }

        protected void onPostExecute(Integer result) {
            if (result.intValue() == 0) {
                AudioProfileSettings.this.mHandler.sendEmptyMessage(AudioProfileSettings.H_RESET_SUCCESS);
            }
        }
    }

    public void onCreate(Bundle icicle) {
        Log.d("@M_Settings/AudioP", "onCreate");
        super.onCreate(icicle);
        this.mContext = getActivity();
        this.mExt = UtilsExt.getAudioProfilePlugin(getActivity());
        this.mCustomerProfilePrefList = new ArrayList();
        this.mProfileManager = (AudioProfileManager) getSystemService("audioprofile");
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.audioprofile_settings);
        this.mExt.addCustomizedPreference(getPreferenceScreen());
        this.mPredefineParent = (PreferenceCategory) findPreference(PREDEFINEDCATEGORY);
        ((AudioProfilePreference) findPreference(GENERAL_PREF_KEY)).setOnSettingsClickListener(this.mProfileSettingListener);
        ((AudioProfilePreference) findPreference(SILENT_PREF_KEY)).setOnSettingsClickListener(this.mProfileSettingListener);
        ((AudioProfilePreference) findPreference(MEETING_PREF_KEY)).setOnSettingsClickListener(this.mProfileSettingListener);
        ((AudioProfilePreference) findPreference(OUTDOOR_PREF_KEY)).setOnSettingsClickListener(this.mProfileSettingListener);
        this.mDefaultKey = "mtk_audioprofile_" + Scenario.GENERAL.toString().toLowerCase();
        this.mGeneralPref = (AudioProfilePreference) findPreference(this.mDefaultKey);
        this.mHandler = new C06865();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        setHasOptionsMenu(true);
    }

    private void updatePreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        List<String> profileKeys = this.mProfileManager.getAllProfileKeys();
        if (profileKeys == null) {
            Log.d("@M_Settings/AudioP", "AudioProfileSettings:profileKey size is 0");
            return;
        }
        Log.d("@M_Settings/AudioP", "AudioProfileSettings:profileKey size" + profileKeys.size());
        int i;
        if (profileKeys.size() == 4) {
            for (i = 0; i < this.mCustomerProfilePrefList.size(); i++) {
                this.mPredefineParent.removePreference((Preference) this.mCustomerProfilePrefList.get(i));
            }
            this.mCustomerProfilePrefList.clear();
            this.mCustomerExist = false;
        } else {
            for (i = 0; i < this.mCustomerProfilePrefList.size(); i++) {
                this.mPredefineParent.removePreference((Preference) this.mCustomerProfilePrefList.get(i));
            }
            this.mCustomerProfilePrefList.clear();
            for (String profileKey : profileKeys) {
                addPreference(root, profileKey);
            }
        }
    }

    private AudioProfilePreference addPreference(PreferenceScreen root, String key) {
        AudioProfilePreference preference = null;
        if (Scenario.CUSTOM.equals(AudioProfileManager.getScenario(key))) {
            preference = new AudioProfilePreference(getActivity());
            preference.setOnSettingsClickListener(this.mProfileSettingListener);
            preference.setProfileKey(key);
            this.mCustomerProfilePrefList.add(preference);
            Log.d("@M_Settings/AudioP", "AudioProfileSettings:Add into profile list " + preference.getKey());
            if (preference.getKey().equals(mEditProfileKey) && mPref != null) {
                Log.d("@M_Settings/AudioP", "AudioProfileSettings:resume mPref: rename profile, key = " + mEditProfileKey);
                mPref = preference;
            }
            if (!this.mCustomerExist) {
                this.mCustomerExist = true;
            }
            this.mPredefineParent.addPreference(preference);
            Log.d("@M_Settings/AudioP", "AudioProfileSettings:Add into profile preference " + preference.getKey());
            String name = this.mProfileManager.getProfileName(key);
            if (name != null) {
                preference.setTitle(name, false);
                Log.d("@M_Settings/AudioP", TAG + String.valueOf(preference.getTitle()));
            }
        }
        return preference;
    }

    private void updateActivePreference() {
        String key = this.mProfileManager.getActiveProfileKey();
        Log.d("@M_Settings/AudioP", "AudioProfileSettings:key " + key);
        if (key == null) {
            key = this.mDefaultKey;
        }
        AudioProfilePreference activePreference = (AudioProfilePreference) findPreference(key);
        if (activePreference != null) {
            activePreference.setChecked();
        }
    }

    private void dynamicshowSummary() {
        this.mGeneralPref.dynamicShowSummary();
        for (AudioProfilePreference pref : this.mCustomerProfilePrefList) {
            pref.dynamicShowSummary();
        }
    }

    public void onResume() {
        Log.d("@M_Settings/AudioP", "AudioProfileSettings:onResume");
        super.onResume();
        if (!dismissFrag()) {
            updatePreferenceHierarchy();
            dynamicshowSummary();
            updateActivePreference();
            registerForContextMenu(getListView());
            this.mProfileManager.listenAudioProfie(this.mListener, 1);
            this.mExt.onAudioProfileSettingResumed(this);
        }
    }

    public void onPause() {
        super.onPause();
        this.mProfileManager.listenAudioProfie(this.mListener, 0);
        this.mExt.onAudioProfileSettingPaused(this);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof AudioProfilePreference) {
            AudioProfilePreference pref = (AudioProfilePreference) preference;
            this.mProfileManager.setActiveProfile(pref.getKey());
            pref.setChecked();
        }
        if (this.mExt.onPreferenceTreeClick(preferenceScreen, preference)) {
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.audio_profile_add).setShowAsAction(0);
        menu.add(0, 2, 0, R.string.audio_profile_reset).setShowAsAction(0);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            if (this.mProfileManager.getProfileCount() >= 10) {
                mErrorType = 2;
                showDialog(1);
                return true;
            }
            mMenuId = 1;
            showDialog(0);
            return true;
        } else if (item.getItemId() != 2) {
            return false;
        } else {
            showDialog(2);
            return true;
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(((AdapterContextMenuInfo) menuInfo).position);
        if (pref instanceof AudioProfilePreference) {
            String key = pref.getKey();
            this.mRenameDialogtext = ((AudioProfilePreference) pref).getTitle();
            menu.setHeaderTitle(this.mRenameDialogtext);
            menu.add(0, 2, 0, R.string.audio_profile_enable);
            AudioProfileManager audioProfileManager = this.mProfileManager;
            if (Scenario.CUSTOM.equals(AudioProfileManager.getScenario(key))) {
                menu.add(0, 3, 0, R.string.audio_profile_rename);
                menu.add(0, 4, 0, R.string.audio_profile_delete);
            }
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(((AdapterContextMenuInfo) item.getMenuInfo()).position);
        if (!(pref instanceof AudioProfilePreference)) {
            return false;
        }
        mPref = (AudioProfilePreference) pref;
        mEditProfileKey = mPref.getKey();
        Log.d("@M_Settings/AudioP", "onContextItemSelected  mPref = " + mPref.toString() + " mEditProfileKey = " + mEditProfileKey);
        switch (item.getItemId()) {
            case 2:
                this.mProfileManager.setActiveProfile(mPref.getKey());
                mPref.setChecked();
                return true;
            case 3:
                mMenuId = 3;
                showDialog(0);
                return true;
            case 4:
                showDialog(3);
                return true;
            default:
                return false;
        }
    }

    public Dialog onCreateDialog(int id) {
        this.mCurrentDialogId = id;
        if (id == 0) {
            int i;
            View content = getActivity().getLayoutInflater().inflate(R.layout.dialog_edittext, null);
            this.mEditText = (EditText) content.findViewById(R.id.edittext);
            if (this.mEditText != null) {
                CharSequence charSequence;
                this.mEditText.setInputType(65537);
                EditText editText = this.mEditText;
                if (mMenuId == 1) {
                    charSequence = "";
                } else {
                    charSequence = this.mRenameDialogtext;
                }
                editText.setText(charSequence);
            }
            Builder builder = new Builder(getActivity());
            if (mMenuId == 1) {
                i = R.string.audio_profile_add;
            } else {
                i = R.string.audio_profile_rename;
            }
            Dialog dialog = builder.setTitle(i).setMessage(R.string.audio_profile_message_rename).setView(content).setPositiveButton(17039370, this).setNegativeButton(17039360, null).create();
            dialog.getWindow().setSoftInputMode(36);
            return dialog;
        } else if (id == 1) {
            int stringId = 0;
            switch (mErrorType) {
                case 0:
                    stringId = R.string.audio_profile_message_name_error;
                    break;
                case 1:
                    stringId = R.string.audio_profile_message_name_length_wrong;
                    break;
                case 2:
                    stringId = R.string.audio_profile_message_overflow;
                    break;
            }
            return new Builder(getActivity()).setTitle(R.string.audio_profile_error).setIcon(17301543).setMessage(stringId).setPositiveButton(17039370, this).create();
        } else if (id == 2) {
            return new Builder(getActivity()).setTitle(R.string.audio_profile_reset).setIcon(17301543).setMessage(R.string.audio_profile_message_reset).setPositiveButton(17039370, this).setNegativeButton(17039360, null).create();
        } else {
            if (id != 3) {
                return null;
            }
            return new Builder(getActivity()).setTitle(R.string.audio_profile_delete).setIcon(17301543).setMessage(getString(R.string.audio_profile_message_delete, new Object[]{mPref.getTitle()})).setPositiveButton(17039370, this).setNegativeButton(17039360, null).create();
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        Log.d("@M_Settings/AudioP", "onClick");
        Log.d("@M_Settings/AudioP", "" + button);
        if (button != -1) {
            Log.d("@M_Settings/AudioP", "return");
            return;
        }
        switch (this.mCurrentDialogId) {
            case 0:
                String title = this.mEditText == null ? "" : String.valueOf(this.mEditText.getText());
                if (title.length() != 0) {
                    if (!this.mProfileManager.isNameExist(title)) {
                        if (mMenuId != 1) {
                            Log.d("@M_Settings/AudioP", "onClick  mPref.setTitle = " + title);
                            mPref.setTitle(title, true);
                            break;
                        }
                        String profileKey = this.mProfileManager.addProfile();
                        Log.d("@M_Settings/AudioP", "AudioProfileSettings:add profile Key" + profileKey);
                        this.mProfileManager.setProfileName(profileKey, title);
                        AudioProfilePreference activePreference = addPreference(getPreferenceScreen(), profileKey);
                        if (activePreference != null) {
                            this.mProfileManager.setActiveProfile(profileKey);
                            activePreference.setChecked();
                            activePreference.dynamicShowSummary();
                            break;
                        }
                        this.mProfileManager.setActiveProfile(this.mDefaultKey);
                        this.mGeneralPref.setChecked();
                        break;
                    }
                    mErrorType = 0;
                    showDialog(1);
                    break;
                }
                mErrorType = 1;
                showDialog(1);
                break;
            case 1:
                if (mErrorType != 2) {
                    showDialog(0);
                    break;
                }
                break;
            case 2:
                if (!(this.mCustomerProfilePrefList == null || this.mCustomerProfilePrefList.size() == 0)) {
                    for (int i = 0; i < this.mCustomerProfilePrefList.size(); i++) {
                        this.mPredefineParent.removePreference((Preference) this.mCustomerProfilePrefList.get(i));
                    }
                    this.mCustomerProfilePrefList.clear();
                    this.mCustomerExist = false;
                }
                this.mPredefineParent.setEnabled(false);
                new ResetTask().execute(new String[0]);
                break;
            case 3:
                if (mPref.isChecked()) {
                    this.mProfileManager.setActiveProfile(this.mDefaultKey);
                    this.mGeneralPref.setChecked();
                }
                this.mProfileManager.deleteProfile(mPref.getKey());
                this.mPredefineParent.removePreference(mPref);
                this.mCustomerProfilePrefList.remove(mPref);
                if (this.mCustomerProfilePrefList.size() == 0) {
                    this.mCustomerExist = false;
                    break;
                }
                break;
            default:
                Log.d("@M_Settings/AudioP", "AudioProfileSettings:unrecongnized dialog id is" + this.mCurrentDialogId);
                break;
        }
    }

    private boolean dismissFrag() {
        Log.d("@M_Settings/AudioP", "Dismiss fragment.");
        if (!this.mIsSwitchUser) {
            return false;
        }
        this.mIsSwitchUser = false;
        Log.d("@M_Settings/AudioP", "AudioProfileSettings will be finished.");
        finish();
        return true;
    }

    protected int getMetricsCategory() {
        return 100004;
    }
}
