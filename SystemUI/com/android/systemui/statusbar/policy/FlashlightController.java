package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.TorchCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class FlashlightController {
    private static final boolean DEBUG = Log.isLoggable("FlashlightController", 3);
    private final String mCameraId;
    private final CameraManager mCameraManager;
    private boolean mFlashlightEnabled;
    private Handler mHandler;
    private final ArrayList<WeakReference<FlashlightListener>> mListeners = new ArrayList(1);
    private boolean mTorchAvailable;
    private final TorchCallback mTorchCallback = new TorchCallback() {
        public void onTorchModeUnavailable(String cameraId) {
            if (TextUtils.equals(cameraId, FlashlightController.this.mCameraId)) {
                setCameraAvailable(false);
            }
        }

        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (TextUtils.equals(cameraId, FlashlightController.this.mCameraId)) {
                setCameraAvailable(true);
                setTorchMode(enabled);
            }
        }

        private void setCameraAvailable(boolean available) {
            synchronized (FlashlightController.this) {
                boolean changed = FlashlightController.this.mTorchAvailable != available;
                FlashlightController.this.mTorchAvailable = available;
            }
            if (changed) {
                if (FlashlightController.DEBUG) {
                    Log.d("FlashlightController", "dispatchAvailabilityChanged(" + available + ")");
                }
                FlashlightController.this.dispatchAvailabilityChanged(available);
            }
        }

        private void setTorchMode(boolean enabled) {
            synchronized (FlashlightController.this) {
                boolean changed = FlashlightController.this.mFlashlightEnabled != enabled;
                FlashlightController.this.mFlashlightEnabled = enabled;
            }
            if (changed) {
                if (FlashlightController.DEBUG) {
                    Log.d("FlashlightController", "dispatchModeChanged(" + enabled + ")");
                }
                FlashlightController.this.dispatchModeChanged(enabled);
            }
        }
    };

    public interface FlashlightListener {
        void onFlashlightAvailabilityChanged(boolean z);

        void onFlashlightChanged(boolean z);

        void onFlashlightError();
    }

    public FlashlightController(Context mContext) {
        String cameraId;
        String str = null;
        this.mCameraManager = (CameraManager) mContext.getSystemService("camera");
        try {
            cameraId = getCameraId();
        } catch (Throwable e) {
            Log.e("FlashlightController", "Couldn't initialize.", e);
            return;
        } finally {
            this.mCameraId = 
/*
Method generation error in method: com.android.systemui.statusbar.policy.FlashlightController.<init>(android.content.Context):void
jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0043: IPUT  (wrap: java.lang.String
  ?: MERGE  (r4_1 java.lang.String) = (r4_0 'str' java.lang.String), (r0_2 'cameraId' java.lang.String)), (r5_0 'this' com.android.systemui.statusbar.policy.FlashlightController) com.android.systemui.statusbar.policy.FlashlightController.mCameraId java.lang.String in method: com.android.systemui.statusbar.policy.FlashlightController.<init>(android.content.Context):void
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:226)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:203)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:100)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:50)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:297)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:328)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:265)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:228)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:118)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:83)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:19)
	at jadx.core.ProcessClass.process(ProcessClass.java:43)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
Caused by: jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r4_1 java.lang.String) = (r4_0 'str' java.lang.String), (r0_2 'cameraId' java.lang.String) in method: com.android.systemui.statusbar.policy.FlashlightController.<init>(android.content.Context):void
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:226)
	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:101)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:393)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:220)
	... 20 more
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:530)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:514)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:211)
	... 23 more

*/

            public void setFlashlight(boolean enabled) {
                boolean pendingError = false;
                synchronized (this) {
                    if (this.mFlashlightEnabled != enabled) {
                        this.mFlashlightEnabled = enabled;
                        try {
                            this.mCameraManager.setTorchMode(this.mCameraId, enabled);
                        } catch (CameraAccessException e) {
                            Log.e("FlashlightController", "Couldn't set torch mode", e);
                            this.mFlashlightEnabled = false;
                            pendingError = true;
                        }
                    }
                }
                dispatchModeChanged(this.mFlashlightEnabled);
                if (pendingError) {
                    dispatchError();
                }
            }

            public synchronized boolean isEnabled() {
                return this.mFlashlightEnabled;
            }

            public synchronized boolean isAvailable() {
                return this.mTorchAvailable;
            }

            public void addListener(FlashlightListener l) {
                synchronized (this.mListeners) {
                    cleanUpListenersLocked(l);
                    this.mListeners.add(new WeakReference(l));
                }
            }

            public void removeListener(FlashlightListener l) {
                synchronized (this.mListeners) {
                    cleanUpListenersLocked(l);
                }
            }

            private synchronized void ensureHandler() {
                if (this.mHandler == null) {
                    HandlerThread thread = new HandlerThread("FlashlightController", 10);
                    thread.start();
                    this.mHandler = new Handler(thread.getLooper());
                }
            }

            private String getCameraId() throws CameraAccessException {
                for (String id : this.mCameraManager.getCameraIdList()) {
                    CameraCharacteristics c = this.mCameraManager.getCameraCharacteristics(id);
                    Boolean flashAvailable = (Boolean) c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = (Integer) c.get(CameraCharacteristics.LENS_FACING);
                    if (flashAvailable != null && flashAvailable.booleanValue() && lensFacing != null && lensFacing.intValue() == 1) {
                        return id;
                    }
                }
                return null;
            }

            private void dispatchModeChanged(boolean enabled) {
                dispatchListeners(1, enabled);
            }

            private void dispatchError() {
                dispatchListeners(1, false);
            }

            private void dispatchAvailabilityChanged(boolean available) {
                dispatchListeners(2, available);
            }

            private void dispatchListeners(int message, boolean argument) {
                synchronized (this.mListeners) {
                    int N = this.mListeners.size();
                    boolean cleanup = false;
                    for (int i = 0; i < N; i++) {
                        FlashlightListener l = (FlashlightListener) ((WeakReference) this.mListeners.get(i)).get();
                        if (l == null) {
                            cleanup = true;
                        } else if (message == 0) {
                            l.onFlashlightError();
                        } else if (message == 1) {
                            l.onFlashlightChanged(argument);
                        } else if (message == 2) {
                            l.onFlashlightAvailabilityChanged(argument);
                        } else {
                            continue;
                        }
                    }
                    if (cleanup) {
                        cleanUpListenersLocked(null);
                    }
                }
            }

            private void cleanUpListenersLocked(FlashlightListener listener) {
                for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                    FlashlightListener found = (FlashlightListener) ((WeakReference) this.mListeners.get(i)).get();
                    if (found == null || found == listener) {
                        this.mListeners.remove(i);
                    }
                }
            }
        }
