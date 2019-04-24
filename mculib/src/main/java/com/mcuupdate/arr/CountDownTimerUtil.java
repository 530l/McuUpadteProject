package com.mcuupdate.arr;

import android.os.CountDownTimer;

import com.mcuupdate.utils.LogUtils;


/**
 * 倒计时工具
 */
public class CountDownTimerUtil extends CountDownTimer {

    /**
     * 构造函数
     *
     * @param millisInFuture    持续时间
     * @param countDownInterval 间隔时间
     */
    long millisInFuture;

    public CountDownTimerUtil(long millisInFuture, long countDownInterval, TimeFinish timeFinish) {
        super(millisInFuture, countDownInterval);
        this.timeFinish = timeFinish;
        this.millisInFuture = millisInFuture;
    }

    /**
     * 每隔间隔时间，就会回调这
     *
     * @param millisUntilFinished 剩余时间
     */
    @Override
    public void onTick(long millisUntilFinished) {
        if (millisInFuture == 300) {
            LogUtils.i(millisUntilFinished / 100 + "毫秒");
        } else {
            LogUtils.i(millisUntilFinished / 1000 + "秒");
        }

    }

    /**
     * 结束的时候回调
     */
    @Override
    public void onFinish() {
        if (timeFinish != null) {
            timeFinish.onFinish();
        }
    }

    public TimeFinish timeFinish;

    public interface TimeFinish {
        void onFinish();
    }
}

