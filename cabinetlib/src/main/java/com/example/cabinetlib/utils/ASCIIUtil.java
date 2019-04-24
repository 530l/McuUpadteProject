package com.example.cabinetlib.utils;


import java.io.UnsupportedEncodingException;

public class ASCIIUtil {
    public static int toInt(char c) {
        return (int) c;
    }

    public static char toChar(int i) {
        return (char) i;
    }

    public static char toChar(byte i) {
        return toChar(ByteUtil.toUnsignedInt(i));
    }

    public static byte toByte(char c) {
        return ByteUtil.toByte((int) c);
    }


/*    public static void main(String[] args) {
        //十六进制--》十进制---》ASCII
        int s = Integer.parseInt("31", 16);
        System.out.print(toChar(s));
        //00052231
//        System.out.print(toChar(Integer.parseInt("00", 16)));
//        System.out.print(toChar(Integer.parseInt("05", 16)));
//        System.out.print(toChar(Integer.parseInt("22", 16)));
//        System.out.print(toChar(Integer.parseInt("31", 16)));
//        System.out.print(convertHexToString("00052231"));

        String x16 = "00052231";
        try {
            System.out.println(x16toString(x16,"US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }*/

    public static String x16toString(String x16, String CharsetName) throws UnsupportedEncodingException {
        if (x16 == null || "".equals(x16.trim())) {
            return "";
        }
        String tempStr = "";
        byte[] b = new byte[x16.length() / 2];
        for (int i = 0; i < x16.length() / 2; i++) {
            tempStr = x16.substring(i * 2, i * 2 + 2);
            int temp = Integer.parseInt(tempStr, 16);
            b[i] = (byte) temp;
        }
        String restr = new String(b, CharsetName);
        return restr;
    }


    public static String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        // 564e3a322d302e312e34 split into two characters 56, 4e, 3a...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            // grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            // convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            // convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }
        // System.out.println(sb.toString());
        return sb.toString();
    }
}
