package com.android.systemui.screenshot;

import android.app.Notification;
import android.app.Notification.BigPictureStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore.Images.Media;
import com.android.systemui.R;
import com.android.systemui.screenshot.GlobalScreenshot.DeleteScreenshotReceiver;
import com.android.systemui.screenshot.GlobalScreenshot.TargetChosenReceiver;
import com.mediatek.storage.StorageManagerEx;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/* compiled from: GlobalScreenshot */
class SaveImageInBackgroundTask extends AsyncTask<SaveImageInBackgroundData, Void, SaveImageInBackgroundData> {
    private static boolean mTickerAddSpace;
    private final String mImageFileName;
    private final String mImageFilePath;
    private final int mImageHeight;
    private final long mImageTime = System.currentTimeMillis();
    private final int mImageWidth;
    private final Builder mNotificationBuilder;
    private final int mNotificationId;
    private final NotificationManager mNotificationManager;
    private final BigPictureStyle mNotificationStyle;
    private final Builder mPublicNotificationBuilder;
    private final File mScreenshotDir;

    SaveImageInBackgroundTask(Context context, SaveImageInBackgroundData data, NotificationManager nManager, int nId) {
        Resources r = context.getResources();
        String imageDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(this.mImageTime));
        this.mImageFileName = String.format("Screenshot_%s.png", new Object[]{imageDate});
        this.mScreenshotDir = new File(StorageManagerEx.getDefaultPath() + "/" + Environment.DIRECTORY_PICTURES, "Screenshots");
        this.mImageFilePath = new File(this.mScreenshotDir, this.mImageFileName).getAbsolutePath();
        this.mImageWidth = data.image.getWidth();
        this.mImageHeight = data.image.getHeight();
        int iconSize = data.iconSize;
        int previewWidth = data.previewWidth;
        int previewHeight = data.previewheight;
        Canvas c = new Canvas();
        Paint paint = new Paint();
        ColorMatrix desat = new ColorMatrix();
        desat.setSaturation(0.25f);
        paint.setColorFilter(new ColorMatrixColorFilter(desat));
        Matrix matrix = new Matrix();
        Bitmap picture = Bitmap.createBitmap(previewWidth, previewHeight, data.image.getConfig());
        matrix.setTranslate((float) ((previewWidth - this.mImageWidth) / 2), (float) ((previewHeight - this.mImageHeight) / 2));
        c.setBitmap(picture);
        c.drawBitmap(data.image, matrix, paint);
        c.drawColor(1090519039);
        c.setBitmap(null);
        float scale = ((float) iconSize) / ((float) Math.min(this.mImageWidth, this.mImageHeight));
        Bitmap icon = Bitmap.createBitmap(iconSize, iconSize, data.image.getConfig());
        matrix.setScale(scale, scale);
        matrix.postTranslate((((float) iconSize) - (((float) this.mImageWidth) * scale)) / 2.0f, (((float) iconSize) - (((float) this.mImageHeight) * scale)) / 2.0f);
        c.setBitmap(icon);
        c.drawBitmap(data.image, matrix, paint);
        c.drawColor(1090519039);
        c.setBitmap(null);
        mTickerAddSpace = !mTickerAddSpace;
        this.mNotificationId = nId;
        this.mNotificationManager = nManager;
        long now = System.currentTimeMillis();
        this.mNotificationBuilder = new Builder(context).setTicker(r.getString(R.string.screenshot_saving_ticker) + (mTickerAddSpace ? " " : "")).setContentTitle(r.getString(R.string.screenshot_saving_title)).setContentText(r.getString(R.string.screenshot_saving_text)).setSmallIcon(R.drawable.stat_notify_image).setWhen(now).setColor(r.getColor(17170521));
        this.mNotificationStyle = new BigPictureStyle().bigPicture(picture.createAshmemBitmap());
        this.mNotificationBuilder.setStyle(this.mNotificationStyle);
        this.mPublicNotificationBuilder = new Builder(context).setContentTitle(r.getString(R.string.screenshot_saving_title)).setContentText(r.getString(R.string.screenshot_saving_text)).setSmallIcon(R.drawable.stat_notify_image).setCategory("progress").setWhen(now).setColor(r.getColor(17170521));
        this.mNotificationBuilder.setPublicVersion(this.mPublicNotificationBuilder.build());
        Notification n = this.mNotificationBuilder.build();
        n.flags |= 32;
        this.mNotificationManager.notify(nId, n);
        this.mNotificationBuilder.setLargeIcon(icon.createAshmemBitmap());
        this.mNotificationStyle.bigLargeIcon((Bitmap) null);
    }

    protected SaveImageInBackgroundData doInBackground(SaveImageInBackgroundData... params) {
        if (params.length != 1) {
            return null;
        }
        if (isCancelled()) {
            params[0].clearImage();
            params[0].clearContext();
            return null;
        }
        Process.setThreadPriority(-2);
        Context context = params[0].context;
        Bitmap image = params[0].image;
        Resources r = context.getResources();
        try {
            this.mScreenshotDir.mkdirs();
            long dateSeconds = this.mImageTime / 1000;
            OutputStream out = new FileOutputStream(this.mImageFilePath);
            image.compress(CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            ContentValues values = new ContentValues();
            ContentResolver resolver = context.getContentResolver();
            values.put("_data", this.mImageFilePath);
            values.put("title", this.mImageFileName);
            values.put("_display_name", this.mImageFileName);
            values.put("datetaken", Long.valueOf(this.mImageTime));
            values.put("date_added", Long.valueOf(dateSeconds));
            values.put("date_modified", Long.valueOf(dateSeconds));
            values.put("mime_type", "image/png");
            values.put("width", Integer.valueOf(this.mImageWidth));
            values.put("height", Integer.valueOf(this.mImageHeight));
            values.put("_size", Long.valueOf(new File(this.mImageFilePath).length()));
            Uri uri = resolver.insert(Media.EXTERNAL_CONTENT_URI, values);
            String subjectDate = DateFormat.getDateTimeInstance().format(new Date(this.mImageTime));
            String subject = String.format("Screenshot (%s)", new Object[]{subjectDate});
            Intent sharingIntent = new Intent("android.intent.action.SEND");
            sharingIntent.setType("image/png");
            sharingIntent.putExtra("android.intent.extra.STREAM", uri);
            sharingIntent.putExtra("android.intent.extra.SUBJECT", subject);
            Intent chooserIntent = Intent.createChooser(sharingIntent, null, PendingIntent.getBroadcast(context, 0, new Intent(context, TargetChosenReceiver.class).putExtra("android:cancel_id", this.mNotificationId), 1342177280).getIntentSender());
            chooserIntent.addFlags(268468224);
            this.mNotificationBuilder.addAction(R.drawable.ic_screenshot_share, r.getString(17040457), PendingIntent.getActivity(context, 0, chooserIntent, 268435456));
            PendingIntent deleteAction = PendingIntent.getBroadcast(context, 0, new Intent(context, DeleteScreenshotReceiver.class).putExtra("android:cancel_id", this.mNotificationId).putExtra("android:screenshot_uri_id", uri.toString()), 1342177280);
            this.mNotificationBuilder.addAction(R.drawable.ic_screenshot_delete, r.getString(17040186), deleteAction);
            params[0].imageUri = uri;
            params[0].image = null;
            params[0].result = 0;
        } catch (Exception e) {
            params[0].clearImage();
            params[0].result = 1;
        }
        if (image != null) {
            image.recycle();
        }
        return params[0];
    }

    protected void onPostExecute(SaveImageInBackgroundData params) {
        if (isCancelled()) {
            params.finisher.run();
            params.clearImage();
            params.clearContext();
            return;
        }
        if (params.result > 0) {
            GlobalScreenshot.notifyScreenshotError(params.context, this.mNotificationManager);
        } else {
            Resources r = params.context.getResources();
            Intent launchIntent = new Intent("android.intent.action.VIEW");
            launchIntent.setDataAndType(params.imageUri, "image/png");
            launchIntent.setFlags(268435456);
            long now = System.currentTimeMillis();
            this.mNotificationBuilder.setContentTitle(r.getString(R.string.screenshot_saved_title)).setContentText(r.getString(R.string.screenshot_saved_text)).setContentIntent(PendingIntent.getActivity(params.context, 0, launchIntent, 0)).setWhen(now).setAutoCancel(true).setColor(r.getColor(17170521));
            this.mPublicNotificationBuilder.setContentTitle(r.getString(R.string.screenshot_saved_title)).setContentText(r.getString(R.string.screenshot_saved_text)).setContentIntent(PendingIntent.getActivity(params.context, 0, launchIntent, 0)).setWhen(now).setAutoCancel(true).setColor(r.getColor(17170521));
            this.mNotificationBuilder.setPublicVersion(this.mPublicNotificationBuilder.build());
            Notification n = this.mNotificationBuilder.build();
            n.flags &= -33;
            this.mNotificationManager.notify(this.mNotificationId, n);
        }
        params.finisher.run();
        params.clearContext();
    }
}
