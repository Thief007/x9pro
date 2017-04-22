package com.android.settings.wifi;

import android.content.Context;
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.security.KeyStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.settings.ProxySelector;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.wifi.Utf8ByteLengthFilter;
import com.mediatek.wifi.WifiConfigControllerExt;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

public class WifiConfigController implements TextWatcher, OnItemSelectedListener, OnCheckedChangeListener {
    private final ArrayAdapter<String> PHASE2_FULL_ADAPTER;
    private final ArrayAdapter<String> PHASE2_PEAP_ADAPTER;
    private final AccessPoint mAccessPoint;
    private int mAccessPointSecurity;
    private final WifiConfigUiBase mConfigUi;
    private Context mContext;
    private TextView mDns1View;
    private TextView mDns2View;
    private TextView mEapAnonymousView;
    private Spinner mEapCaCertSpinner;
    private TextView mEapIdentityView;
    private Spinner mEapMethodSpinner;
    private Spinner mEapUserCertSpinner;
    private boolean mEdit;
    private TextView mGatewayView;
    private ProxyInfo mHttpProxy = null;
    private TextView mIpAddressView;
    private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
    private Spinner mIpSettingsSpinner;
    private String[] mLevels;
    private boolean mModify;
    private TextView mNetworkPrefixLengthView;
    private TextView mPasswordView;
    private ArrayAdapter<String> mPhase2Adapter;
    private Spinner mPhase2Spinner;
    private TextView mProxyExclusionListView;
    private TextView mProxyHostView;
    private TextView mProxyPacView;
    private TextView mProxyPortView;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private Spinner mProxySettingsSpinner;
    private Spinner mSecuritySpinner;
    private TextView mSsidView;
    private StaticIpConfiguration mStaticIpConfiguration = null;
    private final Handler mTextViewChangedHandler;
    private final View mView;
    private WifiConfigControllerExt mWifiConfigControllerExt;
    private String unspecifiedCert = "unspecified";

    class C06061 implements Runnable {
        C06061() {
        }

        public void run() {
            WifiConfigController.this.enableSubmitIfAppropriate();
        }
    }

    public WifiConfigController(WifiConfigUiBase parent, View view, AccessPoint accessPoint, boolean edit, boolean modify) {
        int i;
        this.mConfigUi = parent;
        this.mView = view;
        this.mAccessPoint = accessPoint;
        if (accessPoint == null) {
            i = 0;
        } else {
            i = accessPoint.getSecurity();
        }
        this.mAccessPointSecurity = i;
        this.mEdit = edit;
        this.mModify = modify;
        this.mTextViewChangedHandler = new Handler();
        this.mContext = this.mConfigUi.getContext();
        Resources res = this.mContext.getResources();
        this.mWifiConfigControllerExt = new WifiConfigControllerExt(this, this.mConfigUi, this.mView);
        this.mLevels = res.getStringArray(R.array.wifi_signal);
        this.PHASE2_PEAP_ADAPTER = new ArrayAdapter(this.mContext, 17367048, res.getStringArray(R.array.wifi_peap_phase2_entries));
        this.PHASE2_PEAP_ADAPTER.setDropDownViewResource(17367049);
        this.PHASE2_FULL_ADAPTER = new ArrayAdapter(this.mContext, 17367048, res.getStringArray(R.array.wifi_phase2_entries));
        this.PHASE2_FULL_ADAPTER.setDropDownViewResource(17367049);
        this.unspecifiedCert = this.mContext.getString(R.string.wifi_unspecified);
        this.mIpSettingsSpinner = (Spinner) this.mView.findViewById(R.id.ip_settings);
        this.mIpSettingsSpinner.setOnItemSelectedListener(this);
        this.mProxySettingsSpinner = (Spinner) this.mView.findViewById(R.id.proxy_settings);
        this.mProxySettingsSpinner.setOnItemSelectedListener(this);
        if (this.mAccessPoint == null) {
            this.mConfigUi.setTitle((int) R.string.wifi_add_network);
            this.mSsidView = (TextView) this.mView.findViewById(R.id.ssid);
            this.mSsidView.addTextChangedListener(this);
            this.mSsidView.setFilters(new InputFilter[]{new Utf8ByteLengthFilter(32)});
            this.mSecuritySpinner = (Spinner) this.mView.findViewById(R.id.security);
            this.mSecuritySpinner.setOnItemSelectedListener(this);
            this.mView.findViewById(R.id.type).setVisibility(0);
            showIpConfigFields();
            showProxyFields();
            this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
            ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnCheckedChangeListener(this);
            this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            if (this.mPasswordView == null) {
                this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
                this.mPasswordView.addTextChangedListener(this);
                ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnCheckedChangeListener(this);
            }
            this.mWifiConfigControllerExt.setHexCheckBoxListener();
        } else {
            WifiConfiguration config;
            this.mConfigUi.setTitle(this.mAccessPoint.getSsid());
            ViewGroup group = (ViewGroup) this.mView.findViewById(R.id.info);
            this.mWifiConfigControllerExt.addViews(this.mConfigUi, this.mAccessPoint.getSecurityString(false));
            boolean showAdvancedFields = false;
            if (this.mAccessPoint.isSaved()) {
                config = this.mAccessPoint.getConfig();
                if (config.getIpAssignment() == IpAssignment.STATIC) {
                    this.mIpSettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                    StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                    if (!(staticConfig == null || staticConfig.ipAddress == null)) {
                        addRow(group, R.string.wifi_ip_address, staticConfig.ipAddress.getAddress().getHostAddress());
                    }
                } else {
                    this.mIpSettingsSpinner.setSelection(0);
                }
                if (config.getProxySettings() == ProxySettings.STATIC) {
                    this.mProxySettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                } else {
                    if (config.getProxySettings() == ProxySettings.PAC) {
                        this.mProxySettingsSpinner.setSelection(2);
                        showAdvancedFields = true;
                    } else {
                        this.mProxySettingsSpinner.setSelection(0);
                    }
                }
                if (config != null && config.isPasspoint()) {
                    addRow(group, R.string.passpoint_label, String.format(this.mContext.getString(R.string.passpoint_content), new Object[]{config.providerFriendlyName}));
                }
            }
            if (!(this.mAccessPoint.isSaved() || this.mAccessPoint.isActive()) || this.mEdit) {
                showSecurityFields();
                showIpConfigFields();
                showProxyFields();
                this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
                ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnCheckedChangeListener(this);
                if (showAdvancedFields) {
                    ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setChecked(true);
                    this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(0);
                }
            }
            if (this.mModify) {
                this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else {
                DetailedState state = this.mAccessPoint.getDetailedState();
                String signalLevel = getSignalString();
                if (state != null || signalLevel == null) {
                    if (state != null) {
                        boolean isEphemeral = this.mAccessPoint.isEphemeral();
                        config = this.mAccessPoint.getConfig();
                        String providerFriendlyName = null;
                        if (config != null && config.isPasspoint()) {
                            providerFriendlyName = config.providerFriendlyName;
                        }
                        addRow(group, R.string.wifi_status, AccessPoint.getSummary(this.mConfigUi.getContext(), state, isEphemeral, providerFriendlyName));
                    }
                    if (signalLevel != null) {
                        addRow(group, R.string.wifi_signal, signalLevel);
                    }
                    WifiInfo info = this.mAccessPoint.getInfo();
                    if (!(info == null || info.getLinkSpeed() == -1)) {
                        addRow(group, R.string.wifi_speed, String.format(res.getString(R.string.link_speed), new Object[]{Integer.valueOf(info.getLinkSpeed())}));
                    }
                    if (!(info == null || info.getFrequency() == -1)) {
                        int frequency = info.getFrequency();
                        String band = null;
                        if (frequency >= 2400 && frequency < 2500) {
                            band = res.getString(R.string.wifi_band_24ghz);
                        } else if (frequency < 4900 || frequency >= 5900) {
                            Log.e("WifiConfigController", "Unexpected frequency " + frequency);
                        } else {
                            band = res.getString(R.string.wifi_band_5ghz);
                        }
                        if (band != null) {
                            addRow(group, R.string.wifi_frequency, band);
                        }
                    }
                    this.mView.findViewById(R.id.ip_fields).setVisibility(8);
                } else {
                    this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
                }
                if (this.mAccessPoint.isSaved() || this.mAccessPoint.isActive()) {
                    this.mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
                }
            }
        }
        this.mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        if (this.mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
        this.mWifiConfigControllerExt.addWifiConfigView(this.mEdit);
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = this.mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    private String getSignalString() {
        int level = this.mAccessPoint.getLevel();
        return (level <= -1 || level >= this.mLevels.length) ? null : this.mLevels[level];
    }

    void hideForgetButton() {
        Button forget = this.mConfigUi.getForgetButton();
        if (forget != null) {
            forget.setVisibility(8);
        }
    }

    void hideSubmitButton() {
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit != null) {
            submit.setVisibility(8);
        }
    }

    void enableSubmitIfAppropriate() {
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit != null) {
            boolean enabled;
            boolean passwordInvalid = false;
            if (this.mPasswordView != null) {
                if (!(this.mAccessPointSecurity == 1 && this.mPasswordView.length() == 0)) {
                    if (this.mAccessPointSecurity == 2 && this.mPasswordView.length() < 8) {
                    }
                }
                passwordInvalid = true;
            }
            passwordInvalid = this.mWifiConfigControllerExt.enableSubmitIfAppropriate(this.mPasswordView, this.mAccessPointSecurity, passwordInvalid);
            if (this.mSsidView == null || this.mSsidView.length() != 0) {
                if ((this.mAccessPoint != null && this.mAccessPoint.isSaved()) || !passwordInvalid) {
                    if (ipAndProxyFieldsAreValid()) {
                        enabled = true;
                    } else {
                        enabled = false;
                    }
                    submit.setEnabled(enabled);
                }
            }
            enabled = false;
            submit.setEnabled(enabled);
        }
    }

    WifiConfiguration getConfig() {
        if (!this.mEdit) {
            return null;
        }
        WifiConfiguration config = new WifiConfiguration();
        if (this.mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(this.mSsidView.getText().toString());
            config.hiddenSSID = true;
        } else if (this.mAccessPoint.isSaved()) {
            config.networkId = this.mAccessPoint.getConfig().networkId;
        } else {
            config.SSID = AccessPoint.convertToQuotedString(this.mAccessPoint.getSsidStr());
        }
        String password;
        switch (this.mAccessPointSecurity) {
            case 0:
                config.allowedKeyManagement.set(0);
                break;
            case 1:
                config.allowedKeyManagement.set(0);
                config.allowedAuthAlgorithms.set(0);
                config.allowedAuthAlgorithms.set(1);
                if (this.mPasswordView.length() != 0) {
                    int length = this.mPasswordView.length();
                    password = this.mPasswordView.getText().toString();
                    if ((length != 10 && length != 26 && length != 58) || !password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = '\"' + password + '\"';
                        break;
                    }
                    config.wepKeys[0] = password;
                    break;
                }
                break;
            case 2:
                config.allowedKeyManagement.set(1);
                if (this.mPasswordView.length() != 0) {
                    password = this.mPasswordView.getText().toString();
                    if (!password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = '\"' + password + '\"';
                        break;
                    }
                    config.preSharedKey = password;
                    break;
                }
                break;
            case 3:
                config.allowedKeyManagement.set(2);
                config.allowedKeyManagement.set(3);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = this.mWifiConfigControllerExt.getEapMethod(this.mEapMethodSpinner.getSelectedItemPosition());
                int phase2Method = this.mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case 0:
                        switch (phase2Method) {
                            case 0:
                                config.enterpriseConfig.setPhase2Method(0);
                                break;
                            case 1:
                                config.enterpriseConfig.setPhase2Method(3);
                                break;
                            case 2:
                                config.enterpriseConfig.setPhase2Method(4);
                                break;
                            default:
                                Log.e("WifiConfigController", "Unknown phase2 method" + phase2Method);
                                break;
                        }
                    default:
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }
                String caCert = (String) this.mEapCaCertSpinner.getSelectedItem();
                if (caCert.equals(this.unspecifiedCert)) {
                    caCert = "";
                }
                config.enterpriseConfig.setCaCertificateAlias(caCert);
                String clientCert = (String) this.mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(this.unspecifiedCert)) {
                    clientCert = "";
                }
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                config.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                config.enterpriseConfig.setAnonymousIdentity(this.mEapAnonymousView.getText().toString());
                if (this.mPasswordView.isShown()) {
                    if (this.mPasswordView.length() > 0) {
                        config.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                        break;
                    }
                }
                config.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                break;
                break;
            case 4:
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                break;
            default:
                return null;
        }
        this.mWifiConfigControllerExt.setConfig(config, this.mAccessPointSecurity, this.mPasswordView, this.mEapMethodSpinner);
        config.setIpConfiguration(new IpConfiguration(this.mIpAssignment, this.mProxySettings, this.mStaticIpConfiguration, this.mHttpProxy));
        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        IpAssignment ipAssignment = (this.mIpSettingsSpinner == null || this.mIpSettingsSpinner.getSelectedItemPosition() != 1) ? IpAssignment.DHCP : IpAssignment.STATIC;
        this.mIpAssignment = ipAssignment;
        if (this.mIpAssignment == IpAssignment.STATIC) {
            this.mStaticIpConfiguration = new StaticIpConfiguration();
            if (validateIpConfigFields(this.mStaticIpConfiguration) != 0) {
                return false;
            }
        }
        int selectedPosition = this.mProxySettingsSpinner.getSelectedItemPosition();
        this.mProxySettings = ProxySettings.NONE;
        this.mHttpProxy = null;
        if (selectedPosition == 1 && this.mProxyHostView != null) {
            int result;
            this.mProxySettings = ProxySettings.STATIC;
            String host = this.mProxyHostView.getText().toString();
            String portStr = this.mProxyPortView.getText().toString();
            String exclusionList = this.mProxyExclusionListView.getText().toString();
            int i = 0;
            try {
                i = Integer.parseInt(portStr);
                result = ProxySelector.validate(host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
            }
            if (result != 0) {
                return false;
            }
            this.mHttpProxy = new ProxyInfo(host, i, exclusionList);
        } else if (selectedPosition == 2 && this.mProxyPacView != null) {
            this.mProxySettings = ProxySettings.PAC;
            CharSequence uriSequence = this.mProxyPacView.getText();
            if (TextUtils.isEmpty(uriSequence)) {
                return false;
            }
            Uri uri = Uri.parse(uriSequence.toString());
            if (uri == null) {
                return false;
            }
            this.mHttpProxy = new ProxyInfo(uri);
        }
        return true;
    }

    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        if (this.mIpAddressView == null) {
            return 0;
        }
        String ipAddr = this.mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        try {
            InetAddress inetAddr = NetworkUtils.numericToInetAddress(ipAddr);
            if (!(inetAddr instanceof Inet4Address)) {
                return R.string.wifi_ip_settings_invalid_ip_address;
            }
            int networkPrefixLength = -1;
            try {
                networkPrefixLength = Integer.parseInt(this.mNetworkPrefixLengthView.getText().toString());
                if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                    return R.string.wifi_ip_settings_invalid_network_prefix_length;
                }
                InetAddress dnsAddr;
                staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
                String gateway = this.mGatewayView.getText().toString();
                if (TextUtils.isEmpty(gateway)) {
                    try {
                        byte[] addr = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength).getAddress();
                        addr[addr.length - 1] = (byte) 1;
                        this.mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
                    } catch (RuntimeException e) {
                    } catch (UnknownHostException e2) {
                    }
                } else {
                    InetAddress gatewayAddr = getIPv4Address(gateway);
                    if (gatewayAddr == null) {
                        return R.string.wifi_ip_settings_invalid_gateway;
                    }
                    staticIpConfiguration.gateway = gatewayAddr;
                }
                String dns = this.mDns1View.getText().toString();
                if (TextUtils.isEmpty(dns)) {
                    this.mDns1View.setText(this.mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
                } else {
                    dnsAddr = getIPv4Address(dns);
                    if (dnsAddr == null) {
                        return R.string.wifi_ip_settings_invalid_dns;
                    }
                    staticIpConfiguration.dnsServers.add(dnsAddr);
                }
                if (this.mDns2View.length() > 0) {
                    dnsAddr = getIPv4Address(this.mDns2View.getText().toString());
                    if (dnsAddr == null) {
                        return R.string.wifi_ip_settings_invalid_dns;
                    }
                    staticIpConfiguration.dnsServers.add(dnsAddr);
                }
                return 0;
            } catch (NumberFormatException e3) {
                this.mNetworkPrefixLengthView.setText(this.mConfigUi.getContext().getString(R.string.wifi_network_prefix_length_hint));
            }
        } catch (IllegalArgumentException e4) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
    }

    private void showSecurityFields() {
        this.mWifiConfigControllerExt.setWapiCertSpinnerInvisible(this.mAccessPointSecurity);
        if (this.mAccessPointSecurity == 0) {
            this.mView.findViewById(R.id.security_fields).setVisibility(8);
        } else if (!this.mWifiConfigControllerExt.showSecurityFields(this.mAccessPointSecurity, this.mEdit)) {
            this.mView.findViewById(R.id.security_fields).setVisibility(0);
            if (this.mPasswordView == null) {
                this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
                this.mPasswordView.addTextChangedListener(this);
                ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnCheckedChangeListener(this);
                if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
                    this.mPasswordView.setHint(R.string.wifi_unchanged);
                }
            }
            if (this.mAccessPointSecurity != 3) {
                this.mView.findViewById(R.id.eap).setVisibility(8);
                return;
            }
            this.mView.findViewById(R.id.eap).setVisibility(0);
            if (this.mEapMethodSpinner == null) {
                this.mEapMethodSpinner = (Spinner) this.mView.findViewById(R.id.method);
                this.mWifiConfigControllerExt.setEapmethodSpinnerAdapter();
                this.mEapMethodSpinner.setOnItemSelectedListener(this);
                if (Utils.isWifiOnly(this.mContext) || !this.mContext.getResources().getBoolean(17957022)) {
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter(this.mContext, 17367048, this.mContext.getResources().getStringArray(R.array.eap_method_without_sim_auth));
                    spinnerAdapter.setDropDownViewResource(17367049);
                    this.mEapMethodSpinner.setAdapter(spinnerAdapter);
                }
                this.mPhase2Spinner = (Spinner) this.mView.findViewById(R.id.phase2);
                this.mEapCaCertSpinner = (Spinner) this.mView.findViewById(R.id.ca_cert);
                this.mEapUserCertSpinner = (Spinner) this.mView.findViewById(R.id.user_cert);
                this.mEapIdentityView = (TextView) this.mView.findViewById(R.id.identity);
                this.mEapAnonymousView = (TextView) this.mView.findViewById(R.id.anonymous);
                loadCertificates(this.mEapCaCertSpinner, "CACERT_");
                loadCertificates(this.mEapUserCertSpinner, "USRPKEY_");
                if (this.mAccessPoint == null || !this.mAccessPoint.isSaved()) {
                    showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
                } else {
                    WifiEnterpriseConfig enterpriseConfig = this.mAccessPoint.getConfig().enterpriseConfig;
                    int eapMethod = enterpriseConfig.getEapMethod();
                    int phase2Method = enterpriseConfig.getPhase2Method();
                    this.mEapMethodSpinner.setSelection(eapMethod);
                    showEapFieldsByMethod(eapMethod);
                    this.mWifiConfigControllerExt.setEapMethodSelection(this.mEapMethodSpinner, eapMethod);
                    switch (eapMethod) {
                        case 0:
                            switch (phase2Method) {
                                case 0:
                                    this.mPhase2Spinner.setSelection(0);
                                    break;
                                case 3:
                                    this.mPhase2Spinner.setSelection(1);
                                    break;
                                case 4:
                                    this.mPhase2Spinner.setSelection(2);
                                    break;
                                default:
                                    Log.e("WifiConfigController", "Invalid phase 2 method " + phase2Method);
                                    break;
                            }
                        default:
                            this.mPhase2Spinner.setSelection(phase2Method);
                            break;
                    }
                    setSelection(this.mEapCaCertSpinner, enterpriseConfig.getCaCertificateAlias());
                    setSelection(this.mEapUserCertSpinner, enterpriseConfig.getClientCertificateAlias());
                    this.mEapIdentityView.setText(enterpriseConfig.getIdentity());
                    this.mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
                }
            } else {
                showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
            }
            this.mWifiConfigControllerExt.setEapMethodFields(this.mEdit);
            this.mWifiConfigControllerExt.setGEMINI(this.mEapMethodSpinner.getSelectedItemPosition());
        }
    }

    private void showEapFieldsByMethod(int eapMethod) {
        this.mView.findViewById(R.id.l_method).setVisibility(0);
        this.mView.findViewById(R.id.l_identity).setVisibility(0);
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(0);
        this.mView.findViewById(R.id.password_layout).setVisibility(0);
        this.mView.findViewById(R.id.show_password_layout).setVisibility(0);
        Context context = this.mConfigUi.getContext();
        switch (eapMethod) {
            case 0:
                if (this.mPhase2Adapter != this.PHASE2_PEAP_ADAPTER) {
                    this.mPhase2Adapter = this.PHASE2_PEAP_ADAPTER;
                    this.mPhase2Spinner.setAdapter(this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                return;
            case 1:
                this.mView.findViewById(R.id.l_user_cert).setVisibility(0);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                return;
            case 2:
                if (this.mPhase2Adapter != this.PHASE2_FULL_ADAPTER) {
                    this.mPhase2Adapter = this.PHASE2_FULL_ADAPTER;
                    this.mPhase2Spinner.setAdapter(this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                return;
            case 3:
                setPhase2Invisible();
                setCaCertInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                return;
            case 4:
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
            case R$styleable.SuwSetupWizardLayout_suwIllustrationAspectRatio /*6*/:
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setCaCertInvisible();
                setUserCertInvisible();
                setPasswordInvisible();
                setIdentityInvisible();
                return;
            default:
                return;
        }
    }

    private void setIdentityInvisible() {
        this.mView.findViewById(R.id.l_identity).setVisibility(8);
        this.mPhase2Spinner.setSelection(0);
    }

    private void setPhase2Invisible() {
        this.mView.findViewById(R.id.l_phase2).setVisibility(8);
        this.mPhase2Spinner.setSelection(0);
    }

    private void setCaCertInvisible() {
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(8);
        this.mEapCaCertSpinner.setSelection(0);
    }

    private void setUserCertInvisible() {
        this.mView.findViewById(R.id.l_user_cert).setVisibility(8);
        this.mEapUserCertSpinner.setSelection(0);
    }

    private void setAnonymousIdentInvisible() {
        this.mView.findViewById(R.id.l_anonymous).setVisibility(8);
        this.mEapAnonymousView.setText("");
    }

    private void setPasswordInvisible() {
        this.mPasswordView.setText("");
        this.mView.findViewById(R.id.password_layout).setVisibility(8);
        this.mView.findViewById(R.id.show_password_layout).setVisibility(8);
    }

    private void showIpConfigFields() {
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.ip_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
            config = this.mAccessPoint.getConfig();
        }
        if (this.mIpSettingsSpinner.getSelectedItemPosition() == 1) {
            this.mView.findViewById(R.id.staticip).setVisibility(0);
            if (this.mIpAddressView == null) {
                this.mIpAddressView = (TextView) this.mView.findViewById(R.id.ipaddress);
                this.mIpAddressView.addTextChangedListener(this);
                this.mGatewayView = (TextView) this.mView.findViewById(R.id.gateway);
                this.mGatewayView.addTextChangedListener(this);
                this.mNetworkPrefixLengthView = (TextView) this.mView.findViewById(R.id.network_prefix_length);
                this.mNetworkPrefixLengthView.addTextChangedListener(this);
                this.mDns1View = (TextView) this.mView.findViewById(R.id.dns1);
                this.mDns1View.addTextChangedListener(this);
                this.mDns2View = (TextView) this.mView.findViewById(R.id.dns2);
                this.mDns2View.addTextChangedListener(this);
            }
            if (config != null) {
                StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                if (staticConfig != null) {
                    if (staticConfig.ipAddress != null) {
                        this.mIpAddressView.setText(staticConfig.ipAddress.getAddress().getHostAddress());
                        this.mNetworkPrefixLengthView.setText(Integer.toString(staticConfig.ipAddress.getNetworkPrefixLength()));
                    }
                    if (staticConfig.gateway != null) {
                        this.mGatewayView.setText(staticConfig.gateway.getHostAddress());
                    }
                    Iterator<InetAddress> dnsIterator = staticConfig.dnsServers.iterator();
                    if (dnsIterator.hasNext()) {
                        this.mDns1View.setText(((InetAddress) dnsIterator.next()).getHostAddress());
                    }
                    if (dnsIterator.hasNext()) {
                        this.mDns2View.setText(((InetAddress) dnsIterator.next()).getHostAddress());
                    }
                }
                this.mWifiConfigControllerExt.restrictIpv4View(config);
                return;
            }
            return;
        }
        this.mView.findViewById(R.id.staticip).setVisibility(8);
    }

    private void showProxyFields() {
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.proxy_settings_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
            config = this.mAccessPoint.getConfig();
        }
        if (this.mProxySettingsSpinner.getSelectedItemPosition() == 1) {
            setVisibility(R.id.proxy_warning_limited_support, 0);
            setVisibility(R.id.proxy_fields, 0);
            setVisibility(R.id.proxy_pac_field, 8);
            if (this.mProxyHostView == null) {
                this.mProxyHostView = (TextView) this.mView.findViewById(R.id.proxy_hostname);
                this.mProxyHostView.addTextChangedListener(this);
                this.mProxyPortView = (TextView) this.mView.findViewById(R.id.proxy_port);
                this.mProxyPortView.addTextChangedListener(this);
                this.mProxyExclusionListView = (TextView) this.mView.findViewById(R.id.proxy_exclusionlist);
                this.mProxyExclusionListView.addTextChangedListener(this);
                this.mWifiConfigControllerExt.setProxyText(this.mView);
            }
            if (config != null) {
                ProxyInfo proxyProperties = config.getHttpProxy();
                if (proxyProperties != null) {
                    this.mProxyHostView.setText(proxyProperties.getHost());
                    this.mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    this.mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
                }
            }
        } else if (this.mProxySettingsSpinner.getSelectedItemPosition() == 2) {
            setVisibility(R.id.proxy_warning_limited_support, 8);
            setVisibility(R.id.proxy_fields, 8);
            setVisibility(R.id.proxy_pac_field, 0);
            if (this.mProxyPacView == null) {
                this.mProxyPacView = (TextView) this.mView.findViewById(R.id.proxy_pac);
                this.mProxyPacView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyInfo = config.getHttpProxy();
                if (proxyInfo != null) {
                    this.mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
                }
            }
        } else {
            setVisibility(R.id.proxy_warning_limited_support, 8);
            setVisibility(R.id.proxy_fields, 8);
            setVisibility(R.id.proxy_pac_field, 8);
        }
    }

    private void setVisibility(int id, int visibility) {
        View v = this.mView.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        Context context = this.mConfigUi.getContext();
        String[] certs = KeyStore.getInstance().list(prefix, 1010);
        if (certs == null || certs.length == 0) {
            certs = new String[]{this.unspecifiedCert};
        } else {
            String[] array = new String[(certs.length + 1)];
            array[0] = this.unspecifiedCert;
            System.arraycopy(certs, 0, array, 1, certs.length);
            certs = array;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter(context, 17367048, certs);
        adapter.setDropDownViewResource(17367049);
        spinner.setAdapter(adapter);
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; i--) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }
    }

    public boolean isModify() {
        return this.mModify;
    }

    public void afterTextChanged(Editable s) {
        this.mTextViewChangedHandler.post(new C06061());
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        if (view.getId() == R.id.show_password) {
            int i;
            int pos = this.mPasswordView.getSelectionEnd();
            TextView textView = this.mPasswordView;
            if (isChecked) {
                i = 144;
            } else {
                i = 128;
            }
            textView.setInputType(i | 1);
            if (pos >= 0) {
                ((EditText) this.mPasswordView).setSelection(pos);
            }
        } else if (view.getId() == R.id.wifi_advanced_togglebox) {
            if (isChecked) {
                this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(0);
            } else {
                this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(8);
            }
        }
        onCheckedChanged(view);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == this.mSecuritySpinner) {
            this.mAccessPointSecurity = position;
            this.mAccessPointSecurity = this.mWifiConfigControllerExt.getSecurity(this.mAccessPointSecurity);
            showSecurityFields();
        } else if (parent == this.mEapMethodSpinner) {
            showSecurityFields();
        } else if (parent == this.mProxySettingsSpinner) {
            showProxyFields();
        } else {
            showIpConfigFields();
        }
        enableSubmitIfAppropriate();
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public void updatePassword() {
        int i;
        TextView passwdView = (TextView) this.mView.findViewById(R.id.password);
        if (((CheckBox) this.mView.findViewById(R.id.show_password)).isChecked()) {
            i = 144;
        } else {
            i = 128;
        }
        passwdView.setInputType(i | 1);
    }

    public AccessPoint getAccessPoint() {
        return this.mAccessPoint;
    }

    public WifiConfiguration getAccessPointConfig() {
        if (this.mAccessPoint != null) {
            return this.mAccessPoint.getConfig();
        }
        return null;
    }

    public String getAccessPointSsid() {
        if (this.mAccessPoint != null) {
            return this.mAccessPoint.getSsidStr();
        }
        return null;
    }

    public int getAccessPointSecurity() {
        if (this.mAccessPoint != null) {
            return this.mAccessPoint.getSecurity();
        }
        return 0;
    }

    public DetailedState getAccessPointState() {
        DetailedState detailedState = null;
        if (this.mAccessPoint == null) {
            return null;
        }
        if (this.mAccessPoint.getNetworkInfo() != null) {
            detailedState = this.mAccessPoint.getNetworkInfo().getDetailedState();
        }
        return detailedState;
    }

    public void onCheckedChanged(CompoundButton view) {
        if (view.getId() == R.id.hex_password) {
            this.mWifiConfigControllerExt.setHex(((CheckBox) view).isChecked());
            enableSubmitIfAppropriate();
            Log.d("WifiConfigController", "onClick mHex is=" + ((CheckBox) view).isChecked() + ",enableSubmitIfAppropriate");
        }
    }
}
