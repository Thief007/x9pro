package com.mediatek.galleryfeature.clearmotion;

public class ClearMotionQualityJni {
    public static boolean sValidLib;

    public static native int nativeGetDemoMode();

    public static native int nativeGetFallbackIndex();

    public static native int nativeGetFallbackRange();

    public static native boolean nativeSetDemoMode(int i);

    public static native boolean nativeSetFallbackIndex(int i);

    static {
        sValidLib = true;
        try {
            System.loadLibrary("MJCjni");
        } catch (SecurityException e) {
            sValidLib = false;
        } catch (UnsatisfiedLinkError e2) {
            sValidLib = false;
        }
    }
}
