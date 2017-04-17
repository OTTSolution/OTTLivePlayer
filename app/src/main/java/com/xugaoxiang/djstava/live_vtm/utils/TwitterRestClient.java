package com.xugaoxiang.djstava.live_vtm.utils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

/**
 * Created by user on 2016/8/29.
 */
public class TwitterRestClient {

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, TextHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    public static void post(String url, RequestParams params, TextHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

    public static void downLoadImg(String url , BinaryHttpResponseHandler responseHandler){
        client.get(url , responseHandler);
    }
}
