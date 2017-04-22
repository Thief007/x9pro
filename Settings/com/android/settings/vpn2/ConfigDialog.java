package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.internal.net.VpnProfile;
import com.android.settings.R;
import com.android.setupwizardlib.R$styleable;
import java.net.InetAddress;

class ConfigDialog extends AlertDialog implements TextWatcher, OnClickListener, OnItemSelectedListener {
    private TextView mDnsServers;
    private boolean mEditing;
    private boolean mExists;
    private Spinner mIpsecCaCert;
    private TextView mIpsecIdentifier;
    private TextView mIpsecSecret;
    private Spinner mIpsecServerCert;
    private Spinner mIpsecUserCert;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private TextView mL2tpSecret;
    private final DialogInterface.OnClickListener mListener;
    private CheckBox mMppe;
    private TextView mName;
    private TextView mPassword;
    private final VpnProfile mProfile;
    private TextView mRoutes;
    private CheckBox mSaveLogin;
    private TextView mSearchDomains;
    private TextView mServer;
    private CheckBox mShowOptions;
    private Spinner mType;
    private TextView mUsername;
    private View mView;

    ConfigDialog(Context context, DialogInterface.OnClickListener listener, VpnProfile profile, boolean editing, boolean exists) {
        super(context);
        this.mListener = listener;
        this.mProfile = profile;
        this.mEditing = editing;
        this.mExists = exists;
    }

    protected void onCreate(Bundle savedState) {
        boolean z;
        this.mView = getLayoutInflater().inflate(R.layout.vpn_dialog, null);
        setView(this.mView);
        Context context = getContext();
        this.mName = (TextView) this.mView.findViewById(R.id.name);
        this.mType = (Spinner) this.mView.findViewById(R.id.type);
        this.mServer = (TextView) this.mView.findViewById(R.id.server);
        this.mUsername = (TextView) this.mView.findViewById(R.id.username);
        this.mPassword = (TextView) this.mView.findViewById(R.id.password);
        this.mSearchDomains = (TextView) this.mView.findViewById(R.id.search_domains);
        this.mDnsServers = (TextView) this.mView.findViewById(R.id.dns_servers);
        this.mRoutes = (TextView) this.mView.findViewById(R.id.routes);
        this.mMppe = (CheckBox) this.mView.findViewById(R.id.mppe);
        this.mL2tpSecret = (TextView) this.mView.findViewById(R.id.l2tp_secret);
        this.mIpsecIdentifier = (TextView) this.mView.findViewById(R.id.ipsec_identifier);
        this.mIpsecSecret = (TextView) this.mView.findViewById(R.id.ipsec_secret);
        this.mIpsecUserCert = (Spinner) this.mView.findViewById(R.id.ipsec_user_cert);
        this.mIpsecCaCert = (Spinner) this.mView.findViewById(R.id.ipsec_ca_cert);
        this.mIpsecServerCert = (Spinner) this.mView.findViewById(R.id.ipsec_server_cert);
        this.mSaveLogin = (CheckBox) this.mView.findViewById(R.id.save_login);
        this.mShowOptions = (CheckBox) this.mView.findViewById(R.id.show_options);
        this.mName.setText(this.mProfile.name);
        this.mType.setSelection(this.mProfile.type);
        this.mServer.setText(this.mProfile.server);
        if (this.mProfile.saveLogin) {
            this.mUsername.setText(this.mProfile.username);
            this.mPassword.setText(this.mProfile.password);
        }
        this.mSearchDomains.setText(this.mProfile.searchDomains);
        this.mDnsServers.setText(this.mProfile.dnsServers);
        this.mRoutes.setText(this.mProfile.routes);
        this.mMppe.setChecked(this.mProfile.mppe);
        this.mL2tpSecret.setText(this.mProfile.l2tpSecret);
        this.mIpsecIdentifier.setText(this.mProfile.ipsecIdentifier);
        this.mIpsecSecret.setText(this.mProfile.ipsecSecret);
        loadCertificates(this.mIpsecUserCert, "USRPKEY_", 0, this.mProfile.ipsecUserCert);
        loadCertificates(this.mIpsecCaCert, "CACERT_", R.string.vpn_no_ca_cert, this.mProfile.ipsecCaCert);
        loadCertificates(this.mIpsecServerCert, "USRCERT_", R.string.vpn_no_server_cert, this.mProfile.ipsecServerCert);
        this.mSaveLogin.setChecked(this.mProfile.saveLogin);
        this.mName.addTextChangedListener(this);
        this.mType.setOnItemSelectedListener(this);
        this.mServer.addTextChangedListener(this);
        this.mUsername.addTextChangedListener(this);
        this.mPassword.addTextChangedListener(this);
        this.mDnsServers.addTextChangedListener(this);
        this.mRoutes.addTextChangedListener(this);
        this.mIpsecSecret.addTextChangedListener(this);
        this.mIpsecUserCert.setOnItemSelectedListener(this);
        this.mShowOptions.setOnClickListener(this);
        boolean valid = validate(true);
        if (this.mEditing || !valid) {
            z = true;
        } else {
            z = false;
        }
        this.mEditing = z;
        if (this.mEditing) {
            setTitle(R.string.vpn_edit);
            this.mView.findViewById(R.id.editor).setVisibility(0);
            changeType(this.mProfile.type);
            if (!(this.mProfile.searchDomains.isEmpty() && this.mProfile.dnsServers.isEmpty() && this.mProfile.routes.isEmpty())) {
                showAdvancedOptions();
            }
            if (this.mExists) {
                setButton(-3, context.getString(R.string.vpn_forget), this.mListener);
            }
            setButton(-1, context.getString(R.string.vpn_save), this.mListener);
        } else {
            setTitle(context.getString(R.string.vpn_connect_to, new Object[]{this.mProfile.name}));
            this.mView.findViewById(R.id.login).setVisibility(0);
            setButton(-1, context.getString(R.string.vpn_connect), this.mListener);
        }
        setButton(-2, context.getString(R.string.vpn_cancel), this.mListener);
        super.onCreate(savedState);
        Button button = getButton(-1);
        if (!this.mEditing) {
            valid = validate(false);
        }
        button.setEnabled(valid);
        getWindow().setSoftInputMode(20);
    }

    public void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        if (this.mShowOptions.isChecked()) {
            showAdvancedOptions();
        }
    }

    public void afterTextChanged(Editable field) {
        getButton(-1).setEnabled(validate(this.mEditing));
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void onClick(View view) {
        if (view == this.mShowOptions) {
            showAdvancedOptions();
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == this.mType) {
            changeType(position);
        }
        getButton(-1).setEnabled(validate(this.mEditing));
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void showAdvancedOptions() {
        this.mView.findViewById(R.id.options).setVisibility(0);
        this.mShowOptions.setVisibility(8);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void changeType(int type) {
        this.mMppe.setVisibility(8);
        this.mView.findViewById(R.id.l2tp).setVisibility(8);
        this.mView.findViewById(R.id.ipsec_psk).setVisibility(8);
        this.mView.findViewById(R.id.ipsec_user).setVisibility(8);
        this.mView.findViewById(R.id.ipsec_peer).setVisibility(8);
        switch (type) {
            case 0:
                this.mMppe.setVisibility(0);
                return;
            case 1:
                this.mView.findViewById(R.id.l2tp).setVisibility(0);
                break;
            case 2:
                this.mView.findViewById(R.id.l2tp).setVisibility(0);
                break;
            case 3:
                break;
            case 4:
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                break;
            default:
                return;
        }
    }

    private boolean validate(boolean editing) {
        boolean z = true;
        if (!editing) {
            if (this.mUsername.getText().length() == 0 || this.mPassword.getText().length() == 0) {
                z = false;
            }
            return z;
        } else if (this.mName.getText().length() == 0 || this.mServer.getText().length() == 0 || !validateAddresses(this.mDnsServers.getText().toString(), false) || !validateAddresses(this.mRoutes.getText().toString(), true)) {
            return false;
        } else {
            switch (this.mType.getSelectedItemPosition()) {
                case 0:
                case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                    return true;
                case 1:
                case 3:
                    if (this.mIpsecSecret.getText().length() == 0) {
                        z = false;
                    }
                    return z;
                case 2:
                case 4:
                    if (this.mIpsecUserCert.getSelectedItemPosition() == 0) {
                        z = false;
                    }
                    return z;
                default:
                    return false;
            }
        }
    }

    private boolean validateAddresses(String addresses, boolean cidr) {
        try {
            for (String address : addresses.split(" ")) {
                String address2;
                if (!address2.isEmpty()) {
                    int prefixLength = 32;
                    if (cidr) {
                        String[] parts = address2.split("/", 2);
                        address2 = parts[0];
                        prefixLength = Integer.parseInt(parts[1]);
                    }
                    byte[] bytes = InetAddress.parseNumericAddress(address2).getAddress();
                    int integer = (((bytes[3] & 255) | ((bytes[2] & 255) << 8)) | ((bytes[1] & 255) << 16)) | ((bytes[0] & 255) << 24);
                    if (bytes.length == 4 && prefixLength >= 0 && prefixLength <= 32) {
                        if (prefixLength < 32 && (integer << prefixLength) != 0) {
                        }
                    }
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadCertificates(Spinner spinner, String prefix, int firstId, String selected) {
        Context context = getContext();
        String first = firstId == 0 ? "" : context.getString(firstId);
        String[] certificates = this.mKeyStore.list(prefix);
        if (certificates == null || certificates.length == 0) {
            certificates = new String[]{first};
        } else {
            String[] array = new String[(certificates.length + 1)];
            array[0] = first;
            System.arraycopy(certificates, 0, array, 1, certificates.length);
            certificates = array;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter(context, 17367048, certificates);
        adapter.setDropDownViewResource(17367049);
        spinner.setAdapter(adapter);
        for (int i = 1; i < certificates.length; i++) {
            if (certificates[i].equals(selected)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    boolean isEditing() {
        return this.mEditing;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    VpnProfile getProfile() {
        VpnProfile profile = new VpnProfile(this.mProfile.key);
        profile.name = this.mName.getText().toString();
        profile.type = this.mType.getSelectedItemPosition();
        profile.server = this.mServer.getText().toString().trim();
        profile.username = this.mUsername.getText().toString();
        profile.password = this.mPassword.getText().toString();
        profile.searchDomains = this.mSearchDomains.getText().toString().trim();
        profile.dnsServers = this.mDnsServers.getText().toString().trim();
        profile.routes = this.mRoutes.getText().toString().trim();
        switch (profile.type) {
            case 0:
                profile.mppe = this.mMppe.isChecked();
                break;
            case 1:
                profile.l2tpSecret = this.mL2tpSecret.getText().toString();
                break;
            case 2:
                profile.l2tpSecret = this.mL2tpSecret.getText().toString();
                break;
            case 3:
                break;
            case 4:
                break;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                break;
        }
    }
}
