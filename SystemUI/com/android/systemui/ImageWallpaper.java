package com.android.systemui;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemProperties;
import android.renderscript.Matrix4f;
import android.service.wallpaper.WallpaperService;
import android.service.wallpaper.WallpaperService.Engine;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class ImageWallpaper extends WallpaperService {
    boolean RotationChanged = false;
    DrawableEngine mEngine;
    boolean mIsHwAccelerated;
    WallpaperManager mWallpaperManager;

    class DrawableEngine extends Engine {
        Bitmap mBackground;
        int mBackgroundHeight = -1;
        int mBackgroundWidth = -1;
        private Display mDefaultDisplay;
        private int mDisplayHeightAtLastSurfaceSizeUpdate = -1;
        private int mDisplayWidthAtLastSurfaceSizeUpdate = -1;
        private EGL10 mEgl;
        private EGLConfig mEglConfig;
        private EGLContext mEglContext;
        private EGLDisplay mEglDisplay;
        private EGLSurface mEglSurface;
        int mLastRotation = -1;
        int mLastSurfaceHeight = -1;
        int mLastSurfaceWidth = -1;
        int mLastXTranslation;
        int mLastYTranslation;
        boolean mOffsetsChanged;
        boolean mRedrawNeeded;
        private int mRotationAtLastSurfaceSizeUpdate = -1;
        float mScale = 1.0f;
        private final DisplayInfo mTmpDisplayInfo = new DisplayInfo();
        boolean mVisible = true;
        float mXOffset = 0.5f;
        float mYOffset = 0.5f;

        public DrawableEngine() {
            super(ImageWallpaper.this);
            setFixedSizeAllowed(true);
        }

        public void trimMemory(int level) {
            if (level >= 10 && this.mBackground != null) {
                Log.d("ImageWallpaper", "trimMemory");
                this.mBackground.recycle();
                this.mBackground = null;
                this.mBackgroundWidth = -1;
                this.mBackgroundHeight = -1;
                ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
            }
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
            Log.d("ImageWallpaper", "onCreate");
            super.onCreate(surfaceHolder);
            this.mDefaultDisplay = ((WindowManager) ImageWallpaper.this.getSystemService(WindowManager.class)).getDefaultDisplay();
            updateSurfaceSize(surfaceHolder, getDefaultDisplayInfo());
            setOffsetNotificationsEnabled(false);
        }

        public void onDestroy() {
            super.onDestroy();
            this.mBackground = null;
            ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
        }

        void updateSurfaceSize(SurfaceHolder surfaceHolder, DisplayInfo displayInfo) {
            if (this.mBackgroundWidth <= 0 || this.mBackgroundHeight <= 0) {
                ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                updateWallpaperLocked();
                if (this.mBackgroundWidth <= 0 || this.mBackgroundHeight <= 0) {
                    this.mBackgroundWidth = displayInfo.logicalWidth;
                    this.mBackgroundHeight = displayInfo.logicalHeight;
                }
            }
            Log.d("ImageWallpaper", "mBackgroundWidth/Height: " + this.mBackgroundWidth + "/" + this.mBackgroundHeight);
            int surfaceWidth = Math.max(displayInfo.logicalWidth, this.mBackgroundWidth);
            int surfaceHeight = Math.max(displayInfo.logicalHeight, this.mBackgroundHeight);
            Log.d("ImageWallpaper", "surfaceWidth/Height: " + surfaceWidth + "/" + surfaceHeight);
            Rect frame = surfaceHolder.getSurfaceFrame();
            if (frame != null) {
                int dw = frame.width();
                int dh = frame.height();
                if (surfaceWidth == dw && surfaceHeight == dh && !ImageWallpaper.this.RotationChanged) {
                    return;
                }
            }
            surfaceHolder.setFixedSize(surfaceWidth, surfaceHeight);
        }

        public void onVisibilityChanged(boolean visible) {
            Log.d("ImageWallpaper", "onVisibilityChanged: mVisible, visible=" + this.mVisible + ", " + visible);
            if (this.mVisible != visible) {
                Log.d("ImageWallpaper", "Visibility changed to visible=" + visible);
                this.mVisible = visible;
                drawFrame();
            }
        }

        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixels, int yPixels) {
            Log.d("ImageWallpaper", "onOffsetsChanged: xOffset=" + xOffset + ", yOffset=" + yOffset + ", xOffsetStep=" + xOffsetStep + ", yOffsetStep=" + yOffsetStep + ", xPixels=" + xPixels + ", yPixels=" + yPixels);
            if (!(this.mXOffset == xOffset && this.mYOffset == yOffset)) {
                Log.d("ImageWallpaper", "Offsets changed to (" + xOffset + "," + yOffset + ").");
                this.mXOffset = xOffset;
                this.mYOffset = yOffset;
                this.mOffsetsChanged = true;
            }
            drawFrame();
        }

        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("ImageWallpaper", "onSurfaceChanged: width=" + width + ", height=" + height);
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
        }

        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            Log.d("ImageWallpaper", "onSurfaceRedrawNeeded");
            super.onSurfaceRedrawNeeded(holder);
            drawFrame();
        }

        private DisplayInfo getDefaultDisplayInfo() {
            this.mDefaultDisplay.getDisplayInfo(this.mTmpDisplayInfo);
            return this.mTmpDisplayInfo;
        }

        void drawFrame() {
            try {
                DisplayInfo displayInfo = getDefaultDisplayInfo();
                int newRotation = displayInfo.rotation;
                if (newRotation != this.mLastRotation) {
                    Log.d("ImageWallpaper", "Rotation changed : " + this.mLastRotation + " --> " + newRotation);
                    ImageWallpaper.this.RotationChanged = true;
                    updateSurfaceSize(getSurfaceHolder(), displayInfo);
                    ImageWallpaper.this.RotationChanged = true;
                    this.mRotationAtLastSurfaceSizeUpdate = newRotation;
                    this.mDisplayWidthAtLastSurfaceSizeUpdate = displayInfo.logicalWidth;
                    this.mDisplayHeightAtLastSurfaceSizeUpdate = displayInfo.logicalHeight;
                }
                SurfaceHolder sh = getSurfaceHolder();
                Rect frame = sh.getSurfaceFrame();
                int dw = frame.width();
                int dh = frame.height();
                boolean surfaceDimensionsChanged = dw == this.mLastSurfaceWidth ? dh != this.mLastSurfaceHeight : true;
                boolean redrawNeeded = surfaceDimensionsChanged || newRotation != this.mLastRotation;
                if (redrawNeeded || this.mOffsetsChanged) {
                    this.mLastRotation = newRotation;
                    ImageWallpaper.this.RotationChanged = false;
                    if (this.mBackground == null || surfaceDimensionsChanged) {
                        int i;
                        String str = "ImageWallpaper";
                        StringBuilder append = new StringBuilder().append("Reloading bitmap: mBackground, bgw, bgh, dw, dh = ").append(this.mBackground).append(", ").append(this.mBackground == null ? 0 : this.mBackground.getWidth()).append(", ");
                        if (this.mBackground == null) {
                            i = 0;
                        } else {
                            i = this.mBackground.getHeight();
                        }
                        Log.d(str, append.append(i).append(", ").append(dw).append(", ").append(dh).toString());
                        ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                        updateWallpaperLocked();
                        if (this.mBackground == null) {
                            Log.d("ImageWallpaper", "Unable to load bitmap");
                            if (!ImageWallpaper.this.mIsHwAccelerated) {
                                this.mBackground = null;
                                ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                            }
                            return;
                        } else if (!(dw == this.mBackground.getWidth() && dh == this.mBackground.getHeight())) {
                            Log.d("ImageWallpaper", "Surface != bitmap dimensions: surface w/h, bitmap w/h: " + dw + ", " + dh + ", " + this.mBackground.getWidth() + ", " + this.mBackground.getHeight());
                        }
                    }
                    this.mScale = Math.max(1.0f, Math.max(((float) dw) / ((float) this.mBackground.getWidth()), ((float) dh) / ((float) this.mBackground.getHeight())));
                    int availw = dw - ((int) (((float) this.mBackground.getWidth()) * this.mScale));
                    int availh = dh - ((int) (((float) this.mBackground.getHeight()) * this.mScale));
                    int xPixels = availw / 2;
                    int yPixels = availh / 2;
                    int availwUnscaled = dw - this.mBackground.getWidth();
                    int availhUnscaled = dh - this.mBackground.getHeight();
                    if (availwUnscaled < 0) {
                        xPixels += (int) ((((float) availwUnscaled) * (this.mXOffset - 0.5f)) + 0.5f);
                    }
                    if (availhUnscaled < 0) {
                        yPixels += (int) ((((float) availhUnscaled) * (this.mYOffset - 0.5f)) + 0.5f);
                    }
                    this.mOffsetsChanged = false;
                    this.mRedrawNeeded = false;
                    if (surfaceDimensionsChanged) {
                        this.mLastSurfaceWidth = dw;
                        this.mLastSurfaceHeight = dh;
                    }
                    if (!redrawNeeded && xPixels == this.mLastXTranslation && yPixels == this.mLastYTranslation) {
                        Log.d("ImageWallpaper", "Suppressed drawFrame since the image has not actually moved an integral number of pixels.");
                        if (!ImageWallpaper.this.mIsHwAccelerated) {
                            this.mBackground = null;
                            ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                        }
                        return;
                    }
                    this.mLastXTranslation = xPixels;
                    this.mLastYTranslation = yPixels;
                    Log.d("ImageWallpaper", "Redrawing wallpaper");
                    if (!ImageWallpaper.this.mIsHwAccelerated) {
                        drawWallpaperWithCanvas(sh, availw, availh, xPixels, yPixels);
                    } else if (!drawWallpaperWithOpenGL(sh, availw, availh, xPixels, yPixels)) {
                        drawWallpaperWithCanvas(sh, availw, availh, xPixels, yPixels);
                    }
                    if (!ImageWallpaper.this.mIsHwAccelerated) {
                        this.mBackground = null;
                        ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                    }
                    return;
                }
                Log.d("ImageWallpaper", "Suppressed drawFrame since redraw is not needed and offsets have not changed.");
                if (!ImageWallpaper.this.mIsHwAccelerated) {
                    this.mBackground = null;
                    ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                }
            } catch (Throwable th) {
                if (!ImageWallpaper.this.mIsHwAccelerated) {
                    this.mBackground = null;
                    ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                }
            }
        }

        private void updateWallpaperLocked() {
            Throwable exception = null;
            try {
                this.mBackground = null;
                this.mBackgroundWidth = -1;
                this.mBackgroundHeight = -1;
                this.mBackground = ImageWallpaper.this.mWallpaperManager.getBitmap();
                this.mBackgroundWidth = this.mBackground.getWidth();
                this.mBackgroundHeight = this.mBackground.getHeight();
            } catch (Throwable e) {
                exception = e;
            } catch (Throwable e2) {
                exception = e2;
            }
            if (exception != null) {
                this.mBackground = null;
                this.mBackgroundWidth = -1;
                this.mBackgroundHeight = -1;
                Log.w("ImageWallpaper", "Unable to load wallpaper!", exception);
                try {
                    ImageWallpaper.this.mWallpaperManager.clear();
                } catch (IOException ex) {
                    Log.w("ImageWallpaper", "Unable reset to default wallpaper!", ex);
                }
            }
        }

        protected void dump(String prefix, FileDescriptor fd, PrintWriter out, String[] args) {
            super.dump(prefix, fd, out, args);
            out.print(prefix);
            out.println("ImageWallpaper.DrawableEngine:");
            out.print(prefix);
            out.print(" mBackground=");
            out.print(this.mBackground);
            out.print(" mBackgroundWidth=");
            out.print(this.mBackgroundWidth);
            out.print(" mBackgroundHeight=");
            out.println(this.mBackgroundHeight);
            out.print(prefix);
            out.print(" mLastRotation=");
            out.print(this.mLastRotation);
            out.print(" mLastSurfaceWidth=");
            out.print(this.mLastSurfaceWidth);
            out.print(" mLastSurfaceHeight=");
            out.println(this.mLastSurfaceHeight);
            out.print(prefix);
            out.print(" mXOffset=");
            out.print(this.mXOffset);
            out.print(" mYOffset=");
            out.println(this.mYOffset);
            out.print(prefix);
            out.print(" mVisible=");
            out.print(this.mVisible);
            out.print(" mRedrawNeeded=");
            out.print(this.mRedrawNeeded);
            out.print(" mOffsetsChanged=");
            out.println(this.mOffsetsChanged);
            out.print(prefix);
            out.print(" mLastXTranslation=");
            out.print(this.mLastXTranslation);
            out.print(" mLastYTranslation=");
            out.print(this.mLastYTranslation);
            out.print(" mScale=");
            out.println(this.mScale);
            out.print(prefix);
            out.println(" DisplayInfo at last updateSurfaceSize:");
            out.print(prefix);
            out.print("  rotation=");
            out.print(this.mRotationAtLastSurfaceSizeUpdate);
            out.print("  width=");
            out.print(this.mDisplayWidthAtLastSurfaceSizeUpdate);
            out.print("  height=");
            out.println(this.mDisplayHeightAtLastSurfaceSizeUpdate);
        }

        private void drawWallpaperWithCanvas(SurfaceHolder sh, int w, int h, int left, int top) {
            Canvas c = sh.lockCanvas();
            if (c != null) {
                try {
                    Log.d("ImageWallpaper", "Redrawing: left=" + left + ", top=" + top);
                    float right = ((float) left) + (((float) this.mBackground.getWidth()) * this.mScale);
                    float bottom = ((float) top) + (((float) this.mBackground.getHeight()) * this.mScale);
                    if (w < 0 || h < 0) {
                        c.save(2);
                        c.clipRect((float) left, (float) top, right, bottom, Op.DIFFERENCE);
                        c.drawColor(-16777216);
                        c.restore();
                    }
                    if (this.mBackground != null) {
                        c.drawBitmap(this.mBackground, null, new RectF((float) left, (float) top, right, bottom), null);
                    }
                    sh.unlockCanvasAndPost(c);
                } catch (Throwable th) {
                    sh.unlockCanvasAndPost(c);
                }
            }
        }

        private boolean drawWallpaperWithOpenGL(SurfaceHolder sh, int w, int h, int left, int top) {
            if (!initGL(sh)) {
                return false;
            }
            float right = ((float) left) + (((float) this.mBackground.getWidth()) * this.mScale);
            float bottom = ((float) top) + (((float) this.mBackground.getHeight()) * this.mScale);
            Rect frame = sh.getSurfaceFrame();
            Matrix4f ortho = new Matrix4f();
            ortho.loadOrtho(0.0f, (float) frame.width(), (float) frame.height(), 0.0f, -1.0f, 1.0f);
            Buffer triangleVertices = createMesh(left, top, right, bottom);
            int texture = loadTexture(this.mBackground);
            int program = buildProgram("attribute vec4 position;\nattribute vec2 texCoords;\nvarying vec2 outTexCoords;\nuniform mat4 projection;\n\nvoid main(void) {\n    outTexCoords = texCoords;\n    gl_Position = projection * position;\n}\n\n", "precision mediump float;\n\nvarying vec2 outTexCoords;\nuniform sampler2D texture;\n\nvoid main(void) {\n    gl_FragColor = texture2D(texture, outTexCoords);\n}\n\n");
            int attribPosition = GLES20.glGetAttribLocation(program, "position");
            int attribTexCoords = GLES20.glGetAttribLocation(program, "texCoords");
            int uniformTexture = GLES20.glGetUniformLocation(program, "texture");
            int uniformProjection = GLES20.glGetUniformLocation(program, "projection");
            checkGlError();
            GLES20.glViewport(0, 0, frame.width(), frame.height());
            GLES20.glBindTexture(3553, texture);
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(attribPosition);
            GLES20.glEnableVertexAttribArray(attribTexCoords);
            GLES20.glUniform1i(uniformTexture, 0);
            GLES20.glUniformMatrix4fv(uniformProjection, 1, false, ortho.getArray(), 0);
            checkGlError();
            if (w > 0 || h > 0) {
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glClear(16384);
            }
            triangleVertices.position(0);
            GLES20.glVertexAttribPointer(attribPosition, 3, 5126, false, 20, triangleVertices);
            triangleVertices.position(3);
            GLES20.glVertexAttribPointer(attribTexCoords, 3, 5126, false, 20, triangleVertices);
            GLES20.glDrawArrays(5, 0, 4);
            boolean status = this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface);
            checkEglError();
            finishGL(texture, program);
            return status;
        }

        private FloatBuffer createMesh(int left, int top, float right, float bottom) {
            float[] verticesData = new float[]{(float) left, bottom, 0.0f, 0.0f, 1.0f, right, bottom, 0.0f, 1.0f, 1.0f, (float) left, (float) top, 0.0f, 0.0f, 0.0f, right, (float) top, 0.0f, 1.0f, 0.0f};
            FloatBuffer triangleVertices = ByteBuffer.allocateDirect(verticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(verticesData).position(0);
            return triangleVertices;
        }

        private int loadTexture(Bitmap bitmap) {
            int[] textures = new int[1];
            GLES20.glActiveTexture(33984);
            GLES20.glGenTextures(1, textures, 0);
            checkGlError();
            int texture = textures[0];
            GLES20.glBindTexture(3553, texture);
            checkGlError();
            GLES20.glTexParameteri(3553, 10241, 9729);
            GLES20.glTexParameteri(3553, 10240, 9729);
            GLES20.glTexParameteri(3553, 10242, 33071);
            GLES20.glTexParameteri(3553, 10243, 33071);
            GLUtils.texImage2D(3553, 0, 6408, bitmap, 5121, 0);
            checkGlError();
            return texture;
        }

        private int buildProgram(String vertex, String fragment) {
            int vertexShader = buildShader(vertex, 35633);
            if (vertexShader == 0) {
                return 0;
            }
            int fragmentShader = buildShader(fragment, 35632);
            if (fragmentShader == 0) {
                return 0;
            }
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            checkGlError();
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, 35714, status, 0);
            if (status[0] == 1) {
                return program;
            }
            Log.d("ImageWallpaperGL", "Error while linking program:\n" + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }

        private int buildShader(String source, int type) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            checkGlError();
            GLES20.glCompileShader(shader);
            checkGlError();
            int[] status = new int[1];
            GLES20.glGetShaderiv(shader, 35713, status, 0);
            if (status[0] == 1) {
                return shader;
            }
            Log.d("ImageWallpaperGL", "Error while compiling shader:\n" + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        private void checkEglError() {
            int error = this.mEgl.eglGetError();
            if (error != 12288) {
                Log.w("ImageWallpaperGL", "EGL error = " + GLUtils.getEGLErrorString(error));
            }
        }

        private void checkGlError() {
            int error = GLES20.glGetError();
            if (error != 0) {
                Log.w("ImageWallpaperGL", "GL error = 0x" + Integer.toHexString(error), new Throwable());
            }
        }

        private void finishGL(int texture, int program) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
            GLES20.glDeleteProgram(program);
            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            this.mEgl.eglDestroySurface(this.mEglDisplay, this.mEglSurface);
            this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
            this.mEgl.eglTerminate(this.mEglDisplay);
        }

        private boolean initGL(SurfaceHolder surfaceHolder) {
            this.mEgl = (EGL10) EGLContext.getEGL();
            this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
            if (this.mEgl.eglInitialize(this.mEglDisplay, new int[2])) {
                this.mEglConfig = chooseEglConfig();
                if (this.mEglConfig == null) {
                    throw new RuntimeException("eglConfig not initialized");
                }
                this.mEglContext = createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
                if (this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                    throw new RuntimeException("createContext failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
                }
                EGLSurface tmpSurface = this.mEgl.eglCreatePbufferSurface(this.mEglDisplay, this.mEglConfig, new int[]{12375, 1, 12374, 1, 12344});
                this.mEgl.eglMakeCurrent(this.mEglDisplay, tmpSurface, tmpSurface, this.mEglContext);
                int[] maxSize = new int[1];
                Rect frame = surfaceHolder.getSurfaceFrame();
                GLES20.glGetIntegerv(3379, maxSize, 0);
                this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                this.mEgl.eglDestroySurface(this.mEglDisplay, tmpSurface);
                if (frame.width() > maxSize[0] || frame.height() > maxSize[0]) {
                    this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
                    this.mEgl.eglTerminate(this.mEglDisplay);
                    Log.e("ImageWallpaperGL", "requested  texture size " + frame.width() + "x" + frame.height() + " exceeds the support maximum of " + maxSize[0] + "x" + maxSize[0]);
                    return false;
                }
                this.mEglSurface = this.mEgl.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig, surfaceHolder, null);
                if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                    int error = this.mEgl.eglGetError();
                    if (error == 12299 || error == 12291) {
                        Log.e("ImageWallpaperGL", "createWindowSurface returned " + GLUtils.getEGLErrorString(error) + ".");
                        return false;
                    }
                    throw new RuntimeException("createWindowSurface failed " + GLUtils.getEGLErrorString(error));
                } else if (this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                    return true;
                } else {
                    throw new RuntimeException("eglMakeCurrent failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
                }
            }
            throw new RuntimeException("eglInitialize failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
        }

        EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, 12344});
        }

        private EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            if (!this.mEgl.eglChooseConfig(this.mEglDisplay, getConfig(), configs, 1, configsCount)) {
                throw new IllegalArgumentException("eglChooseConfig failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            } else if (configsCount[0] > 0) {
                return configs[0];
            } else {
                return null;
            }
        }

        private int[] getConfig() {
            return new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12327, 12344, 12344};
        }
    }

    public void onCreate() {
        super.onCreate();
        this.mWallpaperManager = (WallpaperManager) getSystemService("wallpaper");
        if (!isEmulator()) {
            boolean z;
            if (ActivityManager.isHighEndGfx()) {
                z = true;
            } else {
                z = SystemProperties.getBoolean("ro.config.low_ram", true);
            }
            this.mIsHwAccelerated = z;
        }
    }

    public void onTrimMemory(int level) {
        if (this.mEngine != null) {
            this.mEngine.trimMemory(level);
        }
    }

    private static boolean isEmulator() {
        return FeatureOptionUtils.SUPPORT_YES.equals(SystemProperties.get("ro.kernel.qemu", "0"));
    }

    public Engine onCreateEngine() {
        this.mEngine = new DrawableEngine();
        return this.mEngine;
    }
}
