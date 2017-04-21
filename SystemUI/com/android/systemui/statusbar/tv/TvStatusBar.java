package com.android.systemui.statusbar.tv;

import android.os.IBinder;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.view.View;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.NotificationData.Entry;

public class TvStatusBar extends BaseStatusBar {
    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
    }

    public void updateIcon(String slot, int index, int viewIndex, StatusBarIcon old, StatusBarIcon icon) {
    }

    public void removeIcon(String slot, int index, int viewIndex) {
    }

    public void addNotification(StatusBarNotification notification, RankingMap ranking, Entry entry) {
    }

    protected void updateNotificationRanking(RankingMap ranking) {
    }

    public void removeNotification(String key, RankingMap ranking) {
    }

    public void disable(int state1, int state2, boolean animate) {
    }

    public void animateExpandNotificationsPanel() {
    }

    public void animateCollapsePanels(int flags) {
    }

    public void setSystemUiVisibility(int vis, int mask) {
    }

    public void topAppWindowChanged(boolean visible) {
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    public void toggleRecentApps() {
    }

    public void setWindowState(int window, int state) {
    }

    public void buzzBeepBlinked() {
    }

    public void notificationLightOff() {
    }

    public void notificationLightPulse(int argb, int onMillis, int offMillis) {
    }

    protected void setAreThereNotifications() {
    }

    protected void updateNotifications() {
    }

    public View getStatusBarView() {
        return null;
    }

    public void maybeEscalateHeadsUp() {
    }

    protected boolean isPanelFullyCollapsed() {
        return false;
    }

    protected int getMaxKeyguardNotifications() {
        return 0;
    }

    public void animateExpandSettingsPanel() {
    }

    protected void createAndAddWindows() {
    }

    protected void refreshLayout(int layoutDirection) {
    }

    public void onActivated(ActivatableNotificationView view) {
    }

    public void onActivationReset(ActivatableNotificationView view) {
    }

    public void showScreenPinningRequest() {
    }

    public void appTransitionPending() {
    }

    public void appTransitionCancelled() {
    }

    public void appTransitionStarting(long startTime, long duration) {
    }

    protected void updateHeadsUp(String key, Entry entry, boolean shouldInterrupt, boolean alertAgain) {
    }

    protected void setHeadsUpUser(int newUserId) {
    }

    protected boolean isSnoozedPackage(StatusBarNotification sbn) {
        return false;
    }
}
