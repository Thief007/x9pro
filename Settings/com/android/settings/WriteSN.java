package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.NvRAMBackup.Stub;

public class WriteSN extends BroadcastReceiver {
    private static int FACTORY_TEST_FLAG_INDEX = 55;
    private static String INTENT_EXTRA_BARCODE = "extra_barcode";
    String TAG = "libing-WriteSN";
    private Context mContext = null;
    private String mExitBarcode = "";
    private Phone mPhone = null;

    private class BackupToBinThread extends Thread {
        private BackupToBinThread() {
        }

        public void run() {
            super.run();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            NvRAMBackup agent = Stub.asInterface(ServiceManager.getService("NvRAMBackup"));
            try {
                Log.e(WriteSN.this.TAG, "begin saveToBin");
                Log.e(WriteSN.this.TAG, "end saveToBin, return value: " + agent.saveToBin());
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private class WriteImeiThread extends Thread {
        private WriteImeiThread() {
        }

        public void run() {
            super.run();
            try {
                Thread.sleep(6000);
                Log.d("libing", "WriteImeiThread after sleep goto writeRandomImei");
                WriteSN.this.writeRandomImei();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void onReceive(Context context, Intent intent) {
        Log.d("libing", "WriteSn receive action " + intent.getAction());
        if (intent.getAction().equals("com.mediatek.factorymode.write.sn")) {
            this.mExitBarcode = intent.getStringExtra(INTENT_EXTRA_BARCODE);
            Log.d("libing", "onReceive mExitBarcode " + this.mExitBarcode);
            this.mContext = context;
            Log.e(this.TAG, "Start thread to write IMEI");
            new WriteImeiThread().start();
        }
    }

    public void writeRandomImei() {
        writeSn();
        new BackupToBinThread().start();
    }

    private void writeSn() {
        String oldBarcode = getSN();
        Log.d("libing", "oldBarcode " + oldBarcode);
        String newBarcode = oldBarcode;
        Log.d("libing", "mExitBarcode " + this.mExitBarcode);
        if (this.mExitBarcode != null && this.mExitBarcode.equals("P")) {
            newBarcode = setFactoryTestFlagByIndex(true, FACTORY_TEST_FLAG_INDEX);
            Log.d("libing", "newBarcode " + newBarcode);
        }
        String[] cmd = new String[]{"AT+EGMR=1,", ""};
        cmd[0] = "AT+EGMR=1,5,\"" + newBarcode + "\"";
        Log.d("libing", "cmd[0] " + cmd[0]);
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mPhone.invokeOemRilRequestStrings(cmd, null);
        Log.d("libing", "writeSn done");
    }

    private String getSN() {
        return SystemProperties.get("gsm.serial");
    }

    private String setFactoryTestFlagByIndex(boolean pass, int index) {
        Log.d("libing", "setFactoryTestFlagByIndex pass " + pass + " index " + index);
        String flag = pass ? "P" : "F";
        String oldSn = getSN();
        String newSn = oldSn;
        if (oldSn.length() < index + 1) {
            return oldSn;
        }
        newSn = oldSn.substring(0, index) + flag + oldSn.substring(index + 1);
        Log.d("libing", "setFactoryTestFlagByIndex oldSN " + oldSn);
        Log.d("libing", "setFactoryTestFlagByIndex newSN " + newSn);
        return newSn;
    }
}
