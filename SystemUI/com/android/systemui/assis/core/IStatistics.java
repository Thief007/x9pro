package com.android.systemui.assis.core;

import android.content.Context;
import android.os.Message;

public interface IStatistics {
    public static final String INSTANCE_CONFIG = "instance_config";
    public static final String INSTANCE_DATABASE = "instance_database";
    public static final String INSTANCE_DIALOG = "instance_dialog";
    public static final String INSTANCE_NETCLIENT = "instance_netclient";
    public static final String INSTANCE_NOTIFIER = "instance_notifier";
    public static final String INSTANCE_PACKCORE = "instance_packcore";
    public static final String INSTANCE_SHELLCONFIG = "instance_preferences";

    public static class ShellInfo {
        public String mAppletId;
        public int mCustId;
        public int mVersionCode;
        public String mVersionInfo;
    }

    public static class UdpServer {
        public String mIP;
        public int mMaxPort;
        public int mMinPort;
    }

    Context getAppletContext();

    String getRegServerIp();

    int getRegServerPort();

    ShellInfo getShellInfo();

    Object getShellService(String str);

    UdpServer getUdpServer();

    void sendEmptyMessage(int i);

    void sendMessage(Message message);

    void sendMessageDelayed(Message message, long j);

    void setRegServer(String str, int i);

    void setUdpServer(UdpServer udpServer);

    void submitTask(Runnable runnable);
}
