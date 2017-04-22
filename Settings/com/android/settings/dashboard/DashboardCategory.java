package com.android.settings.dashboard;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class DashboardCategory implements Parcelable {
    public static final Creator<DashboardCategory> CREATOR = new C03181();
    public int externalIndex = -1;
    public long id = -1;
    public String key;
    public List<DashboardTile> tiles = new ArrayList();
    public CharSequence title;
    public int titleRes;

    static class C03181 implements Creator<DashboardCategory> {
        C03181() {
        }

        public DashboardCategory createFromParcel(Parcel source) {
            return new DashboardCategory(source);
        }

        public DashboardCategory[] newArray(int size) {
            return new DashboardCategory[size];
        }
    }

    public void addTile(DashboardTile tile) {
        this.tiles.add(tile);
    }

    public void addTile(int n, DashboardTile tile) {
        this.tiles.add(n, tile);
    }

    public void removeTile(int n) {
        this.tiles.remove(n);
    }

    public int getTilesCount() {
        return this.tiles.size();
    }

    public DashboardTile getTile(int n) {
        return (DashboardTile) this.tiles.get(n);
    }

    public CharSequence getTitle(Resources res) {
        if (this.titleRes != 0) {
            return res.getText(this.titleRes);
        }
        return this.title;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.titleRes);
        dest.writeInt(this.externalIndex);
        TextUtils.writeToParcel(this.title, dest, flags);
        dest.writeString(this.key);
        int count = this.tiles.size();
        dest.writeInt(count);
        for (int n = 0; n < count; n++) {
            ((DashboardTile) this.tiles.get(n)).writeToParcel(dest, flags);
        }
    }

    public void readFromParcel(Parcel in) {
        this.titleRes = in.readInt();
        this.externalIndex = in.readInt();
        this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.key = in.readString();
        int count = in.readInt();
        for (int n = 0; n < count; n++) {
            this.tiles.add((DashboardTile) DashboardTile.CREATOR.createFromParcel(in));
        }
    }

    DashboardCategory(Parcel in) {
        readFromParcel(in);
    }
}
