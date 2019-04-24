package com.example.cabinetlib.utils;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ByteUtil {
    /**
     * 无符号byte
     *
     * @param data
     * @return
     */
    public static int toUnsignedInt(String data) {
        return Integer.parseInt(data, 16);
    }

    /**
     * 无符号byte
     *
     * @param data
     * @return
     */
    public static int toUnsignedInt(byte data) {
        return data & 0xff;
    }

    /**
     * 无符号byte
     *
     * @param datas
     * @return
     */
    public static int[] toUnsignedInts(byte... datas) {
        int[] result = new int[datas.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = toUnsignedInt(datas[i]);
        }
        return result;
    }

    /**
     * 无符号byte
     *
     * @param datas
     * @return
     */
    public static int[] toUnsignedInts(String... datas) {
        int[] result = new int[datas.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = toUnsignedInt(datas[i]);
        }
        return result;
    }

    /**
     * 有符号byte
     *
     * @param data
     * @return
     */
    public static byte toByte(int data) {
        return (byte) data;
    }

    /**
     * 有符号byte
     *
     * @param datas
     * @return
     */
    public static byte[] toBytes(int... datas) {
        byte[] result = new byte[datas.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) datas[i];
        }

        return result;
    }

    public static String to16Hex(Byte data) {
        String hex = Integer.toHexString(toUnsignedInt(data));
        hex = StringUtils.leftPad(hex, 2, "0").toUpperCase();
        return hex;
    }

    public static String to16Hex(Integer data) {
        String hex = Integer.toHexString(data);
        hex = StringUtils.leftPad(hex, 2, "0").toUpperCase();
        return hex;
    }

    public static String to16Hexs(Byte... datas) {
        if (datas.length == 0) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (Byte b : datas) {
            list.add(to16Hex(b));
        }

        return StringUtils.join(" ", list);
    }

    public static String to16Hexsstr(byte[] arr) {
        String hexsstr;
        List<String> to16Hexs2 = ByteUtil.to16Hexs2(arr);
        StringBuilder builder = new StringBuilder();
        for (String s : to16Hexs2) {
            builder.append(s);
        }
        hexsstr = builder.toString().trim();
        return hexsstr;
    }

    public static List<String> to16Hexs2(Byte... datas) {
        if (datas.length == 0) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (Byte b : datas) {
            list.add(to16Hex(b));
        }
        return list;
    }


    public static String byteArrayToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        StringBuilder hexBuilder = new StringBuilder();
        for (byte aByte : src) {
            int data = aByte & 0xff;
            String hexStr = Integer.toHexString(data);
            hexBuilder.append(hexStr);
            hexBuilder.append(" ");
        }
        return hexBuilder.toString().trim();
    }


    public static String to16Hexs(byte... datas) {
        return to16Hexs(ArrayUtils.toObject(datas));
    }

    public static List<String> to16Hexs2(byte... datas) {
        return to16Hexs2(ArrayUtils.toObject(datas));
    }

    public static String to16Hexs(Integer... datas) {
        if (datas.length == 0) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (Integer i : datas) {
            list.add(to16Hex(i));
        }

        return StringUtils.join(" ", list);
    }


}
