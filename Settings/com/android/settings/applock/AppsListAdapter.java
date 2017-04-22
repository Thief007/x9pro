package com.android.settings.applock;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import java.util.List;

public class AppsListAdapter extends BaseAdapter {
    private DataBaseDao appListDao;
    private Context context;
    private Handler handler;
    private boolean isEnable;
    private List<AppsInfoBean> list;
    private Bitmap lock;
    private LayoutInflater minflater;
    private Bitmap unlock;
    private View view;
    private ViewHolder viewHolder;

    static class ViewHolder {
        ImageView icon;
        ImageView lock;
        TextView text;

        ViewHolder() {
        }
    }

    public AppsListAdapter(Context context, List<AppsInfoBean> list, Handler handler) {
        this.context = context;
        this.list = list;
        this.handler = handler;
        this.minflater = LayoutInflater.from(context);
        this.appListDao = new AppListDaoImpl(context);
        this.lock = BitmapFactory.decodeResource(context.getResources(), R.drawable.lock);
        this.unlock = BitmapFactory.decodeResource(context.getResources(), R.drawable.unlock);
    }

    public void setEanble(boolean enable) {
        this.isEnable = enable;
    }

    public int getCount() {
        return this.list.size();
    }

    public Object getItem(int position) {
        return this.list.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        this.view = convertView;
        if (this.view == null) {
            this.view = this.minflater.inflate(R.layout.list_item, null);
            this.viewHolder = new ViewHolder();
            this.viewHolder.icon = (ImageView) this.view.findViewById(R.id.imageView_icon);
            this.viewHolder.text = (TextView) this.view.findViewById(R.id.textView_appName);
            this.viewHolder.lock = (ImageView) this.view.findViewById(R.id.imageView_lock);
            this.view.setTag(this.viewHolder);
        } else {
            this.viewHolder = (ViewHolder) this.view.getTag();
        }
        PackageManager pm = this.context.getPackageManager();
        AppsInfoBean appsInfoBean = (AppsInfoBean) this.list.get(position);
        this.viewHolder.icon.setImageDrawable(appsInfoBean.getAppIcon());
        if ("com.android.gallery3d".equals(appsInfoBean.getPackageName())) {
            this.viewHolder.text.setText(appsInfoBean.getAppLabel() + "&" + this.context.getResources().getString(R.string.applock_gallery));
        } else if ("com.android.dialer".equals(appsInfoBean.getPackageName())) {
            this.viewHolder.text.setText(appsInfoBean.getAppLabel() + "&" + this.context.getResources().getString(R.string.applock_contact));
        } else {
            this.viewHolder.text.setText(appsInfoBean.getAppLabel());
        }
        this.viewHolder.text.setTextColor(this.isEnable ? -16777216 : -7829368);
        if (this.appListDao.selectAppLock(appsInfoBean) != null) {
            this.viewHolder.lock.setImageBitmap(this.lock);
        } else {
            this.viewHolder.lock.setImageBitmap(this.unlock);
        }
        return this.view;
    }
}
