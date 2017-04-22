package com.android.settings.applock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.System;
import android.util.Log;

public class VanzoFPAppLockReceiver extends BroadcastReceiver {
    private String VANZO_APPLOCK_ACTION = "com.vanzo.vanzofprint.AppLockReceiver";

    public void onReceive(Context context, Intent intent) {
        if (System.getInt(context.getContentResolver(), "vz_fp_applock", 0) != 0 && intent != null) {
            DataBaseDao appListDao = new AppListDaoImpl(context);
            if (this.VANZO_APPLOCK_ACTION.equals(intent.getAction())) {
                int intentnum = intent.getIntExtra("applock_number", -1);
                Log.i("VanzoFPAppLockReceiver", "intentnum:" + intentnum);
                switch (intentnum) {
                    case 0:
                        String intentpkg = intent.getStringExtra("applock_pkgname");
                        String intentcls = intent.getStringExtra("applock_clsname");
                        if (intentpkg != null && intentcls != null && !"com.android.camera.SecureCameraActivity".equals(intentcls) && !"com.android.incallui.InCallActivity".equals(intentcls)) {
                            AppsInfoBean appsInfoBean = new AppsInfoBean();
                            appsInfoBean.setPackageName(intentpkg);
                            appsInfoBean.setPkgActivityName(intentcls);
                            try {
                                if (appListDao.selectOne(appsInfoBean) != null) {
                                    Log.i("VanzoFPAppLockReceiver", "applock");
                                    Intent intent1 = new Intent();
                                    intent1.putExtra("packageName", intentpkg);
                                    intent1.addFlags(65536);
                                    intent1.setClassName("com.android.settings.applock", "com.android.settings.applock.AppLockPasswordActivity");
                                    intent1.addFlags(268435456);
                                    context.startActivity(intent1);
                                    break;
                                }
                            } catch (Exception e) {
                                Log.i("VanzoFPAppLockReceiver", "e:" + e);
                                break;
                            }
                        }
                        return;
                        break;
                }
            }
        }
    }
}
