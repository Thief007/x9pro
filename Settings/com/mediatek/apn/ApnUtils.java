package com.mediatek.apn;

public class ApnUtils {
    public static final String[] PROJECTION = new String[]{"_id", "name", "apn", "proxy", "port", "user", "server", "password", "mmsc", "mcc", "mnc", "numeric", "mmsproxy", "mmsport", "authtype", "type", "sourcetype", "omacpid", "napid", "proxyid"};

    public static String checkNotSet(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        return value;
    }
}
