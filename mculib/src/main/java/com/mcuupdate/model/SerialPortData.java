package com.mcuupdate.model;




import com.mcuupdate.exception.SerialPortVerifyException;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.VerifyUtil;

import org.apache.commons.lang3.ArrayUtils;

public class SerialPortData {
    private byte[] bytes;

    private int head;

    private int length;

    private int cmd;

    private int type;

    private int not1;

    private int[] data;

    private int[] crc;



    public SerialPortData(byte[] bytes) throws SerialPortVerifyException {
        System.out.println(ByteUtil.to16Hexs(ArrayUtils.toObject(bytes)));
        this.bytes = bytes;
        if(bytes == null || bytes.length < 7){
            throw new SerialPortVerifyException("数据包至少7个字节");
        }

        int[] unsignedInts = ByteUtil.toUnsignedInts(bytes);
        if(!(unsignedInts[0] == 0X55 || unsignedInts[0] == 0XA8)){
            throw new SerialPortVerifyException("头码错误：" + ByteUtil.to16Hex(unsignedInts[0]) + " RIGHT：0X55 || 0XA8");
        }

        if(unsignedInts[1] != bytes.length){
            throw new SerialPortVerifyException("长度错误：" + ByteUtil.to16Hex(unsignedInts[1]) + " RIGHT：" + ByteUtil.to16Hex(bytes.length));
        }

        if(unsignedInts[0] == 0X55) {
            int[] arr4 = ArrayUtils.subarray(unsignedInts, 0, 4);
            int not1 = VerifyUtil.getNOT1Value(arr4);
            if (not1 != unsignedInts[4]) {
                throw new SerialPortVerifyException("补码错误：" + ByteUtil.to16Hex(not1) + " RIGHT：" + ByteUtil.to16Hex(unsignedInts[4]));
            }

            int[] crc = ArrayUtils.subarray(unsignedInts, bytes.length - 2, bytes.length);
            if ((unsignedInts[bytes.length - 2] != crc[0]) || (unsignedInts[bytes.length - 1] != crc[1])) {
                StringBuilder sb = new StringBuilder();
                sb.append("CRC错误：");
                sb.append(ByteUtil.to16Hexs(crc[0], crc[1]));
                sb.append(" RIGHT：");
                sb.append(ByteUtil.to16Hexs(unsignedInts[bytes.length - 2], unsignedInts[bytes.length - 1]));
                throw new SerialPortVerifyException(sb.toString());
            }
        }

        this.head = unsignedInts[0];
        this.length = unsignedInts[1];
        this.cmd = unsignedInts[2];
        this.type = unsignedInts[3];
        this.not1 = not1;
        if(this.length > 7){
            this.data = ArrayUtils.subarray(unsignedInts, 5, bytes.length - 2);
        }
        this.crc = crc;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getLength() {
        return length;
    }

    public int getCmd() {
        return cmd;
    }

    public int getType() {
        return type;
    }

    public int[] getCrc() {
        return crc;
    }

    public int getNot1() {
        return not1;
    }

    public String get16Hex(){
        Byte[] bs = ArrayUtils.toObject(bytes);
        return ByteUtil.to16Hexs(bs);
    }
}
