package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.WifiDisplayStatus;
import android.net.wifi.p2p.WifiP2pDevice;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSDetailItems.Item;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.CastController.CastDevice;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.LinkedHashMap;
import java.util.Set;

public class CastTile extends QSTile<BooleanState> {
    private static final Intent CAST_SETTINGS = new Intent("android.settings.CAST_SETTINGS");
    private static final Intent WFD_SINK_SETTINGS = new Intent("mediatek.settings.WFD_SINK_SETTINGS");
    private final Callback mCallback = new Callback();
    private final CastController mController;
    private final CastDetailAdapter mDetailAdapter;
    private final KeyguardMonitor mKeyguard;

    private final class Callback implements com.android.systemui.statusbar.policy.CastController.Callback, com.android.systemui.statusbar.policy.KeyguardMonitor.Callback {
        private Callback() {
        }

        public void onCastDevicesChanged() {
            Log.d(CastTile.this.TAG, "onCastDevicesChanged");
            CastTile.this.refreshState();
        }

        public void onWfdStatusChanged(WifiDisplayStatus status, boolean sinkMode) {
            Log.d(CastTile.this.TAG, "onWfdStatusChanged: " + status.getActiveDisplayState());
            CastTile.this.mDetailAdapter.wfdStatusChanged(status, sinkMode);
            CastTile.this.refreshState();
        }

        public void onWifiP2pDeviceChanged(WifiP2pDevice device) {
            Log.d(CastTile.this.TAG, "onWifiP2pDeviceChanged");
            CastTile.this.mDetailAdapter.updateDeviceName(device);
        }

        public void onKeyguardChanged() {
            Log.d(CastTile.this.TAG, "onKeyguardChanged");
            CastTile.this.refreshState();
        }
    }

    private final class CastDetailAdapter implements DetailAdapter, com.android.systemui.qs.QSDetailItems.Callback {
        private LinearLayout mDetailView;
        private QSDetailItems mItems;
        private boolean mSinkViewEnabledBak;
        private final LinkedHashMap<String, CastDevice> mVisibleOrder;
        private View mWfdSinkView;

        private CastDetailAdapter() {
            this.mVisibleOrder = new LinkedHashMap();
            this.mSinkViewEnabledBak = true;
        }

        public int getTitle() {
            return R.string.quick_settings_cast_title;
        }

        public Boolean getToggleState() {
            return null;
        }

        public Intent getSettingsIntent() {
            return CastTile.CAST_SETTINGS;
        }

        public void setToggleState(boolean state) {
        }

        public int getMetricsCategory() {
            return 151;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (CastTile.this.mController.isWfdSinkSupported()) {
                this.mItems = QSDetailItems.convertOrInflate(context, this.mItems, parent);
            } else {
                this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            }
            this.mItems.setTagSuffix("Cast");
            if (convertView == null) {
                Log.d(CastTile.this.TAG, "addOnAttachStateChangeListener");
                this.mItems.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    public void onViewAttachedToWindow(View v) {
                        Log.d(CastTile.this.TAG, "onViewAttachedToWindow");
                    }

                    public void onViewDetachedFromWindow(View v) {
                        Log.d(CastTile.this.TAG, "onViewDetachedFromWindow");
                        CastDetailAdapter.this.mVisibleOrder.clear();
                    }
                });
            }
            this.mItems.setEmptyState(R.drawable.ic_qs_cast_detail_empty, R.string.quick_settings_cast_detail_empty_text);
            this.mItems.setCallback(this);
            updateItems(CastTile.this.mController.getCastDevices());
            CastTile.this.mController.setDiscovering(true);
            if (CastTile.this.mController.isWfdSinkSupported()) {
                Log.d(CastTile.this.TAG, "add WFD sink view: " + (this.mWfdSinkView == null));
                if (this.mWfdSinkView == null) {
                    LayoutInflater layoutInflater = LayoutInflater.from(context);
                    this.mWfdSinkView = layoutInflater.inflate(R.layout.qs_wfd_prefrence_material, parent, false);
                    layoutInflater.inflate(R.layout.qs_wfd_widget_switch, (ViewGroup) this.mWfdSinkView.findViewById(16908312));
                    ImageView view = (ImageView) this.mWfdSinkView.findViewById(16908294);
                    if (context.getResources().getBoolean(17956947)) {
                        view.setImageResource(R.drawable.ic_wfd_cellphone);
                    } else {
                        view.setImageResource(R.drawable.ic_wfd_laptop);
                    }
                    ((TextView) this.mWfdSinkView.findViewById(16908304)).setText(R.string.wfd_sink_summary);
                    this.mWfdSinkView.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            boolean z;
                            Switch swi = (Switch) v.findViewById(16908289);
                            boolean checked = swi.isChecked();
                            if (!checked) {
                                CastTile.this.getHost().startActivityDismissingKeyguard(CastTile.WFD_SINK_SETTINGS);
                            }
                            if (checked) {
                                z = false;
                            } else {
                                z = true;
                            }
                            swi.setChecked(z);
                        }
                    });
                }
                if (convertView instanceof LinearLayout) {
                    this.mDetailView = (LinearLayout) convertView;
                    updateSinkView();
                } else {
                    this.mDetailView = new LinearLayout(context);
                    this.mDetailView.setOrientation(1);
                    this.mDetailView.addView(this.mWfdSinkView);
                    View devider = new View(context);
                    devider.setLayoutParams(new LayoutParams(-1, context.getResources().getDimensionPixelSize(R.dimen.qs_tile_divider_height)));
                    devider.setBackgroundColor(context.getResources().getColor(R.color.qs_tile_divider));
                    this.mDetailView.addView(devider);
                    this.mDetailView.addView(this.mItems);
                    View spacer = this.mItems.findViewById(R.id.min_height_spacer);
                    if (spacer != null) {
                        this.mDetailView.setLayoutParams(new LayoutParams(-1, spacer.getLayoutParams().height));
                    } else {
                        Log.d(CastTile.this.TAG, "get min_height_spacer fail");
                    }
                }
                updateDeviceName(CastTile.this.mController.getWifiP2pDev());
                setSinkViewVisible(CastTile.this.mController.isNeedShowWfdSink());
                setSinkViewEnabled(this.mSinkViewEnabledBak);
            }
            if (this.mDetailView != null) {
                return this.mDetailView;
            }
            return this.mItems;
        }

        private void updateItems(Set<CastDevice> devices) {
            Log.d(CastTile.this.TAG, "update items: " + devices.size());
            if (this.mItems != null) {
                Item[] itemArr = null;
                if (!(devices == null || devices.isEmpty())) {
                    CastDevice device;
                    Item item;
                    for (CastDevice device2 : devices) {
                        if (device2.state == 2) {
                            item = new Item();
                            item.icon = R.drawable.ic_qs_cast_on;
                            item.line1 = CastTile.this.getDeviceName(device2);
                            item.line2 = CastTile.this.mContext.getString(R.string.quick_settings_connected);
                            item.tag = device2;
                            item.canDisconnect = true;
                            itemArr = new Item[]{item};
                            break;
                        }
                    }
                    if (itemArr == null) {
                        for (CastDevice device22 : devices) {
                            this.mVisibleOrder.put(device22.id, device22);
                        }
                        itemArr = new Item[devices.size()];
                        int i = 0;
                        for (String id : this.mVisibleOrder.keySet()) {
                            device22 = (CastDevice) this.mVisibleOrder.get(id);
                            if (devices.contains(device22)) {
                                item = new Item();
                                item.icon = R.drawable.ic_qs_cast_off;
                                item.line1 = CastTile.this.getDeviceName(device22);
                                if (device22.state == 1) {
                                    item.line2 = CastTile.this.mContext.getString(R.string.quick_settings_connecting);
                                }
                                item.tag = device22;
                                int i2 = i + 1;
                                itemArr[i] = item;
                                i = i2;
                            }
                        }
                    }
                }
                this.mItems.setItems(itemArr);
            }
        }

        public void onDetailItemClick(Item item) {
            if (item != null && item.tag != null) {
                MetricsLogger.action(CastTile.this.mContext, 157);
                CastDevice device = item.tag;
                Log.d(CastTile.this.TAG, "onDetailItemClick: " + device.name);
                CastTile.this.mController.startCasting(device);
                CastTile.this.mController.updateWfdFloatMenu(true);
            }
        }

        public void onDetailItemDisconnect(Item item) {
            if (item != null && item.tag != null) {
                MetricsLogger.action(CastTile.this.mContext, 158);
                CastDevice device = item.tag;
                Log.d(CastTile.this.TAG, "onDetailItemDisconnect: " + device.name);
                CastTile.this.mController.stopCasting(device);
                CastTile.this.mController.updateWfdFloatMenu(false);
            }
        }

        private void wfdStatusChanged(WifiDisplayStatus status, boolean sinkMode) {
            int activeDisplayState;
            boolean show = CastTile.this.mController.isNeedShowWfdSink();
            setSinkViewVisible(show);
            if (show) {
                activeDisplayState = status.getActiveDisplayState();
            } else {
                activeDisplayState = 0;
            }
            handleWfdStateChanged(activeDisplayState, sinkMode);
        }

        private void handleWfdStateChanged(int wfdState, boolean sinkMode) {
            switch (wfdState) {
                case 0:
                    if (!sinkMode) {
                        setSinkViewEnabled(true);
                        setSinkViewChecked(false);
                        CastTile.this.mController.updateWfdFloatMenu(false);
                        return;
                    }
                    return;
                case 1:
                    if (!sinkMode) {
                        setSinkViewEnabled(false);
                        return;
                    }
                    return;
                case 2:
                    if (!sinkMode) {
                        setSinkViewEnabled(false);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private void updateDeviceName(WifiP2pDevice device) {
            if (device != null && this.mWfdSinkView != null) {
                Log.d(CastTile.this.TAG, "updateDeviceName: " + device.deviceName);
                TextView textView = (TextView) this.mWfdSinkView.findViewById(16908310);
                if (TextUtils.isEmpty(device.deviceName)) {
                    textView.setText(device.deviceAddress);
                } else {
                    textView.setText(device.deviceName);
                }
            }
        }

        private void setSinkViewVisible(boolean visible) {
            if (this.mWfdSinkView != null) {
                Log.d(CastTile.this.TAG, "setSinkViewVisible: " + visible);
                if (!visible) {
                    this.mWfdSinkView.setVisibility(8);
                } else if (this.mWfdSinkView.getVisibility() != 0) {
                    updateDeviceName(CastTile.this.mController.getWifiP2pDev());
                    this.mWfdSinkView.setVisibility(0);
                }
            }
        }

        private void setSinkViewEnabled(boolean enabled) {
            this.mSinkViewEnabledBak = enabled;
            if (this.mWfdSinkView != null) {
                Log.d(CastTile.this.TAG, "setSinkViewEnabled: " + enabled);
                setEnabledStateOnViews(this.mWfdSinkView, enabled);
            }
        }

        private void setEnabledStateOnViews(View v, boolean enabled) {
            v.setEnabled(enabled);
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    setEnabledStateOnViews(vg.getChildAt(i), enabled);
                }
            }
        }

        private void setSinkViewChecked(boolean checked) {
            if (this.mWfdSinkView != null) {
                Log.d(CastTile.this.TAG, "setSinkViewChecked: " + checked);
                ((Switch) this.mWfdSinkView.findViewById(16908289)).setChecked(checked);
            }
        }

        private void updateSinkView() {
            if (this.mWfdSinkView != null) {
                Log.d(CastTile.this.TAG, "updateSinkView summary");
                final TextView summary = (TextView) this.mWfdSinkView.findViewById(16908304);
                summary.post(new Runnable() {
                    public void run() {
                        summary.setText(R.string.wfd_sink_summary);
                    }
                });
            }
        }
    }

    public CastTile(Host host) {
        super(host);
        this.mController = host.getCastController();
        this.mDetailAdapter = new CastDetailAdapter();
        this.mKeyguard = host.getKeyguardMonitor();
        this.mController.setListening(true);
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (this.mController != null) {
            Log.d(this.TAG, "setListening " + listening);
            if (listening) {
                this.mController.addCallback(this.mCallback);
                this.mKeyguard.addCallback(this.mCallback);
            } else {
                this.mController.setDiscovering(false);
                this.mController.removeCallback(this.mCallback);
                this.mKeyguard.removeCallback(this.mCallback);
            }
        }
    }

    protected void handleDestroy() {
        super.handleDestroy();
        if (this.mController != null) {
            Log.d(this.TAG, "handle destroy");
            this.mController.setListening(false);
        }
    }

    protected void handleUserSwitch(int newUserId) {
        super.handleUserSwitch(newUserId);
        if (this.mController != null) {
            this.mController.setCurrentUserId(newUserId);
        }
    }

    protected void handleClick() {
        MetricsLogger.action(this.mContext, getMetricsCategory());
        showDetail(true);
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean canSkipBouncer;
        int i;
        if (this.mKeyguard.isSecure() && this.mKeyguard.isShowing()) {
            canSkipBouncer = this.mKeyguard.canSkipBouncer();
        } else {
            canSkipBouncer = true;
        }
        state.visible = canSkipBouncer;
        state.label = this.mContext.getString(R.string.quick_settings_cast_title);
        state.value = false;
        state.autoMirrorDrawable = false;
        Set<CastDevice> devices = this.mController.getCastDevices();
        boolean connecting = false;
        for (CastDevice device : devices) {
            if (device.state == 2) {
                state.value = true;
                state.label = getDeviceName(device);
            } else if (device.state == 1) {
                connecting = true;
            }
        }
        if (!state.value && connecting) {
            state.label = this.mContext.getString(R.string.quick_settings_connecting);
        }
        if (state.value) {
            i = R.drawable.ic_qs_cast_on;
        } else {
            i = R.drawable.ic_qs_cast_off;
        }
        state.icon = ResourceIcon.get(i);
        this.mDetailAdapter.updateItems(devices);
        this.mDetailAdapter.updateSinkView();
    }

    public int getMetricsCategory() {
        return 114;
    }

    protected String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return null;
        }
        return this.mContext.getString(R.string.accessibility_casting_turned_off);
    }

    private String getDeviceName(CastDevice device) {
        if (device.name != null) {
            return device.name;
        }
        return this.mContext.getString(R.string.quick_settings_cast_device_default_name);
    }
}
