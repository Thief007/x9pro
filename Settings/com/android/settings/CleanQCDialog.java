package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.NvRAMBackup.Stub;

public class CleanQCDialog extends Activity implements OnClickListener {
    private static int FACTORY_TEST_FLAG_INDEX = 55;
    private static String INTENT_EXTRA_BARCODE = "extra_barcode";
    private String TAG = "WriteIMEI";
    private Context mContext = null;
    private String mExitBarcode = "";
    private Phone mPhone = null;
    private Button mResetBtn;

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
                Log.e(CleanQCDialog.this.TAG, "begin saveToBin");
                Log.e(CleanQCDialog.this.TAG, "end saveToBin, return value: " + agent.saveToBin());
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clean_qc);
        this.mResetBtn = (Button) findViewById(R.id.resetBtn);
        this.mResetBtn.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.resetBtn:
                resetQCFlag();
                finish();
                return;
            default:
                return;
        }
    }

    public void resetQCFlag() {
        resetQCSn();
        new BackupToBinThread().start();
    }

    private void resetQCSn() {
        String oldBarcode = getSN();
        Log.d("libing", "resetQCSn oldBarcode " + oldBarcode);
        Log.d("libing", "resetQCSn newBarcode " + oldBarcode);
        String newBarcode = setFactoryTestFlagByIndex(false, FACTORY_TEST_FLAG_INDEX);
        Log.d("libing", "resetQCSn newBarcode after setFactoryTestFlagByIndex " + newBarcode);
        String[] cmd = new String[]{"AT+EGMR=1,", ""};
        cmd[0] = "AT+EGMR=1,5,\"" + newBarcode + "\"";
        Log.d("libing", "cmd[0] " + cmd[0]);
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mPhone.invokeOemRilRequestStrings(cmd, null);
        Log.d("libing", "resetQCSn done");
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
