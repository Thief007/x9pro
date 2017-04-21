package com.android.systemui.assis.main;

import android.content.Context;
import android.os.Environment;
import android.os.Process;
import com.android.systemui.assis.app.LOG;
import com.android.systemui.assis.app.MAIN.PATH;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

public class CrashHandler implements UncaughtExceptionHandler {
    private static CrashHandler myCrashHandler;
    private UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    private Context mContext;

    private CrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static synchronized CrashHandler getInstance() {
        synchronized (CrashHandler.class) {
            if (myCrashHandler == null) {
                myCrashHandler = new CrashHandler();
                CrashHandler crashHandler = myCrashHandler;
                return crashHandler;
            }
            crashHandler = myCrashHandler;
            return crashHandler;
        }
    }

    public void init(Context context) {
        this.mContext = context;
    }

    public void uncaughtException(Thread arg0, Throwable arg1) {
        saveLogFile(getErrorInfo(arg1));
        Process.killProcess(Process.myPid());
        this.defaultUEH.uncaughtException(arg0, arg1);
    }

    private String getErrorInfo(Throwable arg1) {
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        arg1.printStackTrace(pw);
        pw.close();
        return writer.toString();
    }

    public void saveLogFile(String content) {
        LOG.I("CrashHandler", "保存log文件");
        if (Environment.getExternalStorageState().equals("mounted")) {
            File log;
            String sdPath = Environment.getExternalStorageDirectory().getPath();
            String rootPath = new StringBuilder(String.valueOf(sdPath)).append("/").append(".csount/").toString();
            String logPath = new StringBuilder(String.valueOf(sdPath)).append("/").append(".csount/Log/").toString();
            try {
                File root = new File(rootPath);
                if (!root.exists()) {
                    root.mkdirs();
                }
                log = new File(logPath);
                if (!log.exists()) {
                    log.mkdirs();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                log = new File(new StringBuilder(String.valueOf(logPath)).append(PATH.LOG_FILE_NAME).toString());
                if (log.exists()) {
                    log.delete();
                }
                FileOutputStream fout = new FileOutputStream(new StringBuilder(String.valueOf(logPath)).append(PATH.LOG_FILE_NAME).toString());
                fout.write(content.getBytes());
                fout.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            return;
        }
        LOG.I("CKCrashHandler", "没有T卡，不写error log");
    }
}
