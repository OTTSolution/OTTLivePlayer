package com.xugaoxiang.live_vtm;

import android.app.Application;
import android.content.Context;

/**
 * Created by user on 2017/3/9.
 */

public class MyApplication extends Application {
    public static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
//        CrashHandler.getInstance().init(context);
    }
}
