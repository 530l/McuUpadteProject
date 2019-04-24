package com.example.cabinetlib.model;

import java.util.ArrayList;
import java.util.List;

public class EAppInfo {

    private EAppInfo() {
    }

    private static EAppInfo instance;

    public static EAppInfo getInstance() {
        if (instance == null) {
            instance = new EAppInfo();
        }
        return instance;
    }

    public Platform platform;//平台信息
    public byte[] bytes;//烧录当文件
    public int IAPCodePage;
    public List<Byte> startaddress =new ArrayList<>();//起始地址
    public List<Byte> endaddress =new ArrayList<>();//结束地址


}
