package com.example.cabinetlib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.example.cabinetlib.bean.AppInfo_Cb;
import com.example.cabinetlib.bean.Platform_Cb;
import com.example.cabinetlib.comm.CabinetOrder_Cb;
import com.example.cabinetlib.comm.ErrorStatus_Cb;
import com.example.cabinetlib.utils.ByteStringUtil_Cb;
import com.example.cabinetlib.utils.ByteUtil_Cb;
import com.example.cabinetlib.utils.CRC3_Cb;
import com.example.cabinetlib.utils.LogUtils_Cb;
import com.example.cabinetlib.utils.McuIntUtil_Cb;
import com.example.cabinetlib.utils.SerialPortUtil_Cb;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChargingBoxProxy {


    private Timer_Cb timer_cb = null;
    public ChargingBoxProxyCall chargingBoxProxyCall;//升级回调
    public SerialPortUtil_Cb serialPortUtil_cb = SerialPortUtil_Cb.getInstance();
    public AppInfo_Cb equipmentData = AppInfo_Cb.getInstance();//移动电源设备数据
    private String[] burnVsersionArr = null;
    private byte[] fileDatas;//
    private int fileSize;//
    private int pageSize = 64;      //79 - 15 = 64字节每包内容
    private int iapCodePage;//
    private int page;//
    boolean isUpgradeComplete = false;//更新成功了，还需要再点击吗？
    private volatile static int burnindex = 0;//如果烧录完成，需要清空缓存，rest
    public static int count = 0;
    private byte resutl6;

    //1,
    public ChargingBoxProxy(final ChargingBoxProxyCall chargingBoxProxyCall) {
        this.chargingBoxProxyCall = chargingBoxProxyCall;
        //定时器，一分钟，检查更新是否成功,升级更新失败，是通过定时器判断
        timer_cb = new Timer_Cb(60000, 1000, new Timer_Cb.TimeFinish() {
            @Override
            public void onFinish() {
                if (!isUpgradeComplete) {//更新失败¬
                    timingFailure();
                }
            }
        });
        //
        //打开串口
        if (Build.BRAND.equals("alps")) {
            equipmentData.platform = Platform_Cb.MTK_6373;
        } else if (Build.BRAND.equals("qcom")) {
            equipmentData.platform = Platform_Cb.QCOME_SC20J;
        }
        serialPortUtil_cb.setChargingBoxProxy(this);
    }

    //2,
    public void setUpadteBuffer(byte[] bytes) {
        equipmentData.bytes = bytes;
    }

    //3.
    public void checkUpdate(List<Byte> bytesparams) {
        if (equipmentData.bytes == null || equipmentData.bytes.length == 0) {
            chargingBoxProxyCall.failure(ErrorStatus_Cb.NOT_BURN_ERROR, resutl6);//没有烧录档
            return;
        }

        if (bytesparams == null || bytesparams.isEmpty()) {
            chargingBoxProxyCall.failure(ErrorStatus_Cb.Not_SN_kW, resutl6);//没有"孔位，SN 为null"
            return;
        }

        //获取需要更新的烧录档的版本号和芯片型号
        burnVsersionArr = ChargingBoxUtils.getNewBurnFileVersion();
        if (burnVsersionArr == null || burnVsersionArr.length == 0) {//新版本烧录档取芯片型号or版本号失败
            chargingBoxProxyCall.failure(ErrorStatus_Cb.NOT_BURN_VERSION_ERROR, resutl6);
            return;
        }

        //开始进行升级
        startUpadteTime();
        send0x60(bytesparams);
    }


    //Android发送查询命令
    //找到0x800,转为0x00000000 (一个int)
    //0x804 替换为长度(文件大小)
    private void send0x60(List<Byte> bytesparams) {
        count = 1;
        if (burnVsersionArr == null) return;
        //复位转接板
        List<Byte> dataArray = new ArrayList<>();
        //头码 长度  命令  校验  不用传，其他指令按顺序依次传入
        dataArray.add((byte) 0x10);//地址
        dataArray.addAll(bytesparams);

        //MCU   30020500
        byte[] buffer = equipmentData.bytes;
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
        byte[] bytes = ByteStringUtil_Cb.hexStrToByteArray(CRC3_Cb.intToHex(equipmentData.bytes.length));
        List<Byte> temps = new ArrayList<>();
        for (byte aByte : bytes) {
            temps.add(aByte);
        }
        Collections.reverse(temps);
        dataArray.addAll(temps);

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
        //System.out.println("CRC........>>>>" + CRC3_Cb.getCRC3(0, equipmentData.bytes, equipmentData.bytes.length));
        byte[] CRC16arr = ByteStringUtil_Cb.hexStrToByteArray(CRC3_Cb.intToHex(CRC3_Cb.getCRC3(0,
                equipmentData.bytes, equipmentData.bytes.length)));
        List<Byte> CRC16list = new ArrayList<>();
        for (byte aByte : CRC16arr) {
            CRC16list.add(aByte);
        }
        Collections.reverse(CRC16list);

        equipmentData.bytes[2048] = CRC16list.get(0);//0x800
        equipmentData.bytes[2049] = CRC16list.get(1); //0x801

        //
        CRC16arr = ByteStringUtil_Cb.hexStrToByteArray(CRC3_Cb.intToHex(CRC3_Cb.getCRC3(0,
                equipmentData.bytes, equipmentData.bytes.length)));
        CRC16list.clear();
        for (byte b : CRC16arr) {
            CRC16list.add(b);
        }
        Collections.reverse(CRC16list);

        for (Byte temp : CRC16list) {
            dataArray.add(temp);
        }
        serialPortUtil_cb.sendOrderWithData(CabinetOrder_Cb.query_instruct, dataArray, new SerialPortUtil_Cb.DealCallback() {
            @Override
            public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                String hexString = serialPortUtil_cb.byteArrayToHexString(byteSrc);
                send0x60Result(byteSrc);
            }
        });
    }


    private void send0x60Result(byte[] byteSrc) {
        resutl6 = byteSrc[6];
        byte resutl = byteSrc[5];
        LogUtils_Cb.i(resutl + "!!!");
        switch (resutl) {
            case (byte) 0x01://
                //允许在线更新充电宝程序，同时返回程序烧录的起始地址
                send0x62(byteSrc);
                break;
            case (byte) 0x02:
                //充电宝通信超时
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x02, resutl6);
                break;
            case (byte) 0x03:
                //充电宝地址错误
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x03, resutl6);
                break;
            case (byte) 0x0A:
                //该孔位没有充电宝
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x0A, resutl6);
                break;
            case (byte) 0x0B:
                //该孔位充电宝SN号不一致
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x0B, resutl6);
                break;
            case (byte) 0x0C:
                //该孔位充电宝MCU型号不一致
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x0C, resutl6);
                break;
            case (byte) 0x0D:
                //该孔位充电宝已经是最新版本程序
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x0D, resutl6);
                break;
            case (byte) 0x0E:
                //充电宝程序超出最大程序空间范围
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x0E, resutl6);
                break;
            case (byte) 0x46:
                //充电宝程序超出最大程序空间范围
                chargingBoxProxyCall.failure(ErrorStatus_Cb.x46, resutl6);
                break;
        }
    }

    //Android发送擦除命令：
    private void send0x62(byte[] byteSrc) {
        count = 2;
        List<Byte> dataArray = new ArrayList<>();
        dataArray.add((byte) 0x10);//地址
        // IAP占用空间大小，起始地址 a8 00 0c 61 10 01 06 00 00 10 00 c4
        int IAPCodePage = McuIntUtil_Cb.toInt(byteSrc[7], byteSrc[8], byteSrc[9], byteSrc[10]);//反转
        equipmentData.IAPCodePage = IAPCodePage;
        byte[] begin = McuIntUtil_Cb.toBytes(IAPCodePage);
        byte[] end = McuIntUtil_Cb.toBytes(IAPCodePage + equipmentData.bytes.length - 1);//AAp大小，机器大小
        byte[] data = ArrayUtils.addAll(begin, end);
        int[] ints = ByteUtil_Cb.toUnsignedInts(data);
        byte[] bytes = ByteUtil_Cb.toBytes(ints);
        for (byte b : bytes) {
            dataArray.add(b);
        }
        serialPortUtil_cb.sendOrderWithData(CabinetOrder_Cb.erase_instruct, dataArray,
                new SerialPortUtil_Cb.DealCallback() {
                    @Override
                    public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                        LogUtils_Cb.i(order + "________>指令");//a8 00 0b 61 10 02 00 00 00 00 da
                        String hexString = serialPortUtil_cb.byteArrayToHexString(byteSrc);
                        LogUtils_Cb.i(serialPortUtil_cb.serialFormatting(hexString));
                        send0x62Result(byteSrc);
                    }
                });
    }


    private void send0x62Result(byte[] byteSrc) {
        resutl6 = byteSrc[6];
        byte resutl = byteSrc[5];
        LogUtils_Cb.i(resutl + "!!!");
        if (resutl == (byte) 0x01) {
            LogUtils_Cb.i("充电宝程序擦除成功，开始升级");
            initFileData();
            startBurn();
        } else if (resutl == (byte) 0x02) {//充电宝通信超时
            chargingBoxProxyCall.failure(ErrorStatus_Cb.x02, resutl6);
            cancelUpadteTime();
        } else if (resutl == (byte) 0x46) {
            chargingBoxProxyCall.failure(ErrorStatus_Cb.x46, resutl6);
            cancelUpadteTime();
        } else if (resutl == (byte) 0x03) {
            chargingBoxProxyCall.failure(ErrorStatus_Cb.x03, resutl6);
            cancelUpadteTime();
        }
    }

    private void initFileData() {
        fileDatas = equipmentData.bytes;
        fileSize = equipmentData.bytes.length;//6080
        iapCodePage = equipmentData.IAPCodePage;
        page = (fileSize % pageSize) == 0 ? (fileSize / pageSize) : (fileSize / pageSize + 1);//
    }

    //TODO #IAP_Erase IAP_PAGE_ERASE 烧录数据
    private void startBurn() {
        if (burnindex < page) {
            burndata();
        }
    }


    private void burndata() {
        count = 3;
        List<Byte> dataArray = new ArrayList<>();
        dataArray.add((byte) 0x10);//地址
        //烧录档数据
        final int begin = burnindex * pageSize;
        int end = (burnindex + 1) * pageSize;
        //0-64,在fileDatas中截取
        byte[] item = ArrayUtils.subarray(fileDatas, begin, end);//烧录的数据
        //起始地址是ipa,结束地址是该数据长度+ipa
        //起始地址每一段都不一样，都是以发送的该段作为起始。
        byte[] programS = McuIntUtil_Cb.toBytes(begin + iapCodePage);//开始 start
        byte[] programE = null;//结束
        if (burnindex == (page - 1)) {//最后一次，所有的数据+iap
            programE = McuIntUtil_Cb.toBytes(fileSize + iapCodePage - 1);
        } else {//过程中
            programE = McuIntUtil_Cb.toBytes(end + iapCodePage - 1);
        }
        //programS+programE+item  起始地址+结束地址+烧录数据=该数据的数据
        byte[] content = ArrayUtils.addAll(ArrayUtils.addAll(programS, programE), item);
        int[] intscontent = ByteUtil_Cb.toUnsignedInts(content);
        byte[] bytescontent = ByteUtil_Cb.toBytes(intscontent);
        for (byte b : bytescontent) {
            dataArray.add(b);
        }
        if (page - burnindex == 1) {
            serialPortUtil_cb.sendOrderWithData(CabinetOrder_Cb.write_instruct, dataArray,
                    new SerialPortUtil_Cb.DealCallback() {
                        @Override
                        public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                            byte resutl = byteSrc[5];
                            resutl6 = byteSrc[6];
                            if (resutl == (byte) 0x01) {
                                LogUtils_Cb.i("开始升级----->成功");
                                UpgradeComplete(byteSrc);
                            } else {
                                LogUtils_Cb.i("开始升级----->失败");
                                UpgradeErrComplete(resutl);
                            }
                        }
                    });
        } else {
            serialPortUtil_cb.sendOrderWithData(CabinetOrder_Cb.write_instruct, dataArray,
                    new SerialPortUtil_Cb.DealCallback() {
                        @Override
                        public void dealCallback(String[] dataList, int order, byte[] byteSrc) {
                            LogUtils_Cb.i("开始升级ing--------->");
                            byte resutl = byteSrc[5];
                            resutl6 = byteSrc[6];
                            if (resutl == (byte) 0x01) {
                                burnindex++;
                                startBurn();
                            } else {
                                LogUtils_Cb.i("开始升级ing----->失败");
                                UpgradeErrComplete(resutl);
                            }
                        }
                    });
        }
    }


    //更新成功
    private void UpgradeComplete(byte[] bytes) {
        cancelUpadteTime();
        burnindex = 0;
        isUpgradeComplete = true;
        chargingBoxProxyCall.successful(ErrorStatus_Cb.UPADTE_SUCCESSFUL);

    }


    //更新完成，校验失败
    private void UpgradeErrComplete(byte b) {
        cancelUpadteTime();
        burnindex = 0;
        isUpgradeComplete = false;
        if (b == (byte) 0x02) {
            chargingBoxProxyCall.failure(ErrorStatus_Cb.x02, resutl6);
        } else if (b == (byte) 0x03) {
            chargingBoxProxyCall.failure(ErrorStatus_Cb.x03, resutl6);
        } else if (b == (byte) 0x04) {
            chargingBoxProxyCall.failure(ErrorStatus_Cb.x04, resutl6);
        } else {
            chargingBoxProxyCall.failure("烧录过程中失败，可能存在多种状态,步骤errcode", resutl6);
        }
    }

    //定时失败
    private void timingFailure() {
        burnindex = 0;
        isUpgradeComplete = false;
        chargingBoxProxyCall.failure(ErrorStatus_Cb.UPADTE_FAILURE_ERROR, resutl6);
    }


    private void startUpadteTime() {
        if (timer_cb != null)
            timer_cb.start();
    }

    public void cancelUpadteTime() {
        if (timer_cb != null)
            timer_cb.cancel();
    }

}
