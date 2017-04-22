package com.mediatek.lbs.em2.utils;

public class UpSetting {
    public boolean apnEnable = false;
    public boolean autoProfileEnable = false;
    public boolean caEnable = false;
    public int cdmaPreferred = 0;
    public boolean certFromSdcard = false;
    public boolean ecidEnable = false;
    public boolean lppEnable = false;
    public boolean msaEnable = false;
    public boolean msbEnable = false;
    public boolean niRequest = false;
    public boolean otdoaEnable = false;
    public int prefMethod = 1;
    public int qopDelay = 0;
    public int qopHacc = 0;
    public int qopLocAge = 0;
    public int qopVacc = 0;
    public boolean roaming = false;
    public boolean suplLog = false;
    public int suplVersion = 1;
    public boolean syncToslp = false;
    public int tlsVersion = 0;
    public boolean udpEnable = false;
    public byte ut2 = (byte) 11;
    public byte ut3 = (byte) 10;

    public String toString() {
        String ret = (("" + "caEnable=[" + this.caEnable + "] ") + "niRequest=[" + this.niRequest + "] ") + "roaming=[" + this.roaming + "] ";
        if (this.cdmaPreferred == 0) {
            ret = ret + "cdmaPreferred=[WCDMA] ";
        } else if (this.cdmaPreferred == 1) {
            ret = ret + "cdmaPreferred=[CDMA] ";
        } else if (this.cdmaPreferred != 2) {
            ret = ret + "cdmaPreferred=[UNKNOWN " + this.cdmaPreferred + "] ";
        } else {
            ret = ret + "cdmaPreferred=[CDMA_FORCE] ";
        }
        if (this.prefMethod == 0) {
            ret = ret + "prefMethod=[MSA] ";
        } else if (this.prefMethod == 1) {
            ret = ret + "prefMethod=[MSB] ";
        } else if (this.prefMethod != 2) {
            ret = ret + "prefMethod=[UNKNOWN " + this.prefMethod + "] ";
        } else {
            ret = ret + "prefMethod=[NO_PREF] ";
        }
        return ((((((((((((((((((ret + "suplVersion=[" + this.suplVersion + "] ") + "tlsVersion=[" + this.tlsVersion + "] ") + "suplLog=[" + this.suplLog + "] ") + "msaEnable=[" + this.msaEnable + "] ") + "msbEnable=[" + this.msbEnable + "] ") + "ecidEnable=[" + this.ecidEnable + "] ") + "otdoaEnable=[" + this.otdoaEnable + "] ") + "qopHacc=[" + this.qopHacc + "] ") + "qopVacc=[" + this.qopVacc + "] ") + "qopLocAge=[" + this.qopLocAge + "] ") + "qopDelay=[" + this.qopDelay + "] ") + "lppEnable=[" + this.lppEnable + "] ") + "certFromSdcard=[" + this.certFromSdcard + "] ") + "autoProfileEnable=[" + this.autoProfileEnable + "] ") + "ut2=[" + this.ut2 + "] ") + "ut3=[" + this.ut3 + "] ") + "apnEnable=[" + this.apnEnable + "] ") + "syncToslp=[" + this.syncToslp + "] ") + "udpEnable=[" + this.udpEnable + "] ";
    }
}
