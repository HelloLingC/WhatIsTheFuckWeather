package com.lingc.whatisthefuckweather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {

    public static void sendHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address)
                .build();
        okHttpClient.newCall(request).enqueue(callback);
    }
}
