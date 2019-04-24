package com.mcuupadteproject;


import android.app.Application;

import com.mcuupdate.arr.UpdateUtils;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化
        UpdateUtils.getInstance().init(this);

    }
}
