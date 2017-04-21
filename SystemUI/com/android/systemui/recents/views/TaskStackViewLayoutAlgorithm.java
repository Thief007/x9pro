package com.android.systemui.recents.views;

import android.graphics.Rect;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.Task.TaskKey;
import java.util.ArrayList;
import java.util.HashMap;

public class TaskStackViewLayoutAlgorithm {
    static float[] px;
    static float[] xp;
    int mBetweenAffiliationOffset;
    RecentsConfiguration mConfig;
    float mInitialScrollP;
    float mMaxScrollP;
    float mMinScrollP;
    Rect mStackRect = new Rect();
    Rect mStackVisibleRect = new Rect();
    HashMap<TaskKey, Float> mTaskProgressMap = new HashMap();
    Rect mTaskRect = new Rect();
    Rect mViewRect = new Rect();
    int mWithinAffiliationOffset;

    public class VisibilityReport {
        public int numVisibleTasks;
        public int numVisibleThumbnails;

        VisibilityReport(int tasks, int thumbnails) {
            this.numVisibleTasks = tasks;
            this.numVisibleThumbnails = thumbnails;
        }
    }

    public TaskStackViewLayoutAlgorithm(RecentsConfiguration config) {
        this.mConfig = config;
        initializeCurve();
    }

    public void computeRects(int windowWidth, int windowHeight, Rect taskStackBounds) {
        this.mViewRect.set(0, 0, windowWidth, windowHeight);
        this.mStackRect.set(taskStackBounds);
        this.mStackVisibleRect.set(taskStackBounds);
        this.mStackVisibleRect.bottom = this.mViewRect.bottom;
        this.mStackRect.inset((int) (this.mConfig.taskStackWidthPaddingPct * ((float) this.mStackRect.width())), this.mConfig.taskStackTopPaddingPx);
        int size = this.mStackRect.width();
        int left = this.mStackRect.left + ((this.mStackRect.width() - size) / 2);
        this.mTaskRect.set(left, this.mStackRect.top, left + size, this.mStackRect.top + size);
        this.mWithinAffiliationOffset = this.mConfig.taskBarHeight;
        this.mBetweenAffiliationOffset = (int) (((float) this.mTaskRect.height()) * 0.5f);
    }

    void computeMinMaxScroll(ArrayList<Task> tasks, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mTaskProgressMap.clear();
        if (tasks.isEmpty()) {
            this.mMaxScrollP = 0.0f;
            this.mMinScrollP = 0.0f;
            return;
        }
        int taskHeight = this.mTaskRect.height();
        float pAtBottomOfStackRect = screenYToCurveProgress(this.mStackVisibleRect.bottom);
        float pWithinAffiliateOffset = pAtBottomOfStackRect - screenYToCurveProgress((this.mStackVisibleRect.bottom - this.mWithinAffiliationOffset) + ((int) (((1.0f - curveProgressToScale(screenYToCurveProgress(this.mStackVisibleRect.bottom - this.mWithinAffiliationOffset))) * ((float) taskHeight)) / 2.0f)));
        float pBetweenAffiliateOffset = pAtBottomOfStackRect - screenYToCurveProgress(this.mStackVisibleRect.bottom - this.mBetweenAffiliationOffset);
        float pTaskHeightOffset = pAtBottomOfStackRect - screenYToCurveProgress(this.mStackVisibleRect.bottom - taskHeight);
        float pNavBarOffset = pAtBottomOfStackRect - screenYToCurveProgress(this.mStackVisibleRect.bottom - (this.mStackVisibleRect.bottom - this.mStackRect.bottom));
        float pAtFrontMostCardTop = 0.5f;
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = (Task) tasks.get(i);
            this.mTaskProgressMap.put(task.key, Float.valueOf(pAtFrontMostCardTop));
            if (i < taskCount - 1) {
                pAtFrontMostCardTop += task.group.isFrontMostTask(task) ? pBetweenAffiliateOffset : pWithinAffiliateOffset;
            }
        }
        this.mMaxScrollP = (0.0f + pAtFrontMostCardTop) - ((1.0f - pTaskHeightOffset) - pNavBarOffset);
        this.mMinScrollP = tasks.size() == 1 ? Math.max(this.mMaxScrollP, 0.0f) : 0.0f;
        if (launchedWithAltTab && launchedFromHome) {
            this.mInitialScrollP = this.mMaxScrollP;
        } else {
            this.mInitialScrollP = pAtFrontMostCardTop - 0.825f;
        }
        this.mInitialScrollP = Math.min(this.mMaxScrollP, Math.max(0.0f, this.mInitialScrollP));
    }

    public VisibilityReport computeStackVisibilityReport(ArrayList<Task> tasks) {
        if (tasks.size() <= 1) {
            return new VisibilityReport(1, 1);
        }
        int taskHeight = this.mTaskRect.height();
        int numVisibleTasks = 1;
        int numVisibleThumbnails = 1;
        int prevScreenY = curveProgressToScreenY(((Float) this.mTaskProgressMap.get(((Task) tasks.get(tasks.size() - 1)).key)).floatValue() - this.mInitialScrollP);
        for (int i = tasks.size() - 2; i >= 0; i--) {
            Task task = (Task) tasks.get(i);
            float progress = ((Float) this.mTaskProgressMap.get(task.key)).floatValue() - this.mInitialScrollP;
            if (progress < 0.0f) {
                break;
            }
            boolean isFrontMostTaskInGroup = task.group.isFrontMostTask(task);
            if (isFrontMostTaskInGroup) {
                int screenY = curveProgressToScreenY(progress) + ((int) (((1.0f - curveProgressToScale(progress)) * ((float) taskHeight)) / 2.0f));
                if (!(prevScreenY - screenY > this.mConfig.taskBarHeight)) {
                    for (int j = i; j >= 0; j--) {
                        numVisibleTasks++;
                        if (((Float) this.mTaskProgressMap.get(((Task) tasks.get(j)).key)).floatValue() - this.mInitialScrollP < 0.0f) {
                            break;
                        }
                    }
                } else {
                    numVisibleThumbnails++;
                    numVisibleTasks++;
                    prevScreenY = screenY;
                }
            } else if (!isFrontMostTaskInGroup) {
                numVisibleTasks++;
            }
        }
        return new VisibilityReport(numVisibleTasks, numVisibleThumbnails);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform prevTransform) {
        if (task != null && this.mTaskProgressMap.containsKey(task.key)) {
            return getStackTransform(((Float) this.mTaskProgressMap.get(task.key)).floatValue(), stackScroll, transformOut, prevTransform);
        }
        transformOut.reset();
        return transformOut;
    }

    public TaskViewTransform getStackTransform(float taskProgress, float stackScroll, TaskViewTransform transformOut, TaskViewTransform prevTransform) {
        float pTaskRelative = taskProgress - stackScroll;
        float pBounded = Math.max(0.0f, Math.min(pTaskRelative, 1.0f));
        if (pTaskRelative > 1.0f) {
            transformOut.reset();
            transformOut.rect.set(this.mTaskRect);
            return transformOut;
        } else if (pTaskRelative >= 0.0f || prevTransform == null || Float.compare(prevTransform.p, 0.0f) > 0) {
            float scale = curveProgressToScale(pBounded);
            int scaleYOffset = (int) (((1.0f - scale) * ((float) this.mTaskRect.height())) / 2.0f);
            int minZ = this.mConfig.taskViewTranslationZMinPx;
            int maxZ = this.mConfig.taskViewTranslationZMaxPx;
            transformOut.scale = scale;
            transformOut.translationY = (curveProgressToScreenY(pBounded) - this.mStackVisibleRect.top) - scaleYOffset;
            transformOut.translationZ = Math.max((float) minZ, ((float) minZ) + (((float) (maxZ - minZ)) * pBounded));
            transformOut.rect.set(this.mTaskRect);
            transformOut.rect.offset(0, transformOut.translationY);
            Utilities.scaleRectAboutCenter(transformOut.rect, transformOut.scale);
            transformOut.visible = true;
            transformOut.p = pTaskRelative;
            return transformOut;
        } else {
            transformOut.reset();
            transformOut.rect.set(this.mTaskRect);
            return transformOut;
        }
    }

    public Rect getUntransformedTaskViewSize() {
        Rect tvSize = new Rect(this.mTaskRect);
        tvSize.offsetTo(0, 0);
        return tvSize;
    }

    float getStackScrollForTask(Task t) {
        if (this.mTaskProgressMap.containsKey(t.key)) {
            return ((Float) this.mTaskProgressMap.get(t.key)).floatValue();
        }
        return 0.0f;
    }

    public static void initializeCurve() {
        if (xp == null || px == null) {
            int xStep;
            xp = new float[251];
            px = new float[251];
            float[] fx = new float[251];
            float x = 0.0f;
            for (xStep = 0; xStep <= 250; xStep++) {
                fx[xStep] = logFunc(x);
                x += 0.004f;
            }
            float pLength = 0.0f;
            float[] dx = new float[251];
            dx[0] = 0.0f;
            for (xStep = 1; xStep < 250; xStep++) {
                dx[xStep] = (float) Math.sqrt(Math.pow((double) (fx[xStep] - fx[xStep - 1]), 2.0d) + Math.pow(0.004000000189989805d, 2.0d));
                pLength += dx[xStep];
            }
            float p = 0.0f;
            px[0] = 0.0f;
            px[250] = 1.0f;
            for (xStep = 1; xStep <= 250; xStep++) {
                p += Math.abs(dx[xStep] / pLength);
                px[xStep] = p;
            }
            xStep = 0;
            p = 0.0f;
            xp[0] = 0.0f;
            xp[250] = 1.0f;
            for (int pStep = 0; pStep < 250; pStep++) {
                while (xStep < 250 && px[xStep] <= p) {
                    xStep++;
                }
                if (xStep == 0) {
                    xp[pStep] = 0.0f;
                } else {
                    xp[pStep] = (((float) (xStep - 1)) + ((p - px[xStep - 1]) / (px[xStep] - px[xStep - 1]))) * 0.004f;
                }
                p += 0.004f;
            }
        }
    }

    static float reverse(float x) {
        return ((-x) * 1.75f) + 1.0f;
    }

    static float logFunc(float x) {
        return 1.0f - (((float) Math.pow(3000.0d, (double) reverse(x))) / 3000.0f);
    }

    int curveProgressToScreenY(float p) {
        if (p < 0.0f || p > 1.0f) {
            return this.mStackVisibleRect.top + ((int) (((float) this.mStackVisibleRect.height()) * p));
        }
        float pIndex = p * 250.0f;
        int pFloorIndex = (int) Math.floor((double) pIndex);
        int pCeilIndex = (int) Math.ceil((double) pIndex);
        float xFraction = 0.0f;
        if (pFloorIndex < 250 && pCeilIndex != pFloorIndex) {
            xFraction = (xp[pCeilIndex] - xp[pFloorIndex]) * ((pIndex - ((float) pFloorIndex)) / ((float) (pCeilIndex - pFloorIndex)));
        }
        return this.mStackVisibleRect.top + ((int) (((float) this.mStackVisibleRect.height()) * (xp[pFloorIndex] + xFraction)));
    }

    float curveProgressToScale(float p) {
        if (p < 0.0f) {
            return 0.8f;
        }
        if (p > 1.0f) {
            return 1.0f;
        }
        return 0.8f + (0.19999999f * p);
    }

    float screenYToCurveProgress(int screenY) {
        float x = ((float) (screenY - this.mStackVisibleRect.top)) / ((float) this.mStackVisibleRect.height());
        if (x < 0.0f || x > 1.0f) {
            return x;
        }
        float xIndex = x * 250.0f;
        int xFloorIndex = (int) Math.floor((double) xIndex);
        int xCeilIndex = (int) Math.ceil((double) xIndex);
        float pFraction = 0.0f;
        if (xFloorIndex < 250 && xCeilIndex != xFloorIndex) {
            pFraction = (px[xCeilIndex] - px[xFloorIndex]) * ((xIndex - ((float) xFloorIndex)) / ((float) (xCeilIndex - xFloorIndex)));
        }
        return px[xFloorIndex] + pFraction;
    }
}
