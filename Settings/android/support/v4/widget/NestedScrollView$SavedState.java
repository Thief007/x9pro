package android.support.v4.widget;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.view.View.BaseSavedState;

class NestedScrollView$SavedState extends BaseSavedState {
    public static final Creator<NestedScrollView$SavedState> CREATOR = new C00331();
    public int scrollPosition;

    static class C00331 implements Creator<NestedScrollView$SavedState> {
        C00331() {
        }

        public NestedScrollView$SavedState createFromParcel(Parcel in) {
            return new NestedScrollView$SavedState(in);
        }

        public NestedScrollView$SavedState[] newArray(int size) {
            return new NestedScrollView$SavedState[size];
        }
    }

    public NestedScrollView$SavedState(Parcel source) {
        super(source);
        this.scrollPosition = source.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.scrollPosition);
    }

    public String toString() {
        return "HorizontalScrollView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " scrollPosition=" + this.scrollPosition + "}";
    }
}
