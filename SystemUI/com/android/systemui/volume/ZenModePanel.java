package com.android.systemui.volume;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.assis.app.MAIN.CONSTANT;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class ZenModePanel extends LinearLayout {
    private static final boolean DEBUG = Log.isLoggable("ZenModePanel", 3);
    private static final int DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);
    private static final int MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
    private static final int[] MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
    public static final Intent ZEN_PRIORITY_SETTINGS = new Intent("android.settings.ZEN_MODE_PRIORITY_SETTINGS");
    public static final Intent ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    private boolean mAttached;
    private int mAttachedZen;
    private int mBucketIndex = -1;
    private Callback mCallback;
    private Condition[] mConditions;
    private final Context mContext;
    private ZenModeController mController;
    private boolean mCountdownConditionSupported;
    private Condition mExitCondition;
    private boolean mExpanded;
    private int mFirstConditionIndex;
    private final Uri mForeverId;
    private final H mHandler = new H();
    private boolean mHidden;
    private final IconPulser mIconPulser;
    private final LayoutInflater mInflater;
    private final com.android.systemui.volume.Interaction.Callback mInteractionCallback = new com.android.systemui.volume.Interaction.Callback() {
        public void onInteraction() {
            ZenModePanel.this.fireInteraction();
        }
    };
    private int mMaxConditions;
    private int mMaxOptionalConditions;
    private final ZenPrefs mPrefs;
    private boolean mRequestingConditions;
    private Condition mSessionExitCondition;
    private int mSessionZen;
    private final SpTexts mSpTexts;
    private String mTag = ("ZenModePanel/" + Integer.toHexString(System.identityHashCode(this)));
    private Condition mTimeCondition;
    private final TransitionHelper mTransitionHelper = new TransitionHelper();
    private boolean mVoiceCapable;
    private TextView mZenAlarmWarning;
    private SegmentedButtons mZenButtons;
    private final com.android.systemui.volume.SegmentedButtons.Callback mZenButtonsCallback = new com.android.systemui.volume.SegmentedButtons.Callback() {
        public void onSelected(Object value, boolean fromClick) {
            if (value != null && ZenModePanel.this.mZenButtons.isShown() && ZenModePanel.this.isAttachedToWindow()) {
                final int zen = ((Integer) value).intValue();
                if (fromClick) {
                    MetricsLogger.action(ZenModePanel.this.mContext, 165, zen);
                }
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "mZenButtonsCallback selected=" + zen);
                }
                final Uri realConditionId = ZenModePanel.this.getRealConditionId(ZenModePanel.this.mSessionExitCondition);
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        ZenModePanel.this.mController.setZen(zen, realConditionId, "ZenModePanel.selectZen");
                        if (zen != 0) {
                            Prefs.putInt(ZenModePanel.this.mContext, "DndFavoriteZen", zen);
                        }
                    }
                });
            }
        }

        public void onInteraction() {
            ZenModePanel.this.fireInteraction();
        }
    };
    private final com.android.systemui.statusbar.policy.ZenModeController.Callback mZenCallback = new com.android.systemui.statusbar.policy.ZenModeController.Callback() {
        public void onConditionsChanged(Condition[] conditions) {
            ZenModePanel.this.mHandler.obtainMessage(1, conditions).sendToTarget();
        }

        public void onManualRuleChanged(ZenRule rule) {
            ZenModePanel.this.mHandler.obtainMessage(2, rule).sendToTarget();
        }
    };
    private LinearLayout mZenConditions;
    private View mZenIntroduction;
    private View mZenIntroductionConfirm;
    private TextView mZenIntroductionCustomize;
    private TextView mZenIntroductionMessage;

    public interface Callback {
        void onExpanded(boolean z);

        void onInteraction();

        void onPrioritySettings();
    }

    private static class ConditionTag {
        Condition condition;
        TextView line1;
        TextView line2;
        View lines;
        RadioButton rb;

        private ConditionTag() {
        }
    }

    private final class H extends Handler {
        private H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ZenModePanel.this.handleUpdateConditions((Condition[]) msg.obj);
                    return;
                case 2:
                    ZenModePanel.this.handleUpdateManualRule((ZenRule) msg.obj);
                    return;
                case 3:
                    ZenModePanel.this.updateWidgets();
                    return;
                default:
                    return;
            }
        }
    }

    private final class TransitionHelper implements TransitionListener, Runnable {
        private boolean mPendingUpdateConditions;
        private boolean mPendingUpdateWidgets;
        private boolean mTransitioning;
        private final ArraySet<View> mTransitioningViews;

        private TransitionHelper() {
            this.mTransitioningViews = new ArraySet();
        }

        public void clear() {
            this.mTransitioningViews.clear();
            this.mPendingUpdateWidgets = false;
            this.mPendingUpdateConditions = false;
        }

        public void pendingUpdateConditions() {
            this.mPendingUpdateConditions = true;
        }

        public void pendingUpdateWidgets() {
            this.mPendingUpdateWidgets = true;
        }

        public boolean isTransitioning() {
            return !this.mTransitioningViews.isEmpty();
        }

        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            this.mTransitioningViews.add(view);
            updateTransitioning();
        }

        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            this.mTransitioningViews.remove(view);
            updateTransitioning();
        }

        public void run() {
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "TransitionHelper run mPendingUpdateWidgets=" + this.mPendingUpdateWidgets + " mPendingUpdateConditions=" + this.mPendingUpdateConditions);
            }
            if (this.mPendingUpdateWidgets) {
                ZenModePanel.this.updateWidgets();
            }
            if (this.mPendingUpdateConditions) {
                ZenModePanel.this.handleUpdateConditions();
            }
            this.mPendingUpdateConditions = false;
            this.mPendingUpdateWidgets = false;
        }

        private void updateTransitioning() {
            boolean transitioning = isTransitioning();
            if (this.mTransitioning != transitioning) {
                this.mTransitioning = transitioning;
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "TransitionHelper mTransitioning=" + this.mTransitioning);
                }
                if (!this.mTransitioning) {
                    if (this.mPendingUpdateConditions || this.mPendingUpdateWidgets) {
                        ZenModePanel.this.mHandler.post(this);
                    } else {
                        this.mPendingUpdateWidgets = false;
                        this.mPendingUpdateConditions = false;
                    }
                }
            }
        }
    }

    private final class ZenPrefs implements OnSharedPreferenceChangeListener {
        private boolean mConfirmedPriorityIntroduction;
        private boolean mConfirmedSilenceIntroduction;
        private int mMinuteIndex;
        private final int mNoneDangerousThreshold;
        private int mNoneSelected;

        private ZenPrefs() {
            this.mNoneDangerousThreshold = ZenModePanel.this.mContext.getResources().getInteger(R.integer.zen_mode_alarm_warning_threshold);
            Prefs.registerListener(ZenModePanel.this.mContext, this);
            updateMinuteIndex();
            updateNoneSelected();
            updateConfirmedPriorityIntroduction();
            updateConfirmedSilenceIntroduction();
        }

        public void trackNoneSelected() {
            this.mNoneSelected = clampNoneSelected(this.mNoneSelected + 1);
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Setting none selected: " + this.mNoneSelected + " threshold=" + this.mNoneDangerousThreshold);
            }
            Prefs.putInt(ZenModePanel.this.mContext, "DndNoneSelected", this.mNoneSelected);
        }

        public int getMinuteIndex() {
            return this.mMinuteIndex;
        }

        public void setMinuteIndex(int minuteIndex) {
            minuteIndex = clampIndex(minuteIndex);
            if (minuteIndex != this.mMinuteIndex) {
                this.mMinuteIndex = clampIndex(minuteIndex);
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "Setting favorite minute index: " + this.mMinuteIndex);
                }
                Prefs.putInt(ZenModePanel.this.mContext, "DndCountdownMinuteIndex", this.mMinuteIndex);
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updateMinuteIndex();
            updateNoneSelected();
            updateConfirmedPriorityIntroduction();
            updateConfirmedSilenceIntroduction();
        }

        private void updateMinuteIndex() {
            this.mMinuteIndex = clampIndex(Prefs.getInt(ZenModePanel.this.mContext, "DndCountdownMinuteIndex", ZenModePanel.DEFAULT_BUCKET_INDEX));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Favorite minute index: " + this.mMinuteIndex);
            }
        }

        private int clampIndex(int index) {
            return MathUtils.constrain(index, -1, ZenModePanel.MINUTE_BUCKETS.length - 1);
        }

        private void updateNoneSelected() {
            this.mNoneSelected = clampNoneSelected(Prefs.getInt(ZenModePanel.this.mContext, "DndNoneSelected", 0));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "None selected: " + this.mNoneSelected);
            }
        }

        private int clampNoneSelected(int noneSelected) {
            return MathUtils.constrain(noneSelected, 0, Integer.MAX_VALUE);
        }

        private void updateConfirmedPriorityIntroduction() {
            boolean confirmed = Prefs.getBoolean(ZenModePanel.this.mContext, "DndConfirmedPriorityIntroduction", false);
            if (confirmed != this.mConfirmedPriorityIntroduction) {
                this.mConfirmedPriorityIntroduction = confirmed;
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "Confirmed priority introduction: " + this.mConfirmedPriorityIntroduction);
                }
            }
        }

        private void updateConfirmedSilenceIntroduction() {
            boolean confirmed = Prefs.getBoolean(ZenModePanel.this.mContext, "DndConfirmedSilenceIntroduction", false);
            if (confirmed != this.mConfirmedSilenceIntroduction) {
                this.mConfirmedSilenceIntroduction = confirmed;
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "Confirmed silence introduction: " + this.mConfirmedSilenceIntroduction);
                }
            }
        }
    }

    static {
        int[] iArr;
        if (DEBUG) {
            iArr = new int[]{0, 1, 2, 5, 15, 30, 45, 60, 120, 180, 240, 480};
        } else {
            iArr = ZenModeConfig.MINUTE_BUCKETS;
        }
        MINUTE_BUCKETS = iArr;
    }

    public ZenModePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mPrefs = new ZenPrefs();
        this.mInflater = LayoutInflater.from(this.mContext.getApplicationContext());
        this.mIconPulser = new IconPulser(this.mContext);
        this.mForeverId = Condition.newId(this.mContext).appendPath("forever").build();
        this.mSpTexts = new SpTexts(this.mContext);
        this.mVoiceCapable = Util.isVoiceCapable(this.mContext);
        if (DEBUG) {
            Log.d(this.mTag, "new ZenModePanel");
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mZenButtons = (SegmentedButtons) findViewById(R.id.zen_buttons);
        this.mZenButtons.addButton(R.string.interruption_level_none_twoline, R.string.interruption_level_none_with_warning, Integer.valueOf(2));
        this.mZenButtons.addButton(R.string.interruption_level_alarms_twoline, R.string.interruption_level_alarms, Integer.valueOf(3));
        this.mZenButtons.addButton(R.string.interruption_level_priority_twoline, R.string.interruption_level_priority, Integer.valueOf(1));
        this.mZenButtons.setCallback(this.mZenButtonsCallback);
        this.mZenIntroduction = findViewById(R.id.zen_introduction);
        this.mZenIntroductionMessage = (TextView) findViewById(R.id.zen_introduction_message);
        this.mSpTexts.add(this.mZenIntroductionMessage);
        this.mZenIntroductionConfirm = findViewById(R.id.zen_introduction_confirm);
        this.mZenIntroductionConfirm.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ZenModePanel.this.confirmZenIntroduction();
            }
        });
        this.mZenIntroductionCustomize = (TextView) findViewById(R.id.zen_introduction_customize);
        this.mZenIntroductionCustomize.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ZenModePanel.this.confirmZenIntroduction();
                if (ZenModePanel.this.mCallback != null) {
                    ZenModePanel.this.mCallback.onPrioritySettings();
                }
            }
        });
        this.mSpTexts.add(this.mZenIntroductionCustomize);
        this.mZenConditions = (LinearLayout) findViewById(R.id.zen_conditions);
        this.mZenAlarmWarning = (TextView) findViewById(R.id.zen_alarm_warning);
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mZenButtons != null) {
            this.mZenButtons.updateLocale();
        }
    }

    private void confirmZenIntroduction() {
        String prefKey = prefKeyForConfirmation(getSelectedZen(0));
        if (prefKey != null) {
            if (DEBUG) {
                Log.d("ZenModePanel", "confirmZenIntroduction " + prefKey);
            }
            Prefs.putBoolean(this.mContext, prefKey, true);
            this.mHandler.sendEmptyMessage(3);
        }
    }

    private static String prefKeyForConfirmation(int zen) {
        switch (zen) {
            case 1:
                return "DndConfirmedPriorityIntroduction";
            case 2:
                return "DndConfirmedSilenceIntroduction";
            default:
                return null;
        }
    }

    protected void onAttachedToWindow() {
        boolean z = true;
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onAttachedToWindow");
        }
        this.mAttached = true;
        this.mAttachedZen = getSelectedZen(-1);
        this.mSessionZen = this.mAttachedZen;
        this.mTransitionHelper.clear();
        setSessionExitCondition(copy(this.mExitCondition));
        updateWidgets();
        if (this.mHidden) {
            z = false;
        }
        setRequestingConditions(z);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onDetachedFromWindow");
        }
        checkForAttachedZenChange();
        this.mAttached = false;
        this.mAttachedZen = -1;
        this.mSessionZen = -1;
        setSessionExitCondition(null);
        setRequestingConditions(false);
        this.mTransitionHelper.clear();
    }

    private void setSessionExitCondition(Condition condition) {
        if (!Objects.equals(condition, this.mSessionExitCondition)) {
            if (DEBUG) {
                Log.d(this.mTag, "mSessionExitCondition=" + getConditionId(condition));
            }
            this.mSessionExitCondition = condition;
        }
    }

    private void checkForAttachedZenChange() {
        int selectedZen = getSelectedZen(-1);
        if (DEBUG) {
            Log.d(this.mTag, "selectedZen=" + selectedZen);
        }
        if (selectedZen != this.mAttachedZen) {
            if (DEBUG) {
                Log.d(this.mTag, "attachedZen: " + this.mAttachedZen + " -> " + selectedZen);
            }
            if (selectedZen == 2) {
                this.mPrefs.trackNoneSelected();
            }
        }
    }

    private void setExpanded(boolean expanded) {
        if (expanded != this.mExpanded) {
            if (DEBUG) {
                Log.d(this.mTag, "setExpanded " + expanded);
            }
            this.mExpanded = expanded;
            if (this.mExpanded && isShown()) {
                ensureSelection();
            }
            updateWidgets();
            fireExpanded();
        }
    }

    private void setRequestingConditions(final boolean requesting) {
        if (this.mRequestingConditions != requesting) {
            if (DEBUG) {
                Log.d(this.mTag, "setRequestingConditions " + requesting);
            }
            this.mRequestingConditions = requesting;
            if (this.mController != null) {
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        ZenModePanel.this.mController.requestConditions(requesting);
                    }
                });
            }
            if (this.mRequestingConditions) {
                this.mTimeCondition = parseExistingTimeCondition(this.mContext, this.mExitCondition);
                if (this.mTimeCondition != null) {
                    this.mBucketIndex = -1;
                } else {
                    this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                    this.mTimeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
                }
                if (DEBUG) {
                    Log.d(this.mTag, "Initial bucket index: " + this.mBucketIndex);
                }
                this.mConditions = null;
                handleUpdateConditions();
            } else {
                hideAllConditions();
            }
        }
    }

    public void init(ZenModeController controller) {
        this.mController = controller;
        this.mCountdownConditionSupported = this.mController.isCountdownConditionSupported();
        int countdownDelta = this.mCountdownConditionSupported ? 1 : 0;
        this.mFirstConditionIndex = countdownDelta + 1;
        int minConditions = countdownDelta + 1;
        this.mMaxConditions = MathUtils.constrain(this.mContext.getResources().getInteger(R.integer.zen_mode_max_conditions), minConditions, 100);
        this.mMaxOptionalConditions = this.mMaxConditions - minConditions;
        for (int i = 0; i < this.mMaxConditions; i++) {
            this.mZenConditions.addView(this.mInflater.inflate(R.layout.zen_mode_condition, this, false));
        }
        this.mSessionZen = getSelectedZen(-1);
        handleUpdateManualRule(this.mController.getManualRule());
        if (DEBUG) {
            Log.d(this.mTag, "init mExitCondition=" + this.mExitCondition);
        }
        hideAllConditions();
        this.mController.addCallback(this.mZenCallback);
    }

    private void setExitCondition(Condition exitCondition) {
        if (!Objects.equals(this.mExitCondition, exitCondition)) {
            this.mExitCondition = exitCondition;
            if (DEBUG) {
                Log.d(this.mTag, "mExitCondition=" + getConditionId(this.mExitCondition));
            }
            updateWidgets();
        }
    }

    private static Uri getConditionId(Condition condition) {
        return condition != null ? condition.id : null;
    }

    private Uri getRealConditionId(Condition condition) {
        return isForever(condition) ? null : getConditionId(condition);
    }

    private static boolean sameConditionId(Condition lhs, Condition rhs) {
        return lhs == null ? rhs == null : rhs != null ? lhs.id.equals(rhs.id) : false;
    }

    private static Condition copy(Condition condition) {
        return condition == null ? null : condition.copy();
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    private void handleUpdateManualRule(ZenRule rule) {
        handleUpdateZen(rule != null ? rule.zenMode : 0);
        handleExitConditionChanged(rule != null ? rule.condition : null);
    }

    private void handleUpdateZen(int zen) {
        if (!(this.mSessionZen == -1 || this.mSessionZen == zen)) {
            setExpanded(isShown());
            this.mSessionZen = zen;
        }
        this.mZenButtons.setSelectedValue(Integer.valueOf(zen), false);
        updateWidgets();
        handleUpdateConditions();
        if (this.mExpanded) {
            Condition selected = getSelectedCondition();
            if (!Objects.equals(this.mExitCondition, selected)) {
                select(selected);
            }
        }
    }

    private void handleExitConditionChanged(Condition exitCondition) {
        setExitCondition(exitCondition);
        if (DEBUG) {
            Log.d(this.mTag, "handleExitConditionChanged " + this.mExitCondition);
        }
        int N = getVisibleConditions();
        for (int i = 0; i < N; i++) {
            ConditionTag tag = getConditionTagAt(i);
            if (tag != null && sameConditionId(tag.condition, this.mExitCondition)) {
                bind(exitCondition, this.mZenConditions.getChildAt(i));
            }
        }
    }

    private Condition getSelectedCondition() {
        int N = getVisibleConditions();
        for (int i = 0; i < N; i++) {
            ConditionTag tag = getConditionTagAt(i);
            if (tag != null && tag.rb.isChecked()) {
                return tag.condition;
            }
        }
        return null;
    }

    private int getSelectedZen(int defValue) {
        Object zen = this.mZenButtons.getSelectedValue();
        return zen != null ? ((Integer) zen).intValue() : defValue;
    }

    private void updateWidgets() {
        int i = 0;
        if (this.mTransitionHelper.isTransitioning()) {
            this.mTransitionHelper.pendingUpdateWidgets();
            return;
        }
        int i2;
        int zen = getSelectedZen(0);
        boolean zenImportant = zen == 1;
        boolean zenNone = zen == 2;
        boolean introduction = (!zenImportant || this.mPrefs.mConfirmedPriorityIntroduction) ? zenNone && !this.mPrefs.mConfirmedSilenceIntroduction : true;
        SegmentedButtons segmentedButtons = this.mZenButtons;
        if (this.mHidden) {
            i2 = 8;
        } else {
            i2 = 0;
        }
        segmentedButtons.setVisibility(i2);
        View view = this.mZenIntroduction;
        if (introduction) {
            i2 = 0;
        } else {
            i2 = 8;
        }
        view.setVisibility(i2);
        if (introduction) {
            TextView textView = this.mZenIntroductionMessage;
            if (zenImportant) {
                i2 = R.string.zen_priority_introduction;
            } else if (this.mVoiceCapable) {
                i2 = R.string.zen_silence_introduction_voice;
            } else {
                i2 = R.string.zen_silence_introduction;
            }
            textView.setText(i2);
            textView = this.mZenIntroductionCustomize;
            if (zenImportant) {
                i2 = 0;
            } else {
                i2 = 8;
            }
            textView.setVisibility(i2);
        }
        String warning = computeAlarmWarningText(zenNone);
        TextView textView2 = this.mZenAlarmWarning;
        if (warning == null) {
            i = 8;
        }
        textView2.setVisibility(i);
        this.mZenAlarmWarning.setText(warning);
    }

    private String computeAlarmWarningText(boolean zenNone) {
        if (!zenNone) {
            return null;
        }
        long now = System.currentTimeMillis();
        long nextAlarm = this.mController.getNextAlarm();
        if (nextAlarm < now) {
            return null;
        }
        int warningRes = 0;
        if (this.mSessionExitCondition != null) {
            if (!isForever(this.mSessionExitCondition)) {
                long time = ZenModeConfig.tryParseCountdownConditionId(this.mSessionExitCondition.id);
                if (time > now && nextAlarm < time) {
                    warningRes = R.string.zen_alarm_warning;
                }
                if (warningRes == 0) {
                    return null;
                }
                boolean soon = nextAlarm - now >= CONSTANT.ONE_DAY;
                boolean is24 = DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser());
                String skeleton = soon ? is24 ? "Hm" : "hma" : is24 ? "EEEHm" : "EEEhma";
                CharSequence formattedTime = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton), nextAlarm);
                String template = getResources().getString(soon ? R.string.alarm_template : R.string.alarm_template_far, new Object[]{formattedTime});
                return getResources().getString(warningRes, new Object[]{template});
            }
        }
        warningRes = R.string.zen_alarm_warning_indef;
        if (warningRes == 0) {
            return null;
        }
        if (nextAlarm - now >= CONSTANT.ONE_DAY) {
        }
        boolean is242 = DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser());
        if (soon) {
            if (is242) {
            }
        }
        CharSequence formattedTime2 = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton), nextAlarm);
        if (soon) {
        }
        String template2 = getResources().getString(soon ? R.string.alarm_template : R.string.alarm_template_far, new Object[]{formattedTime2});
        return getResources().getString(warningRes, new Object[]{template2});
    }

    private static Condition parseExistingTimeCondition(Context context, Condition condition) {
        if (condition == null) {
            return null;
        }
        long time = ZenModeConfig.tryParseCountdownConditionId(condition.id);
        if (time == 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        long span = time - now;
        if (span <= 0 || span > ((long) (MAX_BUCKET_MINUTES * 60000))) {
            return null;
        }
        return ZenModeConfig.toTimeCondition(context, time, Math.round(((float) span) / 60000.0f), now, ActivityManager.getCurrentUser(), false);
    }

    private void handleUpdateConditions(Condition[] conditions) {
        conditions = trimConditions(conditions);
        if (Arrays.equals(conditions, this.mConditions)) {
            int count = this.mConditions == null ? 0 : this.mConditions.length;
            if (DEBUG) {
                Log.d(this.mTag, "handleUpdateConditions unchanged conditionCount=" + count);
            }
            return;
        }
        this.mConditions = conditions;
        handleUpdateConditions();
    }

    private Condition[] trimConditions(Condition[] conditions) {
        if (conditions == null || conditions.length <= this.mMaxOptionalConditions) {
            return conditions;
        }
        int found = -1;
        for (int i = 0; i < conditions.length; i++) {
            Condition c = conditions[i];
            if (this.mSessionExitCondition != null && sameConditionId(this.mSessionExitCondition, c)) {
                found = i;
                break;
            }
        }
        Condition[] rt = (Condition[]) Arrays.copyOf(conditions, this.mMaxOptionalConditions);
        if (found >= this.mMaxOptionalConditions) {
            rt[this.mMaxOptionalConditions - 1] = conditions[found];
        }
        return rt;
    }

    private void handleUpdateConditions() {
        if (this.mTransitionHelper.isTransitioning()) {
            this.mTransitionHelper.pendingUpdateConditions();
            return;
        }
        int i;
        int conditionCount = this.mConditions == null ? 0 : this.mConditions.length;
        if (DEBUG) {
            Log.d(this.mTag, "handleUpdateConditions conditionCount=" + conditionCount);
        }
        bind(forever(), this.mZenConditions.getChildAt(0));
        if (this.mCountdownConditionSupported && this.mTimeCondition != null) {
            bind(this.mTimeCondition, this.mZenConditions.getChildAt(1));
        }
        for (i = 0; i < conditionCount; i++) {
            bind(this.mConditions[i], this.mZenConditions.getChildAt(this.mFirstConditionIndex + i));
        }
        for (i = this.mZenConditions.getChildCount() - 1; i > this.mFirstConditionIndex + conditionCount; i--) {
            this.mZenConditions.getChildAt(i).setVisibility(8);
        }
        if (this.mExpanded && isShown()) {
            ensureSelection();
        }
    }

    private Condition forever() {
        return new Condition(this.mForeverId, foreverSummary(this.mContext), "", "", 0, 1, 0);
    }

    private static String foreverSummary(Context context) {
        return context.getString(17040755);
    }

    private ConditionTag getConditionTagAt(int index) {
        return (ConditionTag) this.mZenConditions.getChildAt(index).getTag();
    }

    private int getVisibleConditions() {
        int rt = 0;
        int N = this.mZenConditions.getChildCount();
        for (int i = 0; i < N; i++) {
            int i2;
            if (this.mZenConditions.getChildAt(i).getVisibility() == 0) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            rt += i2;
        }
        return rt;
    }

    private void hideAllConditions() {
        int N = this.mZenConditions.getChildCount();
        for (int i = 0; i < N; i++) {
            this.mZenConditions.getChildAt(i).setVisibility(8);
        }
    }

    private void ensureSelection() {
        int visibleConditions = getVisibleConditions();
        if (visibleConditions != 0) {
            int i = 0;
            while (i < visibleConditions) {
                ConditionTag tag = getConditionTagAt(i);
                if (tag == null || !tag.rb.isChecked()) {
                    i++;
                } else {
                    if (DEBUG) {
                        Log.d(this.mTag, "Not selecting a default, checked=" + tag.condition);
                    }
                    return;
                }
            }
            ConditionTag foreverTag = getConditionTagAt(0);
            if (foreverTag != null) {
                if (DEBUG) {
                    Log.d(this.mTag, "Selecting a default");
                }
                int favoriteIndex = this.mPrefs.getMinuteIndex();
                if (favoriteIndex == -1 || !this.mCountdownConditionSupported) {
                    foreverTag.rb.setChecked(true);
                } else {
                    this.mTimeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[favoriteIndex], ActivityManager.getCurrentUser());
                    this.mBucketIndex = favoriteIndex;
                    bind(this.mTimeCondition, this.mZenConditions.getChildAt(1));
                    getConditionTagAt(1).rb.setChecked(true);
                }
            }
        }
    }

    private static boolean isCountdown(Condition c) {
        return c != null ? ZenModeConfig.isValidCountdownConditionId(c.id) : false;
    }

    private boolean isForever(Condition c) {
        return c != null ? this.mForeverId.equals(c.id) : false;
    }

    private void bind(Condition condition, View row) {
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        String line1;
        boolean enabled = condition.state == 1;
        final ConditionTag tag = row.getTag() != null ? (ConditionTag) row.getTag() : new ConditionTag();
        row.setTag(tag);
        boolean first = tag.rb == null;
        if (tag.rb == null) {
            tag.rb = (RadioButton) row.findViewById(16908289);
        }
        tag.condition = condition;
        final Uri conditionId = getConditionId(tag.condition);
        if (DEBUG) {
            Log.d(this.mTag, "bind i=" + this.mZenConditions.indexOfChild(row) + " first=" + first + " condition=" + conditionId);
        }
        tag.rb.setEnabled(enabled);
        boolean isCountdown = (this.mSessionExitCondition == null && this.mAttachedZen == 0) ? false : !sameConditionId(this.mSessionExitCondition, tag.condition) ? isCountdown(this.mSessionExitCondition) ? isCountdown(tag.condition) : false : true;
        if (isCountdown != tag.rb.isChecked()) {
            if (DEBUG) {
                Log.d(this.mTag, "bind checked=" + isCountdown + " condition=" + conditionId);
            }
            tag.rb.setChecked(isCountdown);
        }
        tag.rb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (ZenModePanel.this.mExpanded && isChecked) {
                    if (ZenModePanel.DEBUG) {
                        Log.d(ZenModePanel.this.mTag, "onCheckedChanged " + conditionId);
                    }
                    int N = ZenModePanel.this.getVisibleConditions();
                    for (int i = 0; i < N; i++) {
                        ConditionTag childTag = ZenModePanel.this.getConditionTagAt(i);
                        if (!(childTag == null || childTag == tag)) {
                            childTag.rb.setChecked(false);
                        }
                    }
                    MetricsLogger.action(ZenModePanel.this.mContext, 164);
                    ZenModePanel.this.select(tag.condition);
                    ZenModePanel.this.announceConditionSelection(tag);
                }
            }
        });
        if (tag.lines == null) {
            tag.lines = row.findViewById(16908290);
        }
        if (tag.line1 == null) {
            tag.line1 = (TextView) row.findViewById(16908308);
            this.mSpTexts.add(tag.line1);
        }
        if (tag.line2 == null) {
            tag.line2 = (TextView) row.findViewById(16908309);
            this.mSpTexts.add(tag.line2);
        }
        if (TextUtils.isEmpty(condition.line1)) {
            line1 = condition.summary;
        } else {
            line1 = condition.line1;
        }
        String line2 = condition.line2;
        tag.line1.setText(line1);
        if (TextUtils.isEmpty(line2)) {
            tag.line2.setVisibility(8);
        } else {
            tag.line2.setVisibility(0);
            tag.line2.setText(line2);
        }
        tag.lines.setEnabled(enabled);
        tag.lines.setAlpha(enabled ? 1.0f : 0.4f);
        ImageView button1 = (ImageView) row.findViewById(16908313);
        final View view = row;
        button1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ZenModePanel.this.onClickTimeButton(view, tag, false);
            }
        });
        ImageView button2 = (ImageView) row.findViewById(16908314);
        view = row;
        button2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ZenModePanel.this.onClickTimeButton(view, tag, true);
            }
        });
        tag.lines.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                tag.rb.setChecked(true);
            }
        });
        long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
        if (time > 0) {
            button1.setVisibility(0);
            button2.setVisibility(0);
            if (this.mBucketIndex > -1) {
                button1.setEnabled(this.mBucketIndex > 0);
                button2.setEnabled(this.mBucketIndex < MINUTE_BUCKETS.length + -1);
            } else {
                button1.setEnabled(time - System.currentTimeMillis() > ((long) (MIN_BUCKET_MINUTES * 60000)));
                button2.setEnabled(!Objects.equals(condition.summary, ZenModeConfig.toTimeCondition(this.mContext, MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser()).summary));
            }
            button1.setAlpha(button1.isEnabled() ? 1.0f : 0.5f);
            button2.setAlpha(button2.isEnabled() ? 1.0f : 0.5f);
        } else {
            button1.setVisibility(8);
            button2.setVisibility(8);
        }
        if (first) {
            Interaction.register(tag.rb, this.mInteractionCallback);
            Interaction.register(tag.lines, this.mInteractionCallback);
            Interaction.register(button1, this.mInteractionCallback);
            Interaction.register(button2, this.mInteractionCallback);
        }
        row.setVisibility(0);
    }

    private void announceConditionSelection(ConditionTag tag) {
        String modeText;
        switch (getSelectedZen(0)) {
            case 1:
                modeText = this.mContext.getString(R.string.interruption_level_priority);
                break;
            case 2:
                modeText = this.mContext.getString(R.string.interruption_level_none);
                break;
            case 3:
                modeText = this.mContext.getString(R.string.interruption_level_alarms);
                break;
            default:
                return;
        }
        announceForAccessibility(this.mContext.getString(R.string.zen_mode_and_condition, new Object[]{modeText, tag.line1.getText()}));
    }

    private void onClickTimeButton(View row, ConditionTag tag, boolean up) {
        MetricsLogger.action(this.mContext, 163, up);
        Condition newCondition = null;
        int N = MINUTE_BUCKETS.length;
        if (this.mBucketIndex == -1) {
            long time = ZenModeConfig.tryParseCountdownConditionId(getConditionId(tag.condition));
            long now = System.currentTimeMillis();
            int i = 0;
            while (i < N) {
                int j = up ? i : (N - 1) - i;
                int bucketMinutes = MINUTE_BUCKETS[j];
                long bucketTime = now + ((long) (60000 * bucketMinutes));
                if ((up && bucketTime > time) || (!up && bucketTime < time)) {
                    this.mBucketIndex = j;
                    newCondition = ZenModeConfig.toTimeCondition(this.mContext, bucketTime, bucketMinutes, now, ActivityManager.getCurrentUser(), false);
                    break;
                }
                i++;
            }
            if (newCondition == null) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            this.mBucketIndex = Math.max(0, Math.min(N - 1, (up ? 1 : -1) + this.mBucketIndex));
            newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        }
        this.mTimeCondition = newCondition;
        bind(this.mTimeCondition, row);
        tag.rb.setChecked(true);
        select(this.mTimeCondition);
        announceConditionSelection(tag);
    }

    private void select(Condition condition) {
        if (DEBUG) {
            Log.d(this.mTag, "select " + condition);
        }
        if (this.mSessionZen == -1 || this.mSessionZen == 0) {
            if (DEBUG) {
                Log.d(this.mTag, "Ignoring condition selection outside of manual zen");
            }
            return;
        }
        final Uri realConditionId = getRealConditionId(condition);
        if (this.mController != null) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    ZenModePanel.this.mController.setZen(ZenModePanel.this.mSessionZen, realConditionId, "ZenModePanel.selectCondition");
                }
            });
        }
        setExitCondition(condition);
        if (realConditionId == null) {
            this.mPrefs.setMinuteIndex(-1);
        } else if (isCountdown(condition) && this.mBucketIndex != -1) {
            this.mPrefs.setMinuteIndex(this.mBucketIndex);
        }
        setSessionExitCondition(copy(condition));
    }

    private void fireInteraction() {
        if (this.mCallback != null) {
            this.mCallback.onInteraction();
        }
    }

    private void fireExpanded() {
        if (this.mCallback != null) {
            this.mCallback.onExpanded(this.mExpanded);
        }
    }
}
