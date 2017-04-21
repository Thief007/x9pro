package com.mediatek.systemui.floatpanel;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.List;

public class FloatAppAdapter extends BaseAdapter {
    private List<FloatAppItem> mActivitiesList;
    private Context mContext;
    private final int mFloatContainer;
    private FloatModel mFloatModel;
    private IconResizer mIconResizer;
    private LayoutInflater mInflater = ((LayoutInflater) this.mContext.getSystemService("layout_inflater"));

    public class IconResizer {
        private Canvas mCanvas = new Canvas();
        private Context mContext;
        private int mIconHeight = -1;
        private int mIconWidth = -1;
        private final Rect mOldBounds = new Rect();

        public IconResizer(Context context) {
            this.mContext = context;
            this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
            this.mIconWidth = (int) this.mContext.getResources().getDimension(17104896);
            this.mIconHeight = this.mIconWidth;
        }

        public Drawable createIconThumbnail(Drawable icon) {
            int width = this.mIconWidth;
            int height = this.mIconHeight;
            int iconWidth = icon.getIntrinsicWidth();
            int iconHeight = icon.getIntrinsicHeight();
            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            }
            Log.d("FloatAppAdapter", "createIconThumbnail: iconWidth = " + iconWidth + ", iconHeight = " + iconHeight + ", mIconWidth = " + this.mIconWidth + ", mIconHeight = " + this.mIconHeight + ", icon = " + icon + ",icon.getOpacity() = " + icon.getOpacity());
            if (width <= 0 || height <= 0) {
                return icon;
            }
            Bitmap thumb;
            Canvas canvas;
            int x;
            int y;
            if (width < iconWidth || height < iconHeight) {
                float ratio = ((float) iconWidth) / ((float) iconHeight);
                if (iconWidth > iconHeight) {
                    height = (int) (((float) width) / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (((float) height) * ratio);
                }
                thumb = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, icon.getOpacity() != -1 ? Config.ARGB_8888 : Config.RGB_565);
                canvas = this.mCanvas;
                canvas.setBitmap(thumb);
                this.mOldBounds.set(icon.getBounds());
                x = (this.mIconWidth - width) / 2;
                y = (this.mIconHeight - height) / 2;
                icon.setBounds(x, y, x + width, y + height);
                icon.draw(canvas);
                icon.setBounds(this.mOldBounds);
                icon = new BitmapDrawable(this.mContext.getResources(), thumb);
                canvas.setBitmap(null);
                return icon;
            } else if (iconWidth >= width || iconHeight >= height) {
                return icon;
            } else {
                thumb = Bitmap.createBitmap(this.mIconWidth, this.mIconHeight, Config.ARGB_8888);
                canvas = this.mCanvas;
                canvas.setBitmap(thumb);
                this.mOldBounds.set(icon.getBounds());
                x = (width - iconWidth) / 2;
                y = (height - iconHeight) / 2;
                icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                icon.draw(canvas);
                icon.setBounds(this.mOldBounds);
                icon = new BitmapDrawable(this.mContext.getResources(), thumb);
                canvas.setBitmap(null);
                return icon;
            }
        }
    }

    public FloatAppAdapter(Context context, FloatModel model, int floatContainer) {
        List floatApps;
        this.mContext = context;
        this.mFloatModel = model;
        this.mIconResizer = new IconResizer(context);
        this.mFloatContainer = floatContainer;
        if (floatContainer == 1) {
            floatApps = this.mFloatModel.getFloatApps();
        } else {
            floatApps = this.mFloatModel.getEditApps();
        }
        this.mActivitiesList = floatApps;
        Log.d("FloatAppAdapter", "FloatAppAdapter construct: floatContainer = " + floatContainer + "mActivitiesList = " + this.mActivitiesList);
    }

    public int getCount() {
        return this.mActivitiesList != null ? this.mActivitiesList.size() : 0;
    }

    public Object getItem(int position) {
        return this.mActivitiesList.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.float_panel_item, parent, false);
        }
        if (view != null) {
            bindView(view, position);
        }
        return view;
    }

    private void bindView(View view, int position) {
        FloatAppItem item = (FloatAppItem) this.mActivitiesList.get(position);
        TextView appName = (TextView) view.findViewById(R.id.app_name);
        appName.setText(item.label);
        if (item.icon == null) {
            item.icon = this.mIconResizer.createIconThumbnail(item.resolveInfo.loadIcon(this.mContext.getPackageManager()));
        }
        appName.setCompoundDrawablesWithIntrinsicBounds(null, item.icon, null, null);
        if (item.visible) {
            view.setVisibility(0);
        } else {
            view.setVisibility(4);
        }
        view.setTag(item);
    }

    public void addItem(FloatAppItem item, int position) {
        Log.d("FloatAppAdapter", "addItem: position = " + position + ", item = " + item + ", size = " + this.mActivitiesList.size());
        if (position == -1) {
            position = this.mActivitiesList.size();
            this.mActivitiesList.add(item);
        } else {
            this.mActivitiesList.add(position, item);
        }
        updateItemInfoBetween(position, this.mActivitiesList.size());
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        Log.d("FloatAppAdapter", "removeItem: position = " + position + ", item = " + this.mActivitiesList.get(position) + ", size = " + this.mActivitiesList.size());
        this.mActivitiesList.remove(position);
        updateItemInfoBetween(position, this.mActivitiesList.size());
        notifyDataSetChanged();
    }

    public void reorder(int fromPosition, int toPosition) {
        FloatAppItem appItem = (FloatAppItem) this.mActivitiesList.get(fromPosition);
        Log.d("FloatAppAdapter", "Reorder item: fromPosition = " + fromPosition + ", toPosition = " + toPosition + ", appItem = " + appItem + ", size = " + this.mActivitiesList.size());
        this.mActivitiesList.remove(fromPosition);
        this.mActivitiesList.add(toPosition, appItem);
        if (fromPosition > toPosition) {
            updateItemInfoBetween(toPosition, fromPosition + 1);
        } else {
            updateItemInfoBetween(fromPosition, toPosition + 1);
        }
        notifyDataSetChanged();
    }

    public Intent intentForPosition(int position) {
        if (this.mActivitiesList == null || this.mActivitiesList.size() <= position) {
            Log.d("FloatAppAdapter", "No intent for position(" + position + "), list size is " + this.mActivitiesList.size());
            return null;
        }
        Intent intent = new Intent("android.intent.action.MAIN", null);
        intent.addCategory("android.intent.category.LAUNCHER");
        FloatAppItem item = (FloatAppItem) this.mActivitiesList.get(position);
        intent.setClassName(item.packageName, item.className);
        intent.setPackage(item.packageName);
        if (item.extras != null) {
            intent.putExtras(item.extras);
        }
        return intent;
    }

    private void updateItemInfoBetween(int fromPosition, int toPosition) {
        Log.d("FloatAppAdapter", "updateItemInfoBetween: fromPosition = " + fromPosition + ", toPosition = " + toPosition + ", size = " + this.mActivitiesList.size());
        for (int i = fromPosition; i < toPosition; i++) {
            FloatAppItem item = (FloatAppItem) this.mActivitiesList.get(i);
            item.position = this.mActivitiesList.indexOf(item);
            item.container = this.mFloatContainer;
            this.mFloatModel.addItemToModifyListIfNeeded(item);
        }
    }
}
