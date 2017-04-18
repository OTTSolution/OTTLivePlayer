package com.xugaoxiang.live_vtm.utils;

import android.os.Environment;

import com.xugaoxiang.live_vtm.MyApplication;
import com.xugaoxiang.live_vtm.R;

import java.io.File;


public class FilePath {

    public static final String APPSERVER = Environment.getExternalStorageDirectory() + File.separator + "MyApp" + File.separator + "AppServer";

    public static final String CRASH = Environment.getExternalStorageDirectory()
            + File.separator
            + MyApplication.context.getString(R.string.app_name)
            + File.separator;
}
