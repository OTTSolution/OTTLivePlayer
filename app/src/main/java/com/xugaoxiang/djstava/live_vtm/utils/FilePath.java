package com.xugaoxiang.djstava.live_vtm.utils;

import android.os.Environment;

import com.xugaoxiang.djstava.live_vtm.MyApplication;
import com.longjingtech.ott.live_vtm.R;

import java.io.File;

/**
 * Created by zero on 2016/11/24.
 */

public class FilePath {

    public static final String APPSERVER = Environment.getExternalStorageDirectory() + File.separator + "MyApp" + File.separator + "AppServer";

    public static final String CRASH = Environment.getExternalStorageDirectory()
            + File.separator
            + MyApplication.context.getString(R.string.app_name)
            + File.separator;
}
