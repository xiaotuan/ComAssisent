package com.qty.comassisent;

import android.Manifest;

public class ComAssisentConstants {

    // 权限
    public static final String[] PERMISIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * 串口数据文件存储的文件夹名称
     */
    public static final String DATA_FILE_DIR = "SerialPortData";
    /**
     * 串口数据文件的名称
     */
    public static final String DATA_FILE_NAME = "data";
}
