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
	 * 这里的selectedProvince，selectedCity作为全局变量，它们被赋值只有两种情况，1、由WeatherActivity传递来；
	 * 2、在按键监听中被赋值。而这两种赋值对改动selectedProvince，selectedCity的值是完全同质的。无论是
	 * 由WeatherActivity重回到本活动后在省市县列表中切换还是刚进入该app后在省市县中的列表切换，都需要selectedProvince，
	 * selectedCity。在实际操作中，很有可能是由WeatherActivity返回到本活动后，又进行其他省市县的切换，而这个情况的实现需要基于
	 * selectedProvince，selectedCity是全局变量，可以被这两种情况交错更改赋值，这样才可能得到正确的列表显示。
	 * 
	 * 因为省市县列表的切换显示需要selectedProvince，selectedCity，所以只要selectedProvince，selectedCity的切换幅值正确，
	 * 那么当使用query...()后得到的列表也一定是正确的；因此核心任务是关注selectedProvince，selectedCity的切换幅值是否正确。
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
		 * 下面两行重新得到之前选中的省份和市，以用于重新显示之前的列表（注意，是“之前的列表”）。
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
		
		//不管哪种情况，进入到本活动后，首先显示省份列表（当然后面有可能覆盖省份列表）
		queryProvinces();
		
		/*            $$$$$进入app后自动跳转到WeatherActivity执行这个if语句$$$$$
		 * 当某次进入该app已经显示了某个城市的天气后（此时city_selected为true），下一次进入该app的时候，
		 * 直接由省份列表自动跳转到上一次显示的城市天气的WeatherActivity活动中。当然有两种情况city_selected
		 * 也是为true，但是却不应该跳转，分别是：在WeatherActivity中点击switch_city 以及 在WeatherActivity中点击
		 * 返回键。这两种情况其实统一为由WeatherActivity跳转到本活动后是不能再跳回到WeatherActivity的。
		 * 解决这个问题的方法就是由WeatherActivity跳转到本活动的时候传入isFromWeatherActivity参数，用于
		 * 告诉本活动此时不跳转。
		 * */
		if (prefs.getBoolean("city_selected", false)&& !isFromWeatherActivity) {		
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);//此时，没有传递任何参数，即countyCode为null
			finish();
			
			/*
			 * return的作用是直接退出onCreate（）活动，也就是说下面的代码都不执行了，这样提高了代码效率；
			 * 当然，不要return应该也是可以的
			 * */
			return;
		}
		
		/*        $$$$$由点击县进入WeatherActivity又按下返回键回到本活动执行这个if语句$$$$$
		 * 当selectedCity为null时，这个if语句没有什么用；那么什么时候selectedCity不为null呢？那就是当本活动是由
		 * WeatherActivity按下返回键开启的时候，这时候，由WeatherActivity返回来selectedCity的值不为null，
		 * 可以用来显示之前的县列表
		 * */
		if(selectedCity!=null){
			queryCounties();//问题就在这里！！！！
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
							 * 点击某一个县之后，向WeatherActivity传递三个参数，分别是countyCode，selectedProvince，selectedCity。
							 * 其中，countyCode作为查询该县天气的服务器上的地址；当点击了县之后，本活动会被关闭，因此selectedProvince，
							 * selectedCity的值也将被清除，因此需要将这两个对象传递到WeatherActivity活动中，暂时保存；这样当WeatherActivity活动
							 * 按下返回键的时候，selectedProvince，selectedCity回到本活动，告诉ChooseAreaActivity应该显示哪些市列表和县列表
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
			titleText.setText("中国");
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
			titleText.setText(selectedCity.getCityName());//问题就在这！！！
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
										"加载失败", Toast.LENGTH_SHORT).show();
					}
				});
				
			}
			
		});
		
	}

	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
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
