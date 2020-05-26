package com.qty.comassisent.serialport;

public class SerialPortConfig {

    /**
     * 串口名称
     */
    public String name;
    /**
     * 串口地址
     */
    public String path;
    /**
     * 串口频率
     */
    public int baudRate;
    /**
     * 命令文件路径
     */
    public String commandath;
    /**
     * 发送数据的格式
     */
    public boolean isSendTxt;
    /**
     * 接收数据是否显示为字符
     */
    public boolean isReceiveShowTxt;
    /**
     * 发送数据的中止符
     */
    public String sendLineBreak;
    /**
     * 接收数据的中止符
     */
    public String receiveLineBreak;
    /**
     * 是否自动发送
     */
    public boolean isAutoSend;
    /**
     * 延迟自动发送时间
     */
    public int autoSendDelay;

}
