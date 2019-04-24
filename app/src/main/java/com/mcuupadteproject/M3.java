package com.mcuupadteproject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.mcuupdate.arr.SerialPortService;
import com.mcuupdate.arr.UpdateProxy;
import com.mcuupdate.arr.UpdateUtils;
import com.mcuupdate.model.AppInfo;
import com.mcuupdate.utils.ByteStringUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.LayerUtil;
import com.mcuupdate.utils.LogUtils;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class M3 extends Activity {

    UpdateProxy updateProxy;
    TextView text;
    byte[] buffer = null;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.text);
        //
        updateProxy = new UpdateProxy(new UpdateProxy.UseUpdateProxyCall() {


            @Override
            public void successful(String status_text, String mcucode) {
                LayerUtil.showToast(M3.this, status_text);
            }

            @Override
            public void failure(String status_text) {
                LayerUtil.showToast(M3.this, status_text);
            }
        }, this);

        //这里应该是从服务端获取的Fiel
        try {

            InputStream in = UpdateUtils.getInstance().getmContext().getResources()
                    .getAssets().open("jigui/RackRoom_v1035_0x50D3");
//            InputStream in = UpdateUtils.getInstance().getmContext().getResources()
//                    .getAssets().open("RackRoom_v1225_0x272F_AP");
//
//                    InputStream in = UpdateUtils.getInstance().getmContext().getResources()
//                            .getAssets().open("RackRoom_v1035_0x50D3");
            buffer = new byte[in.available()];
            in.read(buffer);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        findViewById(R.id.btm11).setOnClickListener(v -> {
            try {
                OutputStream outputStream = SerialPortService.getInstance().getOutputStream();
                if (outputStream == null) {
                    SerialPortService.getInstance().openSerialPort();
                    new Handler().postDelayed(() -> {
                        try {
                            updateProxy.setUpadteBuffer(buffer);
                            updateProxy.checkUpdate();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, 1000);
                } else {
                    updateProxy.setUpadteBuffer(buffer);
                    updateProxy.checkUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.btm10).setOnClickListener(v -> {
            AppInfo appInfo = new AppInfo();
            appInfo.setIAPCodePage(AppInfo.getInstance().getIAPCodePage());
            appInfo.setVersion(AppInfo.getInstance().getOldVersion());
            appInfo.setVersion(AppInfo.getInstance().getNewVersion());
            String log = ToStringBuilder.reflectionToString(appInfo, ToStringStyle.MULTI_LINE_STYLE) + "\r\n=== 升级完成 ==";
            LogUtils.e("***************" + log);
            text.setText("");
            text.setText(log);
        });
    }
}
