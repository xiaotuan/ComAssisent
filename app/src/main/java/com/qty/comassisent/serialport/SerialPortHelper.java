package com.qty.comassisent.serialport;

import android.content.Context;
import android.text.TextUtils;

import com.qty.comassisent.util.Utils;
import com.qty.comassisent.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class SerialPortHelper {

    private static final int SLEEP_TIME = 500;

    private Context mContext;
    private SerialPortConfig mConfig;
    private SerialPort mSerialPort;
    private SerialPortCallback mCallback;
    private ReadThread mReadThread;

    public SerialPortHelper(Context context, SerialPortConfig config) {
        Log.d(this, "[" + config.name + "]SerialPortHelper=>path: " + config.path + ", baudRate: " + config.baudRate);
        mContext = context;
        mConfig = config;
    }

    public void setCallback(SerialPortCallback cb) {
        mCallback = cb;
    }

    public boolean open() {
        boolean success = true;
        try {
            mSerialPort = new SerialPort(new File(mConfig.path), mConfig.baudRate, 0);
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (Exception e) {
            Log.e(this, "[" + mConfig.name + "]open=>error: ", e);
            close();
            success = false;
        }
        return success;
    }

    public void close() {
        if (mReadThread != null) {
            mReadThread.stopThread();
            mReadThread = null;
        }
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    public boolean sendCommand(String command) {
        boolean success = false;
        if (command != null) {
            if (mSerialPort != null) {
                try {
                    byte[] data;
                    if (mConfig.isSendTxt) {
                        data = command.getBytes();
                    } else {
                        command.replaceAll(" ", "");
                        data = Utils.HexToByteArr(command);
                    }
                    data = appendLineBreakToData(data);
                    mSerialPort.getOutputStream().write(data);
                    mSerialPort.getOutputStream().flush();
                    success = true;
                } catch (Exception e) {
                    Log.e(this, "[" + mConfig.name + "]sendCommand=>error: ", e);
                }
            } else {
                Log.w(this, "[" + mConfig.name + "]sendCommand=>Serial port is not opened.");
            }
        } else {
            Log.w(this, "[" + mConfig.name + "]sendCommand=>command is null.");
        }
        return success;
    }

    private byte[] appendLineBreakToData(byte[] data) {
        if (TextUtils.isEmpty(mConfig.sendLineBreak.trim())) {
            return data;
        }
        String[] linebreaks = mConfig.sendLineBreak.trim().split(" ");
        byte[] endData = Arrays.copyOf(data, data.length + linebreaks.length);
        for (int i = 0; i < linebreaks.length; i++) {
            try {
                endData[data.length + i] = Byte.parseByte(linebreaks[i], 16);
            } catch (Exception e) {
                Log.e(this, "[" + mConfig.name + "]appendLineBreakToData=>error: ", e);
                return null;
            }
        }
        return endData;
    }

    private class ReadThread extends Thread {

        private ByteArrayOutputStream datas = new ByteArrayOutputStream();
        private boolean isStoped = false;

        @Override
        public void run() {
            InputStream in = mSerialPort.getInputStream();
            try {
                int length;
                byte[] buffer;
                while (!isStoped && in != null) {
                    length = in.available();
//                    Log.d(this, "[" + mConfig.name + "]run=>length: " + length);
                    if (length > 0) {
                        buffer = new byte[length];
                        in.read(buffer);
                        datas.write(buffer);
                        byte[] completedData = getCompletedData();
                        if (completedData != null && mCallback != null) {
                            String dataStr = getCompletedDataString(completedData);
                            mCallback.onReceiveData(mConfig, dataStr);
                        }
                    } else {
                        try { Thread.sleep(SLEEP_TIME); } catch (Exception ignore) {}
                    }
                }
            } catch (Exception e) {
                Log.d(this, "[" + mConfig.name + "]run=>error: ", e);
                if (mCallback != null) {
                    mCallback.onError(mConfig);
                }
            }
            if (!isStoped) {
                close();
            }
            Log.d(this, "[" + mConfig.name + "]run=>ReadThread end.");
        }

        private byte[] getCompletedData() throws IOException {
            if (TextUtils.isEmpty(mConfig.receiveLineBreak.trim())) {
                byte[] data = datas.toByteArray();
                datas.reset();
                return data;
            }
            String[] breaks = mConfig.receiveLineBreak.trim().split(" ");
            byte[] completedData = null;
            byte[] allDatas = datas.toByteArray();
            int lineBreakIndex = findLineBreak(allDatas, breaks);
//            Log.d(this, "[" + mConfig.name + "]getCompletedData=>length: " + allDatas.length + ", index: " + lineBreakIndex + ", data: " + Utils.ByteArrToHex(allDatas));
            if (lineBreakIndex != -1) {
                completedData = Arrays.copyOfRange(allDatas, 0, lineBreakIndex);
                byte[] uncompletedData = Arrays.copyOfRange(allDatas, lineBreakIndex + breaks.length, allDatas.length);
                datas.reset();
                datas.write(uncompletedData);
            }
            return completedData;
        }

        private int findLineBreak(byte[] datas, String[] breaks) {
            int index = -1;
            for (int i = 0; i < datas.length; i++) {
                if (i + breaks.length < datas.length) {
                    boolean find = true;
                    for (int j = i, k = 0; j < datas.length && k < breaks.length; j++, k++) {
                        if (datas[j] != Byte.parseByte(breaks[k], 16)) {
                            find = false;
                        }
                    }
                    if (find) {
                        index = i;
                        break;
                    }
                }
            }
            return index;
        }

        private String getCompletedDataString(byte[] data) {
            if (mConfig.isReceiveShowTxt) {
                return new String(data);
            } else {
                return Utils.ByteArrToHex(data);
            }
        }

        public void stopThread() {
            isStoped = true;
            try {
                interrupt();
            } catch (Exception ignore) {}
        }
    }

    public interface SerialPortCallback {
        void onError(SerialPortConfig config);
        void onReceiveData(SerialPortConfig config, String data);
    }

}
