package com.fai.sst.p002d;

import android.content.Context;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* compiled from: Unknown */
public class C0672b {
    public static Object m11a(Context context) {
        if (C0672b.m19h(context).exists()) {
            Object d = C0672b.m15d(context);
            if (d != null) {
                return d;
            }
        }
        return !C0672b.m16e(context) ? null : C0672b.m12a(C0672b.m13b(context), C0672b.m14c(context));
    }

    private static Object m12a(String str, String str2) {
        try {
            return Class.forName(C0671a.f43u).getDeclaredConstructor(new Class[]{String.class, String.class, String.class, ClassLoader.class}).newInstance(new Object[]{str, str2, null, C0672b.class.getClassLoader()});
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String m13b(Context context) {
        return C0672b.m19h(context).getAbsolutePath();
    }

    private static String m14c(Context context) {
        return C0672b.m20i(context).getAbsolutePath();
    }

    private static Object m15d(Context context) {
        return C0672b.m12a(C0672b.m13b(context), C0672b.m14c(context));
    }

    private static boolean m16e(Context context) {
        Closeable j;
        IOException e;
        Throwable th;
        Closeable closeable = null;
        try {
            C0672b.m17f(context);
            j = C0672b.m21j(context);
            try {
                closeable = C0672b.m18g(context);
                C0675f.m33a(j, closeable);
                C0675f.m35a(closeable);
                C0675f.m35a(j);
                return true;
            } catch (IOException e2) {
                e = e2;
                try {
                    e.printStackTrace();
                    C0675f.m35a(closeable);
                    C0675f.m35a(j);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    C0675f.m35a(closeable);
                    C0675f.m35a(j);
                    throw th;
                }
            }
        } catch (IOException e3) {
            e = e3;
            j = null;
            e.printStackTrace();
            C0675f.m35a(closeable);
            C0675f.m35a(j);
            return false;
        } catch (Throwable th3) {
            th = th3;
            j = null;
            C0675f.m35a(closeable);
            C0675f.m35a(j);
            throw th;
        }
    }

    private static void m17f(Context context) {
        File h = C0672b.m19h(context);
        h.delete();
        new File(h.getAbsolutePath().replace(".zip", ".dex")).delete();
    }

    private static OutputStream m18g(Context context) {
        return new FileOutputStream(C0672b.m19h(context));
    }

    private static File m19h(Context context) {
        return new File(C0672b.m20i(context), C0671a.f44v);
    }

    private static File m20i(Context context) {
        return context.getFilesDir();
    }

    private static InputStream m21j(Context context) {
        return context.getAssets().open(C0671a.f44v);
    }
}
