package com.mcuupdate.arr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

;


import com.mcuupdate.model.AppInfo;
import com.mcuupdate.model.SerialPortData;
import com.mcuupdate.utils.ASCIIUtil;
import com.mcuupdate.utils.ByteUtil;
import com.mcuupdate.utils.LayerUtil;
import com.mcuupdate.utils.LogUtils;
import com.mcuupdate.utils.VerifyUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * 更新功能入口
 */
public class UpdateProxy {

    /**
     * 检查更新
     */
//    private String status_text;
    public ProgressDialog progressDialog;
    private Context mContext;
    private CountDownTimerUtil upadteTimerCount = null;
    private Activity activity;
    private String newVersionservice = "", newModelservice = "";
    private String tempversion = "";

    public UpdateProxy(final UseUpdateProxyCall useUpdateProxyCall, Activity activity) {
        this.useUpdateProxyCall = useUpdateProxyCall;
        this.activity = activity;
        mContext = UpdateUtils.getInstance().getmContext();
        if (mContext == null) {
            try {
                throw new Exception("请初始化CommandHolder");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //升级回调，成功Or失败
        UpdateUtils.getInstance().setUpdateCallback(new UpdateCallback() {
            @Override
            public void successful() {
                handler.sendEmptyMessage(4);
            }
        });

        //定时器，一分钟，检查更新是否成功
        //升级更新失败，是通过定时器判断
        upadteTimerCount = new CountDownTimerUtil(60000, 1000,
                new CountDownTimerUtil.TimeFinish() {
                    @Override
                    public void onFinish() {
                        if (UpdateUtils.getInstance().updateHolder.count != 33) {//失败了
                            if (progressDialog != null) progressDialog.dismiss();
//                            useUpdateProxyCall.failure("升级失败");
                            useUpdateProxyCall.failure(ErrorStatus.UPADTE_FAILURE_ERROR);
                        }
                    }
                });
    }


    public void setUpadteBuffer(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        AppInfo.getInstance().setBytes(bytes);
    }


    /**
     * 流程：   1 .点击checkUpdate
     * 2.重启MCC
     * 3.尝试连接心跳//如果失败，继续连接
     * 4. 连接成功，尝试进行升级
     *
     * @throws Exception
     */
    public void checkUpdate() throws Exception {
        //setFilePath(file);
        if (AppInfo.getInstance().getBytes() == null || AppInfo.getInstance().getBytes().length == 0) {
//            useUpdateProxyCall.failure("没有新的烧录档");
            useUpdateProxyCall.failure(ErrorStatus.NOT_BURN_ERROR);
            return;
        }
        try {
            //重启MCU才能获取版本
            UpdateUtils.getInstance().restartMcu(new UpdateUtils.RestartMcuCall() {
                @Override
                public void onSuccessful() {
                    LogUtils.i("00000000000000000000000000000");
                    //首次主动尝试连接串口
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                startExecuteUpdateCommand();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 500);
                }
            });
        } catch (Exception e) {

        }
    }


    // TODO AP_Connect
    public void startExecuteUpdateCommand() throws IOException {//[55, 07, 06, 00, 9E, 9F, B7]
        LogUtils.i("-------------500000000000000----");
        byte[] bytes = VerifyUtil.getBytes(0X55, 0X06, 0X00);
        try {
            SerialPortService.getInstance().sendMessage(bytes, new SerialPortService.SerualPrortMechanicalCllBack() {
                @Override
                public void callbytes(byte[] buffer, SerialPortData serialPortData) throws IOException {
                    if (serialPortData.getCmd() == 0X4F && serialPortData.getType() == 0X00) {
                        LogUtils.i("-------------连接成功----");
                        try {
                            tryupadte();//连接成功，尝试更新
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void tryupadte() throws IOException {
        //重启MCU才能获取版本
        LogUtils.i("-------------555555555555555555555555555555----");
        //[0]烧录档的版本号  [1]芯片型号
        final String[] newNewBurnFileArr = UpdateUtils.getInstance().getNewBurnFileVersion();
        //V1226  31220500
        if (newNewBurnFileArr == null || newNewBurnFileArr.length < 2) {
//            useUpdateProxyCall.failure("请获新版本烧录档取芯片型号or版本号失败");
            useUpdateProxyCall.failure(ErrorStatus.NOT_BURN_VERSION_ERROR);
            return;
        }
        newVersionservice = newNewBurnFileArr[0];
        newModelservice = newNewBurnFileArr[1];

        if (newVersionservice.isEmpty() && newModelservice.isEmpty()) {
//            useUpdateProxyCall.failure("请获新版本烧录档取芯片型号or版本号失败");
            useUpdateProxyCall.failure(ErrorStatus.NOT_BURN_VERSION_ERROR);
            return;
        }

        try {
            UpdateUtils.getInstance().getOldBurnFileModel(new UpdateUtils.OldBurnFileDataCallback() {
                @Override
                public void onSuccessful() {
                    LogUtils.i("-------------getoldburndata----");
                    //更新后获取的版本；newviewsion是最新版本，oldviewsion是旧版本
                    //重启开机，获取的oldversion是当前版本，newviewsion是null
                    String ChipVersion = AppInfo.getInstance().getChipVersion();//31220500
//                String newVersion = AppInfo.getInstance().getOldVersion();//V1226
                    String version = "";
                    if (tempversion.isEmpty()) {
                        version = AppInfo.getInstance().getOldVersion();//V1226
                        tempversion = AppInfo.getInstance().getOldVersion();//记录当前版本
                    } else {
                        version = AppInfo.getInstance().getNewVersion();//V1226
                    }
//                    LogUtils.i("newversion=" + newNewBurnFileArr[0] + "\\n" + "newModel="
//                            + newNewBurnFileArr[1] + "\n" + "oldvserion=" + AppInfo.getInstance().getOldVersion()
//                            + "\n" + "oldmodel=" + ChipVersion);

                    //比较芯片型号，不一致，不让更新
//                newversion=V1225\     nnewModel=31220500
                    //oldvserion=V1225  oldmodel=31220500
                    //如果
                    if (newNewBurnFileArr[1].equals(ChipVersion)) {
                        if (newVersionservice.equals(version)) {
//                            status_text = "当前已经是最新版本";
                            callbakUI(1);
                        } else {
                            callbakUI(2);
                            startUpadteTime();
                            LogUtils.i("-------------startupdate----");
                            UpdateUtils.getInstance().startupdate();
                        }
                    } else {
//                        status_text = "MCU版本号不一致";
                        callbakUI(3);
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    //当前已经是最新版本
                    useUpdateProxyCall.failure(ErrorStatus.ALREADY_NEW_UPADTE_ERROR);
                    break;
                case 2:
                    progressDialog = LayerUtil.showLoadingSpinner(activity, "重启MCU..准备升级");
                    break;
                case 3:
                    //MCU版本号不一致
                    useUpdateProxyCall.failure(ErrorStatus.MCUCODE_ERROR);
                    break;
                case 4:
                    String newversion = AppInfo.getInstance().getNewVersion();
                    if (newVersionservice.equals(newversion)) {
                        if (progressDialog != null) progressDialog.dismiss();
                        cancelUpadteTime();
                        useUpdateProxyCall.successful(ErrorStatus.UPADTE_SUCCESSFUL, newModelservice);
                    }
                    break;
            }
        }
    };


    private void startUpadteTime() {
        upadteTimerCount.start();
    }

    private void cancelUpadteTime() {
        upadteTimerCount.cancel();
    }

    private void callbakUI(int type) {
        handler.sendEmptyMessage(type);
    }


    //这个是SDK内部用的升级回调，不是上层调用用

    public  interface UpdateCallback {
        void successful();
    }


    //定义上层回调的接口

    public UseUpdateProxyCall useUpdateProxyCall;

    public interface UseUpdateProxyCall {
        void successful(String status_text, String mcucode);

        void failure(String status_text);
    }

}
