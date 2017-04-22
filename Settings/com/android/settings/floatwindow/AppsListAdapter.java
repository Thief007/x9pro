package com.android.settings.floatwindow;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
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
    private String defaultApp = null;
    private List<AppInfo> mAppsInfosList = null;
    private Context mContext = null;
    private LayoutInflater mInflater = null;

    class ViewHolder {
        TextView appNameView;
        ImageView iconView;
        ImageView stateView;

        ViewHolder() {
        }
    }

    public AppsListAdapter(Context context) {
        this.mContext = context;
    }

    public AppsListAdapter(Context context, List<AppInfo> list, LayoutInflater inflater) {
        this.mContext = context;
        this.mAppsInfosList = list;
        this.mInflater = inflater;
    }

    public void setDefaultApp(String app) {
        this.defaultApp = app;
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
        Object obj = null;
        Object activityName = null;
        if (this.defaultApp != null) {
            obj = this.defaultApp.split("&")[0];
            activityName = this.defaultApp.split("&")[1];
        }
        if (((AppInfo) this.mAppsInfosList.get(position)).getPackageName().equals(obj) && ((AppInfo) this.mAppsInfosList.get(position)).getMainActivityName().equals(r0)) {
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
            appsInfosList.add(appInfo);
        }
        return appsInfosList;
    }
}
