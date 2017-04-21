package com.android.systemui.statusbar;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Pair;
import com.android.internal.statusbar.IStatusBar.Stub;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;

public class CommandQueue extends Stub {
    private Callbacks mCallbacks;
    private Handler mHandler = new H();
    private StatusBarIconList mList;

    public interface Callbacks {
        void addIcon(String str, int i, int i2, StatusBarIcon statusBarIcon);

        void animateCollapsePanels(int i);

        void animateExpandNotificationsPanel();

        void animateExpandSettingsPanel();

        void appTransitionCancelled();

        void appTransitionPending();

        void appTransitionStarting(long j, long j2);

        void buzzBeepBlinked();

        void cancelPreloadRecentApps();

        void disable(int i, int i2, boolean z);

        void hideDefaultAccountStatus();

        void hideRecentApps(boolean z, boolean z2);

        void notificationLightOff();

        void notificationLightPulse(int i, int i2, int i3);

        void preloadRecentApps();

        void removeIcon(String str, int i, int i2);

        void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z);

        void setSystemUiVisibility(int i, int i2);

        void setWindowState(int i, int i2);

        void showAssistDisclosure();

        void showDefaultAccountStatus(int i);

        void showRecentApps(boolean z);

        void showScreenPinningRequest();

        void startAssist(Bundle bundle);

        void toggleRecentApps();

        void topAppWindowChanged(boolean z);

        void updateIcon(String str, int i, int i2, StatusBarIcon statusBarIcon, StatusBarIcon statusBarIcon2);
    }

    private final class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            Callbacks -get0;
            switch (msg.what & -65536) {
                case 65536:
                    int index = msg.what & 65535;
                    int viewIndex = CommandQueue.this.mList.getViewIndex(index);
                    switch (msg.arg1) {
                        case 1:
                            StatusBarIcon icon = msg.obj;
                            StatusBarIcon old = CommandQueue.this.mList.getIcon(index);
                            if (old == null) {
                                CommandQueue.this.mList.setIcon(index, icon);
                                CommandQueue.this.mCallbacks.addIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex, icon);
                                return;
                            }
                            CommandQueue.this.mList.setIcon(index, icon);
                            CommandQueue.this.mCallbacks.updateIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex, old, icon);
                            return;
                        case 2:
                            if (CommandQueue.this.mList.getIcon(index) != null) {
                                CommandQueue.this.mList.removeIcon(index);
                                CommandQueue.this.mCallbacks.removeIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex);
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                case 131072:
                    CommandQueue.this.mCallbacks.disable(msg.arg1, msg.arg2, true);
                    return;
                case 196608:
                    CommandQueue.this.mCallbacks.animateExpandNotificationsPanel();
                    return;
                case 262144:
                    CommandQueue.this.mCallbacks.animateCollapsePanels(0);
                    return;
                case 327680:
                    CommandQueue.this.mCallbacks.animateExpandSettingsPanel();
                    return;
                case 393216:
                    CommandQueue.this.mCallbacks.setSystemUiVisibility(msg.arg1, msg.arg2);
                    return;
                case 458752:
                    -get0 = CommandQueue.this.mCallbacks;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    -get0.topAppWindowChanged(z);
                    return;
                case 524288:
                    CommandQueue.this.mCallbacks.setImeWindowStatus((IBinder) msg.obj, msg.arg1, msg.arg2, msg.getData().getBoolean("showImeSwitcherKey", false));
                    return;
                case 589824:
                    CommandQueue.this.mCallbacks.toggleRecentApps();
                    return;
                case 655360:
                    CommandQueue.this.mCallbacks.preloadRecentApps();
                    return;
                case 720896:
                    CommandQueue.this.mCallbacks.cancelPreloadRecentApps();
                    return;
                case 786432:
                    CommandQueue.this.mCallbacks.setWindowState(msg.arg1, msg.arg2);
                    return;
                case 851968:
                    -get0 = CommandQueue.this.mCallbacks;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    -get0.showRecentApps(z);
                    return;
                case 917504:
                    Callbacks -get02 = CommandQueue.this.mCallbacks;
                    boolean z2 = msg.arg1 != 0;
                    if (msg.arg2 == 0) {
                        z = false;
                    }
                    -get02.hideRecentApps(z2, z);
                    return;
                case 983040:
                    CommandQueue.this.mCallbacks.buzzBeepBlinked();
                    return;
                case 1048576:
                    CommandQueue.this.mCallbacks.notificationLightOff();
                    return;
                case 1114112:
                    CommandQueue.this.mCallbacks.notificationLightPulse(((Integer) msg.obj).intValue(), msg.arg1, msg.arg2);
                    return;
                case 1179648:
                    CommandQueue.this.mCallbacks.showScreenPinningRequest();
                    return;
                case 1245184:
                    CommandQueue.this.mCallbacks.appTransitionPending();
                    return;
                case 1310720:
                    CommandQueue.this.mCallbacks.appTransitionCancelled();
                    return;
                case 1376256:
                    Pair<Long, Long> data = msg.obj;
                    CommandQueue.this.mCallbacks.appTransitionStarting(((Long) data.first).longValue(), ((Long) data.second).longValue());
                    return;
                case 1441792:
                    CommandQueue.this.mCallbacks.showAssistDisclosure();
                    return;
                case 1507328:
                    CommandQueue.this.mCallbacks.startAssist((Bundle) msg.obj);
                    return;
                case 1572864:
                    CommandQueue.this.mCallbacks.showDefaultAccountStatus(((Integer) msg.obj).intValue());
                    return;
                case 1638400:
                    CommandQueue.this.mCallbacks.hideDefaultAccountStatus();
                    return;
                default:
                    return;
            }
        }
    }

    public CommandQueue(Callbacks callbacks, StatusBarIconList list) {
        this.mCallbacks = callbacks;
        this.mList = list;
    }

    public void setIcon(int index, StatusBarIcon icon) {
        synchronized (this.mList) {
            int what = 65536 | index;
            this.mHandler.removeMessages(what);
            this.mHandler.obtainMessage(what, 1, 0, icon.clone()).sendToTarget();
        }
    }

    public void removeIcon(int index) {
        synchronized (this.mList) {
            int what = 65536 | index;
            this.mHandler.removeMessages(what);
            this.mHandler.obtainMessage(what, 2, 0, null).sendToTarget();
        }
    }

    public void disable(int state1, int state2) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(131072);
            this.mHandler.obtainMessage(131072, state1, state2, null).sendToTarget();
        }
    }

    public void animateExpandNotificationsPanel() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(196608);
            this.mHandler.sendEmptyMessage(196608);
        }
    }

    public void animateCollapsePanels() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(262144);
            this.mHandler.sendEmptyMessage(262144);
        }
    }

    public void animateExpandSettingsPanel() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(327680);
            this.mHandler.sendEmptyMessage(327680);
        }
    }

    public void setSystemUiVisibility(int vis, int mask) {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(393216, vis, mask, null).sendToTarget();
        }
    }

    public void topAppWindowChanged(boolean menuVisible) {
        int i = 0;
        synchronized (this.mList) {
            this.mHandler.removeMessages(458752);
            Handler handler = this.mHandler;
            if (menuVisible) {
                i = 1;
            }
            handler.obtainMessage(458752, i, 0, null).sendToTarget();
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(524288);
            Message m = this.mHandler.obtainMessage(524288, vis, backDisposition, token);
            m.getData().putBoolean("showImeSwitcherKey", showImeSwitcher);
            m.sendToTarget();
        }
    }

    public void showRecentApps(boolean triggeredFromAltTab) {
        int i = 0;
        synchronized (this.mList) {
            this.mHandler.removeMessages(851968);
            Handler handler = this.mHandler;
            if (triggeredFromAltTab) {
                i = 1;
            }
            handler.obtainMessage(851968, i, 0, null).sendToTarget();
        }
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        int i = 1;
        synchronized (this.mList) {
            int i2;
            this.mHandler.removeMessages(917504);
            Handler handler = this.mHandler;
            if (triggeredFromAltTab) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (!triggeredFromHomeKey) {
                i = 0;
            }
            handler.obtainMessage(917504, i2, i, null).sendToTarget();
        }
    }

    public void toggleRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(589824);
            this.mHandler.obtainMessage(589824, 0, 0, null).sendToTarget();
        }
    }

    public void preloadRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(655360);
            this.mHandler.obtainMessage(655360, 0, 0, null).sendToTarget();
        }
    }

    public void cancelPreloadRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(720896);
            this.mHandler.obtainMessage(720896, 0, 0, null).sendToTarget();
        }
    }

    public void setWindowState(int window, int state) {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(786432, window, state, null).sendToTarget();
        }
    }

    public void buzzBeepBlinked() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(983040);
            this.mHandler.sendEmptyMessage(983040);
        }
    }

    public void notificationLightOff() {
        synchronized (this.mList) {
            this.mHandler.sendEmptyMessage(1048576);
        }
    }

    public void notificationLightPulse(int argb, int onMillis, int offMillis) {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(1114112, onMillis, offMillis, Integer.valueOf(argb)).sendToTarget();
        }
    }

    public void showScreenPinningRequest() {
        synchronized (this.mList) {
            this.mHandler.sendEmptyMessage(1179648);
        }
    }

    public void appTransitionPending() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(1245184);
            this.mHandler.sendEmptyMessage(1245184);
        }
    }

    public void appTransitionCancelled() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(1245184);
            this.mHandler.sendEmptyMessage(1245184);
        }
    }

    public void appTransitionStarting(long startTime, long duration) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(1376256);
            this.mHandler.obtainMessage(1376256, Pair.create(Long.valueOf(startTime), Long.valueOf(duration))).sendToTarget();
        }
    }

    public void showAssistDisclosure() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(1441792);
            this.mHandler.obtainMessage(1441792).sendToTarget();
        }
    }

    public void startAssist(Bundle args) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(1507328);
            this.mHandler.obtainMessage(1507328, args).sendToTarget();
        }
    }

    public void showDefaultAccountStatus(int subId) {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(1572864, Integer.valueOf(subId)).sendToTarget();
        }
    }

    public void hideDefaultAccountStatus() {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(1638400, null).sendToTarget();
        }
    }
}
