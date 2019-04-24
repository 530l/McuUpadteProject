package com.mcuupdate.arr;

public final class ErrorStatus {

    public static final String NOT_BURN_ERROR = "4003";//没有新的烧录档
    public static final String NOT_BURN_VERSION_ERROR = "4004";//新版本烧录档取芯片型号or版本号失败
    public static final String UPADTE_SUCCESSFUL = "4005";//更新成功
    public static final String UPADTE_FAILURE_ERROR = "4006";//更新失败
    public static final String ALREADY_NEW_UPADTE_ERROR = "4007";//当前已经是最新版本
    public static final String MCUCODE_ERROR = "4008";//MCU版本号不一致

}
