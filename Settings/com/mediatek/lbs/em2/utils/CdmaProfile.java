package com.mediatek.lbs.em2.utils;

public class CdmaProfile {
    public String mcpAddr = "";
    public boolean mcpEnable = false;
    public int mcpPort = 0;
    public String name = "";
    public String pdeAddr = "";
    public boolean pdeAddrValid = false;
    public int pdeIpType = 0;
    public int pdePort = 0;
    public String pdeUrlAddr = "";
    public boolean pdeUrlValid = false;

    public String toString() {
        String ret = (((("" + "name=[" + this.name + "] ") + "mcpEnable=[" + this.mcpEnable + "] ") + "mcpAddr=[" + this.mcpAddr + "] ") + "mcpPort=[" + this.mcpPort + "] ") + "pdeAddrValid=[" + this.pdeAddrValid + "] ";
        if (this.pdeIpType == 0) {
            ret = ret + "pdeIpType=[IPv4] ";
        } else if (this.pdeIpType != 1) {
            ret = ret + "pdeIpType=[UNKNOWN " + this.pdeIpType + "] ";
        } else {
            ret = ret + "pdeIpType=[IPv6] ";
        }
        return (((ret + "pdeAddr=[" + this.pdeAddr + "] ") + "pdePort=[" + this.pdePort + "] ") + "pdeUrlValid=[" + this.pdeUrlValid + "] ") + "pdeUrlAddr=[" + this.pdeUrlAddr + "] ";
    }
}
