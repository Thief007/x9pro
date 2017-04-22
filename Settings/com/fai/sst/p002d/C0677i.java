package com.fai.sst.p002d;

/* compiled from: Unknown */
public class C0677i {
    public static String m38a(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < str.length() - 1; i += 2) {
            stringBuilder.append((char) (((C0677i.m39c(str.substring(i, i + 1)) << 4) + C0677i.m39c(str.substring(i + 1, i + 2))) - 20));
        }
        return stringBuilder.toString();
    }

    private static int m39c(String str) {
        int i = 0;
        String[] strArr = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        if (str.length() == 1) {
            while (i < strArr.length) {
                if (strArr[i].equalsIgnoreCase(str)) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }
}
