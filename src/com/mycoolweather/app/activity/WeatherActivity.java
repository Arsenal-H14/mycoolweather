package com.mycoolweather.app.activity;

import com.mycoolweather.app.R;
import com.mycoolweather.app.model.City;
import com.mycoolweather.app.model.Province;
import com.mycoolweather.app.service.AutoUpdateService;
import com.mycoolweather.app.util.HttpCallbackListener;
import com.mycoolweather.app.util.HttpUtil;
import com.mycoolweather.app.util.Utility;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WeatherActivity extends Activity implements OnClickListener{
	private LinearLayout weatherInfoLayout;
	
	/**
	 * 用于显示城市名
	 */
	private TextView cityNameText;
	/**
	 * 用于显示发布时间
	 */
	private TextView publishText;
	/**
	 * 用于显示天气描述信息
	 */
	private TextView weatherDespText;
	/**
	 * 用于显示气温1
	 */
	private TextView temp1Text;
	/**
	 * 用于显示气温2
	 */
	private TextView temp2Text;
	/**
	 * 用于显示当前日期
	 */
	private TextView currentDateText;
	
	private Button switchCity;
	private Button refreshWeather;
	
	private City selectedCity;
	private Province selectedProvince;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherDespText = (TextView) findViewById(R.id.weather_desp);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		currentDateText = (TextView) findViewById(R.id.current_date);
		switchCity = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
		
		/*
		 * 下面3行得到ChooseAreaActivity传递过来的参数值，其中countyCode仅在本活动中使用；而selectedProvince，selectedCity
		 * 并不会在本活动中使用，而只是当按下返回键的时候将这两个对象返回到ChooseAreaActivity活动中。
		 * */
		String countyCode = getIntent().getStringExtra("county_code");
		selectedCity = (City) getIntent().getSerializableExtra("City");
		selectedProvince = (Province) getIntent().getSerializableExtra("Province");
		
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
		
		/*
		 * countyCode为null时，直接由SharedPreference文件中的数据显示天气信息
		 * */
		if(!TextUtils.isEmpty(countyCode)){
			publishText.setText("同步中...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		}else{
			showWeather();
		}
		
	}	

	private void queryWeatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(address, "countyCode");
		
	} 
		
	private void queryFromServer(final String address,  final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener(){
			@Override
			public void onFinish(final String response){				
				if("countyCode".equals(type)){
					if(!TextUtils.isEmpty(response)){
						String[] array = response.split("\\|");
						
						if(array != null && array.length == 2){
							String weatherCode = array[1];
							queryWeatherInfo(weatherCode);
						}		
					}
				}else if("weatherCode".equals(type)){
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							showWeather();						
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e){
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						publishText.setText("同步失败");
					}
				});
			}	
		});
		
	}

	protected void queryWeatherInfo(String weatherCode) {
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(address, "weatherCode");	
	}

	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("今天" + prefs.getString("publish_time", "") + "发布");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		//仅仅传递from_weather_activity，告诉ChooseAreaActivity不要再跳转过来
		case R.id.switch_city:
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
			
			//从SharedPreferences文件中得到当前城市中的weather_code，直接就可以用于刷新当前城市的天气
		case R.id.refresh_weather:
			publishText.setText("同步中...");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			if(!TextUtils.isEmpty(weatherCode))
				queryWeatherInfo(weatherCode);
			break;

		default:
			break;
		}
		
	}
	
	@Override
	public void onBackPressed() {
		/*
		 *按下返回键后，本活动返回 selectedProvince，selectedCity，from_weather_activity给ChooseAreaActivity活动。
		 * 其中，from_weather_activity的作用是告诉ChooseAreaActivity活动不要再次跳转到本活动中，其作用与按下switch_city
		 * 后的from_weather_activity相同。
		 * */
		Intent intent = new Intent(this, ChooseAreaActivity.class);		
		intent.putExtra("from_weather_activity", true);		
		Bundle mBundle = new Bundle();
		mBundle.putSerializable("Province", selectedProvince);			
		mBundle.putSerializable("City", selectedCity);
		intent.putExtras(mBundle);
		startActivity(intent);
		finish();
	}
}
