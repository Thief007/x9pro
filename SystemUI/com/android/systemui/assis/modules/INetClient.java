package com.android.systemui.assis.modules;

import com.android.systemui.assis.core.IBase;
import com.android.systemui.assis.core.ITaskListener;

public interface INetClient extends IBase {
    public static final int DOWNLOAD_ERROR_FAILED = 7;
    public static final int DOWNLOAD_ERROR_FATAL_ERROR = 6;
    public static final int DOWNLOAD_ERROR_FOLDER_NOT_EXIST = 5;
    public static final int DOWNLOAD_ERROR_ILLEGAL_FILENAME = 3;
    public static final int DOWNLOAD_ERROR_INVALID_URL = 1;
    public static final int DOWNLOAD_ERROR_NONE = 0;
    public static final int DOWNLOAD_ERROR_NOT_ENOUGH_SPACE = 2;
    public static final int DOWNLOAD_ERROR_SIZE_ERROR = 4;

    public static class DownloadRequest {
        public String fileName;
        public String localPath;
        public int notifyId;
        public long targetSize;
        public String url;
    }

    public enum NET_TYPE {
        NET_TYPE_INVALID,
        NET_TYPE_WIFI,
        NET_TYPE_GPRS
    }

    public static class Server {
        public String mAddress;
        public int mPort;
    }

    void downloadObject(DownloadRequest downloadRequest, ITaskListener iTaskListener);

    NET_TYPE getCurrentNetType();

    String getCurrentNetTypeName();

    long getCurrentTraffic();

    boolean isNetActivated();

    String postDataToServer(String str, String str2);

    String pullDataFromServer(String str);

    String tcpRequest(Server server, String str);
}
