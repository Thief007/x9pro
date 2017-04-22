package android.support.v4.widget;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.view.View.BaseSavedState;

class DrawerLayout$SavedState extends BaseSavedState {
    public static final Creator<DrawerLayout$SavedState> CREATOR = new C00321();
    int lockModeLeft = 0;
    int lockModeRight = 0;
    int openDrawerGravity = 0;

    static class C00321 implements Creator<DrawerLayout$SavedState> {
        C00321() {
        }

        public DrawerLayout$SavedState createFromParcel(Parcel source) {
            return new DrawerLayout$SavedState(source);
        }

        public DrawerLayout$SavedState[] newArray(int size) {
            return new DrawerLayout$SavedState[size];
        }
    }

    public DrawerLayout$SavedState(Parcel in) {
        super(in);
        this.openDrawerGravity = in.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.openDrawerGravity);
    }
}
