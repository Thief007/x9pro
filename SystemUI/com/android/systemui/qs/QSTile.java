package com.android.systemui.qs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.Listenable;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.mediatek.systemui.statusbar.policy.HotKnotController;
import java.util.Objects;

public abstract class QSTile<TState extends State> implements Listenable {
    protected static final boolean DEBUG = Log.isLoggable("QSTile", 3);
    protected final String TAG = ("QSTile." + getClass().getSimpleName());
    private boolean mAnnounceNextStateChange;
    private Callback mCallback;
    protected final Context mContext;
    protected final H mHandler;
    protected final Host mHost;
    protected TState mState = newTileState();
    private TState mTmpState = newTileState();
    protected final Handler mUiHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onAnnouncementRequested(CharSequence charSequence);

        void onScanStateChanged(boolean z);

        void onShowDetail(boolean z);

        void onStateChanged(State state);

        void onToggleStateChanged(boolean z);
    }

    public static abstract class Icon {
        public abstract Drawable getDrawable(Context context);

        public int hashCode() {
            return Icon.class.hashCode();
        }
    }

    public static class ResourceIcon extends Icon {
        private static final SparseArray<Icon> ICONS = new SparseArray();
        protected final int mResId;

        private ResourceIcon(int resId) {
            this.mResId = resId;
        }

        public static Icon get(int resId) {
            Icon icon = (Icon) ICONS.get(resId);
            if (icon != null) {
                return icon;
            }
            icon = new ResourceIcon(resId);
            ICONS.put(resId, icon);
            return icon;
        }

        public Drawable getDrawable(Context context) {
            Drawable d = context.getDrawable(this.mResId);
            if (d instanceof Animatable) {
                ((Animatable) d).start();
            }
            return d;
        }

        public boolean equals(Object o) {
            return (o instanceof ResourceIcon) && ((ResourceIcon) o).mResId == this.mResId;
        }

        public String toString() {
            return String.format("ResourceIcon[resId=0x%08x]", new Object[]{Integer.valueOf(this.mResId)});
        }
    }

    protected class AnimationIcon extends ResourceIcon {
        private boolean mAllowAnimation;

        public AnimationIcon(int resId) {
            super(resId);
        }

        public void setAllowAnimation(boolean allowAnimation) {
            this.mAllowAnimation = allowAnimation;
        }

        public Drawable getDrawable(Context context) {
            AnimatedVectorDrawable d = (AnimatedVectorDrawable) context.getDrawable(this.mResId).getConstantState().newDrawable();
            d.start();
            if (this.mAllowAnimation) {
                this.mAllowAnimation = false;
            } else {
                d.stop();
            }
            return d;
        }
    }

    public static class State {
        public boolean autoMirrorDrawable = true;
        public String contentDescription;
        public String dualLabelContentDescription;
        public Icon icon;
        public String label;
        public boolean visible;

        public boolean copyTo(State other) {
            if (other == null) {
                throw new IllegalArgumentException();
            } else if (other.getClass().equals(getClass())) {
                boolean changed = (other.visible == this.visible && Objects.equals(other.icon, this.icon) && Objects.equals(other.label, this.label) && Objects.equals(other.contentDescription, this.contentDescription) && Objects.equals(Boolean.valueOf(other.autoMirrorDrawable), Boolean.valueOf(this.autoMirrorDrawable))) ? !Objects.equals(other.dualLabelContentDescription, this.dualLabelContentDescription) : true;
                other.visible = this.visible;
                other.icon = this.icon;
                other.label = this.label;
                other.contentDescription = this.contentDescription;
                other.dualLabelContentDescription = this.dualLabelContentDescription;
                other.autoMirrorDrawable = this.autoMirrorDrawable;
                return changed;
            } else {
                throw new IllegalArgumentException();
            }
        }

        public String toString() {
            return toStringBuilder().toString();
        }

        protected StringBuilder toStringBuilder() {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
            sb.append("visible=").append(this.visible);
            sb.append(",icon=").append(this.icon);
            sb.append(",label=").append(this.label);
            sb.append(",contentDescription=").append(this.contentDescription);
            sb.append(",dualLabelContentDescription=").append(this.dualLabelContentDescription);
            sb.append(",autoMirrorDrawable=").append(this.autoMirrorDrawable);
            return sb.append(']');
        }
    }

    public static class BooleanState extends State {
        public boolean value;

        public boolean copyTo(State other) {
            BooleanState o = (BooleanState) other;
            boolean changed = super.copyTo(other) || o.value != this.value;
            o.value = this.value;
            return changed;
        }

        protected StringBuilder toStringBuilder() {
            StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",value=" + this.value);
            return rt;
        }
    }

    public interface DetailAdapter {
        View createDetailView(Context context, View view, ViewGroup viewGroup);

        int getMetricsCategory();

        Intent getSettingsIntent();

        int getTitle();

        Boolean getToggleState();

        void setToggleState(boolean z);
    }

    protected final class H extends Handler {
        private H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            try {
                String name;
                if (msg.what == 1) {
                    name = "handleSetCallback";
                    QSTile.this.handleSetCallback((Callback) msg.obj);
                } else if (msg.what == 2) {
                    name = "handleClick";
                    QSTile.this.mAnnounceNextStateChange = true;
                    QSTile.this.handleClick();
                } else if (msg.what == 3) {
                    name = "handleSecondaryClick";
                    QSTile.this.handleSecondaryClick();
                } else if (msg.what == 4) {
                    name = "handleLongClick";
                    QSTile.this.handleLongClick();
                } else if (msg.what == 5) {
                    name = "handleRefreshState";
                    QSTile.this.handleRefreshState(msg.obj);
                } else if (msg.what == 6) {
                    name = "handleShowDetail";
                    r5 = QSTile.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    r5.handleShowDetail(z);
                } else if (msg.what == 7) {
                    name = "handleUserSwitch";
                    QSTile.this.handleUserSwitch(msg.arg1);
                } else if (msg.what == 8) {
                    name = "handleToggleStateChanged";
                    r5 = QSTile.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    r5.handleToggleStateChanged(z);
                } else if (msg.what == 9) {
                    name = "handleScanStateChanged";
                    r5 = QSTile.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    r5.handleScanStateChanged(z);
                } else if (msg.what == 10) {
                    name = "handleDestroy";
                    QSTile.this.handleDestroy();
                } else if (msg.what == 11) {
                    name = "handleClearState";
                    QSTile.this.handleClearState();
                } else {
                    throw new IllegalArgumentException("Unknown msg: " + msg.what);
                }
            } catch (Throwable t) {
                String error = "Error in " + null;
                Log.w(QSTile.this.TAG, error, t);
                QSTile.this.mHost.warn(error, t);
            }
        }
    }

    public interface Host {

        public interface Callback {
            void onTilesChanged();
        }

        void collapsePanels();

        BluetoothController getBluetoothController();

        CastController getCastController();

        Context getContext();

        FlashlightController getFlashlightController();

        HotKnotController getHotKnotController();

        HotspotController getHotspotController();

        KeyguardMonitor getKeyguardMonitor();

        LocationController getLocationController();

        Looper getLooper();

        NetworkController getNetworkController();

        RotationLockController getRotationLockController();

        ZenModeController getZenModeController();

        void startActivityDismissingKeyguard(PendingIntent pendingIntent);

        void startActivityDismissingKeyguard(Intent intent);

        void warn(String str, Throwable th);
    }

    public static final class SignalState extends State {
        public boolean activityIn;
        public boolean activityOut;
        public boolean connected;
        public boolean enabled;
        public boolean filter;
        public boolean isOverlayIconWide;
        public int overlayIconId;

        public boolean copyTo(State other) {
            SignalState o = (SignalState) other;
            boolean changed = (o.enabled == this.enabled && o.connected == this.connected && o.activityIn == this.activityIn && o.activityOut == this.activityOut && o.overlayIconId == this.overlayIconId) ? o.isOverlayIconWide != this.isOverlayIconWide : true;
            o.enabled = this.enabled;
            o.connected = this.connected;
            o.activityIn = this.activityIn;
            o.activityOut = this.activityOut;
            o.overlayIconId = this.overlayIconId;
            o.filter = this.filter;
            o.isOverlayIconWide = this.isOverlayIconWide;
            return !super.copyTo(other) ? changed : true;
        }

        protected StringBuilder toStringBuilder() {
            StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",enabled=" + this.enabled);
            rt.insert(rt.length() - 1, ",connected=" + this.connected);
            rt.insert(rt.length() - 1, ",activityIn=" + this.activityIn);
            rt.insert(rt.length() - 1, ",activityOut=" + this.activityOut);
            rt.insert(rt.length() - 1, ",overlayIconId=" + this.overlayIconId);
            rt.insert(rt.length() - 1, ",filter=" + this.filter);
            rt.insert(rt.length() - 1, ",wideOverlayIcon=" + this.isOverlayIconWide);
            return rt;
        }
    }

    protected enum UserBoolean {
        USER_TRUE(true, true),
        USER_FALSE(true, false),
        BACKGROUND_TRUE(false, true),
        BACKGROUND_FALSE(false, false);
        
        public final boolean userInitiated;
        public final boolean value;

        private UserBoolean(boolean userInitiated, boolean value) {
            this.value = value;
            this.userInitiated = userInitiated;
        }
    }

    public abstract int getMetricsCategory();

    protected abstract void handleClick();

    protected abstract void handleUpdateState(TState tState, Object obj);

    protected abstract TState newTileState();

    protected QSTile(Host host) {
        this.mHost = host;
        this.mContext = host.getContext();
        this.mHandler = new H(host.getLooper());
    }

    public boolean supportsDualTargets() {
        return false;
    }

    public Host getHost() {
        return this.mHost;
    }

    public QSTileView createTileView(Context context) {
        return new QSTileView(context);
    }

    public DetailAdapter getDetailAdapter() {
        return null;
    }

    public void setCallback(Callback callback) {
        this.mHandler.obtainMessage(1, callback).sendToTarget();
    }

    public void click() {
        this.mHandler.sendEmptyMessage(2);
    }

    public void secondaryClick() {
        this.mHandler.sendEmptyMessage(3);
    }

    public void longClick() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void showDetail(boolean show) {
        int i;
        H h = this.mHandler;
        if (show) {
            i = 1;
        } else {
            i = 0;
        }
        h.obtainMessage(6, i, 0).sendToTarget();
    }

    protected final void refreshState() {
        refreshState(null);
    }

    protected final void refreshState(Object arg) {
        this.mHandler.obtainMessage(5, arg).sendToTarget();
    }

    public final void clearState() {
        this.mHandler.sendEmptyMessage(11);
    }

    public void userSwitch(int newUserId) {
        this.mHandler.obtainMessage(7, newUserId, 0).sendToTarget();
    }

    public void fireToggleStateChanged(boolean state) {
        int i;
        H h = this.mHandler;
        if (state) {
            i = 1;
        } else {
            i = 0;
        }
        h.obtainMessage(8, i, 0).sendToTarget();
    }

    public void fireScanStateChanged(boolean state) {
        int i;
        H h = this.mHandler;
        if (state) {
            i = 1;
        } else {
            i = 0;
        }
        h.obtainMessage(9, i, 0).sendToTarget();
    }

    public void destroy() {
        this.mHandler.sendEmptyMessage(10);
    }

    public TState getState() {
        return this.mState;
    }

    public void setDetailListening(boolean listening) {
    }

    private void handleSetCallback(Callback callback) {
        this.mCallback = callback;
        handleRefreshState(null);
    }

    protected void handleSecondaryClick() {
    }

    protected void handleLongClick() {
    }

    protected void handleClearState() {
        this.mTmpState = newTileState();
        this.mState = newTileState();
    }

    protected void handleRefreshState(Object arg) {
        handleUpdateState(this.mTmpState, arg);
        if (this.mTmpState.copyTo(this.mState)) {
            handleStateChanged();
        }
    }

    private void handleStateChanged() {
        boolean delayAnnouncement = shouldAnnouncementBeDelayed();
        if (this.mCallback != null) {
            this.mCallback.onStateChanged(this.mState);
            if (this.mAnnounceNextStateChange && !delayAnnouncement) {
                String announcement = composeChangeAnnouncement();
                if (announcement != null) {
                    this.mCallback.onAnnouncementRequested(announcement);
                }
            }
        }
        if (!this.mAnnounceNextStateChange) {
            delayAnnouncement = false;
        }
        this.mAnnounceNextStateChange = delayAnnouncement;
    }

    protected boolean shouldAnnouncementBeDelayed() {
        return false;
    }

    protected String composeChangeAnnouncement() {
        return null;
    }

    private void handleShowDetail(boolean show) {
        if (this.mCallback != null) {
            this.mCallback.onShowDetail(show);
        }
    }

    private void handleToggleStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onToggleStateChanged(state);
        }
    }

    private void handleScanStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onScanStateChanged(state);
        }
    }

    protected void handleUserSwitch(int newUserId) {
        handleRefreshState(null);
    }

    protected void handleDestroy() {
        setListening(false);
        this.mCallback = null;
    }
}
