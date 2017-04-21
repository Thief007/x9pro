package com.mediatek.keyguard.PowerOffAlarm;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.HashMap;

public final class Alarm implements Parcelable {
    public static final Creator<Alarm> CREATOR = new Creator<Alarm>() {
        public Alarm createFromParcel(Parcel p) {
            return new Alarm(p);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };
    Uri alert;
    DaysOfWeek daysOfWeek;
    boolean enabled;
    int hour;
    int id;
    String label;
    int minutes;
    boolean silent;
    long time;
    boolean vibrate;

    static final class DaysOfWeek {
        private static int[] DAY_MAP = new int[]{2, 3, 4, 5, 6, 7, 1};
        private static HashMap<Integer, Integer> DAY_TO_BIT_MASK = new HashMap();
        private int mDays;

        static {
            for (int i = 0; i < DAY_MAP.length; i++) {
                DAY_TO_BIT_MASK.put(Integer.valueOf(DAY_MAP[i]), Integer.valueOf(i));
            }
        }

        DaysOfWeek(int days) {
            this.mDays = days;
        }

        public int getCoded() {
            return this.mDays;
        }

        public String toString() {
            return "DaysOfWeek{mDays=" + this.mDays + '}';
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel p, int flags) {
        int i;
        int i2 = 1;
        p.writeInt(this.id);
        if (this.enabled) {
            i = 1;
        } else {
            i = 0;
        }
        p.writeInt(i);
        p.writeInt(this.hour);
        p.writeInt(this.minutes);
        p.writeInt(this.daysOfWeek.getCoded());
        p.writeLong(this.time);
        if (this.vibrate) {
            i = 1;
        } else {
            i = 0;
        }
        p.writeInt(i);
        p.writeString(this.label);
        p.writeParcelable(this.alert, flags);
        if (!this.silent) {
            i2 = 0;
        }
        p.writeInt(i2);
    }

    public String toString() {
        return "Alarm{alert=" + this.alert + ", id=" + this.id + ", enabled=" + this.enabled + ", hour=" + this.hour + ", minutes=" + this.minutes + ", daysOfWeek=" + this.daysOfWeek + ", time=" + this.time + ", vibrate=" + this.vibrate + ", label='" + this.label + '\'' + ", silent=" + this.silent + '}';
    }

    public Alarm(Parcel p) {
        boolean z;
        boolean z2 = true;
        this.id = p.readInt();
        if (p.readInt() == 1) {
            z = true;
        } else {
            z = false;
        }
        this.enabled = z;
        this.hour = p.readInt();
        this.minutes = p.readInt();
        this.daysOfWeek = new DaysOfWeek(p.readInt());
        this.time = p.readLong();
        if (p.readInt() == 1) {
            z = true;
        } else {
            z = false;
        }
        this.vibrate = z;
        this.label = p.readString();
        this.alert = (Uri) p.readParcelable(null);
        if (p.readInt() != 1) {
            z2 = false;
        }
        this.silent = z2;
    }

    public Alarm() {
        this.id = -1;
        this.hour = 0;
        this.minutes = 0;
        this.vibrate = true;
        this.daysOfWeek = new DaysOfWeek(0);
        this.label = "";
        this.alert = RingtoneManager.getDefaultUri(4);
    }

    public int hashCode() {
        return this.id;
    }

    public boolean equals(Object o) {
        boolean z = false;
        if (!(o instanceof Alarm)) {
            return false;
        }
        if (this.id == ((Alarm) o).id) {
            z = true;
        }
        return z;
    }
}
