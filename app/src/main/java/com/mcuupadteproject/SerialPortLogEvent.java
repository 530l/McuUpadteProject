package com.mcuupadteproject;

/**
 * Created by Administrator on 2018/6/22.
 */

public class SerialPortLogEvent {
    private String log;

    private int type;


    public SerialPortLogEvent(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public SerialPortLogEvent(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }
}
