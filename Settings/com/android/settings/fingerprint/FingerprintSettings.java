package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.android.setupwizardlib.R$styleable;
import java.util.List;

public class FingerprintSettings extends SubSettings {

    public static class FingerprintPreference extends Preference {
        private Fingerprint mFingerprint;
        private View mView;

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr) {
            this(context, attrs, defStyleAttr, 0);
        }

        public FingerprintPreference(Context context, AttributeSet attrs) {
            this(context, attrs, 16842894);
        }

        public FingerprintPreference(Context context) {
            this(context, null);
        }

        public View getView() {
            return this.mView;
        }

        public void setFingerprint(Fingerprint item) {
            this.mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return this.mFingerprint;
        }

        protected void onBindView(View view) {
            super.onBindView(view);
            this.mView = view;
        }
    }

    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnSwitchChangeListener {
        private Preference mAppLockPre;
        private AuthenticationCallback mAuthCallback = new C03791();
        private Preference mChangePassword;
        private CheckBoxPreference mCheckBoxBack;
        private CheckBoxPreference mCheckBoxCamera;
        private CheckBoxPreference mCheckBoxGallery;
        private CheckBoxPreference mCheckBoxLauncher;
        private CheckBoxPreference mCheckBoxMenu;
        private CheckBoxPreference mCheckBoxMusic;
        private CheckBoxPreference mCheckBoxVideo;
        private CancellationSignal mFingerprintCancel;
        private final Runnable mFingerprintLockoutReset = new C03824();
        private FingerprintManager mFingerprintManager;
        private final Handler mHandler = new C03813();
        private Drawable mHighlightDrawable;
        private boolean mInFingerprintLockout;
        private boolean mLaunchedConfirm;
        private RemovalCallback mRemoveCallback = new C03802();
        private SwitchBar mSwitchBar;
        private byte[] mToken;

        class C03791 extends AuthenticationCallback {
            C03791() {
            }

            public void onAuthenticationSucceeded(AuthenticationResult result) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1001, result.getFingerprint().getFingerId(), 0).sendToTarget();
            }

            public void onAuthenticationFailed() {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1002).sendToTarget();
            }

            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1003, errMsgId, 0, errString).sendToTarget();
            }

            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1004, helpMsgId, 0, helpString).sendToTarget();
            }
        }

        class C03802 extends RemovalCallback {
            C03802() {
            }

            public void onRemovalSucceeded(Fingerprint fingerprint) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1000, fingerprint.getFingerId(), 0).sendToTarget();
            }

            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                Activity activity = FingerprintSettingsFragment.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, 0);
                }
            }
        }

        class C03813 extends Handler {
            C03813() {
            }

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1000:
                        FingerprintSettingsFragment.this.removeFingerprintPreference(msg.arg1);
                        FingerprintSettingsFragment.this.updateAddPreference();
                        FingerprintSettingsFragment.this.retryFingerprint();
                        return;
                    case 1001:
                        FingerprintSettingsFragment.this.mFingerprintCancel = null;
                        FingerprintSettingsFragment.this.highlightFingerprintItem(msg.arg1);
                        FingerprintSettingsFragment.this.retryFingerprint();
                        return;
                    case 1003:
                        FingerprintSettingsFragment.this.handleError(msg.arg1, (CharSequence) msg.obj);
                        return;
                    default:
                        return;
                }
            }
        }

        class C03824 implements Runnable {
            C03824() {
            }

            public void run() {
                FingerprintSettingsFragment.this.mInFingerprintLockout = false;
                FingerprintSettingsFragment.this.retryFingerprint();
            }
        }

        public static class ConfirmLastDeleteDialog extends DialogFragment {
            private Fingerprint mFp;

            class C03841 implements OnClickListener {
                C03841() {
                }

                public void onClick(DialogInterface dialog, int which) {
                    ((FingerprintSettingsFragment) ConfirmLastDeleteDialog.this.getTargetFragment()).deleteFingerPrint(ConfirmLastDeleteDialog.this.mFp);
                    dialog.dismiss();
                }
            }

            class C03852 implements OnClickListener {
                C03852() {
                }

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }

            public Dialog onCreateDialog(Bundle savedInstanceState) {
                this.mFp = (Fingerprint) getArguments().getParcelable("fingerprint");
                return new Builder(getActivity()).setTitle(R.string.fingerprint_last_delete_title).setMessage(R.string.fingerprint_last_delete_message).setPositiveButton(R.string.fingerprint_last_delete_confirm, new C03841()).setNegativeButton(R.string.cancel, new C03852()).create();
            }
        }

        public static class RenameDeleteDialog extends DialogFragment {
            private final Context mContext;
            private EditText mDialogTextField;
            private String mFingerName;
            private Fingerprint mFp;
            private Boolean mTextHadFocus;
            private int mTextSelectionEnd;
            private int mTextSelectionStart;

            class C03861 implements OnClickListener {
                C03861() {
                }

                public void onClick(DialogInterface dialog, int which) {
                    String newName = RenameDeleteDialog.this.mDialogTextField.getText().toString();
                    CharSequence name = RenameDeleteDialog.this.mFp.getName();
                    if (!newName.equals(name)) {
                        Log.v("FingerprintSettings", "rename " + name + " to " + newName);
                        MetricsLogger.action(RenameDeleteDialog.this.getContext(), 254, RenameDeleteDialog.this.mFp.getFingerId());
                        ((FingerprintSettingsFragment) RenameDeleteDialog.this.getTargetFragment()).renameFingerPrint(RenameDeleteDialog.this.mFp.getFingerId(), newName);
                    }
                    dialog.dismiss();
                }
            }

            class C03872 implements OnClickListener {
                C03872() {
                }

                public void onClick(DialogInterface dialog, int which) {
                    RenameDeleteDialog.this.onDeleteClick(dialog);
                }
            }

            public RenameDeleteDialog(Context context) {
                this.mContext = context;
            }

            public Dialog onCreateDialog(Bundle savedInstanceState) {
                this.mFp = (Fingerprint) getArguments().getParcelable("fingerprint");
                if (savedInstanceState != null) {
                    this.mFingerName = savedInstanceState.getString("fingerName");
                    this.mTextHadFocus = Boolean.valueOf(savedInstanceState.getBoolean("textHadFocus"));
                    this.mTextSelectionStart = savedInstanceState.getInt("startSelection");
                    this.mTextSelectionEnd = savedInstanceState.getInt("endSelection");
                }
                final AlertDialog alertDialog = new Builder(getActivity()).setView(R.layout.fingerprint_rename_dialog).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new C03861()).setNegativeButton(R.string.security_settings_fingerprint_enroll_dialog_delete, new C03872()).create();
                alertDialog.setOnShowListener(new OnShowListener() {
                    public void onShow(DialogInterface dialog) {
                        RenameDeleteDialog.this.mDialogTextField = (EditText) alertDialog.findViewById(R.id.fingerprint_rename_field);
                        RenameDeleteDialog.this.mDialogTextField.setText(RenameDeleteDialog.this.mFingerName == null ? RenameDeleteDialog.this.mFp.getName() : RenameDeleteDialog.this.mFingerName);
                        if (RenameDeleteDialog.this.mTextHadFocus == null) {
                            RenameDeleteDialog.this.mDialogTextField.selectAll();
                        } else {
                            RenameDeleteDialog.this.mDialogTextField.setSelection(RenameDeleteDialog.this.mTextSelectionStart, RenameDeleteDialog.this.mTextSelectionEnd);
                        }
                    }
                });
                if (this.mTextHadFocus == null || this.mTextHadFocus.booleanValue()) {
                    alertDialog.getWindow().setSoftInputMode(5);
                }
                return alertDialog;
            }

            private void onDeleteClick(DialogInterface dialog) {
                Log.v("FingerprintSettings", "Removing fpId=" + this.mFp.getFingerId());
                MetricsLogger.action(getContext(), 253, this.mFp.getFingerId());
                FingerprintSettingsFragment parent = (FingerprintSettingsFragment) getTargetFragment();
                if (parent.mFingerprintManager.getEnrolledFingerprints().size() > 1) {
                    parent.deleteFingerPrint(this.mFp);
                } else {
                    ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                    Bundle args = new Bundle();
                    args.putParcelable("fingerprint", this.mFp);
                    lastDeleteDialog.setArguments(args);
                    lastDeleteDialog.setTargetFragment(getTargetFragment(), 0);
                    lastDeleteDialog.show(getFragmentManager(), ConfirmLastDeleteDialog.class.getName());
                }
                dialog.dismiss();
            }

            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                if (this.mDialogTextField != null) {
                    outState.putString("fingerName", this.mDialogTextField.getText().toString());
                    outState.putBoolean("textHadFocus", this.mDialogTextField.hasFocus());
                    outState.putInt("startSelection", this.mDialogTextField.getSelectionStart());
                    outState.putInt("endSelection", this.mDialogTextField.getSelectionEnd());
                }
            }
        }

        private void stopFingerprint() {
            if (!(this.mFingerprintCancel == null || this.mFingerprintCancel.isCanceled())) {
                this.mFingerprintCancel.cancel();
            }
            this.mFingerprintCancel = null;
        }

        protected void handleError(int errMsgId, CharSequence msg) {
            this.mFingerprintCancel = null;
            switch (errMsgId) {
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    return;
                case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                    this.mInFingerprintLockout = true;
                    if (!this.mHandler.hasCallbacks(this.mFingerprintLockoutReset)) {
                        this.mHandler.postDelayed(this.mFingerprintLockoutReset, 30000);
                        break;
                    }
                    break;
            }
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, msg, 0);
            }
            retryFingerprint();
        }

        private void retryFingerprint() {
            if (!this.mInFingerprintLockout) {
                this.mFingerprintCancel = new CancellationSignal();
                this.mFingerprintManager.authenticate(null, this.mFingerprintCancel, 0, this.mAuthCallback, null);
            }
        }

        protected int getMetricsCategory() {
            return 49;
        }

        public void onActivityCreated(Bundle savedInstanceState) {
            boolean z = true;
            super.onActivityCreated(savedInstanceState);
            this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
            this.mSwitchBar.show();
            SwitchBar switchBar = this.mSwitchBar;
            if (SystemProperties.getInt("persist.sys.fp_switch", 1) != 1) {
                z = false;
            }
            switchBar.setChecked(z);
            this.mSwitchBar.addOnSwitchChangeListener(this);
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                this.mToken = savedInstanceState.getByteArray("hw_auth_token");
                this.mLaunchedConfirm = savedInstanceState.getBoolean("launched_confirm", false);
            }
            this.mFingerprintManager = (FingerprintManager) getActivity().getSystemService("fingerprint");
            if (this.mToken == null && !this.mLaunchedConfirm) {
                this.mLaunchedConfirm = true;
                launchChooseOrConfirmLock();
            }
        }

        public void onViewCreated(View view, Bundle savedInstanceState) {
            int i;
            super.onViewCreated(view, savedInstanceState);
            TextView v = (TextView) LayoutInflater.from(view.getContext()).inflate(R.layout.fingerprint_settings_footer, null);
            if (isFingerprintDisabled()) {
                i = R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled;
            } else {
                i = R.string.security_settings_fingerprint_enroll_disclaimer;
            }
            v.setText(LearnMoreSpan.linkify(getText(i), getString(getHelpResource())));
            v.setText(removeLearnmore(v.getText().toString()));
            v.setMovementMethod(new LinkMovementMethod());
            getListView().addFooterView(v);
            getListView().setFooterDividersEnabled(false);
        }

        public String removeLearnmore(String name) {
            StringBuilder sb = new StringBuilder();
            if (name.contains(".")) {
                sb.append(name.substring(0, name.indexOf(".") + 1));
                return sb.toString();
            } else if (!name.contains("。")) {
                return name;
            } else {
                sb.append(name.substring(0, name.indexOf("。") + 1));
                return sb.toString();
            }
        }

        private boolean isFingerprintDisabled() {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
            if (dpm == null || (dpm.getKeyguardDisabledFeatures(null) & 32) == 0) {
                return false;
            }
            return true;
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = findPreference(name);
            if (prefToRemove == null) {
                Log.w("FingerprintSettings", "Can't find preference to remove: " + name);
            } else if (!getPreferenceScreen().removePreference(prefToRemove)) {
                Log.w("FingerprintSettings", "Failed to remove preference with key " + name);
            }
        }

        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            addPreferencesFromResource(R.xml.security_settings_fingerprint);
            root = getPreferenceScreen();
            addFingerprintItemPreferences(root);
            addFingerprintControlFun(root);
            return root;
        }

        private void addFingerprintControlFun(PreferenceScreen rootcontol) {
            this.mCheckBoxCamera = new CheckBoxPreference(rootcontol.getContext());
            this.mCheckBoxCamera.setKey("finger_fun_camera");
            this.mCheckBoxCamera.setTitle(R.string.control_camera);
            this.mCheckBoxCamera.setPersistent(false);
            this.mCheckBoxCamera.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mCheckBoxCamera);
            this.mCheckBoxMusic = new CheckBoxPreference(rootcontol.getContext());
            this.mCheckBoxMusic.setKey("finger_fun_music");
            this.mCheckBoxMusic.setTitle(R.string.control_music);
            this.mCheckBoxMusic.setPersistent(false);
            this.mCheckBoxMusic.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mCheckBoxMusic);
            this.mCheckBoxGallery = new CheckBoxPreference(rootcontol.getContext());
            this.mCheckBoxGallery.setKey("finger_fun_gallery");
            this.mCheckBoxGallery.setTitle(R.string.control_gallery);
            this.mCheckBoxGallery.setPersistent(false);
            this.mCheckBoxGallery.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mCheckBoxGallery);
            this.mCheckBoxLauncher = new CheckBoxPreference(rootcontol.getContext());
            this.mCheckBoxLauncher.setKey("finger_fun_launcher");
            this.mCheckBoxLauncher.setTitle(R.string.control_launcher);
            this.mCheckBoxLauncher.setPersistent(false);
            this.mCheckBoxLauncher.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mCheckBoxLauncher);
            this.mCheckBoxVideo = new CheckBoxPreference(rootcontol.getContext());
            this.mCheckBoxVideo.setKey("finger_fun_video");
            this.mCheckBoxVideo.setTitle(R.string.control_video);
            this.mCheckBoxVideo.setPersistent(false);
            this.mCheckBoxVideo.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mCheckBoxVideo);
            this.mCheckBoxBack = new CheckBoxPreference(rootcontol.getContext());
            this.mCheckBoxBack.setKey("finger_fun_back");
            this.mCheckBoxBack.setTitle(R.string.control_back);
            this.mCheckBoxBack.setPersistent(false);
            this.mCheckBoxBack.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mCheckBoxBack);
            this.mCheckBoxMenu = new CheckBoxPreference(rootcontol.getContext());
            this.mCheckBoxMenu.setKey("finger_fun_menu");
            this.mCheckBoxMenu.setTitle(R.string.control_menu);
            this.mCheckBoxMenu.setPersistent(false);
            this.mCheckBoxMenu.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mCheckBoxMenu);
            this.mAppLockPre = new Preference(rootcontol.getContext());
            this.mAppLockPre.setKey("finger_fun_applock");
            this.mAppLockPre.setTitle(R.string.fp_app_lock);
            this.mAppLockPre.setOnPreferenceChangeListener(this);
            this.mAppLockPre.setIcon(R.drawable.ic_lockscreen);
            rootcontol.addPreference(this.mAppLockPre);
            this.mChangePassword = new Preference(rootcontol.getContext());
            this.mChangePassword.setKey("finger_fun_changepass");
            this.mChangePassword.setTitle(R.string.password_change_password);
            this.mChangePassword.setOnPreferenceChangeListener(this);
            rootcontol.addPreference(this.mChangePassword);
            rootcontol.removePreference(this.mCheckBoxCamera);
            rootcontol.removePreference(this.mCheckBoxMusic);
            rootcontol.removePreference(this.mCheckBoxGallery);
            rootcontol.removePreference(this.mCheckBoxLauncher);
            rootcontol.removePreference(this.mCheckBoxVideo);
            updateCheckboxPreference();
        }

        private void updateCheckboxPreference() {
            boolean z;
            boolean z2 = true;
            CheckBoxPreference checkBoxPreference = this.mCheckBoxCamera;
            if (SystemProperties.getInt("persist.sys.fp_camera", 0) == 1) {
                z = true;
            } else {
                z = false;
            }
            checkBoxPreference.setChecked(z);
            checkBoxPreference = this.mCheckBoxGallery;
            if (SystemProperties.getInt("persist.sys.fp_gallery", 0) == 1) {
                z = true;
            } else {
                z = false;
            }
            checkBoxPreference.setChecked(z);
            checkBoxPreference = this.mCheckBoxMusic;
            if (SystemProperties.getInt("persist.sys.fp_music", 0) == 1) {
                z = true;
            } else {
                z = false;
            }
            checkBoxPreference.setChecked(z);
            checkBoxPreference = this.mCheckBoxLauncher;
            if (SystemProperties.getInt("persist.sys.fp_launcher", 0) == 1) {
                z = true;
            } else {
                z = false;
            }
            checkBoxPreference.setChecked(z);
            checkBoxPreference = this.mCheckBoxVideo;
            if (SystemProperties.getInt("persist.sys.fp_video", 0) == 1) {
                z = true;
            } else {
                z = false;
            }
            checkBoxPreference.setChecked(z);
            checkBoxPreference = this.mCheckBoxBack;
            if (SystemProperties.getInt("persist.sys.fp_back", 0) == 1) {
                z = true;
            } else {
                z = false;
            }
            checkBoxPreference.setChecked(z);
            checkBoxPreference = this.mCheckBoxMenu;
            if (SystemProperties.getInt("persist.sys.fp_menu", 0) == 1) {
                z = true;
            } else {
                z = false;
            }
            checkBoxPreference.setChecked(z);
            if (SystemProperties.getInt("persist.sys.fp_switch", 1) != 1) {
                z2 = false;
            }
            setCheckBoxEnabled(z2);
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            root.removeAll();
            List<Fingerprint> items = this.mFingerprintManager.getEnrolledFingerprints();
            int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                Fingerprint item = (Fingerprint) items.get(i);
                FingerprintPreference pref = new FingerprintPreference(root.getContext());
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            Preference addPreference = new Preference(root.getContext());
            addPreference.setKey("key_fingerprint_add");
            addPreference.setTitle(R.string.fingerprint_add_title);
            addPreference.setIcon(R.drawable.ic_add_24dp);
            root.addPreference(addPreference);
            addPreference.setOnPreferenceChangeListener(this);
            updateAddPreference();
        }

        private void updateAddPreference() {
            boolean z = false;
            boolean tooMany = this.mFingerprintManager.getEnrolledFingerprints().size() >= getContext().getResources().getInteger(17694871);
            CharSequence maxSummary = tooMany ? getContext().getString(R.string.fingerprint_add_max, new Object[]{Integer.valueOf(getContext().getResources().getInteger(17694871))}) : "";
            Preference addPreference = findPreference("key_fingerprint_add");
            addPreference.setSummary(maxSummary);
            if (!tooMany) {
                z = true;
            }
            addPreference.setEnabled(z);
        }

        private static String genKey(int id) {
            return "key_fingerprint_item_" + id;
        }

        public void onResume() {
            Intent intent = new Intent("android.intent.action.SETTINGAUTH");
            intent.putExtra("flag", true);
            getActivity().sendBroadcast(intent);
            super.onResume();
            updatePreferences();
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
            retryFingerprint();
        }

        public void onPause() {
            Intent intent = new Intent("android.intent.action.SETTINGAUTH");
            intent.putExtra("flag", false);
            getActivity().sendBroadcast(intent);
            super.onPause();
            stopFingerprint();
        }

        private void setCheckBoxEnabled(boolean isEnabled) {
            this.mCheckBoxGallery.setEnabled(isEnabled);
            this.mCheckBoxCamera.setEnabled(isEnabled);
            this.mCheckBoxMusic.setEnabled(isEnabled);
            this.mCheckBoxLauncher.setEnabled(isEnabled);
            this.mCheckBoxVideo.setEnabled(isEnabled);
            this.mCheckBoxBack.setEnabled(isEnabled);
            this.mCheckBoxMenu.setEnabled(isEnabled);
            this.mAppLockPre.setEnabled(isEnabled);
            this.mChangePassword.setEnabled(isEnabled);
        }

        public void onSwitchChanged(Switch switchView, boolean isChecked) {
            if (isChecked) {
                SystemProperties.set("persist.sys.fp_switch", isChecked ? "1" : "0");
            } else {
                SystemProperties.set("persist.sys.fp_switch", isChecked ? "1" : "0");
            }
            setCheckBoxEnabled(isChecked);
        }

        public void onSaveInstanceState(Bundle outState) {
            outState.putByteArray("hw_auth_token", this.mToken);
            outState.putBoolean("launched_confirm", this.mLaunchedConfirm);
        }

        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
            String key = pref.getKey();
            Intent intent;
            if ("key_fingerprint_add".equals(key)) {
                intent = new Intent();
                intent.setClassName("com.android.settings", FingerprintEnrollEnrolling.class.getName());
                intent.putExtra("hw_auth_token", this.mToken);
                startActivityForResult(intent, 10);
            } else if (pref instanceof FingerprintPreference) {
                showRenameDeleteDialog(((FingerprintPreference) pref).getFingerprint());
                return super.onPreferenceTreeClick(preferenceScreen, pref);
            } else if ("finger_fun_applock".equals(key)) {
                intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.applock.PasswordActivity");
                startActivity(intent);
            } else if ("finger_fun_changepass".equals(key)) {
                intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.applock.PasswordActivity");
                intent.putExtra("start_type", "change.password");
                startActivity(intent);
            }
            return true;
        }

        private void showRenameDeleteDialog(Fingerprint fp) {
            RenameDeleteDialog renameDeleteDialog = new RenameDeleteDialog(getContext());
            Bundle args = new Bundle();
            args.putParcelable("fingerprint", fp);
            renameDeleteDialog.setArguments(args);
            renameDeleteDialog.setTargetFragment(this, 0);
            renameDeleteDialog.show(getFragmentManager(), RenameDeleteDialog.class.getName());
        }

        public boolean onPreferenceChange(Preference preference, Object value) {
            String key = preference.getKey();
            boolean isChecked = String.valueOf(value).contains("true");
            if ("finger_fun_camera".equals(key)) {
                String str;
                String str2 = "persist.sys.fp_camera";
                if (isChecked) {
                    str = "1";
                } else {
                    str = "0";
                }
                SystemProperties.set(str2, str);
            } else if ("finger_fun_gallery".equals(key)) {
                SystemProperties.set("persist.sys.fp_gallery", isChecked ? "1" : "0");
            } else if ("finger_fun_music".equals(key)) {
                SystemProperties.set("persist.sys.fp_music", isChecked ? "1" : "0");
            } else if ("finger_fun_video".equals(key)) {
                SystemProperties.set("persist.sys.fp_video", isChecked ? "1" : "0");
            } else if ("finger_fun_launcher".equals(key)) {
                SystemProperties.set("persist.sys.fp_launcher", isChecked ? "1" : "0");
            } else if ("finger_fun_back".equals(key)) {
                SystemProperties.set("persist.sys.fp_back", isChecked ? "1" : "0");
            } else if ("finger_fun_menu".equals(key)) {
                SystemProperties.set("persist.sys.fp_menu", isChecked ? "1" : "0");
            }
            if (!"fingerprint_enable_keyguard_toggle".equals(key)) {
                Log.v("FingerprintSettings", "Unknown key:" + key);
            }
            return true;
        }

        protected int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 102 || requestCode == 101) {
                if ((resultCode == 1 || resultCode == -1) && data != null) {
                    this.mToken = data.getByteArrayExtra("hw_auth_token");
                }
            } else if (requestCode == 10 && resultCode == 3) {
                Activity activity = getActivity();
                activity.setResult(3);
                activity.finish();
            }
            if (this.mToken == null) {
                getActivity().finish();
            }
        }

        public void onDestroy() {
            this.mSwitchBar.hide();
            super.onDestroy();
            if (getActivity().isFinishing()) {
                int result = this.mFingerprintManager.postEnroll();
                if (result < 0) {
                    Log.w("FingerprintSettings", "postEnroll failed: result = " + result);
                }
            }
        }

        private Drawable getHighlightDrawable() {
            if (this.mHighlightDrawable == null) {
                Activity activity = getActivity();
                if (activity != null) {
                    this.mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
                }
            }
            return this.mHighlightDrawable;
        }

        private void highlightFingerprintItem(int fpId) {
            FingerprintPreference fpref = (FingerprintPreference) findPreference(genKey(fpId));
            Drawable highlight = getHighlightDrawable();
            if (highlight != null) {
                final View view = fpref.getView();
                highlight.setHotspot((float) (view.getWidth() / 2), (float) (view.getHeight() / 2));
                view.setBackground(highlight);
                view.setPressed(true);
                view.setPressed(false);
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        view.setBackground(null);
                    }
                }, 500);
            }
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            long challenge = this.mFingerprintManager.preEnroll();
            if (!new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(101, getString(R.string.security_settings_fingerprint_preference_title), null, null, challenge)) {
                intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
                intent.putExtra("minimum_quality", 65536);
                intent.putExtra("hide_disabled_prefs", true);
                intent.putExtra("has_challenge", true);
                intent.putExtra("challenge", challenge);
                startActivityForResult(intent, 102);
            }
        }

        private void deleteFingerPrint(Fingerprint fingerPrint) {
            this.mFingerprintManager.remove(fingerPrint, this.mRemoveCallback);
        }

        private void renameFingerPrint(int fingerId, String newName) {
            this.mFingerprintManager.rename(fingerId, newName);
            updatePreferences();
        }
    }

    private static class LearnMoreSpan extends URLSpan {
        private static final Typeface TYPEFACE_MEDIUM = Typeface.create("sans-serif-medium", 0);

        private LearnMoreSpan(String url) {
            super(url);
        }

        public void onClick(View widget) {
            Context ctx = widget.getContext();
            Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
            if (intent != null) {
                try {
                    ((Activity) ctx).startActivityForResult(intent, 0);
                } catch (ActivityNotFoundException e) {
                    Log.w("FingerprintSettings", "Actvity was not found for intent, " + intent.toString());
                }
            }
        }

        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri) {
            int i = 0;
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = (Annotation[]) msg.getSpans(0, msg.length(), Annotation.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            int length = spans.length;
            while (i < length) {
                Annotation annotation = spans[i];
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                LearnMoreSpan link = new LearnMoreSpan(uri);
                builder.setSpan(link, start, end, msg.getSpanFlags(link));
                i++;
            }
            return builder;
        }
    }

    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintSettingsFragment.class.getName().equals(fragmentName)) {
            return true;
        }
        return false;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.security_settings_fingerprint_preference_title));
    }
}
