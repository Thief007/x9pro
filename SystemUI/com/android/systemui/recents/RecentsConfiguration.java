package com.android.systemui.recents;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.recents.misc.Console;
import com.android.systemui.recents.misc.SystemServicesProxy;

public class RecentsConfiguration {
    static RecentsConfiguration sInstance;
    static int sPrevConfigurationHashCode;
    public int altTabKeyDelay;
    public boolean debugModeEnabled;
    public boolean developerOptionsEnabled;
    public int dismissAllButtonSizePx;
    public Rect displayRect = new Rect();
    public boolean fakeShadows;
    public Interpolator fastOutLinearInInterpolator;
    public Interpolator fastOutSlowInInterpolator;
    public int filteringCurrentViewsAnimDuration;
    public int filteringNewViewsAnimDuration;
    boolean hasTransposedNavBar;
    boolean hasTransposedSearchBar;
    boolean isLandscape;
    public boolean launchedFromAppWithThumbnail;
    public boolean launchedFromHome;
    public boolean launchedFromSearchHome;
    public boolean launchedHasConfigurationChanged;
    public int launchedNumVisibleTasks;
    public int launchedNumVisibleThumbnails;
    public boolean launchedReuseTaskStackViews;
    public int launchedToTaskId;
    public boolean launchedWithAltTab;
    public boolean launchedWithNoRecentTasks;
    public Interpolator linearOutSlowInInterpolator;
    public boolean lockToAppEnabled;
    public int maxNumTasksToLoad;
    public boolean multiStackEnabled;
    public int navBarScrimEnterDuration;
    public Interpolator quintOutInterpolator;
    public int searchBarSpaceHeightPx;
    public int svelteLevel;
    public Rect systemInsets = new Rect();
    public int taskBarDismissDozeDelaySeconds;
    public int taskBarHeight;
    public float taskBarViewAffiliationColorMinAlpha;
    public int taskBarViewDarkTextColor;
    public int taskBarViewDefaultBackgroundColor;
    public int taskBarViewHighlightColor;
    public int taskBarViewLightTextColor;
    public int taskStackMaxDim;
    public float taskStackOverscrollPct;
    public int taskStackScrollDuration;
    public int taskStackTopPaddingPx;
    public float taskStackWidthPaddingPct;
    public int taskViewAffiliateGroupEnterOffsetPx;
    public int taskViewEnterFromAppDuration;
    public int taskViewEnterFromHomeDuration;
    public int taskViewEnterFromHomeStaggerDelay;
    public int taskViewExitToAppDuration;
    public int taskViewExitToHomeDuration;
    public int taskViewHighlightPx;
    public int taskViewRemoveAnimDuration;
    public int taskViewRemoveAnimTranslationXPx;
    public int taskViewRoundedCornerRadiusPx;
    public float taskViewThumbnailAlpha;
    public int taskViewTranslationZMaxPx;
    public int taskViewTranslationZMinPx;
    public int transitionEnterFromAppDelay;
    public int transitionEnterFromHomeDelay;
    public boolean useHardwareLayers;

    private RecentsConfiguration(Context context) {
        this.fastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.fastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
        this.linearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.quintOutInterpolator = AnimationUtils.loadInterpolator(context, 17563653);
    }

    public static RecentsConfiguration reinitialize(Context context, SystemServicesProxy ssp) {
        if (sInstance == null) {
            sInstance = new RecentsConfiguration(context);
        }
        int configHashCode = context.getResources().getConfiguration().hashCode();
        if (sPrevConfigurationHashCode != configHashCode) {
            sInstance.update(context);
            sPrevConfigurationHashCode = configHashCode;
        }
        sInstance.updateOnReinitialize(context, ssp);
        return sInstance;
    }

    public static RecentsConfiguration getInstance() {
        return sInstance;
    }

    void update(Context context) {
        boolean z = true;
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        this.debugModeEnabled = Prefs.getBoolean(context, "debugModeEnabled", false);
        if (this.debugModeEnabled) {
            Console.Enabled = true;
        }
        if (res.getConfiguration().orientation != 2) {
            z = false;
        }
        this.isLandscape = z;
        this.hasTransposedSearchBar = res.getBoolean(R.bool.recents_has_transposed_search_bar);
        this.hasTransposedNavBar = res.getBoolean(R.bool.recents_has_transposed_nav_bar);
        this.displayRect.set(0, 0, dm.widthPixels, dm.heightPixels);
        this.filteringCurrentViewsAnimDuration = res.getInteger(R.integer.recents_filter_animate_current_views_duration);
        this.filteringNewViewsAnimDuration = res.getInteger(R.integer.recents_filter_animate_new_views_duration);
        this.maxNumTasksToLoad = ActivityManager.getMaxRecentTasksStatic();
        this.searchBarSpaceHeightPx = res.getDimensionPixelSize(R.dimen.recents_search_bar_space_height);
        this.taskStackScrollDuration = res.getInteger(R.integer.recents_animate_task_stack_scroll_duration);
        this.taskStackWidthPaddingPct = res.getFloat(R.dimen.recents_stack_width_padding_percentage);
        this.taskStackOverscrollPct = res.getFloat(R.dimen.recents_stack_overscroll_percentage);
        this.taskStackMaxDim = res.getInteger(R.integer.recents_max_task_stack_view_dim);
        this.taskStackTopPaddingPx = res.getDimensionPixelSize(R.dimen.recents_stack_top_padding);
        this.dismissAllButtonSizePx = res.getDimensionPixelSize(R.dimen.recents_dismiss_all_button_size);
        this.transitionEnterFromAppDelay = res.getInteger(R.integer.recents_enter_from_app_transition_duration);
        this.transitionEnterFromHomeDelay = res.getInteger(R.integer.recents_enter_from_home_transition_duration);
        this.taskViewEnterFromAppDuration = res.getInteger(R.integer.recents_task_enter_from_app_duration);
        this.taskViewEnterFromHomeDuration = res.getInteger(R.integer.recents_task_enter_from_home_duration);
        this.taskViewEnterFromHomeStaggerDelay = res.getInteger(R.integer.recents_task_enter_from_home_stagger_delay);
        this.taskViewExitToAppDuration = res.getInteger(R.integer.recents_task_exit_to_app_duration);
        this.taskViewExitToHomeDuration = res.getInteger(R.integer.recents_task_exit_to_home_duration);
        this.taskViewRemoveAnimDuration = res.getInteger(R.integer.recents_animate_task_view_remove_duration);
        this.taskViewRemoveAnimTranslationXPx = res.getDimensionPixelSize(R.dimen.recents_task_view_remove_anim_translation_x);
        this.taskViewRoundedCornerRadiusPx = res.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.taskViewHighlightPx = res.getDimensionPixelSize(R.dimen.recents_task_view_highlight);
        this.taskViewTranslationZMinPx = res.getDimensionPixelSize(R.dimen.recents_task_view_z_min);
        this.taskViewTranslationZMaxPx = res.getDimensionPixelSize(R.dimen.recents_task_view_z_max);
        this.taskViewAffiliateGroupEnterOffsetPx = res.getDimensionPixelSize(R.dimen.recents_task_view_affiliate_group_enter_offset);
        this.taskViewThumbnailAlpha = res.getFloat(R.dimen.recents_task_view_thumbnail_alpha);
        this.taskBarViewDefaultBackgroundColor = context.getColor(R.color.recents_task_bar_default_background_color);
        this.taskBarViewLightTextColor = context.getColor(R.color.recents_task_bar_light_text_color);
        this.taskBarViewDarkTextColor = context.getColor(R.color.recents_task_bar_dark_text_color);
        this.taskBarViewHighlightColor = context.getColor(R.color.recents_task_bar_highlight_color);
        this.taskBarViewAffiliationColorMinAlpha = res.getFloat(R.dimen.recents_task_affiliation_color_min_alpha_percentage);
        this.taskBarHeight = res.getDimensionPixelSize(R.dimen.recents_task_bar_height);
        this.taskBarDismissDozeDelaySeconds = res.getInteger(R.integer.recents_task_bar_dismiss_delay_seconds);
        this.navBarScrimEnterDuration = res.getInteger(R.integer.recents_nav_bar_scrim_enter_duration);
        this.useHardwareLayers = res.getBoolean(R.bool.config_recents_use_hardware_layers);
        this.altTabKeyDelay = res.getInteger(R.integer.recents_alt_tab_key_delay);
        this.fakeShadows = res.getBoolean(R.bool.config_recents_fake_shadows);
        this.svelteLevel = res.getInteger(R.integer.recents_svelte_level);
    }

    public void updateSystemInsets(Rect insets) {
        this.systemInsets.set(insets);
    }

    void updateOnReinitialize(Context context, SystemServicesProxy ssp) {
        boolean z;
        boolean z2 = true;
        if (ssp.getGlobalSetting(context, "development_settings_enabled") != 0) {
            z = true;
        } else {
            z = false;
        }
        this.developerOptionsEnabled = z;
        if (ssp.getSystemSetting(context, "lock_to_app_enabled") == 0) {
            z2 = false;
        }
        this.lockToAppEnabled = z2;
        this.multiStackEnabled = "true".equals(ssp.getSystemProperty("persist.sys.debug.multi_window"));
    }

    public void updateOnConfigurationChange() {
        this.launchedReuseTaskStackViews = false;
        this.launchedHasConfigurationChanged = true;
    }

    public boolean shouldAnimateStatusBarScrim() {
        return this.launchedFromHome;
    }

    public boolean hasStatusBarScrim() {
        return !this.launchedWithNoRecentTasks;
    }

    public boolean shouldAnimateNavBarScrim() {
        return true;
    }

    public boolean hasNavBarScrim() {
        return (this.launchedWithNoRecentTasks || (this.hasTransposedNavBar && this.isLandscape)) ? false : true;
    }

    public void getAvailableTaskStackBounds(int windowWidth, int windowHeight, int topInset, int rightInset, Rect searchBarBounds, Rect taskStackBounds) {
        if (this.isLandscape && this.hasTransposedSearchBar) {
            taskStackBounds.set(0, topInset, windowWidth - rightInset, windowHeight);
        } else {
            taskStackBounds.set(0, searchBarBounds.bottom, windowWidth, windowHeight);
        }
    }

    public void getSearchBarBounds(int windowWidth, int windowHeight, int topInset, Rect searchBarSpaceBounds) {
        int searchBarSize = this.searchBarSpaceHeightPx;
        if (this.isLandscape && this.hasTransposedSearchBar) {
            searchBarSpaceBounds.set(0, topInset, searchBarSize, windowHeight);
        } else {
            searchBarSpaceBounds.set(0, topInset, windowWidth, topInset + searchBarSize);
        }
    }
}
