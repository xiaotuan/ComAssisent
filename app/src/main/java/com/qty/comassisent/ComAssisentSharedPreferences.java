package com.qty.comassisent;

import android.content.Context;
import android.content.SharedPreferences;

public class ComAssisentSharedPreferences {

    private static final String PREFERENCE_NAME = "ComAssisent";

    /**
     * 窗口数量
     */
    public static final String PREF_WINDOW_COUNT = "window_count";
    /**
     * 自动清除
     */
    public static final String PREF_AUTO_CLEAR = "auto_clear";
    /**
     * 最大行数
     */
    public static final String PREF_MAX_LINE = "max_line";
    /**
     * 命令文件路径
     */
    public static final String PREF_CMD_FILE_PATH = "cmd_file_path";
    /**
     * 日志视图文字大小
     */
    public static final String PREF_LOG_VIEW_TEXT_SIZE = "log_view_text_size";
    /**
     * 日志格式
     */
    public static final String PREF_LOG_FOMART_INDEX = "log_fomart_index";
    /**
     * 内置命令序列文件路径
     */
    public static final String PREF_INNER_CMD_FILE_PATH = "inner_cmd_file_path";
    /**
     * 串口频率
     */
    public static final String PREF_BAUD_RATE = "baud_rate";
    /**
     * 发送数据的格式
     */
    public static final String PREF_SEND_TXT = "send_show_txt";
    /**
     * 接收数据的显示方式
     */
    public static final String PREF_RECEIVE_SHOW_TXT = "receive_show_txt";
    /**
     * 发送数据的中止符
     */
    public static final String PREF_SEND_LINE_BREAK = "send_line_break";
    /**
     * 接收数据的中止符
     */
    public static final String PREF_RECEIVE_LINE_BREAK = "receive_line_break";
    /**
     * 自动发送
     */
    public static final String PREF_AUTO_SEND = "auto_send";
    /**
     * 自动发送延迟时间
     */
    public static final String PREF_AUTO_SEND_DELAY = "auto_send_delay";


    private SharedPreferences mSharedPreferences;

    public ComAssisentSharedPreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    public String getString(String key, String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    public int getInt(String key, int defValue) {
        return mSharedPreferences.getInt(key, defValue);
    }

    public long getLong(String key, long defValue) {
        return mSharedPreferences.getLong(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        return mSharedPreferences.getFloat(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    public boolean putString(String key, String value) {
        return mSharedPreferences.edit().putString(key, value).commit();
    }

    public boolean putInt(String key, int value) {
        return mSharedPreferences.edit().putInt(key, value).commit();
    }

    public boolean putLong(String key, long value) {
        return mSharedPreferences.edit().putLong(key,value).commit();
    }

    public boolean putFloat(String key, float value) {
        return mSharedPreferences.edit().putFloat(key, value).commit();
    }

    public boolean putBoolean(String key, boolean value) {
        return mSharedPreferences.edit().putBoolean(key, value).commit();

    }

    public boolean remove(String key) {
        return mSharedPreferences.edit().remove(key).commit();
    }
}
