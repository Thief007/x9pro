package com.android.systemui.tuner;

import android.app.ActivityManager;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.NET;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.Host.Callback;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.qs.QSTile.State;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.util.ArrayList;
import java.util.List;

public class QsTuner extends Fragment implements Callback {
    private FrameLayout mAddTarget;
    private FrameLayout mDropTarget;
    private DraggableQsPanel mQsPanel;
    private ScrollView mScrollRoot;
    private CustomHost mTileHost;

    public interface DropListener {
        void onDrop(String str);
    }

    private static class CustomHost extends QSTileHost {

        private static class BlankSecurityController implements SecurityController {
            private BlankSecurityController() {
            }

            public boolean hasDeviceOwner() {
                return false;
            }

            public boolean hasProfileOwner() {
                return false;
            }

            public String getDeviceOwnerName() {
                return null;
            }

            public String getProfileOwnerName() {
                return null;
            }

            public boolean isVpnEnabled() {
                return false;
            }

            public String getPrimaryVpnName() {
                return null;
            }

            public String getProfileVpnName() {
                return null;
            }

            public void addCallback(SecurityControllerCallback callback) {
            }

            public void removeCallback(SecurityControllerCallback callback) {
            }
        }

        public CustomHost(Context context) {
            super(context, null, null, null, null, null, null, null, null, null, null, null, new BlankSecurityController(), null, null, null, null, null);
        }

        protected QSTile<?> createTile(String tileSpec) {
            return new DraggableTile(this, tileSpec);
        }

        public void replace(String oldTile, String newTile) {
            if (!oldTile.equals(newTile)) {
                MetricsLogger.action(getContext(), 230, oldTile + "," + newTile);
                List<String> order = new ArrayList(this.mTileSpecs);
                int index = order.indexOf(oldTile);
                if (index < 0) {
                    Log.e("QsTuner", "Can't find " + oldTile);
                    return;
                }
                order.remove(newTile);
                order.add(index, newTile);
                setTiles(order);
            }
        }

        public void remove(String tile) {
            MetricsLogger.action(getContext(), 232, tile);
            List<String> tiles = new ArrayList(this.mTileSpecs);
            tiles.remove(tile);
            setTiles(tiles);
        }

        public void add(String tile) {
            MetricsLogger.action(getContext(), 231, tile);
            List<String> tiles = new ArrayList(this.mTileSpecs);
            tiles.add(tile);
            setTiles(tiles);
        }

        public void reset() {
            Secure.putStringForUser(getContext().getContentResolver(), "sysui_qs_tiles", "default", ActivityManager.getCurrentUser());
        }

        private void setTiles(List<String> tiles) {
            Secure.putStringForUser(getContext().getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", tiles), ActivityManager.getCurrentUser());
        }

        public void showAddDialog() {
            int i;
            String[] defaults;
            int index;
            List<String> tiles = this.mTileSpecs;
            int numBroadcast = 0;
            for (i = 0; i < tiles.size(); i++) {
                if (((String) tiles.get(i)).startsWith("intent(")) {
                    numBroadcast++;
                }
            }
            if (SystemProperties.get("ro.mtk_wfd_support").equals(FeatureOptionUtils.SUPPORT_YES)) {
                defaults = (getContext().getString(R.string.quick_settings_tiles_default_swm) + "," + getContext().getString(R.string.quick_settings_tiles_extra)).split(",");
            } else {
                defaults = (getContext().getString(R.string.quick_settings_tiles_default) + "," + getContext().getString(R.string.quick_settings_tiles_extra)).split(",");
            }
            final String[] available = new String[((defaults.length + 1) - (tiles.size() - numBroadcast))];
            final String[] availableTiles = new String[available.length];
            int index2 = 0;
            for (i = 0; i < defaults.length; i++) {
                if (!tiles.contains(defaults[i])) {
                    int resource = QsTuner.getLabelResource(defaults[i]);
                    if (resource != 0) {
                        availableTiles[index2] = defaults[i];
                        index = index2 + 1;
                        available[index2] = getContext().getString(resource);
                        index2 = index;
                    } else {
                        availableTiles[index2] = defaults[i];
                        index = index2 + 1;
                        available[index2] = defaults[i];
                        index2 = index;
                    }
                }
            }
            index = index2 + 1;
            available[index2] = getContext().getString(R.string.broadcast_tile);
            new Builder(getContext()).setTitle(R.string.add_tile).setItems(available, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which < available.length - 1) {
                        CustomHost.this.add(availableTiles[which]);
                    } else {
                        CustomHost.this.showBroadcastTileDialog();
                    }
                }
            }).show();
        }

        public void showBroadcastTileDialog() {
            final EditText editText = new EditText(getContext());
            new Builder(getContext()).setTitle(R.string.broadcast_tile).setView(editText).setNegativeButton(17039360, null).setPositiveButton(17039370, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String action = editText.getText().toString();
                    if (CustomHost.this.isValid(action)) {
                        CustomHost.this.add("intent(" + action + ')');
                    }
                }
            }).show();
        }

        private boolean isValid(String action) {
            for (int i = 0; i < action.length(); i++) {
                char c = action.charAt(i);
                if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '.') {
                    return false;
                }
            }
            return true;
        }
    }

    private class DragHelper implements OnDragListener {
        private final DropListener mListener;
        private final View mView;

        public DragHelper(View view, DropListener dropListener) {
            this.mView = view;
            this.mListener = dropListener;
            this.mView.setOnDragListener(this);
        }

        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case 3:
                    QsTuner.this.stopDrag();
                    this.mListener.onDrop(event.getClipData().getItemAt(0).getText().toString());
                    break;
                case 4:
                    QsTuner.this.stopDrag();
                    break;
                case 5:
                    this.mView.setBackgroundColor(2013265919);
                    break;
                case 6:
                    break;
            }
            this.mView.setBackgroundColor(0);
            return true;
        }
    }

    private class DraggableQsPanel extends QSPanel implements OnTouchListener {
        public DraggableQsPanel(Context context) {
            super(context);
            this.mBrightnessView.setVisibility(8);
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            for (TileRecord r : this.mRecords) {
                DragHelper dragHelper = new DragHelper(r.tileView, (DraggableTile) r.tile);
                r.tileView.setTag(r.tile);
                r.tileView.setOnTouchListener(this);
                for (int i = 0; i < r.tileView.getChildCount(); i++) {
                    r.tileView.getChildAt(i).setClickable(false);
                }
            }
        }

        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case 0:
                    String tileSpec = ((DraggableTile) v.getTag()).mSpec;
                    v.startDrag(ClipData.newPlainText(tileSpec, tileSpec), new DragShadowBuilder(v), null, 0);
                    QsTuner.this.onStartDrag();
                    return true;
                default:
                    return false;
            }
        }
    }

    private static class DraggableTile extends QSTile<State> implements DropListener {
        private String mSpec;
        private QSTileView mView;

        protected DraggableTile(Host host, String tileSpec) {
            super(host);
            Log.d(this.TAG, "Creating tile " + tileSpec);
            this.mSpec = tileSpec;
        }

        public QSTileView createTileView(Context context) {
            this.mView = super.createTileView(context);
            return this.mView;
        }

        public boolean supportsDualTargets() {
            return !"wifi".equals(this.mSpec) ? "bt".equals(this.mSpec) : true;
        }

        public void setListening(boolean listening) {
        }

        protected State newTileState() {
            return new State();
        }

        protected void handleClick() {
        }

        protected void handleUpdateState(State state, Object arg) {
            state.visible = true;
            state.icon = ResourceIcon.get(getIcon());
            state.label = getLabel();
        }

        private String getLabel() {
            int resource = QsTuner.getLabelResource(this.mSpec);
            if (resource != 0) {
                return this.mContext.getString(resource);
            }
            if (!this.mSpec.startsWith("intent(")) {
                return this.mSpec;
            }
            int lastDot = this.mSpec.lastIndexOf(46);
            if (lastDot >= 0) {
                return this.mSpec.substring(lastDot + 1, this.mSpec.length() - 1);
            }
            return this.mSpec.substring("intent(".length(), this.mSpec.length() - 1);
        }

        private int getIcon() {
            if (this.mSpec.equals("wifi")) {
                return R.drawable.ic_qs_wifi_full_3;
            }
            if (this.mSpec.equals("bt")) {
                return R.drawable.ic_qs_bluetooth_connected;
            }
            if (this.mSpec.equals("inversion")) {
                return R.drawable.ic_invert_colors_enable;
            }
            if (this.mSpec.equals("cell")) {
                return R.drawable.ic_qs_signal_full_3;
            }
            if (this.mSpec.equals("airplane")) {
                return R.drawable.ic_signal_airplane_enable;
            }
            if (this.mSpec.equals("dnd")) {
                return R.drawable.ic_qs_dnd_on;
            }
            if (this.mSpec.equals("rotation")) {
                return R.drawable.ic_portrait_from_auto_rotate;
            }
            if (this.mSpec.equals("flashlight")) {
                return R.drawable.ic_signal_flashlight_enable;
            }
            if (this.mSpec.equals("location")) {
                return R.drawable.ic_signal_location_enable;
            }
            if (this.mSpec.equals("cast")) {
                return R.drawable.ic_qs_cast_on;
            }
            if (this.mSpec.equals("hotspot")) {
                return R.drawable.ic_hotspot_enable;
            }
            if (this.mSpec.equals("audioprofile")) {
                return R.drawable.ic_qs_custom_on;
            }
            if (this.mSpec.equals("hotknot")) {
                return R.drawable.ic_signal_hotknot_enable;
            }
            if (this.mSpec.equals("superscreenshot")) {
                return R.drawable.ic_super_screen_shot_on;
            }
            if (this.mSpec.equals("screenrecord")) {
                return R.drawable.ic_screen_record_on;
            }
            return R.drawable.android;
        }

        public int getMetricsCategory() {
            return NET.DEFAULT_TIMEOUT;
        }

        public boolean equals(Object o) {
            if (o instanceof DraggableTile) {
                return this.mSpec.equals(((DraggableTile) o).mSpec);
            }
            return false;
        }

        public void onDrop(String sourceText) {
            ((CustomHost) this.mHost).replace(this.mSpec, sourceText);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 1, 0, 17040444);
    }

    public void onResume() {
        super.onResume();
        MetricsLogger.visibility(getContext(), 228, true);
    }

    public void onPause() {
        super.onPause();
        MetricsLogger.visibility(getContext(), 228, false);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                this.mTileHost.reset();
                break;
            case 16908332:
                getFragmentManager().popBackStack();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mScrollRoot = (ScrollView) inflater.inflate(R.layout.tuner_qs, container, false);
        this.mQsPanel = new DraggableQsPanel(getContext());
        this.mTileHost = new CustomHost(getContext());
        this.mTileHost.setCallback(this);
        this.mQsPanel.setTiles(this.mTileHost.getTiles());
        this.mQsPanel.setHost(this.mTileHost);
        this.mQsPanel.refreshAllTiles();
        ((ViewGroup) this.mScrollRoot.findViewById(R.id.all_details)).addView(this.mQsPanel, 0);
        this.mDropTarget = (FrameLayout) this.mScrollRoot.findViewById(R.id.remove_target);
        setupDropTarget();
        this.mAddTarget = (FrameLayout) this.mScrollRoot.findViewById(R.id.add_target);
        setupAddTarget();
        return this.mScrollRoot;
    }

    public void onDestroyView() {
        this.mTileHost.destroy();
        super.onDestroyView();
    }

    private void setupDropTarget() {
        QSTileView tileView = new QSTileView(getContext());
        State state = new State();
        state.visible = true;
        state.icon = ResourceIcon.get(R.drawable.ic_delete);
        state.label = getString(17040186);
        tileView.onStateChanged(state);
        this.mDropTarget.addView(tileView);
        this.mDropTarget.setVisibility(8);
        DragHelper dragHelper = new DragHelper(tileView, new DropListener() {
            public void onDrop(String sourceText) {
                QsTuner.this.mTileHost.remove(sourceText);
            }
        });
    }

    private void setupAddTarget() {
        QSTileView tileView = new QSTileView(getContext());
        State state = new State();
        state.visible = true;
        state.icon = ResourceIcon.get(R.drawable.ic_add_circle_qs);
        state.label = getString(R.string.add_tile);
        tileView.onStateChanged(state);
        this.mAddTarget.addView(tileView);
        tileView.setClickable(true);
        tileView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                QsTuner.this.mTileHost.showAddDialog();
            }
        });
    }

    public void onStartDrag() {
        this.mDropTarget.post(new Runnable() {
            public void run() {
                QsTuner.this.mDropTarget.setVisibility(0);
                QsTuner.this.mAddTarget.setVisibility(8);
            }
        });
    }

    public void stopDrag() {
        this.mDropTarget.post(new Runnable() {
            public void run() {
                QsTuner.this.mDropTarget.setVisibility(8);
                QsTuner.this.mAddTarget.setVisibility(0);
            }
        });
    }

    public void onTilesChanged() {
        this.mQsPanel.setTiles(this.mTileHost.getTiles());
    }

    private static int getLabelResource(String spec) {
        if (spec.equals("wifi")) {
            return R.string.quick_settings_wifi_label;
        }
        if (spec.equals("bt")) {
            return R.string.quick_settings_bluetooth_label;
        }
        if (spec.equals("inversion")) {
            return R.string.quick_settings_inversion_label;
        }
        if (spec.equals("cell")) {
            return R.string.quick_settings_cellular_detail_title;
        }
        if (spec.equals("airplane")) {
            return R.string.airplane_mode;
        }
        if (spec.equals("dnd")) {
            return R.string.quick_settings_dnd_label;
        }
        if (spec.equals("rotation")) {
            return R.string.quick_settings_rotation_locked_label;
        }
        if (spec.equals("flashlight")) {
            return R.string.quick_settings_flashlight_label;
        }
        if (spec.equals("location")) {
            return R.string.quick_settings_location_label;
        }
        if (spec.equals("cast")) {
            return R.string.quick_settings_cast_title;
        }
        if (spec.equals("hotspot")) {
            return R.string.quick_settings_hotspot_label;
        }
        if (spec.equals("audioprofile")) {
            return R.string.audio_profile;
        }
        if (spec.equals("hotknot")) {
            return R.string.quick_settings_hotknot_label;
        }
        if (spec.equals("superscreenshot")) {
            return R.string.quick_settings_superscreenshot_label;
        }
        if (spec.equals("screenrecord")) {
            return R.string.quick_settings_screenrecord_label;
        }
        return 0;
    }
}
