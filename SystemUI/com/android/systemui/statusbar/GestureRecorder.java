package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;

public class GestureRecorder {
    public static final String TAG = GestureRecorder.class.getSimpleName();
    private Gesture mCurrentGesture;
    private LinkedList<Gesture> mGestures;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 6351) {
                GestureRecorder.this.save();
            }
        }
    };
    private int mLastSaveLen = -1;
    private String mLogfile;

    public class Gesture {
        boolean mComplete = false;
        long mDownTime = -1;
        private LinkedList<Record> mRecords = new LinkedList();
        private HashSet<String> mTags = new HashSet();

        public abstract class Record {
            long time;

            public abstract String toJson();
        }

        public class TagRecord extends Record {
            public String info;
            public String tag;

            public TagRecord(long when, String tag, String info) {
                super();
                this.time = when;
                this.tag = tag;
                this.info = info;
            }

            public String toJson() {
                return String.format("{\"type\":\"tag\", \"time\":%d, \"tag\":\"%s\", \"info\":\"%s\"}", new Object[]{Long.valueOf(this.time), this.tag, this.info});
            }
        }

        public void tag(long when, String tag, String info) {
            this.mRecords.add(new TagRecord(when, tag, info));
            this.mTags.add(tag);
        }

        public boolean isComplete() {
            return this.mComplete;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            sb.append("[");
            for (Record r : this.mRecords) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(r.toJson());
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public GestureRecorder(String filename) {
        this.mLogfile = filename;
        this.mGestures = new LinkedList();
        this.mCurrentGesture = null;
    }

    public void tag(long when, String tag, String info) {
        synchronized (this.mGestures) {
            if (this.mCurrentGesture == null) {
                this.mCurrentGesture = new Gesture();
                this.mGestures.add(this.mCurrentGesture);
            }
            this.mCurrentGesture.tag(when, tag, info);
        }
        saveLater();
    }

    public void tag(String tag, String info) {
        tag(SystemClock.uptimeMillis(), tag, info);
    }

    public String toJsonLocked() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("[");
        int count = 0;
        for (Gesture g : this.mGestures) {
            if (g.isComplete()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(g.toJson());
                count++;
            }
        }
        this.mLastSaveLen = count;
        sb.append("]");
        return sb.toString();
    }

    public void saveLater() {
        this.mHandler.removeMessages(6351);
        this.mHandler.sendEmptyMessageDelayed(6351, 5000);
    }

    public void save() {
        synchronized (this.mGestures) {
            try {
                BufferedWriter w = new BufferedWriter(new FileWriter(this.mLogfile, true));
                w.append(toJsonLocked() + "\n");
                w.close();
                this.mGestures.clear();
                if (!(this.mCurrentGesture == null || this.mCurrentGesture.isComplete())) {
                    this.mGestures.add(this.mCurrentGesture);
                }
                Log.v(TAG, String.format("Wrote %d complete gestures to %s", new Object[]{Integer.valueOf(this.mLastSaveLen), this.mLogfile}));
            } catch (IOException e) {
                Log.e(TAG, String.format("Couldn't write gestures to %s", new Object[]{this.mLogfile}), e);
                this.mLastSaveLen = -1;
            }
        }
    }
}
