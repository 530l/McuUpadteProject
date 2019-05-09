package com.example.cabinetlib.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;


public class LayerUtil_Cb {
    public static AlertDialog.Builder showMessage(Context context, String title, String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton("确定", null);
        builder.show();
        return builder;
    }

    public static void showToast(Context context, String msg){
        showToast(context, msg, Toast.LENGTH_LONG);
    }

    public static void showToast(final Context context, final String msg, final int duration) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, duration).show();
            }
        });

    }

    public static ProgressDialog showLoadingSpinner(Context context){
        return showLoadingSpinner(context, "加载中，请稍后.");
    }

    public static ProgressDialog showLoadingSpinner(Context context, String msg){
        ProgressDialog dialog = new ProgressDialog(context);
        //dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setMessage(msg);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

}
