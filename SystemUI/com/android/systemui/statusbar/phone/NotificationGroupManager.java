package com.android.systemui.statusbar.phone;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData.Entry;
import java.util.HashMap;
import java.util.HashSet;

public class NotificationGroupManager {
    private int mBarState = -1;
    private final HashMap<String, NotificationGroup> mGroupMap = new HashMap();
    private OnGroupChangeListener mListener;

    public static class NotificationGroup {
        public final HashSet<Entry> children = new HashSet();
        public boolean expanded;
        public Entry summary;
    }

    public interface OnGroupChangeListener {
        void onGroupCreatedFromChildren(NotificationGroup notificationGroup);

        void onGroupExpansionChanged(ExpandableNotificationRow expandableNotificationRow, boolean z);

        void onGroupsProhibitedChanged();
    }

    public void setOnGroupChangeListener(OnGroupChangeListener listener) {
        this.mListener = listener;
    }

    public void setGroupExpanded(StatusBarNotification sbn, boolean expanded) {
        NotificationGroup group = (NotificationGroup) this.mGroupMap.get(sbn.getGroupKey());
        if (group != null) {
            setGroupExpanded(group, expanded);
        }
    }

    private void setGroupExpanded(NotificationGroup group, boolean expanded) {
        group.expanded = expanded;
        if (group.summary != null) {
            this.mListener.onGroupExpansionChanged(group.summary.row, expanded);
        }
    }

    public void onEntryRemoved(Entry removed) {
        onEntryRemovedInternal(removed, removed.notification);
    }

    private void onEntryRemovedInternal(Entry removed, final StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        String groupKey = sbn.getGroupKey();
        final NotificationGroup group = (NotificationGroup) this.mGroupMap.get(groupKey);
        if (group != null) {
            if (notif.isGroupSummary()) {
                group.summary = null;
            } else {
                group.children.remove(removed);
            }
            if (group.children.isEmpty()) {
                if (group.summary == null) {
                    this.mGroupMap.remove(groupKey);
                } else if (group.expanded) {
                    removed.row.post(new Runnable() {
                        public void run() {
                            if (group.children.isEmpty()) {
                                NotificationGroupManager.this.setGroupExpanded(sbn, false);
                            }
                        }
                    });
                } else {
                    group.summary.row.updateExpandButton();
                }
            }
        }
    }

    public void onEntryAdded(Entry added) {
        StatusBarNotification sbn = added.notification;
        Notification notif = sbn.getNotification();
        String groupKey = sbn.getGroupKey();
        NotificationGroup group = (NotificationGroup) this.mGroupMap.get(groupKey);
        if (group == null) {
            group = new NotificationGroup();
            this.mGroupMap.put(groupKey, group);
        }
        if (notif.isGroupSummary()) {
            group.summary = added;
            group.expanded = added.row.areChildrenExpanded();
            if (!group.children.isEmpty()) {
                this.mListener.onGroupCreatedFromChildren(group);
                return;
            }
            return;
        }
        group.children.add(added);
        if (group.summary != null && group.children.size() == 1 && !group.expanded) {
            group.summary.row.updateExpandButton();
        }
    }

    public void onEntryUpdated(Entry entry, StatusBarNotification oldNotification) {
        if (this.mGroupMap.get(oldNotification.getGroupKey()) != null) {
            onEntryRemovedInternal(entry, oldNotification);
        }
        onEntryAdded(entry);
    }

    public boolean isVisible(StatusBarNotification sbn) {
        if (!sbn.getNotification().isGroupChild()) {
            return true;
        }
        NotificationGroup group = (NotificationGroup) this.mGroupMap.get(sbn.getGroupKey());
        if (group == null || (!group.expanded && group.summary != null)) {
            return false;
        }
        return true;
    }

    public boolean hasGroupChildren(StatusBarNotification sbn) {
        boolean z = false;
        if (areGroupsProhibited() || !sbn.getNotification().isGroupSummary()) {
            return false;
        }
        NotificationGroup group = (NotificationGroup) this.mGroupMap.get(sbn.getGroupKey());
        if (group == null) {
            return false;
        }
        if (!group.children.isEmpty()) {
            z = true;
        }
        return z;
    }

    public void setStatusBarState(int newState) {
        if (this.mBarState != newState) {
            boolean prohibitedBefore = areGroupsProhibited();
            this.mBarState = newState;
            boolean nowProhibited = areGroupsProhibited();
            if (nowProhibited != prohibitedBefore) {
                if (nowProhibited) {
                    for (NotificationGroup group : this.mGroupMap.values()) {
                        if (group.expanded) {
                            setGroupExpanded(group, false);
                        }
                    }
                }
                this.mListener.onGroupsProhibitedChanged();
            }
        }
    }

    private boolean areGroupsProhibited() {
        return this.mBarState == 1;
    }

    public boolean isChildInGroupWithSummary(StatusBarNotification sbn) {
        if (!sbn.getNotification().isGroupChild()) {
            return false;
        }
        NotificationGroup group = (NotificationGroup) this.mGroupMap.get(sbn.getGroupKey());
        if (group == null || group.summary == null) {
            return false;
        }
        return true;
    }

    public ExpandableNotificationRow getGroupSummary(StatusBarNotification sbn) {
        NotificationGroup group = (NotificationGroup) this.mGroupMap.get(sbn.getGroupKey());
        if (group == null || group.summary == null) {
            return null;
        }
        return group.summary.row;
    }
}
