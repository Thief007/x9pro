package com.android.systemui.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.systemui.R;

class GlobalScreenshot {
    private ImageView mBackgroundView;
    private float mBgPadding;
    private float mBgPaddingScale;
    private MediaActionSound mCameraSound;
    private Context mContext;
    private Display mDisplay;
    private Matrix mDisplayMatrix = new Matrix();
    private DisplayMetrics mDisplayMetrics;
    private int mNotificationIconSize;
    private NotificationManager mNotificationManager;
    private final int mPreviewHeight;
    private final int mPreviewWidth;
    private AsyncTask<SaveImageInBackgroundData, Void, SaveImageInBackgroundData> mSaveInBgTask;
    private Bitmap mScreenBitmap;
    private AnimatorSet mScreenshotAnimation;
    private ImageView mScreenshotFlash;
    private View mScreenshotLayout;
    private ImageView mScreenshotView;
    private LayoutParams mWindowLayoutParams;
    private WindowManager mWindowManager;

    public static class DeleteScreenshotReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("android:cancel_id") && intent.hasExtra("android:screenshot_uri_id")) {
                NotificationManager nm = (NotificationManager) context.getSystemService("notification");
                int id = intent.getIntExtra("android:cancel_id", 0);
                Uri uri = Uri.parse(intent.getStringExtra("android:screenshot_uri_id"));
                nm.cancel(id);
                new DeleteImageInBackgroundTask(context).execute(new Uri[]{uri});
            }
        }
    }

    public static class TargetChosenReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("android:cancel_id")) {
                ((NotificationManager) context.getSystemService("notification")).cancel(intent.getIntExtra("android:cancel_id", 0));
            }
        }
    }

    public GlobalScreenshot(Context context) {
        Resources r = context.getResources();
        this.mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mScreenshotLayout = layoutInflater.inflate(R.layout.global_screenshot, null);
        this.mBackgroundView = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot_background);
        this.mScreenshotView = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot);
        this.mScreenshotFlash = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot_flash);
        this.mScreenshotLayout.setFocusable(true);
        this.mScreenshotLayout.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        this.mWindowLayoutParams = new LayoutParams(-1, -1, 0, 0, 2015, 17302784, -3);
        this.mWindowLayoutParams.setTitle("ScreenshotAnimation");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        this.mNotificationIconSize = r.getDimensionPixelSize(17104902);
        this.mBgPadding = (float) r.getDimensionPixelSize(R.dimen.global_screenshot_bg_padding);
        this.mBgPaddingScale = this.mBgPadding / ((float) this.mDisplayMetrics.widthPixels);
        int panelWidth = 0;
        try {
            panelWidth = r.getDimensionPixelSize(R.dimen.notification_panel_width);
        } catch (NotFoundException e) {
        }
        if (panelWidth <= 0) {
            panelWidth = this.mDisplayMetrics.widthPixels;
        }
        this.mPreviewWidth = panelWidth;
        this.mPreviewHeight = r.getDimensionPixelSize(R.dimen.notification_max_height);
        this.mCameraSound = new MediaActionSound();
        this.mCameraSound.load(0);
    }

    private void saveScreenshotInWorkerThread(Runnable finisher) {
        SaveImageInBackgroundData data = new SaveImageInBackgroundData();
        data.context = this.mContext;
        data.image = this.mScreenBitmap;
        data.iconSize = this.mNotificationIconSize;
        data.finisher = finisher;
        data.previewWidth = this.mPreviewWidth;
        data.previewheight = this.mPreviewHeight;
        if (this.mSaveInBgTask != null) {
            this.mSaveInBgTask.cancel(false);
        }
        this.mSaveInBgTask = new SaveImageInBackgroundTask(this.mContext, data, this.mNotificationManager, R.id.notification_screenshot).execute(new SaveImageInBackgroundData[]{data});
    }

    private float getDegreesForRotation(int value) {
        switch (value) {
            case 1:
                return 270.0f;
            case 2:
                return 180.0f;
            case 3:
                return 90.0f;
            default:
                return 0.0f;
        }
    }

    void takeScreenshot(Runnable finisher, boolean statusBarVisible, boolean navBarVisible) {
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        float[] dims = new float[]{(float) this.mDisplayMetrics.widthPixels, (float) this.mDisplayMetrics.heightPixels};
        float degrees = getDegreesForRotation(this.mDisplay.getRotation());
        boolean requiresRotation = degrees > 0.0f;
        if (requiresRotation) {
            this.mDisplayMatrix.reset();
            this.mDisplayMatrix.preRotate(-degrees);
            this.mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }
        this.mScreenBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);
        if (this.mScreenBitmap == null) {
            notifyScreenshotError(this.mContext, this.mNotificationManager);
            finisher.run();
            return;
        }
        if (requiresRotation) {
            Bitmap ss = Bitmap.createBitmap(this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels, Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate((float) (ss.getWidth() / 2), (float) (ss.getHeight() / 2));
            c.rotate(degrees);
            c.translate((-dims[0]) / 2.0f, (-dims[1]) / 2.0f);
            c.drawBitmap(this.mScreenBitmap, 0.0f, 0.0f, null);
            c.setBitmap(null);
            this.mScreenBitmap.recycle();
            this.mScreenBitmap = ss;
        }
        this.mScreenBitmap.setHasAlpha(false);
        this.mScreenBitmap.prepareToDraw();
        startAnimation(finisher, this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels, statusBarVisible, navBarVisible);
    }

    private void startAnimation(final Runnable finisher, int w, int h, boolean statusBarVisible, boolean navBarVisible) {
        this.mScreenshotView.setImageBitmap(this.mScreenBitmap);
        this.mScreenshotLayout.requestFocus();
        if (this.mScreenshotAnimation != null) {
            this.mScreenshotAnimation.end();
            this.mScreenshotAnimation.removeAllListeners();
        }
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        ValueAnimator screenshotDropInAnim = createScreenshotDropInAnimation();
        ValueAnimator screenshotFadeOutAnim = createScreenshotDropOutAnimation(w, h, statusBarVisible, navBarVisible);
        this.mScreenshotAnimation = new AnimatorSet();
        this.mScreenshotAnimation.playSequentially(new Animator[]{screenshotDropInAnim, screenshotFadeOutAnim});
        this.mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                GlobalScreenshot.this.saveScreenshotInWorkerThread(finisher);
                GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                GlobalScreenshot.this.mScreenBitmap = null;
                GlobalScreenshot.this.mScreenshotView.setImageBitmap(null);
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            public void run() {
                GlobalScreenshot.this.mCameraSound.play(0);
                GlobalScreenshot.this.mScreenshotView.setLayerType(2, null);
                GlobalScreenshot.this.mScreenshotView.buildLayer();
                GlobalScreenshot.this.mScreenshotAnimation.start();
            }
        });
    }

    private ValueAnimator createScreenshotDropInAnimation() {
        final Interpolator flashAlphaInterpolator = new Interpolator() {
            public float getInterpolation(float x) {
                if (x <= 0.60465115f) {
                    return (float) Math.sin(((double) (x / 0.60465115f)) * 3.141592653589793d);
                }
                return 0.0f;
            }
        };
        final Interpolator scaleInterpolator = new Interpolator() {
            public float getInterpolation(float x) {
                if (x < 0.30232558f) {
                    return 0.0f;
                }
                return (x - 0.60465115f) / 0.39534885f;
            }
        };
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.setDuration(430);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                GlobalScreenshot.this.mBackgroundView.setAlpha(0.0f);
                GlobalScreenshot.this.mBackgroundView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotView.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationX(0.0f);
                GlobalScreenshot.this.mScreenshotView.setTranslationY(0.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleX(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setScaleY(GlobalScreenshot.this.mBgPaddingScale + 1.0f);
                GlobalScreenshot.this.mScreenshotView.setVisibility(0);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(0.0f);
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                GlobalScreenshot.this.mScreenshotFlash.setVisibility(8);
            }
        });
        anim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                float scaleT = (GlobalScreenshot.this.mBgPaddingScale + 1.0f) - (scaleInterpolator.getInterpolation(t) * 0.27499998f);
                GlobalScreenshot.this.mBackgroundView.setAlpha(scaleInterpolator.getInterpolation(t) * 0.5f);
                GlobalScreenshot.this.mScreenshotView.setAlpha(t);
                GlobalScreenshot.this.mScreenshotView.setScaleX(scaleT);
                GlobalScreenshot.this.mScreenshotView.setScaleY(scaleT);
                GlobalScreenshot.this.mScreenshotFlash.setAlpha(flashAlphaInterpolator.getInterpolation(t));
            }
        });
        return anim;
    }

    private ValueAnimator createScreenshotDropOutAnimation(int w, int h, boolean statusBarVisible, boolean navBarVisible) {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.setStartDelay(500);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                GlobalScreenshot.this.mBackgroundView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setVisibility(8);
                GlobalScreenshot.this.mScreenshotView.setLayerType(0, null);
            }
        });
        if (statusBarVisible && navBarVisible) {
            final Interpolator scaleInterpolator = new Interpolator() {
                public float getInterpolation(float x) {
                    if (x < 0.8604651f) {
                        return (float) (1.0d - Math.pow((double) (1.0f - (x / 0.8604651f)), 2.0d));
                    }
                    return 1.0f;
                }
            };
            float halfScreenWidth = (((float) w) - (this.mBgPadding * 2.0f)) / 2.0f;
            float halfScreenHeight = (((float) h) - (this.mBgPadding * 2.0f)) / 2.0f;
            final PointF finalPos = new PointF((-halfScreenWidth) + (0.45f * halfScreenWidth), (-halfScreenHeight) + (0.45f * halfScreenHeight));
            anim.setDuration(430);
            anim.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = ((Float) animation.getAnimatedValue()).floatValue();
                    float scaleT = (GlobalScreenshot.this.mBgPaddingScale + 0.725f) - (scaleInterpolator.getInterpolation(t) * 0.27500004f);
                    GlobalScreenshot.this.mBackgroundView.setAlpha((1.0f - t) * 0.5f);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(1.0f - scaleInterpolator.getInterpolation(t));
                    GlobalScreenshot.this.mScreenshotView.setScaleX(scaleT);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(scaleT);
                    GlobalScreenshot.this.mScreenshotView.setTranslationX(finalPos.x * t);
                    GlobalScreenshot.this.mScreenshotView.setTranslationY(finalPos.y * t);
                }
            });
        } else {
            anim.setDuration(320);
            anim.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = ((Float) animation.getAnimatedValue()).floatValue();
                    float scaleT = (GlobalScreenshot.this.mBgPaddingScale + 0.725f) - (0.125f * t);
                    GlobalScreenshot.this.mBackgroundView.setAlpha((1.0f - t) * 0.5f);
                    GlobalScreenshot.this.mScreenshotView.setAlpha(1.0f - t);
                    GlobalScreenshot.this.mScreenshotView.setScaleX(scaleT);
                    GlobalScreenshot.this.mScreenshotView.setScaleY(scaleT);
                }
            });
        }
        return anim;
    }

    static void notifyScreenshotError(Context context, NotificationManager nManager) {
        Resources r = context.getResources();
        nManager.notify(R.id.notification_screenshot, new BigTextStyle(new Builder(context).setTicker(r.getString(R.string.screenshot_failed_title)).setContentTitle(r.getString(R.string.screenshot_failed_title)).setContentText(r.getString(R.string.screenshot_failed_text)).setSmallIcon(R.drawable.stat_notify_image_error).setWhen(System.currentTimeMillis()).setVisibility(1).setCategory("err").setAutoCancel(true).setColor(context.getColor(17170521))).bigText(r.getString(R.string.screenshot_failed_text)).build());
    }
}
