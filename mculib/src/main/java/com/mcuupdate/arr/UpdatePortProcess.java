package com.mcuupdate.arr;


import com.mcuupdate.model.AppInfo;
import com.mcuupdate.model.SerialPortData;
import com.mcuupdate.utils.ASCIIUtil;
import com.mcuupdate.utils.ByteStringUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.LogUtils;
import com.mcuupdate.utils.McuIntUtil;
import com.mcuupdate.utils.VerifyUtil;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.List;


/**
 * 更新过程一系列指令
 */
public final class UpdatePortProcess {
    //    Byte0   Byte1     Byte2   Byte3     Byte4     Byte5~8     Byte9~12    Byte13~n     Byte n-1   Byte n
    //    头码     长度     命令     类型      校验   起始地址        结束地址        数据内容                CRC校验
    //    0x55     n        CMD     TYPE       补码         XX          XX          XX                    XX
    //    点击发送，不一定秒回，有可能要再点一下,这是因为多个APP转用了串口的
    private UpdateProxy.UpdateCallback updateCallback;
    public int count = 0;//执行到那个方法
    private byte[] fileDatas;
    private int fileSize;
    private int pageSize = 64;      //79 - 15 = 64字节每包内容
    private int iapCodePage;
    private int page;
    private volatile static int burnindex = 0;//如果烧录完成，需要清空缓存，rest

    public UpdatePortProcess(UpdateProxy.UpdateCallback updateCallback) {
        this.updateCallback = updateCallback;
    }


    // TODO AP_Connect
    public void startExecuteUpdateCommand() {//[55, 07, 06, 00, 9E, 9F, B7]
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X06, 0X00);
        count = 1;
        sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                if (serialPortData.getCmd() == 0X4F && serialPortData.getType() == 0X00) {
                    LogUtils.i("-------------连接成功----");
                    sendserialPortProcess2();
                }
            }
        }, 1);


    }


    //TODO _IAP_Info
    private void sendserialPortProcess2() {//[55, 07, 03, 00, A1, D3, 9B]
        SerialPortService.getInstance().serialPortDataList.clear();
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X03, 0X00);
        sendMessage2(bytes, new SerialPortService.SerualPrortMechanicalCllBack2() {
            @Override
            public void callbytes(byte[] buffer, List<SerialPortData> serialPortData) {
                count = 2;
                try {
                    byte[] temp = serialPortData.get(1).getBytes();
                    int size = McuIntUtil.toInt(temp[5], temp[6], temp[7], temp[8]);//反转
                    //IAP占用空间大小
                    AppInfo.getInstance().setIAPCodePage(size);

                    byte[] models = new byte[]{temp[12], temp[11], temp[10], temp[9]};
                    String chipVersion = ByteStringUtil.byteArrayToHexStr(models);//芯片型号
                    AppInfo.getInstance().setChipVersion(chipVersion); //芯片型号
                    String log = "IAP占用空间大小 IAP_CODE_PAGE= 0x"
                            + Integer.toHexString(size) + "   "
                            + AppInfo.getInstance().getIAPCodePage() + "\r\n";
                    LogUtils.e("******" + log);
                    sendserialPortProcess3();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 2);
    }


    //TODO #_IAP_Flash   CMD_READ  获取版本信息
    private void sendserialPortProcess3() {//[55, 0F, 01, 02, 99, 00, 1C, 00, 00, 17, 1C, 00, 00, 82, 2E]
        LogUtils.e("******获取版本信息--->Start");
        int iapCodePage = AppInfo.getInstance().getIAPCodePage();
        byte[] bytes = null;
        //5K
        count = 3;
        if (iapCodePage == 5120) {  //1400进制， 转10进制是 51200
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X1C, 0X00, 0X00, 0X17, 0X1C, 0X00, 0X00);
        } else if (iapCodePage == 4096) {//00 14 00 00 反转过来 00001400  十六进制，就是4090 这个 IAP占用空间大小 需要反转
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X18, 0X00, 0X00, 0X17, 0X18, 0X00, 0X00);
        }
        if (iapCodePage == 5120 || iapCodePage == 4096) {
            sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
                @Override
                public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 17; i <= 21; i++) {
                        sb.append(ASCIIUtil.toChar(ByteUtil.toUnsignedInt(serialPortData.getBytes()[i])));
                    }
                    AppInfo.getInstance().setVersion(sb.toString());
                    String log = "获取版本号 VERSION = " + sb.toString() + "\r\n";
                    LogUtils.e("******" + log);
                    sendserialPortProcess4();
                    LogUtils.e("******获取版本信息--->end");
                }
            }, 1);
        }

    }


    //TODO AP_GetBootMode
    private void sendserialPortProcess4() {//[55, 07, 08, 00, 9C, DC, 8C]
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X08, 0X00);
        count = 4;
        sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                LogUtils.e("******发送成功sendserialPortProcess4 ");
                sendserialPortProcess5();
            }
        }, 1);
    }


    // TODO AP_Reset  IAP_MODE
    private void sendserialPortProcess5() {//[55, 07, 04, 01, 9F, EF, FA]
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X04, 0X01);
        count = 5;
        sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                byte[] bytesresult = serialPortData.getBytes();
                int flag = ByteUtil.toUnsignedInt(bytesresult[2]);
                if (flag == 0X4F) {
                    LogUtils.e("******发送成功5, 0X4F = OK " + ASCIIUtil.toChar(flag));
                } else {
                    LogUtils.e("******发送成功5" + (ByteUtil.to16Hex(bytesresult[2]) + " = FAIL"));
                }
                sendserialPortProcess6();
            }
        }, 1);
    }

    //TODO #IAP_Erase IAP_PAGE_ERASE 擦除数据
    private void sendserialPortProcess6() {
        count = 6;
        byte[] begin = McuIntUtil.toBytes(AppInfo.getInstance().getIAPCodePage());
        byte[] end = McuIntUtil.toBytes(AppInfo.getInstance().getIAPCodePage()
                + AppInfo.getInstance().getBytes().length - 1);
        byte[] data = ArrayUtils.addAll(begin, end);
        byte[] datas = VerifyUtil.getBytes(0X55, 0X00, 0X08, ByteUtil.toUnsignedInts(data));
        sendMessage(datas, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                LogUtils.e("******发送成功sendserialPortProcess6");
                initFileData();
                sendserialPortProcess7();
            }
        }, 1);

    }

    private void initFileData() {
        fileDatas = AppInfo.getInstance().getBytes();
        fileSize = AppInfo.getInstance().getBytes().length;//6080
        iapCodePage = AppInfo.getInstance().getIAPCodePage();
        page = (fileSize % pageSize) == 0 ? (fileSize / pageSize) : (fileSize / pageSize + 1);//95
    }

    //TODO #IAP_Erase IAP_PAGE_ERASE 烧录数据
    private void sendserialPortProcess7() {
        count = 7;
        if (burnindex < page) {
            burndata();
        }
    }

    private void burndata() {
        int begin = burnindex * pageSize;
        int end = (burnindex + 1) * pageSize;
        byte[] item = ArrayUtils.subarray(fileDatas, begin, end);

        byte[] programS = McuIntUtil.toBytes(begin + iapCodePage);
        byte[] programE = null;
        if (burnindex == (page - 1)) {
            programE = McuIntUtil.toBytes(fileSize + iapCodePage - 1);
        } else {
            programE = McuIntUtil.toBytes(end + iapCodePage - 1);
        }
        byte[] content = ArrayUtils.addAll(ArrayUtils.addAll(programS, programE), item);
        if (ByteUtil.toUnsignedInt(content[4]) == 0X40) {
            content[4] = ByteUtil.toByte(0X3F);
        }
        byte[] datas = VerifyUtil.getBytes(0X55, 0X01, 0X01, ByteUtil.toUnsignedInts(content));

        if (page - burnindex == 1) {
            sendMessage(datas, new SerialPortService.SerualPrortMechanicalCllBack() {
                @Override
                public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                    if (serialPortData.getCmd() == 0X4F && serialPortData.getType() == 0X00) {
                        LogUtils.i("-------burndata-last-----烧录数据7----");
                        sendserialPortProcess8();
                    }
                }
            }, 1);
        } else {
            sendMessage(datas, new SerialPortService.SerualPrortMechanicalCllBack() {
                @Override
                public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                    if (serialPortData.getCmd() == 0X4F && serialPortData.getType() == 0X00) {
                        LogUtils.e("-------burndata------烧录数据7----");
                        burnindex++;
                        sendserialPortProcess7();
                    }
                }
            }, 1);
        }
    }


    //TODO #-_IAP_CRC  crc 校验结果"
    private void sendserialPortProcess8() {
        LogUtils.e("-------------sendserialPortProcess8------");
        count = 8;
        byte[] begin = McuIntUtil.toBytes(AppInfo.getInstance().getIAPCodePage());
        byte[] end = McuIntUtil.toBytes(AppInfo.getInstance().getBytes().length);
        byte[] data = ArrayUtils.addAll(begin, end);
        byte[] datas = VerifyUtil.getBytes(0X55, 0X02, 0X00, ByteUtil.toUnsignedInts(data));
        sendMessage(datas, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                byte[] bytes = serialPortData.getBytes();
                int b1 = ByteUtil.toUnsignedInt(bytes[bytes.length - 4]);
                int b2 = ByteUtil.toUnsignedInt(bytes[bytes.length - 3]);
                LogUtils.e("CRC：" + ByteUtil.to16Hex(b1) + "  " + ByteUtil.to16Hex(b2));
                int[] crc = VerifyUtil.getCRCValue(AppInfo.getInstance().getBytes(), 0, AppInfo.getInstance().getBytes().length);
                LogUtils.e("******发送成功sendserialPortProcess8" +
                        " -->  CRC校验(BIN):" + ByteUtil.to16Hex(crc[0]) + " " + ByteUtil.to16Hex(crc[1]) + "\r\n");
                sendserialPortProcess9();
            }
        }, 1);
    }


    //TODO #IAP_Reset  uMode 重启"
    private void sendserialPortProcess9() {
        count = 9;
        byte[] datas = VerifyUtil.getBytes(0X55, 0X04, 0X00);
        sendMessage(datas, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                LogUtils.e("******发送成功sendserialPortProcess9--");
                sendserialPortProcess11();
            }
        }, 1);
        int b = VerifyUtil.CRC_XModem(AppInfo.getInstance().getBytes(), 0, AppInfo.getInstance().getBytes().length);
        LogUtils.e("******--" + b);
    }

    //TODO AP_Connect
    private void sendserialPortProcess11() {//[55, 07, 06, 00, 9E, 9F, B7]
        count = 11;
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X06, 0X00);
        sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                if (serialPortData.getCmd() == 0X4F && serialPortData.getType() == 0X00) {
                    LogUtils.e("-------------sendserialPortProcess11----");
                    sendserialPortProcess22();
                }
            }
        }, 1);
    }

    //TODO _IAP_Info
    private void sendserialPortProcess22() {//[55, 07, 03, 00, A1, D3, 9B]
        count = 22;
        SerialPortService.getInstance().serialPortDataList.clear();
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X03, 0X00);
        sendMessage2(bytes, new SerialPortService.SerualPrortMechanicalCllBack2() {
            @Override
            public void callbytes(byte[] buffer, List<SerialPortData> serialPortData) {
                byte[] temp = serialPortData.get(1).getBytes();
                int size = McuIntUtil.toInt(temp[5], temp[6], temp[7], temp[8]);
                AppInfo.getInstance().setIAPCodePage(size);//IAP占用空间大小
                byte[] models = new byte[]{temp[12], temp[11], temp[10], temp[9]};
                String chipVersion = ByteStringUtil.byteArrayToHexStr(models);//芯片型号
                AppInfo.getInstance().setChipVersion(chipVersion); //芯片型号
                LogUtils.e("-------------sendserialPortProcess22----");
                sendserialPortProcess33();
            }
        }, 2);
    }

    //TODO #_IAP_Flash   CMD_READ  获取版本信息
    private void sendserialPortProcess33() {//[55, 0F, 01, 02, 99, 00, 1C, 00, 00, 17, 1C, 00, 00, 82, 2E]
        count = 33;
        int iapCodePage = AppInfo.getInstance().getIAPCodePage();
        byte[] bytes = null;
        //5K
        if (iapCodePage == 5120) {
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X1C, 0X00, 0X00, 0X17, 0X1C, 0X00, 0X00);
        } else if (iapCodePage == 4096) {
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X18, 0X00, 0X00, 0X17, 0X18, 0X00, 0X00);
        }
        sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
            @Override
            public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                StringBuffer sb = new StringBuffer();
                for (int i = 17; i <= 21; i++) {
                    sb.append(ASCIIUtil.toChar(ByteUtil.toUnsignedInt(serialPortData.getBytes()[i])));
                }
                AppInfo.getInstance().setVersion(sb.toString());
                LogUtils.e("-------------sendserialPortProcess33----");
                //更新成功，重置缓存
                if (updateCallback != null) {
                    updateCallback.successful();
                    resetStatus();
                }
            }
        }, 1);
    }

    private void resetStatus() {
        burnindex = 0;
        count = 0;
        SerialPortService.getInstance().serialPortDataList.clear();
        SerialPortService.getInstance().clear();
    }

    private void sendMessage(byte[] bytes, SerialPortService.SerualPrortMechanicalCllBack mechanicalCllBack, int type) {
        try {
            SerialPortService.getInstance().sendMessage(bytes, mechanicalCllBack, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage2(byte[] bytes, SerialPortService.SerualPrortMechanicalCllBack2 cllBack2, int type) {
        try {
            SerialPortService.getInstance().sendMessage2(bytes, cllBack2, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
