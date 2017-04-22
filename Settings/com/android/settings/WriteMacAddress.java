package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.settings.NvRAMAgent.Stub;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WriteMacAddress extends BroadcastReceiver {
    public static int FILE_ID = SystemProperties.getInt("ro.nid.wifi_mac_address", 30);
    public static boolean WRITE_MAC_SWITCH = SystemProperties.getBoolean("ro.init.write_mac_address", true);
    String TAG = "WriteMacAddress";
    private int offset = 5;

    public void onReceive(Context context, Intent intent) {
        Log.i(this.TAG, "WriteMacAddress switch value:" + WRITE_MAC_SWITCH);
        if (WRITE_MAC_SWITCH && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            NvRAMAgent agent = Stub.asInterface(ServiceManager.getService("NvRAMAgent"));
            byte[] readBuff = null;
            if (agent != null) {
                try {
                    readBuff = agent.readFile(FILE_ID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (readBuff != null) {
                    StringBuilder ss = new StringBuilder();
                    ss.append(readBuff[4]);
                    ss.append(readBuff[5]);
                    ss.append(readBuff[6]);
                    ss.append(readBuff[7]);
                    ss.append(readBuff[8]);
                    ss.append(readBuff[9]);
                    Log.i(this.TAG, "ss values is >>>" + ss.toString());
                    if (readBuff != null && (ss.toString().equals("000000") || ss.toString().equals(""))) {
                        Log.i(this.TAG, "write nvraming...");
                        String values = stringRandom();
                        byte[] macValues = values.getBytes();
                        readBuff[4] = (byte) 0;
                        for (int i = 0; i < 5; i++) {
                            readBuff[this.offset + i] = (byte) (Integer.parseInt(values.substring(i * 2, (i * 2) + 2)) & 255);
                        }
                        try {
                            int writeFile = agent.writeFile(FILE_ID, readBuff);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public String stringRandom() {
        List<String> list = Arrays.asList(new String("0123456789876543").split(""));
        Collections.shuffle(list);
        String out = new String();
        for (String s : list) {
            out = out + s;
        }
        String temp = out.substring(0, 2) + out.substring(3, 5) + out.substring(7, 9) + out.substring(10, 12) + out.substring(13, 15);
        Log.i(this.TAG, ">>>>>>>>>>>>>>>>" + temp);
        return temp;
    }
}
