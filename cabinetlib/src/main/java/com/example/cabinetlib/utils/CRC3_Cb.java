package com.example.cabinetlib.utils;

public class CRC3_Cb {


    //TODO------------------------------工具栏-----------------------------------------------------------

    //需要做无符号操作，16位。。。。CRC3
    public static int getCRC3(int crc, byte[] buffer, int length) {
        int i, j;
        for (i = 0; i < length; i++) {
            j = (crc >> 8) ^ ByteUtil_Cb.toUnsignedInt(buffer[i]);
            crc = (crc << 8) ^ _crc(j);
        }
        return getUnsignedByte((short) crc);
    }

    private static int getUnsignedByte(short data) {      //将data字节型数据转换为0~65535 (0xFFFF 即 WORD)。
        return data & 0x0FFFF;
    }

    private static int _crc(int n) {
        int i, acc;
        for (n <<= 8, acc = 0, i = 8; i > 0; i--, n <<= 1) {//16位的一半32768（65535/2）
            if (((n ^ acc) & 0x8000) == getUnsignedByte((short) 32768)) {//
                acc = (acc << 1) ^ 0x1021;
            } else {
                acc = (acc << 1);
            }
        }
        return getUnsignedByte((short) acc);
    }

    public static String intToHex(int n) {
        //StringBuffer s = new StringBuffer();
        StringBuilder sb = new StringBuilder(8);
        String a;
        char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        while (n != 0) {
            sb = sb.append(b[n % 16]);
            n = n / 16;
        }
        a = sb.reverse().toString();
        return a;
    }

    /**
     * int转byte数组
     *
     * @param num
     * @return bytes
     */
    public static byte[] longToByte(long num) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((num >> 24) & 0xff);
        bytes[1] = (byte) ((num >> 16) & 0xff);
        bytes[2] = (byte) ((num >> 8) & 0xff);
        bytes[3] = (byte) (num & 0xff);
        return bytes;
    }

    //byte 与 int 的相互转换
    public static byte intToByte(int x) {
        return (byte) x;
    }

    public static int byteToInt(byte b) {
        //Java 总是把 byte 当做有符处理；我们可以通过将其和 0xFF 进行二进制与得到它的无符值
        return b & 0xFF;
    }

    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    //TODO------------------------------工具栏------------------------------------------------------------
}
