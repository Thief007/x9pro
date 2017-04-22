package com.mediatek.lbs.em2.utils;

public class AgpsSetting {
    public boolean agpsEnable = false;
    public int agpsProtocol = 0;
    public boolean gpevt = false;

    public String toString() {
        String ret = "" + "agpsEnable=[" + this.agpsEnable + "] ";
        if (this.agpsProtocol == 0) {
            ret = ret + "agpsProtocol=[UP] ";
        } else if (this.agpsProtocol != 1) {
            ret = ret + "agpsProtocol=[UKNOWN " + this.agpsProtocol + "] ";
        } else {
            ret = ret + "agpsProtocol=[CP] ";
        }
        return ret + "gpevt=[" + this.gpevt + "] ";
    }
}
