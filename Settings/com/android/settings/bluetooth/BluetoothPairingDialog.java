package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController.AlertParams;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.setupwizardlib.R$styleable;
import java.util.Locale;
import java.util.regex.Pattern;

public final class BluetoothPairingDialog extends AlertActivity implements OnCheckedChangeListener, OnClickListener, TextWatcher {
    private static boolean sReceiverRegistered = false;
    private LocalBluetoothManager mBluetoothManager;
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    private BluetoothDevice mDevice;
    private Button mOkButton;
    private String mPairingKey;
    private EditText mPairingView;
    private final BroadcastReceiver mReceiver = new C02991();
    private int mType;

    class C02991 extends BroadcastReceiver {
        C02991() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.BOND_STATE_CHANGED".equals(action)) {
                int bondState = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
                if (bondState == 12 || bondState == 10) {
                    BluetoothPairingDialog.this.dismiss();
                }
            } else if ("android.bluetooth.device.action.PAIRING_CANCEL".equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (device == null || device.equals(BluetoothPairingDialog.this.mDevice)) {
                    BluetoothPairingDialog.this.dismiss();
                }
            }
        }
    }

    class C03002 implements OnCheckedChangeListener {
        C03002() {
        }

        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked) {
                BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(1);
            } else {
                BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(2);
            }
        }
    }

    class C03013 implements OnCheckedChangeListener {
        C03013() {
        }

        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked) {
                BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(1);
            } else {
                BluetoothPairingDialog.this.mDevice.setPhonebookAccessPermission(2);
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            this.mBluetoothManager = Utils.getLocalBtManager(this);
            if (this.mBluetoothManager == null) {
                Log.e("BluetoothPairingDialog", "Error: BluetoothAdapter not supported by system");
                finish();
                return;
            }
            this.mCachedDeviceManager = this.mBluetoothManager.getCachedDeviceManager();
            this.mDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            this.mType = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT", Integer.MIN_VALUE);
            switch (this.mType) {
                case 0:
                case 1:
                case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                    createUserEntryDialog();
                    break;
                case 2:
                    if (intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
                        this.mPairingKey = String.format(Locale.US, "%06d", new Object[]{Integer.valueOf(intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE))});
                        createConfirmationDialog();
                        break;
                    }
                    Log.e("BluetoothPairingDialog", "Invalid Confirmation Passkey received, not showing any dialog");
                    return;
                case 3:
                case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                    createConsentDialog();
                    break;
                case 4:
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    if (intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
                        if (this.mType == 4) {
                            this.mPairingKey = String.format(Locale.US, "%06d", new Object[]{Integer.valueOf(pairingKey)});
                        } else {
                            this.mPairingKey = String.format(Locale.US, "%04d", new Object[]{Integer.valueOf(pairingKey)});
                        }
                        createDisplayPasskeyOrPinDialog();
                        break;
                    }
                    Log.e("BluetoothPairingDialog", "Invalid Confirmation Passkey or PIN received, not showing any dialog");
                    return;
                default:
                    Log.e("BluetoothPairingDialog", "Incorrect pairing type received, not showing any dialog");
                    break;
            }
            registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.PAIRING_CANCEL"));
            registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED"));
            sReceiverRegistered = true;
            return;
        }
        Log.e("BluetoothPairingDialog", "Error: this activity may be started only with intent android.bluetooth.device.action.PAIRING_REQUEST");
        finish();
    }

    private void createUserEntryDialog() {
        AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createPinEntryView();
        p.mPositiveButtonText = getString(17039370);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(17039360);
        p.mNegativeButtonListener = this;
        setupAlert();
        this.mOkButton = this.mAlert.getButton(-1);
        this.mOkButton.setEnabled(false);
    }

    private View createPinEntryView() {
        int messageId;
        int maxLength;
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_entry, null);
        TextView messageViewCaptionHint = (TextView) view.findViewById(R.id.pin_values_hint);
        TextView messageView2 = (TextView) view.findViewById(R.id.message_below_pin);
        CheckBox alphanumericPin = (CheckBox) view.findViewById(R.id.alphanumeric_pin);
        CheckBox contactSharing = (CheckBox) view.findViewById(R.id.phonebook_sharing_message_entry_pin);
        contactSharing.setText(getString(R.string.bluetooth_pairing_shares_phonebook, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)}));
        if (this.mDevice.getPhonebookAccessPermission() == 1) {
            contactSharing.setChecked(true);
        } else if (this.mDevice.getPhonebookAccessPermission() == 2) {
            contactSharing.setChecked(false);
        } else if (this.mDevice.getBluetoothClass().getDeviceClass() == 1032) {
            contactSharing.setChecked(true);
            this.mDevice.setPhonebookAccessPermission(1);
        } else {
            contactSharing.setChecked(false);
            this.mDevice.setPhonebookAccessPermission(2);
        }
        contactSharing.setOnCheckedChangeListener(new C03002());
        this.mPairingView = (EditText) view.findViewById(R.id.text);
        this.mPairingView.addTextChangedListener(this);
        alphanumericPin.setOnCheckedChangeListener(this);
        int messageIdHint = R.string.bluetooth_pin_values_hint;
        switch (this.mType) {
            case 0:
                break;
            case 1:
                messageId = R.string.bluetooth_enter_passkey_other_device;
                maxLength = 6;
                alphanumericPin.setVisibility(8);
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                messageIdHint = R.string.bluetooth_pin_values_hint_16_digits;
                break;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type for createPinEntryView: " + this.mType);
                return null;
        }
        messageId = R.string.bluetooth_enter_pin_other_device;
        maxLength = 16;
        messageViewCaptionHint.setText(messageIdHint);
        messageView2.setText(messageId);
        this.mPairingView.setInputType(2);
        this.mPairingView.setFilters(new InputFilter[]{new LengthFilter(maxLength)});
        return view;
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.bluetooth_pin_confirm, null);
        TextView pairingViewCaption = (TextView) view.findViewById(R.id.pairing_caption);
        TextView pairingViewContent = (TextView) view.findViewById(R.id.pairing_subhead);
        TextView messagePairing = (TextView) view.findViewById(R.id.pairing_code_message);
        CheckBox contactSharing = (CheckBox) view.findViewById(R.id.phonebook_sharing_message_confirm_pin);
        contactSharing.setText(getString(R.string.bluetooth_pairing_shares_phonebook, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)}));
        if (this.mDevice.getPhonebookAccessPermission() == 1) {
            contactSharing.setChecked(true);
        } else if (this.mDevice.getPhonebookAccessPermission() == 2) {
            contactSharing.setChecked(false);
        } else if (this.mDevice.getBluetoothClass().getDeviceClass() == 1032) {
            contactSharing.setChecked(true);
            this.mDevice.setPhonebookAccessPermission(1);
        } else {
            contactSharing.setChecked(false);
            this.mDevice.setPhonebookAccessPermission(2);
        }
        contactSharing.setOnCheckedChangeListener(new C03013());
        CharSequence charSequence = null;
        switch (this.mType) {
            case 2:
                break;
            case 3:
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                messagePairing.setVisibility(0);
                break;
            case 4:
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                messagePairing.setVisibility(0);
                break;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received, not creating view");
                return null;
        }
        charSequence = this.mPairingKey;
        if (charSequence != null) {
            pairingViewCaption.setVisibility(0);
            pairingViewContent.setVisibility(0);
            pairingViewContent.setText(charSequence);
        }
        return view;
    }

    private void createConfirmationDialog() {
        AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createConsentDialog() {
        AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createView();
        p.mPositiveButtonText = getString(R.string.bluetooth_pairing_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.bluetooth_pairing_decline);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private void createDisplayPasskeyOrPinDialog() {
        AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.bluetooth_pairing_request, new Object[]{this.mCachedDeviceManager.getName(this.mDevice)});
        p.mView = createView();
        p.mNegativeButtonText = getString(17039360);
        p.mNegativeButtonListener = this;
        setupAlert();
        if (this.mType == 4) {
            this.mDevice.setPairingConfirmation(true);
        } else if (this.mType == 5) {
            this.mDevice.setPin(BluetoothDevice.convertPinToBytes(this.mPairingKey));
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (sReceiverRegistered) {
            unregisterReceiver(this.mReceiver);
            sReceiverRegistered = false;
        }
    }

    public void afterTextChanged(Editable s) {
        boolean z = true;
        if (this.mOkButton == null) {
            return;
        }
        if (this.mType == 7) {
            Button button = this.mOkButton;
            if (s.length() < 16) {
                z = false;
            }
            button.setEnabled(z);
            return;
        }
        button = this.mOkButton;
        if (s.length() <= 0) {
            z = false;
        }
        button.setEnabled(z);
    }

    private void onPair(String value) {
        switch (this.mType) {
            case 0:
            case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                byte[] pinBytes = BluetoothDevice.convertPinToBytes(value);
                if (pinBytes != null) {
                    this.mDevice.setPin(pinBytes);
                    break;
                }
                return;
            case 1:
                this.mDevice.setPasskey(Integer.parseInt(value));
                break;
            case 2:
            case 3:
                this.mDevice.setPairingConfirmation(true);
                break;
            case 4:
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                this.mDevice.setRemoteOutOfBandData();
                break;
            default:
                Log.e("BluetoothPairingDialog", "Incorrect pairing type received");
                break;
        }
    }

    private void onCancel() {
        this.mDevice.cancelPairingUserInput();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            onCancel();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -1:
                if (this.mPairingView != null) {
                    onPair(this.mPairingView.getText().toString());
                    return;
                } else {
                    onPair(null);
                    return;
                }
            default:
                onCancel();
                return;
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (this.mPairingView.getInputType() != 2) {
            String pinCodeStr = s.toString();
            Log.d("BluetoothPairingDialog", "onTextChanged " + pinCodeStr);
            String str = stringFilter(pinCodeStr);
            if (!pinCodeStr.equals(str)) {
                this.mPairingView.setText(str);
                this.mPairingView.setSelection(str.length());
            }
        }
    }

    private String stringFilter(String text) {
        return Pattern.compile("[^\\x20-\\x7e]").matcher(text).replaceAll("").trim();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            this.mPairingView.setInputType(1);
        } else {
            this.mPairingView.setInputType(2);
        }
    }

    public void onBackPressed() {
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            Log.e("BluetoothPairingDialog", e.getMessage());
        }
    }
}
