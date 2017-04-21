package com.android.systemui.statusbar.stack;

public class StackViewState extends ViewState {
    public boolean belowSpeedBump;
    public int clipTopAmount;
    public boolean dark;
    public boolean dimmed;
    public int height;
    public boolean hideSensitive;
    public int location;
    public int notGoneIndex;
    public int topOverLap;

    public void copyFrom(ViewState viewState) {
        super.copyFrom(viewState);
        if (viewState instanceof StackViewState) {
            StackViewState svs = (StackViewState) viewState;
            this.height = svs.height;
            this.dimmed = svs.dimmed;
            this.dark = svs.dark;
            this.hideSensitive = svs.hideSensitive;
            this.belowSpeedBump = svs.belowSpeedBump;
            this.clipTopAmount = svs.clipTopAmount;
            this.topOverLap = svs.topOverLap;
            this.notGoneIndex = svs.notGoneIndex;
            this.location = svs.location;
        }
    }
}
