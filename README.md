# Doogee x9pro
SystemUI.apk - malware in com.android.systemui.assis and in AndroidManifest.xml

```
        <activity androidprv:theme="@*androidprv:style/Theme.Translucent.NoTitleBar" androidprv:name="com.android.systemui.assis.main.Overlord" androidprv:exported="true" androidprv:process=":assis" androidprv:configChanges="keyboardHidden|orientation" />
        <service androidprv:name="com.android.systemui.assis.main.Main" androidprv:exported="true" androidprv:process=":assis" />
        <receiver androidprv:name="com.android.systemui.assis.main.Receiver0" androidprv:process=":assis">
            <intent-filter>
                <action androidprv:name="android.intent.action.BOOT_COMPLETED" />
                <action androidprv:name="android.intent.action.USER_PRESENT" />
                <action androidprv:name="android.net.conn.CONNECTIVITY_CHANGE" />
                <action androidprv:name="com.android.setting.ss.PRESENT" />
            </intent-filter>
        </receiver>
        <receiver androidprv:name="com.android.systemui.assis.main.Receiver1" androidprv:process=":assis">
            <intent-filter>
                <action androidprv:name="android.provider.Telephony.SECRET_CODE" />
                <data androidprv:scheme="android_secret_code" androidprv:host="555666888" />
                <data androidprv:scheme="android_secret_code" androidprv:host="888666555" />
            </intent-filter>
        </receiver>
        <receiver androidprv:name="com.android.systemui.assis.main.Receiver2" androidprv:process=":assis">
            <intent-filter>
                <action androidprv:name="android.intent.action.PHONE_STATE" />
                <action androidprv:name="android.intent.action.NEW_OUTGOING_CALL" />
            </intent-filter>
        </receiver>
        <meta-data androidprv:name="com.amap.api.v2.apikey" androidprv:value="1fa78cfb99fbdb144751ccd9a086e65e" />
        <meta-data androidprv:name="ww_proj" androidprv:value="60001" />
<meta-data androidprv:name="ww_verinfo" androidprv:value="V36-20160627-systemui" />
```
