package com.example.cabinetlib;

import com.example.cabinetlib.bean.AppInfo_Cb;
import com.example.cabinetlib.utils.ByteStringUtil_Cb;
import com.example.cabinetlib.utils.ByteUtil_Cb;

public class ChargingBoxUtils {

    /**
     * @return [0]烧录档的版本号  [1]芯片型号
     */
    public static String[] getNewBurnFileVersion() {
        String burnVsersion = "", chipVersion = "";
        byte[] buffer = AppInfo_Cb.getInstance().bytes;
        if (buffer != null) {
            byte[] version = new byte[]{buffer[2060], buffer[2061], buffer[2062], buffer[2063], buffer[2064]};
            int[] ints = ByteUtil_Cb.toUnsignedInts(version);
            burnVsersion = "" + (char) ints[0] + (char) ints[1] + (char) ints[2] + (char) ints[3] + (char) ints[4];//版本
            byte[] chipVersionArr = new byte[]{buffer[2072], buffer[2073], buffer[2074], buffer[2075]};//芯片型号
            chipVersion = ByteStringUtil_Cb.byteArrayToHexStr(chipVersionArr);
            return new String[]{burnVsersion, chipVersion};
        }
        return null;
    }
}
