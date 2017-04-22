package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Locale;

public class LaungageSettingReceiver extends BroadcastReceiver {
    String TAG = "LanguageSettingReceiver";

    public void onReceive(Context context, Intent intent) {
        Log.i(this.TAG, "receive change request");
        if ("com.vanzo.changelanguage".equals(intent.getAction())) {
            Log.i(this.TAG, "change language");
            String ext = intent.getExtras().get("language").toString();
            if (ext.equals("zh_CN")) {
                LanguagePicker.updateLocale(new Locale("zh", "CN"));
            } else if (ext.equals("zh_TW")) {
                LanguagePicker.updateLocale(new Locale("zh", "TW"));
            } else if (ext.equals("en_US")) {
                LanguagePicker.updateLocale(new Locale("en", "US"));
            } else if (ext.equals("vi_VN")) {
                LanguagePicker.updateLocale(new Locale("vi", "VN"));
            } else if (ext.equals("in_ID")) {
                LanguagePicker.updateLocale(new Locale("in", "ID"));
            } else if (ext.equals("hi_IN")) {
                LanguagePicker.updateLocale(new Locale("hi", "IN"));
            } else if (ext.equals("ar_EG")) {
                LanguagePicker.updateLocale(new Locale("ar", "EG"));
            } else if (ext.equals("fa_IR")) {
                LanguagePicker.updateLocale(new Locale("fa", "IR"));
            } else if (ext.equals("ur_PK")) {
                LanguagePicker.updateLocale(new Locale("ur", "PK"));
            } else if (ext.equals("my_ZG")) {
                LanguagePicker.updateLocale(new Locale("my", "ZG"));
            } else if (ext.equals("fr_FR")) {
                LanguagePicker.updateLocale(new Locale("fr", "FR"));
            } else if (ext.equals("bn_BD")) {
                LanguagePicker.updateLocale(new Locale("bn", "BD"));
            } else if (ext.equals("pt_BR")) {
                LanguagePicker.updateLocale(new Locale("pt", "BR"));
            } else if (ext.equals("es_ES")) {
                LanguagePicker.updateLocale(new Locale("es", "ES"));
            } else if (ext.equals("tr_TR")) {
                LanguagePicker.updateLocale(new Locale("tr", "TR"));
            } else if (ext.equals("ru_RU")) {
                LanguagePicker.updateLocale(new Locale("ru", "RU"));
            } else if (ext.equals("th_TH")) {
                LanguagePicker.updateLocale(new Locale("th", "TH"));
            }
        }
    }
}
