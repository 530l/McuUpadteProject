package com.mcuupdate.arr;


import android.serialport.SerialPort;

import com.mcuupdate.model.AppInfo;
import com.mcuupdate.model.SerialPortData;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.LogUtils;

import org.apache.commons.lang3.ArrayUtils;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * 打开串口，读写串口， 回调数据，等等
 */

public class SerialPortService {


    private static SerialPortService serialPortService;
    private SerialPort serialPort = null;


    private OutputStream outputStream;
    private InputStream inputStream;
    private int uartBaudrate = 115200;        //波特率
    private ReadThread readThread;
    private int length = 0;
    private List<Byte> bytes = new ArrayList<>();
    public List<SerialPortData> serialPortDataList = new ArrayList<>();
    private int type;

    public static SerialPortService getInstance() {
        // 对象实例化时与否判断（不使用同步代码块，sPayUtils不等于null时，直接返回对象，提高运行效率）
        if (serialPortService == null) {
            //同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
            synchronized (UpdatePortProcess.class) {
                //未初始化，则初始instance变量
                if (serialPortService == null) {
                    serialPortService = new SerialPortService();
                }
            }
        }
        return serialPortService;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void openSerialPort() throws IOException {
        if (serialPort != null) {
            return;
        }
        AppInfo appInfo = AppInfo.getInstance();
        if (appInfo.getPlatform() == null) {
            return;
        }
        serialPort = new SerialPort(new File(appInfo.getPlatform().getSerialPort1()), uartBaudrate, 0);
        outputStream = serialPort.getOutputStream();
        inputStream = serialPort.getInputStream();
        readThread = new ReadThread();
        readThread.start();
    }

    public void closeSerialPort() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            inputStream = null;
        }

        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outputStream = null;
        }

        try {
            if (serialPort != null) {
                serialPort.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serialPort = null;
        }

        if (readThread != null) {
            readThread.interrupt();
            serialPortDataList.clear();
            bytes.clear();
            length = 0;
        }
    }


    //读串口线程
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!interrupted()) {
                try {
                    byte[] buffer = new byte[1024];
                    int size = inputStream.read(buffer);//串口被一个IO操作，如果有多个IO，要被等待
//                    LogUtils.tt("--------------------------" + size);
                    String to16Hexs = ByteUtil.to16Hexs(buffer);
                    LogUtils.i(to16Hexs);
                    for (int i = 0; i < size; i++) {
                        int value = ByteUtil.toUnsignedInt(buffer[i]);
                        if (value == 0X55 && length == 0) {//头码
                            bytes.add(buffer[i]);
                            continue;
                        }
                        //长度
                        if (length == 0 && bytes.size() > 0) {
                            length = ByteUtil.toUnsignedInt(buffer[i]);//长度
                            bytes.add(buffer[i]);
                            continue;
                        }
                        //遍历剩下的字节码
                        if (length > 0) {
                            bytes.add(buffer[i]);
                        }

                        //长度==完整的bite
                        //1, 代表客户端发一条指令，串口完整的发送一段数据过来
                        //2，代表客户端发送一条指令，串口会分2段数据发送过来
                        //ps:  1
                        // 55 13 4F 00 49 00 00 66 10 00 14 00 04 20 EC FF FF E6 74
                        //ps:  2
                        // 55 13 4F 00 49 00 00 66 10 00 14 00 04 20 EC FF FF E6 74
                        // 55 0F 4F 00 4D 00 10 00 00 53 22 05 00 8A 1F 55 0F 4F 00 4D 00 14 00 00 31 22 05 00 F6 6A
                        if (length > 0 && length == bytes.size()) {
                            Byte[] result = bytes.toArray(new Byte[length]);
                            SerialPortData serialPortData = new SerialPortData(ArrayUtils.toPrimitive(result));
                            if (type == 2) {
                                serialPortDataList.add(serialPortData);
                                if (serialPortDataList.size() == 2) {
                                    if (serualPrortMechanicalCllBack2 != null) {
                                        serualPrortMechanicalCllBack2.callbytes(buffer, serialPortDataList);
                                        LogUtils.ii(ByteUtil.to16Hexs(buffer));
                                    }
                                }
                            } else if (type == 1) {
                                if (mechanicalCllBack != null) {
                                    mechanicalCllBack.callbytes(buffer, serialPortData);

                                }
                            }
                            clear();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void clear() {
        bytes.clear();
        length = 0;
    }


    //发送串口数据
    public void sendMessage(byte[] bytes, SerualPrortMechanicalCllBack mechanicalCllBack, int type) throws IOException {
        if (outputStream != null) {
            setMechanicalCllBack(mechanicalCllBack);
            this.type = type;
            String to16Hexs = ByteUtil.to16Hexs(bytes);
            LogUtils.iparame(to16Hexs);
            writeOrder(bytes);
        }
    }


    public void sendMessage2(byte[] bytes, SerualPrortMechanicalCllBack2 cllBack2, int type) throws IOException {
        if (outputStream != null) {
            setCllBack2(cllBack2);
            this.type = type;
            String to16Hexs = ByteUtil.to16Hexs(bytes);
            serialPortDataList.clear();
            LogUtils.ii(to16Hexs);
            writeOrder(bytes);
        }
    }

    private void writeOrder(byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //串口1个指令分1段数据返回

    SerualPrortMechanicalCllBack mechanicalCllBack;

    public void setMechanicalCllBack(SerualPrortMechanicalCllBack mechanicalCllBack) {
        this.mechanicalCllBack = mechanicalCllBack;
    }

    public interface SerualPrortMechanicalCllBack {
        void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException;

    }

    //串口1个指令分2段数据返回

    SerualPrortMechanicalCllBack2 serualPrortMechanicalCllBack2;

    public void setCllBack2(SerualPrortMechanicalCllBack2 serualPrortMechanicalCllBack2) {
        this.serualPrortMechanicalCllBack2 = serualPrortMechanicalCllBack2;
    }

    public interface SerualPrortMechanicalCllBack2 {

        void callbytes(byte[] buffer, List<SerialPortData> serialPortData);
    }

}
