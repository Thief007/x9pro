package com.android.systemui;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

public class BitmapHelper {
    public static Bitmap createCircularClip(Bitmap input, int width, int height) {
        if (input == null) {
            return null;
        }
        int inWidth = input.getWidth();
        int inHeight = input.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setShader(new BitmapShader(input, TileMode.CLAMP, TileMode.CLAMP));
        paint.setAntiAlias(true);
        RectF srcRect = new RectF(0.0f, 0.0f, (float) inWidth, (float) inHeight);
        RectF dstRect = new RectF(0.0f, 0.0f, (float) width, (float) height);
        Matrix m = new Matrix();
        m.setRectToRect(srcRect, dstRect, ScaleToFit.CENTER);
        canvas.setMatrix(m);
        canvas.drawCircle((float) (inWidth / 2), (float) (inHeight / 2), (float) (inWidth / 2), paint);
        return output;
    }
}
