package com.xugaoxiang.live_vtm.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.xugaoxiang.live_vtm.bean.LiveBean;
import com.xugaoxiang.live_vtm.utils.FilePath;
import com.xugaoxiang.live_vtm.utils.MacAddress;
import com.xugaoxiang.live_vtm.utils.NetWorkUtils;
import com.xugaoxiang.live_vtm.utils.StreamUtils;
import com.xugaoxiang.live_vtm.utils.TwitterRestClient;
import com.google.gson.Gson;
import com.xugaoxiang.live_vtm.R;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ServiceProgramActivity extends Activity {
    private static final String TAG = ServiceProgramActivity.class.getName();
    public static String BASE_URI = "http://10.10.10.200:8080";

    private static final String LIVE_URI = "/api/live-l2";
    private static final String LIVE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyApp/LiveInfo";
    private static final String LOCAL_URL = Environment.getExternalStorageDirectory() + File.separator + "MyApp" + File.separator + "AppServer";
    private final static int CODE_NETWORK_ERROR = 0;

    private final static int CODE_NETWORK_SUCCESS = 1;

    private static int what = 0;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_NETWORK_SUCCESS:
                    setLiveData();
                    break;

                case CODE_NETWORK_ERROR:
                    Toast.makeText(ServiceProgramActivity.this, "网络连接异常，请检查网络!", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        }
    };

    private Gson gson;
    private LiveBean liveBean;
    public static String language;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_list);
        gson = new Gson();
        Intent intent = getIntent();
        if (intent != null) {
            language = intent.getStringExtra("language");
            Log.e(TAG, language + "");
        }

        String localUrl = getLocalFileURL();
        if (!TextUtils.isEmpty(localUrl)) {
            BASE_URI = localUrl;
        }

        initData();
    }

    private void test() {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                String fileName = "Live_vtm-crash-2000-01-01 11:29:29.log";
                subscriber.onNext(fileName);
                subscriber.onCompleted();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        Log.e(TAG, "onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onNext(String s) {
                        String macAddress = MacAddress.getMacFromIp() == null ? MacAddress.getMacFromFile() : MacAddress.getMacFromIp();
                        RequestParams params = new RequestParams();
                        try {
                            params.put("log", new File(FilePath.CRASH, s));
                            params.put("mac", macAddress);
                            TwitterRestClient.post("http://10.10.10.200:8888/api/log", params, new TextHttpResponseHandler() {
                                @Override
                                public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
                                    Log.e(TAG, "onFailure:" + s);
                                }

                                @Override
                                public void onSuccess(int i, Header[] headers, String s) {
                                    Log.e(TAG, "onSuccess:" + s);
                                }
                            });
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private String getLocalFileURL() {
        File file = new File(LOCAL_URL);
        String str = "";
        if (file.exists()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                str = StreamUtils.stream2String(stream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return str;
    }

    private void setLiveData() {
        if (liveBean != null) {
            MainActivity.openLive(this, liveBean);
            finish();
        }
    }

    private void initData() {
        liveBean = new LiveBean();
        String str = getFileLiveInfo();
        Log.e(TAG, str);

        if (!TextUtils.isEmpty(str)) {
            liveBean = gson.fromJson(str, LiveBean.class);
        }

        if (!NetWorkUtils.getNetState(this)) {
            handler.sendEmptyMessage(CODE_NETWORK_ERROR);
            return;
        }

        getServiceLiveList();
    }

    private String getFileLiveInfo() {
        File file = new File(LIVE_DIR, "liveList.xml");
        String str = "";
        if (file.exists()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                str = StreamUtils.stream2String(stream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return str;
    }

    private void getServiceLiveList() {
        new Thread() {

            private FileOutputStream stream;

            @Override
            public void run() {
                try {
                    String macAddress = null;

                    if (MacAddress.getMacFromIp() == null) {
                        macAddress = MacAddress.getMacFromFile();
                    }

                    HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URI + LIVE_URI + "?user_id=" + macAddress).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.connect();
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        String s = StreamUtils.stream2String(is);
                        if (!TextUtils.isEmpty(s)) {
                            liveBean = gson.fromJson(s, LiveBean.class);
                            File dirFile = new File(LIVE_DIR);
                            if (!dirFile.exists() || !dirFile.isDirectory()) {
                                dirFile.mkdirs();
                            }
                            File file = new File(LIVE_DIR, "liveList.xml");
                            stream = new FileOutputStream(file);
                            stream.write(s.getBytes());
                            what = CODE_NETWORK_SUCCESS;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    what = CODE_NETWORK_ERROR;
                    if (liveBean != null) {
                        what = CODE_NETWORK_SUCCESS;
                    }
                } finally {
                    handler.sendEmptyMessage(what);
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
}
