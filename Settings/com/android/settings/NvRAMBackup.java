package com.android.settings;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface NvRAMBackup extends IInterface {

    public static abstract class Stub extends Binder implements NvRAMBackup {

        private static class Proxy implements NvRAMBackup {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public boolean saveToBin() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("NvRAMBackup");
                    this.mRemote.transact(1, _data, _reply, 0);
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "NvRAMBackup");
        }

        public static NvRAMBackup asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("NvRAMBackup");
            if (iin == null || !(iin instanceof NvRAMBackup)) {
                return new Proxy(obj);
            }
            return (NvRAMBackup) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return true;
        }
    }

    boolean saveToBin() throws RemoteException;
}
