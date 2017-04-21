package com.android.systemui.recents.views;

import android.graphics.Rect;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.List;

public class RecentsViewLayoutAlgorithm {
    RecentsConfiguration mConfig;

    public RecentsViewLayoutAlgorithm(RecentsConfiguration config) {
        this.mConfig = config;
    }

    private int getRelativeCoordinate(int availableOffset, int availableSize, int otherCoord, int otherSize) {
        return ((int) (((float) availableSize) * (((float) otherCoord) / ((float) otherSize)))) + availableOffset;
    }

    List<Rect> computeStackRects(List<TaskStackView> stackViews, Rect availableBounds) {
        ArrayList<Rect> bounds = new ArrayList(stackViews.size());
        int stackViewsCount = stackViews.size();
        for (int i = 0; i < stackViewsCount; i++) {
            TaskStack stack = ((TaskStackView) stackViews.get(i)).getStack();
            Rect sb = stack.stackBounds;
            Rect db = stack.displayBounds;
            Rect ab = availableBounds;
            bounds.add(new Rect(getRelativeCoordinate(ab.left, ab.width(), sb.left, db.width()), getRelativeCoordinate(ab.top, ab.height(), sb.top, db.height()), getRelativeCoordinate(ab.left, ab.width(), sb.right, db.width()), getRelativeCoordinate(ab.top, ab.height(), sb.bottom, db.height())));
        }
        return bounds;
    }
}
