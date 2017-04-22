package android.support.v4.media.session;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.ResultReceiver;

final class MediaSessionCompat$ResultReceiverWrapper implements Parcelable {
    public static final Creator<MediaSessionCompat$ResultReceiverWrapper> CREATOR = new C00121();
    private ResultReceiver mResultReceiver;

    static class C00121 implements Creator<MediaSessionCompat$ResultReceiverWrapper> {
        C00121() {
        }

        public MediaSessionCompat$ResultReceiverWrapper createFromParcel(Parcel p) {
            return new MediaSessionCompat$ResultReceiverWrapper(p);
        }

        public MediaSessionCompat$ResultReceiverWrapper[] newArray(int size) {
            return new MediaSessionCompat$ResultReceiverWrapper[size];
        }
    }

    MediaSessionCompat$ResultReceiverWrapper(Parcel in) {
        this.mResultReceiver = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        this.mResultReceiver.writeToParcel(dest, flags);
    }
}
