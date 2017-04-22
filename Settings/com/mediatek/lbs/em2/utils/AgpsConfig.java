package com.mediatek.lbs.em2.utils;

import java.util.ArrayList;
import java.util.Iterator;

public class AgpsConfig {
    public AgpsSetting agpsSetting = new AgpsSetting();
    public CdmaProfile cdmaProfile = new CdmaProfile();
    public CpSetting cpSetting = new CpSetting();
    public SuplProfile curSuplProfile = new SuplProfile();
    public ArrayList<SuplProfile> suplProfiles = new ArrayList();
    public UpSetting upSetting = new UpSetting();

    public SuplProfile getCurSuplProfile() {
        return this.curSuplProfile;
    }

    public AgpsSetting getAgpsSetting() {
        return this.agpsSetting;
    }

    public CpSetting getCpSetting() {
        return this.cpSetting;
    }

    public UpSetting getUpSetting() {
        return this.upSetting;
    }

    public String toString() {
        String ret = "" + "### SuplProfiles ###\n";
        Iterator i$ = this.suplProfiles.iterator();
        while (i$.hasNext()) {
            ret = ret + ((SuplProfile) i$.next()) + "\n";
        }
        return (((((((((ret + "### SuplProfile ###\n") + this.curSuplProfile + "\n") + "### CdmaProfile ###\n") + this.cdmaProfile + "\n") + "### AgpsSetting ###\n") + this.agpsSetting + "\n") + "### CpSetting ###\n") + this.cpSetting + "\n") + "### UpSetting ###\n") + this.upSetting;
    }
}
