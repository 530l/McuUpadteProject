package com.example.cabinetlib;

/**
 * @Copyright 广州市数商云网络科技有限公司
 * @Author XXC
 * @Date 2019/4/17 0017 12:42
 * Describe
 */
public class CMDInfo {

    public CMDInfo(byte[] bytes, int size) {
        this.bytes = bytes;
        this.size = size;
    }

    private byte[] bytes;
    private int size;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
