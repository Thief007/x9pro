package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSDetailItems.Item;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BluetoothController.Callback;
import java.util.Collection;

public class BluetoothTile extends QSTile<BooleanState> {
    private static final Intent BLUETOOTH_SETTINGS = new Intent("android.settings.BLUETOOTH_SETTINGS");
    private final Callback mCallback = new Callback() {
        public void onBluetoothStateChange(boolean enabled) {
            BluetoothTile.this.refreshState();
        }

        public void onBluetoothDevicesChanged() {
            BluetoothTile.this.mUiHandler.post(new Runnable() {
                public void run() {
                    BluetoothTile.this.mDetailAdapter.updateItems();
                }
            });
            BluetoothTile.this.refreshState();
        }
    };
    private final BluetoothController mController;
    private final BluetoothDetailAdapter mDetailAdapter;

    private final class BluetoothDetailAdapter implements DetailAdapter, QSDetailItems.Callback {
        private QSDetailItems mItems;

        private BluetoothDetailAdapter() {
        }

        public int getTitle() {
            return R.string.quick_settings_bluetooth_label;
        }

        public Boolean getToggleState() {
            return Boolean.valueOf(((BooleanState) BluetoothTile.this.mState).value);
        }

        public Intent getSettingsIntent() {
            return BluetoothTile.BLUETOOTH_SETTINGS;
        }

        public void setToggleState(boolean state) {
            MetricsLogger.action(BluetoothTile.this.mContext, 154, state);
            BluetoothTile.this.mController.setBluetoothEnabled(state);
            BluetoothTile.this.showDetail(false);
        }

        public int getMetricsCategory() {
            return 150;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Bluetooth");
            this.mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty, R.string.quick_settings_bluetooth_detail_empty_text);
            this.mItems.setCallback(this);
            this.mItems.setMinHeightInItems(0);
            updateItems();
            setItemsVisible(((BooleanState) BluetoothTile.this.mState).value);
            return this.mItems;
        }

        public void setItemsVisible(boolean visible) {
            if (this.mItems != null) {
                this.mItems.setItemsVisible(visible);
            }
        }

        private void updateItems() {
            if (this.mItems != null) {
                Item[] itemArr = null;
                Collection<CachedBluetoothDevice> devices = BluetoothTile.this.mController.getDevices();
                if (devices != null) {
                    itemArr = new Item[getBondedCount(devices)];
                    int i = 0;
                    for (CachedBluetoothDevice device : devices) {
                        if (device.getBondState() != 10) {
                            Item item = new Item();
                            item.icon = R.drawable.ic_qs_bluetooth_on;
                            item.line1 = device.getName();
                            int state = device.getMaxConnectionState();
                            if (state == 2) {
                                item.icon = R.drawable.ic_qs_bluetooth_connected;
                                item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connected);
                                item.canDisconnect = true;
                            } else if (state == 1) {
                                item.icon = R.drawable.ic_qs_bluetooth_connecting;
                                item.line2 = BluetoothTile.this.mContext.getString(R.string.quick_settings_connecting);
                            }
                            item.tag = device;
                            int i2 = i + 1;
                            itemArr[i] = item;
                            i = i2;
                        }
                    }
                }
                this.mItems.setItems(itemArr);
            }
        }

        private int getBondedCount(Collection<CachedBluetoothDevice> devices) {
            int ct = 0;
            for (CachedBluetoothDevice device : devices) {
                if (device.getBondState() != 10) {
                    ct++;
                }
            }
            return ct;
        }

        public void onDetailItemClick(Item item) {
            if (item != null && item.tag != null) {
                CachedBluetoothDevice device = item.tag;
                if (device != null && device.getMaxConnectionState() == 0) {
                    BluetoothTile.this.mController.connect(device);
                }
            }
        }

        public void onDetailItemDisconnect(Item item) {
            if (item != null && item.tag != null) {
                CachedBluetoothDevice device = item.tag;
                if (device != null) {
                    BluetoothTile.this.mController.disconnect(device);
                }
            }
        }
    }

    public BluetoothTile(Host host) {
        super(host);
        this.mController = host.getBluetoothController();
        this.mDetailAdapter = new BluetoothDetailAdapter();
    }

    public boolean supportsDualTargets() {
        return true;
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
        if (listening) {
            this.mController.addStateChangedCallback(this.mCallback);
        } else {
            this.mController.removeStateChangedCallback(this.mCallback);
        }
    }

    protected void handleClick() {
        boolean z;
        boolean z2 = false;
        boolean isEnabled = Boolean.valueOf(((BooleanState) this.mState).value).booleanValue();
        Context context = this.mContext;
        int metricsCategory = getMetricsCategory();
        if (isEnabled) {
            z = false;
        } else {
            z = true;
        }
        MetricsLogger.action(context, metricsCategory, z);
        BluetoothController bluetoothController = this.mController;
        if (!isEnabled) {
            z2 = true;
        }
        bluetoothController.setBluetoothEnabled(z2);
    }

    protected void handleLongClick() {
        if (!((BooleanState) this.mState).value) {
            ((BooleanState) this.mState).value = true;
            this.mController.setBluetoothEnabled(true);
        }
        showDetail(true);
    }

    protected void handleSecondaryClick() {
        if (!((BooleanState) this.mState).value) {
            ((BooleanState) this.mState).value = true;
            this.mController.setBluetoothEnabled(true);
        }
        showDetail(true);
    }

    protected void handleUpdateState(BooleanState state, Object arg) {
        boolean supported = this.mController.isBluetoothSupported();
        boolean enabled = this.mController.isBluetoothEnabled();
        boolean connected = this.mController.isBluetoothConnected();
        boolean connecting = this.mController.isBluetoothConnecting();
        state.visible = supported;
        state.value = enabled;
        state.autoMirrorDrawable = false;
        if (enabled) {
            state.label = null;
            if (connected) {
                state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_connected);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_connected);
                state.label = this.mController.getLastDeviceName();
            } else if (connecting) {
                state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_connecting);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_connecting);
                state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            } else {
                state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_on);
                state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_on);
            }
            if (TextUtils.isEmpty(state.label)) {
                state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            }
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_off);
            state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_off);
        }
        String bluetoothName = state.label;
        if (connected) {
            bluetoothName = this.mContext.getString(R.string.accessibility_bluetooth_name, new Object[]{state.label});
            state.dualLabelContentDescription = bluetoothName;
        }
        state.dualLabelContentDescription = bluetoothName;
    }

    public int getMetricsCategory() {
        return 113;
    }

    protected String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_off);
    }
}
