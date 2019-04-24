package com.mcuupadteproject;

import com.example.cabinetlib.utils.ByteStringUtil;
import com.example.cabinetlib.utils.ByteUtil;
import com.mcuupdate.arr.UpdateUtils;
import com.mcuupdate.utils.ASCIIUtil;

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

    @Test
    public void show() {
//        byte[] arr = ByteStringUtil.hexStrToByteArray("C0");
//        System.out.println(arr[0]);//-64
//        byte[] arr1 = ByteStringUtil.hexStrToByteArray("8D");
//        System.out.println(arr1[0]);//-115
//        System.out.println(ByteStringUtil.byteArrayToHexStr(new byte[]{-64}));//
//        System.out.println(ByteStringUtil.byteArrayToHexStr(new byte[]{-115}));//
        /*      -64
                -115
                C0
                8D*/

        byte[] as = {1, 23, 3, 42, 4, 6, 7, 3, 5, 10, 39, 49, 50};
//

        byte[] as2 = ByteStringUtil.hexStrToByteArray("550F4F004D00100000532205008A1F550F4F004D0014000031220500F66A");
        if (as2 != null) {

        }

    }

    @Test
    public void  t(){
//      int[]arr=  ByteUtil.toUnsignedInts("30020500");
//      System.out.println(Arrays.toString(arr));
//
//
//     byte[]arr2=   longToByte(Long.parseLong("30020500"));
//        for (int i = 0; i <arr2.length ; i++) {
//            System.out.println(arr2[i]);
//        }
        //先把字符串转char
//        System.out.println((byte) 'V');
//        System.out.println(ASCIIUtil.toInt('V'));
//        System.out.println(intToHex(ASCIIUtil.toInt('V')));
        String str ="V1044";
        //V1044 = 0x56 0x31 0x30 0x34 0x34 0x00 0x00 0x00
//        for (int i = 0; i <str.length() ; i++) {
//            char c=   str.charAt(i);
//            System.out.println(intToHex(ASCIIUtil.toInt(c)));
//        }
        System.out.println(intToHex(11036));
        byte[]bytes= ByteStringUtil.hexStrToByteArray(intToHex(11036));
        for (int i = 0; i < bytes.length; i++) {
            System.out.println(bytes[i]);
        }
    }
    private static String intToHex(int n) {
        //StringBuffer s = new StringBuffer();
        StringBuilder sb = new StringBuilder(8);
        String a;
        char []b = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        while(n != 0){
            sb = sb.append(b[n%16]);
            n = n/16;
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
    public void show2(){
//        int a=0x1234;
        int a=5;
        byte low = (byte)(a & 0xff);
        byte hig = (byte)(a>>15);
        System.out.println("高8位是："+hig+", 低8位是："+low);
    }
}