package com.example.cabinetlib.comm;

public final class ErrorStatus_Cb {

    public static final String NOT_BURN_ERROR = "4009";//没有新的烧录档
    public static final String NOT_BURN_VERSION_ERROR = "4010";//新版本烧录档取芯片型号or版本号失败
    public static final String UPADTE_SUCCESSFUL = "4011";//更新成功
    public static final String UPADTE_FAILURE_ERROR = "4012";//更新失败
    public static final String Not_SN_kW = "4013";//没有孔位，SN 为null
    public static final String x01 = "x01";//"允许在线更新充电宝程序，同时返回程序烧录的起始地址";
    public static final String x02 = "x02";//"充电宝通信超时";
    public static final String x03 = "x03";//"充电宝地址错误";
    public static final String x04 = "x04";//"充电宝程序CRC16校验失败";
    public static final String x0A = "x0A";//"该孔位没有充电宝";
    public static final String x0B = "x0B";//"该孔位充电宝SN号不一致";
    public static final String x0C = "x0C";//"该孔位充电宝MCU型号不一致";
    public static final String x0D = "x0D";//"该孔位充电宝已经是最新版本程序";
    public static final String x0E = "x0E";//"充电宝程序超出最大程序空间范围";
    public static final String x46 = "x46";//"机柜和充电宝异常，没回调信息";


}
