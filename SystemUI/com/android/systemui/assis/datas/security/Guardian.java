package com.android.systemui.assis.datas.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Guardian {
    private static String base64_random = "sviptodo";

    public static class Base64 {
        private static final String TAG = "com.hiapk.pcsuite.contact.Base64";
        private static final byte[] decodingTable = new byte[128];
        private static final byte[] encodingTable = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 43, (byte) 47};

        static {
            int i;
            for (i = 0; i < 128; i++) {
                decodingTable[i] = (byte) -1;
            }
            for (i = 65; i <= 90; i++) {
                decodingTable[i] = (byte) ((byte) (i - 65));
            }
            for (i = 97; i <= 122; i++) {
                decodingTable[i] = (byte) ((byte) ((i - 97) + 26));
            }
            for (i = 48; i <= 57; i++) {
                decodingTable[i] = (byte) ((byte) ((i - 48) + 52));
            }
            decodingTable[43] = (byte) 62;
            decodingTable[47] = (byte) 63;
        }

        public static byte[] encode(byte[] data) {
            byte[] bytes;
            int modulus = data.length % 3;
            if (modulus != 0) {
                bytes = new byte[(((data.length / 3) + 1) * 4)];
            } else {
                bytes = new byte[((data.length * 4) / 3)];
            }
            int dataLength = data.length - modulus;
            int i = 0;
            int j = 0;
            while (i < dataLength) {
                int a1 = data[i] & 255;
                int a2 = data[i + 1] & 255;
                int a3 = data[i + 2] & 255;
                bytes[j] = (byte) encodingTable[(a1 >>> 2) & 63];
                bytes[j + 1] = (byte) encodingTable[((a1 << 4) | (a2 >>> 4)) & 63];
                bytes[j + 2] = (byte) encodingTable[((a2 << 2) | (a3 >>> 6)) & 63];
                bytes[j + 3] = (byte) encodingTable[a3 & 63];
                i += 3;
                j += 4;
            }
            int d1;
            int b2;
            switch (modulus) {
                case 1:
                    d1 = data[data.length - 1] & 255;
                    b2 = (d1 << 4) & 63;
                    bytes[bytes.length - 4] = (byte) encodingTable[(d1 >>> 2) & 63];
                    bytes[bytes.length - 3] = (byte) encodingTable[b2];
                    bytes[bytes.length - 2] = (byte) 61;
                    bytes[bytes.length - 1] = (byte) 61;
                    break;
                case 2:
                    d1 = data[data.length - 2] & 255;
                    int d2 = data[data.length - 1] & 255;
                    b2 = ((d1 << 4) | (d2 >>> 4)) & 63;
                    int b3 = (d2 << 2) & 63;
                    bytes[bytes.length - 4] = (byte) encodingTable[(d1 >>> 2) & 63];
                    bytes[bytes.length - 3] = (byte) encodingTable[b2];
                    bytes[bytes.length - 2] = (byte) encodingTable[b3];
                    bytes[bytes.length - 1] = (byte) 61;
                    break;
            }
            return bytes;
        }

        public static String encode(String data) {
            return new String(encode(data.getBytes()));
        }

        public static byte[] decode(byte[] data) {
            byte[] bytes;
            data = discardNonBase64Bytes(data);
            if (data[data.length - 2] == (byte) 61) {
                bytes = new byte[((((data.length / 4) - 1) * 3) + 1)];
            } else if (data[data.length - 1] != (byte) 61) {
                bytes = new byte[((data.length / 4) * 3)];
            } else {
                bytes = new byte[((((data.length / 4) - 1) * 3) + 2)];
            }
            int i = 0;
            int j = 0;
            while (i < data.length - 4) {
                byte b2 = (byte) decodingTable[data[i + 1]];
                byte b3 = (byte) decodingTable[data[i + 2]];
                byte b4 = (byte) decodingTable[data[i + 3]];
                bytes[j] = (byte) ((byte) ((((byte) decodingTable[data[i]]) << 2) | (b2 >> 4)));
                bytes[j + 1] = (byte) ((byte) ((b2 << 4) | (b3 >> 2)));
                bytes[j + 2] = (byte) ((byte) ((b3 << 6) | b4));
                i += 4;
                j += 3;
            }
            if (data[data.length - 2] == (byte) 61) {
                bytes[bytes.length - 1] = (byte) ((byte) ((((byte) decodingTable[data[data.length - 4]]) << 2) | (((byte) decodingTable[data[data.length - 3]]) >> 4)));
            } else if (data[data.length - 1] != (byte) 61) {
                b2 = (byte) decodingTable[data[data.length - 3]];
                b3 = (byte) decodingTable[data[data.length - 2]];
                b4 = (byte) decodingTable[data[data.length - 1]];
                bytes[bytes.length - 3] = (byte) ((byte) ((((byte) decodingTable[data[data.length - 4]]) << 2) | (b2 >> 4)));
                bytes[bytes.length - 2] = (byte) ((byte) ((b2 << 4) | (b3 >> 2)));
                bytes[bytes.length - 1] = (byte) ((byte) ((b3 << 6) | b4));
            } else {
                b2 = (byte) decodingTable[data[data.length - 3]];
                b3 = (byte) decodingTable[data[data.length - 2]];
                bytes[bytes.length - 2] = (byte) ((byte) ((((byte) decodingTable[data[data.length - 4]]) << 2) | (b2 >> 4)));
                bytes[bytes.length - 1] = (byte) ((byte) ((b2 << 4) | (b3 >> 2)));
            }
            return bytes;
        }

        public static byte[] decode(String data) {
            byte[] bytes;
            data = discardNonBase64Chars(data);
            if (data.charAt(data.length() - 2) == '=') {
                bytes = new byte[((((data.length() / 4) - 1) * 3) + 1)];
            } else if (data.charAt(data.length() - 1) != '=') {
                bytes = new byte[((data.length() / 4) * 3)];
            } else {
                bytes = new byte[((((data.length() / 4) - 1) * 3) + 2)];
            }
            int i = 0;
            int j = 0;
            while (i < data.length() - 4) {
                byte b2 = (byte) decodingTable[data.charAt(i + 1)];
                byte b3 = (byte) decodingTable[data.charAt(i + 2)];
                byte b4 = (byte) decodingTable[data.charAt(i + 3)];
                bytes[j] = (byte) ((byte) ((((byte) decodingTable[data.charAt(i)]) << 2) | (b2 >> 4)));
                bytes[j + 1] = (byte) ((byte) ((b2 << 4) | (b3 >> 2)));
                bytes[j + 2] = (byte) ((byte) ((b3 << 6) | b4));
                i += 4;
                j += 3;
            }
            if (data.charAt(data.length() - 2) == '=') {
                bytes[bytes.length - 1] = (byte) ((byte) ((((byte) decodingTable[data.charAt(data.length() - 4)]) << 2) | (((byte) decodingTable[data.charAt(data.length() - 3)]) >> 4)));
            } else if (data.charAt(data.length() - 1) != '=') {
                b2 = (byte) decodingTable[data.charAt(data.length() - 3)];
                b3 = (byte) decodingTable[data.charAt(data.length() - 2)];
                b4 = (byte) decodingTable[data.charAt(data.length() - 1)];
                bytes[bytes.length - 3] = (byte) ((byte) ((((byte) decodingTable[data.charAt(data.length() - 4)]) << 2) | (b2 >> 4)));
                bytes[bytes.length - 2] = (byte) ((byte) ((b2 << 4) | (b3 >> 2)));
                bytes[bytes.length - 1] = (byte) ((byte) ((b3 << 6) | b4));
            } else {
                b2 = (byte) decodingTable[data.charAt(data.length() - 3)];
                b3 = (byte) decodingTable[data.charAt(data.length() - 2)];
                bytes[bytes.length - 2] = (byte) ((byte) ((((byte) decodingTable[data.charAt(data.length() - 4)]) << 2) | (b2 >> 4)));
                bytes[bytes.length - 1] = (byte) ((byte) ((b2 << 4) | (b3 >> 2)));
            }
            return bytes;
        }

        public static String decode(String data, String charset) {
            if (charset == null) {
                return new String(decode(data));
            }
            try {
                return new String(decode(data), charset);
            } catch (UnsupportedEncodingException e) {
                return new String(decode(data));
            }
        }

        public static String decode(byte[] data, String charset) {
            if (charset == null) {
                return new String(decode(data));
            }
            try {
                return new String(decode(data), charset);
            } catch (UnsupportedEncodingException e) {
                return new String(decode(data));
            }
        }

        private static byte[] discardNonBase64Bytes(byte[] data) {
            byte[] temp = new byte[data.length];
            int i = 0;
            int bytesCopied = 0;
            while (i < data.length) {
                int bytesCopied2;
                if (isValidBase64Byte(data[i])) {
                    bytesCopied2 = bytesCopied + 1;
                    temp[bytesCopied] = (byte) data[i];
                } else {
                    bytesCopied2 = bytesCopied;
                }
                i++;
                bytesCopied = bytesCopied2;
            }
            byte[] newData = new byte[bytesCopied];
            System.arraycopy(temp, 0, newData, 0, bytesCopied);
            return newData;
        }

        private static String discardNonBase64Chars(String data) {
            StringBuffer sb = new StringBuffer();
            int length = data.length();
            for (int i = 0; i < length; i++) {
                if (isValidBase64Byte((byte) data.charAt(i))) {
                    sb.append(data.charAt(i));
                }
            }
            return sb.toString();
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private static boolean isValidBase64Byte(byte b) {
            if (b != (byte) 61) {
                return b >= (byte) 0 && b < Byte.MIN_VALUE && decodingTable[b] != (byte) -1;
            } else {
                return true;
            }
        }
    }

    public static class MD5 {
        private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        public static void main(String[] args) {
            System.out.println(md5sum("/init.rc"));
        }

        public static String toHexString(byte[] b) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < b.length; i++) {
                sb.append(HEX_DIGITS[(b[i] & 240) >>> 4]);
                sb.append(HEX_DIGITS[b[i] & 15]);
            }
            return sb.toString();
        }

        public static String md5sum(String filename) {
            byte[] buffer = new byte[1024];
            try {
                InputStream fis = new FileInputStream(filename);
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                while (true) {
                    int numRead = fis.read(buffer);
                    if (numRead > 0) {
                        md5.update(buffer, 0, numRead);
                    } else {
                        fis.close();
                        return toHexString(md5.digest());
                    }
                }
            } catch (Exception e) {
                System.out.println("error");
                return null;
            }
        }

        public static String md5sum(InputStream stream) {
            byte[] buffer = new byte[1024];
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                while (true) {
                    int numRead = stream.read(buffer);
                    if (numRead <= 0) {
                        return toHexString(md5.digest());
                    }
                    md5.update(buffer, 0, numRead);
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e2) {
                e2.printStackTrace();
                return null;
            }
        }
    }

    public static String md5Encode(String value) {
        String tmp = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(value.getBytes("utf8"));
            tmp = binToHex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
        return tmp;
    }

    public static String binToHex(byte[] md) {
        StringBuffer sb = new StringBuffer("");
        for (int read : md) {
            int read2;
            if (read2 < 0) {
                read2 += 256;
            }
            if (read2 < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(read2));
        }
        return sb.toString();
    }

    public static String encodeBase64(String value) {
        return base64_random + Base64.encode(value);
    }

    public static String decodeBase64(String value) {
        if (value == null || value.length() <= base64_random.length()) {
            return "";
        }
        return Base64.decode(value.substring(base64_random.length(), value.length()), "utf-8");
    }
}
