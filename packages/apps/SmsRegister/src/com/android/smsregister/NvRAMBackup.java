
package com.android.smsregister;

import android.os.IBinder;

public interface NvRAMBackup extends android.os.IInterface {
    public static abstract class Stub extends android.os.Binder implements NvRAMBackup {
        private static final java.lang.String DESCRIPTOR = "NvRAMBackup";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static NvRAMBackup asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = (android.os.IInterface) obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof NvRAMBackup))) {
                return ((NvRAMBackup) iin);
            }
            return new NvRAMBackup.Stub.Proxy(obj);
        }

        public android.os.IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply,
                int flags) throws android.os.RemoteException {
            return true;
        }

        private static class Proxy implements NvRAMBackup {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            public boolean saveToBin() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_saveToBin, _data, _reply, 0);
                    _result = (0 != _reply.readInt());
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

        }

        static final int TRANSACTION_saveToBin = (IBinder.FIRST_CALL_TRANSACTION);

    }

    public boolean saveToBin() throws android.os.RemoteException;

}
