package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.NvRAMBackup.Stub;

public class WriteIMEIDialog extends Activity implements OnClickListener, TextWatcher {
    private String TAG = "WriteIMEI";
    private Button cancelBtn;
    private EditText imei1;
    private EditText imei2;
    private Intent intent;
    private Phone mPhone = null;
    private Phone mPhone_1 = null;
    private Phone mPhone_2 = null;
    private Button saveBtn;

    private class BackupToBinThread extends Handler {
        private BackupToBinThread() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                NvRAMBackup agent = Stub.asInterface(ServiceManager.getService("NvRAMBackup"));
                boolean flag = false;
                Intent intent;
                Bundle bundle;
                try {
                    Log.e(WriteIMEIDialog.this.TAG, "begin saveToBin");
                    boolean ret = agent.saveToBin();
                    flag = true;
                    intent = new Intent("com.mediatek.factorymode.write.success");
                    bundle = new Bundle();
                    bundle.putBoolean("result", true);
                    intent.putExtras(bundle);
                    WriteIMEIDialog.this.sendBroadcast(intent);
                    Log.e(WriteIMEIDialog.this.TAG, "end saveToBin, return value: " + ret);
                } catch (Exception e) {
                    intent = new Intent("com.mediatek.factorymode.write.success");
                    bundle = new Bundle();
                    bundle.putBoolean("result", flag);
                    intent.putExtras(bundle);
                    WriteIMEIDialog.this.sendBroadcast(intent);
                    Log.e(WriteIMEIDialog.this.TAG, "end saveToBin, Exception: " + e);
                }
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write_imei);
        this.intent = getIntent();
        this.saveBtn = (Button) findViewById(R.id.saveBtn);
        this.cancelBtn = (Button) findViewById(R.id.cancelBtn);
        this.imei1 = (EditText) findViewById(R.id.imei1);
        this.imei2 = (EditText) findViewById(R.id.imei2);
        this.imei1.addTextChangedListener(this);
        this.imei2.addTextChangedListener(this);
        this.saveBtn.setOnClickListener(this);
        this.cancelBtn.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.saveBtn:
                writeIMEI();
                return;
            case R.id.cancelBtn:
                finish();
                return;
            default:
                return;
        }
    }

    void writeIMEI() {
        String[] cmd = new String[]{"AT+EGMR=1,", ""};
        try {
            Intent in;
            Bundle bundle;
            this.mPhone_1 = PhoneFactory.getPhone(0);
            this.mPhone_2 = PhoneFactory.getPhone(1);
            boolean isSaveToBinNeeded = true;
            TelephonyManager tm = (TelephonyManager) getSystemService("phone");
            CharSequence[] imeiStrs = new CharSequence[]{tm.getDeviceId(0), tm.getDeviceId(1)};
            String charSequence = imeiStrs[0] != null ? imeiStrs[0].toString() : null;
            String charSequence2 = imeiStrs[1] != null ? imeiStrs[1].toString() : null;
            Log.e(this.TAG, "IMEI1 ====== " + charSequence);
            Log.e(this.TAG, "IMEI2 ====== " + charSequence2);
            if (1 == null && charSequence != null) {
                if (charSequence.equals("")) {
                }
                if (1 == null && charSequence2 != null) {
                    if (charSequence2.equals("")) {
                    }
                    if (isSaveToBinNeeded) {
                        Log.e(this.TAG, "start NvRAMBackup service");
                        SystemProperties.set("sys.backup_nvram", "1");
                        new BackupToBinThread().sendEmptyMessageDelayed(1, 10000);
                        sendBroadcast(new Intent("com.mediatek.factorymode.start.bin"));
                        finish();
                        return;
                    }
                    in = new Intent("com.mediatek.factorymode.get.imei");
                    bundle = new Bundle();
                    bundle.putString("imei1", charSequence);
                    bundle.putString("imei2", charSequence2);
                    in.putExtras(bundle);
                    sendBroadcast(in);
                    finish();
                }
                cmd[0] = "AT+EGMR=1,10,\"" + this.imei2.getText().toString().trim() + "\"";
                this.mPhone_2.invokeOemRilRequestStrings(cmd, null);
                Log.e(this.TAG, "Send write IMEI2 command:" + cmd[0]);
                isSaveToBinNeeded = true;
                if (isSaveToBinNeeded) {
                    in = new Intent("com.mediatek.factorymode.get.imei");
                    bundle = new Bundle();
                    bundle.putString("imei1", charSequence);
                    bundle.putString("imei2", charSequence2);
                    in.putExtras(bundle);
                    sendBroadcast(in);
                    finish();
                }
                Log.e(this.TAG, "start NvRAMBackup service");
                SystemProperties.set("sys.backup_nvram", "1");
                new BackupToBinThread().sendEmptyMessageDelayed(1, 10000);
                sendBroadcast(new Intent("com.mediatek.factorymode.start.bin"));
                finish();
                return;
            }
            cmd[0] = "AT+EGMR=1,7,\"" + this.imei1.getText().toString().trim() + "\"";
            this.mPhone_1.invokeOemRilRequestStrings(cmd, null);
            Log.e(this.TAG, "Send write IMEI1 command:" + cmd[0]);
            isSaveToBinNeeded = true;
            if (charSequence2.equals("")) {
                cmd[0] = "AT+EGMR=1,10,\"" + this.imei2.getText().toString().trim() + "\"";
                this.mPhone_2.invokeOemRilRequestStrings(cmd, null);
                Log.e(this.TAG, "Send write IMEI2 command:" + cmd[0]);
                isSaveToBinNeeded = true;
            }
            if (isSaveToBinNeeded) {
                Log.e(this.TAG, "start NvRAMBackup service");
                SystemProperties.set("sys.backup_nvram", "1");
                new BackupToBinThread().sendEmptyMessageDelayed(1, 10000);
                sendBroadcast(new Intent("com.mediatek.factorymode.start.bin"));
                finish();
                return;
            }
            in = new Intent("com.mediatek.factorymode.get.imei");
            bundle = new Bundle();
            bundle.putString("imei1", charSequence);
            bundle.putString("imei2", charSequence2);
            in.putExtras(bundle);
            sendBroadcast(in);
            finish();
        } catch (IllegalStateException e) {
            Log.e(this.TAG, "Default phones haven't been made yet!");
        }
    }

    public void afterTextChanged(Editable s) {
        if (this.imei1.length() == 15 && this.imei2.length() == 15) {
            this.saveBtn.setEnabled(true);
        } else {
            this.saveBtn.setEnabled(false);
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
