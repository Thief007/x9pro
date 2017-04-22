package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.settings.sim.TelephonyUtils;

public class IccLockSettings extends InstrumentedPreferenceActivity implements OnPinEnteredListener {
    private int mDialogState = 0;
    private TabContentFactory mEmptyTabContent = new C01454();
    private String mError;
    private Handler mHandler = new C01421();
    private boolean mIsAirplaneModeOn = false;
    private ListView mListView;
    private ISettingsMiscExt mMiscExt;
    private String mNewPin;
    private String mOldPin;
    private Phone mPhone;
    private String mPin;
    private EditPinPreference mPinDialog;
    private SwitchPreference mPinToggle;
    private Resources mRes;
    SimHotSwapHandler mSimHotSwapHandler;
    private ISimRoamingExt mSimRoamingExt;
    private final BroadcastReceiver mSimStateReceiver = new C01432();
    private TabHost mTabHost;
    private OnTabChangeListener mTabListener = new C01443();
    private TabWidget mTabWidget;
    private boolean mToState;

    class C01421 extends Handler {
        C01421() {
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            AsyncResult ar = msg.obj;
            IccLockSettings iccLockSettings;
            switch (msg.what) {
                case 100:
                    iccLockSettings = IccLockSettings.this;
                    if (ar.exception != null) {
                        z = false;
                    }
                    iccLockSettings.iccLockChanged(z, msg.arg1);
                    return;
                case 101:
                    iccLockSettings = IccLockSettings.this;
                    if (ar.exception != null) {
                        z = false;
                    }
                    iccLockSettings.iccPinChanged(z, msg.arg1);
                    return;
                case 102:
                    IccLockSettings.this.updatePreferences();
                    return;
                default:
                    return;
            }
        }
    }

    class C01432 extends BroadcastReceiver {
        C01432() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                IccLockSettings.this.mHandler.sendMessage(IccLockSettings.this.mHandler.obtainMessage(102));
            } else if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                IccLockSettings.this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
                IccLockSettings.this.updatePreferences();
            }
        }
    }

    class C01443 implements OnTabChangeListener {
        C01443() {
        }

        public void onTabChanged(String tabId) {
            Phone phone = null;
            SubscriptionInfo sir = SubscriptionManager.from(IccLockSettings.this.getBaseContext()).getActiveSubscriptionInfoForSimSlotIndex(Integer.parseInt(tabId));
            IccLockSettings iccLockSettings = IccLockSettings.this;
            if (sir != null) {
                phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
            }
            iccLockSettings.mPhone = phone;
            Log.d("IccLockSettings", "onTabChanged()... mPhone: " + IccLockSettings.this.mPhone);
            IccLockSettings.this.updatePreferences();
            IccLockSettings.this.changeSimTitle();
        }
    }

    class C01454 implements TabContentFactory {
        C01454() {
        }

        public View createTabContent(String tag) {
            return new View(IccLockSettings.this.mTabHost.getContext());
        }
    }

    class C01465 implements OnSimHotSwapListener {
        C01465() {
        }

        public void onSimHotSwap() {
            Log.d("IccLockSettings", "onSimHotSwap, finish Activity~~");
            IccLockSettings.this.finish();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        int numSims = ((TelephonyManager) context.getSystemService("phone")).getSimCount();
        if (Utils.isMonkeyRunning()) {
            finish();
            return;
        }
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getApplicationContext());
        this.mMiscExt = UtilsExt.getMiscPlugin(this);
        this.mSimRoamingExt = UtilsExt.getSimRoamingExtPlugin(this);
        addPreferencesFromResource(R.xml.sim_lock_settings);
        this.mPinDialog = (EditPinPreference) findPreference("sim_pin");
        this.mPinToggle = (SwitchPreference) findPreference("sim_toggle");
        if (savedInstanceState != null && savedInstanceState.containsKey("dialogState")) {
            this.mDialogState = savedInstanceState.getInt("dialogState");
            this.mPin = savedInstanceState.getString("dialogPin");
            this.mError = savedInstanceState.getString("dialogError");
            this.mToState = savedInstanceState.getBoolean("enableState");
            switch (this.mDialogState) {
                case 3:
                    this.mOldPin = savedInstanceState.getString("oldPinCode");
                    break;
                case 4:
                    this.mOldPin = savedInstanceState.getString("oldPinCode");
                    this.mNewPin = savedInstanceState.getString("newPinCode");
                    break;
            }
        }
        this.mPinDialog.setOnPinEnteredListener(this);
        getPreferenceScreen().setPersistent(false);
        if (numSims > 1) {
            Phone phone;
            setContentView(R.layout.icc_lock_tabs);
            this.mTabHost = (TabHost) findViewById(16908306);
            this.mTabWidget = (TabWidget) findViewById(16908307);
            this.mListView = (ListView) findViewById(16908298);
            this.mTabHost.setup();
            this.mTabHost.setOnTabChangedListener(this.mTabListener);
            this.mTabHost.clearAllTabs();
            SubscriptionManager sm = SubscriptionManager.from(this);
            for (int i = 0; i < numSims; i++) {
                Object string;
                SubscriptionInfo subInfo = sm.getActiveSubscriptionInfoForSimSlotIndex(i);
                TabHost tabHost = this.mTabHost;
                String valueOf = String.valueOf(i);
                if (subInfo == null) {
                    string = context.getString(R.string.sim_editor_title, new Object[]{Integer.valueOf(i + 1)});
                } else {
                    string = subInfo.getDisplayName();
                }
                tabHost.addTab(buildTabSpec(valueOf, String.valueOf(string)));
            }
            SubscriptionInfo sir = sm.getActiveSubscriptionInfoForSimSlotIndex(0);
            if (sir == null) {
                phone = null;
            } else {
                phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
            }
            this.mPhone = phone;
        } else {
            this.mPhone = PhoneFactory.getDefaultPhone();
            changeSimTitle();
        }
        Log.d("IccLockSettings", "onCreate()... mPhone: " + this.mPhone);
        this.mRes = getResources();
        updatePreferences();
        this.mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new C01465());
        setTitle(this.mMiscExt.customizeSimDisplayString(getTitle().toString(), -1));
    }

    private void updatePreferences() {
        boolean z;
        boolean z2 = false;
        EditPinPreference editPinPreference = this.mPinDialog;
        if (this.mPhone == null || this.mIsAirplaneModeOn) {
            z = false;
        } else {
            z = true;
        }
        editPinPreference.setEnabled(z);
        SwitchPreference switchPreference = this.mPinToggle;
        if (!(this.mPhone == null || this.mIsAirplaneModeOn)) {
            z2 = true;
        }
        switchPreference.setEnabled(z2);
        if (this.mPhone != null) {
            this.mPinToggle.setChecked(this.mPhone.getIccCard().getIccLockEnabled());
        }
    }

    protected int getMetricsCategory() {
        return 56;
    }

    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mSimStateReceiver, filter);
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getApplicationContext());
        updatePreferences();
        if (this.mDialogState != 0) {
            showPinDialog();
        } else {
            resetDialogState();
        }
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.mSimStateReceiver);
    }

    protected void onSaveInstanceState(Bundle out) {
        if (this.mPinDialog.isDialogOpen()) {
            out.putInt("dialogState", this.mDialogState);
            out.putString("dialogPin", this.mPinDialog.getEditText().getText().toString());
            out.putString("dialogError", this.mError);
            out.putBoolean("enableState", this.mToState);
            switch (this.mDialogState) {
                case 3:
                    out.putString("oldPinCode", this.mOldPin);
                    return;
                case 4:
                    out.putString("oldPinCode", this.mOldPin);
                    out.putString("newPinCode", this.mNewPin);
                    return;
                default:
                    return;
            }
        }
        super.onSaveInstanceState(out);
    }

    private void showPinDialog() {
        if (this.mDialogState != 0) {
            setDialogValues();
            this.mPinDialog.showPinDialog();
        }
    }

    private void setDialogValues() {
        this.mPinDialog.setText(this.mPin);
        String message = "";
        switch (this.mDialogState) {
            case 1:
                CharSequence string;
                message = this.mRes.getString(R.string.sim_enter_pin);
                EditPinPreference editPinPreference = this.mPinDialog;
                if (this.mToState) {
                    string = this.mRes.getString(R.string.sim_enable_sim_lock);
                } else {
                    string = this.mRes.getString(R.string.sim_disable_sim_lock);
                }
                editPinPreference.setDialogTitle(string);
                break;
            case 2:
                message = this.mRes.getString(R.string.sim_enter_old);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
            case 3:
                message = this.mRes.getString(R.string.sim_enter_new);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
            case 4:
                message = this.mRes.getString(R.string.sim_reenter_new);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
        }
        if (this.mError != null) {
            message = this.mError + "\n" + message;
            this.mError = null;
        }
        Log.d("IccLockSettings", "setDialogValues mDialogState = " + this.mDialogState);
        this.mPinDialog.setDialogMessage(message);
        changeDialogStrings(this.mPinDialog.getDialogTitle().toString(), message);
    }

    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (positiveResult) {
            this.mPin = preference.getText();
            if (reasonablePin(this.mPin)) {
                switch (this.mDialogState) {
                    case 1:
                        tryChangeIccLockState();
                        break;
                    case 2:
                        this.mOldPin = this.mPin;
                        this.mDialogState = 3;
                        this.mError = null;
                        this.mPin = null;
                        showPinDialog();
                        break;
                    case 3:
                        this.mNewPin = this.mPin;
                        this.mDialogState = 4;
                        this.mPin = null;
                        showPinDialog();
                        break;
                    case 4:
                        if (!this.mPin.equals(this.mNewPin)) {
                            this.mError = this.mRes.getString(R.string.sim_pins_dont_match);
                            this.mDialogState = 3;
                            this.mPin = null;
                            showPinDialog();
                            break;
                        }
                        this.mError = null;
                        tryChangePin();
                        break;
                }
                return;
            }
            this.mError = this.mRes.getString(R.string.sim_bad_pin);
            if (isResumed()) {
                showPinDialog();
            }
            return;
        }
        resetDialogState();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean z = false;
        if (preference == this.mPinToggle) {
            this.mToState = this.mPinToggle.isChecked();
            SwitchPreference switchPreference = this.mPinToggle;
            if (!this.mToState) {
                z = true;
            }
            switchPreference.setChecked(z);
            this.mDialogState = 1;
            showPinDialog();
        } else if (preference == this.mPinDialog) {
            this.mDialogState = 2;
            return false;
        }
        return true;
    }

    private void tryChangeIccLockState() {
        Message callback = Message.obtain(this.mHandler, 100);
        if (this.mPhone != null) {
            this.mPhone.getIccCard().setIccLockEnabled(this.mToState, this.mPin, callback);
            this.mPinToggle.setEnabled(false);
        }
    }

    private void iccLockChanged(boolean success, int attemptsRemaining) {
        if (success) {
            this.mPinToggle.setChecked(this.mToState);
            this.mSimRoamingExt.showPinToast(this.mToState);
        } else {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining), 1).show();
        }
        this.mPinToggle.setEnabled(true);
        resetDialogState();
    }

    private void iccPinChanged(boolean success, int attemptsRemaining) {
        if (success) {
            Toast.makeText(this, this.mMiscExt.customizeSimDisplayString(this.mRes.getString(R.string.sim_change_succeeded), this.mPhone.getSubId()), 0).show();
        } else {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining), 1).show();
        }
        resetDialogState();
    }

    private void tryChangePin() {
        Message callback = Message.obtain(this.mHandler, 101);
        if (this.mPhone != null) {
            this.mPhone.getIccCard().changeIccLockPassword(this.mOldPin, this.mNewPin, callback);
        }
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;
        if (attemptsRemaining == 0) {
            displayMessage = this.mRes.getString(R.string.wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = this.mRes.getQuantityString(R.plurals.wrong_pin_code, attemptsRemaining, new Object[]{Integer.valueOf(attemptsRemaining)});
        } else {
            displayMessage = this.mRes.getString(R.string.pin_failed);
        }
        displayMessage = this.mMiscExt.customizeSimDisplayString(displayMessage, this.mPhone.getSubId());
        Log.d("IccLockSettings", "getPinPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    private boolean reasonablePin(String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 8) {
            return false;
        }
        return true;
    }

    private void resetDialogState() {
        this.mError = null;
        this.mDialogState = 2;
        this.mPin = "";
        setDialogValues();
        this.mDialogState = 0;
    }

    private TabSpec buildTabSpec(String tag, String title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }

    protected void onDestroy() {
        super.onDestroy();
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
    }

    private void changeSimTitle() {
        if (this.mPhone != null) {
            int subId = this.mPhone.getSubId();
            Log.d("IccLockSettings", "changeSimTitle subId = " + subId);
            this.mPinToggle.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_pin_toggle), subId));
            this.mPinDialog.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_pin_change), subId));
        }
    }

    private void changeDialogStrings(String dialogTitle, String dialogMessage) {
        if (this.mPhone != null) {
            int subId = this.mPhone.getSubId();
            Log.d("IccLockSettings", "changeSimTitle subId = " + subId);
            this.mPinDialog.setDialogTitle(this.mMiscExt.customizeSimDisplayString(dialogTitle, subId));
            this.mPinDialog.setDialogMessage(this.mMiscExt.customizeSimDisplayString(dialogMessage, subId));
        }
    }
}
