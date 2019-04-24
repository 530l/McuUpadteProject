package com.mcuupadteproject;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cabinetlib.CabinetOrder;
import com.example.cabinetlib.SerialPortUtil;
import com.example.cabinetlib.model.Platform;
import com.example.cabinetlib.model.EAppInfo;
import com.example.cabinetlib.utils.LogUtils;
import com.mcuupdate.arr.SerialPortService;
import com.mcuupdate.model.AppInfo;
import com.mcuupdate.model.SerialPortData;
import com.mcuupdate.utils.ASCIIUtil;
import com.mcuupdate.utils.ByteStringUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.McuIntUtil;
import com.mcuupdate.utils.VerifyUtil;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CabinetActivity extends AppCompatActivity {


    String src = "";

    private void showToast(String src) {
        this.src = src;
        handler.sendEmptyMessage(1);

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(CabinetActivity.this, src, Toast.LENGTH_SHORT).show();
        }
    };

    SerialPortUtil serialPortUtil;
    EAppInfo equipmentData = EAppInfo.getInstance();//移动电源设备数据
    String[] burnVsersionArr = null;
    TextView text;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cabinet_layout);
        text = findViewById(R.id.text);
        initConfig();
        setOnClickListener();

    }
    //找到0x800,转为0x00000000 (一个int)
    //0x804 替换为长度(文件大小)
    //equipmentData.bytes[2048] =-90
    //equipmentData.bytes[2049]=90
//    equipmentData.bytes[2050]=0
    //    equipmentData.bytes[2051]=0

    void initConfig() {
        //获取设备平台名称
        if (Build.BRAND.equals("alps")) {
            equipmentData.platform = Platform.MTK_6373;
        } else if (Build.BRAND.equals("qcom")) {
            equipmentData.platform = Platform.QCOME_SC20J;
        }
        //打开串口
        serialPortUtil = SerialPortUtil.getInstance();
        serialPortUtil.openSerialPort();
        //这里应该是从服务端获取的Fiel
        try {
            InputStream in = getResources().getAssets().open("PowerBank_V1044_0xC08D");
            byte[] tempbuffer = new byte[in.available()];
            in.read(tempbuffer);
            equipmentData.bytes = tempbuffer;
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取需要更新的烧录档的版本号和芯片型号
        burnVsersionArr = getNewBurnFileVersion();
    }

    /**
     * @return [0]烧录档的版本号  [1]芯片型号
     */
    public String[] getNewBurnFileVersion() {
        String burnVsersion = "", chipVersion = "";
        byte[] buffer = equipmentData.bytes;
        if (buffer != null) {
            byte[] version = new byte[]{buffer[2060], buffer[2061], buffer[2062], buffer[2063], buffer[2064]};
            int[] ints = ByteUtil.toUnsignedInts(version);
            burnVsersion = "" + (char) ints[0] + (char) ints[1] + (char) ints[2] + (char) ints[3] + (char) ints[4];//版本
            byte[] chipVersionArr = new byte[]{buffer[2072], buffer[2073], buffer[2074], buffer[2075]};//芯片型号
            chipVersion = ByteStringUtil.byteArrayToHexStr(chipVersionArr);
        }
        return new String[]{burnVsersion, chipVersion};
    }

    void setOnClickListener() {
        findViewById(R.id.btm1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send0x60();
            }
        });
    }

    //Android发送查询命令
    //找到0x800,转为0x00000000 (一个int)
    //0x804 替换为长度(文件大小)
    void send0x60() {
        if (burnVsersionArr == null) return;
//        Toast.makeText(this, "---", Toast.LENGTH_SHORT).show();
        //复位转接板
        List<Byte> dataArray = new ArrayList<>();
        //头码 长度  命令  校验  不用传，其他指令按顺序依次传入
        dataArray.add((byte) 0x10);//地址
        dataArray.add((byte) 0x03);//孔位
        dataArray.add((byte) 0x00);//SN
        dataArray.add((byte) 0x98);//SN
        dataArray.add((byte) 0xF9);//SN
        dataArray.add((byte) 0xEF);//SN

        //MCU   30020500
        byte[] buffer = equipmentData.bytes;
//     String xinhao=  burnVsersionArr[1];
//     byte[]mucarr=  ByteStringUtil.hexStrToByteArray(xinhao);
        byte[] chipVersionArr = new byte[]{buffer[2072], buffer[2073], buffer[2074], buffer[2075]};//芯片型号
        for (byte b : chipVersionArr) {
            dataArray.add(b);
        }
        //版本号
        String str = burnVsersionArr[0];
        byte[] version = new byte[]{buffer[2060], buffer[2061], buffer[2062], buffer[2063], buffer[2064]};
        for (byte b : version) {
            dataArray.add(b);
        }
        for (int i = 0; i < 8 - str.length(); i++) {
            dataArray.add((byte) 0x00);
        }

        //长度
        byte[] bytes = ByteStringUtil.hexStrToByteArray(intToHex(equipmentData.bytes.length));
        List<Byte> temps = new ArrayList<>();
        for (byte aByte : bytes) {
            temps.add(aByte);
        }
        Collections.reverse(temps);
        for (Byte temp : temps) {
            dataArray.add(temp);
        }

        //C那边的0x800,在java这里，需要把十六进制转为10进制，去byte数组查找对应的索引
        //byte[] buffer = equipmentData.bytes;
        //byte[] version = new byte[]{buffer[2048], buffer[2049], buffer[2050], buffer[2051]};
        //String  chipVersion = ByteStringUtil.byteArrayToHexStr(version);
        //System.out.println("--------------->"+chipVersion);
        equipmentData.bytes[2048] = 0;//0x800
        equipmentData.bytes[2049] = 0; //0x801
        equipmentData.bytes[2050] = 0;//0x802
        equipmentData.bytes[2051] = 0;//0x803
        equipmentData.bytes[2052] = temps.get(0);//0x804
        equipmentData.bytes[2053] = temps.get(1);//0x805
        equipmentData.bytes[2054] = 0;//0x806
        equipmentData.bytes[2055] = 0;//0x807


        System.out.println("CRC........>>>>" + getCRC3(0, equipmentData.bytes, equipmentData.bytes.length));
        byte[] CRC16arr = ByteStringUtil.hexStrToByteArray(intToHex(getCRC3(0, equipmentData.bytes,
                equipmentData.bytes.length)));
        List<Byte> CRC16list = new ArrayList<>();
        for (byte aByte : CRC16arr) {
            CRC16list.add(aByte);
        }
        Collections.reverse(CRC16list);
        for (Byte temp : CRC16list) {
            dataArray.add(temp);
        }
        serialPortUtil.sendOrderWithData(CabinetOrder.query_instruct, dataArray, new SerialPortUtil.DealCallback() {
            @Override
            public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                String hexString = serialPortUtil.byteArrayToHexString(byteSrc);
                LogUtils.i(hexString);//a8 00 0b 61 10 02 00 00 00 00 da
                send0x60Result(byteSrc);
            }
        });
    }


    void send0x60Result(byte[] byteSrc) {
        byte resutl = byteSrc[5];
        LogUtils.i(resutl + "!!!");
        switch (resutl) {
            case 0x01://
                showToast("允许在线更新充电宝程序");
                send0x62(byteSrc);
                break;
            case 0x02://
                showToast("该孔位没有充电宝");
                break;
            case 0x03://
                showToast("该孔位充电宝SN号不一致");
                break;
            case 0x04://
                showToast("该孔位充电宝MCU型号不一致");
                break;
            case 0x05://
                showToast("该孔位充电宝已经是最新版本程序");
                break;
            case 0x06://
                showToast("充电宝程序超出最大程序空间范围");
                break;
        }
    }


    //Android发送擦除命令：
    void send0x62(byte[] byteSrc) {
        List<Byte> dataArray = new ArrayList<>();
        dataArray.add((byte) 0x10);//地址

        List<Byte> startaddress = new ArrayList<>();
        startaddress.add(byteSrc[6]);//起始地址
        startaddress.add(byteSrc[7]);//起始地址
        startaddress.add(byteSrc[8]);//起始地址
        startaddress.add(byteSrc[9]);//起始地址
        dataArray.addAll(startaddress);
        equipmentData.startaddress.clear();
        equipmentData.startaddress.addAll(startaddress);
        // IAP占用空间大小
        int IAPCodePage = McuIntUtil.toInt(byteSrc[6], byteSrc[7], byteSrc[8], byteSrc[9]);//反转
        equipmentData.IAPCodePage = IAPCodePage;
        byte[] begin = McuIntUtil.toBytes(IAPCodePage);
        byte[] end = McuIntUtil.toBytes(IAPCodePage + equipmentData.bytes.length - 1);
        byte[] data = ArrayUtils.addAll(begin, end);
        int[] ints = ByteUtil.toUnsignedInts(data);
        byte[] bytes = ByteUtil.toBytes(ints);
        equipmentData.endaddress.clear();
        for (byte b : bytes) {
            equipmentData.endaddress.add(b); //结束地址
            dataArray.add(b);
        }
        //
        serialPortUtil.sendOrderWithData(CabinetOrder.erase_instruct, dataArray, new SerialPortUtil.DealCallback() {
            @Override
            public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                String hexString = serialPortUtil.byteArrayToHexString(byteSrc);
                LogUtils.i(serialPortUtil.serialFormatting(hexString));
                send0x62Result(byteSrc);
            }
        });
    }


    //a8 00 07 63 10 01 xx

    void send0x62Result(byte[] byteSrc) {
        byte resutl = byteSrc[5];
        LogUtils.i(resutl + "!!!");
        if (resutl == 0x01) {
            LogUtils.i("充电宝程序擦除成功");
            initFileData();
            sendserialPortProcess7();
        } else {
            LogUtils.i("充电宝程序擦除失败");
        }
    }


    private byte[] fileDatas;//
    private int fileSize;//
    private int pageSize = 64;      //79 - 15 = 64字节每包内容
    private int iapCodePage;//
    private int page;//
    private volatile static int burnindex = 0;//如果烧录完成，需要清空缓存，rest

    private void initFileData() {
        fileDatas = equipmentData.bytes;
        fileSize = equipmentData.bytes.length;//6080
        iapCodePage = equipmentData.IAPCodePage;
        page = (fileSize % pageSize) == 0 ? (fileSize / pageSize) : (fileSize / pageSize + 1);//
    }

    //TODO #IAP_Erase IAP_PAGE_ERASE 烧录数据
    private void sendserialPortProcess7() {
        if (burnindex < page) {
            burndata();
        }
    }

    private void burndata() {
        if (equipmentData.startaddress.isEmpty() && equipmentData.endaddress.isEmpty()) {
            showToast("起始地址数据错误，更新失败");
            return;
        }
        List<Byte> dataArray = new ArrayList<>();

        dataArray.add((byte) 0x10);//地址

        dataArray.addAll(equipmentData.startaddress);//起始地址
        dataArray.addAll(equipmentData.endaddress);//结束地址

        //烧录档数据
        int begin = burnindex * pageSize;
        int end = (burnindex + 1) * pageSize;
        byte[] item = ArrayUtils.subarray(fileDatas, begin, end);

        byte[] programS = McuIntUtil.toBytes(begin + iapCodePage);//开始
        byte[] programE = null;//结束
        if (burnindex == (page - 1)) {//最后一次
            programE = McuIntUtil.toBytes(fileSize + iapCodePage - 1);
        } else {//过程中
            programE = McuIntUtil.toBytes(end + iapCodePage - 1);
        }
        byte[] content = ArrayUtils.addAll(ArrayUtils.addAll(programS, programE), item);
        if (ByteUtil.toUnsignedInt(content[4]) == 0X40) {
            content[4] = ByteUtil.toByte(0X3F);
        }
        int[] intscontent = ByteUtil.toUnsignedInts(content);
        byte[] bytescontent = ByteUtil.toBytes(intscontent);
        for (byte b : bytescontent) {
            dataArray.add(b);
        }

        if (page - burnindex == 1) {
            serialPortUtil.sendOrderWithData(CabinetOrder.write_instruct, dataArray,
                    new SerialPortUtil.DealCallback() {
                        @Override
                        public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                            LogUtils.i("开始升级----->完成");
                            UpgradeComplete(byteSrc);
                        }
                    });
        } else {
            serialPortUtil.sendOrderWithData(CabinetOrder.write_instruct, dataArray,
                    new SerialPortUtil.DealCallback() {
                        @Override
                        public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                            burnindex++;
                            sendserialPortProcess7();
                            LogUtils.i("开始升级----->" + burnindex);
                        }
                    });
        }
    }

    private void UpgradeComplete(byte[] bytes) {

    }


    //TODO------------------------------工具栏-----------------------------------------------------------

    //需要做无符号操作，16位。。。。CRC3
    public int getCRC3(int crc, byte[] buffer, int length) {
        int i, j;
        for (i = 0; i < length; i++) {
            j = (crc >> 8) ^ ByteUtil.toUnsignedInt(buffer[i]);
            crc = (crc << 8) ^ _crc(j);
        }
        return getUnsignedByte((short) crc);
    }

    public int getUnsignedByte(short data) {      //将data字节型数据转换为0~65535 (0xFFFF 即 WORD)。
        return data & 0x0FFFF;
    }

    int _crc(int n) {
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
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    //TODO------------------------------工具栏------------------------------------------------------------
}
