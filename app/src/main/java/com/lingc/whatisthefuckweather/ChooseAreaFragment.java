package com.lingc.whatisthefuckweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lingc.whatisthefuckweather.db.City;
import com.lingc.whatisthefuckweather.db.County;
import com.lingc.whatisthefuckweather.db.Province;
import com.lingc.whatisthefuckweather.util.HttpUtil;
import com.lingc.whatisthefuckweather.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    /**
     * 级别变量
     */
    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 3;

    /**
     * 布局变量
     */
    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private List<String> dataList = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    private ProgressDialog progressDialog;

    /**
     *
     */
    private int provinceCode;

    private int cityCode;

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县区列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView = view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                } else if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有省份，优先从数据库中查询
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size() > 0) {
            /* 如果数据库中存在数据 */
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            /* 如果不存在 */
            String address = "http://guolin.tech/api/china";
            queryFormService(address, "province");
        }
    }

    /**
     * 查询选中省内所有的城市，优先从数据库中查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            /* 数据库中存在数据 */
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            /* 不存在 */
            provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFormService(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县区，优先从数据库中查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            /* 数据库中存在数据 */
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            /* 不存在 */
            cityCode = selectedCity.getCityCode();
            //String address = "http://api.jisuapi.com/area/town?parentid=" + cityCode + "&appkey=c112a0f5c4a602e3";
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFormService(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器查询数据
     */
    private void queryFormService(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String reponseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(reponseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(reponseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(reponseText, selectedCity.getId());
                    Log.d("Main", result + "");
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
