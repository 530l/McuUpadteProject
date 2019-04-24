package com.example.cabinetlib.utils;


import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class McuIntUtil {

    //MCU 的整形与java的整形转换
    public static int toInt(byte b1, byte b2, byte b3, byte b4) {
        List<String> list = new ArrayList<>();
        list.add(ByteUtil.to16Hex(b1));
        list.add(ByteUtil.to16Hex(b2));
        list.add(ByteUtil.to16Hex(b3));
        list.add(ByteUtil.to16Hex(b4));
        //反转
        Collections.reverse(list);
        //合并
        String hex = StringUtils.join(list.toArray(), "");
        return ByteUtil.toUnsignedInt(hex);
    }


    public static byte[] toBytes(int data) {
        String hex = Integer.toHexString(data);
        hex = StringUtils.leftPad(hex, 8, "0");

        int d1 = Integer.valueOf(hex.substring(0, 2), 16);
        int d2 = Integer.valueOf(hex.substring(2, 4), 16);
        int d3 = Integer.valueOf(hex.substring(4, 6), 16);
        int d4 = Integer.valueOf(hex.substring(6), 16);

        byte[] bytes = new byte[4];
        bytes[0] = ByteUtil.toByte(d4);
        bytes[1] = ByteUtil.toByte(d3);
        bytes[2] = ByteUtil.toByte(d2);
        bytes[3] = ByteUtil.toByte(d1);

        return bytes;
    }

//    public static void main(String[] args) {
//        int data1 = toInt(ByteUtil.toByte(0X00), ByteUtil.toByte(0X10), ByteUtil.toByte(0X00), ByteUtil.toByte(0X00));
//        System.out.println(data1);
//    }


}
