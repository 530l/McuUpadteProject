package com.mcuupdate.model;


public enum Platform {

    MTK_6373("MTK_6373","M803","/sys/devices/power_bank/power_bank_mcu_rst","","/dev/ttyMT2",""),

    QCOME_SC20J("QCOME_SC20J","M802","/sys/devices/soc.0/power_bank.33/power_bank_mcu_rst","","/dev/ttyHSL1","");

//    IOT3288("IOT3288","IOT3288","","","/dev/ttyS3","/dev/ttyS0");

    private String name;

    private String model;

    private String IO1;

    private String IO2;

    private String SerialPort1;

    private String SerialPort2;

    Platform(String name, String model, String IO1, String IO2, String serialPort1, String serialPort2) {
        this.name = name;
        this.model = model;
        this.IO1 = IO1;
        this.IO2 = IO2;
        SerialPort1 = serialPort1;
        SerialPort2 = serialPort2;
    }

    public String getName() {
        return name;
    }


    public String getIO1() {
        return IO1;
    }

    public String getIO2() {
        return IO2;
    }

    public String getSerialPort1() {
        return SerialPort1;
    }

    public String getSerialPort2() {
        return SerialPort2;
    }
}
