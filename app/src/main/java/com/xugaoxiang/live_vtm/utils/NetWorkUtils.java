package com.xugaoxiang.live_vtm.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class NetWorkUtils {

    public static boolean getNetState(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo isNetWorkInfo = manager.getActiveNetworkInfo();
        boolean isNetWork = false;
        if (isNetWorkInfo != null) {
            isNetWork = isNetWorkInfo.isAvailable();
        }
        if (!isNetWork) {
            return false;
        }else {
            return true;
        }
    }
}
