package com.mediatek.lbs.em2.utils;

public class CpSetting {
    public boolean cpAutoReset = false;
    public byte[] epcMolrLppPayload = new byte[0];
    public boolean epcMolrLppPayloadEnable = false;
    public String externalAddr = "";
    public boolean externalAddrEnable = false;
    public String mlcNumber = "";
    public boolean mlcNumberEnable = false;
    public int molrPosMethod = 0;

    public String toString() {
        String ret = "";
        if (this.molrPosMethod == 0) {
            ret = ret + "molrPosMethod=[LOC_EST] ";
        } else if (this.molrPosMethod != 1) {
            ret = ret + "molrPosMethod=[UNKNOWN " + this.molrPosMethod + "] ";
        } else {
            ret = ret + "molrPosMethod=[ASSIST_DATA] ";
        }
        ret = ((((((ret + "externalAddrEnable=[" + this.externalAddrEnable + "] ") + "externalAddr=[" + this.externalAddr + "] ") + "mlcNumberEnable=[" + this.mlcNumberEnable + "] ") + "mlcNumber=[" + this.mlcNumber + "] ") + "cpAutoReset=[" + this.cpAutoReset + "] ") + "epcMolrLppPayloadEnable=[" + this.epcMolrLppPayloadEnable + "] ") + "epcMolrLppPayload.len=[" + this.epcMolrLppPayload.length + "][";
        for (int i = 0; i < this.epcMolrLppPayload.length; i++) {
            ret = ret + String.format("%02x", new Object[]{Byte.valueOf(this.epcMolrLppPayload[i])});
        }
        return ret + "]";
    }
}
