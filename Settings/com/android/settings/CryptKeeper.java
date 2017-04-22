package com.android.settings;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IMountService;
import android.os.storage.IMountService.Stub;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import java.util.List;

public class CryptKeeper extends Activity implements OnEditorActionListener, OnKeyListener, OnTouchListener, TextWatcher {
    private AudioManager mAudioManager;
    protected OnPatternListener mChooseNewLockPatternListener = new C00814();
    private final Runnable mClearPatternRunnable = new C00792();
    private boolean mCooldown = false;
    private boolean mCorrupt;
    private boolean mEncryptionGoneBad;
    private final Runnable mFakeUnlockAttemptRunnable = new C00781();
    private final Handler mHandler = new C00803();
    private LockPatternView mLockPatternView;
    private int mNotificationCountdown = 0;
    private EditText mPasswordEntry;
    private PhoneStateBroadcastReceiver mPhoneStateReceiver;
    private int mReleaseWakeLockCountdown = 0;
    private StatusBarManager mStatusBar;
    private int mStatusString = R.string.enter_password;
    private boolean mValidationComplete;
    private boolean mValidationRequested;
    WakeLock mWakeLock;

    class C00781 implements Runnable {
        C00781() {
        }

        public void run() {
            CryptKeeper.this.handleBadAttempt(Integer.valueOf(1));
        }
    }

    class C00792 implements Runnable {
        C00792() {
        }

        public void run() {
            CryptKeeper.this.mLockPatternView.clearPattern();
        }
    }

    class C00803 extends Handler {
        C00803() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    CryptKeeper.this.updateProgress();
                    return;
                case 2:
                    CryptKeeper.this.notifyUser();
                    return;
                default:
                    return;
            }
        }
    }

    class C00814 implements OnPatternListener {
        C00814() {
        }

        public void onPatternStart() {
            CryptKeeper.this.mLockPatternView.removeCallbacks(CryptKeeper.this.mClearPatternRunnable);
        }

        public void onPatternCleared() {
        }

        public void onPatternDetected(List<Cell> pattern) {
            CryptKeeper.this.mLockPatternView.setEnabled(false);
            if (pattern.size() >= 4) {
                new DecryptTask().execute(new String[]{LockPatternUtils.patternToString(pattern)});
                return;
            }
            CryptKeeper.this.fakeUnlockAttempt(CryptKeeper.this.mLockPatternView);
        }

        public void onPatternCellAdded(List<Cell> list) {
        }
    }

    class C00825 extends AsyncTask<Void, Void, Void> {
        String owner_info;
        int passwordType = 0;
        boolean password_visible;
        boolean pattern_visible;

        C00825() {
        }

        public Void doInBackground(Void... v) {
            boolean z = false;
            try {
                boolean z2;
                IMountService service = CryptKeeper.this.getMountService();
                this.passwordType = service.getPasswordType();
                this.owner_info = service.getField("OwnerInfo");
                if ("0".equals(service.getField("PatternVisible"))) {
                    z2 = false;
                } else {
                    z2 = true;
                }
                this.pattern_visible = z2;
                if (!"0".equals(service.getField("PasswordVisible"))) {
                    z = true;
                }
                this.password_visible = z;
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error calling mount service " + e);
            }
            return null;
        }

        public void onPostExecute(Void v) {
            int i;
            boolean z = true;
            ContentResolver contentResolver = CryptKeeper.this.getContentResolver();
            String str = "show_password";
            if (this.password_visible) {
                i = 1;
            } else {
                i = 0;
            }
            System.putInt(contentResolver, str, i);
            if (this.passwordType == 3) {
                CryptKeeper.this.setContentView(R.layout.crypt_keeper_pin_entry);
                CryptKeeper.this.mStatusString = R.string.enter_pin;
            } else if (this.passwordType == 2) {
                CryptKeeper.this.setContentView(R.layout.crypt_keeper_pattern_entry);
                CryptKeeper.this.setBackFunctionality(false);
                CryptKeeper.this.mStatusString = R.string.enter_pattern;
            } else {
                CryptKeeper.this.setContentView(R.layout.crypt_keeper_password_entry);
                CryptKeeper.this.mStatusString = R.string.enter_password;
            }
            ((TextView) CryptKeeper.this.findViewById(R.id.status)).setText(CryptKeeper.this.mStatusString);
            TextView ownerInfo = (TextView) CryptKeeper.this.findViewById(R.id.owner_info);
            ownerInfo.setText(this.owner_info);
            ownerInfo.setSelected(true);
            CryptKeeper.this.passwordEntryInit();
            CryptKeeper.this.findViewById(16908290).setSystemUiVisibility(4194304);
            if (CryptKeeper.this.mLockPatternView != null) {
                LockPatternView -get2 = CryptKeeper.this.mLockPatternView;
                if (this.pattern_visible) {
                    z = false;
                }
                -get2.setInStealthMode(z);
            }
            if (CryptKeeper.this.mCooldown) {
                CryptKeeper.this.setBackFunctionality(false);
                CryptKeeper.this.cooldown();
            }
        }
    }

    class C00869 implements OnClickListener {
        C00869() {
        }

        public void onClick(View v) {
            CryptKeeper.this.takeEmergencyCallAction();
        }
    }

    private class DecryptTask extends AsyncTask<String, Void, Integer> {
        private DecryptTask() {
        }

        private void hide(int id) {
            View view = CryptKeeper.this.findViewById(id);
            if (view != null) {
                view.setVisibility(8);
            }
        }

        protected void onPreExecute() {
            super.onPreExecute();
            CryptKeeper.this.beginAttempt();
        }

        protected Integer doInBackground(String... params) {
            try {
                return Integer.valueOf(CryptKeeper.this.getMountService().decryptStorage(params[0]));
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error while decrypting...", e);
                return Integer.valueOf(-1);
            }
        }

        protected void onPostExecute(Integer failedAttempts) {
            Log.d("CryptKeeper", "failedAttempts : " + failedAttempts);
            if (failedAttempts.intValue() == 0) {
                if (CryptKeeper.this.mLockPatternView != null) {
                    CryptKeeper.this.mLockPatternView.removeCallbacks(CryptKeeper.this.mClearPatternRunnable);
                    CryptKeeper.this.mLockPatternView.postDelayed(CryptKeeper.this.mClearPatternRunnable, 500);
                }
                ((TextView) CryptKeeper.this.findViewById(R.id.status)).setText(R.string.starting_android);
                hide(R.id.passwordEntry);
                hide(R.id.switch_ime_button);
                hide(R.id.lockPattern);
                hide(R.id.owner_info);
                hide(R.id.emergencyCallButton);
            } else if (failedAttempts.intValue() == 30) {
                Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.REASON", "CryptKeeper.MAX_FAILED_ATTEMPTS");
                CryptKeeper.this.sendBroadcast(intent);
            } else if (failedAttempts.intValue() == -1) {
                CryptKeeper.this.setContentView(R.layout.crypt_keeper_progress);
                CryptKeeper.this.showFactoryReset(true);
            } else {
                CryptKeeper.this.handleBadAttempt(failedAttempts);
            }
        }
    }

    private static class NonConfigurationInstanceState {
        final WakeLock wakelock;

        NonConfigurationInstanceState(WakeLock _wakelock) {
            this.wakelock = _wakelock;
        }
    }

    private class PhoneStateBroadcastReceiver extends BroadcastReceiver {
        private PhoneStateBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PHONE_STATE".equals(action)) {
                Log.d("CryptKeeper", "PhoneStateBroadcastReceiver action:" + action + " state:" + intent.getExtra("state"));
                CryptKeeper.this.updateEmergencyCallButtonState();
            }
        }
    }

    private class ValidationTask extends AsyncTask<Void, Void, Boolean> {
        int state;

        private ValidationTask() {
        }

        protected Boolean doInBackground(Void... params) {
            boolean z = false;
            IMountService service = CryptKeeper.this.getMountService();
            try {
                Log.d("CryptKeeper", "Validating encryption state.");
                this.state = service.getEncryptionState();
                if (this.state == 1) {
                    Log.w("CryptKeeper", "Unexpectedly in CryptKeeper even though there is no encryption.");
                    return Boolean.valueOf(true);
                }
                if (this.state == 0) {
                    z = true;
                }
                return Boolean.valueOf(z);
            } catch (RemoteException e) {
                Log.w("CryptKeeper", "Unable to get encryption state properly");
                return Boolean.valueOf(true);
            }
        }

        protected void onPostExecute(Boolean result) {
            boolean z = true;
            CryptKeeper.this.mValidationComplete = true;
            if (Boolean.FALSE.equals(result)) {
                Log.w("CryptKeeper", "Incomplete, or corrupted encryption detected. Prompting user to wipe.");
                CryptKeeper.this.mEncryptionGoneBad = true;
                CryptKeeper cryptKeeper = CryptKeeper.this;
                if (this.state != -4) {
                    z = false;
                }
                cryptKeeper.mCorrupt = z;
            } else {
                Log.d("CryptKeeper", "Encryption state validated. Proceeding to configure UI");
            }
            CryptKeeper.this.setupUi();
        }
    }

    private void beginAttempt() {
        ((TextView) findViewById(R.id.status)).setText(R.string.checking_decryption);
    }

    private void handleBadAttempt(Integer failedAttempts) {
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 1500);
        }
        if (failedAttempts.intValue() % 10 == 0) {
            this.mCooldown = true;
            cooldown();
            return;
        }
        TextView status = (TextView) findViewById(R.id.status);
        if (30 - failedAttempts.intValue() < 10) {
            status.setText(TextUtils.expandTemplate(getText(R.string.crypt_keeper_warn_wipe), new CharSequence[]{Integer.toString(remainingAttempts)}));
        } else {
            int passwordType = 0;
            try {
                passwordType = getMountService().getPasswordType();
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error calling mount service " + e);
            }
            if (passwordType == 3) {
                status.setText(R.string.cryptkeeper_wrong_pin);
            } else if (passwordType == 2) {
                status.setText(R.string.cryptkeeper_wrong_pattern);
            } else {
                status.setText(R.string.cryptkeeper_wrong_password);
            }
        }
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            this.mLockPatternView.setEnabled(true);
        }
        if (this.mPasswordEntry != null) {
            this.mPasswordEntry.setEnabled(true);
            ((InputMethodManager) getSystemService("input_method")).showSoftInput(this.mPasswordEntry, 0);
            setBackFunctionality(true);
        }
    }

    private boolean isDebugView() {
        return getIntent().hasExtra("com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW");
    }

    private boolean isDebugView(String viewType) {
        return viewType.equals(getIntent().getStringExtra("com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW"));
    }

    private void notifyUser() {
        if (this.mNotificationCountdown > 0) {
            Log.d("CryptKeeper", "Counting down to notify user..." + this.mNotificationCountdown);
            this.mNotificationCountdown--;
        } else if (this.mAudioManager != null) {
            Log.d("CryptKeeper", "Notifying user that we are waiting for input...");
            try {
                this.mAudioManager.playSoundEffect(5, 100);
            } catch (Exception e) {
                Log.w("CryptKeeper", "notifyUser: Exception while playing sound: " + e);
            }
        }
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessageDelayed(2, 5000);
        if (!this.mWakeLock.isHeld()) {
            return;
        }
        if (this.mReleaseWakeLockCountdown > 0) {
            this.mReleaseWakeLockCountdown--;
        } else {
            this.mWakeLock.release();
        }
    }

    public void onBackPressed() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CryptKeeper", "onCreate()");
        String state = SystemProperties.get("vold.decrypt");
        if (isDebugView() || !("".equals(state) || "trigger_restart_framework".equals(state))) {
            try {
                if (getResources().getBoolean(R.bool.crypt_keeper_allow_rotation)) {
                    setRequestedOrientation(-1);
                }
            } catch (NotFoundException e) {
            }
            this.mStatusBar = (StatusBarManager) getSystemService("statusbar");
            this.mStatusBar.disable(53936128);
            setAirplaneModeIfNecessary();
            this.mAudioManager = (AudioManager) getSystemService("audio");
            NonConfigurationInstanceState lastInstance = getLastNonConfigurationInstance();
            if (lastInstance instanceof NonConfigurationInstanceState) {
                this.mWakeLock = lastInstance.wakelock;
                Log.d("CryptKeeper", "Restoring wakelock from NonConfigurationInstanceState");
            }
            return;
        }
        disableCryptKeeperComponent(this);
        finish();
    }

    public void onStart() {
        super.onStart();
        Log.d("CryptKeeper", "onStart()");
        listenPhoneStateBroadcast(this);
        setupUi();
    }

    private void setupUi() {
        if (this.mEncryptionGoneBad || isDebugView("error")) {
            setContentView(R.layout.crypt_keeper_progress);
            showFactoryReset(this.mCorrupt);
            return;
        }
        if (!"".equals(SystemProperties.get("vold.encrypt_progress")) || isDebugView("progress")) {
            setContentView(R.layout.crypt_keeper_progress);
            encryptionProgressInit();
        } else if (this.mValidationComplete || isDebugView("password")) {
            new C00825().execute(new Void[0]);
        } else if (!this.mValidationRequested) {
            new ValidationTask().execute((Void[]) null);
            this.mValidationRequested = true;
        }
    }

    public void onStop() {
        super.onStop();
        removePhoneStateBroadcast(this);
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
    }

    public Object onRetainNonConfigurationInstance() {
        NonConfigurationInstanceState state = new NonConfigurationInstanceState(this.mWakeLock);
        Log.d("CryptKeeper", "Handing wakelock off to NonConfigurationInstanceState");
        this.mWakeLock = null;
        return state;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d("CryptKeeper", "onDestroy()");
        if (this.mWakeLock != null) {
            Log.d("CryptKeeper", "Releasing and destroying wakelock");
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            this.mWakeLock = null;
        }
        if (this.mStatusBar != null) {
            this.mStatusBar.disable(0);
        }
    }

    private void encryptionProgressInit() {
        Log.d("CryptKeeper", "Encryption progress screen initializing.");
        if (this.mWakeLock == null) {
            Log.d("CryptKeeper", "Acquiring wakelock.");
            this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(26, "CryptKeeper");
            this.mWakeLock.acquire();
        }
        ((ProgressBar) findViewById(R.id.progress_bar)).setIndeterminate(true);
        setBackFunctionality(false);
        updateProgress();
    }

    private void showFactoryReset(final boolean corrupt) {
        findViewById(R.id.encroid).setVisibility(8);
        Button button = (Button) findViewById(R.id.factory_reset);
        button.setVisibility(0);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.REASON", "CryptKeeper.showFactoryReset() corrupt=" + corrupt);
                CryptKeeper.this.sendBroadcast(intent);
            }
        });
        if (corrupt) {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_data_corrupt_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_data_corrupt_summary);
        } else {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_failed_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_failed_summary);
        }
        View view = findViewById(R.id.bottom_divider);
        if (view != null) {
            view.setVisibility(0);
        }
    }

    private void updateProgress() {
        String state = SystemProperties.get("vold.encrypt_progress");
        if ("error_partially_encrypted".equals(state)) {
            showFactoryReset(false);
            return;
        }
        CharSequence status = getText(R.string.crypt_keeper_setup_description);
        int percent = 0;
        try {
            percent = isDebugView() ? 50 : Integer.parseInt(state);
        } catch (Exception e) {
            Log.w("CryptKeeper", "Error parsing progress: " + e.toString());
        }
        String progress = Integer.toString(percent);
        Log.v("CryptKeeper", "Encryption progress: " + progress);
        try {
            int time = Integer.parseInt(SystemProperties.get("vold.encrypt_time_remaining"));
            if (time >= 0) {
                progress = DateUtils.formatElapsedTime((long) (((time + 9) / 10) * 10));
                status = getText(R.string.crypt_keeper_setup_time_remaining);
            }
        } catch (Exception e2) {
        }
        TextView tv = (TextView) findViewById(R.id.status);
        if (tv != null) {
            tv.setText(TextUtils.expandTemplate(status, new CharSequence[]{progress}));
        }
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 1000);
    }

    private void cooldown() {
        if (this.mPasswordEntry != null) {
            this.mPasswordEntry.setEnabled(false);
        }
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setEnabled(false);
        }
        ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_force_power_cycle);
    }

    private final void setBackFunctionality(boolean isEnabled) {
        if (isEnabled) {
            this.mStatusBar.disable(53936128);
        } else {
            this.mStatusBar.disable(58130432);
        }
    }

    private void fakeUnlockAttempt(View postingView) {
        beginAttempt();
        postingView.postDelayed(this.mFakeUnlockAttemptRunnable, 1000);
    }

    private void passwordEntryInit() {
        Log.d("CryptKeeper", "passwordEntryInit().");
        this.mPasswordEntry = (EditText) findViewById(R.id.passwordEntry);
        if (this.mPasswordEntry != null) {
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.requestFocus();
            this.mPasswordEntry.setOnKeyListener(this);
            this.mPasswordEntry.setOnTouchListener(this);
            this.mPasswordEntry.addTextChangedListener(this);
        }
        this.mLockPatternView = (LockPatternView) findViewById(R.id.lockPattern);
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setOnPatternListener(this.mChooseNewLockPatternListener);
        }
        if (!getTelephonyManager().isVoiceCapable()) {
            View emergencyCall = findViewById(R.id.emergencyCallButton);
            if (emergencyCall != null) {
                Log.d("CryptKeeper", "Removing the emergency Call button");
                emergencyCall.setVisibility(8);
            }
        }
        View imeSwitcher = findViewById(R.id.switch_ime_button);
        final InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (!(imeSwitcher == null || isPatternLockType() || !hasMultipleEnabledIMEsOrSubtypes(imm, false))) {
            imeSwitcher.setVisibility(0);
            imeSwitcher.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    imm.showInputMethodPicker(false);
                }
            });
        }
        if (this.mWakeLock == null) {
            Log.d("CryptKeeper", "Acquiring wakelock.");
            PowerManager pm = (PowerManager) getSystemService("power");
            if (pm != null) {
                this.mWakeLock = pm.newWakeLock(26, "CryptKeeper");
                this.mWakeLock.acquire();
                this.mReleaseWakeLockCountdown = 96;
            }
        }
        if (this.mLockPatternView == null && !this.mCooldown) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    imm.showSoftInputUnchecked(0, null);
                }
            }, 0);
        }
        updateEmergencyCallButtonState();
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessageDelayed(2, 120000);
        getWindow().addFlags(4718592);
    }

    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager imm, boolean shouldIncludeAuxiliarySubtypes) {
        boolean z = true;
        int filteredImisCount = 0;
        for (InputMethodInfo imi : imm.getEnabledInputMethodList()) {
            if (filteredImisCount > 1) {
                return true;
            }
            List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
            if (subtypes.isEmpty()) {
                filteredImisCount++;
            } else {
                int auxCount = 0;
                for (InputMethodSubtype subtype : subtypes) {
                    if (subtype.isAuxiliary()) {
                        auxCount++;
                    }
                }
                if (subtypes.size() - auxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                    filteredImisCount++;
                }
            }
        }
        if (filteredImisCount <= 1 && imm.getEnabledInputMethodSubtypeList(null, false).size() <= 1) {
            z = false;
        }
        return z;
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return Stub.asInterface(service);
        }
        return null;
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId != 0 && actionId != 6) {
            return false;
        }
        String password = v.getText().toString();
        if (TextUtils.isEmpty(password)) {
            return true;
        }
        v.setText(null);
        this.mPasswordEntry.setEnabled(false);
        setBackFunctionality(false);
        if (password.length() >= 4) {
            new DecryptTask().execute(new String[]{password});
        } else {
            fakeUnlockAttempt(this.mPasswordEntry);
        }
        return true;
    }

    private final void setAirplaneModeIfNecessary() {
        if (!(getTelephonyManager().getLteOnCdmaMode() == 1)) {
            Log.d("CryptKeeper", "Going into airplane mode.");
            Global.putInt(getContentResolver(), "airplane_mode_on", 1);
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.putExtra("state", true);
            sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void updateEmergencyCallButtonState() {
        Button emergencyCall = (Button) findViewById(R.id.emergencyCallButton);
        if (emergencyCall != null) {
            if (isEmergencyCallCapable()) {
                int textId;
                emergencyCall.setVisibility(0);
                emergencyCall.setOnClickListener(new C00869());
                if (getTelecomManager().isInCall()) {
                    textId = R.string.cryptkeeper_return_to_call;
                    Log.d("CryptKeeper", "show cryptkeeper_return_to_call");
                } else {
                    textId = R.string.cryptkeeper_emergency_call;
                    Log.d("CryptKeeper", "show cryptkeeper_emergency_call");
                }
                emergencyCall.setText(textId);
                return;
            }
            emergencyCall.setVisibility(8);
        }
    }

    private boolean isEmergencyCallCapable() {
        return getResources().getBoolean(17956947);
    }

    private void takeEmergencyCallAction() {
        TelecomManager telecomManager = getTelecomManager();
        Log.d("CryptKeeper", "onClick Button telecomManager.isInCall() = " + telecomManager.isInCall());
        if (telecomManager.isInCall()) {
            telecomManager.showInCallScreen(false);
        } else {
            launchEmergencyDialer();
        }
    }

    private void launchEmergencyDialer() {
        Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
        intent.setFlags(276824064);
        setBackFunctionality(true);
        startActivity(intent);
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) getSystemService("phone");
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) getSystemService("telecom");
    }

    private void delayAudioNotification() {
        this.mNotificationCountdown = 20;
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        delayAudioNotification();
        return false;
    }

    public boolean onTouch(View v, MotionEvent event) {
        delayAudioNotification();
        return false;
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        delayAudioNotification();
    }

    public void afterTextChanged(Editable s) {
    }

    private static void disableCryptKeeperComponent(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName name = new ComponentName(context, CryptKeeper.class);
        Log.d("CryptKeeper", "Disabling component " + name);
        pm.setComponentEnabledSetting(name, 2, 1);
    }

    private void listenPhoneStateBroadcast(Activity activity) {
        this.mPhoneStateReceiver = new PhoneStateBroadcastReceiver();
        activity.registerReceiver(this.mPhoneStateReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));
    }

    private void removePhoneStateBroadcast(Activity activity) {
        if (this.mPhoneStateReceiver != null) {
            activity.unregisterReceiver(this.mPhoneStateReceiver);
            this.mPhoneStateReceiver = null;
        }
    }

    private boolean isPatternLockType() {
        try {
            IMountService service = getMountService();
            if (service == null) {
                return false;
            }
            int type = service.getPasswordType();
            return service.getPasswordType() == 2;
        } catch (Exception e) {
            Log.e("CryptKeeper", "Error calling mount service " + e);
            return false;
        }
    }
}
