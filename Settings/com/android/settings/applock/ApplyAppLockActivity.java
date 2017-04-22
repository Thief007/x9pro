package com.android.settings.applock;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.System;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.List;

public class ApplyAppLockActivity extends BaseActivity implements OnItemClickListener, OnClickListener {
    private ImageView Imgopen;
    private Button appBtn;
    private DataBaseDao appListDao;
    private boolean flagSys = false;
    private Handler handler;
    private LinearLayout layoutback;
    private List<AppsInfoBean> list;
    private AppsListAdapter listAdapter;
    private ListView listView_apps;
    private boolean lockState = false;
    private ProgressBar mLoadin;

    class C02871 extends Handler {
        C02871() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1001:
                    ApplyAppLockActivity.this.loadOk();
                    ApplyAppLockActivity.this.mLoadin.setVisibility(8);
                    return;
                default:
                    return;
            }
        }
    }

    class LoadApps extends Thread {
        LoadApps() {
        }

        public void run() {
            super.run();
            PackageManager packageManager = ApplyAppLockActivity.this.getPackageManager();
            Intent mainIntent = new Intent("android.intent.action.MAIN", null);
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            for (ResolveInfo info : packageManager.queryIntentActivities(mainIntent, 0)) {
                AppsInfoBean appsInfoBean = new AppsInfoBean();
                if (!(info == null || info.activityInfo == null || "com.vanzo.vanzofprint".equals(info.activityInfo.packageName) || "com.android.settings".equals(info.activityInfo.packageName) || "com.android.cleanprocesstool".equals(info.activityInfo.packageName) || "com.airui.buildinlauncher".equals(info.activityInfo.packageName) || "com.cooee.unilauncher".equals(info.activityInfo.packageName) || "com.android.dialer.PeopleActivity".equals(info.activityInfo.name) || "com.android.providers.downloads.ui.DownloadList".equals(info.activityInfo.name) || "com.vz.gallery3d.VzGalleryActivity".equals(info.activityInfo.name) || "com.airui.launcher".equals(info.activityInfo.packageName))) {
                    appsInfoBean.setAppIcon(info.loadIcon(packageManager));
                    appsInfoBean.setAppLabel(info.loadLabel(packageManager));
                    appsInfoBean.setPackageName(info.activityInfo.packageName);
                    appsInfoBean.setPkgActivityName(info.activityInfo.name);
                    ApplyAppLockActivity.this.list.add(appsInfoBean);
                }
            }
            ApplyAppLockActivity.this.handler.sendEmptyMessage(1001);
        }
    }

    public void findViews() {
        setContentView(R.layout.applock_main_activity);
        this.listView_apps = (ListView) findViewById(R.id.listView_apps);
        this.Imgopen = (ImageView) findViewById(R.id.img_open);
        this.appBtn = (Button) findViewById(R.id.tv_Start);
        this.layoutback = (LinearLayout) findViewById(R.id.ma_app_lock_back);
        this.mLoadin = (ProgressBar) findViewById(R.id.load_in);
    }

    public void setListeners() {
        this.listView_apps.setOnItemClickListener(this);
        this.appBtn.setOnClickListener(this);
        this.layoutback.setOnClickListener(this);
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        AppsInfoBean appsInfoBean = (AppsInfoBean) this.list.get(position);
        ImageView imageViewlock = (ImageView) view.findViewById(R.id.imageView_lock);
        if (this.appListDao.selectOne(appsInfoBean) != null) {
            this.appListDao.delete(appsInfoBean.getPkgActivityName());
            imageViewlock.setImageResource(R.drawable.unlock);
            return;
        }
        this.appListDao.insert(appsInfoBean);
        imageViewlock.setImageResource(R.drawable.lock);
    }

    protected void init() {
        super.init();
        this.appListDao = new AppListDaoImpl(getApplicationContext());
        this.list = new ArrayList();
        this.handler = new C02871();
        this.mLoadin.setVisibility(0);
        new LoadApps().start();
        this.lockState = getSharedPreferences("settinglock", 0).getBoolean("settinglock", false);
        if (this.lockState) {
            this.Imgopen.setImageDrawable(getResources().getDrawable(R.drawable.on));
        } else {
            this.Imgopen.setImageDrawable(getResources().getDrawable(R.drawable.off));
        }
    }

    protected void loadOk() {
        this.listAdapter = new AppsListAdapter(this, this.list, this.handler);
        this.listView_apps.setAdapter(this.listAdapter);
        this.listView_apps.setVisibility(0);
        if (this.listView_apps != null && this.listAdapter != null) {
            this.listView_apps.setClickable(this.lockState);
            this.listView_apps.setEnabled(this.lockState);
            this.listAdapter.setEanble(this.lockState);
            this.listAdapter.notifyDataSetChanged();
        }
    }

    protected void onPause() {
        super.onPause();
        getSharedPreferences("settinglock", 0).edit().putBoolean("settinglock", this.lockState).commit();
        finish();
    }

    public void onClick(View arg0) {
        int i = 1;
        if (arg0 == this.appBtn) {
            int i2;
            boolean z;
            if (this.lockState) {
                i2 = 0;
            } else {
                i2 = 1;
            }
            if (i2 != 0) {
                this.Imgopen.setImageResource(R.drawable.on);
                Intent intent1 = new Intent();
                intent1.setClass(getApplicationContext(), FpService.class);
                startService(intent1);
            } else {
                this.Imgopen.setImageResource(R.drawable.off);
            }
            if (this.lockState) {
                z = false;
            } else {
                z = true;
            }
            this.lockState = z;
            ContentResolver contentResolver = getContentResolver();
            String str = "vz_fp_applock";
            if (!this.lockState) {
                i = 0;
            }
            System.putInt(contentResolver, str, i);
            if (this.listView_apps != null && this.listAdapter != null) {
                this.listView_apps.setClickable(this.lockState);
                this.listView_apps.setEnabled(this.lockState);
                this.listAdapter.setEanble(this.lockState);
                this.listAdapter.notifyDataSetChanged();
            }
        } else if (arg0 == this.layoutback) {
            finish();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 16908332:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
