package com.example.cabinetlib.utils;


import android.serialport.SerialPort;

import com.example.cabinetlib.ChargingBoxProxy;
import com.example.cabinetlib.bean.CMDInfo_Cb;
import com.example.cabinetlib.comm.Constants_Cb;
import com.example.cabinetlib.bean.AppInfo_Cb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Created by HeCh.
 * @time 2018/8/14 0014 11:21
 * Description:
 */

public class SerialPortUtil_Cb {


    private static SerialPortUtil_Cb sSerialPortUtil;
    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private boolean serialPortStatus = false; //是否打开串口标志
    private boolean threadStatus; //线程状态，为了安全终止线程
    private int count = 0;
    private byte[] trulyData;
    private int trueSize;
    private int totalSize = 0;
    private int index = 0;
    private long lastReadTime = 0;
    private ChargingBoxProxy chargingBoxProxy;

    public void setChargingBoxProxy(ChargingBoxProxy chargingBoxProxy) {
        this.chargingBoxProxy = chargingBoxProxy;
    }

    public OutputStream getmOutputStream() {
        return mOutputStream;
    }

    public static SerialPortUtil_Cb getInstance() {
        synchronized (SerialPortUtil_Cb.class) {
            //未初始化，则初始instance变量
            if (sSerialPortUtil == null) {
                sSerialPortUtil = new SerialPortUtil_Cb();
            }
        }
        return sSerialPortUtil;
    }


    /**
     * 打开串口
     */
    public void openSerialPort() {
        AppInfo_Cb appInfo = AppInfo_Cb.getInstance();
        if (appInfo.platform == null) {
            return;
        }
        try {
            if (mSerialPort == null) {
                mSerialPort = new SerialPort(new File(appInfo.platform.getSerialPort1()),
                        Constants_Cb.uartBaudrate, 0);
                mOutputStream = mSerialPort.getOutputStream();
                mInputStream = mSerialPort.getInputStream();
                mReadThread = new ReadThread();
                mReadThread.setName(Constants_Cb.uartPort);
                mReadThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送带数据的命令
     *
     * @param order
     * @param data
     */
    public void sendOrderWithData(int order, List<Byte> data, DealCallback dealCallback) {
        this.dealCallback = dealCallback;
        byte beginCode = (byte) 0xA8;
        int length = data.size() + 5;
        byte commandCode = (byte) order;
        byte address = (byte) 0x10;
        byte[] tempOrder = new byte[length - 1];
        tempOrder[0] = beginCode;
        if (length >= 256) {
            tempOrder[1] = (byte) (length / 256);
            tempOrder[2] = (byte) (length - 256);
        } else {
            tempOrder[1] = 0;
            tempOrder[2] = (byte) length;
        }
        tempOrder[3] = commandCode;
        tempOrder[4] = address;
        for (int i = 0; i < data.size(); i++) {
            tempOrder[i + 4] = data.get(i);
        }
        byte checkCode = generateCheckCode(tempOrder);
        final byte[] orderCode = new byte[length];
        for (int i = 0; i < tempOrder.length; i++) {
            orderCode[i] = tempOrder[i];
        }
        orderCode[length - 1] = checkCode;
        if (mOutputStream != null) {
            String hex = byteArrayToHexString(orderCode);
//            Log.i("send", hex);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    writeOrder(orderCode);
                }
            }).start();
        }
    }


    private synchronized void writeOrder(byte[] orderCode) {
        try {
            Thread.sleep(50);
            mOutputStream.write(orderCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 生成校验码
     * 从头码至校验的前一位数据，全部相加取低位，并取反加+1
     *
     * @param buffer
     * @return
     */
    private static byte generateCheckCode(byte[] buffer) {
        int total = 0;
        for (int i = 0; i < buffer.length; i++) {
            byte b = buffer[i];
            int ii = b & 0xFF;
            total += ii;
        }
        return (byte) (256 - (total % 256));
    }


    //校验
    private static boolean validate(byte[] buffer) {
        if (buffer == null || buffer.length <= 2) {
            String operation = String.format("SerialPortUtil_ERROR", 3000);
            String hint = "";
            if (buffer == null)
                hint = "buffer=NULL";
            else
                hint = byteArrayToHexString(buffer);
            return false;
        }
        int size = (buffer[1] & 0xff) * 256 + (buffer[2] & 0xff);
        int total = 0;
        for (int i = 0; i < size - 1; i++) {
            byte b = buffer[i];
            int ii = b & 0xFF;
            total += ii;
        }

        int validate = (256 - (total % 256)) & 0xFF;
        int validateValue = buffer[size - 1] & 0xFF;

        return validate == validateValue;
    }

    public static String byteArrayToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        StringBuilder hexBuilder = new StringBuilder();
        for (byte aByte : src) {
            int data = aByte & 0xff;
            String hex = Integer.toHexString(data);
            String hexStr = hex.length() == 1 ? "0" + hex : hex;
            hexBuilder.append(hexStr);
            hexBuilder.append(" ");
        }
        return hexBuilder.toString().trim();
    }


    //读串口数据
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!interrupted()) {
                try {
                    int size;
                    byte[] buffer = new byte[1024];
                    if (mInputStream == null) return;
                    size = mInputStream.read(buffer);
                    //当长度超过流长度分批读取时，500ms内未收到剩余内容时则清除该内容
                    if (count == 1) {
                        if (lastReadTime > 0) {
                            long time = System.currentTimeMillis() - lastReadTime;
                            if (time > 500) {
                                count = 0;
                                lastReadTime = 0;
                            }
                        }
                    }
                    if (((buffer[0] & 0xff) == 0xA8 && (buffer[1] & 0xff)
                            * 256 + (buffer[2] & 0xff) > size) || count == 1) {//分流处理
                        readCmd(buffer, size);
                    } else {
                        List<CMDInfo_Cb> infoList = new ArrayList<>();
                        int len = 0;
                        int index = 0;
                        int lastIndex = 1;
                        parsingCmd(buffer, infoList, len, index, lastIndex, size);
                        for (CMDInfo_Cb info : infoList) {
                            readCmd(info.getBytes(), info.getSize());
                            lastReadTime = System.currentTimeMillis();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

    }


    /**
     * 分析指令信息数量
     *
     * @param cmd
     * @param infoList
     * @param len
     * @param index
     * @param lastIndex
     */
    private void parsingCmd(byte[] cmd, List<CMDInfo_Cb> infoList, int len, int index, int lastIndex, int size) {
        while (true) {
            if ((cmd[len] & 0xff) == 0xA8) {
                trueSize = (cmd[1 + len] & 0xff) * 256 + (cmd[2 + len] & 0xff);
                byte[] b = new byte[trueSize];
                for (int i = 0; i < trueSize; i++) {
                    if (size <= i + len)
                        break;
                    b[i] = cmd[i + len];
                    index++;
                }
                CMDInfo_Cb info = new CMDInfo_Cb(b, trueSize);
                if (validate(b))
                    infoList.add(info);
                len += trueSize;
                if (len >= size)
                    break;
            } else {
                if (len + lastIndex >= size)
                    break;
                if ((cmd[len + lastIndex] & 0xff) == 0xA8) {
                    parsingCmd(cmd, infoList, len + lastIndex, 0, lastIndex, size);
                    break;
                }
                lastIndex++;
            }
        }
    }

    /**
     * 解析指令信息
     *
     * @param buffer
     * @param size
     */
    private void readCmd(byte[] buffer, int size) {
        switch (count) {
            case 0:
                if ((buffer[0] & 0xff) == 0xA8) {
                    index = 0;
                    trueSize = (buffer[1] & 0xff) * 256 + (buffer[2] & 0xff);
                    trulyData = new byte[trueSize];
                    totalSize = size;
                    if (trueSize < size) {
                        size = trueSize;
                    }
                    for (int i = 0; i < size; i++) {
                        trulyData[i] = buffer[i];
                        index++;
                    }
                    if (trueSize > size) {
                        count++;
                    } else {
                        count = 0;
                        totalSize = 0;
                        //读完数据处理
                        dealData(trulyData);
                    }
                }
                break;
            case 1:
                for (int i = 0; i < size; i++) {
                    if (index >= trueSize) {
                        break;
                    }
                    trulyData[totalSize + i] = buffer[i];
                    index++;
                }
                totalSize += size;
                if (trueSize <= totalSize) {
                    count = 0;
                    totalSize = 0;
                    //读完数据处理
                    dealData(trulyData);
                }
                index = 0;
                break;
        }
    }

    /**
     * 处理完整的数据
     *
     * @param src
     */
    private void dealData(byte[] src) {
        if (validate(src)) {
            //校验完成开始处理数据
            String hexString = byteArrayToHexString(src);
            String[] dataList = hexString.split(" ");
            int hex = Integer.parseInt(dataList[0], 16);
            if (hex == 0xA8) {
                //TODO 测试用的日志
                int order = Integer.parseInt(dataList[3], 16);
                //LogUtil.e("order = " + order);
                //处理响应信息
                dealCallback(dataList, order, src);
            }
        }
    }

    /**
     * 处理响应信息
     * TODO 收到数据
     *
     * @param dataList
     * @param order
     * @param byteSrc
     */
    private void dealCallback(String[] dataList, int order, byte[] byteSrc) {
        String hexString = byteArrayToHexString(byteSrc);
        handleMes(dataList, order, byteSrc);
    }

    private void handleMes(String[] dataList, int order, byte[] byteSrc) {
        if (dealCallback != null) {
            byte ordertemp = byteSrc[3];
            if (ordercheck(ordertemp)) {
                if (chargingBoxProxy != null) {
                    chargingBoxProxy.cancelUpadteTime();
                }
                return;
            }
            dealCallback.dealCallback(dataList, order, byteSrc);

        }
    }

    private boolean ordercheck(byte b) {
        //22----96------103
        int UnsignedInt1 = ByteUtil_Cb.toUnsignedInt(b);
        int UnsignedInt2 = ByteUtil_Cb.toUnsignedInt((byte) 0x60);
        int UnsignedInt3 = ByteUtil_Cb.toUnsignedInt((byte) 0x67);
        if (UnsignedInt1 < UnsignedInt2 || UnsignedInt1 > UnsignedInt3) {
            //超过范围
            return true;
        }
        return false;
    }

    DealCallback dealCallback = null;

    public interface DealCallback {
        void dealCallback(String[] dataList, int order, byte[] byteSrc);
    }


    /**
     * 数据格式化
     */
    public String serialFormatting(String hex) {
        String hexStr = "";
        if (hex == null || hex.equals(""))
            return hexStr;
        List<String> hexs = Arrays.asList(hex.split(" "));
        hexStr = hexs.get(0) + " " + hexs.get(1) + " " + hexs.get(2) + " " + hexs.get(3) + "\n";
        int index = 0;
        int page = 0;
        int z = 0;
        for (int j = 4; j < hexs.size(); j++) {

            //机芯信息位置
            if (j < (4 + 6) + z)
                hexStr += hexs.get(j) + " ";
            if (j == (4 + 5) + z)
                hexStr += "\n";

            //机芯数据5组
            if (j > 4 + 5 + page + z && j <= 4 + 20 + page + z) {
                hexStr += hexs.get(j) + " ";
                index++;
            }

            //每一小组15个字节，每一小组换行
            if (index == 15) {
                hexStr += "\n";
                index = 0;
                page += 15;
                //机芯数量
                if (page % 75 == 0) {
                    page = 0;
                    z += 81;
                }
            }
        }
        return hexStr;
    }


    /**
     * 关闭串口
     */

    public void closeSerialPort() {
        this.serialPortStatus = false;
        this.threadStatus = true; //线程状态
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mInputStream = null;
        }

        try {
            if (mOutputStream != null) {
                mOutputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mOutputStream = null;
        }

        try {
            if (mSerialPort != null) {
                mSerialPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mSerialPort = null;
        }

        if (mReadThread != null) {
            mReadThread.interrupt();
        }
    }


    /**
     * 发送命令
     *
     * @param order
     */
    public void sendOrder(int order) {
        byte beginCode = (byte) 0xA8;
        byte length = 04;
        byte commandCode = (byte) order;
        byte address = (byte) 0x10;
        byte[] tempOrder = new byte[]{beginCode, length, commandCode, address};
        byte checkCode = generateCheckCode(tempOrder);
        byte[] orderCode = new byte[]{beginCode, length, commandCode, address, checkCode};
        try {
            mOutputStream.write(orderCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}