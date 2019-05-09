package com.mcuupadteproject;

import com.example.cabinetlib.utils.ByteUtil_Cb;
import com.mcuupdate.arr.UpdateUtils;
import com.mcuupdate.utils.ASCIIUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.McuIntUtil;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);

    }


    private static String intToHex(int n) {
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
     * 十进制整数转十六进制byte
     *
     * @param n
     * @return
     */
    private Byte intToHexByte(int n) {
        //StringBuffer s = new StringBuffer();
        StringBuilder sb = new StringBuilder(8);
        String a;
        char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        while (n != 0) {
            sb = sb.append(b[n % 16]);
            n = n / 16;
        }
        a = sb.reverse().toString();
        return Byte.parseByte(a, 16);
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

    @Test
    public void show2() {
//        int a=0x1234;
        int a = 5;
        byte low = (byte) (a & 0xff);
        byte hig = (byte) (a >> 15);
        System.out.println("高8位是：" + hig + ", 低8位是：" + low);
    }

    @Test
    public void show3() {
        byte b = (byte) 500;//-128-127
        System.out.println(b);

    }

    @Test
    public void showall() {
        byte[] item = {1, 2, 3};//烧录的数据
        byte[] programS = {4, 5, 6};
        byte[] programE = {7, 8, 9};//结束
        //[4, 5, 6, 7, 8, 9]
        byte[] temp = ArrayUtils.addAll(programS, programE);
        System.out.println(Arrays.toString(temp));
        //[4, 5, 6, 7, 8, 9, 1, 2, 3]
        byte[] content = ArrayUtils.addAll(temp, item);
        System.out.println(Arrays.toString(content));
        //[1, 2, 3, 4, 5, 6, 7, 8, 9]
        byte[] content2 = ArrayUtils.addAll(item, temp);
        System.out.println(Arrays.toString(content2));
    }

    @Test
    public void showk() {
        int UnsignedInt1 = ByteUtil_Cb.toUnsignedInt((byte) 0x16);
        int UnsignedInt2 = ByteUtil_Cb.toUnsignedInt((byte) 0x60);
        int UnsignedInt3 = ByteUtil_Cb.toUnsignedInt((byte) 0x67);
        System.out.println(UnsignedInt1 + "----" + UnsignedInt2+"------"+UnsignedInt3);

    }
}