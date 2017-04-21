package com.android.systemui.statusbar;

import android.os.SystemClock;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.view.View;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NotificationData {
    private final ArrayMap<String, Entry> mEntries = new ArrayMap();
    private final Environment mEnvironment;
    private NotificationGroupManager mGroupManager;
    private HeadsUpManager mHeadsUpManager;
    private final Comparator<Entry> mRankingComparator = new Comparator<Entry>() {
        private final Ranking mRankingA = new Ranking();
        private final Ranking mRankingB = new Ranking();

        public int compare(Entry a, Entry b) {
            boolean -wrap0;
            boolean -wrap02;
            StatusBarNotification na = a.notification;
            StatusBarNotification nb = b.notification;
            int aPriority = na.getNotification().priority;
            int bPriority = nb.getNotification().priority;
            String mediaNotification = NotificationData.this.mEnvironment.getCurrentMediaNotificationKey();
            boolean aMedia = a.key.equals(mediaNotification) ? aPriority > -2 : false;
            boolean bMedia = b.key.equals(mediaNotification) ? bPriority > -2 : false;
            if (aPriority >= 2) {
                -wrap0 = NotificationData.isSystemNotification(na);
            } else {
                -wrap0 = false;
            }
            if (bPriority >= 2) {
                -wrap02 = NotificationData.isSystemNotification(nb);
            } else {
                -wrap02 = false;
            }
            int d = nb.getScore() - na.getScore();
            boolean isHeadsUp = a.row.isHeadsUp();
            if (isHeadsUp != b.row.isHeadsUp()) {
                int i;
                if (isHeadsUp) {
                    i = -1;
                } else {
                    i = 1;
                }
                return i;
            } else if (isHeadsUp) {
                return NotificationData.this.mHeadsUpManager.compare(a, b);
            } else {
                if (aMedia != bMedia) {
                    return aMedia ? -1 : 1;
                } else if (-wrap0 != -wrap02) {
                    return -wrap0 ? -1 : 1;
                } else if (NotificationData.this.mRankingMap != null) {
                    NotificationData.this.mRankingMap.getRanking(a.key, this.mRankingA);
                    NotificationData.this.mRankingMap.getRanking(b.key, this.mRankingB);
                    return this.mRankingA.getRank() - this.mRankingB.getRank();
                } else if (d != 0) {
                    return d;
                } else {
                    return (int) (nb.getNotification().when - na.getNotification().when);
                }
            }
        }
    };
    private RankingMap mRankingMap;
    private final ArrayList<Entry> mSortedAndFiltered = new ArrayList();
    private final Ranking mTmpRanking = new Ranking();

    public interface Environment {
        String getCurrentMediaNotificationKey();

        NotificationGroupManager getGroupManager();

        boolean isDeviceProvisioned();

        boolean isNotificationForCurrentProfiles(StatusBarNotification statusBarNotification);

        boolean shouldHideSensitiveContents(int i);
    }

    public static final class Entry {
        public boolean autoRedacted;
        public StatusBarIconView icon;
        private boolean interruption;
        public String key;
        private long lastFullScreenIntentLaunchTime = -2000;
        public boolean legacy;
        public StatusBarNotification notification;
        public ExpandableNotificationRow row;
        public int targetSdk;

        public Entry(StatusBarNotification n, StatusBarIconView ic) {
            this.key = n.getKey();
            this.notification = n;
            this.icon = ic;
        }

        public void setInterruption() {
            this.interruption = true;
        }

        public boolean hasInterrupted() {
            return this.interruption;
        }

        public void reset() {
            this.autoRedacted = false;
            this.legacy = false;
            this.lastFullScreenIntentLaunchTime = -2000;
            if (this.row != null) {
                this.row.reset();
            }
        }

        public View getContentView() {
            return this.row.getPrivateLayout().getContractedChild();
        }

        public View getExpandedContentView() {
            return this.row.getPrivateLayout().getExpandedChild();
        }

        public View getHeadsUpContentView() {
            return this.row.getPrivateLayout().getHeadsUpChild();
        }

        public View getPublicContentView() {
            return this.row.getPublicLayout().getContractedChild();
        }

        public void notifyFullScreenIntentLaunched() {
            this.lastFullScreenIntentLaunchTime = SystemClock.elapsedRealtime();
        }

        public boolean hasJustLaunchedFullScreenIntent() {
            return SystemClock.elapsedRealtime() < this.lastFullScreenIntentLaunchTime + 2000;
        }
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public NotificationData(Environment environment) {
        this.mEnvironment = environment;
        this.mGroupManager = environment.getGroupManager();
    }

    public ArrayList<Entry> getActiveNotifications() {
        return this.mSortedAndFiltered;
    }

    public Entry get(String key) {
        return (Entry) this.mEntries.get(key);
    }

    public void add(Entry entry, RankingMap ranking) {
        this.mEntries.put(entry.notification.getKey(), entry);
        updateRankingAndSort(ranking);
        this.mGroupManager.onEntryAdded(entry);
    }

    public Entry remove(String key, RankingMap ranking) {
        Entry removed = (Entry) this.mEntries.remove(key);
        if (removed == null) {
            return null;
        }
        updateRankingAndSort(ranking);
        this.mGroupManager.onEntryRemoved(removed);
        return removed;
    }

    public void updateRanking(RankingMap ranking) {
        updateRankingAndSort(ranking);
    }

    public boolean isAmbient(String key) {
        if (this.mRankingMap == null) {
            return false;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.isAmbient();
    }

    public int getVisibilityOverride(String key) {
        if (this.mRankingMap == null) {
            return -1000;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.getVisibilityOverride();
    }

    private void updateRankingAndSort(RankingMap ranking) {
        if (ranking != null) {
            this.mRankingMap = ranking;
        }
        filterAndSort();
    }

    public void filterAndSort() {
        this.mSortedAndFiltered.clear();
        int N = this.mEntries.size();
        for (int i = 0; i < N; i++) {
            Entry entry = (Entry) this.mEntries.valueAt(i);
            if (!shouldFilterOut(entry.notification)) {
                this.mSortedAndFiltered.add(entry);
            }
        }
        Collections.sort(this.mSortedAndFiltered, this.mRankingComparator);
    }

    boolean shouldFilterOut(StatusBarNotification sbn) {
        boolean z;
        if (this.mEnvironment.isDeviceProvisioned()) {
            z = true;
        } else {
            z = showNotificationEvenIfUnprovisioned(sbn);
        }
        if (!z || !this.mEnvironment.isNotificationForCurrentProfiles(sbn)) {
            return true;
        }
        if (sbn.getNotification().visibility == -1 && this.mEnvironment.shouldHideSensitiveContents(sbn.getUserId())) {
            return true;
        }
        if (BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS || !this.mGroupManager.isChildInGroupWithSummary(sbn)) {
            return false;
        }
        return true;
    }

    public boolean hasActiveClearableNotifications() {
        for (Entry e : this.mSortedAndFiltered) {
            if (e.getContentView() != null && e.notification.isClearable()) {
                return true;
            }
        }
        return false;
    }

    public static boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        if ("android".equals(sbn.getPackageName())) {
            return sbn.getNotification().extras.getBoolean("android.allowDuringSetup");
        }
        return false;
    }

    public void dump(PrintWriter pw, String indent) {
        int N = this.mSortedAndFiltered.size();
        pw.print(indent);
        pw.println("active notifications: " + N);
        int active = 0;
        while (active < N) {
            dumpEntry(pw, indent, active, (Entry) this.mSortedAndFiltered.get(active));
            active++;
        }
        int M = this.mEntries.size();
        pw.print(indent);
        pw.println("inactive notifications: " + (M - active));
        int inactiveCount = 0;
        for (int i = 0; i < M; i++) {
            Entry entry = (Entry) this.mEntries.valueAt(i);
            if (!this.mSortedAndFiltered.contains(entry)) {
                dumpEntry(pw, indent, inactiveCount, entry);
                inactiveCount++;
            }
        }
    }

    private void dumpEntry(PrintWriter pw, String indent, int i, Entry e) {
        pw.print(indent);
        pw.println("  [" + i + "] key=" + e.key + " icon=" + e.icon);
        StatusBarNotification n = e.notification;
        pw.print(indent);
        pw.println("      pkg=" + n.getPackageName() + " id=" + n.getId() + " score=" + n.getScore());
        pw.print(indent);
        pw.println("      notification=" + n.getNotification());
        pw.print(indent);
        pw.println("      tickerText=\"" + n.getNotification().tickerText + "\"");
    }

    private static boolean isSystemNotification(StatusBarNotification sbn) {
        String sbnPackage = sbn.getPackageName();
        return !"android".equals(sbnPackage) ? "com.android.systemui".equals(sbnPackage) : true;
    }
}
