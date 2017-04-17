package com.xugaoxiang.djstava.live_vtm.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.xugaoxiang.djstava.live_vtm.activity.ServiceProgramActivity;
import com.xugaoxiang.djstava.live_vtm.R;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by zero on 2016/11/23.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static CrashHandler instance;

    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private Map<String, String> deviceInfo = new HashMap<>();
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private FileOutputStream fos;
    private int i;
    private boolean flag;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        if (instance == null)
            instance = new CrashHandler();
        return instance;
    }

    public void init(Context context) {
        mContext = context;
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable ex) {
//        handleException(ex);
//        android.os.Process.killProcess(android.os.Process.myPid());
//        System.exit(1);
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(t, ex);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error : ", e);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean  handleException(Throwable ex) {
        if (flag || ex == null)
            return false;
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();
        //收集设备参数信息
        collectDeviceInfo(mContext);
        //保存日志文件
        if (FileUtils.isExternalStorageAvailable()){
            upLoadErrorMessage(ex);
        }
        return true;
    }

    public void upLoadErrorMessage(final Throwable ex){ Observable.create(new Observable.OnSubscribe<String>() {
        @Override
        public void call(Subscriber<? super String> subscriber) {
            String fileName = saveCrashInfoToFile(ex);
            subscriber.onNext(fileName);
            subscriber.onCompleted();
        }
    })
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(new Subscriber<String>() {
                @Override
                public void onCompleted() {
                    Log.e("CrashHandler" , "onCompleted");
                }

                @Override
                public void onError(Throwable e) {
                    Log.e("-------------" , e.getMessage());
                }

                @Override
                public void onNext(String s) {
                    String macAddress = MacAddress.getMacFromIp()==null?MacAddress.getMacFromFile():MacAddress.getMacFromIp();
                    RequestParams params = new RequestParams();
                    try {
                        params.put("log" , new File(FilePath.CRASH , s));
                        params.put("mac" , macAddress);
                        Looper.prepare();
                        TwitterRestClient.post(ServiceProgramActivity.BASE_URI+"/api/log", params, new TextHttpResponseHandler() {
                            @Override
                            public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
                            }

                            @Override
                            public void onSuccess(int i, Header[] headers, String s) {
                            }
                        });
                        Looper.loop();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    /**
     * 收集设备参数信息
     * @param context
     */
    private void collectDeviceInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                deviceInfo.put("versionName", pi.versionName == null ? "versionName not set" : pi.versionName);
                deviceInfo.put("versionCode", String.valueOf(pi.versionCode));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error while collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                // 通过设置Accessible属性为true,才能对私有变量进行访问，不然会得到一个IllegalAccessException的异常
                field.setAccessible(true);
                deviceInfo.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "Error while collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     * @param ex
     * @return
     */
    public String saveCrashInfoToFile(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : deviceInfo.entrySet())
            sb.append(entry.getKey() + "=" + entry.getValue() + "\n");
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        Log.e("CrashHandler" , "error"+result);
        sb.append(result);
        try {
            String time = formatter.format(new Date());
            String fileName = mContext.getString(R.string.app_name) + "-crash-" + time  + ".log";
            File dir = new File(FilePath.CRASH);
            if (!dir.exists())
                dir.mkdirs();
            File file = new File(dir, fileName);
            if (!file.exists())
                file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "Error while writing crashFile...", e);
        }finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}
