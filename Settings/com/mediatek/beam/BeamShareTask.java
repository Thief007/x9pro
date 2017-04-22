package com.mediatek.beam;

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class BeamShareTask {
    private String mData;
    private long mDoneBytes;
    private int mId = -1;
    private String mMimeType;
    private long mModifiedDate = 0;
    private int mState;
    private long mTotalBytes;
    private int mType;

    public interface BeamShareTaskMetaData extends BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.android.settings.provider.beam.share/share_tasks");
    }

    public enum Direction {
        in,
        out
    }

    public Uri getTaskUri() {
        if (this.mId != -1) {
            return Uri.withAppendedPath(BeamShareTaskMetaData.CONTENT_URI, Integer.toString(this.mId));
        }
        throw new IllegalStateException("null id task can't get uri");
    }

    public BeamShareTask(Cursor cursor) {
        this.mId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        this.mType = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
        this.mState = cursor.getInt(cursor.getColumnIndexOrThrow("state"));
        this.mData = cursor.getString(cursor.getColumnIndexOrThrow("data"));
        this.mMimeType = cursor.getString(cursor.getColumnIndexOrThrow("mime"));
        this.mTotalBytes = cursor.getLong(cursor.getColumnIndexOrThrow("total"));
        this.mDoneBytes = cursor.getLong(cursor.getColumnIndexOrThrow("done"));
        this.mModifiedDate = cursor.getLong(cursor.getColumnIndexOrThrow("modified"));
    }

    public Direction getDirection() {
        switch (this.mType) {
            case 0:
            case 2:
                return Direction.in;
            case 1:
            case 3:
                return Direction.out;
            default:
                return Direction.out;
        }
    }

    public int getState() {
        return this.mState;
    }

    public String getMimeType() {
        return this.mMimeType;
    }

    public long getTotalBytes() {
        return this.mTotalBytes;
    }

    public long getModifiedDate() {
        return this.mModifiedDate;
    }

    public String getData() {
        return this.mData;
    }
}
