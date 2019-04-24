package com.mcuupdate.arr;

import android.app.Application;

import android.content.Context;
import android.os.Build;

import com.mcuupdate.model.AppInfo;
import com.mcuupdate.model.MenuData;
import com.mcuupdate.model.Platform;
import com.mcuupdate.model.SerialPortData;
import com.mcuupdate.utils.ASCIIUtil;
import com.mcuupdate.utils.ByteStringUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.LogUtils;
import com.mcuupdate.utils.McuIntUtil;
import com.mcuupdate.utils.VerifyUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * 更新工具类， 提供版本号，对比等等一系列信息
 */
public class UpdateUtils {

    private static UpdateUtils sendCommandUtils;

    private Context mContext;
    public UpdatePortProcess updateHolder;


    //设置更新成功进行回调
    public void setUpdateCallback(UpdateProxy.UpdateCallback updateCallback) {
        updateHolder = new UpdatePortProcess(updateCallback);
    }

    public Context getmContext() {
        return mContext;
    }

    private UpdateUtils() {
    }

    public static UpdateUtils getInstance() {
        // 对象实例化时与否判断（不使用同步代码块，sPayUtils不等于null时，直接返回对象，提高运行效率）
        if (sendCommandUtils == null) {
            //同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
            synchronized (UpdateUtils.class) {
                //未初始化，则初始instance变量
                if (sendCommandUtils == null) {
                    sendCommandUtils = new UpdateUtils();
                }
            }
        }
        return sendCommandUtils;
    }


    /**
     * 必须在全局Application先调用，获取context上下文，
     */
    public void init(Application app) {
        mContext = app;
        //打开串口
        if (Build.BRAND.equals("alps")) {
            AppInfo.getInstance().setPlatform(Platform.MTK_6373);
        } else if (Build.BRAND.equals("qcom")) {
            AppInfo.getInstance().setPlatform(Platform.QCOME_SC20J);
        }
        //关闭马上打开串口
    }


    /**
     * 获取需要更新的烧录档的版本号和芯片型号
     *
     * @return [0]烧录档的版本号  [1]芯片型号    TODO  如果烧录档文件大则需要处理一下
     */
    public String[] getNewBurnFileVersion() {
        String burnVsersion = "", chipVersion = "";
        byte[] buffer = AppInfo.getInstance().getBytes();
        if (buffer != null) {
            byte[] version = new byte[]{buffer[2060], buffer[2061], buffer[2062], buffer[2063], buffer[2064]};
            int[] ints = ByteUtil.toUnsignedInts(version);
            burnVsersion = "" + (char) ints[0] + (char) ints[1] + (char) ints[2] + (char) ints[3] + (char) ints[4];//版本
            byte[] chipVersionArr = new byte[]{buffer[2072], buffer[2073], buffer[2074], buffer[2075]};//芯片型号
            chipVersion = ByteStringUtil.byteArrayToHexStr(chipVersionArr);
        }
        return new String[]{burnVsersion, chipVersion};
    }


    /**
     * 获取旧的烧录档版本号
     * [55, 07, 03, 00, A1, D3, 9B]
     *
     * @throws IOException
     */
    public void getOldBurnFileModel(OldBurnFileDataCallback oldBurnFileDataCallback) throws IOException {
        this.oldBurnFileDataCallback = oldBurnFileDataCallback;
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X03, 0X00);
        SerialPortService.getInstance().sendMessage2(bytes, new SerialPortService.SerualPrortMechanicalCllBack2() {
            @Override
            public void callbytes(byte[] buffer, List<SerialPortData> serialPortData) {
                try {
                    byte[] temp = serialPortData.get(1).getBytes();
                    int size = McuIntUtil.toInt(temp[5], temp[6], temp[7], temp[8]);//反转  IAP占用空间
                    AppInfo.getInstance().setIAPCodePage(size); //IAP占用空间大小
//                byte[] models = new byte[]{temp[12], temp[11], temp[10], temp[9]};
                    byte[] models = new byte[]{temp[9], temp[10], temp[11], temp[12]};
                    String chipVersion = ByteStringUtil.byteArrayToHexStr(models);
                    AppInfo.getInstance().setChipVersion(chipVersion); //芯片型号
                    getOldBurnFileVersion();
                } catch (Exception e) {
                }
            }
        }, 2);
    }

    /**
     * 获取旧的烧录版本号
     *
     * @throws IOException
     */
    private void getOldBurnFileVersion() throws Exception {//[55, 0F, 01, 02, 99, 00, 1C, 00, 00, 17, 1C, 00, 00, 82, 2E]
        int iapCodePage = AppInfo.getInstance().getIAPCodePage();
        if (iapCodePage == 0) {
            throw new Exception("请获旧的取芯片型号");
        }

        byte[] bytes = null;
        if (iapCodePage == 5120) { //1400进制， 转10进制是 51200 //5K
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X1C, 0X00, 0X00, 0X17, 0X1C, 0X00, 0X00);
        } else if (iapCodePage == 4096) {//00 14 00 00 反转过来 00001400  十六进制，就是4090 这个 IAP占用空间大小 需要反转
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X18, 0X00, 0X00, 0X17, 0X18, 0X00, 0X00);
        }
        if (iapCodePage == 5120 || iapCodePage == 4096) {
            SerialPortService.getInstance().sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
                @Override
                public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 17; i <= 21; i++) {
                        sb.append(ASCIIUtil.toChar(ByteUtil.toUnsignedInt(serialPortData.getBytes()[i])));
                    }
                    AppInfo.getInstance().setVersion(sb.toString());
//                String log = "获取版本号 VERSION = " + sb.toString() + "\r\n";
//                LogUtils.e("******" + log);
                    oldBurnFileDataCallback.onSuccessful();//获取旧的芯片型号和版本成功
                }
            }, 1);
        }
    }

    private OldBurnFileDataCallback oldBurnFileDataCallback;

    //获取Old版本信息
    public interface OldBurnFileDataCallback {
        void onSuccessful();
    }


    public interface RestartMcuCall {
        void onSuccessful();
    }

    //重启MCU
    public void restartMcu(final RestartMcuCall restartMcuCall) {

        List<MenuData> menuDatas = new ArrayList<>();

//        String model = Build.BRAND.toUpperCase() + " " + Build.MODEL.toUpperCase();
//        menuDatas.add(new MenuData("厂商型号", model, model));

        //判断不同的厂商，节点不一样而已
        if (Build.BRAND.equals("alps")) {
            menuDatas.add(new MenuData("方案PLATFORM", Platform.MTK_6373.getName(), Platform.MTK_6373));
        } else if (Build.BRAND.equals("qcom")) {
            menuDatas.add(new MenuData("方案PLATFORM", Platform.QCOME_SC20J.getName(), Platform.QCOME_SC20J));
        }
        if (!menuDatas.isEmpty()) {
            final MenuData menuData = menuDatas.get(0);
            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //重启MCU代码
                        try {
                            Platform platform = (Platform) menuData.getValue();
                            IOService.writeValue(platform.getIO1(), true);
                            Thread.sleep(100);
                            IOService.writeValue(platform.getIO1(), false);
                        } catch (Exception e) {
                        } finally {
                            restartMcuCall.onSuccessful();
                        }
                    }
                }).start();
            } catch (Exception ex) {
                LogUtils.i("重启MCU失败。。。");
            }
        }
    }

    public void startupdate() {
        //开始升级
        if (updateHolder != null) {
            updateHolder.startExecuteUpdateCommand();
        }
    }
}
