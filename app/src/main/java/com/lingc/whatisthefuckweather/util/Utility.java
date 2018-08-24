package com.lingc.whatisthefuckweather.util;

import android.text.TextUtils;
import android.util.Log;

import com.lingc.whatisthefuckweather.db.City;
import com.lingc.whatisthefuckweather.db.County;
import com.lingc.whatisthefuckweather.db.Province;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Utility {

    static String weatherId1;

    /**
     * 解析和处理服务器返回的省级数据
     *
     * @param response
     * @return
     */
    public static boolean handleProvinceResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvince = new JSONArray(response);
                for (int i = 0;i < allProvince.length(); i++) {
                    JSONObject provinceObject = allProvince.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     *
     * @param response
     * @return
     */
    public static boolean handleCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器的县区级数据
     *
     * @param response
     * @return
     */
    public static boolean handleCountyResponse(String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 解析和处理服务器的县区级数据（弃用方法）
     * @param response
     * @return
     */
    public static boolean handleCountyResponse2(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                String result = new JSONObject(response).getString("result");
                JSONArray allCounty = new JSONArray(result);
                for (int i = 0; i < allCounty.length(); i++) {
                    JSONObject countyObject = allCounty.getJSONObject(i);
                    County county = new County();
                    county.setCityId(countyObject.getInt("parentid"));
                    county.setCountyName(countyObject.getString("name"));
                    /* 查询Weather Id */
                    //getWeatherId(countyObject.getString("name"));
                    /* Ah--oh--，不要让主线程跑得太快，子线程会追不上的 */
                    //Thread.sleep(310);
                    //county.setWeatherId(weatherId1);
                    boolean a = county.save();
                    //Log.d("Ok?", a + "");
                    //Log.d("Over", weatherId1 + "");
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 获取WeatherId
     *
     * @param countyName
     */
    public static void getWeatherId(String countyName) {
        HttpUtil.sendHttpRequest("https://search.heweather.com/find?location=" +
                countyName + "&key=ad57db89cd9e4c6ca40b46d42d3c11f0", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String result = response.body().string();
                    String heWeather6 = new JSONObject(result).getString("HeWeather6");
                    JSONArray heWeather6Array = new JSONArray(heWeather6);
                    JSONArray basicArray = new JSONArray(heWeather6Array.getJSONObject(0).getString("basic"));
                    String weatherId = basicArray.getJSONObject(0).getString("cid");
                    Utility.weatherId1 = weatherId;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
