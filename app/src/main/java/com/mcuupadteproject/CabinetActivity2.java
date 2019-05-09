package com.mcuupadteproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import com.example.cabinetlib.ChargingBoxProxy;
import com.example.cabinetlib.ChargingBoxProxyCall;
import com.example.cabinetlib.utils.LogUtils_Cb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 充电宝升级
 */
public class CabinetActivity2 extends Activity {

    //
    ChargingBoxProxy chargingBoxProxy = null;
    byte[] buffer = null;
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String str = (String) msg.obj;
            Toast.makeText(CabinetActivity2.this, str + "-->步骤" + resutl6, Toast.LENGTH_SHORT).show();
            LogUtils_Cb.i(str + "-----)(");

        }
    };

    private void call(String status) {
        Message message = handler.obtainMessage();
        message.what = 1;
        message.obj = status;
        handler.sendMessage(message);
        isCheckStatus = false;
    }


    void initConfigData() {
        //模拟真实数据
        //这里应该是从服务端获取的Fiel
        try {
            InputStream in = getResources().getAssets().open("PowerBank_V1044_0xC08D");
//            InputStream in = getResources().getAssets().open("PowerBank_1029_0xEB9A_");
            buffer = new byte[in.available()];
            in.read(buffer);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean isCheckStatus = false;
    byte resutl6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cab2);
        //1
        chargingBoxProxy = new ChargingBoxProxy(new ChargingBoxProxyCall() {
            @Override
            public void successful(String successcode) {
                call(successcode);
            }

            @Override
            public void failure(String errcode, byte resutl6) {
                CabinetActivity2.this.resutl6 = resutl6;
                call(errcode);

            }
        });
        //
        initConfigData();

        //
        findViewById(R.id.check_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCheckStatus) {
                    check();
                    isCheckStatus = true;
                }

//                timer_cb.cancel();
            }
        });
    }


    private void check() {
        try {
            OutputStream outputStream = chargingBoxProxy.serialPortUtil_cb.getmOutputStream();
            if (outputStream == null) {
                chargingBoxProxy.serialPortUtil_cb.openSerialPort();
                handler.postDelayed(() -> {
                    try {
                        startCheckUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 1000);
            } else {
                startCheckUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCheckUpdate() {
        //2
        chargingBoxProxy.setUpadteBuffer(buffer);
        //这些参数需要调用者，传入参数
        List<Byte> bytes = new ArrayList<>();
//        bytes.add((byte) 0x03);//孔位
//        bytes.add((byte) 0x00);//SN
//        bytes.add((byte) 0x99);//SN99
//        bytes.add((byte) 0x00);//SN00
//        bytes.add((byte) 0x5A);//SN5A


//        bytes.add((byte) 0x02);//孔位
//        bytes.add((byte) 0x00);//SN
//        bytes.add((byte) 0x98);//SN99
//        bytes.add((byte) 0xDB);//SN00
//        bytes.add((byte) 0xEA);//SN5A


//        bytes.add((byte) 0x03);//孔位
//        bytes.add((byte) 0x00);//SN
//        bytes.add((byte) 0x98);//SN99
//        bytes.add((byte) 0xF9);//SN00
//        bytes.add((byte) 0xEF);//SN5A

//        bytes.add((byte) 0x04);//孔位
//        bytes.add((byte) 0x00);//SN
//        bytes.add((byte) 0x99);//SN99
//        bytes.add((byte) 0x00);//SN00
//        bytes.add((byte) 0x20);//SN5A


        bytes.add((byte) 0x05);//孔位
        bytes.add((byte) 0x00);//SN
        bytes.add((byte) 0x99);//SN99
        bytes.add((byte) 0x00);//SN00
        bytes.add((byte) 0x5A);//SN5A

        //3
        chargingBoxProxy.checkUpdate(bytes);
    }
}
