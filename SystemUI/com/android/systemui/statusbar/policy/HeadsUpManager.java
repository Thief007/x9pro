package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings.Global;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pools.Pool;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.TreeSet;

public class HeadsUpManager implements OnComputeInternalInsetsListener {
    private PhoneStatusBar mBar;
    private Clock mClock;
    private final Context mContext;
    private final int mDefaultSnoozeLengthMs;
    private HashSet<Entry> mEntriesToRemoveAfterExpand = new HashSet();
    private final Pool<HeadsUpEntry> mEntryPool = new Pool<HeadsUpEntry>() {
        private Stack<HeadsUpEntry> mPoolObjects = new Stack();

        public HeadsUpEntry acquire() {
            if (this.mPoolObjects.isEmpty()) {
                return new HeadsUpEntry();
            }
            return (HeadsUpEntry) this.mPoolObjects.pop();
        }

        public boolean release(HeadsUpEntry instance) {
            instance.reset();
            this.mPoolObjects.push(instance);
            return true;
        }
    };
    private final Handler mHandler = new Handler();
    private boolean mHasPinnedNotification;
    private HashMap<String, HeadsUpEntry> mHeadsUpEntries = new HashMap();
    private boolean mHeadsUpGoingAway;
    private final int mHeadsUpNotificationDecay;
    private boolean mIsExpanded;
    private boolean mIsObserving;
    private final HashSet<OnHeadsUpChangedListener> mListeners = new HashSet();
    private final int mMinimumDisplayTime;
    private final int mNotificationsTopPadding;
    private boolean mReleaseOnExpandFinish;
    private ContentObserver mSettingsObserver;
    private int mSnoozeLengthMs;
    private final ArrayMap<String, Long> mSnoozedPackages;
    private TreeSet<HeadsUpEntry> mSortedEntries = new TreeSet();
    private final int mStatusBarHeight;
    private final View mStatusBarWindowView;
    private HashSet<String> mSwipedOutKeys = new HashSet();
    private int[] mTmpTwoArray = new int[2];
    private final int mTouchAcceptanceDelay;
    private boolean mTrackingHeadsUp;
    private int mUser;
    private boolean mWaitingOnCollapseWhenGoingAway;

    public interface OnHeadsUpChangedListener {
        void onHeadsUpPinned(ExpandableNotificationRow expandableNotificationRow);

        void onHeadsUpPinnedModeChanged(boolean z);

        void onHeadsUpStateChanged(Entry entry, boolean z);

        void onHeadsUpUnPinned(ExpandableNotificationRow expandableNotificationRow);
    }

    public static class Clock {
        public long currentTimeMillis() {
            return SystemClock.elapsedRealtime();
        }
    }

    public class HeadsUpEntry implements Comparable<HeadsUpEntry> {
        public long earliestRemovaltime;
        public Entry entry;
        private Runnable mRemoveHeadsUpRunnable;
        public long postTime;

        public void setEntry(final Entry entry) {
            this.entry = entry;
            this.postTime = HeadsUpManager.this.mClock.currentTimeMillis() + ((long) HeadsUpManager.this.mTouchAcceptanceDelay);
            this.mRemoveHeadsUpRunnable = new Runnable() {
                public void run() {
                    if (HeadsUpManager.this.mTrackingHeadsUp) {
                        HeadsUpManager.this.mEntriesToRemoveAfterExpand.add(entry);
                    } else {
                        HeadsUpManager.this.removeHeadsUpEntry(entry);
                    }
                }
            };
            updateEntry();
        }

        public void updateEntry() {
            HeadsUpManager.this.mSortedEntries.remove(this);
            long currentTime = HeadsUpManager.this.mClock.currentTimeMillis();
            this.earliestRemovaltime = ((long) HeadsUpManager.this.mMinimumDisplayTime) + currentTime;
            this.postTime = Math.max(this.postTime, currentTime);
            removeAutoRemovalCallbacks();
            if (!HeadsUpManager.this.hasFullScreenIntent(this.entry)) {
                HeadsUpManager.this.mHandler.postDelayed(this.mRemoveHeadsUpRunnable, Math.max((this.postTime + ((long) HeadsUpManager.this.mHeadsUpNotificationDecay)) - currentTime, (long) HeadsUpManager.this.mMinimumDisplayTime));
            }
            HeadsUpManager.this.mSortedEntries.add(this);
        }

        public int compareTo(HeadsUpEntry o) {
            if (this.postTime < o.postTime) {
                return 1;
            }
            if (this.postTime == o.postTime) {
                return this.entry.key.compareTo(o.entry.key);
            }
            return -1;
        }

        public void removeAutoRemovalCallbacks() {
            HeadsUpManager.this.mHandler.removeCallbacks(this.mRemoveHeadsUpRunnable);
        }

        public boolean wasShownLongEnough() {
            return this.earliestRemovaltime < HeadsUpManager.this.mClock.currentTimeMillis();
        }

        public void removeAsSoonAsPossible() {
            removeAutoRemovalCallbacks();
            HeadsUpManager.this.mHandler.postDelayed(this.mRemoveHeadsUpRunnable, this.earliestRemovaltime - HeadsUpManager.this.mClock.currentTimeMillis());
        }

        public void reset() {
            removeAutoRemovalCallbacks();
            this.entry = null;
            this.mRemoveHeadsUpRunnable = null;
        }
    }

    public HeadsUpManager(final Context context, View statusBarWindowView) {
        this.mContext = context;
        Resources resources = this.mContext.getResources();
        this.mTouchAcceptanceDelay = resources.getInteger(R.integer.touch_acceptance_delay);
        this.mSnoozedPackages = new ArrayMap();
        this.mDefaultSnoozeLengthMs = resources.getInteger(R.integer.heads_up_default_snooze_length_ms);
        this.mSnoozeLengthMs = this.mDefaultSnoozeLengthMs;
        this.mMinimumDisplayTime = resources.getInteger(R.integer.heads_up_notification_minimum_time);
        this.mHeadsUpNotificationDecay = resources.getInteger(R.integer.heads_up_notification_decay);
        this.mClock = new Clock();
        this.mSnoozeLengthMs = Global.getInt(context.getContentResolver(), "heads_up_snooze_length_ms", this.mDefaultSnoozeLengthMs);
        this.mSettingsObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                int packageSnoozeLengthMs = Global.getInt(context.getContentResolver(), "heads_up_snooze_length_ms", -1);
                if (packageSnoozeLengthMs > -1 && packageSnoozeLengthMs != HeadsUpManager.this.mSnoozeLengthMs) {
                    HeadsUpManager.this.mSnoozeLengthMs = packageSnoozeLengthMs;
                    Log.v("HeadsUpManager", "mSnoozeLengthMs = " + HeadsUpManager.this.mSnoozeLengthMs);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Global.getUriFor("heads_up_snooze_length_ms"), false, this.mSettingsObserver);
        this.mStatusBarWindowView = statusBarWindowView;
        this.mStatusBarHeight = resources.getDimensionPixelSize(17104919);
        this.mNotificationsTopPadding = context.getResources().getDimensionPixelSize(R.dimen.notifications_top_padding);
    }

    private void updateTouchableRegionListener() {
        boolean shouldObserve;
        if (this.mHasPinnedNotification || this.mHeadsUpGoingAway) {
            shouldObserve = true;
        } else {
            shouldObserve = this.mWaitingOnCollapseWhenGoingAway;
        }
        if (shouldObserve != this.mIsObserving) {
            if (shouldObserve) {
                this.mStatusBarWindowView.getViewTreeObserver().addOnComputeInternalInsetsListener(this);
                this.mStatusBarWindowView.requestLayout();
            } else {
                this.mStatusBarWindowView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this);
            }
            this.mIsObserving = shouldObserve;
        }
    }

    public void setBar(PhoneStatusBar bar) {
        this.mBar = bar;
    }

    public void addListener(OnHeadsUpChangedListener listener) {
        this.mListeners.add(listener);
    }

    public void showNotification(Entry headsUp) {
        Log.v("HeadsUpManager", "showNotification");
        MetricsLogger.count(this.mContext, "note_peek", 1);
        addHeadsUpEntry(headsUp);
        updateNotification(headsUp, true);
        headsUp.setInterruption();
    }

    public void updateNotification(Entry headsUp, boolean alert) {
        Log.v("HeadsUpManager", "updateNotification");
        headsUp.row.setChildrenExpanded(false, false);
        headsUp.row.sendAccessibilityEvent(2048);
        if (alert) {
            HeadsUpEntry headsUpEntry = (HeadsUpEntry) this.mHeadsUpEntries.get(headsUp.key);
            headsUpEntry.updateEntry();
            setEntryPinned(headsUpEntry, shouldHeadsUpBecomePinned(headsUp));
        }
    }

    private void addHeadsUpEntry(Entry entry) {
        HeadsUpEntry headsUpEntry = (HeadsUpEntry) this.mEntryPool.acquire();
        headsUpEntry.setEntry(entry);
        this.mHeadsUpEntries.put(entry.key, headsUpEntry);
        entry.row.setHeadsUp(true);
        setEntryPinned(headsUpEntry, shouldHeadsUpBecomePinned(entry));
        for (OnHeadsUpChangedListener listener : this.mListeners) {
            listener.onHeadsUpStateChanged(entry, true);
        }
        entry.row.sendAccessibilityEvent(2048);
    }

    private boolean shouldHeadsUpBecomePinned(Entry entry) {
        return this.mIsExpanded ? hasFullScreenIntent(entry) : true;
    }

    private boolean hasFullScreenIntent(Entry entry) {
        return entry.notification.getNotification().fullScreenIntent != null;
    }

    private void setEntryPinned(HeadsUpEntry headsUpEntry, boolean isPinned) {
        ExpandableNotificationRow row = headsUpEntry.entry.row;
        if (row.isPinned() != isPinned) {
            row.setPinned(isPinned);
            updatePinnedMode();
            for (OnHeadsUpChangedListener listener : this.mListeners) {
                if (isPinned) {
                    listener.onHeadsUpPinned(row);
                } else {
                    listener.onHeadsUpUnPinned(row);
                }
            }
        }
    }

    private void removeHeadsUpEntry(Entry entry) {
        HeadsUpEntry remove = (HeadsUpEntry) this.mHeadsUpEntries.remove(entry.key);
        this.mSortedEntries.remove(remove);
        entry.row.sendAccessibilityEvent(2048);
        entry.row.setHeadsUp(false);
        setEntryPinned(remove, false);
        for (OnHeadsUpChangedListener listener : this.mListeners) {
            listener.onHeadsUpStateChanged(entry, false);
        }
        this.mEntryPool.release(remove);
    }

    private void updatePinnedMode() {
        boolean hasPinnedNotification = hasPinnedNotificationInternal();
        if (hasPinnedNotification != this.mHasPinnedNotification) {
            this.mHasPinnedNotification = hasPinnedNotification;
            updateTouchableRegionListener();
            for (OnHeadsUpChangedListener listener : this.mListeners) {
                listener.onHeadsUpPinnedModeChanged(hasPinnedNotification);
            }
        }
    }

    public boolean removeNotification(String key) {
        Log.v("HeadsUpManager", "remove");
        if (wasShownLongEnough(key)) {
            releaseImmediately(key);
            return true;
        }
        getHeadsUpEntry(key).removeAsSoonAsPossible();
        return false;
    }

    private boolean wasShownLongEnough(String key) {
        HeadsUpEntry headsUpEntry = getHeadsUpEntry(key);
        HeadsUpEntry topEntry = getTopEntry();
        if (this.mSwipedOutKeys.contains(key)) {
            this.mSwipedOutKeys.remove(key);
            return true;
        } else if (headsUpEntry != topEntry) {
            return true;
        } else {
            return headsUpEntry.wasShownLongEnough();
        }
    }

    public boolean isHeadsUp(String key) {
        return this.mHeadsUpEntries.containsKey(key);
    }

    public void releaseAllImmediately() {
        Log.v("HeadsUpManager", "releaseAllImmediately");
        for (String key : new ArrayList(this.mHeadsUpEntries.keySet())) {
            releaseImmediately(key);
        }
    }

    public void releaseImmediately(String key) {
        HeadsUpEntry headsUpEntry = getHeadsUpEntry(key);
        if (headsUpEntry != null) {
            removeHeadsUpEntry(headsUpEntry.entry);
        }
    }

    public boolean isSnoozed(String packageName) {
        String key = snoozeKey(packageName, this.mUser);
        Long snoozedUntil = (Long) this.mSnoozedPackages.get(key);
        if (snoozedUntil != null) {
            if (snoozedUntil.longValue() > SystemClock.elapsedRealtime()) {
                Log.v("HeadsUpManager", key + " snoozed");
                return true;
            }
            this.mSnoozedPackages.remove(packageName);
        }
        return false;
    }

    public void snooze() {
        for (String key : this.mHeadsUpEntries.keySet()) {
            this.mSnoozedPackages.put(snoozeKey(((HeadsUpEntry) this.mHeadsUpEntries.get(key)).entry.notification.getPackageName(), this.mUser), Long.valueOf(SystemClock.elapsedRealtime() + ((long) this.mSnoozeLengthMs)));
        }
        this.mReleaseOnExpandFinish = true;
    }

    private static String snoozeKey(String packageName, int user) {
        return user + "," + packageName;
    }

    private HeadsUpEntry getHeadsUpEntry(String key) {
        return (HeadsUpEntry) this.mHeadsUpEntries.get(key);
    }

    public Entry getEntry(String key) {
        return ((HeadsUpEntry) this.mHeadsUpEntries.get(key)).entry;
    }

    public TreeSet<HeadsUpEntry> getSortedEntries() {
        return this.mSortedEntries;
    }

    public HeadsUpEntry getTopEntry() {
        return this.mSortedEntries.isEmpty() ? null : (HeadsUpEntry) this.mSortedEntries.first();
    }

    public boolean shouldSwallowClick(String key) {
        HeadsUpEntry entry = (HeadsUpEntry) this.mHeadsUpEntries.get(key);
        if (entry == null || this.mClock.currentTimeMillis() >= entry.postTime) {
            return false;
        }
        return true;
    }

    public void onComputeInternalInsets(InternalInsetsInfo info) {
        if (!this.mIsExpanded) {
            if (this.mHasPinnedNotification) {
                int minX = Integer.MAX_VALUE;
                int maxX = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = 0;
                for (HeadsUpEntry entry : this.mSortedEntries) {
                    ExpandableNotificationRow row = entry.entry.row;
                    if (row.isPinned()) {
                        row.getLocationOnScreen(this.mTmpTwoArray);
                        minX = Math.min(minX, this.mTmpTwoArray[0]);
                        minY = Math.min(minY, 0);
                        maxX = Math.max(maxX, this.mTmpTwoArray[0] + row.getWidth());
                        maxY = Math.max(maxY, row.getHeadsUpHeight());
                    }
                }
                info.setTouchableInsets(3);
                info.touchableRegion.set(minX, minY, maxX, this.mNotificationsTopPadding + maxY);
            } else if (this.mHeadsUpGoingAway || this.mWaitingOnCollapseWhenGoingAway) {
                info.setTouchableInsets(3);
                info.touchableRegion.set(0, 0, this.mStatusBarWindowView.getWidth(), this.mStatusBarHeight);
            }
        }
    }

    public void setUser(int user) {
        this.mUser = user;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HeadsUpManager state:");
        pw.print("  mTouchAcceptanceDelay=");
        pw.println(this.mTouchAcceptanceDelay);
        pw.print("  mSnoozeLengthMs=");
        pw.println(this.mSnoozeLengthMs);
        pw.print("  now=");
        pw.println(SystemClock.elapsedRealtime());
        pw.print("  mUser=");
        pw.println(this.mUser);
        for (HeadsUpEntry entry : this.mSortedEntries) {
            pw.print("  HeadsUpEntry=");
            pw.println(entry.entry);
        }
        int N = this.mSnoozedPackages.size();
        pw.println("  snoozed packages: " + N);
        for (int i = 0; i < N; i++) {
            pw.print("    ");
            pw.print(this.mSnoozedPackages.valueAt(i));
            pw.print(", ");
            pw.println((String) this.mSnoozedPackages.keyAt(i));
        }
    }

    public boolean hasPinnedHeadsUp() {
        return this.mHasPinnedNotification;
    }

    private boolean hasPinnedNotificationInternal() {
        for (String key : this.mHeadsUpEntries.keySet()) {
            if (((HeadsUpEntry) this.mHeadsUpEntries.get(key)).entry.row.isPinned()) {
                return true;
            }
        }
        return false;
    }

    public void addSwipedOutNotification(String key) {
        this.mSwipedOutKeys.add(key);
    }

    public void unpinAll() {
        for (String key : this.mHeadsUpEntries.keySet()) {
            setEntryPinned((HeadsUpEntry) this.mHeadsUpEntries.get(key), false);
        }
    }

    public void onExpandingFinished() {
        if (this.mReleaseOnExpandFinish) {
            releaseAllImmediately();
            this.mReleaseOnExpandFinish = false;
        } else {
            for (Entry entry : this.mEntriesToRemoveAfterExpand) {
                removeHeadsUpEntry(entry);
            }
        }
        this.mEntriesToRemoveAfterExpand.clear();
    }

    public void setTrackingHeadsUp(boolean trackingHeadsUp) {
        this.mTrackingHeadsUp = trackingHeadsUp;
    }

    public void setIsExpanded(boolean isExpanded) {
        if (isExpanded != this.mIsExpanded) {
            this.mIsExpanded = isExpanded;
            if (isExpanded) {
                this.mWaitingOnCollapseWhenGoingAway = false;
                this.mHeadsUpGoingAway = false;
                updateTouchableRegionListener();
            }
        }
    }

    public int getTopHeadsUpHeight() {
        HeadsUpEntry topEntry = getTopEntry();
        return topEntry != null ? topEntry.entry.row.getHeadsUpHeight() : 0;
    }

    public int compare(Entry a, Entry b) {
        HeadsUpEntry aEntry = getHeadsUpEntry(a.key);
        HeadsUpEntry bEntry = getHeadsUpEntry(b.key);
        if (aEntry != null && bEntry != null) {
            return aEntry.compareTo(bEntry);
        }
        return aEntry == null ? 1 : -1;
    }

    public void setHeadsUpGoingAway(boolean headsUpGoingAway) {
        if (headsUpGoingAway != this.mHeadsUpGoingAway) {
            this.mHeadsUpGoingAway = headsUpGoingAway;
            if (!headsUpGoingAway) {
                waitForStatusBarLayout();
            }
            updateTouchableRegionListener();
        }
    }

    private void waitForStatusBarLayout() {
        this.mWaitingOnCollapseWhenGoingAway = true;
        this.mStatusBarWindowView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (HeadsUpManager.this.mStatusBarWindowView.getHeight() <= HeadsUpManager.this.mStatusBarHeight) {
                    HeadsUpManager.this.mStatusBarWindowView.removeOnLayoutChangeListener(this);
                    HeadsUpManager.this.mWaitingOnCollapseWhenGoingAway = false;
                    HeadsUpManager.this.updateTouchableRegionListener();
                }
            }
        });
    }

    public static void setIsClickedNotification(View child, boolean clicked) {
        child.setTag(R.id.is_clicked_heads_up_tag, clicked ? Boolean.valueOf(true) : null);
    }

    public static boolean isClickedHeadsUpNotification(View child) {
        Boolean clicked = (Boolean) child.getTag(R.id.is_clicked_heads_up_tag);
        return clicked != null ? clicked.booleanValue() : false;
    }
}
