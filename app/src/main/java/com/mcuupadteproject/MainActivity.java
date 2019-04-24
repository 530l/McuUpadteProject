package com.mcuupadteproject;

import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


import com.mcuupdate.arr.IOService;
import com.mcuupdate.arr.SerialPortService;
import com.mcuupdate.arr.UpdatePortProcess;
import com.mcuupdate.arr.UpdateProxy;
import com.mcuupdate.model.AppInfo;
import com.mcuupdate.model.MenuData;
import com.mcuupdate.model.Platform;
import com.mcuupdate.utils.ASCIIUtil;
import com.mcuupdate.utils.ByteStringUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.LayerUtil;
import com.mcuupdate.utils.LogUtils;
import com.mcuupdate.utils.McuIntUtil;
import com.mcuupdate.utils.VerifyUtil;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    UpdatePortProcess updateCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //打开串口
        EventBus.getDefault().register(this);
        if (Build.BRAND.equals("alps")) {
            AppInfo.getInstance().setPlatform(Platform.MTK_6373);
        } else if (Build.BRAND.equals("qcom")) {
            AppInfo.getInstance().setPlatform(Platform.QCOME_SC20J);
        }
        try {
            updateCommand = new UpdatePortProcess(new UpdateProxy.UpdateCallback() {
                @Override
                public void successful() {

                }
            });
            //RackRoom_test oldVersion=V1026
            InputStream in = getResources().getAssets().open("jigui/RackRoom_v1226");
            //RackRoom_test_1027  ---> oldVersion=V1126
//            InputStream in = getResources().getAssets().open("RackRoom_test_1027");
            int lenght = in.available();
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            AppInfo.getInstance().setBytes(buffer);
            in.close();
            SerialPortService.getInstance().openSerialPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        get();
        initListener();
    }

    private void get() {
        InputStream in = null;
        try {
            in = getResources().getAssets().open("jigui/RackRoom_v1226");
            int lenght = in.available();
            byte[] buffer = new byte[lenght];
            in.read(buffer);
            in.close();
            byte[] version = new byte[]{buffer[2060], buffer[2061], buffer[2062], buffer[2063], buffer[2064]};
            int[] ints = ByteUtil.toUnsignedInts(version);
            String v = "" + (char) ints[0] + (char) ints[1] + (char) ints[2] + (char) ints[3] + (char) ints[4];//版本
            //
            byte[] xin = new byte[]{buffer[2072], buffer[2073], buffer[2074], buffer[2075]};
            String s = ByteStringUtil.byteArrayToHexStr(xin);//芯片型号
            System.out.print(s);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void sendserialPortProcess2() throws IOException {//[55, 07, 03, 00, A1, D3, 9B]
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X03, 0X00);
        SerialPortService.getInstance().sendMessage2(bytes, (buffer, serialPortData) -> {
            try {
                byte[] temp = serialPortData.get(1).getBytes();
                int size = McuIntUtil.toInt(temp[5], temp[6], temp[7], temp[8]);//反转  IAP占用空间
                AppInfo.getInstance().setIAPCodePage(size); //IAP占用空间大小
                byte[] models = new byte[]{temp[12], temp[11], temp[10], temp[9]};
                String chipVersion = ByteStringUtil.byteArrayToHexStr(models);//芯片型号
                AppInfo.getInstance().setChipVersion(chipVersion); //芯片型号
                /**
                 * *****IAP占用空间大小 IAP_CODE_PAGE= 0x1400 5120
                 *     芯片型号--》00052231
                 */
                String log = "IAP占用空间大小 IAP_CODE_PAGE= 0x" + Integer.toHexString(size) + " "
                        + AppInfo.getInstance().getIAPCodePage() + "\r\n" + "芯片型号--》" + chipVersion;
                LogUtils.i("******" + log);
            } catch (Exception e) {
            }

        }, 2);
    }

    //TODO #_IAP_Flash   CMD_READ  获取版本信息
    public void sendserialPortProcess3() throws IOException {//[55, 0F, 01, 02, 99, 00, 1C, 00, 00, 17, 1C, 00, 00, 82, 2E]
        int iapCodePage = AppInfo.getInstance().getIAPCodePage();
        byte[] bytes = null;
        //5K

        if (iapCodePage == 5120) {  //1400进制， 转10进制是 51200
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X1C, 0X00, 0X00, 0X17, 0X1C, 0X00, 0X00);
        } else if (iapCodePage == 4096) {//00 14 00 00 反转过来 00001400  十六进制，就是4090 这个 IAP占用空间大小 需要反转
            bytes = VerifyUtil.getBytes(0X55, 0X01, 0X02, 0X00, 0X18, 0X00, 0X00, 0X17, 0X18, 0X00, 0X00);
        }
        if (iapCodePage == 5120 || iapCodePage == 4096) {
            SerialPortService.getInstance().sendMessage(bytes, (buffer, serialPortData) -> {
                StringBuffer sb = new StringBuffer();
                for (int i = 17; i <= 21; i++) {
                    sb.append(ASCIIUtil.toChar(ByteUtil.toUnsignedInt(serialPortData.getBytes()[i])));
                }
                AppInfo.getInstance().setVersion(sb.toString());
                String log = "获取版本号 VERSION = " + sb.toString() + "\r\n";
                LogUtils.e("******" + log);
            }, 1);
        }
    }

    void initListener() {

//        findViewById(R.id.btm1).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess1();
//        });
        findViewById(R.id.btm2).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess2();
            try {
                sendserialPortProcess2();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        findViewById(R.id.btm3).setOnClickListener(v -> {
            try {
                sendserialPortProcess3();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
//        findViewById(R.id.btm4).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess4();
//        });
//        findViewById(R.id.btm5).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess5();
//        });
//        findViewById(R.id.btm6).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess6();
//        });
//        findViewById(R.id.btm7).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess7();
//        });
//        findViewById(R.id.btm8).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess8();
//        });
//        findViewById(R.id.btm9).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess9();
//        });
//
        findViewById(R.id.btm10).setOnClickListener(v -> {
            AppInfo appInfo = new AppInfo();
            appInfo.setIAPCodePage(AppInfo.getInstance().getIAPCodePage());
            appInfo.setVersion(AppInfo.getInstance().getOldVersion());
            appInfo.setVersion(AppInfo.getInstance().getNewVersion());
            String log = ToStringBuilder.reflectionToString(appInfo, ToStringStyle.MULTI_LINE_STYLE) + "\r\n=== 升级完成 ==";
            LogUtils.e("***************" + log);
        });
        findViewById(R.id.btm11).setOnClickListener(v -> {
//            dialog.setLoadingBuilder(Z_TYPE.CIRCLE_CLOCK)
//                    .setLoadingColor(Color.BLACK)
//                    .setHintText("升级中...")
//                    .show();
            updateCommand.startExecuteUpdateCommand();
        });

        findViewById(R.id.count).setOnClickListener(v -> {
//            LogUtils.i("*****************count==" + UpdatePortProcess.count);
        });

        findViewById(R.id.btn_reboot).setOnClickListener(v -> {
            reboot();
        });

        findViewById(R.id.version_btm).setOnClickListener(v -> {
//            UpdatePortProcess.getInstance().sendserialPortProcess9();
        });
    }


    //重启MCU
    private void reboot() {
        List<MenuData> menuDatas = new ArrayList<>();

        String model = Build.BRAND.toUpperCase() + " " + Build.MODEL.toUpperCase();
        menuDatas.add(new MenuData("厂商型号", model, model));

        if (Build.BRAND.equals("alps")) {
            menuDatas.add(new MenuData("方案PLATFORM", Platform.MTK_6373.getName(), Platform.MTK_6373));
        } else if (Build.BRAND.equals("qcom")) {
            menuDatas.add(new MenuData("方案PLATFORM", Platform.QCOME_SC20J.getName(), Platform.QCOME_SC20J));
        } else {
            menuDatas.add(new MenuData("方案PLATFORM", "请选择", null));
        }

        menuDatas.add(new MenuData("固件BIN", "RackRoom_v1126", "RackRoom_v1126"));

        final MenuData menuData = menuDatas.get(1);
        if (menuData.getValue() == null) {
            LayerUtil.showToast(this, "请选择方案PLATFORM");
            return;
        }

        if (menuDatas.get(2).getValue() == null) {
            LayerUtil.showToast(this, "固件BIN");
            return;
        }
        final ProgressDialog progressDialog = LayerUtil.showLoadingSpinner(this, "重启MCU..");
        progressDialog.show();
        Thread thread = new Thread(() -> {
            try {
                //重启MCU代码
                Platform platform = (Platform) menuData.getValue();
                IOService.writeValue(platform.getIO1(), true);
                Thread.sleep(100);
                IOService.writeValue(platform.getIO1(), false);
            } catch (Exception ex) {
                LayerUtil.showToast(MainActivity.this, ex.toString());
            } finally {
                progressDialog.dismiss();
            }
        });
        thread.start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void messageEventBus(SerialPortLogEvent event) {
        if (event.getType() == 1) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
