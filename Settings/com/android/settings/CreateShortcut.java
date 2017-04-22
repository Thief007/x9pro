package com.android.settings;

import android.app.LauncherActivity;
import android.app.LauncherActivity.ListItem;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.ListView;
import com.android.settings.Settings.TetherSettingsActivity;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardTile;
import com.android.settingslib.TetherUtil;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class CreateShortcut extends LauncherActivity {
    protected Intent getTargetIntent() {
        Intent targetIntent = new Intent("android.intent.action.MAIN", null);
        targetIntent.addCategory("com.android.settings.SHORTCUT");
        targetIntent.addFlags(268435456);
        return targetIntent;
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent shortcutIntent = intentForPosition(position);
        shortcutIntent.setFlags(2097152);
        Intent intent = new Intent();
        intent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher_settings));
        intent.putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent);
        intent.putExtra("android.intent.extra.shortcut.NAME", itemForPosition(position).label);
        ActivityInfo activityInfo = itemForPosition(position).resolveInfo.activityInfo;
        if (activityInfo.metaData != null && activityInfo.metaData.containsKey("com.android.settings.TOP_LEVEL_HEADER_ID")) {
            intent.putExtra("android.intent.extra.shortcut.ICON", createIcon(getDrawableResource(activityInfo.metaData.getInt("com.android.settings.TOP_LEVEL_HEADER_ID"))));
        }
        setResult(-1, intent);
        finish();
    }

    private Bitmap createIcon(int resource) {
        View view = LayoutInflater.from(new ContextThemeWrapper(this, 16974372)).inflate(R.layout.shortcut_badge, null);
        ((ImageView) view.findViewById(16908294)).setImageResource(resource);
        int spec = MeasureSpec.makeMeasureSpec(0, 0);
        view.measure(spec, spec);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return bitmap;
    }

    private int getDrawableResource(int topLevelId) {
        ArrayList<DashboardCategory> categories = new ArrayList();
        SettingsActivity.loadCategoriesFromResource(R.xml.dashboard_categories, categories, this);
        for (DashboardCategory category : categories) {
            for (DashboardTile tile : category.tiles) {
                if (tile.id == ((long) topLevelId)) {
                    return tile.iconRes;
                }
            }
        }
        return 0;
    }

    public List<ListItem> makeListItems() {
        List<ListItem> list = super.makeListItems();
        list.removeAll(makeRemoveListItems(list));
        return list;
    }

    private List<ListItem> makeRemoveListItems(List<ListItem> list) {
        ArrayList<ListItem> result = new ArrayList();
        if (list != null) {
            ArrayList<String> needRemoveItemComp = new ArrayList();
            if (FeatureOption.MTK_AUDIO_PROFILES) {
                needRemoveItemComp.add("com.android.settings.Settings$SoundSettingsActivity");
                needRemoveItemComp.add("com.android.settings.Settings$NotificationSettingsActivity");
                Log.i("CreateShortcut", "Not support google sound, remove it");
            } else {
                needRemoveItemComp.add("com.android.settings.Settings$AudioProfileSettingsActivity");
                Log.i("CreateShortcut", "Not support mtk audio profle, remove it");
            }
            if (FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
                needRemoveItemComp.add("com.android.settings.Settings$DreamSettingsActivity");
                Log.i("CreateShortcut", "Not support daydream, remove it");
            }
            for (ListItem item : list) {
                if (needRemoveItemComp.contains(item.className)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    protected boolean onEvaluateShowIcons() {
        return false;
    }

    protected List<ResolveInfo> onQueryPackageManager(Intent queryIntent) {
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(queryIntent, 128);
        if (activities == null) {
            return null;
        }
        for (int i = activities.size() - 1; i >= 0; i--) {
            if (((ResolveInfo) activities.get(i)).activityInfo.name.endsWith(TetherSettingsActivity.class.getSimpleName()) && !TetherUtil.isTetheringSupported(this)) {
                activities.remove(i);
            }
        }
        return activities;
    }
}
