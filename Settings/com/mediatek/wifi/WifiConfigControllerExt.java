package com.mediatek.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.SystemProperties;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.wifi.WifiConfigController;
import com.android.settings.wifi.WifiConfigUiBase;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.settings.ext.IWifiExt.Builder;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class WifiConfigControllerExt {
    private static String sWFATestFlag = null;
    private WifiConfigUiBase mConfigUi;
    private Context mContext = this.mConfigUi.getContext();
    private WifiConfigController mController;
    private IWifiExt mExt;
    private boolean mHex;
    private Spinner mSimSlot;
    private TelephonyManager mTelephonyManager;
    private View mView;
    private Spinner mWapiAsCert;
    private Spinner mWapiClientCert;

    public WifiConfigControllerExt(WifiConfigController controller, WifiConfigUiBase configUi, View view) {
        this.mController = controller;
        this.mConfigUi = configUi;
        this.mView = view;
        this.mExt = UtilsExt.getWifiPlugin(this.mContext);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
    }

    public void addViews(WifiConfigUiBase configUi, String security) {
        ViewGroup group = (ViewGroup) this.mView.findViewById(R.id.info);
        View row = configUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(configUi.getContext().getString(R.string.wifi_security));
        this.mExt.setSecurityText((TextView) row.findViewById(R.id.name));
        ((TextView) row.findViewById(R.id.value)).setText(security);
        group.addView(row);
    }

    public static String addQuote(String s) {
        return "\"" + s + "\"";
    }

    public boolean enableSubmitIfAppropriate(TextView passwordView, int accessPointSecurity, boolean pwInvalid) {
        boolean passwordInvalid = pwInvalid;
        if (passwordView != null) {
            if ((accessPointSecurity != 1 || isWEPKeyValid(passwordView.getText().toString())) && (accessPointSecurity != 2 || passwordView.length() >= 8)) {
                if (accessPointSecurity == 4) {
                    if (passwordView.length() >= 8 && 64 >= passwordView.length()) {
                        if (this.mHex) {
                            if (passwordView.getText().toString().matches("[0-9A-Fa-f]*")) {
                            }
                        }
                    }
                }
            }
            passwordInvalid = true;
        }
        if (accessPointSecurity != 5) {
            return passwordInvalid;
        }
        if (this.mWapiAsCert == null || this.mWapiAsCert.getSelectedItemPosition() != 0) {
            if (this.mWapiClientCert == null || this.mWapiClientCert.getSelectedItemPosition() != 0) {
                return passwordInvalid;
            }
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isWEPKeyValid(String password) {
        if (password == null || password.length() == 0) {
            return false;
        }
        int keyLength = password.length();
        if (!(keyLength == 10 || keyLength == 26)) {
            if (keyLength == 32) {
            }
            if (!(keyLength == 5 || keyLength == 13 || keyLength == 16)) {
                return false;
            }
            return true;
        }
    }

    public void setConfig(WifiConfiguration config, int accessPointSecurity, TextView passwordView, Spinner eapMethodSpinner) {
        config.priority = this.mExt.getPriority(config.priority);
        switch (accessPointSecurity) {
            case 3:
                config.simSlot = addQuote("-1");
                Log.d("WifiConfigControllerExt", "(String) eapMethodSpinner.getSelectedItem()=" + ((String) eapMethodSpinner.getSelectedItem()));
                if ("AKA".equals((String) eapMethodSpinner.getSelectedItem()) || "SIM".equals((String) eapMethodSpinner.getSelectedItem()) || "AKA'".equals((String) eapMethodSpinner.getSelectedItem())) {
                    eapSimAkaSimSlotConfig(config, eapMethodSpinner);
                    Log.d("WifiConfigControllerExt", "eap-sim/aka, config.toString(): " + config.toString());
                    return;
                }
                return;
            case 4:
                config.allowedKeyManagement.set(5);
                config.allowedProtocols.set(2);
                config.allowedPairwiseCiphers.set(3);
                config.allowedGroupCiphers.set(4);
                if (passwordView.length() != 0) {
                    String password = passwordView.getText().toString();
                    Log.v("WifiConfigControllerExt", "getConfig(), mHex=" + this.mHex);
                    if (this.mHex) {
                        config.preSharedKey = password;
                        return;
                    } else {
                        config.preSharedKey = '\"' + password + '\"';
                        return;
                    }
                }
                return;
            case R$styleable.SuwSetupWizardLayout_suwIllustration /*5*/:
                String str;
                config.allowedKeyManagement.set(6);
                config.allowedProtocols.set(2);
                config.allowedPairwiseCiphers.set(3);
                config.allowedGroupCiphers.set(4);
                WifiEnterpriseConfig wifiEnterpriseConfig = config.enterpriseConfig;
                if (this.mWapiAsCert.getSelectedItemPosition() == 0) {
                    str = "";
                } else {
                    str = (String) this.mWapiAsCert.getSelectedItem();
                }
                wifiEnterpriseConfig.setCaCertificateWapiAlias(str);
                wifiEnterpriseConfig = config.enterpriseConfig;
                if (this.mWapiClientCert.getSelectedItemPosition() == 0) {
                    str = "";
                } else {
                    str = (String) this.mWapiClientCert.getSelectedItem();
                }
                wifiEnterpriseConfig.setClientCertificateWapiAlias(str);
                return;
            default:
                return;
        }
    }

    private void eapSimAkaSimSlotConfig(WifiConfiguration config, Spinner eapMethodSpinner) {
        if (this.mSimSlot == null) {
            Log.d("WifiConfigControllerExt", "mSimSlot is null");
            this.mSimSlot = (Spinner) this.mView.findViewById(R.id.sim_slot);
        }
        String strSimAka = (String) eapMethodSpinner.getSelectedItem();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Log.d("WifiConfigControllerExt", "((String) mSimSlot.getSelectedItem()) " + ((String) this.mSimSlot.getSelectedItem()));
            simSlotConfig(config, strSimAka);
            Log.d("WifiConfigControllerExt", "eap-sim, choose sim_slot" + ((String) this.mSimSlot.getSelectedItem()));
        }
        Log.d("WifiConfigControllerExt", "eap-sim, config.simSlot: " + config.simSlot);
    }

    private void simSlotConfig(WifiConfiguration config, String strSimAka) {
        int simSlot = this.mSimSlot.getSelectedItemPosition() - 1;
        if (simSlot > -1) {
            config.simSlot = addQuote("" + simSlot);
            Log.d("WifiConfigControllerExt", "config.simSlot " + addQuote("" + simSlot));
        }
    }

    public void setEapmethodSpinnerAdapter() {
        Spinner eapMethodSpinner = (Spinner) this.mView.findViewById(R.id.method);
        Context context = this.mConfigUi.getContext();
        ArrayAdapter<String> adapter = new ArrayAdapter(context, 17367048, new ArrayList(Arrays.asList(context.getResources().getStringArray(R.array.wifi_eap_method))));
        if (this.mController.getAccessPoint() != null) {
            this.mExt.setEapMethodArray(adapter, this.mController.getAccessPointSsid(), this.mController.getAccessPointSecurity());
        }
        adapter.setDropDownViewResource(17367049);
        eapMethodSpinner.setAdapter(adapter);
    }

    public void setEapMethodFields(boolean edit) {
        int eapMethod = ((Spinner) this.mView.findViewById(R.id.method)).getSelectedItemPosition();
        if (this.mController.getAccessPoint() != null) {
            eapMethod = this.mExt.getEapMethodbySpinnerPos(eapMethod, this.mController.getAccessPointSsid(), this.mController.getAccessPointSecurity());
        }
        Log.d("WifiConfigControllerExt", "showSecurityFields modify method = " + eapMethod);
        this.mExt.hideWifiConfigInfo(new Builder().setAccessPoint(this.mController.getAccessPoint()).setEdit(edit).setViews(this.mView), this.mConfigUi.getContext());
    }

    public void setGEMINI(int eapMethod) {
        Spinner eapMethodSpinner = (Spinner) this.mView.findViewById(R.id.method);
        if (this.mController.getAccessPoint() != null) {
            eapMethod = this.mExt.getEapMethodbySpinnerPos(eapMethod, this.mController.getAccessPointSsid(), this.mController.getAccessPointSecurity());
        }
        if (eapMethod == 4 || eapMethod == 5 || eapMethod == 6) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                this.mView.findViewById(R.id.sim_slot_fields).setVisibility(0);
                this.mSimSlot = (Spinner) this.mView.findViewById(R.id.sim_slot);
                Context context = this.mConfigUi.getContext();
                String[] tempSimAkaMethods = context.getResources().getStringArray(R.array.sim_slot);
                int sum = this.mTelephonyManager.getSimCount();
                Log.d("WifiConfigControllerExt", "the num of sim slot is :" + sum);
                String[] simAkaMethods = new String[(sum + 1)];
                for (int i = 0; i < sum + 1; i++) {
                    if (i < tempSimAkaMethods.length) {
                        simAkaMethods[i] = tempSimAkaMethods[i];
                    } else {
                        simAkaMethods[i] = tempSimAkaMethods[1].replaceAll("1", "" + i);
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter(context, 17367048, simAkaMethods);
                adapter.setDropDownViewResource(17367049);
                this.mSimSlot.setAdapter(adapter);
                if (this.mController.getAccessPoint() != null && this.mController.getAccessPoint().isSaved()) {
                    WifiConfiguration config = this.mController.getAccessPointConfig();
                    if (config != null && config.simSlot != null) {
                        String[] simslots = config.simSlot.split("\"");
                        if (simslots.length > 1) {
                            this.mSimSlot.setSelection(Integer.parseInt(simslots[1]) + 1);
                        }
                    }
                }
            }
        } else if (FeatureOption.MTK_GEMINI_SUPPORT) {
            this.mView.findViewById(R.id.sim_slot_fields).setVisibility(8);
        }
    }

    public boolean showSecurityFields(int accessPointSecurity, boolean edit) {
        Log.d("WifiConfigControllerExt", "showSecurityFields, accessPointSecurity = " + accessPointSecurity);
        Log.d("WifiConfigControllerExt", "showSecurityFields, edit = " + edit);
        if (accessPointSecurity != 3) {
            ((TextView) this.mView.findViewById(R.id.identity)).setEnabled(true);
            ((CheckBox) this.mView.findViewById(R.id.show_password)).setEnabled(true);
            this.mView.findViewById(R.id.eap).setVisibility(8);
            this.mView.findViewById(R.id.eap_identity).setVisibility(8);
        }
        this.mView.findViewById(R.id.hex_password).setVisibility(8);
        if (accessPointSecurity == 4) {
            this.mView.findViewById(R.id.hex_password).setVisibility(0);
            ((CheckBox) this.mView.findViewById(R.id.hex_password)).setChecked(this.mHex);
        }
        if (accessPointSecurity == 5) {
            this.mView.findViewById(R.id.security_fields).setVisibility(8);
            this.mView.findViewById(R.id.wapi_cert_fields).setVisibility(0);
            this.mWapiAsCert = (Spinner) this.mView.findViewById(R.id.wapi_as_cert);
            this.mWapiClientCert = (Spinner) this.mView.findViewById(R.id.wapi_user_cert);
            this.mWapiAsCert.setOnItemSelectedListener(this.mController);
            this.mWapiClientCert.setOnItemSelectedListener(this.mController);
            loadCertificates(this.mWapiAsCert, "WAPISERVERCERT_");
            loadCertificates(this.mWapiClientCert, "WAPIUSERCERT_");
            if (this.mController.getAccessPoint() != null && this.mController.getAccessPoint().isSaved()) {
                WifiConfiguration config = this.mController.getAccessPointConfig();
                setCertificate(this.mWapiAsCert, "WAPISERVERCERT_", config.enterpriseConfig.getCaCertificateWapiAlias());
                setCertificate(this.mWapiClientCert, "WAPIUSERCERT_", config.enterpriseConfig.getClientCertificateWapiAlias());
            }
            return true;
        }
        if (accessPointSecurity == 3) {
            this.mView.findViewById(R.id.eap_identity).setVisibility(0);
        }
        setHexCheckBoxListener();
        this.mExt.hideWifiConfigInfo(new Builder().setAccessPoint(this.mController.getAccessPoint()).setEdit(edit).setViews(this.mView), this.mConfigUi.getContext());
        return false;
    }

    public void setWapiCertSpinnerInvisible(int accessPointSecurity) {
        if (accessPointSecurity != 5) {
            this.mView.findViewById(R.id.wapi_cert_fields).setVisibility(8);
        }
    }

    public void setHexCheckBoxListener() {
        ((CheckBox) this.mView.findViewById(R.id.hex_password)).setOnCheckedChangeListener(this.mController);
    }

    private void setCertificate(Spinner spinner, String prefix, String cert) {
        if (cert != null && cert.startsWith(prefix)) {
            setSelection(spinner, cert.substring(prefix.length()));
        }
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

    private void loadCertificates(Spinner spinner, String prefix) {
        Context context = this.mConfigUi.getContext();
        String unspecifiedCert = context.getString(R.string.wifi_unspecified);
        String[] certs = KeyStore.getInstance().list(prefix, 1010);
        if (certs == null || certs.length == 0) {
            certs = new String[]{unspecifiedCert};
        } else {
            String[] array = new String[(certs.length + 1)];
            array[0] = unspecifiedCert;
            System.arraycopy(certs, 0, array, 1, certs.length);
            certs = array;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter(context, 17367048, certs);
        adapter.setDropDownViewResource(17367049);
        spinner.setAdapter(adapter);
    }

    public void setHex(boolean hexEnabled) {
        this.mHex = hexEnabled;
    }

    public int getEapMethod(int eapMethod) {
        Log.d("WifiConfigControllerExt", "getEapMethod, eapMethod = " + eapMethod);
        int result = eapMethod;
        if (this.mController.getAccessPoint() != null) {
            result = this.mExt.getEapMethodbySpinnerPos(eapMethod, this.mController.getAccessPointSsid(), this.mController.getAccessPointSecurity());
        }
        Log.d("WifiConfigControllerExt", "getEapMethod, result = " + result);
        return result;
    }

    public void setEapMethodSelection(Spinner eapMethodSpinner, int eapMethod) {
        int eapMethodPos = eapMethod;
        if (this.mController.getAccessPoint() != null) {
            eapMethodPos = this.mExt.getPosByEapMethod(eapMethod, this.mController.getAccessPointSsid(), this.mController.getAccessPointSecurity());
        }
        eapMethodSpinner.setSelection(eapMethodPos);
        Log.d("WifiConfigControllerExt", "[skyfyx]showSecurityFields modify pos = " + eapMethodPos);
        Log.d("WifiConfigControllerExt", "[skyfyx]showSecurityFields modify method = " + eapMethod);
    }

    public void setProxyText(View view) {
        this.mExt.setProxyText((TextView) view.findViewById(R.id.proxy_exclusionlist_text));
    }

    public void restrictIpv4View(WifiConfiguration config) {
        TextView ipAddressView = (TextView) this.mView.findViewById(R.id.ipaddress);
        TextView gatewayView = (TextView) this.mView.findViewById(R.id.gateway);
        TextView networkPrefixLengthView = (TextView) this.mView.findViewById(R.id.network_prefix_length);
        TextView dns1View = (TextView) this.mView.findViewById(R.id.dns1);
        TextView dns2View = (TextView) this.mView.findViewById(R.id.dns2);
        StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
        Log.d("WifiConfigControllerExt", "staticConfig = " + staticConfig);
        if (staticConfig != null) {
            Log.d("WifiConfigControllerExt", "IpAddressView = " + staticConfig.ipAddress);
            if (staticConfig.ipAddress != null && (staticConfig.ipAddress.getAddress() instanceof Inet4Address)) {
                ipAddressView.setText(staticConfig.ipAddress.getAddress().getHostAddress());
                networkPrefixLengthView.setText(Integer.toString(staticConfig.ipAddress.getNetworkPrefixLength()));
            }
            Log.d("WifiConfigControllerExt", "gatewayView = " + staticConfig.gateway);
            if (staticConfig.gateway != null && (staticConfig.gateway instanceof Inet4Address)) {
                gatewayView.setText(staticConfig.gateway.getHostAddress());
            }
            Iterator<InetAddress> dnsIterator = staticConfig.dnsServers.iterator();
            while (dnsIterator.hasNext()) {
                InetAddress dsn1 = (InetAddress) dnsIterator.next();
                Log.d("WifiConfigControllerExt", "dsn1 = " + dsn1);
                if (dsn1 instanceof Inet4Address) {
                    dns1View.setText(dsn1.getHostAddress());
                    break;
                }
            }
            while (dnsIterator.hasNext()) {
                InetAddress dsn2 = (InetAddress) dnsIterator.next();
                Log.d("WifiConfigControllerExt", "dsn2 = " + dsn2);
                if (dsn2 instanceof Inet4Address) {
                    dns2View.setText(dsn2.getHostAddress());
                    return;
                }
            }
        }
    }

    public void addWifiConfigView(boolean edit) {
        this.mExt.setSecurityText((TextView) this.mView.findViewById(R.id.security_text));
        if (this.mController.getAccessPoint() == null) {
            int viewId = R.id.security;
            if (FeatureOption.MTK_WAPI_SUPPORT) {
                String type = SystemProperties.get("persist.sys.wlan", "wifi-wapi");
                if (type.equals("wifi-wapi")) {
                    if (isWFATestSupported()) {
                        viewId = R.id.security_wfa;
                    } else {
                        viewId = R.id.security;
                    }
                } else if (type.equals("wifi")) {
                    if (isWFATestSupported()) {
                        viewId = R.id.wpa_security_wfa;
                    } else {
                        viewId = R.id.wpa_security;
                    }
                } else if (type.equals("wapi")) {
                    viewId = R.id.wapi_security;
                }
            } else if (isWFATestSupported()) {
                viewId = R.id.wpa_security_wfa;
            } else {
                viewId = R.id.wpa_security;
            }
            switchWlanSecuritySpinner((Spinner) this.mView.findViewById(viewId));
        } else {
            WifiConfiguration config = this.mController.getAccessPointConfig();
            Log.d("WifiConfigControllerExt", "addWifiConfigView, config = " + config);
            this.mExt.setAPNetworkId(config);
            if (this.mController.getAccessPoint().isSaved() && config != null) {
                Log.d("WifiConfigControllerExt", "priority=" + config.priority);
                this.mExt.setAPPriority(config.priority);
            }
            this.mExt.setPriorityView((LinearLayout) this.mView.findViewById(R.id.priority_field), config, edit);
        }
        this.mExt.addDisconnectButton((AlertDialog) this.mConfigUi, edit, this.mController.getAccessPointState(), this.mController.getAccessPointConfig());
        this.mExt.hideWifiConfigInfo(new Builder().setAccessPoint(this.mController.getAccessPoint()).setEdit(edit).setViews(this.mView), this.mConfigUi.getContext());
    }

    private void switchWlanSecuritySpinner(Spinner securitySpinner) {
        ((Spinner) this.mView.findViewById(R.id.security)).setVisibility(8);
        ((Spinner) this.mView.findViewById(R.id.wapi_security)).setVisibility(8);
        ((Spinner) this.mView.findViewById(R.id.wpa_security)).setVisibility(8);
        ((Spinner) this.mView.findViewById(R.id.security_wfa)).setVisibility(8);
        ((Spinner) this.mView.findViewById(R.id.wpa_security_wfa)).setVisibility(8);
        securitySpinner.setVisibility(0);
        securitySpinner.setOnItemSelectedListener(this.mController);
    }

    public int getSecurity(int accessPointSecurity) {
        Log.d("WifiConfigControllerExt", "getSecurity, accessPointSecurity = " + accessPointSecurity);
        if (FeatureOption.MTK_WAPI_SUPPORT) {
            String type = SystemProperties.get("persist.sys.wlan", "wifi-wapi");
            if (type.equals("wapi") && accessPointSecurity > 0) {
                accessPointSecurity += 3;
            } else if (type.equals("wifi")) {
                if (isWFATestSupported() && accessPointSecurity > 2) {
                    accessPointSecurity--;
                }
            } else if (type.equals("wifi-wapi") && isWFATestSupported() && accessPointSecurity > 2) {
                accessPointSecurity--;
            }
        } else if (isWFATestSupported() && accessPointSecurity > 2) {
            accessPointSecurity--;
        }
        Log.d("WifiConfigControllerExt", "getSecurity, accessPointSecurity = " + accessPointSecurity);
        return accessPointSecurity;
    }

    private static boolean isWFATestSupported() {
        if (sWFATestFlag == null) {
            sWFATestFlag = SystemProperties.get("persist.radio.wifi.wpa2wpaalone", "");
            Log.d("WifiConfigControllerExt", "isWFATestSupported(), sWFATestFlag=" + sWFATestFlag);
        }
        return "true".equals(sWFATestFlag);
    }
}
