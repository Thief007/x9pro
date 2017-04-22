package com.p003v.smartwake;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.p003v.common.AppInfo;
import java.util.ArrayList;
import java.util.List;

public class AppsListAdapter extends BaseAdapter {
    private List<AppInfo> mAppsInfosList = null;
    private Context mContext = null;
    private int mGesture = 0;
    private LayoutInflater mInflater = null;

    class ViewHolder {
        TextView appNameView;
        ImageView iconView;
        ImageView stateView;

        ViewHolder() {
        }
    }

    public AppsListAdapter(Context context, int gesture) {
        this.mContext = context;
        this.mGesture = gesture;
    }

    public AppsListAdapter(Context context, List<AppInfo> list, LayoutInflater inflater, int gesture) {
        this.mContext = context;
        this.mAppsInfosList = list;
        this.mInflater = inflater;
        this.mGesture = gesture;
    }

    private boolean isApkExist(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 1);
            Log.d("tanglei", "isApkExist = true");
            return true;
        } catch (NameNotFoundException e) {
            Log.w("tanglei", "Apk not found " + packageName);
            return false;
        }
    }

    public int getCount() {
        return this.mAppsInfosList.size();
    }

    public Object getItem(int position) {
        return this.mAppsInfosList.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.v_apps_list_item, null);
            viewHolder = new ViewHolder();
            viewHolder.iconView = (ImageView) convertView.findViewById(R.id.appicon);
            viewHolder.appNameView = (TextView) convertView.findViewById(R.id.appname);
            viewHolder.stateView = (ImageView) convertView.findViewById(R.id.state);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.iconView.setBackgroundDrawable((Drawable) ((AppInfo) this.mAppsInfosList.get(position)).getAppIcon());
        viewHolder.appNameView.setText(((AppInfo) this.mAppsInfosList.get(position)).getAppName());
        String packageName = null;
        Object activityName = null;
        if (this.mGesture == 0) {
            packageName = SystemProperties.get("persist.sys.smartwake_c_name", "com.mediatek.camera&com.android.camera.CameraLauncher").split("&")[0];
            activityName = SystemProperties.get("persist.sys.smartwake_c_name", "com.mediatek.camera&com.android.camera.CameraLauncher").split("&")[1];
        } else if (this.mGesture == 1) {
            packageName = SystemProperties.get("persist.sys.smartwake_e_name", "com.android.browser&com.android.browser.BrowserActivity").split("&")[0];
            activityName = SystemProperties.get("persist.sys.smartwake_e_name", "com.android.browser&com.android.browser.BrowserActivity").split("&")[1];
        } else if (this.mGesture == 2) {
            packageName = SystemProperties.get("persist.sys.smartwake_w_name", "com.mediatek.filemanager&com.mediatek.filemanager.FileManagerOperationActivity").split("&")[0];
            activityName = SystemProperties.get("persist.sys.smartwake_w_name", "com.mediatek.filemanager&com.mediatek.filemanager.FileManagerOperationActivity").split("&")[1];
        } else if (this.mGesture == 3) {
            if (isApkExist(this.mContext, "com.android.music")) {
                packageName = SystemProperties.get("persist.sys.smartwake_m_name", "com.android.music&com.android.music.MusicBrowserActivity").split("&")[0];
                activityName = SystemProperties.get("persist.sys.smartwake_m_name", "com.android.music&com.android.music.MusicBrowserActivity").split("&")[1];
            } else {
                packageName = SystemProperties.get("persist.sys.smartwake_m_name", "com.v5music&com.v5music.view.activity.SplashActivity").split("&")[0];
                activityName = SystemProperties.get("persist.sys.smartwake_m_name", "com.v5music&com.v5music.view.activity.SplashActivity").split("&")[1];
            }
        } else if (this.mGesture == 4) {
            packageName = SystemProperties.get("persist.sys.smartwake_o_name", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[0];
            activityName = SystemProperties.get("persist.sys.smartwake_o_name", "com.android.dialer&com.android.dialer.DialtactsActivity").split("&")[1];
        }
        if (((AppInfo) this.mAppsInfosList.get(position)).getPackageName().equals(packageName) && ((AppInfo) this.mAppsInfosList.get(position)).getMainActivityName().equals(r0)) {
            viewHolder.stateView.setVisibility(0);
        } else {
            viewHolder.stateView.setVisibility(8);
        }
        return convertView;
    }

    public void updateListView(List<AppInfo> list) {
        this.mAppsInfosList = list;
        notifyDataSetChanged();
    }

    public List<AppInfo> getDownLoadAppInfoList() {
        List<AppInfo> appsInfosList = new ArrayList(this.mContext.getPackageManager().getInstalledPackages(0).size());
        Intent mainIntent = new Intent("android.intent.action.MAIN", null);
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> mAllIntentApps = this.mContext.getPackageManager().queryIntentActivities(mainIntent, 0);
        for (int i = 0; i < mAllIntentApps.size(); i++) {
            AppInfo appInfo = new AppInfo();
            appInfo.setAppIcon(((ResolveInfo) mAllIntentApps.get(i)).loadIcon(this.mContext.getPackageManager()));
            appInfo.setAppName(((ResolveInfo) mAllIntentApps.get(i)).loadLabel(this.mContext.getPackageManager()).toString());
            appInfo.setPackageName(((ResolveInfo) mAllIntentApps.get(i)).activityInfo.packageName);
            appInfo.setMainActivityName(((ResolveInfo) mAllIntentApps.get(i)).activityInfo.name);
            String info = ((ResolveInfo) mAllIntentApps.get(i)).activityInfo.packageName + ((ResolveInfo) mAllIntentApps.get(i)).activityInfo.name;
            if (info.length() < 90) {
                appsInfosList.add(appInfo);
            } else {
                Log.d("libing", "getDownLoadAppInfoList info " + info);
            }
        }
        return appsInfosList;
    }
}
