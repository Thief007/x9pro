package com.android.settings.sim;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings.Global;
import android.support.v4.app.NotificationCompat.Builder;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Settings.SimSettingsActivity;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.List;

public class SimSelectNotification extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (!UtilsExt.shouldDisableForAutoSanity()) {
            List<SubscriptionInfo> subs = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
            int detectedType = intent.getIntExtra("simDetectStatus", 0);
            Log.d("SimSelectNotification", "sub info update, subs = " + subs + ", type = " + detectedType);
            if (detectedType != 4) {
                if (subs != null && subs.size() > 1) {
                    CdmaUtils.checkCdmaSimStatus(context, subs.size());
                }
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
                SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                int numSlots = telephonyManager.getSimCount();
                boolean isInProvisioning = Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 0;
                if (numSlots >= 2 && !isInProvisioning) {
                    boolean z;
                    cancelNotification(context);
                    String simStatus = intent.getStringExtra("ss");
                    if ("ABSENT".equals(simStatus)) {
                        z = true;
                    } else {
                        z = "LOADED".equals(simStatus);
                    }
                    if (z) {
                        Log.d("SimSelectNotification", "simstatus = " + simStatus);
                    } else {
                        Log.d("SimSelectNotification", "sim state is not Absent or Loaded");
                    }
                    for (int i = 0; i < numSlots; i++) {
                        int state = telephonyManager.getSimState(i);
                        if (!(state == 1 || state == 5 || state == 0)) {
                            Log.d("SimSelectNotification", "All sims not in valid state yet");
                        }
                    }
                    List<SubscriptionInfo> sil = subscriptionManager.getActiveSubscriptionInfoList();
                    if (sil == null || sil.size() < 1) {
                        Log.d("SimSelectNotification", "Subscription list is empty");
                        return;
                    }
                    subscriptionManager.clearDefaultsForInactiveSubIds();
                    boolean dataSelected = SubscriptionManager.isUsableSubIdValue(SubscriptionManager.getDefaultDataSubId());
                    boolean smsSelected = SubscriptionManager.isUsableSubIdValue(SubscriptionManager.getDefaultSmsSubId());
                    if (dataSelected && smsSelected) {
                        Log.d("SimSelectNotification", "Data & SMS default sims are selected. No notification");
                        return;
                    }
                    createNotification(context);
                    if (isSimDialogNeeded(context)) {
                        Intent newIntent;
                        if (sil.size() == 1) {
                            newIntent = new Intent(context, SimDialogActivity.class);
                            newIntent.addFlags(268435456);
                            newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 3);
                            newIntent.putExtra(SimDialogActivity.PREFERRED_SIM, ((SubscriptionInfo) sil.get(0)).getSimSlotIndex());
                            context.startActivity(newIntent);
                        } else if (!dataSelected) {
                            newIntent = new Intent(context, SimDialogActivity.class);
                            newIntent.addFlags(268435456);
                            newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 0);
                            context.startActivity(newIntent);
                        }
                    }
                }
            }
        }
    }

    private void createNotification(Context context) {
        Resources resources = context.getResources();
        Builder builder = new Builder(context).setSmallIcon(R.drawable.ic_sim_card_alert_white_48dp).setColor(context.getColor(R.color.sim_noitification)).setContentTitle(resources.getString(R.string.sim_notification_title)).setContentText(resources.getString(R.string.sim_notification_summary));
        customizeSimDisplay(context, builder);
        Intent resultIntent = new Intent(context, SimSettingsActivity.class);
        resultIntent.addFlags(268435456);
        builder.setContentIntent(PendingIntent.getActivity(context, 0, resultIntent, 268435456));
        ((NotificationManager) context.getSystemService("notification")).notify(1, builder.build());
    }

    public static void cancelNotification(Context context) {
        ((NotificationManager) context.getSystemService("notification")).cancel(1);
    }

    private void customizeSimDisplay(Context context, Builder builder) {
        Resources resources = context.getResources();
        String title = resources.getString(R.string.sim_notification_title);
        String text = resources.getString(R.string.sim_notification_summary);
        ISettingsMiscExt miscExt = UtilsExt.getMiscPlugin(context);
        title = miscExt.customizeSimDisplayString(title, -1);
        text = miscExt.customizeSimDisplayString(text, -1);
        builder.setContentTitle(title);
        builder.setContentText(text);
    }

    private boolean isSimDialogNeeded(Context context) {
        if (FeatureOption.MTK_CT6M_SUPPORT) {
            return false;
        }
        return UtilsExt.getSimManagmentExtPlugin(context).isSimDialogNeeded();
    }
}
