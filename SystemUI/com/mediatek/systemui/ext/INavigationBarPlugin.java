package com.mediatek.systemui.ext;

import android.graphics.drawable.Drawable;

public interface INavigationBarPlugin {
    Drawable getBackImage(Drawable drawable);

    Drawable getBackImeImage(Drawable drawable);

    Drawable getBackImelandImage(Drawable drawable);

    Drawable getBackLandImage(Drawable drawable);

    Drawable getHomeImage(Drawable drawable);

    Drawable getHomeLandImage(Drawable drawable);

    Drawable getRecentImage(Drawable drawable);

    Drawable getRecentLandImage(Drawable drawable);
}
