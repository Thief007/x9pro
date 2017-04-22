package com.mediatek.lbs.em2.utils;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class AgpsInterface {
    protected LocalSocket client;
    protected DataInputStream in;
    protected BufferedOutputStream out;

    public AgpsInterface() throws IOException {
        checkVersion();
    }

    public void checkVersion() {
        try {
            connect();
            DataCoder.putInt(this.out, 1);
            DataCoder.putShort(this.out, (short) 1);
            DataCoder.putShort(this.out, (short) 1);
            this.out.flush();
            short majorVersion = (short) DataCoder.getShort(this.in);
            short minorVersion = (short) DataCoder.getShort(this.in);
            if (majorVersion != (short) 1) {
                throw new IOException("app maj ver=1 is not equal to AGPSD's maj ver=" + majorVersion);
            } else if (minorVersion >= (short) 1) {
                DataCoder.getByte(this.in);
                close();
            } else {
                throw new IOException("app min ver=1 is greater than AGPSD's min ver=" + minorVersion);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Throwable th) {
            close();
        }
    }

    public AgpsConfig getAgpsConfig() {
        AgpsConfig config = new AgpsConfig();
        try {
            connect();
            DataCoder.putInt(this.out, 100);
            this.out.flush();
            getAgpsConfigInt(100, config);
            DataCoder.getByte(this.in);
            close();
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Throwable th) {
            close();
        }
    }

    private void getAgpsConfigInt(int cmd, AgpsConfig config) throws IOException {
        AgpsSetting agpsSetting = config.getAgpsSetting();
        agpsSetting.agpsEnable = DataCoder.getBoolean(this.in);
        agpsSetting.agpsProtocol = DataCoder.getInt(this.in);
        agpsSetting.gpevt = DataCoder.getBoolean(this.in);
        CpSetting cpSetting = config.getCpSetting();
        cpSetting.molrPosMethod = DataCoder.getInt(this.in);
        cpSetting.externalAddrEnable = DataCoder.getBoolean(this.in);
        cpSetting.externalAddr = DataCoder.getString(this.in);
        cpSetting.mlcNumberEnable = DataCoder.getBoolean(this.in);
        cpSetting.mlcNumber = DataCoder.getString(this.in);
        cpSetting.cpAutoReset = DataCoder.getBoolean(this.in);
        cpSetting.epcMolrLppPayloadEnable = DataCoder.getBoolean(this.in);
        cpSetting.epcMolrLppPayload = DataCoder.getBinary(this.in);
        UpSetting upSetting = config.getUpSetting();
        upSetting.caEnable = DataCoder.getBoolean(this.in);
        upSetting.niRequest = DataCoder.getBoolean(this.in);
        upSetting.roaming = DataCoder.getBoolean(this.in);
        upSetting.cdmaPreferred = DataCoder.getInt(this.in);
        upSetting.prefMethod = DataCoder.getInt(this.in);
        upSetting.suplVersion = DataCoder.getInt(this.in);
        upSetting.tlsVersion = DataCoder.getInt(this.in);
        upSetting.suplLog = DataCoder.getBoolean(this.in);
        upSetting.msaEnable = DataCoder.getBoolean(this.in);
        upSetting.msbEnable = DataCoder.getBoolean(this.in);
        upSetting.ecidEnable = DataCoder.getBoolean(this.in);
        upSetting.otdoaEnable = DataCoder.getBoolean(this.in);
        upSetting.qopHacc = DataCoder.getInt(this.in);
        upSetting.qopVacc = DataCoder.getInt(this.in);
        upSetting.qopLocAge = DataCoder.getInt(this.in);
        upSetting.qopDelay = DataCoder.getInt(this.in);
        if (cmd >= 105) {
            upSetting.lppEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 106) {
            upSetting.certFromSdcard = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 107) {
            upSetting.autoProfileEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 108) {
            upSetting.ut2 = (byte) DataCoder.getByte(this.in);
            upSetting.ut3 = (byte) DataCoder.getByte(this.in);
        }
        if (cmd >= 109) {
            upSetting.apnEnable = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 110) {
            upSetting.syncToslp = DataCoder.getBoolean(this.in);
        }
        if (cmd >= 111) {
            upSetting.udpEnable = DataCoder.getBoolean(this.in);
        }
        SuplProfile suplProfile = config.getCurSuplProfile();
        suplProfile.name = DataCoder.getString(this.in);
        suplProfile.addr = DataCoder.getString(this.in);
        suplProfile.port = DataCoder.getInt(this.in);
        suplProfile.tls = DataCoder.getBoolean(this.in);
        suplProfile.mccMnc = DataCoder.getString(this.in);
        suplProfile.appId = DataCoder.getString(this.in);
        suplProfile.providerId = DataCoder.getString(this.in);
        suplProfile.defaultApn = DataCoder.getString(this.in);
        suplProfile.optionalApn = DataCoder.getString(this.in);
        suplProfile.optionalApn2 = DataCoder.getString(this.in);
        suplProfile.addressType = DataCoder.getString(this.in);
    }

    public void setSuplProfile(SuplProfile profile) {
        try {
            connect();
            DataCoder.putInt(this.out, 219);
            DataCoder.putString(this.out, profile.name);
            DataCoder.putString(this.out, profile.addr);
            DataCoder.putInt(this.out, profile.port);
            DataCoder.putBoolean(this.out, profile.tls);
            DataCoder.putString(this.out, profile.mccMnc);
            DataCoder.putString(this.out, profile.appId);
            DataCoder.putString(this.out, profile.providerId);
            DataCoder.putString(this.out, profile.defaultApn);
            DataCoder.putString(this.out, profile.optionalApn);
            DataCoder.putString(this.out, profile.optionalApn2);
            DataCoder.putString(this.out, profile.addressType);
            this.out.flush();
            DataCoder.getByte(this.in);
            close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Throwable th) {
            close();
        }
    }

    protected void connect() throws IOException {
        if (this.client != null) {
            this.client.close();
        }
        this.client = new LocalSocket();
        this.client.connect(new LocalSocketAddress("agpsd2", Namespace.RESERVED));
        this.client.setSoTimeout(3000);
        this.out = new BufferedOutputStream(this.client.getOutputStream());
        this.in = new DataInputStream(this.client.getInputStream());
    }

    protected void close() {
        try {
            if (this.client != null) {
                this.client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
