package com.mediatek.systemui.statusbar.extcb;

public enum BehaviorSet {
    DEFAULT_BS(0),
    OP01_BS(1),
    OP02_BS(2),
    OP09_BS(3);
    
    private int mBehaviorSet;

    private BehaviorSet(int behaviorSet) {
        this.mBehaviorSet = behaviorSet;
    }

    public int getBehaviorSet() {
        return this.mBehaviorSet;
    }
}
