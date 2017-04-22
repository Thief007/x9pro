package android.support.v4.widget;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.view.View.BaseSavedState;

class SlidingPaneLayout$SavedState extends BaseSavedState {
    public static final Creator<SlidingPaneLayout$SavedState> CREATOR = new C00341();
    boolean isOpen;

    static class C00341 implements Creator<SlidingPaneLayout$SavedState> {
        C00341() {
        }

        public SlidingPaneLayout$SavedState createFromParcel(Parcel in) {
            return new SlidingPaneLayout$SavedState(in);
        }

        public SlidingPaneLayout$SavedState[] newArray(int size) {
            return new SlidingPaneLayout$SavedState[size];
        }
    }

    private SlidingPaneLayout$SavedState(Parcel in) {
        boolean z = false;
        super(in);
        if (in.readInt() != 0) {
            z = true;
        }
        this.isOpen = z;
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(this.isOpen ? 1 : 0);
    }
}
