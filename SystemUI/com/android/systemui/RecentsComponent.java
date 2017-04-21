package com.android.systemui;

import android.view.Display;
import android.view.View;

public interface RecentsComponent {

    public interface Callbacks {
        void onVisibilityChanged(boolean z);
    }

    void cancelPreloadingRecents();

    void hideRecents(boolean z, boolean z2);

    void preloadRecents();

    void setCallback(Callbacks callbacks);

    void showNextAffiliatedTask();

    void showPrevAffiliatedTask();

    void showRecents(boolean z, View view);

    void toggleRecents(Display display, int i, View view);
}
