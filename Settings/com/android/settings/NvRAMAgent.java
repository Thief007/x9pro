package com.android.settings;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface NvRAMAgent extends IInterface {

    public static abstract class Stub extends Binder implements NvRAMAgent {

        private static class Proxy implements NvRAMAgent {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public byte[] readFile(int file_lid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("NvRAMAgent");
                    _data.writeInt(file_lid);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int writeFile(int file_lid, byte[] buff) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("NvRAMAgent");
                    _data.writeInt(file_lid);
                    _data.writeByteArray(buff);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "NvRAMAgent");
        }

        public static NvRAMAgent asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("NvRAMAgent");
            if (iin == null || !(iin instanceof NvRAMAgent)) {
                return new Proxy(obj);
            }
            return (NvRAMAgent) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface("NvRAMAgent");
                    byte[] _result = readFile(data.readInt());
                    reply.writeNoException();
                    reply.writeByteArray(_result);
                    return true;
                case 2:
                    data.enforceInterface("NvRAMAgent");
                    int _result2 = writeFile(data.readInt(), data.createByteArray());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 1598968902:
                    reply.writeString("NvRAMAgent");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    byte[] readFile(int i) throws RemoteException;

    int writeFile(int i, byte[] bArr) throws RemoteException;
}
