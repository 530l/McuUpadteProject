package com.example.cabinetlib;


/**
 * 使用者调用
 */
public interface ChargingBoxProxyCall {
    void successful(String successcode);

    void failure(String errcode, byte resutl6);


}
