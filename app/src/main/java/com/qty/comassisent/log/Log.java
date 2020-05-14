package com.qty.comassisent.log;

public class Log {

    private static final String TAG = "Comassisent";

    private static final boolean DEBUG = true;
    private static final boolean INFO = true;
    private static final boolean WARN = true;
    private static final boolean ERROR = true;

    public static void d(String msg) {
        Log.d(null, msg, null);
    }

    public static void d(String msg, Throwable tr) {
        Log.d(null, msg, tr);
    }

    public static void d(Object prefix, String msg) {
        Log.d(prefix, msg, null);
    }

    public static void d(Object prefix, String msg, Throwable tr) {
        if (DEBUG) {
            android.util.Log.d(TAG, getPrefix(prefix) + msg, tr);
        }
    }

    public static void i(String msg) {
        Log.i(null, msg, null);
    }

    public static void i(String msg, Throwable tr) {
        Log.i(null, msg, tr);
    }

    public static void i(Object prefix, String msg) {
        Log.i(prefix, msg, null);
    }

    public static void i(Object prefix, String msg, Throwable tr) {
        if (INFO) {
            android.util.Log.i(TAG, getPrefix(prefix) + msg, tr);
        }
    }

    public static void w(String msg) {
        Log.w(null, msg, null);
    }

    public static void w(String msg, Throwable tr) {
        Log.w(null, msg, tr);
    }

    public static void w(Object prefix, String msg) {
        Log.w(prefix, msg, null);
    }

    public static void w(Object prefix, String msg, Throwable tr) {
        if (WARN) {
            android.util.Log.w(TAG, getPrefix(prefix) + msg, tr);
        }
    }

    public static void e(String msg) {
        Log.e(null, msg, null);
    }

    public static void e(String msg, Throwable tr) {
        Log.e(null, msg, tr);
    }

    public static void e(Object prefix, String msg) {
        Log.e(prefix, msg, null);
    }

    public static void e(Object prefix, String msg, Throwable tr) {
        if (ERROR) {
            android.util.Log.e(TAG, getPrefix(prefix) + msg, tr);
        }
    }

    private static String getPrefix(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return "[" + (String)obj + "]";
        return "[" + obj.getClass().getSimpleName() + "]";
    }
}
