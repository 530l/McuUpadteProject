package com.mcuupadteproject;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cabinetlib.comm.CabinetOrder_Cb;
import com.example.cabinetlib.utils.CRC3_Cb;
import com.example.cabinetlib.utils.LogUtils_Cb;
import com.example.cabinetlib.utils.SerialPortUtil_Cb;
import com.example.cabinetlib.bean.Platform_Cb;
import com.example.cabinetlib.bean.AppInfo_Cb;
import com.mcuupdate.utils.ByteStringUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.McuIntUtil;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

    SerialPortUtil_Cb serialPortUtil;
    AppInfo_Cb equipmentData = AppInfo_Cb.getInstance();//移动电源设备数据
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
            equipmentData.platform = Platform_Cb.MTK_6373;
        } else if (Build.BRAND.equals("qcom")) {
            equipmentData.platform = Platform_Cb.QCOME_SC20J;
        }
        //打开串口
        serialPortUtil = SerialPortUtil_Cb.getInstance();
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
        dataArray.add((byte) 0x99);//SN99
        dataArray.add((byte) 0x00);//SN00
        dataArray.add((byte) 0x5A);//SN5A

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
        byte[] bytes = ByteStringUtil.hexStrToByteArray(CRC3_Cb.intToHex(equipmentData.bytes.length));
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

        //java发送给硬件的，都是无符号，如果我们这边有符号的数超过了值，就需要转为无符号了
        //为什么有时候需要唔符号，例如byte b =(byte) 500; 这个b=-12，这时候就是有符号的了，给硬件，显示不行
        System.out.println("CRC........>>>>" + CRC3_Cb. getCRC3(0, equipmentData.bytes, equipmentData.bytes.length));
        byte[] CRC16arr = ByteStringUtil.hexStrToByteArray(CRC3_Cb.intToHex(CRC3_Cb.getCRC3(0, equipmentData.bytes,
                equipmentData.bytes.length)));
        List<Byte> CRC16list = new ArrayList<>();
        for (byte aByte : CRC16arr) {
            CRC16list.add(aByte);
        }
        Collections.reverse(CRC16list);
        for (Byte temp : CRC16list) {
            dataArray.add(temp);
        }
        serialPortUtil.sendOrderWithData(CabinetOrder_Cb.query_instruct, dataArray, new SerialPortUtil_Cb.DealCallback() {
            @Override
            public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                String hexString = serialPortUtil.byteArrayToHexString(byteSrc);
                LogUtils_Cb.i(hexString);//a8 00 0b 61 10 02 00 00 00 00 da
                send0x60Result(byteSrc);
            }
        });
    }


    void send0x60Result(byte[] byteSrc) {
        byte resutl = byteSrc[5];
        LogUtils_Cb.i(resutl + "!!!");
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
        // IAP占用空间大小，起始地址 a8 00 0c 61 10 01 06 00 00 10 00 c4
        int IAPCodePage = McuIntUtil.toInt(byteSrc[7], byteSrc[8], byteSrc[9], byteSrc[10]);//反转
        equipmentData.IAPCodePage = IAPCodePage;
        byte[] begin = McuIntUtil.toBytes(IAPCodePage);
        byte[] end = McuIntUtil.toBytes(IAPCodePage + equipmentData.bytes.length-1);//AAp大小，机器大小
        byte[] data = ArrayUtils.addAll(begin, end);
        int[] ints = ByteUtil.toUnsignedInts(data);
        byte[] bytes = ByteUtil.toBytes(ints);
//        equipmentData.startandendaddress.clear();
        for (byte b : bytes) {
            dataArray.add(b);
//            equipmentData.startandendaddress.add(b); //起始地址,结束地址
        }
        //
        serialPortUtil.sendOrderWithData(CabinetOrder_Cb.erase_instruct, dataArray, new SerialPortUtil_Cb.DealCallback() {
            @Override
            public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                String hexString = serialPortUtil.byteArrayToHexString(byteSrc);
                LogUtils_Cb.i(serialPortUtil.serialFormatting(hexString));
                send0x62Result(byteSrc);
            }
        });
    }


    //a8 00 07 63 10 01 xx

    void send0x62Result(byte[] byteSrc) {
        byte resutl = byteSrc[5];
        LogUtils_Cb.i(resutl + "!!!");
        if (resutl == 0x01) {
            LogUtils_Cb.i("充电宝程序擦除成功");
            initFileData();
            sendserialPortProcess7();
        } else {
            LogUtils_Cb.i("充电宝程序擦除失败");
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

        List<Byte> dataArray = new ArrayList<>();

        dataArray.add((byte) 0x10);//地址

        //烧录档数据
        int begin = burnindex * pageSize;
        int end = (burnindex + 1) * pageSize;
        //0-64,在fileDatas中截取
        byte[] item = ArrayUtils.subarray(fileDatas, begin, end);//烧录的数据

        //起始地址是ipa,结束地址是该数据长度+ipa
        //起始地址每一段都不一样，都是以发送的该段作为起始。
        byte[] programS = McuIntUtil.toBytes(begin + iapCodePage);//开始 start
        byte[] programE = null;//结束
        if (burnindex == (page - 1)) {//最后一次，所有的数据+iap
            programE = McuIntUtil.toBytes(fileSize + iapCodePage - 1);
        } else {//过程中
            programE = McuIntUtil.toBytes(end + iapCodePage - 1);
        }
        //programS+programE+item  起始地址+结束地址+烧录数据=该数据的数据
        byte[] content = ArrayUtils.addAll(ArrayUtils.addAll(programS, programE), item);
     /*   if (ByteUtil_Cb.toUnsignedInt(content[4]) == 0X40) {
            System.out.println("------------------所发生的放松放松");
            content[4] = ByteUtil_Cb.toByte(0X3F);
        }*/
        int[] intscontent = ByteUtil.toUnsignedInts(content);
        byte[] bytescontent = ByteUtil.toBytes(intscontent);
        for (byte b : bytescontent) {
            dataArray.add(b);
        }

        if (page - burnindex == 1) {
            serialPortUtil.sendOrderWithData(CabinetOrder_Cb.write_instruct, dataArray,
                    new SerialPortUtil_Cb.DealCallback() {
                        @Override
                        public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                            LogUtils_Cb.i("开始升级----->完成");
                            UpgradeComplete(byteSrc);
                        }
                    });
        } else {
            serialPortUtil.sendOrderWithData(CabinetOrder_Cb.write_instruct, dataArray,
                    new SerialPortUtil_Cb.DealCallback() {
                        @Override
                        public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                            burnindex++;
                            sendserialPortProcess7();
                            LogUtils_Cb.i("开始升级----->" + burnindex);
                        }
                    });
        }
    }

    private void UpgradeComplete(byte[] bytes) {

    }



}
