package com.mycoolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mycoolweather.app.R;
import com.mycoolweather.app.db.CoolWeatherDB;
import com.mycoolweather.app.model.City;
import com.mycoolweather.app.model.County;
import com.mycoolweather.app.model.Province;
import com.mycoolweather.app.util.HttpCallbackListener;
import com.mycoolweather.app.util.HttpUtil;
import com.mycoolweather.app.util.Utility;


public class ChooseAreaActivity extends Activity{
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private List<String> dataList = new ArrayList<String>();
	private ArrayAdapter<String> adapter;
	private ListView listView;
	private TextView titleText;
	private ProgressDialog progressDialog;
	
	private CoolWeatherDB coolWeatherDB;
	
	private List<Province> provinceList;
	private List<City> cityList;
	private List<County> countyList;
	
	/*
	 * �����selectedProvince��selectedCity��Ϊȫ�ֱ��������Ǳ���ֵֻ�����������1����WeatherActivity��������
	 * 2���ڰ��������б���ֵ���������ָ�ֵ�ԸĶ�selectedProvince��selectedCity��ֵ����ȫͬ�ʵġ�������
	 * ��WeatherActivity�ػص��������ʡ�����б����л����Ǹս����app����ʡ�����е��б��л�������ҪselectedProvince��
	 * selectedCity����ʵ�ʲ����У����п�������WeatherActivity���ص�������ֽ�������ʡ���ص��л�������������ʵ����Ҫ����
	 * selectedProvince��selectedCity��ȫ�ֱ��������Ա����������������ĸ�ֵ�������ſ��ܵõ���ȷ���б���ʾ��
	 * 
	 * ��Ϊʡ�����б���л���ʾ��ҪselectedProvince��selectedCity������ֻҪselectedProvince��selectedCity���л���ֵ��ȷ��
	 * ��ô��ʹ��query...()��õ����б�Ҳһ������ȷ�ģ���˺��������ǹ�עselectedProvince��selectedCity���л���ֵ�Ƿ���ȷ��
	 * */
	private Province selectedProvince;
	private City selectedCity;
	
	private int currentLevel;
	private boolean isFromWeatherActivity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		
		/*
		 * �����������µõ�֮ǰѡ�е�ʡ�ݺ��У�������������ʾ֮ǰ���б�ע�⣬�ǡ�֮ǰ���б�����
		 * */
		selectedProvince = (Province) getIntent().getSerializableExtra("Province");
		selectedCity = (City) getIntent().getSerializableExtra("City");
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		titleText = (TextView) findViewById(R.id.title_text);
		listView = (ListView) findViewById(R.id.list_view);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setAdapter(adapter);
		
		//����������������뵽�����������ʾʡ���б���Ȼ�����п��ܸ���ʡ���б�
		queryProvinces();
		
		/*            $$$$$����app���Զ���ת��WeatherActivityִ�����if���$$$$$
		 * ��ĳ�ν����app�Ѿ���ʾ��ĳ�����е������󣨴�ʱcity_selectedΪtrue������һ�ν����app��ʱ��
		 * ֱ����ʡ���б��Զ���ת����һ����ʾ�ĳ���������WeatherActivity��С���Ȼ���������city_selected
		 * Ҳ��Ϊtrue������ȴ��Ӧ����ת���ֱ��ǣ���WeatherActivity�е��switch_city �Լ� ��WeatherActivity�е��
		 * ���ؼ��������������ʵͳһΪ��WeatherActivity��ת��������ǲ��������ص�WeatherActivity�ġ�
		 * ����������ķ���������WeatherActivity��ת�������ʱ����isFromWeatherActivity����������
		 * ���߱����ʱ����ת��
		 * */
		if (prefs.getBoolean("city_selected", false)&& !isFromWeatherActivity) {		
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);//��ʱ��û�д����κβ�������countyCodeΪnull
			finish();
			
			/*
			 * return��������ֱ���˳�onCreate�������Ҳ����˵����Ĵ��붼��ִ���ˣ���������˴���Ч�ʣ�
			 * ��Ȼ����ҪreturnӦ��Ҳ�ǿ��Ե�
			 * */
			return;
		}
		
		/*        $$$$$�ɵ���ؽ���WeatherActivity�ְ��·��ؼ��ص����ִ�����if���$$$$$
		 * ��selectedCityΪnullʱ�����if���û��ʲô�ã���ôʲôʱ��selectedCity��Ϊnull�أ��Ǿ��ǵ��������
		 * WeatherActivity���·��ؼ�������ʱ����ʱ����WeatherActivity������selectedCity��ֵ��Ϊnull��
		 * ����������ʾ֮ǰ�����б�
		 * */
		if(selectedCity!=null){
			queryCounties();//����������������
	}				
		
		listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,long arg3){
				if(currentLevel == LEVEL_PROVINCE)
				{
					selectedProvince = provinceList.get(index);					
					queryCities();
				}
				else
				{
					if(currentLevel == LEVEL_CITY)
					{
					    selectedCity = cityList.get(index);						
						queryCounties();
					}
					else 
					{
						if(currentLevel == LEVEL_COUNTY)
						{
							Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
							String countyCode = countyList.get(index).getCountyCode();
							Bundle mBundle = new Bundle();		
						
							/*
							 * ���ĳһ����֮����WeatherActivity���������������ֱ���countyCode��selectedProvince��selectedCity��
							 * ���У�countyCode��Ϊ��ѯ���������ķ������ϵĵ�ַ�����������֮�󣬱���ᱻ�رգ����selectedProvince��
							 * selectedCity��ֵҲ��������������Ҫ�����������󴫵ݵ�WeatherActivity��У���ʱ���棻������WeatherActivity�
							 * ���·��ؼ���ʱ��selectedProvince��selectedCity�ص����������ChooseAreaActivityӦ����ʾ��Щ���б�����б�
							 * */
							mBundle.putSerializable("Province", selectedProvince);								
							mBundle.putSerializable("City", selectedCity);							
							intent.putExtras(mBundle);							
							intent.putExtra("county_code", countyCode);
							startActivity(intent);
							finish();
						}
					}					
				}
			}
		});
				
	}

	private void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if(provinceList.size()>0){
			dataList.clear();
			for(Province province : provinceList)
				dataList.add(province.getProvinceName());
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVINCE;
		}else
			queryFromServer(null,"province");	
	}
	
	
	protected void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size()>0){
			dataList.clear();
			for(City city : cityList)
				dataList.add(city.getCityName());
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		}else
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		
	}

	protected void queryCounties() {
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size()>0){
			dataList.clear();
			for(County county : countyList)
				dataList.add(county.getCountyName());
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());//��������⣡����
			currentLevel = LEVEL_COUNTY;
		}else{		
			queryFromServer(selectedCity.getCityCode(),"county");		
		}
	}

	private void queryFromServer(String code, final String type) {
		String address;
		if(!TextUtils.isEmpty(code))
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		else
			address = "http://www.weather.com.cn/data/list3/city.xml";
		
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener(){

			@Override
			public void onFinish(String response) {
				boolean result = false;
				if("province".equals(type)){
					result = Utility.handleProvincesResponse(coolWeatherDB, response);
				}else {
					if("city".equals(type)){
					result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
				}else {
					if("county".equals(type))						
					    result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				}
				if(result){
					runOnUiThread(new Runnable(){
						@Override
						public void run(){
							closeProgressDialog();
							if("province".equals(type))
								queryProvinces();
							else {
								if("city".equals(type))
								    queryCities();
							    else if("county".equals(type))						    
								    queryCounties();
							}
						}					
					});
				}
				
			}

			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this,
										"����ʧ��", Toast.LENGTH_SHORT).show();
					}
				});
				
			}
			
		});
		
	}

	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
		
	}
	
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		
	}
	
	@Override
	public void onBackPressed() {
		if(currentLevel == LEVEL_CITY)
		{
			queryProvinces();
		}
		else
		{
			if(currentLevel == LEVEL_COUNTY)
			{
				queryCities();
			}
			else{
				if(currentLevel == LEVEL_PROVINCE)
				{
					finish();
				}
			}			
		}	
	}
}
