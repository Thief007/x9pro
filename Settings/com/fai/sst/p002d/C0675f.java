package com.fai.sst.p002d;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* compiled from: Unknown */
public class C0675f {
    private static final int f55a = Integer.parseInt(C0671a.f46x);

    public static long m33a(InputStream inputStream, OutputStream outputStream) {
        return C0675f.m34a(inputStream, outputStream, new byte[1024]);
    }

    public static long m34a(InputStream inputStream, OutputStream outputStream, byte[] bArr) {
        long j = 0;
        if (inputStream != null && outputStream != null) {
            if (C0678j.m40a(bArr)) {
                bArr = new byte[1024];
            }
            while (true) {
                int read = inputStream.read(bArr);
                if (-1 == read) {
                    break;
                }
                C0675f.m36a(bArr);
                outputStream.write(bArr, 0, read);
                j += (long) read;
            }
        }
        return j;
    }

    public static void m35a(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    public static void m36a(byte[] bArr) {
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) ((byte) (bArr[i] ^ f55a));
        }
    }
}
