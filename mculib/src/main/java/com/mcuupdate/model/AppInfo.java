package com.mcuupdate.model;

import org.apache.commons.lang3.StringUtils;

public class AppInfo {
    private static AppInfo instance;

    public static AppInfo getInstance() {
        if (instance == null) {
            instance = new AppInfo();
        }
        return instance;
    }

    private Platform platform;

    private byte[] bytes;


    //IAP占用空间大小
    private int IAPCodePage;

    //芯片型号
    private String chipVersion;

    //旧版本信息
    private String oldVersion;

    //新版本信息
    private String newVersion;

    //正在升级
    private boolean updating;

    public void setVersion(String version) {
        if (StringUtils.isBlank(this.oldVersion)) {
            this.oldVersion = version;
        } else {
            this.newVersion = version;
        }
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public int getIAPCodePage() {
        return IAPCodePage;
    }

    public void setIAPCodePage(int IAPCodePage) {
        this.IAPCodePage = IAPCodePage;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public boolean isUpdating() {
        return updating;
    }

    public void setUpdating(boolean updating) {
        this.updating = updating;
    }

    public void setChipVersion(String chipVersion) {
        this.chipVersion = chipVersion;
    }

    public String getChipVersion() {
        return chipVersion;
    }
}
