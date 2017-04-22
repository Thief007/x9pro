package com.mediatek.settings.wfd;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.setupwizardlib.R$styleable;
import com.mediatek.settings.FeatureOption;

public final class WfdSinkSurfaceFragment extends DialogFragment implements Callback, OnLongClickListener {
    private static final String TAG = WfdSinkSurfaceFragment.class.getSimpleName();
    private Activity mActivity;
    private boolean mCountdownShowing = false;
    private Dialog mDialog;
    private WfdSinkExt mExt;
    private boolean mGuideShowing = false;
    private boolean mLatinCharTest = false;
    private int mOrientationBak = -100;
    private SurfaceView mSinkView;
    private WfdSinkLayout mSinkViewLayout;
    private boolean mSurfaceShowing = false;
    private int mTestLatinChar = 160;

    class C07431 implements OnClickListener {
        C07431() {
        }

        public void onClick(View v) {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "ok button onClick");
            WfdSinkSurfaceFragment.this.removeWfdSinkGuide();
        }
    }

    private class FullScreenDialog extends Dialog {
        private Activity mActivity;
        private int mSystemUiBak;

        class C07451 implements Runnable {
            C07451() {
            }

            public void run() {
                WfdSinkSurfaceFragment.this.requestFullScreen(FullScreenDialog.this.mSystemUiBak);
            }
        }

        class C07462 implements OnSystemUiVisibilityChangeListener {
            C07462() {
            }

            public void onSystemUiVisibilityChange(int i) {
                Log.i("@M_" + WfdSinkSurfaceFragment.TAG, "onSystemUiVisibilityChange: " + i);
                if (i == 0) {
                    WfdSinkSurfaceFragment.this.mSinkViewLayout.setFullScreenFlag(false);
                    if (WfdSinkSurfaceFragment.this.mSinkViewLayout.mHasFocus) {
                        WfdSinkSurfaceFragment.this.requestFullScreen(FullScreenDialog.this.mSystemUiBak);
                        return;
                    }
                    return;
                }
                WfdSinkSurfaceFragment.this.mSinkViewLayout.setFullScreenFlag(true);
            }
        }

        public FullScreenDialog(Activity activity) {
            super(activity, 16973841);
            this.mActivity = activity;
        }

        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onCreate");
            LayoutParams viewParams = new LayoutParams(-1, -1);
            WfdSinkSurfaceFragment.this.mSinkViewLayout = new WfdSinkLayout(this.mActivity);
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setFocusableInTouchMode(true);
            setContentView(WfdSinkSurfaceFragment.this.mSinkViewLayout);
            WfdSinkSurfaceFragment.this.mSinkView = new SurfaceView(this.mActivity);
            WfdSinkSurfaceFragment.this.mSinkView.setFocusableInTouchMode(false);
            WfdSinkSurfaceFragment.this.mSinkView.setFocusable(false);
            WfdSinkSurfaceFragment.this.mSinkViewLayout.addView(WfdSinkSurfaceFragment.this.mSinkView, viewParams);
        }

        protected void onStart() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onStart");
            super.onStart();
            this.mSystemUiBak = WfdSinkSurfaceFragment.this.mSinkViewLayout.getSystemUiVisibility();
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setOnFocusGetCallback(new C07451());
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setOnSystemUiVisibilityChangeListener(new C07462());
            WfdSinkSurfaceFragment.this.requestFullScreen(this.mSystemUiBak);
            this.mActivity.getWindow().addFlags(128);
            WfdSinkSurfaceFragment.this.mSinkView.getHolder().addCallback(WfdSinkSurfaceFragment.this);
        }

        protected void onStop() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onStop");
            WfdSinkSurfaceFragment.this.mSinkViewLayout.setSystemUiVisibility(this.mSystemUiBak);
            this.mActivity.getWindow().clearFlags(128);
            WfdSinkSurfaceFragment.this.mSinkView.getHolder().removeCallback(WfdSinkSurfaceFragment.this);
            WfdSinkSurfaceFragment.this.restoreOrientation();
            super.onStop();
        }

        public void dismiss() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog dismiss");
            WfdSinkSurfaceFragment.this.disconnect();
            this.mActivity.finish();
            super.dismiss();
        }

        public void onBackPressed() {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "dialog onBackPressed");
            if (WfdSinkSurfaceFragment.this.mGuideShowing) {
                WfdSinkSurfaceFragment.this.removeWfdSinkGuide();
                return;
            }
            WfdSinkSurfaceFragment.this.disconnect();
            super.onBackPressed();
        }
    }

    private static class KeyCodeConverter {
        private static final SparseIntArray KEYCODE_ASCII = new SparseIntArray();

        private KeyCodeConverter() {
        }

        static {
            populateKeycodeAscii();
        }

        private static void populateKeycodeAscii() {
            SparseIntArray codes = KEYCODE_ASCII;
            codes.put(57, 18);
            codes.put(58, 18);
            codes.put(111, 27);
            codes.put(59, 15);
            codes.put(60, 15);
            codes.put(123, 0);
            codes.put(122, 0);
            codes.put(113, 0);
            codes.put(114, 0);
            codes.put(115, 0);
            codes.put(67, 8);
            codes.put(93, 12);
            codes.put(66, 13);
            codes.put(112, 127);
            codes.put(61, 9);
        }

        public static int keyCodeToAscii(int keyCode) {
            return KEYCODE_ASCII.get(keyCode);
        }
    }

    private class WfdSinkLayout extends FrameLayout {
        private boolean mCatchEvents = true;
        private CountDown mCountDown;
        private Runnable mFocusGetCallback;
        private boolean mFullScreenFlag = false;
        private boolean mHasFocus = false;
        private boolean mHasPerformedLongPress = false;
        private float mInitX;
        private float mInitY;
        private int mTouchSlop;

        class CountDown implements Runnable {
            private int mCountDownNum;
            private int mOriginalWindowAttachCount;

            CountDown() {
            }

            public void run() {
                if (WfdSinkSurfaceFragment.this.mCountdownShowing) {
                    this.mCountDownNum--;
                    if (this.mCountDownNum <= 0) {
                        if (WfdSinkLayout.this.mParent != null && this.mOriginalWindowAttachCount == WfdSinkLayout.this.getWindowAttachCount() && WfdSinkSurfaceFragment.this.onLongClick(WfdSinkSurfaceFragment.this.mSinkViewLayout)) {
                            WfdSinkLayout.this.mHasPerformedLongPress = true;
                        }
                        return;
                    } else if (WfdSinkSurfaceFragment.this.mCountdownShowing) {
                        ViewGroup countdownView = (ViewGroup) WfdSinkSurfaceFragment.this.mSinkViewLayout.getTag(R.id.wfd_sink_countdown_num);
                        if (countdownView != null) {
                            TextView tv = (TextView) countdownView.findViewById(R.id.wfd_sink_countdown_num);
                            if (tv != null) {
                                tv.setText(this.mCountDownNum + "");
                                tv.postInvalidate();
                            }
                        }
                    }
                } else {
                    this.mCountDownNum = 3;
                    WfdSinkSurfaceFragment.this.addCountdownView(this.mCountDownNum + "");
                }
                WfdSinkLayout.this.postDelayed(this, 1000);
            }

            public void rememberWindowAttachCount() {
                this.mOriginalWindowAttachCount = WfdSinkLayout.this.getWindowAttachCount();
            }
        }

        public WfdSinkLayout(Context context) {
            super(context);
            this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        public boolean onTouchEvent(MotionEvent ev) {
            if (!this.mCatchEvents) {
                return false;
            }
            int action = ev.getAction();
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onTouchEvent action=" + action);
            StringBuilder eventDesc;
            switch (action & 255) {
                case 0:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        eventDesc = new StringBuilder();
                        eventDesc.append(String.valueOf(0)).append(",");
                        eventDesc.append(getTouchEventDesc(ev));
                        sendUibcInputEvent(eventDesc.toString());
                    }
                    this.mInitX = ev.getX();
                    this.mInitY = ev.getY();
                    this.mHasPerformedLongPress = false;
                    checkForLongClick(0);
                    break;
                case 1:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        eventDesc = new StringBuilder();
                        eventDesc.append(String.valueOf(1)).append(",");
                        eventDesc.append(getTouchEventDesc(ev));
                        sendUibcInputEvent(eventDesc.toString());
                    }
                    removePendingCallback();
                    break;
                case 2:
                    if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                        eventDesc = new StringBuilder();
                        eventDesc.append(String.valueOf(2)).append(",");
                        eventDesc.append(getTouchEventDesc(ev));
                        sendUibcInputEvent(eventDesc.toString());
                    }
                    if (Math.hypot((double) (ev.getX() - this.mInitX), (double) (ev.getY() - this.mInitY)) > ((double) this.mTouchSlop)) {
                        removePendingCallback();
                        break;
                    }
                    break;
                case 3:
                    removePendingCallback();
                    break;
            }
            return true;
        }

        public boolean onGenericMotionEvent(MotionEvent event) {
            if (!this.mCatchEvents) {
                return false;
            }
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onGenericMotionEvent event.getSource()=" + event.getSource());
            if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT && event.getSource() == 8194) {
                switch (event.getAction()) {
                    case R$styleable.SuwSetupWizardLayout_suwIllustrationHorizontalTile /*7*/:
                        StringBuilder eventDesc = new StringBuilder();
                        eventDesc.append(String.valueOf(2)).append(",");
                        eventDesc.append(getTouchEventDesc(event));
                        sendUibcInputEvent(eventDesc.toString());
                        return true;
                    case R$styleable.SuwSetupWizardLayout_suwIllustrationImage /*8*/:
                        return true;
                }
            }
            return true;
        }

        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (!this.mCatchEvents || !this.mFullScreenFlag) {
                return false;
            }
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onKeyPreIme keyCode=" + keyCode + ", action=" + event.getAction());
            if (FeatureOption.MTK_WFD_SINK_UIBC_SUPPORT) {
                int asciiCode = event.getUnicodeChar();
                if (asciiCode == 0 || asciiCode < 32) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Can't find unicode for keyCode=" + keyCode);
                    asciiCode = KeyCodeConverter.keyCodeToAscii(keyCode);
                }
                boolean onKeyUp = event.getAction() == 1;
                if (WfdSinkSurfaceFragment.this.mLatinCharTest && keyCode == 131) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Latin Test Mode enabled");
                    asciiCode = WfdSinkSurfaceFragment.this.mTestLatinChar;
                    if (onKeyUp) {
                        if (WfdSinkSurfaceFragment.this.mTestLatinChar == 255) {
                            WfdSinkSurfaceFragment.this.mTestLatinChar = 160;
                        } else {
                            WfdSinkSurfaceFragment wfdSinkSurfaceFragment = WfdSinkSurfaceFragment.this;
                            wfdSinkSurfaceFragment.mTestLatinChar = wfdSinkSurfaceFragment.mTestLatinChar + 1;
                        }
                    }
                }
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onKeyPreIme asciiCode=" + asciiCode);
                if (asciiCode == 0) {
                    Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "Can't find control for keyCode=" + keyCode);
                } else {
                    int i;
                    StringBuilder eventDesc = new StringBuilder();
                    if (onKeyUp) {
                        i = 4;
                    } else {
                        i = 3;
                    }
                    eventDesc.append(String.valueOf(i)).append(",").append(String.format("0x%04x", new Object[]{Integer.valueOf(asciiCode)})).append(", 0x0000");
                    sendUibcInputEvent(eventDesc.toString());
                    return true;
                }
            }
            return false;
        }

        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "onWindowFocusChanged: " + hasWindowFocus);
            this.mHasFocus = hasWindowFocus;
            if (hasWindowFocus && this.mFocusGetCallback != null) {
                this.mFocusGetCallback.run();
            }
        }

        private String getTouchEventDesc(MotionEvent ev) {
            int pointerCount = ev.getPointerCount();
            StringBuilder eventDesc = new StringBuilder();
            eventDesc.append(String.valueOf(pointerCount)).append(",");
            for (int p = 0; p < pointerCount; p++) {
                eventDesc.append(String.valueOf(ev.getPointerId(p))).append(",").append(String.valueOf((int) (ev.getXPrecision() * ev.getX(p)))).append(",").append(String.valueOf((int) (ev.getYPrecision() * ev.getY(p)))).append(",");
            }
            String ret = eventDesc.toString();
            return ret.substring(0, ret.length() - 1);
        }

        private void sendUibcInputEvent(String eventDesc) {
            Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "sendUibcInputEvent: " + eventDesc);
            WfdSinkSurfaceFragment.this.mExt.sendUibcEvent(eventDesc);
        }

        private void checkForLongClick(int delayOffset) {
            this.mHasPerformedLongPress = false;
            if (this.mCountDown == null) {
                this.mCountDown = new CountDown();
            }
            this.mCountDown.rememberWindowAttachCount();
            postDelayed(this.mCountDown, (long) ((ViewConfiguration.getLongPressTimeout() + 1000) - delayOffset));
        }

        private void removePendingCallback() {
            Log.v("@M_" + WfdSinkSurfaceFragment.TAG, "removePendingCallback");
            if (this.mCountDown != null && !this.mHasPerformedLongPress) {
                removeCallbacks(this.mCountDown);
                WfdSinkSurfaceFragment.this.removeCountDown();
            }
        }

        private void setCatchEvents(boolean catched) {
            this.mCatchEvents = catched;
        }

        private void setFullScreenFlag(boolean fullScreen) {
            this.mFullScreenFlag = fullScreen;
        }

        private void setOnFocusGetCallback(Runnable runnable) {
            this.mFocusGetCallback = runnable;
        }

        protected void onDetachedFromWindow() {
            removePendingCallback();
            super.onDetachedFromWindow();
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle == null && FeatureOption.MTK_WFD_SINK_SUPPORT) {
            this.mActivity = getActivity();
            this.mExt = new WfdSinkExt(this.mActivity);
            this.mExt.registerSinkFragment(this);
            this.mActivity.getActionBar().hide();
            setShowsDialog(true);
            return;
        }
        Log.d("@M_" + TAG, "bundle is not null, recreate");
        dismissAllowingStateLoss();
        getActivity().finish();
    }

    public void onStart() {
        Log.d("@M_" + TAG, "onStart");
        super.onStart();
        this.mExt.onStart();
    }

    public void onStop() {
        Log.d("@M_" + TAG, "onStop");
        this.mExt.onStop();
        dismissAllowingStateLoss();
        this.mActivity.finish();
        super.onStop();
    }

    private void disconnect() {
        if (this.mSurfaceShowing) {
            this.mExt.disconnectWfdSinkConnection();
        }
        this.mSurfaceShowing = false;
        if (this.mGuideShowing) {
            removeWfdSinkGuide();
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d("@M_" + TAG, "mDialog is null? " + (this.mDialog == null));
        this.mLatinCharTest = SystemProperties.get("wfd.uibc.latintest", "0").equals("1");
        if (this.mDialog == null) {
            this.mDialog = new FullScreenDialog(getActivity());
        }
        return this.mDialog;
    }

    public void addWfdSinkGuide() {
        if (!this.mGuideShowing) {
            ViewGroup guide = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.wfd_sink_guide, null);
            ((Button) guide.findViewById(R.id.wfd_sink_guide_ok_btn)).setOnClickListener(new C07431());
            ((TextView) guide.findViewById(R.id.wfd_sink_guide_content)).setText(getActivity().getResources().getString(R.string.wfd_sink_guide_content, new Object[]{Integer.valueOf(3)}));
            this.mSinkViewLayout.addView(guide);
            this.mSinkViewLayout.setTag(R.string.wfd_sink_guide_content, guide);
            this.mSinkViewLayout.setCatchEvents(false);
            this.mGuideShowing = true;
        }
    }

    private void removeWfdSinkGuide() {
        if (this.mGuideShowing) {
            View guide = (View) this.mSinkViewLayout.getTag(R.string.wfd_sink_guide_content);
            if (guide != null) {
                this.mSinkViewLayout.removeView(guide);
                this.mSinkViewLayout.setTag(R.string.wfd_sink_guide_content, null);
            }
        }
        this.mSinkViewLayout.setCatchEvents(true);
        this.mGuideShowing = false;
    }

    private void addCountdownView(String countdownNum) {
        if (!this.mCountdownShowing) {
            ViewGroup countdownView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.wfd_sink_countdown, null);
            ((TextView) countdownView.findViewById(R.id.wfd_sink_countdown_num)).setText(countdownNum);
            this.mSinkViewLayout.addView(countdownView);
            this.mSinkViewLayout.setTag(R.id.wfd_sink_countdown_num, countdownView);
            this.mCountdownShowing = true;
        }
    }

    private void removeCountDown() {
        if (this.mCountdownShowing) {
            View countdownView = (View) this.mSinkViewLayout.getTag(R.id.wfd_sink_countdown_num);
            if (countdownView != null) {
                this.mSinkViewLayout.removeView(countdownView);
                this.mSinkViewLayout.setTag(R.id.wfd_sink_countdown_num, null);
            }
        }
        this.mCountdownShowing = false;
    }

    public void requestOrientation(boolean isPortrait) {
        int i;
        this.mOrientationBak = this.mActivity.getRequestedOrientation();
        Activity activity = this.mActivity;
        if (isPortrait) {
            i = 1;
        } else {
            i = 0;
        }
        activity.setRequestedOrientation(i);
    }

    public void restoreOrientation() {
        if (this.mOrientationBak != -100) {
            this.mActivity.setRequestedOrientation(this.mOrientationBak);
        }
    }

    private void requestFullScreen(int systemUi) {
        if (VERSION.SDK_INT >= 14) {
            systemUi |= 2;
        }
        if (VERSION.SDK_INT >= 16) {
            systemUi |= 4;
        }
        if (VERSION.SDK_INT >= 18) {
            systemUi |= 20480;
        }
        final int newUiOptions = systemUi;
        this.mSinkViewLayout.postDelayed(new Runnable() {
            public void run() {
                Log.d("@M_" + WfdSinkSurfaceFragment.TAG, "request full screen: " + Integer.toHexString(newUiOptions));
                WfdSinkSurfaceFragment.this.mSinkViewLayout.setSystemUiVisibility(newUiOptions);
            }
        }, 500);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("@M_" + TAG, "surface changed: " + width + "x" + height);
        int systemUiVis = this.mSinkViewLayout.getSystemUiVisibility();
        if (this.mSinkViewLayout.mHasFocus && (systemUiVis & 2) == 0) {
            requestFullScreen(systemUiVis);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("@M_" + TAG, "surface created");
        if (!this.mSurfaceShowing) {
            this.mExt.setupWfdSinkConnection(holder.getSurface());
        }
        this.mSurfaceShowing = true;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("@M_" + TAG, "surface destroyed");
        disconnect();
    }

    public boolean onLongClick(View v) {
        Log.d("@M_" + TAG, "onLongClick");
        dismissAllowingStateLoss();
        this.mActivity.finish();
        return true;
    }
}
