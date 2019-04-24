package com.example.cabinetlib.utils;

import java.util.Arrays;


public class ByteStringUtil {

    //16进制字符串，转二进制数组
    public static byte[] hexStrToByteArray(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new byte[0];
        }
        byte[] byteArray = new byte[str.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            String subStr = str.substring(2 * i, 2 * i + 2);
            byteArray[i] = ((byte) Integer.parseInt(subStr, 16));
        }
        return byteArray;
    }

    //二进制数组，转十六进制字符串
    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //byte ---》 String
    public static String byteArrayToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        StringBuilder hexBuilder = new StringBuilder();
        for (byte aByte : src) {
            int data = aByte & 0xff;
            String hexStr = Integer.toHexString(data);
            hexBuilder.append(hexStr);
            hexBuilder.append(",");
        }
        return hexBuilder.toString().trim();
    }

    public static String StrToAddHexStr(String[] strings) {
        long all = Long.parseLong("0", 16);
        for (int i = 0; i < strings.length; i++) {
            long one = Long.parseLong(strings[i], 16);
            all = all + one;
        }
        return Long.toHexString(256 - (all % 256));
    }



    public static void main(String[] args) {
//        byte[] arr = new byte[]{85, -86, 1, 0, 1, 0, 1};
//        System.out.println(Arrays.toString(hexStrToByteArray("55AA0100010001")));
//        System.out.println(byteArrayToHexStr(arr));

        byte[] arr=  hexStrToByteArray("0xC08D");
        System.out.println(arr[0]);

    }
}