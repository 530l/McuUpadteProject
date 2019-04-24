package com.mcuupadteproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


import com.mcuupdate.arr.UpdateProxy;
import com.mcuupdate.arr.UpdateUtils;
import com.mcuupdate.model.AppInfo;
import com.mcuupdate.utils.LayerUtil;
import com.mcuupdate.utils.LogUtils;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.io.InputStream;


public class MainActivity2 extends AppCompatActivity {


    UpdateProxy updateProxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initListener();
        updateProxy = new UpdateProxy(new UpdateProxy.UseUpdateProxyCall() {

            @Override
            public void successful(String status_text, String mcucode) {
                LayerUtil.showToast(MainActivity2.this, status_text);
            }

            @Override
            public void failure(String status_text) {
                LayerUtil.showToast(MainActivity2.this, status_text);
            }
        }, this);


        new Thread(new Runnable() {
            @Override
            public void run() {
                //这里应该是从服务端获取的Fiel
                try {

//                    InputStream in = UpdateUtils.getInstance().getmContext().getResources()
//                            .getAssets().open("RackRoom_v1226_0x54D5_AP");

                    InputStream in = UpdateUtils.getInstance().getmContext().getResources()
                            .getAssets().open("jigui/RackRoom_v1225_0x272F_AP");

//                    InputStream in = UpdateUtils.getInstance().getmContext().getResources()
//                            .getAssets().open("RackRoom_v1035_0x50D3");
                    byte[] buffer = new byte[in.available()];
                    in.read(buffer);
                    AppInfo.getInstance().setBytes(buffer);
                    in.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

//    public static void writeBytesToFile(InputStream is, File file) throws IOException {
//        FileOutputStream fos = null;
//        try {
//            byte[] data = new byte[1028];
//            int nbread = 0;
//            fos = new FileOutputStream(file);
//            while ((nbread = is.read(data)) > -1) {
//                fos.write(data, 0, nbread);
//            }
//        } finally {
//            if (fos != null) {
//                fos.close();
//            }
//        }
//    }

    void initListener() {
        findViewById(R.id.btm10).setOnClickListener(v -> {
            AppInfo appInfo = new AppInfo();
            appInfo.setIAPCodePage(AppInfo.getInstance().getIAPCodePage());
            appInfo.setVersion(AppInfo.getInstance().getOldVersion());
            appInfo.setVersion(AppInfo.getInstance().getNewVersion());
            String log = ToStringBuilder.reflectionToString(appInfo, ToStringStyle.MULTI_LINE_STYLE) + "\r\n=== 升级完成 ==";
            LogUtils.e("***************" + log);
        });

        findViewById(R.id.btm11).setOnClickListener(v -> {
            try {
//                String path = "file:///android_asset/" + "RackRoom_v1226";
//                String path = "file:///android_asset/" + "RackRoom_v1326";
//                String path = "file:///android_asset/RackRoom_v1326";
//                String path = "file:///android_asset/" + "RackRoom_v1426_52253";
//                File file = new File(path);
                updateProxy.checkUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
