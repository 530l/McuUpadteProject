package com.example.cabinetlib.bean;



public class AppInfo_Cb {

    private AppInfo_Cb() {
    }

    private static AppInfo_Cb instance;

    public static AppInfo_Cb getInstance() {
        if (instance == null) {
            instance = new AppInfo_Cb();
        }
        return instance;
    }

    public Platform_Cb platform;//平台信息
    public byte[] bytes;//烧录当文件
    public int IAPCodePage;//起始地址


}
