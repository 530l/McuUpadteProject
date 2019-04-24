package com.example.cabinetlib.utils;

public class VerifyUtil {

    public static int CRC_XModem(byte[] bytes, int beginIndex, int endIndex) {
        int crc = 0x00;          // initial value
        int polynomial = 0x1021;
        for (int index = beginIndex; index < endIndex; index++) {
            byte b = bytes[index];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        return crc;
    }

    /**
     * CRC验证
     *
     * @param bytes
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public static int[] getCRCValue(byte[] bytes, int beginIndex, int endIndex) {
        int[] result = new int[2];
        int crc12 = CRC_XModem(bytes, beginIndex, endIndex);
        result[0] = crc12 % 256;
        result[1] = crc12 / 256;
        return result;
    }

    /**
     * 取反+1
     *
     * @return
     */
    public static int getNOT1Value(int... datas) {
        int result = 0;
        for (int data : datas) {
            result += data;
        }
        result = ~result + 1;

        String hex = Integer.toHexString(result);
        result = ByteUtil.toUnsignedInt(hex.substring(hex.length() - 2));
        return result;
    }

    /**
     * @param head  头码 0X55
     * @param cmd   命令 CMD
     * @param type  类型 TYPE
     * @param datas 数据内容
     * @return
     */

//    Byte0	  Byte1	    Byte2	Byte3	  Byte4	    Byte5~8	    Byte9~12	Byte13~n	 Byte n-1	Byte n
//    头码     长度	    命令     类型	   校验  	起始地址	    结束地址	    数据内容	            CRC校验
//    0x55	   n	    CMD	    TYPE	   补码	      XX	      XX      	  XX	                XX
    public static int[] getUnsignedInts(int head, int cmd, int type, int... datas) {
        int length = datas.length + 7;//+7 是 除了自定义内容外（起始地址，结束地址	，数据内容	） CRC校验 是2个字节
        int[] result = new int[length];
        result[0] = head;
        result[1] = length;
        result[2] = cmd;
        result[3] = type;
        //第5位 取反+1校验
        result[4] = VerifyUtil.getNOT1Value(result);

        for (int i = 0; i < datas.length; i++) {
            result[i + 5] = datas[i];
        }

        //最后2位 CRC16校验
        int[] crc16 = VerifyUtil.getCRCValue(ByteUtil.toBytes(result), 0, result.length - 2);
        result[result.length - 2] = crc16[0];
        result[result.length - 1] = crc16[1];
        return result;
    }

    /**
     * @param head  头码 0X55
     * @param cmd   命令 CMD
     * @param type  类型 TYPE
     * @param datas 数据内容
     * @return
     */
    public static byte[] getBytes(int head, int cmd, int type, int... datas) {
        int[] ints = getUnsignedInts(head, cmd, type, datas);
        byte[] bytes = ByteUtil.toBytes(ints);
        return bytes;
    }


}
